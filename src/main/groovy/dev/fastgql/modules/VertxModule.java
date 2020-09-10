package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.db.DatasourceConfig;
import dev.fastgql.db.DebeziumConfig;
import dev.fastgql.modules.Annotations.ServerPort;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

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
  @ServerPort
  int port() {
    return config.getInteger("http.port", 8080);
  }
}
