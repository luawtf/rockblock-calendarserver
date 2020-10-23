package wtf.lua.rockblock.calendarserver;

import java.util.regex.Pattern;

/**
 * Month represents a YYYY-MM calendar date.
 * The specific representation used is described in ISO 8601's "Calendar dates" section:
 * <a href="https://wikipedia.org/wiki/ISO_8601#Calendar_dates">https://wikipedia.org/wiki/ISO_8601#Calendar_dates</a>
 *
 * <p>
 * Copyright (C) 2020 Lua MacDougall
 * <br/><br/>
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <br/><br/>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <br/><br/>
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see &lt;https://www.gnu.org/licenses/&gt;.
 * </p>
 *
 * @author Lua MacDougall &lt;luawhat@gmail.com&gt;
 */
public final class Month {
  /** Four-digit year piece, 0000 through 9999 (inclusive). */
  public final int year;
  /** Two-digit month piece, 01 through 12 (inclusive). */
  public final int month;

  /** YYYY-MM date expression string, EX: "2020-02". */
  public final String expression;

  /**
   * Create a new Month instance with a given year + month.
   * The date expression string will be automatically generated.
   * @param year  {@link Month#year}
   * @param month {@link Month#month}
   * @throws InvalidMonthException If either "year" or "month" parameters are out of range.
   */
  public Month(int year, int month) throws InvalidMonthException {
    if (year < 0000 || year > 9999)
      throw new InvalidMonthException(String.format("Invalid year %d", year));
    if (month < 01 || month > 12)
      throw new InvalidMonthException(String.format("Invalid month %d", month));

    this.year = year;
    this.month = month;

    expression = String.format("%04d-%02d", year, month);
  }

  /**
   * Convert this Month instance into a string.
   * @return YYYY-MM date expression string.
   */
  @Override
  public String toString() {
    return expression;
  }

  /**
   * Compare an object "obj" to this Month instance.
   * @return Is "obj" an instance of {@link Month} with {@link Month#year} and {@link Month#month} fields equal to our own?
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Month) {
      var objMonth = (Month)obj;
      return objMonth.year == year && objMonth.month == month;
    } else return false;
  }

  /**
   * Generate a hash code for this Month instance.
   * @return YYYYMM date integer representation.
   */
  @Override
  public int hashCode() {
    return year * 100 + month;
  }

  private static final Pattern expressionPattern = Pattern.compile("^(\\d\\d\\d\\d)-(\\d\\d)$");

  /**
   * Parse a YYYY-MM date expression string and create a new Month instance with the resulting year + month.
   * @param expression Expression string to parse.
   * @return Month instance.
   * @throws InvalidMonthException If "expression" is null/invalid or resulting year/month are out of range.
   */
  public static Month parse(String expression) throws InvalidMonthException {
    if (expression == null)
      throw new InvalidMonthException("Expression is null");

    var matcher = expressionPattern.matcher(expression);
    if (!matcher.matches())
      throw new InvalidMonthException("Expression does not match pattern");

    var yearGroup = matcher.group(1);
    var monthGroup = matcher.group(2);

    int year, month;
    try { year = Integer.parseInt(yearGroup); }
    catch (NumberFormatException e) { year = 0000; }
    try { month = Integer.parseInt(monthGroup); }
    catch (NumberFormatException e) { month = 01; }

    return new Month(year, month);
  }
}
