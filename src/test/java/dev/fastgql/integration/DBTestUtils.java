/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.integration;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.sqlclient.Pool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test utils for executing SQL queries.
 *
 * @author Kamil Bobrowski
 */
public class DBTestUtils {

  private static final Logger log = LoggerFactory.getLogger(DBTestUtils.class);

  /**
   * Execute SQL queries stored in a resource.
   *
   * @param sqlResource name of resource
   * @param pool reactive pool of sql connections
   * @return single of RowSet
   */
  public static Single<RowSet<Row>> executeSQLQuery(String sqlResource, Pool pool) {
    return Single.fromCallable(() -> {
      System.out.println("*************** READING MOD RESOURCE");
      return ResourcesTestUtils.readResource(sqlResource);
    })
        .subscribeOn(Schedulers.io())
        .flatMap(
            sqlQuery ->
                pool.rxBegin()
                    .doOnSuccess(transaction -> log.info("[executing] {}", sqlQuery))
                    .flatMap(
                        transaction ->
                            transaction
                                .rxQuery(sqlQuery)
                                .flatMap(rows -> {
                                  System.out.println("******** ROWS AFFECTED: " + rows.rowCount());
                                  return transaction.rxCommit().doOnComplete(() -> log.info("****** transaction commited")).doOnError(error -> log.error(error.getMessage())).andThen(Single.just(rows));
                                })
                                .doOnSuccess(result -> log.info("[response] {}", sqlQuery)).doOnError(error -> log.error("****** ERRRROR " + error))));
  }
}
