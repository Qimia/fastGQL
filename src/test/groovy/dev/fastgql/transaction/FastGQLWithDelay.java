package dev.fastgql.transaction;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.fastgql.FastGQL;
import dev.fastgql.integration.NoGraphiQLModule;
import dev.fastgql.modules.*;
import java.util.concurrent.TimeUnit;

public class FastGQLWithDelay extends FastGQL {
  @Override
  protected Injector createInjector() {
    return Guice.createInjector(
        new VertxModule(vertx, config()),
        new ServerModule(),
        new GraphQLModule(),
        new DatabaseModule(),
        new SQLExecutorWithDelayModule(10, TimeUnit.SECONDS),
        new NoGraphiQLModule(),
        new PermissionsAPIModule());
  }
}
