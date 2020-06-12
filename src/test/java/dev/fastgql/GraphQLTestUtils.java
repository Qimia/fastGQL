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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.testcontainers.containers.JdbcDatabaseContainer;

public class GraphQLTestUtils {

  public static void verifyQuerySimple(
      String directory, int port, Vertx vertx, VertxTestContext context) {
    String query = String.format("%s/query.graphql", directory);
    String expected = String.format("%s/expected.json", directory);
    verifyQuery(port, query, expected, vertx, context);
  }

  public static void verifySubscriptionSimple(
      String directory,
      int port,
      int delay,
      TimeUnit unit,
      JdbcDatabaseContainer<?> jdbcDatabaseContainer,
      Vertx vertx,
      VertxTestContext context) {
    System.out.println(String.format("Test: %s", directory));
    String query = String.format("%s/query.graphql", directory);
    List<String> expected =
        List.of(
            String.format("%s/expected-1.json", directory),
            String.format("%s/expected-2.json", directory));
    GraphQLTestUtils.verifySubscription(port, query, expected, vertx, context);
    DBTestUtils.executeSQLQueryFromResourceWithDelay(
        String.format("%s/query.sql", directory), delay, unit, jdbcDatabaseContainer, context);
  }

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

            AtomicJsonObject atomicJsonObject = new AtomicJsonObject();
            webSocket.handler(
                message -> {
                  System.out.println(message);
                  if (atomicJsonObject.checkIfSameAsLastObjectAndUpdate(message.toJsonObject())) {
                    return;
                  }
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

  static class AtomicJsonObject {
    private JsonObject json = new JsonObject();

    private synchronized boolean checkIfSameAsLastObjectAndUpdate(JsonObject newJson) {
      if (json.equals(newJson)) {
        return true;
      } else {
        this.json = newJson;
        return false;
      }
    }
  }
}
