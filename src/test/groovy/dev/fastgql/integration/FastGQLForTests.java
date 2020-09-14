package dev.fastgql.integration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.fastgql.FastGQL;
import dev.fastgql.modules.*;

public class FastGQLForTests extends FastGQL {

  @Override
  protected Injector createInjector() {
    return Guice.createInjector(
        new VertxModule(vertx, config()),
        new ServerModule(),
        new GraphQLModule(),
        new DatabaseModule(),
        new SQLExecutorModule(),
        new NoGraphiQLModule(),
        new PermissionsAPIModule());
  }
}
