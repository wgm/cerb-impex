package com.webgroupmedia.cerb4.exporter.osTicket;

import com.cerb4.impex.Configuration;
import com.webgroupmedia.cerb4.exporter.osTicket.entities.Knowledgebase;
import com.webgroupmedia.cerb4.exporter.osTicket.entities.Ticket;
import com.webgroupmedia.cerb4.exporter.osTicket.entities.Worker;

public class Driver {

	public Driver() {
//		if(!checkSourceVersion()) {
//			System.err.println("The source doesn't appear to be a Cerberus Helpdesk 3.6 database. Aborting!");
//			System.exit(1);
//		}

		Boolean bExportTickets = new Boolean(Configuration.get("exportTickets", "false")); 
		Boolean bExportWorkers = new Boolean(Configuration.get("exportWorkers", "false"));
		Boolean bExportKb = new Boolean(Configuration.get("exportKb", "false")); 
		
		if(bExportWorkers)
			new Worker().export();
		
		if(bExportTickets)
			new Ticket().export();
		
		if(bExportKb)
			new Knowledgebase().export();		
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
