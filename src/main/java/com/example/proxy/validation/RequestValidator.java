package com.example.proxy.validation;

import jakarta.servlet.http.HttpServletRequest;

public interface RequestValidator {
  void validate(HttpServletRequest request) throws ValidationException;
}
