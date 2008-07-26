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
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				// [TODO] Move to CLI
				Database.conn = DriverManager.getConnection("jdbc:mysql://xev.webgroupmedia.com:3306/cer_wgm_support", "jeff", "bubbleb00ble");
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		return Database.conn;
	}
}
