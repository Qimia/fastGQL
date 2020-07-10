package dev.fastgql;

import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;

public class QueryClient extends AbstractVerticle {
  public static void main(String[] args) {
    Launcher.executeCommand("run", QueryClient.class.getName());
  }

  @Override
  public void start(Promise<Void> future) {
    WebClient.create(vertx)
        .post(8080, "localhost", "/graphql")
        .expect(ResponsePredicate.SC_OK)
        .expect(ResponsePredicate.JSON)
        .as(BodyCodec.jsonObject())
        .rxSendJsonObject(
            new JsonObject().put("query", "query { addresses { id customers_on_address { id } } }"))
        .subscribe(
            response -> {
              System.out.println(response.body());
              future.complete();
            },
            future::fail);
  }
}
