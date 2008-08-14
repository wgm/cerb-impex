package com.cerberusweb.cerb2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

import com.cerberusweb.cerb2.entities.Ticket;
import com.cerberusweb.cerb2.entities.Worker;

public class Driver {
	public Driver() {
		if(!checkSourceVersion()) {
			System.err.println("The source doesn't appear to be a Cerberus Helpdesk 2.7 database. Aborting!");
			System.exit(1);
		}
		
		new Worker().export();
//		new Address().export();
		new Ticket().export();
	}
	
	// Check DB version; 2.7 required
	private boolean checkSourceVersion() {
		HashSet<String> setPatches = new HashSet<String>();
		
		try {
			Connection conn = Database.getInstance();
			
			// Make sure we have a 2.7 patch and no 3.x hashes
			Statement stmtPatches = conn.createStatement();
			stmtPatches.execute("SELECT script_md5 FROM db_script_hash"); 
			ResultSet rsPatches = stmtPatches.getResultSet();
			
			// Store our patches
			while(rsPatches.next()) {
				setPatches.add(rsPatches.getString("script_md5"));
			}
			rsPatches.close();
			stmtPatches.close();

		} catch (SQLException sqlE) {
			sqlE.printStackTrace(); // [TODO] Logging
		}

		if(setPatches.contains("fbdb155c25f4ba500442f8cfaf6bc9bc") // 2.7 clean
				&& !setPatches.contains("2cb22a275bb6162852906ac6cf19f1a9")) // 3.0 clean
			return true;
		
		return false;
	}
}
