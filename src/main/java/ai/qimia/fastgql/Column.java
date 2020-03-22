package ai.qimia.fastgql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Column<T> {

  private final Class<T> clazz;
  private final String referenceTable;
}
