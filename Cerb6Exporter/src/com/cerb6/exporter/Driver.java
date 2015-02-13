package com.cerb6.exporter;

import com.cerb6.exporter.entities.Contact;
import com.cerb6.exporter.entities.Knowledgebase;
import com.cerb6.exporter.entities.Org;
import com.cerb6.exporter.entities.Ticket;
import com.cerb6.exporter.entities.Worker;
import com.cerb4.impex.Configuration;

public class Driver {
	public Driver() {
//		if(!checkSourceVersion()) {
//			System.err.println("The source doesn't appear to be a Cerberus Helpdesk 3.6 database. Aborting!");
//			System.exit(1);
//		}

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
		
//		new Address().export();
	}
	
	// Check DB version; 3.6+ required
//	private boolean checkSourceVersion() {
//		HashSet<String> setPatches = new HashSet<String>();
//		
//		try {
//			Connection conn = Database.getInstance();
//			
//			// Make sure we have a 3.6 ahsh
//			Statement stmtPatches = conn.createStatement();
//			stmtPatches.execute("SELECT script_md5 FROM db_script_hash"); 
//			ResultSet rsPatches = stmtPatches.getResultSet();
//			
//			// Store our patches
//			while(rsPatches.next()) {
//				setPatches.add(rsPatches.getString("script_md5"));
//			}
//			rsPatches.close();
//			stmtPatches.close();
//
//		} catch (SQLException sqlE) {
//			sqlE.printStackTrace(); // [TODO] Logging
//		}
//
//		if(setPatches.contains("a646f96e8f3bd6f7ced1737d389b1239")) // 3.6 clean
//			return true;
//		
//		return false;
//	}
	
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
