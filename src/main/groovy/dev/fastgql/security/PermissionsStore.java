package dev.fastgql.security;

import dev.fastgql.dsl.PermissionsSpec;

public class PermissionsStore {
  private static PermissionsSpec permissionsSpec = null;

  public static synchronized void setPermissionsSpec(PermissionsSpec permissionsSpec) {
    System.out.println("SETTING: " + permissionsSpec);
    PermissionsStore.permissionsSpec = permissionsSpec;
  }

  public static synchronized PermissionsSpec getPermissionsSpec() {
    return permissionsSpec;
  }
}
