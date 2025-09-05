package com.example.proxy.validation;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Minimal UAE Open Banking style validator (sample).
 * Checks common FAPI-style headers used by many Open Banking regimes.
 * Adjust required headers/patterns to match your UAE profile spec.
 */
public class UaeOpenBankingValidator implements RequestValidator {

  @Override
  public void validate(HttpServletRequest request) {
    // x-fapi-interaction-id must be a UUID
    String interaction = request.getHeader("x-fapi-interaction-id");
    if (interaction == null || !interaction.matches("^[0-9a-fA-F-]{36}$")) {
      throw new ValidationException("x-fapi-interaction-id must be a UUID");
    }
    // customer ip header present
    if (request.getHeader("x-customer-ip-address")==null &&
        request.getHeader("x-fapi-customer-ip-address")==null){
      throw new ValidationException("Missing customer IP header");
    }
    // JWS signature presence for non-GET
    if (!"GET".equalsIgnoreCase(request.getMethod())){
      String sig = request.getHeader("x-jws-signature");
      if (sig==null || sig.isBlank()){
        throw new ValidationException("Missing x-jws-signature for non-GET request");
      }
    }
  }
}
