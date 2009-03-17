package com.webgroupmedia.cerb4.exporter.jira;

import java.io.File;

import com.cerb4.impex.Configuration;

public class Main {

	public static void main(String[] args) {
		
		if(1==1) {
			System.out.println("Command to run Jira exporter is: java -jar Cerb4-ImpEx.jar jira.cfg");
			System.exit(0);
		}
		
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
		new Driver();
	}
	
}
