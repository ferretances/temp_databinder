package net.databinder.util;

import java.util.Date;

import javax.persistence.criteria.Path;

public final class JPAUtil {

  private static final String PORCET = "%";

  public static Path<String> propertyStringExpressionToPath(final Path<?> e,
      final String property) {
    return e.get(property);
  }

  public static Path<Number> propertyNumberExpressionToPath(final Path<?> e,
      final String property) {
    return e.get(property);
  }

  public static Path<Boolean> propertyBooleanExpressionToPath(final Path<?> e,
      final String property) {
    return e.get(property);
  }

  public static Path<Date> propertyDateExpressionToPath(final Path<?> e,
      final String property) {
    return e.get(property);
  }

  public static String likePattern(final String value) {
    return new StringBuilder(PORCET).append(value).append(PORCET).toString();
  }
}
