package dev.fastgql;

import dagger.Component;
import dev.fastgql.modules.DatabaseModule;
import dev.fastgql.modules.GraphQLModule;
import dev.fastgql.modules.ServerModule;
import dev.fastgql.modules.VertxModule;
import io.reactivex.Single;
import io.vertx.reactivex.core.http.HttpServer;
import javax.inject.Singleton;

@Singleton
@Component(
    modules = {VertxModule.class, ServerModule.class, GraphQLModule.class, DatabaseModule.class})
public interface ServerComponent {

  Single<HttpServer> getHttpServer();

  @Component.Factory
  interface Factory {

    ServerComponent create(VertxModule vertxModule);
  }
}
