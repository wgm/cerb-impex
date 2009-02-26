package com.webgroupmedia.cerb4.exporter.ldap;


import com.cerb4.impex.Configuration;
import com.webgroupmedia.cerb4.exporter.ldap.entities.Contact;


public class Driver {

	public Driver() {
		Boolean bExportContacts = new Boolean(Configuration.get("exportContacts", "false")); 
		
		if(bExportContacts)
			new Contact().export();
	}
	
}
