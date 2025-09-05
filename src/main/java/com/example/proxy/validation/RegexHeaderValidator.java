package com.example.proxy.validation;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

public class RegexHeaderValidator implements RequestValidator {
  private final String header;
  private final Pattern pattern;
  public RegexHeaderValidator(String h, String regex){ this.header=h; this.pattern=Pattern.compile(regex); }
  @Override
  public void validate(HttpServletRequest request){
    String v = request.getHeader(header);
    if (v==null || !pattern.matcher(v).matches()){
      throw new ValidationException("Invalid header " + header);
    }
  }
}
