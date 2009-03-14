package com.webgroupmedia.cerb4.exporter.rt;

import com.cerb4.impex.Configuration;
import com.webgroupmedia.cerb4.exporter.rt.entities.Contact;
import com.webgroupmedia.cerb4.exporter.rt.entities.Org;
import com.webgroupmedia.cerb4.exporter.rt.entities.Ticket;
import com.webgroupmedia.cerb4.exporter.rt.entities.Worker;




public class Driver {
	public Driver() {
//		if(!checkSourceVersion()) {
//			System.err.println("The source doesn't appear to be a Kayako eSupport 3.0.0+ database. Aborting!");
//			System.exit(1);
//		}

		Boolean bExportTickets = new Boolean(Configuration.get("exportTickets", "false")); 
		Boolean bExportWorkers = new Boolean(Configuration.get("exportWorkers", "false"));
		Boolean bExportOrgs = new Boolean(Configuration.get("exportOrgs", "false")); 
		Boolean bExportContacts = new Boolean(Configuration.get("exportContacts", "false")); 
		
		if(bExportWorkers)
			new Worker().export();
		
		if(bExportTickets)
			new Ticket().export();
		
		if(bExportOrgs)
			new Org().export();
		
		if(bExportContacts)
			new Contact().export();
		
	}
	
//	// Check Kayako 3.x.x required
//	private boolean checkSourceVersion() {
//		System.out.println("Checking Kayako Version...");
//
//		boolean isAtLeastV3 = false;
//		try {
//			Connection conn = Database.getInstance();
//			
//			Statement stmtVersion = conn.createStatement();
//			stmtVersion.execute("SELECT data FROM swsettings WHERE vkey = 'version'"); 
//			ResultSet rsPatches = stmtVersion.getResultSet();
//			
//			while(rsPatches.next()) {
//				String versionStr = rsPatches.getString("data");
//				String[] versionArr = versionStr.split("[.]");
//				if(versionArr == null || versionArr.length == 0) {
//					isAtLeastV3 = false;
//					break;
//				}
//				System.out.println("Found Kayako version: " + versionArr[0]);
//				isAtLeastV3 = versionArr[0].equals("3"); 
//			}
//			rsPatches.close();
//			stmtVersion.close();
//
//		} catch (SQLException sqlE) {
//			sqlE.printStackTrace(); // [TODO] Logging
//		}
//
//		return isAtLeastV3;
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
	
	public static String generateGroupsListSQL(String groupListStr) {
		String[] groups = groupListStr.split(",");
		//List<String> groupsList = new ArrayList<String>();
		String groupsQueryStr = "";
		boolean firstTime = true;
		for (String group : groups) {
			if(!firstTime) {
				groupsQueryStr += ",";
			}
			groupsQueryStr += "'"+group.trim()+"'";
			firstTime = false;
		}
		return groupsQueryStr;
	}
}
