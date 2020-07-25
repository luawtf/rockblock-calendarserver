package wtf.lua.rockblock.calendarserver;

import io.javalin.Javalin;

/**
 * App provides the program entrypoint and some package information utilities
 */
public final class App {
	/**
	 * CalendarServer program entrypoint
	 * @param args Command-line arguments
	 */
	public static void main(String[] args) {
		Config config = new Config(args);

		Javalin app = Javalin.create((appConfig) -> {
			appConfig.showJavalinBanner = false;
			appConfig.enableCorsForAllOrigins();
			if (config.debug) appConfig.enableDevLogging();
		});

		app.get("/:month", (ctx) -> {
			ctx.json(config);
		});

		app.start(config.apiPort);
	}

	/**
	 * Retrieve the program title
	 * @return Program title
	 */
	public static String getTitle() { return App.class.getPackage().getImplementationTitle(); }
	/**
	 * Retrieve the program version
	 * @return Program version
	 */
	public static String getVersion() { return App.class.getPackage().getImplementationVersion(); }
}
