package com.example.proxy.validation;

import org.openapi4j.operation.validator.model.Request;
import org.openapi4j.operation.validator.model.Response;
import org.openapi4j.operation.validator.validation.RequestValidator;
import org.openapi4j.operation.validator.validation.ResponseValidator;
import org.openapi4j.parser.model.v3.OpenApi3;

public class OpenApiValidatorUtil {
  public static void validateRequest(OpenApi3 api, Request req) throws Exception {
    new RequestValidator(api).validate(req);
  }
  public static void validateResponse(OpenApi3 api, Request req, Response resp) throws Exception {
    new ResponseValidator(api).validate(req, resp);
  }
}
