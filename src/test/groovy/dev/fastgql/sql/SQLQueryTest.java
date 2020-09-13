package dev.fastgql.sql;

import dev.fastgql.dsl.OpSpec;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class SQLQueryTest {

  @Nested
  class TableTest {
    @Test
    public void test() {
      OpSpec opSpec = new OpSpec();
      List<OpSpec.Check> checks = GetCheck.getCheck();
      // OpSpec.Check check = opSpec.new Check("id");
      // OpSpec.Condition condition = opSpec.new Condition(OpSpec.RelationalOperator.eq, 101);
      // check.setCondition(condition);
      Query.Table table = new Query.Table("test", "t0", Map.of());
      table.addChecks(checks);
      System.out.println(table.sqlCheckString());
    }
  }
}
