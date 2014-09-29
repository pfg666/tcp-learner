package util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Config {

	/*
	 * a b c 5 a b d 3 a e f 6 a b -> a add e b t y 4 a e -> .a add b t
	 * 
	 * equal - else index*step + value:\n
	 * 
	 * a: b: c: 5
	 * 
	 * d: 3
	 * 
	 * e: f: 6
	 * 
	 * b: t: y: 4
	 */

	// protected String defaultConfigFilepath;
	protected Object obj = this;

	public void readConfig(String[] args, String configFileName) {
		// public void readConfig( String[] args, String defaultConfigFilepath)
		// {

		// this.defaultConfigFilepath = defaultConfigFilepath; // needed for
		// usage message
		/*
		 * String configFileName = fetchAlternativeConfigFileFromArgs(args); if
		 * (configFileName == null) configFileName = defaultConfigFilepath;
		 */

		if (configFileName == null) {
			System.err.println("Config file not defined");
			System.exit(-1);
		}
		handleConfigFile(configFileName);
	}

	@SuppressWarnings("rawtypes")
	public void handleConfigFile(String configFileName) {
		InputStream input = null;
		try {
			input = new FileInputStream(configFileName);
		} catch (FileNotFoundException e) {
			System.err.println("Cannot read config file : " + configFileName); // shouldn't
																				// be
																				// reach
																				// because
																				// above
																				// check
			System.exit(-1);
		}

		// read config fields from config yaml file
		Yaml yaml = new Yaml();
		Map config = (Map) yaml.load(input);

		Class thisClass = obj.getClass();
		Field[] fields = thisClass.getFields();

		for (Field field : fields) {
			String name = field.getName();
			String[] parts = name.split("_");

			Map map = config;
			for (int i = 0; i < (parts.length - 1); i++) {
				if (map == null)
					break;
				map = (Map) map.get(parts[i]);
			}
			if (map == null)
				continue; // next field
			try {
				Object value = map.get(parts[parts.length - 1]);
				if (value != null)
					field.set(obj, value);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Problem reading config file : "
						+ configFileName);
				System.exit(-1);
			}
		}
	}

	public void dumpAsYaml(Writer outstream, String indentStep) {

		PrintWriter stream = new PrintWriter(outstream);

		@SuppressWarnings("rawtypes")
		Class thisClass = obj.getClass();

		Field[] fields = thisClass.getFields();

		String[] prevParts = {};
		for (Field field : fields) {
			String name = field.getName();
			String[] parts = name.split("_");

			for (int i = 0; i < (parts.length - 1); i++) {
				if (prevParts.length - 1 > i && parts[i].equals(prevParts[i])) {
					continue;
				} else {
					stream.println(repeat(indentStep, i) + parts[i] + ":");
				}
			}
			Object value = null;
			try {
				value = field.get(obj);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String value_str = "";
			if (value != null) {
				if (value instanceof Collection<?>) {
					value_str = "[ ";
					List<String> l = (List<String>) value;
					for (String s : l) {
						value_str = value_str + "'" + s + "', "; // note: use
																	// single
																	// instead
																	// double
																	// quotes,
																	// because
																	// within
																	// double-quotes
																	// special
																	// characters
																	// are
																	// interpreted
																	// which we
																	// don't
																	// have in
																	// the
																	// Strings!
					}
					value_str = value_str + "]";

				} else if (value instanceof Map<?, ?>) {
					value_str = value.toString();
				} else {
					value_str = value.toString();
				}
				stream.println(repeat(indentStep, parts.length - 1)
						+ parts[parts.length - 1] + ": " + value_str);

			}
			prevParts = parts;
		}

	}

	public String repeat(String s, int times) {
		StringBuffer b = new StringBuffer();

		for (int i = 0; i < times; i++) {
			b.append(s);
		}
		return b.toString();
	}
}
