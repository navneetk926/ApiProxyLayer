package com.example.proxy.validation;

import jakarta.servlet.http.HttpServletRequest;

public class RequiredHeaderValidator implements RequestValidator {
  private final String header;
  public RequiredHeaderValidator(String h){ this.header = h; }
  @Override
  public void validate(HttpServletRequest request){
    if (request.getHeader(header)==null){
      throw new ValidationException("Missing required header: " + header);
    }
  }
}
