package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
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
  public JsonObject config() {
    return config;
  }
}
