package ai.qimia.vertxtest;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GraphQLTestUtils {

  public static String readResource(String name) throws IOException {
    //noinspection UnstableApiUsage
    return Resources.toString(
      Resources.getResource(name),
      Charsets.UTF_8
    );
  }

  public static String readResource(String name, VertxTestContext context) {
    String resource = null;
    try {
      resource = readResource(name);
    } catch (IOException e) {
      context.failNow(e);
    }
    assertNotNull(resource);
    return resource;
  }


  public static void verifyQuery(int port, String inputResource, String outputResource,
                                 Vertx vertx, VertxTestContext context) {
    String graphQLQuery = readResource(inputResource, context);
    String expectedResponseString = readResource(outputResource, context);

    JsonObject expectedResponse = new JsonObject(expectedResponseString);

    JsonObject request = new JsonObject()
      .put("query", graphQLQuery);

    //noinspection ResultOfMethodCallIgnored
    WebClient
      .create(vertx)
      .post(port, "localhost", "/graphql")
      .expect(ResponsePredicate.SC_OK)
      .expect(ResponsePredicate.JSON)
      .as(BodyCodec.jsonObject())
      .rxSendJsonObject(request)
      .subscribe(
        response -> context.verify(() -> {
          assertEquals(expectedResponse, response.body());
          context.completeNow();
        }),
        context::failNow
      );
  }

  public static void verifyQuery(int port, int number, Vertx vertx, VertxTestContext context) {
    String inputResource = String.format("test-input-%d.graphql", number);
    String outputResource = String.format("test-output-%d.json", number);
    verifyQuery(port, inputResource, outputResource, vertx, context);
  }

  public static void verifySubscription(int port, String inputResource, List<String> outputResources,
                                        Vertx vertx, VertxTestContext context, Checkpoint checkpoints) {
    AtomicInteger currentResponseAtomic = new AtomicInteger(0);

    String graphQLQuery = readResource(inputResource, context);
    List<JsonObject> expectedResponses = outputResources
      .stream()
      .map(name -> readResource(name, context))
      .map(JsonObject::new)
      .collect(Collectors.toList());

    HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(port));
    httpClient.webSocket("/graphql", websocketRes -> {
      if (websocketRes.succeeded()) {
        WebSocket webSocket = websocketRes.result();

        webSocket.handler(message -> {
          int currentResponse = currentResponseAtomic.getAndIncrement();
          if (currentResponse < expectedResponses.size()) {
            assertEquals(expectedResponses.get(currentResponse), message.toJsonObject());
          }
          checkpoints.flag();
        });

        JsonObject request = new JsonObject()
          .put("id", "1")
          .put("type", ApolloWSMessageType.START.getText())
          .put("payload", new JsonObject()
            .put("query", graphQLQuery));
        webSocket.write(new Buffer(request.toBuffer()));
      } else {
        context.failNow(websocketRes.cause());
      }
    });
  }

  public static void verifySubscription(int port, int number, int numberOfEvents, Vertx vertx,
                                        VertxTestContext context, Checkpoint checkpoints) {
    String inputResource = String.format("test-subscription-input-%d.graphql", number);
    List<String> outputResources = IntStream.rangeClosed(1, numberOfEvents)
      .mapToObj(count -> String.format("test-subscription-output-%d-%d.json", number, count))
      .collect(Collectors.toList());
    verifySubscription(port, inputResource, outputResources, vertx, context, checkpoints);
  }
}
