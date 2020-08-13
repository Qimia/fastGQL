package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.modules.Annotations.ServerPort;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;

public class VertxModule extends AbstractModule {
  private final Vertx vertx;
  private final JsonObject config;

  public VertxModule(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
  }

  @Provides
  public Vertx provideVertx() {
    return vertx;
  }

  @Provides
  DatasourceConfig provideDatasourceConfig() {
    return DatasourceConfig.createWithJsonConfig(config.getJsonObject("datasource"));
  }

  @Provides
  DebeziumConfig provideDebeziumConfig() {
    return DebeziumConfig.createWithJsonConfig(config.getJsonObject("debezium"));
  }

  @Provides
  JWTAuth provideJWTAuth() {
    if (config.containsKey("auth")) {
      JsonObject optionsJson = config.getJsonObject("auth");
      JWTAuthOptions jwtAuthOptions =
          new JWTAuthOptions().addPubSecKey(new PubSecKeyOptions(optionsJson));
      return JWTAuth.create(vertx, jwtAuthOptions);
    }
    return null;
  }

  @Provides
  @ServerPort
  int port() {
    return config.getInteger("http.port", 8080);
  }
}
