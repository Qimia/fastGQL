/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql.sql;

import dev.fastgql.common.TableWithAlias;
import java.util.Set;

/**
 * This interface is used by each object which can be a parent to {@link Component}.
 *
 * @author Kamil Bobrowski
 */
public interface ComponentParent {

  /**
   * Add component as a child.
   *
   * @param component component to be added
   */
  void addComponent(Component component);

  /**
   * Get true table name (as appears in database) when this component is serving as parent to other
   * components.
   *
   * @return table name
   */
  String tableNameWhenParent();

  /**
   * Get set of tables which are needed to be queried.
   *
   * @return set of table names
   */
  Set<TableWithAlias> getQueriedTables();
}
