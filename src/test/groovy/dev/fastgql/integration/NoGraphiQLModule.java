package dev.fastgql.integration;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;
import javax.inject.Singleton;

public class NoGraphiQLModule extends AbstractModule {
  @Provides
  @Singleton
  GraphiQLHandler provideGraphiQLHandler() {
    return null;
  }
}
