package dev.fastgql.modules;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import javax.inject.Qualifier;

class Annotations {
  @Qualifier
  @Retention(RUNTIME)
  @interface ServerPort {}

  @Qualifier
  @Retention(RUNTIME)
  @interface UpdateHandler {}
}
