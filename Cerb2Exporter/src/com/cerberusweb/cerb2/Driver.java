package com.cerberusweb.cerb2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.cerberusweb.cerb2.Database;
import com.cerberusweb.cerb2.entities.Ticket;
import com.cerberusweb.cerb2.entities.Worker;

public class Driver {
	public Driver() {
		// check db version; 2.7 required
		try {
			Connection conn = Database.getInstance();
			Statement s = conn.createStatement();
			s.execute("SELECT * FROM db_script_hash WHERE script_md5='fbdb155c25f4ba500442f8cfaf6bc9bc';"); // 2.7 clean hash
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
