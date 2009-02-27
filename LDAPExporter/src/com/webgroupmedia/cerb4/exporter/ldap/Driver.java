package com.webgroupmedia.cerb4.exporter.ldap;


import com.cerb4.impex.Configuration;
import com.webgroupmedia.cerb4.exporter.ldap.entities.Contact;
import com.webgroupmedia.cerb4.exporter.ldap.entities.Worker;


public class Driver {

	public Driver() {
		Boolean bExportContacts = new Boolean(Configuration.get("exportContacts", "false")); 
		Boolean bExportWorkers = new Boolean(Configuration.get("exportWorkers", "false"));
		
		if(bExportWorkers)
			new Worker().export();

		if(bExportContacts && !bExportWorkers)
			new Contact().export();
		
	}
	
}
