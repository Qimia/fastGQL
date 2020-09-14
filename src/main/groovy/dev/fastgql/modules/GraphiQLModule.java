package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.modules.Annotations.JwtToken;
import dev.fastgql.security.JWTConfig;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.handler.graphql.GraphiQLHandlerOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.web.handler.graphql.GraphiQLHandler;
import java.util.Map;
import javax.inject.Singleton;

public class GraphiQLModule extends AbstractModule {
  @Provides
  @JwtToken
  String provideJWTToken(Vertx vertx, JWTConfig jwtConfig) {
    JWTAuth jwtAuth =
        JWTAuth.create(
            vertx,
            new JWTAuthOptions()
                .addPubSecKey(
                    new PubSecKeyOptions()
                        .setAlgorithm(jwtConfig.getAlgorithm())
                        .setPublicKey(jwtConfig.getPublicKey())
                        .setSecretKey(jwtConfig.getSecretKey())));

    return jwtAuth.generateToken(
        JsonObject.mapFrom(Map.of("id", 101)),
        new JWTOptions().setAlgorithm(jwtConfig.getAlgorithm()));
  }

  @Provides
  @Singleton
  GraphiQLHandler provideGraphiQLHandler(@JwtToken String jwtToken) {

    Map<String, String> headers =
        Map.of(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + jwtToken);

    return GraphiQLHandler.create(
        new GraphiQLHandlerOptions()
            .setHeaders(headers)
            .setGraphQLUri("/v1/graphql")
            .setEnabled(true));
  }
}
