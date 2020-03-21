package ai.qimia.vertxtest;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.client.predicate.ResponsePredicate;
import io.vertx.reactivex.ext.web.codec.BodyCodec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class GraphQLServerTest {

  private static final int port = 8081;

  private static Network network = Network.newNetwork();
  private static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("debezium/postgres:11")
    .withNetwork(network)
    .withNetworkAliases("postgres");

  private static String readResouce(String name) throws IOException {
    //noinspection UnstableApiUsage
    return Resources.toString(
      Resources.getResource(name),
      Charsets.UTF_8
    );
  }

  private static void verifyResponse(String inputResource, String outputResource, Vertx vertx, VertxTestContext context) {
    String graphQLQuery;
    JsonObject expectedResponse;
    try {
      graphQLQuery = readResouce(inputResource);
      expectedResponse = new JsonObject(readResouce(outputResource));
    } catch (IOException e) {
      context.failNow(e);
      return;
    }

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

  private static void verifyResponse(int number, Vertx vertx, VertxTestContext context) {
    String inputResource = String.format("test-input-%d.graphql", number);
    String outputResource = String.format("test-output-%d.json", number);
    verifyResponse(inputResource, outputResource, vertx, context);
  }

  private static void initializeDB() throws SQLException, IOException {
    String initSQL = readResouce("init.sql");
    try (
      Connection connection = DriverManager.getConnection(
        postgresContainer.getJdbcUrl(),
        postgresContainer.getUsername(),
        postgresContainer.getPassword()
      );
      Statement statement = connection.createStatement()
    ) {
      statement.execute(initSQL);
    }
  }

  @BeforeAll
  public static void setUp(Vertx vertx, VertxTestContext context) {

    Startables.deepStart(Stream.of(postgresContainer)).join();

    try {
      initializeDB();
    } catch (SQLException | IOException e) {
      context.failNow(e);
      return;
    }

    int postgresPort = Integer.parseInt(
      StringUtils.substringBetween(postgresContainer.getJdbcUrl(), "localhost:", "/")
    );

    DatasourceConfig datasourceConfig = new DatasourceConfig();
    datasourceConfig.setHost("localhost");
    datasourceConfig.setPort(postgresPort);
    datasourceConfig.setDb(postgresContainer.getDatabaseName());
    datasourceConfig.setUsername(postgresContainer.getUsername());
    datasourceConfig.setPassword(postgresContainer.getPassword());
    JsonObject config = new JsonObject()
      .put("http.port", port)
      .put("datasource", JsonObject.mapFrom(datasourceConfig));
    DeploymentOptions options = new DeploymentOptions()
      .setConfig(config);
    vertx.deployVerticle(new GraphQLServer(), options, context.completing());
  }

  @AfterAll
  public static void tearDown(VertxTestContext context) {
    postgresContainer.close();
    network.close();
    context.completeNow();
  }

  @Test
  public void shouldReturnCorrectResponseForQuery1(Vertx vertx, VertxTestContext context) {
    verifyResponse(1, vertx, context);
  }

}
