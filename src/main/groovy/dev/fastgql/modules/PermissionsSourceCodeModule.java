package dev.fastgql.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.fastgql.security.Permissions;
import dev.fastgql.dsl.PermissionsSpec;

import java.util.function.Supplier;

public class PermissionsSourceCodeModule extends AbstractModule {
  @Provides
  Supplier<PermissionsSpec> providePermissionsSpecSupplier() {
    PermissionsSpec permissionsSpec = Permissions.getPermissionsSpec();
    return () -> permissionsSpec;
  }
}
