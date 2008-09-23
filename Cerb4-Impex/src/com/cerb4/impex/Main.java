package com.cerb4.impex;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

public class Main {

	private static URLClassLoader cl;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String argConfigFile = "<null>";

		String javaVersion = System.getProperty("java.version");
		String[] versionSplit = javaVersion.split("\\.");
		if(versionSplit.length < 2) {
			System.err.println("Java version invalid (found: "+javaVersion+").  You must have Java 5 (1.1.5) or higher.   Export has been aborted.");
			System.exit(1);
		}

		int majorVersion = Integer.parseInt(versionSplit[0]);
		int minorVersion = Integer.parseInt(versionSplit[1]);
		
		if(majorVersion < 1 ||  (majorVersion == 1 && minorVersion < 5)) {
			System.err.println("Your version of Java ("+javaVersion+") is too old.  You must have Java 5 (1.1.5) or higher.  Export has been aborted.");
			System.exit(1);
		}

		if(1 != args.length) {
			System.err.println("Syntax: C4ImpEx.jar <config file>");
			System.exit(1);
		} else {
			argConfigFile = args[0];
		}
		
		if(!new File(argConfigFile).exists()) {
			System.err.println("Config file '" + argConfigFile + "' doesn't exist.");
			System.exit(1);
		} else {
			Configuration.loadConfigFile(argConfigFile);
		}
		
		Main.loadDrivers();

		String cfgExportDriver = Configuration.get("exportDriver", "");

		try {
			Class<?> driverClassManifest = cl.loadClass(cfgExportDriver);
//			System.out.println("Loading '" + driverClassManifest.getName());
			Constructor<?> constr = (Constructor<?>) driverClassManifest.getConstructor(new Class[] {});
			constr.newInstance(); // new Object[] {}

		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		
	}

	private static void loadDrivers() {
		ArrayList<URL> urls = new ArrayList<URL>();

		// Load drivers from disk
		File dir = new File("drivers");
		File[] driverFiles = dir.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".jar");
				}
			});
		
		for(File driverFile : driverFiles) {
			try {
				urls.add(driverFile.toURI().toURL());
//				System.out.println("Adding " + driverFile.toURI().toURL());
			} catch(Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		cl = new URLClassLoader((URL[]) urls.toArray(new URL[urls.size()]));
	}
	
}
