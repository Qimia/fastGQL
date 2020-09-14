package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.dsl.PermissionsSpec;
import dev.fastgql.security.PermissionsStore;
import java.util.function.Supplier;

public class PermissionsAPIModule extends AbstractModule {
  @Provides
  Supplier<PermissionsSpec> providePermissionsSpecSupplier() {
    return PermissionsStore::getPermissionsSpec;
  }
}
