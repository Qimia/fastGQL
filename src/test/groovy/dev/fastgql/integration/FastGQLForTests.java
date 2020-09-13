package dev.fastgql.integration;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import dev.fastgql.FastGQL;
import dev.fastgql.modules.*;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;

import javax.inject.Singleton;

public class FastGQLForTests extends FastGQL {

  static class NoGraphiQLModule extends AbstractModule {
    @Provides
    @Singleton
    GraphiQLHandler provideGraphiQLHandler() {
      return null;
    }
  }

  @Override
  protected Injector createInjector() {
    return Guice.createInjector(
        new VertxModule(vertx, config()),
        new ServerModule(),
        new GraphQLModule(),
        new DatabaseModule(),
        new SQLExecutorModule(),
        new NoGraphiQLModule(),
        new PermissionsAPIModule()
    );
  }
}
