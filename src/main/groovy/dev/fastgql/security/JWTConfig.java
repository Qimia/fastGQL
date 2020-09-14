package dev.fastgql.security;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;

public class JWTConfig {

  private final String algorithm;
  private final String publicKey;
  private final String secretKey;

  private JWTConfig() {
    this.algorithm = null;
    this.publicKey = null;
    this.secretKey = null;
  }

  private JWTConfig(String algorithm, String publicKey, String secretKey) {
    this.algorithm = algorithm;
    this.publicKey = publicKey;
    this.secretKey = secretKey;
  }

  public static JWTConfig createWithJsonConfig(JsonObject config) {
    if (config != null) {
      return new JWTConfig(
          config.getString("algorithm"),
          config.getString("publicKey"),
          config.getString("secretKey"));
    } else {
      return new JWTConfig();
    }
  }

  public JWTAuthHandler getJWTAuthHandler(Vertx vertx) {
    if (algorithm != null && publicKey != null) {
      JWTAuthOptions jwtAuthOptions =
          new JWTAuthOptions()
              .addPubSecKey(new PubSecKeyOptions().setAlgorithm(algorithm).setPublicKey(publicKey));
      JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);
      return JWTAuthHandler.create(jwtAuth);
    }
    return null;
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public String getPublicKey() {
    return publicKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  @Override
  public String toString() {
    return "JWTConfig{"
        + "algorithm='"
        + algorithm
        + '\''
        + ", publicKey='"
        + publicKey
        + '\''
        + ", secretKey='"
        + secretKey
        + '\''
        + '}';
  }
}
