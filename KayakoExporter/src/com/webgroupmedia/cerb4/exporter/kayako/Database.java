package com.webgroupmedia.cerb4.exporter.kayako;

import java.sql.Connection;
import java.sql.DriverManager;

import com.cerb4.impex.Configuration;


public class Database {
	private static Connection conn = null;
	
	private Database() {
	}

	public static Connection getInstance() {
		
		if(null == Database.conn) {
			try {
				String sDbConnection = Configuration.get("dbConnection", "");
				String sDbUser = Configuration.get("dbUser", "");
				String sDbPassword = Configuration.get("dbPassword", "");
				
				if(0 == sDbConnection.length() || 0 == sDbUser.length()) {
					System.err.println("No database connection information was provided by the config file.");
				}
				
				Class.forName("com.mysql.jdbc.Driver").newInstance();

				Database.conn = DriverManager.getConnection(
						sDbConnection,
						sDbUser,
						sDbPassword
					);
				
				if(Database.conn.isClosed()) {
					System.err.println("Couldn't connect to the source database.");
				}
				
			} catch (Exception ex) {
				System.err.println(ex.getMessage());
				System.exit(1);
			}
		}
		
		return Database.conn;
	}
}
