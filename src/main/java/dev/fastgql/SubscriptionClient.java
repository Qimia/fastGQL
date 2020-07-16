package dev.fastgql;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.graphql.ApolloWSMessageType;

public class SubscriptionClient extends AbstractVerticle {

  public static void main(String[] args) {
    Launcher.executeCommand("run", SubscriptionClient.class.getName());
  }

  @Override
  public void start() {
    String bearerToken =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9."
            + "eyJpYXQiOjE1OTQ3MjA4MTF9."
            + "tCUr0CM_j6ZOiJakW2ODxvxxEJtNnmMWquSTGhJmK1aMu4aeAtHyGJlwpkmLo-"
            + "FBMWsU8elGLiTZ5xeGISS8tMWd4rfg03yyjSOjaDeNTZiMNYb0JZ06b8Sd6rGV2F"
            + "XapcgDqLlZvxfYCwL5mRIKSCZs_gmSAZ47y6RvKALA96bToB6LFJNA_vXQKWxmFu"
            + "AjuEBMs0RCGDY_VeJ9VIDUvtuW7h3sUR2Vs3XeJVtNtfwmR932UFV5ANhRU0n_18"
            + "G8i_VEtPxmGuv8Z2C-UnOaE5ryiMltXwRt15NDNy77hhzSW2xOGwnttqxoHIixWi"
            + "JuIi1Z0XPurvtf7oymIKRtBg";
    WebSocketConnectOptions wsOptions =
        new WebSocketConnectOptions()
            .addHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + bearerToken)
            .setHost("localhost")
            .setPort(8080)
            .setURI("/graphql");

    vertx
        .createHttpClient()
        .webSocket(
            wsOptions,
            websocketRes -> {
              if (websocketRes.succeeded()) {
                WebSocket webSocket = websocketRes.result();

                webSocket.handler(
                    message -> System.out.println(message.toJsonObject().encodePrettily()));

                JsonObject request =
                    new JsonObject()
                        .put("id", "1")
                        .put("type", ApolloWSMessageType.START.getText())
                        .put(
                            "payload",
                            new JsonObject()
                                .put(
                                    "query",
                                    "subscription { addresses { id customers_on_address { id } } }"));
                webSocket.write(request.toBuffer());
              } else {
                websocketRes.cause().printStackTrace();
              }
            });
  }
}
