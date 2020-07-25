package wtf.lua.rockblock.calendarserver;

public final class App {
	public static String getTitle() { return App.class.getPackage().getName(); }
	public static String getVersion() { return App.class.getPackage().getSpecificationVersion(); }

	public static void main(String[] args) {
		Config config = new Config();
		Config.applyArguments(config, args);

		System.out.println("Hello, world.");
	}
}
