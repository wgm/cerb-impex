package com.webgroupmedia.cerb4.exporter.zendesk;

import com.cerb4.impex.Configuration;
import com.webgroupmedia.cerb4.exporter.zendesk.entities.Contact;
import com.webgroupmedia.cerb4.exporter.zendesk.entities.Org;
import com.webgroupmedia.cerb4.exporter.zendesk.entities.Ticket;
import com.webgroupmedia.cerb4.exporter.zendesk.entities.Worker;

public class Driver {
	public Driver() {
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
	
}
