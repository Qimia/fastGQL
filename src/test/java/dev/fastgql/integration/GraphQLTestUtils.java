/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.ApolloWSMessageType;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.WebSocket;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;

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
    verifyQuerySimpleWithToken(directory, port, "", vertx, context);
  }

  /**
   * Wrapper function which verifies that query stored in {@param directory}/query.graphql resulted
   * in response stored in {@param directory}/expected.json. With JWT token provided.
   *
   * @param directory directory in resources
   * @param port port on which FastGQL is running
   * @param token JWT token
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  public static void verifyQuerySimpleWithToken(
      String directory, int port, String token, Vertx vertx, VertxTestContext context) {
    String query = String.format("%s/query.graphql", directory);
    String expected = String.format("%s/expected.json", directory);
    verifyQueryWithToken(port, query, expected, token, vertx, context);
  }

  /**
   * Verify single GraphQL query.
   *
   * @param port port on which FastGQL is running
   * @param inputResource resource which stores input query as GraphQL query
   * @param outputResource resource which stores expected response as JSON
   * @param token JWT token
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  private static void verifyQueryWithToken(
      int port,
      String inputResource,
      String outputResource,
      String token,
      Vertx vertx,
      VertxTestContext context) {
    String graphQLQuery = ResourcesTestUtils.readResource(inputResource, context);
    String expectedResponseString = ResourcesTestUtils.readResource(outputResource, context);

    JsonObject expectedResponse = new JsonObject(expectedResponseString);

    JsonObject request = new JsonObject().put("query", graphQLQuery);

    HttpRequest<Buffer> bufferHttpRequest =
        WebClient.create(vertx).post(port, "localhost", "/graphql");
    if (!token.isEmpty()) {
      bufferHttpRequest.bearerTokenAuthentication(token);
    }
    bufferHttpRequest
        .expect(ResponsePredicate.SC_OK)
        .expect(ResponsePredicate.JSON)
        .as(BodyCodec.jsonObject())
        .rxSendJsonObject(request)
        .subscribe(
            response ->
                context.verify(
                    () -> {
                      Assertions.assertEquals(expectedResponse, response.body());
                      context.completeNow();
                    }),
            context::failNow);
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
    verifySubscriptionWithToken(port, inputResource, outputResources, "", vertx, context);
  }

  /**
   * Verify that GraphQL subscription stored in {@param inputResource} resulted in a series of
   * responses stored in {@param outputResources}, with JWT token provided. Repeating responses are
   * ignored.
   *
   * @param port port on which FastGQL is running
   * @param inputResource resource which stores input query as GraphQL query
   * @param outputResources list of resources which store expected responses as JSONs
   * @param token JWT token
   * @param vertx vertx instance
   * @param context vertx test context
   */
  public static void verifySubscriptionWithToken(
      int port,
      String inputResource,
      List<String> outputResources,
      String token,
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

    WebSocketConnectOptions webSocketConnectOptions =
        new WebSocketConnectOptions().setHost("localhost").setPort(port).setURI("/graphql");
    if (!token.isEmpty()) {
      webSocketConnectOptions.addHeader(
          HttpHeaders.AUTHORIZATION.toString(), String.format("Bearer %s", token));
    }
    vertx
        .createHttpClient()
        .webSocket(
            webSocketConnectOptions,
            websocketRes -> {
              if (websocketRes.succeeded()) {
                WebSocket webSocket = websocketRes.result();

                AtomicJsonObject atomicJsonObject = new AtomicJsonObject();
                webSocket.handler(
                    message -> {
                      System.out.println(message);
                      if (atomicJsonObject.checkIfSameAsLastObjectAndUpdate(
                          message.toJsonObject())) {
                        return;
                      }
                      int currentResponse = currentResponseAtomic.getAndIncrement();
                      if (currentResponse < expectedResponses.size()) {
                        context.verify(
                            () ->
                                Assertions.assertEquals(
                                    expectedResponses.get(currentResponse),
                                    message.toJsonObject()));
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

  /**
   * Verify that the endpoint /graphql will return Unauthorized Status Code 401 if the token is not
   * not provided.
   *
   * @param port port on which FastGQL is running
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  public static void verifyUnauthorizedQuery(int port, Vertx vertx, VertxTestContext context) {
    WebClient.create(vertx)
        .post(port, "localhost", "/graphql")
        .expect(ResponsePredicate.SC_UNAUTHORIZED)
        .rxSend()
        .subscribe(
            response ->
                context.verify(
                    () -> {
                      Assertions.assertEquals(401, response.statusCode());
                      context.completeNow();
                    }),
            context::failNow);
  }

  /**
   * Verify that the websocket will be unauthorized if the token is not provided.
   *
   * @param port port on which FastGQL is running
   * @param vertx Vert.x instance
   * @param context Vert.x test context
   */
  public static void verifyUnauthorizedSubscription(
      int port, Vertx vertx, VertxTestContext context) {
    WebSocketConnectOptions wsOptions =
        new WebSocketConnectOptions().setHost("localhost").setPort(port).setURI("/graphql");
    vertx
        .createHttpClient()
        .webSocket(
            wsOptions,
            websocketRes -> {
              Assertions.assertTrue(websocketRes.failed());
              context.completeNow();
            });
  }
}
