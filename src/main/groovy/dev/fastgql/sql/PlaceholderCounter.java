package dev.fastgql.sql;

import dev.fastgql.db.DatasourceConfig;

import java.util.concurrent.atomic.AtomicInteger;

public class PlaceholderCounter {

  private final AtomicInteger atomicInteger = new AtomicInteger(1);
  private final DatasourceConfig.DBType dbType;

  PlaceholderCounter(DatasourceConfig.DBType dbType) {
    this.dbType = dbType;
  }

  String next() {
    switch (dbType) {
      case postgresql:
        return String.format("$%d", atomicInteger.getAndIncrement());
      case mysql:
        return "?";
      default:
        throw new RuntimeException("DB type not supported");
    }
  }
}