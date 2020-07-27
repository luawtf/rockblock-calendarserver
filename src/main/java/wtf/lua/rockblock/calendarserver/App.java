package wtf.lua.rockblock.calendarserver;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import org.slf4j.*;

import io.javalin.*;
import io.javalin.http.*;

/**
 * App provides the program's entrypoint and functions for getting the program's name and version
 */
public final class App {
	private static final Logger log = LoggerFactory.getLogger(App.class);

	/**
	 * Program entrypoint
	 * @param args Command-line argument array
	 */
	public static void main(String[] args) {
		log.info("Starting CalendarServer...");

		// Generate configuration from input arguments
		var config = new Config(args);

		// Create the Cache which provides all calendar downloading/parsing/serializing/caching functionality
		var cache = new Cache(config);

		// Create Javalin instance
		var app = Javalin.create((appConfig) -> {
			appConfig.showJavalinBanner = false;
			appConfig.enableCorsForAllOrigins();
			// appConfig.enableDevLogging();
		});

		// Add GET handler for months
		app.get("/:month", (ctx) -> {
			Month month;
			try { month = Month.parse(ctx.pathParam("month")); }
			catch (InvalidMonthException e) { throw new BadRequestResponse(e.getMessage()); }

			log.debug("Requested month {}", month.value);

			ctx.contentType("application/json");
			ctx.result(cache.request(month));
		});

		// Handle HTTP and other exceptions
		app.exception(HttpResponseException.class, (e, ctx) -> {
			var status = e.getStatus();
			var message = e.getMessage();
			if (message == null)
				message = e.toString();

			ctx.contentType("text/plain").status(status).result(message);

			var url = ctx.fullUrl();
			log.debug("HttpResponseException thrown while running handlers for \"{}\":", url, e);
		});
		app.exception(Exception.class, (e, ctx) -> {
			var message = e.getMessage();
			if (message == null)
				message = e.toString();

			ctx.status(500);
			ctx.contentType("text/plain");
			ctx.result(message);

			var url = ctx.fullUrl();
			log.error("Error thrown while running handlers for \"{}\":", url, e);
		});

		// Start listening!
		app.start(config.apiPort);
	}

	/**
	 * Get the program's title
	 * @return Program title string
	 */
	public static String getTitle() {
		var str = App.class.getPackage().getImplementationTitle();
		if (str != null) return str;

		return "calendarserver";
	}
	/**
	 * Get the program's version
	 * @return Program version string
	 */
	public static String getVersion() {
		var str = App.class.getPackage().getImplementationVersion();
		if (str != null) return str;

		return "unknown";
	}
}
