package dev.fastgql.modules;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

class Annotations {
  @Qualifier
  @Retention(RUNTIME)
  @interface ServerPort {}

  @Qualifier
  @Retention(RUNTIME)
  @interface UpdateHandler {}
}
