package com.example.proxy.config;

import java.util.List;

public class RouteDefinition {
  private String name;
  private String path;
  private String targetUrl;
  private List<String> requiredHeaders;
  private String openApiSpec;
  private String profile; // e.g., UAE_OB

  public String getName(){return name;}
  public void setName(String n){this.name=n;}
  public String getPath(){return path;}
  public void setPath(String p){this.path=p;}
  public String getTargetUrl(){return targetUrl;}
  public void setTargetUrl(String t){this.targetUrl=t;}
  public List<String> getRequiredHeaders(){return requiredHeaders;}
  public void setRequiredHeaders(List<String> r){this.requiredHeaders=r;}
  public String getOpenApiSpec(){return openApiSpec;}
  public void setOpenApiSpec(String s){this.openApiSpec=s;}
  public String getProfile(){return profile;}
  public void setProfile(String p){this.profile=p;}
}
