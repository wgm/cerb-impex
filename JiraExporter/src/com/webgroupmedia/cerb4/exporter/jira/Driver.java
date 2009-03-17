package com.webgroupmedia.cerb4.exporter.jira;

import com.cerb4.impex.Configuration;
import com.webgroupmedia.cerb4.exporter.jira.entities.Ticket;



public class Driver {
	public Driver() {
		Boolean bExportTickets = new Boolean(Configuration.get("exportTickets", "false")); 
		
		if(bExportTickets)
			new Ticket().export();
	}
	

}
