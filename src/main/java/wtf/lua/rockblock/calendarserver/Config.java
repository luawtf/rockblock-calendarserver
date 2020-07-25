package wtf.lua.rockblock.calendarserver;

import org.apache.commons.cli.*;

enum LongName {
	HELP("help"),
	VERSION("version"),

	DEBUG("debug"),

	API_PORT("api-port"),
	API_CACHE("api-cache"),

	DOWNLOAD_TIMEOUT("download-timeout"),

	TEMPLATE_URL("template-url"),
	TEMPLATE_AGENT("template-agent");

	private final String value;
	private LongName(String value) { this.value = value; }
	public String toString() { return value; }
}

public final class Config {
	/** Enable debug logging output? */
	public boolean debug = false;

	/** Port number to listen on */
	public int apiPort = 2000;
	/** API cache time-to-live (in milliseconds) */
	public long apiCacheTTL = 3600000; // 30 minutes

	/** Timeout before retrying a download */
	public long downloadTimeout = 30000; // 30 seconds

	/** Template URL for downloading iCalendar data */
	public String templateURL = "https://westvancouverschools.ca/rockridge-secondary/events/$/?ical=1";
	/** Template URL for the UserAgent header when downloading iCalendar data */
	public String templateUserAgent = "RockBlock-CalendarServer/$ (https://github.com/luawtf/rockblock-calendarserver)";

	/**
	 * Generate a URL from the URL template
	 * @param val Value to format the template with
	 * @return Formatted URL string
	 */
	public String formatURL(String val) { return templateURL.replace("$", val); }
	/**
	 * Generate a UserAgent from the UserAgent template
	 * @param val Value to format the template with
	 * @return Formatted UserAgent string
	 */
	public String formatUserAgent(String val) { return templateUserAgent.replace("$", val); }

	/**
	 * Instantiate a Config with default values
	 */
	public Config() {}

	private void addOption(Options opts, String shortName, LongName longName, String description) {
		opts.addOption(new Option(shortName, longName.toString(), false, description));
	}
	private Options makeOptions() {
		Options opts = new Options();
		addOption(opts, "h", LongName.HELP, "display program help page");
		addOption(opts, "v", LongName.VERSION, "display program version");
		addOption(opts, "d", LongName.DEBUG, "enable debug logging output");
		addOption(opts, "p", LongName.API_PORT, "port number to listen on");
		addOption(opts, "c", LongName.API_CACHE, "api cache time-to-live (ms)");
		addOption(opts, "t", LongName.DOWNLOAD_TIMEOUT, "timeout before retrying a download (ms)");
		addOption(opts, "u", LongName.TEMPLATE_URL, "template URL for downloading iCalendar data");
		addOption(opts, "a", LongName.TEMPLATE_AGENT, "template for the UserAgent header");
		return opts;
	}

	private String getVersion() {
		return String.format("%s v%s", App.getTitle(), App.getVersion());
	}
	private void printVersion() {
		System.out.println(getVersion());
		System.exit(0);
	}
	private void printHelp(Options opts) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.setOptionComparator(null);
		formatter.printHelp(
			App.getTitle(),
			"Parses events from an iCalendar source and serves them via HTTP as JSON objects",
			opts,
			getVersion(),
			false
		);

		System.exit(0);
	}

	private boolean getBoolean(CommandLine cmd, LongName optionName, boolean defaultValue) {
		return cmd.hasOption(optionName.toString()) || defaultValue;
	}
	private String getString(CommandLine cmd, LongName optionName, String defaultValue) {
		if (!getBoolean(cmd, optionName, false)) return defaultValue;

		String value = cmd.getOptionValue(optionName.toString());
		if (value == null || value.isEmpty()) return defaultValue;

		return value;
	}
	private long getLong(CommandLine cmd, LongName optionName, long defaultValue) {
		String value = getString(cmd, optionName, "");
		try { return Long.parseLong(value); }
		catch (NumberFormatException e) { return defaultValue; }
	}
	private int getInt(CommandLine cmd, LongName optionName, int defaultValue) {
		return (int)getLong(cmd, optionName, (long)defaultValue);
	}

	/**
	 * Instantiate a Config from command-line arguments
	 * NOTE: This will System.exit() if the help text is requested or the parsing fails
	 */
	public Config(String[] args) {
		try {
			Options opts = makeOptions();
			CommandLineParser parser = new DefaultParser();
			CommandLine cmd = parser.parse(opts, args);

			if (getBoolean(cmd, LongName.HELP, false))
				printHelp(opts);
			if (getBoolean(cmd, LongName.VERSION, false))
				printVersion();

			debug			= getBoolean(cmd, LongName.DEBUG, debug);
			apiPort			= getInt(cmd, LongName.API_PORT, apiPort);
			apiCacheTTL		= getLong(cmd, LongName.API_CACHE, apiCacheTTL);
			downloadTimeout		= getLong(cmd, LongName.DOWNLOAD_TIMEOUT, downloadTimeout);
			templateURL		= getString(cmd, LongName.TEMPLATE_URL, templateURL);
			templateUserAgent	= getString(cmd, LongName.TEMPLATE_AGENT, templateUserAgent);
		} catch (ParseException e) {
			// Squelch it!
		}
	}
}
