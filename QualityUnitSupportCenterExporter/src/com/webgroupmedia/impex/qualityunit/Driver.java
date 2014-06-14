package com.webgroupmedia.impex.qualityunit;

import com.cerb4.impex.Configuration;
import com.webgroupmedia.impex.qualityunit.entities.Ticket;
import com.webgroupmedia.impex.qualityunit.entities.Worker;

public class Driver {
	
	public Driver() {
		Boolean bExportTickets = new Boolean(Configuration.get("exportTickets", "false")); 
		Boolean bExportWorkers = new Boolean(Configuration.get("exportWorkers", "false"));
		
		if(bExportWorkers)
			new Worker().export();
		
		if(bExportTickets)
			new Ticket().export();
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
