package com.cerberusweb.cerb2.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.cerberusweb.cerb2.Database;
import com.cerberusweb.cerb2.Driver;

public class Org {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT c.id, c.name, c.company_mailing_address AS street_address, "+
				"c.company_mailing_city AS city, c.company_mailing_state AS state, cn.country_name AS country, "+
				"c.company_mailing_zip AS zip, c.company_phone as phone, c.company_fax AS fax, "+
				"c.company_website as website, c.company_email as email "+
				"FROM company c "+
				"INNER JOIN country cn ON (cn.country_id=c.company_mailing_country_id) "+
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
