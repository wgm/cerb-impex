package com.cerb3.exporter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.cerb3.exporter.entities.Ticket;
import com.cerb3.exporter.entities.Worker;

public class Driver {
	public Driver() {
		// check db version; 3.6 required
		try {
			Connection conn = Database.getInstance();
			Statement s = conn.createStatement();
			s.execute("SELECT * FROM db_script_hash WHERE script_md5='a646f96e8f3bd6f7ced1737d389b1239';"); // 3.6 clean hash
			ResultSet rs = s.getResultSet();
			
			if (!rs.next()) {
				System.err.println("3.6 db script hash not found!  Exiting...");
				System.exit(1);
			}
			
		} catch (SQLException sqlE) {
			sqlE.printStackTrace();
		}
		
		new Worker().export();
//		new Address().export();
		new Ticket().export();
	}
}
