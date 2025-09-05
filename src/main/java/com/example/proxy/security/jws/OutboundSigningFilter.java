package com.example.proxy.security.jws;

import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class OutboundSigningFilter {

  public static ExchangeFilterFunction withDetachedJwsHeader(DetachedJwsSigner signer){
    return ExchangeFilterFunction.ofRequestProcessor(req -> {
      // Read body (if present) by buffering via attribute; for simplicity assume String body set earlier
      String body = req.attribute("proxiedBody").map(Object::toString).orElse("");
      try{
        String sig = signer.sign(body);
        ClientRequest newReq = ClientRequest.from(req)
                .header("x-jws-signature", sig)
                .build();
        return Mono.just(newReq);
      }catch(Exception e){
        return Mono.error(e);
      }
    });
  }
}
