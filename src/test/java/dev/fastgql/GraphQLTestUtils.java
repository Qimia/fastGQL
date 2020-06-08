/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package dev.fastgql;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.ApolloWSMessageType;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.WebSocket;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class GraphQLTestUtils {

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public static void verifyQuery(
      int port,
      String inputResource,
      String outputResource,
      Vertx vertx,
      VertxTestContext context) {
    String graphQLQuery = TestUtils.readResource(inputResource, context);
    String expectedResponseString = TestUtils.readResource(outputResource, context);

    JsonObject expectedResponse = new JsonObject(expectedResponseString);

    JsonObject request = new JsonObject().put("query", graphQLQuery);

    WebClient.create(vertx)
        .post(port, "localhost", "/graphql")
        .expect(ResponsePredicate.SC_OK)
        .expect(ResponsePredicate.JSON)
        .as(BodyCodec.jsonObject())
        .rxSendJsonObject(request)
        .subscribe(
            response ->
                context.verify(
                    () -> {
                      assertEquals(expectedResponse, response.body());
                      context.completeNow();
                    }),
            context::failNow);
  }

  public static void verifySubscription(
      int port,
      String inputResource,
      List<String> outputResources,
      Vertx vertx,
      VertxTestContext context) {
    Checkpoint checkpoints = context.checkpoint(outputResources.size());
    AtomicInteger currentResponseAtomic = new AtomicInteger(0);

    String graphQLQuery = TestUtils.readResource(inputResource, context);
    List<JsonObject> expectedResponses =
        outputResources.stream()
            .map(name -> TestUtils.readResource(name, context))
            .map(JsonObject::new)
            .collect(Collectors.toList());

    HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(port));
    httpClient.webSocket(
        "/graphql",
        websocketRes -> {
          if (websocketRes.succeeded()) {
            WebSocket webSocket = websocketRes.result();

            webSocket.handler(
                message -> {
                  System.out.println(message);
                  int currentResponse = currentResponseAtomic.getAndIncrement();
                  if (currentResponse < expectedResponses.size()) {
                    context.verify(
                        () ->
                            assertEquals(
                                expectedResponses.get(currentResponse), message.toJsonObject()));
                  }
                  checkpoints.flag();
                });

            JsonObject request =
                new JsonObject()
                    .put("id", "1")
                    .put("type", ApolloWSMessageType.START.getText())
                    .put("payload", new JsonObject().put("query", graphQLQuery));
            webSocket.write(new Buffer(request.toBuffer()));
          } else {
            context.failNow(websocketRes.cause());
          }
        });
  }
}
