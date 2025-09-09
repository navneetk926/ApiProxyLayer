package com.example.genericapi.web;

import com.example.genericapi.config.AppProperties;
import com.example.genericapi.model.ApiEvent;
import com.example.genericapi.openap i.OpenApiSpecRegistry;
import com.example.genericapi.openapi.OpenApiValidationService;
import com.example.genericapi.repo.ApiEventRepository;
import com.example.genericapi.kafka.KafkaPublisher;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ValidationFilter implements Filter {
    private final AppProperties props;
    private final OpenApiSpecRegistry specRegistry;
    private final OpenApiValidationService validationService;
    private final ApiEventRepository repo;
    private final KafkaPublisher publisher;

    public ValidationFilter(AppProperties props,
                            OpenApiSpecRegistry specRegistry,
                            OpenApiValidationService validationService,
                            ApiEventRepository repo,
                            KafkaPublisher publisher) {
        this.props = props;
        this.specRegistry = specRegistry;
        this.validationService = validationService;
        this.repo = repo;
        this.publisher = publisher;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;

        // Wrap request to read body multiple times
        CachedBodyHttpServletRequest cachedReq = new CachedBodyHttpServletRequest(httpReq);
        String body = cachedReq.getBodyAsString();
        String path = cachedReq.getRequestURI();
        String method = cachedReq.getMethod();

        // load spec — for simplicity use first spec defined in config named "default" if present
        String specLocation = props.getOpenapiSpecs().getOrDefault("default", "classpath:openapi/api.yaml");
        OpenApi3 openApi = specRegistry.loadSpec("default", specLocation);

        Map<String, String> headers = Collections.list(cachedReq.getHeaderNames())
                .stream().collect(Collectors.toMap(h -> h, cachedReq::getHeader));

        // Validate request
        ValidationResults results = validationService.validate(openApi, path, method, headers, Collections.emptyMap(), Collections.emptyMap(), body);
        boolean isValid = !results.hasErrors();

        // Persist event
        ApiEvent ev = new ApiEvent();
        ev.setPath(path);
        ev.setMethod(method);
        ev.setHeaders(headers);
        ev.setBody(body);
        ev.setValid(isValid);
        ev.setValidationErrors(results.toString());
        repo.save(ev);

        if (!isValid) {
            httpResp.setStatus(HttpStatus.BAD_REQUEST.value());
            httpResp.getWriter().write("Validation failed:\n" + results.toString());
            return;
        }

        // publish to kafka
        String topic = props.getKafka().getTopic();
        if (topic != null && !topic.isBlank()) {
            // use event id as key (or any other key)
            publisher.publish(topic, ev.getId(), body);
        }

        // return 202 (accepted)
        httpResp.setStatus(HttpStatus.ACCEPTED.value());
        httpResp.getWriter().write("Accepted");
    }
}
