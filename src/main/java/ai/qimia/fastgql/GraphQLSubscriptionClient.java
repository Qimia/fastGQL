package ai.qimia.fastgql;


import io.vertx.core.Launcher;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.ApolloWSMessageType;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.WebSocket;

public class GraphQLSubscriptionClient extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand("run", GraphQLSubscriptionClient.class.getName());
  }

  @Override
  public void start() {
    HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setDefaultPort(8080));
    httpClient.webSocket("/graphql", websocketRes -> {
      if (websocketRes.succeeded()) {
        WebSocket webSocket = websocketRes.result();

        webSocket.handler(message -> System.out.println(message.toJsonObject().encodePrettily()));

        JsonObject request = new JsonObject()
          .put("id", "1")
          .put("type", ApolloWSMessageType.START.getText())
          .put("payload", new JsonObject()
            .put("query", "subscription { customers { id } }"));
        webSocket.write(new Buffer(request.toBuffer()));
      } else {
        websocketRes.cause().printStackTrace();
      }
    });
  }
}
