package wtf.lua.rockblock.calendarserver;

import java.util.regex.*;

/**
 * InvalidMonthExpressionException represents an exception that occurs while parsing a month expression
 */
final class InvalidMonthExpressionException extends Exception {
	private static final long serialVersionUID = 1L;

	public InvalidMonthExpressionException(String message) {
		super(message);
	}
}

/**
 * MonthExpression provides an interface for parsing/sharing year-month "expressions"
 */
public final class MonthExpression {
	/** Year number (2016-2100) */
	public final int year;
	/** Month number (1-12) */
	public final int month;

	/** Expression as a string (ex 2020-08) */
	public final String value;

	/**
	 * Instantiate a new MonthExpression, arguments will be checked for validity
	 * @param year Year number
	 * @param month Month number
	 * @throws InvalidMonthExpressionException Year/month out-of-range
	 */
	public MonthExpression(int year, int month) throws InvalidMonthExpressionException {
		if (year < 2016 || year > 2100)
			throw new InvalidMonthExpressionException(String.format("Invalid year %d", year));
		if (month < 1 || month > 12)
			throw new InvalidMonthExpressionException(String.format("Invalid month %d", month));

		this.year = year;
		this.month = month;

		value = String.format("%04d-%02d", year, month);
	}

	private static final Pattern expressionPattern = Pattern.compile("^\\d\\d\\d\\d-\\d\\d$");

	/**
	 * Parse a string containing a MonthExpression into a MonthExpression instance
	 * @param expression Expression string to parse
	 * @return Resulting MonthExpression
	 * @throws InvalidMonthExpressionException Invalid expression or out-of-range
	 */
	public static MonthExpression parse(String expression) throws InvalidMonthExpressionException {
		if (expression == null)
			throw new InvalidMonthExpressionException("Expression is null");
		if (!expressionPattern.matcher(expression).matches())
			throw new InvalidMonthExpressionException("Expression does not match pattern");

		String[] args = expression.split("-");

		int year, month;
		try { year = Integer.parseInt(args[0]); }
		catch (NumberFormatException e) { year = 0; }
		try { month = Integer.parseInt(args[1]); }
		catch (NumberFormatException e) { month = 1; }

		return new MonthExpression(year, month);
	}
}
