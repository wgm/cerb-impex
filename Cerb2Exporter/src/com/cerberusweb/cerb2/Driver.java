package com.cerberusweb.cerb2;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

import com.cerb4.impex.Configuration;
import com.cerberusweb.cerb2.entities.Contact;
import com.cerberusweb.cerb2.entities.Knowledgebase;
import com.cerberusweb.cerb2.entities.Org;
import com.cerberusweb.cerb2.entities.Ticket;
import com.cerberusweb.cerb2.entities.Worker;

public class Driver {
	public Driver() {
		if(!checkSourceVersion()) {
			System.err.println("The source doesn't appear to be a Cerberus Helpdesk 2.7 database. Aborting!");
			System.exit(1);
		}

		Boolean bExportTickets = new Boolean(Configuration.get("exportTickets", "false")); 
		Boolean bExportWorkers = new Boolean(Configuration.get("exportWorkers", "false")); 
		Boolean bExportOrgs = new Boolean(Configuration.get("exportOrgs", "false")); 
		Boolean bExportContacts = new Boolean(Configuration.get("exportContacts", "false")); 
		Boolean bExportKb = new Boolean(Configuration.get("exportKb", "false")); 
		
		if(bExportWorkers)
			new Worker().export();
		
		if(bExportTickets)
			new Ticket().export();
		
		if(bExportOrgs)
			new Org().export();
		
		if(bExportContacts)
			new Contact().export();
		
		if(bExportKb)
			new Knowledgebase().export();
		
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
	
	public static String fixMagicQuotes (String str) {
		Boolean bFixMagicQuotes = new Boolean(Configuration.get("fixMagicQuotes", "false")); 
		
		if(null == str)
			str = "";
		
		// Fix magic quotes from earlier versions of PHP apps
		if(bFixMagicQuotes) {
			str = str.replace("\\\\", "\\");
			str = str.replace("\\'", "'");
			str = str.replace("\\\"", "\"");
		}
		
		return str;
	}
}
