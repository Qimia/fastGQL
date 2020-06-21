/*
 * Copyright fastGQL Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.fastgql;

import java.lang.reflect.Field;

public class TestUtils {

  public static Field getField(Object object, String name) throws NoSuchFieldException {
    Field field = object.getClass().getDeclaredField(name);
    field.setAccessible(true);
    return field;
  }

  public static Object getFieldByReflection(Object object, String name)
      throws NoSuchFieldException, IllegalAccessException {
    return getField(object, name).get(object);
  }

  public static void setFieldByReflection(Object object, String name, Object mockObject)
      throws NoSuchFieldException, IllegalAccessException {
    getField(object, name).set(object, mockObject);
  }
}
