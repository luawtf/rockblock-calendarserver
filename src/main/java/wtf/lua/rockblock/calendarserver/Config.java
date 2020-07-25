package wtf.lua.rockblock.calendarserver;

import org.apache.commons.cli.*;

enum LongName {
	HELP("help"),

	DEBUG("debug"),

	API_PORT("api-port"),
	API_CACHE("api-cache"),

	DOWNLOAD_RETRIES("download-retries"),
	DOWNLOAD_TIMEOUT("download-timeout"),
	DOWNLOAD_MAXSIZE("download-maxsize"),

	TEMPLATE_URL("template-url"),
	TEMPLATE_AGENT("template-agent");

	private final String value;
	private LongName(String value) { this.value = value; }
	public String toString() { return value; }
}

public final class Config {
	/** Enable debug logging output? */
	public boolean debug = false;

	/** Port number to listen on. */
	public int apiPort = 2000;
	/** API cache time-to-live (in milliseconds). */
	public long apiCacheTTL = 3600000; // 30 minutes

	/** Number of retries before considering a download as failed. */
	public int downloadRetries = 3;
	/** Timeout before retrying a download. */
	public long downloadTimeout = 30000; // 30 seconds
	/** Maximum size in bytes of a chunk of iCalendar data. */
	public long downloadMaxSize = 104857600; // 10 MiB

	/** Template URL for downloading iCalendar data. */
	public String templateURL = "https://westvancouverschools.ca/rockridge-secondary/events/$/?ical=1";
	/** Template URL for the UserAgent header when downloading iCalendar data. */
	public String templateUserAgent = "RockBlock-CalendarServer/$ (https://github.com/luawtf/rockblock-calendarserver)";

	/**
	 * Generate a URL from the URL template.
	 * @param val Value to format the template with.
	 * @return Formatted URL string.
	 */
	public String formatURL(String val) { return templateURL.replace("$", val); }
	/**
	 * Generate a UserAgent from the UserAgent template.
	 * @param val Value to format the template with.
	 * @return Formatted UserAgent string.
	 */
	public String formatUserAgent(String val) { return templateUserAgent.replace("$", val); }
	/**
	 * Generate a UserAgent from the UserAgent template using the current version string.
	 * @return Formatted UserAgent string.
	 */
	public String formatUserAgent() { return formatUserAgent(App.getVersion()); }

	/**
	 * Parse command line arguments and write their values to a config.
	 * @param config Config to modify.
	 * @param args Arguments to parse and apply to `config`.
	 */
	public static void applyArguments(Config config, String[] args) {
		try {
			Options options = makeOptions();
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(options, args);

			if (getBoolean(cmd, LongName.HELP, false)) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.setOptionComparator(null);
				formatter.printHelp(App.getTitle(), options);

				System.exit(0);
			}

			config.debug			= getBoolean(cmd, LongName.DEBUG, config.debug);
			config.apiPort			= getInt(cmd, LongName.API_PORT, config.apiPort);
			config.apiCacheTTL		= getLong(cmd, LongName.API_CACHE, config.apiCacheTTL);
			config.downloadRetries		= getInt(cmd, LongName.DOWNLOAD_RETRIES, config.downloadRetries);
			config.downloadTimeout		= getLong(cmd, LongName.DOWNLOAD_TIMEOUT, config.downloadTimeout);
			config.downloadMaxSize		= getLong(cmd, LongName.DOWNLOAD_MAXSIZE, config.downloadMaxSize);
			config.templateURL		= getString(cmd, LongName.TEMPLATE_URL, config.templateURL);
			config.templateUserAgent	= getString(cmd, LongName.TEMPLATE_AGENT, config.templateUserAgent);
		} catch (ParseException e) {
			// TODO: Log this exception
		}
	}

	private static boolean getBoolean(CommandLine cmd, LongName optionName, boolean defaultValue) {
		return cmd.hasOption(optionName.toString()) || defaultValue;
	}
	private static String getString(CommandLine cmd, LongName optionName, String defaultValue) {
		if (!getBoolean(cmd, optionName, false)) return defaultValue;

		String value = cmd.getOptionValue(optionName.toString());
		if (value == null || value.isEmpty()) return defaultValue;
		else return value;
	}
	private static int getInt(CommandLine cmd, LongName optionName, int defaultValue) {
		String value = getString(cmd, optionName, "");
		try { return Integer.parseInt(value); }
		catch (NumberFormatException e) { return defaultValue; }
	}
	private static long getLong(CommandLine cmd, LongName optionName, long defaultValue) {
		String value = getString(cmd, optionName, "");
		try { return Long.parseLong(value); }
		catch (NumberFormatException e) { return defaultValue; }
	}

	private static void addOption(Options options, String nameShort, LongName nameLong, String description) {
		options.addOption(new Option(nameShort, nameLong.toString(), false, description));
	}
	private static Options makeOptions() {
		Options options = new Options();

		addOption(options, "h", LongName.HELP, "display program help page");
		addOption(options, "d", LongName.DEBUG, "enable debug logging output");
		addOption(options, "p", LongName.API_PORT, "port number to listen on");
		addOption(options, "c", LongName.API_CACHE, "api cache time-to-live (ms)");
		addOption(options, "r", LongName.DOWNLOAD_RETRIES, "number of retries before considering a download failed");
		addOption(options, "t", LongName.DOWNLOAD_TIMEOUT, "timeout before retrying a download (ms)");
		addOption(options, "m", LongName.DOWNLOAD_MAXSIZE, "maximum size in bytes of a chunk of iCalendar data");
		addOption(options, "u", LongName.TEMPLATE_URL, "template URL for downloading iCalendar data");
		addOption(options, "a", LongName.TEMPLATE_AGENT, "template for the UserAgent header");

		return options;
	}
}
