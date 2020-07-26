package wtf.lua.rockblock.calendarserver;

import io.javalin.*;
import io.javalin.http.*;

/**
 * App provides the program entrypoint and some package information utilities
 */
public final class App {
	/**
	 * CalendarServer program entrypoint
	 * @param args Command-line arguments
	 */
	public static void main(String[] args) {
		var config = new Config(args);

		var downloader = new CalendarDownloader(
			config.formatUserAgent(getVersion()), config.downloadTimeout
		);

		var app = Javalin.create((appConfig) -> {
			appConfig.showJavalinBanner = false;
			appConfig.enableCorsForAllOrigins();
			if (config.debug) appConfig.enableDevLogging();
		});

		app.get("/:month", (ctx) -> {
			MonthExpression month;
			try { month = MonthExpression.parse(ctx.pathParam("month")); }
			catch (InvalidMonthExpressionException e) { throw new BadRequestResponse(e.getMessage()); }

			ctx.result(
				downloader.downloadCalendar(config.formatURL(month.toString()))
					.thenApply(CalendarInterpreter::interpret)
					.thenApply(ctx::json)
			);
		});

		app.start(config.apiPort);
	}

	/**
	 * Retrieve the program title
	 * @return Program title
	 */
	public static String getTitle() {
		var str = App.class.getPackage().getImplementationTitle();
		if (str != null) return str;
		else return "calendarserver";
	}
	/**
	 * Retrieve the program version
	 * @return Program version
	 */
	public static String getVersion() {
		var str = App.class.getPackage().getImplementationVersion();
		if (str != null) return str;
		else return "unknown";
	}
}
