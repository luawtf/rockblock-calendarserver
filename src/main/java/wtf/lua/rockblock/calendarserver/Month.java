package wtf.lua.rockblock.calendarserver;

import java.util.regex.*;

/**
 * InvalidMonthException is thrown by Month's methods when an exceptional case is encountered
 */
final class InvalidMonthException extends Exception {
	private static final long serialVersionUID = -1191398746339823172L;

	/**
	 * Create a new InvalidMonthException instance with a message
	 * @param message Reason why the month is invalid
	 */
	public InvalidMonthException(String message) {
		super(message);
	}
}

/**
 * Month provides a container for a year-month date with validation and string parsing for month expressions (ex "2020-08")
 */
public final class Month {
	/** Year number 1970-2100 (ex 2020) */
	public final int year;
	/** Month number 1-12 (ex 8) */
	public final int month;

	/** Month expression string (ex "2020-08") */
	public final String value;

	/**
	 * Create a new Month instance with year and month integers
	 * @param year Year number (1920-2100)
	 * @param month Month number (1-12)
	 * @throws InvalidMonthException If `year` or `month` is out-of-bounds
	 */
	public Month(int year, int month) throws InvalidMonthException {
		if (year < 1970 || year > 2100)
			throw new InvalidMonthException(String.format("Invalid year %d", year));
		if (month < 1 || month > 12)
			throw new InvalidMonthException(String.format("Invalid month %d", month));

		this.year = year;
		this.month = month;

		value = String.format("%04d-%02d", year, month);
	}

	private static final Pattern expressionPattern = Pattern.compile("^\\d\\d\\d\\d-\\d\\d$");

	/**
	 * Parse a month expression string and generate a new Month instance
	 * @param expression Month expression string (ex "2020-08")
	 * @return New Month instance with parsed year/month values
	 * @throws InvalidMonthException If `expression` is invalid or out-of-bounds
	 */
	public static Month parse(String expression) throws InvalidMonthException {
		if (expression == null)
			throw new InvalidMonthException("Expression is null");
		if (!expressionPattern.matcher(expression).matches())
			throw new InvalidMonthException("Expression does not match pattern");

		String[] args = expression.split("-");

		int year, month;
		try { year = Integer.parseInt(args[0]); }
		catch (NumberFormatException e) { year = 0; }
		try { month = Integer.parseInt(args[1]); }
		catch (NumberFormatException e) { month = 1; }

		return new Month(year, month);
	}
}
