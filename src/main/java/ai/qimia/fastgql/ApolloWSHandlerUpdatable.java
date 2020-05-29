package ai.qimia.fastgql;

import graphql.GraphQL;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.Handler;
import io.vertx.ext.web.handler.graphql.ApolloWSOptions;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.graphql.ApolloWSHandler;
import io.vertx.reactivex.ext.web.handler.graphql.ApolloWSMessage;
import org.dataloader.DataLoaderRegistry;
import java.util.function.Function;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class ApolloWSHandlerUpdatable implements Handler<RoutingContext> {
  private final Subject<ApolloWSHandler> apolloWSHandlerSubject = BehaviorSubject.<ApolloWSHandler>create().toSerialized();
  private final ApolloWSOptions options;
  private static final Function<ApolloWSMessage, Object> DEFAULT_QUERY_CONTEXT_FACTORY = rc -> rc;
  private static final Function<ApolloWSMessage, DataLoaderRegistry> DEFAULT_DATA_LOADER_REGISTRY_FACTORY = rc -> null;
  private Function<ApolloWSMessage, Object> queryContextFactory = DEFAULT_QUERY_CONTEXT_FACTORY;
  private Function<ApolloWSMessage, DataLoaderRegistry> dataLoaderRegistryFactory = DEFAULT_DATA_LOADER_REGISTRY_FACTORY;

  private ApolloWSHandlerUpdatable(ApolloWSOptions options) {
    this.options = options;
  }

  public static ApolloWSHandlerUpdatable create() {
    return new ApolloWSHandlerUpdatable(new ApolloWSOptions());
  }

  public static ApolloWSHandlerUpdatable create(ApolloWSOptions options) {
    return new ApolloWSHandlerUpdatable(options);
  }

  public synchronized void updateGraphQL(GraphQL graphQL) {
    apolloWSHandlerSubject.onNext(
      ApolloWSHandler
        .create(graphQL, options)
        .queryContext(queryContextFactory)
        .dataLoaderRegistry(dataLoaderRegistryFactory)
    );
  }

  public synchronized ApolloWSHandlerUpdatable queryContext(Function<ApolloWSMessage, Object> factory) {
    queryContextFactory = factory != null ? factory : DEFAULT_QUERY_CONTEXT_FACTORY;
    return this;
  }

  public synchronized ApolloWSHandlerUpdatable dataLoaderRegistry(Function<ApolloWSMessage, DataLoaderRegistry> factory) {
    dataLoaderRegistryFactory = factory != null ? factory : DEFAULT_DATA_LOADER_REGISTRY_FACTORY;
    return this;
  }

  @Override
  public void handle(RoutingContext ctx) {
    apolloWSHandlerSubject.take(1).subscribe(apolloWSHandler -> apolloWSHandler.handle(ctx));
  }
}
