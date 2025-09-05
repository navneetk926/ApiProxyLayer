package com.example.proxy.security;

import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;

@Configuration
public class MtlsWebClientConfig {

  @Value("${security.mtls.keystore.path}")
  private String keyStorePath;
  @Value("${security.mtls.keystore.password}")
  private String keyStorePassword;
  @Value("${security.mtls.truststore.path}")
  private String trustStorePath;
  @Value("${security.mtls.truststore.password}")
  private String trustStorePassword;

  @Bean
  public WebClient webClient() throws Exception {
    KeyStore ks = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType", "PKCS12"));
    try (FileInputStream fis = new FileInputStream(keyStorePath)) {
      ks.load(fis, keyStorePassword.toCharArray());
    }
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, keyStorePassword.toCharArray());

    KeyStore ts = KeyStore.getInstance(System.getProperty("javax.net.ssl.trustStoreType", "JKS"));
    try (FileInputStream fis = new FileInputStream(trustStorePath)) {
      ts.load(fis, trustStorePassword.toCharArray());
    }
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ts);

    var sslCtx = SslContextBuilder.forClient()
            .keyManager(kmf)
            .trustManager(tmf)
            .build();

    HttpClient httpClient = HttpClient.create().secure(spec -> spec.sslContext(sslCtx));
    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
  }
}
