package com.example.proxy.controller;

import com.example.proxy.config.RouteConfig;
import com.example.proxy.config.RouteDefinition;
import com.example.proxy.registry.OpenApiRegistry;
import com.example.proxy.validation.*;
import com.example.proxy.security.jws.DetachedJwsSigner;
import com.example.proxy.security.jws.OutboundSigningFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.openapi4j.operation.validator.model.impl.DefaultRequest;
import org.openapi4j.operation.validator.model.impl.DefaultResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@RestController
@RequestMapping("/proxy")
public class GenericProxyController {

  private final WebClient webClient;
  private final RouteConfig routeConfig;
  private final OpenApiRegistry openApiRegistry;

  private DetachedJwsSigner signer;

  @Value("${security.jws.keystore.path:}")
  private String jwsKsPath;
  @Value("${security.jws.keystore.password:}")
  private String jwsKsPassword;
  @Value("${security.jws.key.alias:}")
  private String jwsKeyAlias;
  @Value("${security.jws.key.password:}")
  private String jwsKeyPassword;

  public GenericProxyController(WebClient webClient, RouteConfig routeConfig, OpenApiRegistry openApiRegistry) {
    this.webClient = webClient;
    this.routeConfig = routeConfig;
    this.openApiRegistry = openApiRegistry;
  }

  @PostConstruct
  public void init() {
    try {
      if (!jwsKsPath.isBlank()) {
        signer = new DetachedJwsSigner(jwsKsPath, jwsKsPassword, jwsKeyAlias, jwsKeyPassword);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to init JWS signer", e);
    }
  }

  @RequestMapping("/{serviceName}/**")
  public Mono<ResponseEntity<String>> proxy(
      @PathVariable String serviceName,
      HttpServletRequest request,
      @RequestBody(required = false) String body) {

    RouteDefinition route = routeConfig.getRoutes().stream()
        .filter(r -> r.getName().equals(serviceName))
        .findFirst().orElse(null);

    if (route == null) return Mono.just(ResponseEntity.notFound().build());

    // Profile-specific validations (UAE OB)
    if ("UAE_OB".equalsIgnoreCase(route.getProfile())) {
      new UaeOpenBankingValidator().validate(request);
    }

    // OpenAPI request validation
    if (route.getOpenApiSpec()!=null){
      try {
        var api = openApiRegistry.get(route.getName());
        DefaultRequest req = new DefaultRequest.Builder(request.getRequestURI())
            .method(request.getMethod())
            .body(body)
            .build();
        OpenApiValidatorUtil.validateRequest(api, req);
      } catch (Exception e) {
        return Mono.just(ResponseEntity.badRequest().body("Request validation failed: " + e.getMessage()));
      }
    }

    WebClient client = this.webClient;
    if (signer != null && !"GET".equalsIgnoreCase(request.getMethod())) {
      client = this.webClient.mutate()
          .filter(OutboundSigningFilter.withDetachedJwsHeader(signer))
          .build();
    }

    String target = route.getTargetUrl();

    return client.method(HttpMethod.valueOf(request.getMethod()))
        .uri(target)
        .headers(h -> Collections.list(request.getHeaderNames()).forEach(n -> h.add(n, request.getHeader(n))))
        .attribute("proxiedBody", body == null ? "" : body)
        .bodyValue(body == null ? "" : body)
        .retrieve()
        .toEntity(String.class)
        .flatMap(resp -> {
          if (route.getOpenApiSpec()!=null){
            try{
              var api = openApiRegistry.get(route.getName());
              DefaultRequest req = new DefaultRequest.Builder(request.getRequestURI())
                  .method(request.getMethod()).build();
              DefaultResponse r = new DefaultResponse.Builder(resp.getStatusCode().value())
                  .body(resp.getBody()).build();
              OpenApiValidatorUtil.validateResponse(api, req, r);
            }catch(Exception e){
              return Mono.just(ResponseEntity.internalServerError().body("Response validation failed: " + e.getMessage()));
            }
          }
          return Mono.just(resp);
        });
  }
}
