/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration;

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

/**
 * Test utils for verifying GraphQL queries.
 *
 * @author Kamil Bobrowski
 */
public class GraphQLTestUtils {

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

  /**
   * Verify single GraphQL query.
   *
   * @param port port on which FastGQL is running
   * @param inputResource resource which stores input query as GraphQL query
   * @param outputResource resource which stores expected response as JSON
   * @param vertx vertx instance
   * @param context vertx test context
   */
  public static void verifyQuery(
      int port,
      String inputResource,
      String outputResource,
      Vertx vertx,
      VertxTestContext context) {
    String graphQLQuery = ResourcesTestUtils.readResource(inputResource, context);
    String expectedResponseString = ResourcesTestUtils.readResource(outputResource, context);

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

  /**
   * Wrapper function which verifies that query stored in {@param directory}/query.graphql resulted
   * in response stored in {@param directory}/expected.json.
   *
   * @param directory directory in resources
   * @param port port on which FastGQL is running
   * @param vertx vertx instance
   * @param context vertx test context
   */
  public static void verifyQuerySimple(
      String directory, int port, Vertx vertx, VertxTestContext context) {
    String query = String.format("%s/query.graphql", directory);
    String expected = String.format("%s/expected.json", directory);
    verifyQuery(port, query, expected, vertx, context);
  }

  /**
   * Verify that GraphQL subscription stored in {@param inputResource} resulted in a series of
   * responses stored in {@param outputResources}. Repeating responses are ignored.
   *
   * @param port port on which FastGQL is running
   * @param inputResource resource which stores input query as GraphQL query
   * @param outputResources list of resources which store expected responses as JSONs
   * @param vertx vertx instance
   * @param context vertx test context
   */
  public static void verifySubscription(
      int port,
      String inputResource,
      List<String> outputResources,
      Vertx vertx,
      VertxTestContext context) {
    Checkpoint checkpoints = context.checkpoint(outputResources.size());
    AtomicInteger currentResponseAtomic = new AtomicInteger(0);

    String graphQLQuery = ResourcesTestUtils.readResource(inputResource, context);
    List<JsonObject> expectedResponses =
        outputResources.stream()
            .map(name -> ResourcesTestUtils.readResource(name, context))
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
}
