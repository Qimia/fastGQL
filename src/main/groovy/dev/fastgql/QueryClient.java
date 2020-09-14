package dev.fastgql;

import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;

public class QueryClient extends AbstractVerticle {

  private static final String JWT_TOKEN =
      "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9."
          + "eyJpYXQiOjE1OTQ3MjA4MTF9."
          + "tCUr0CM_j6ZOiJakW2ODxvxxEJtNnmMWquSTGhJmK1aMu4aeAtHyGJlwpkmLo-"
          + "FBMWsU8elGLiTZ5xeGISS8tMWd4rfg03yyjSOjaDeNTZiMNYb0JZ06b8Sd6rGV"
          + "2FXapcgDqLlZvxfYCwL5mRIKSCZs_gmSAZ47y6RvKALA96bToB6LFJNA_vXQKW"
          + "xmFuAjuEBMs0RCGDY_VeJ9VIDUvtuW7h3sUR2Vs3XeJVtNtfwmR932UFV5ANhR"
          + "U0n_18G8i_VEtPxmGuv8Z2C-UnOaE5ryiMltXwRt15NDNy77hhzSW2xOGwnttq"
          + "xoHIixWiJuIi1Z0XPurvtf7oymIKRtBg";

  // private static final String QUERY = "query { addresses { id customers_on_address { id } } }";
  private static final String QUERY = "query { __meta { user } }";

  public static void main(String[] args) {
    Launcher.executeCommand("run", QueryClient.class.getName());
  }

  @Override
  public void start(Promise<Void> future) {
    WebClient.create(vertx)
        .post(8080, "localhost", "/v1/graphql")
        .bearerTokenAuthentication(JWT_TOKEN)
        .expect(ResponsePredicate.SC_OK)
        .expect(ResponsePredicate.JSON)
        .as(BodyCodec.jsonObject())
        .rxSendJsonObject(new JsonObject().put("query", QUERY))
        .subscribe(
            response -> {
              System.out.println(response.body());
              future.complete();
            },
            future::fail);
  }
}
