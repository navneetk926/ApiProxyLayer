package com.example.proxy.security.jws;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64URL;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

/**
 * Builds detached JWS signatures for OB-style request signing.
 */
public class DetachedJwsSigner {

  private final PrivateKey privateKey;
  private final X509Certificate cert;

  public DetachedJwsSigner(String keystorePath, String keystorePassword, String keyAlias, String keyPassword) throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12");
    try(FileInputStream fis = new FileInputStream(keystorePath)){
      ks.load(fis, keystorePassword.toCharArray());
    }
    this.privateKey = (PrivateKey) ks.getKey(keyAlias, keyPassword.toCharArray());
    this.cert = (X509Certificate) ks.getCertificate(keyAlias);
  }

  public String sign(String payload) throws Exception {
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.PS256) // commonly used in OB
            .criticalParams(Collections.singleton("b64"))
            .customParam("b64", false) // detached payload
            .x509CertChain(List.of(Base64URL.encode(cert.getEncoded()).toString()))
            .build();

    Payload p = new Payload(payload);
    JWSObject jwsObject = new JWSObject(header, p);
    JWSSigner signer = new RSASSASigner(privateKey);
    jwsObject.sign(signer);

    // return compact serialized with detached payload (..signature only)
    String[] parts = jwsObject.serialize(false).split("\.");
    return parts[0] + ".." + parts[2];
  }
}
