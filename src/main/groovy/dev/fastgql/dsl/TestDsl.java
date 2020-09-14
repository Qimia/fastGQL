package dev.fastgql.dsl;

import groovy.lang.*;
import java.io.File;
import java.io.IOException;
import org.codehaus.groovy.control.CompilerConfiguration;

public class TestDsl {

  public static void main(String[] args) throws IOException {
    CompilerConfiguration config = new CompilerConfiguration();
    config.setScriptBaseClass(PermissionsConfig.class.getName());
    GroovyShell shell = new GroovyShell(new Binding(), config);
    PermissionsSpec result =
        (PermissionsSpec) shell.evaluate(new File("src/main/resources/permissions.groovy"));
    OpSpec op = result.getRole("default").getTable("customers").getOp(OpType.select);
    System.out.println(op);
  }
}
