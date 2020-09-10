package dev.fastgql.transaction;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.fastgql.FastGQL;
import dev.fastgql.modules.DatabaseModule;
import dev.fastgql.modules.GraphQLModule;
import dev.fastgql.modules.ServerModule;
import dev.fastgql.modules.VertxModule;
import java.util.concurrent.TimeUnit;

public class FastGQLWithDelay extends FastGQL {
  @Override
  protected Injector createInjector() {
    return Guice.createInjector(
        new VertxModule(vertx, config()),
        new ServerModule(),
        new GraphQLModule(),
        new DatabaseModule(),
        new SQLExecutorWithDelayModule(10, TimeUnit.SECONDS));
  }
}
