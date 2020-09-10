package dev.fastgql;

import dev.fastgql.dsl.PermissionsConfig;
import dev.fastgql.dsl.PermissionsSpec;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.codehaus.groovy.control.CompilerConfiguration;

public class Config {

  public static PermissionsSpec permissions() {
    return Permissions.permissions();
  }

  public static PermissionsSpec permissions(Path filePath) throws IOException {
    CompilerConfiguration config = new CompilerConfiguration();
    config.setScriptBaseClass(PermissionsConfig.class.getName());
    GroovyShell shell = new GroovyShell(new Binding(), config);
    String fileString = Files.readString(filePath);
    return (PermissionsSpec) shell.evaluate(fileString);
  }
}
