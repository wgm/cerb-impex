package com.cerb4.impex;

import java.sql.Connection;
import java.sql.DriverManager;

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
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		return Database.conn;
	}
}
