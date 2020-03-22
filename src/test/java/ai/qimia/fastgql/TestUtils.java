package ai.qimia.fastgql;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.vertx.junit5.VertxTestContext;
import java.io.IOException;

public class TestUtils {

  @SuppressWarnings("UnstableApiUsage")
  public static String readResource(String name) throws IOException {
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
    return resource;
  }
}
