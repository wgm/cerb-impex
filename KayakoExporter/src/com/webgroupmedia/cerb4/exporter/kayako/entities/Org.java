package com.webgroupmedia.cerb4.exporter.kayako.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.kayako.Database;
import com.webgroupmedia.cerb4.exporter.kayako.Driver;

public class Org {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT companyname, businesstelephonenumber, businessfaxnumber, webpage "+
				"FROM swcontacts "
			);
			ResultSet rs = s.getResultSet();
	
			
			Set<String> companySet = new HashSet<String>();
			File outputDir = null;
			while(rs.next()) {
				
				Document doc = DocumentHelper.createDocument();
				Element eContact = doc.addElement("organization");
				doc.setXMLEncoding("ISO-8859-1");
				
				Integer iId = iCount;
				String sName = Driver.fixMagicQuotes(rs.getString("companyname"));
				
				if(companySet.contains(sName)) {
					continue;
				}
				companySet.add(sName);
				
				String sPhone = Driver.fixMagicQuotes(rs.getString("businesstelephonenumber"));
				String sFax = Driver.fixMagicQuotes(rs.getString("businessfaxnumber"));
				String sWebsite = Driver.fixMagicQuotes(rs.getString("webpage"));
				
				
				
				eContact.addElement("name").addText(sName);
				eContact.addElement("street").addText("");
				eContact.addElement("city").addText("");
				eContact.addElement("province").addText("");
				eContact.addElement("postal").addText("");
				eContact.addElement("country").addText("");
				eContact.addElement("phone").addText(sPhone);
				eContact.addElement("fax").addText(sFax);
				eContact.addElement("website").addText(sWebsite);
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/03-orgs-" + String.format("%09d", iId));
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
