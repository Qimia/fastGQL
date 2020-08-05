package dev.fastgql.modules;

import dagger.Module;
import dagger.Provides;
import dev.fastgql.modules.Annotations.ServerPort;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import javax.inject.Singleton;

@Module
public class VertxModule {

  private final Vertx vertx;
  private final JsonObject config;

  public VertxModule(Vertx vertx, JsonObject config) {
    this.vertx = vertx;
    this.config = config;
  }

  @Provides
  @Singleton
  Vertx provideVertx() {
    return vertx;
  }

  @Provides
  @Singleton
  JsonObject provideConfig() {
    return config;
  }

  //  @Provides
  //  @Singleton
  //  DatasourceConfig provideDatasourceConfig() {
  //    return DatasourceConfig.createWithJsonConfig(config.getJsonObject("datasource"));
  //  }
  //
  //  @Provides
  //  @Singleton
  //  DebeziumConfig provideDebeziumConfig() {
  //    return DebeziumConfig.createWithJsonConfig(config.getJsonObject("debezium"));
  //  }

  @Provides
  @ServerPort
  @Singleton
  int port() {
    return config.getInteger("http.port", 8080);
  }
}
