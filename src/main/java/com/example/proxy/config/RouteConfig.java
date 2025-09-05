package com.example.proxy.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "proxy")
public class RouteConfig {
  private List<RouteDefinition> routes;
  public List<RouteDefinition> getRoutes(){return routes;}
  public void setRoutes(List<RouteDefinition> r){this.routes=r;}
}
