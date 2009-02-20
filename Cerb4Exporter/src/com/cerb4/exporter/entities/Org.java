package com.cerb4.exporter.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.exporter.Database;
import com.cerb4.exporter.Driver;
import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;

public class Org {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT id, name, street AS street_address, "+
				"city, province AS state, country, "+
				"postal AS zip, phone, fax, "+
				"website "+
				"FROM contact_org c "+
				"ORDER BY id ASC"
			);
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				Document doc = DocumentHelper.createDocument();
				Element eContact = doc.addElement("organization");
				doc.setXMLEncoding(sExportEncoding);
				
				Integer iId = rs.getInt("id");
				String sName = Driver.fixMagicQuotes(rs.getString("name"));
				String sSteetAddress = Driver.fixMagicQuotes(rs.getString("street_address"));
				String sCity = Driver.fixMagicQuotes(rs.getString("city"));
				String sState = Driver.fixMagicQuotes(rs.getString("state"));
				String sZip = Driver.fixMagicQuotes(rs.getString("zip"));
				String sCountry = Driver.fixMagicQuotes(rs.getString("country"));
				String sPhone = Driver.fixMagicQuotes(rs.getString("phone"));
				String sFax = Driver.fixMagicQuotes(rs.getString("fax"));
				String sWebsite = Driver.fixMagicQuotes(rs.getString("website"));
				
				eContact.addElement("name").addText(sName);
				eContact.addElement("street").addText(sSteetAddress);
				eContact.addElement("city").addText(sCity);
				eContact.addElement("province").addText(sState);
				eContact.addElement("postal").addText(sZip);
				eContact.addElement("country").addText(sCountry);
				eContact.addElement("phone").addText(sPhone);
				eContact.addElement("fax").addText(sFax);
				eContact.addElement("website").addText(sWebsite);
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/02-orgs-" + String.format("%09d", iId));
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
