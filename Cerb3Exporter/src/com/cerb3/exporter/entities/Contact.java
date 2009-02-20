package com.cerb3.exporter.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb3.exporter.Database;
import com.cerb3.exporter.Driver;
import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;

public class Contact {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT p.public_user_id, p.name_first, p.name_last, a.address_address AS email, p.`password` AS pass, p.phone_work AS phone, c.name AS org FROM public_gui_users p INNER JOIN address a ON (a.public_user_id=p.public_user_id) LEFT JOIN company c ON (c.id=p.company_id) ORDER BY a.public_user_id ASC");
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				Document doc = DocumentHelper.createDocument();
				Element eContact = doc.addElement("contact");
				doc.setXMLEncoding(sExportEncoding);
				
				Integer iId = rs.getInt("public_user_id");
				String sFirstName = Driver.fixMagicQuotes(rs.getString("name_first"));
				String sLastName = Driver.fixMagicQuotes(rs.getString("name_last"));
				String sEmail = Driver.fixMagicQuotes(rs.getString("email"));
				String sPassword = Driver.fixMagicQuotes(rs.getString("pass"));
				String sPhone = Driver.fixMagicQuotes(rs.getString("phone"));
				String sOrg = Driver.fixMagicQuotes(rs.getString("org"));
				
				eContact.addElement("first_name").addText(sFirstName);
				eContact.addElement("last_name").addText(sLastName);
				eContact.addElement("email").addText(sEmail);
				eContact.addElement("password").addText(sPassword);
				eContact.addElement("phone").addText(sPhone);
				eContact.addElement("organization").addText(sOrg);
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/03-contacts-" + String.format("%09d", iId));
					outputDir.mkdirs();
				}
				
				String sXmlFileName = outputDir.getPath() + "/" + String.format("%09d",iId) + ".xml";
				
				try {
					new XMLThread(doc, sXmlFileName).start();
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				iCount++;
			}
			
			rs.close();
			s.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
