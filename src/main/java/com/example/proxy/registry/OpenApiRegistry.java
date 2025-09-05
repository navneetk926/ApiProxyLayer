package com.example.proxy.registry;

import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.OpenApi3;
import org.springframework.stereotype.Component;
import com.example.proxy.config.RouteConfig;
import com.example.proxy.config.RouteDefinition;

import java.util.HashMap;
import java.util.Map;

@Component
public class OpenApiRegistry {
  private final Map<String, OpenApi3> cache = new HashMap<>();

  public OpenApiRegistry(RouteConfig cfg){
    if (cfg.getRoutes()!=null){
      for (RouteDefinition r: cfg.getRoutes()){
        if (r.getOpenApiSpec()!=null){
          try{
            OpenApi3 api = new OpenApi3Parser().parse(r.getOpenApiSpec(), true);
            cache.put(r.getName(), api);
          }catch(Exception e){
            throw new RuntimeException("Failed to parse OpenAPI for " + r.getName(), e);
          }
        }
      }
    }
  }

  public OpenApi3 get(String name){ return cache.get(name); }
}
