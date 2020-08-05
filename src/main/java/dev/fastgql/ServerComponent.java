package dev.fastgql;

import dagger.Component;
import dev.fastgql.modules.ServerModule;
import dev.fastgql.modules.VertxModule;
import io.reactivex.Single;
import io.vertx.reactivex.core.http.HttpServer;
import javax.inject.Singleton;

@Singleton
@Component(modules = {ServerModule.class, VertxModule.class})
public interface ServerComponent {

  Single<HttpServer> getHttpServer();

  @Component.Factory
  interface Factory {

    ServerComponent create(VertxModule vertxModule);
  }
}
