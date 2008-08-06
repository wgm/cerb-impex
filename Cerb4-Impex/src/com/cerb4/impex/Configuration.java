package com.cerb4.impex;

import java.io.FileInputStream;
import java.util.Properties;

public class Configuration {
	private static Properties properties = new Properties();

	static {
		try {
			properties.load(new FileInputStream("impex.config"));
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private Configuration() {
	}
	
	public static String get(String key, String defaultValue) {
		return Configuration.properties.getProperty(key, defaultValue);
	}
}
