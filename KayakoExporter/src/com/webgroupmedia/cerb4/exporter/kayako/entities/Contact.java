package com.webgroupmedia.cerb4.exporter.kayako.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.kayako.Database;
import com.webgroupmedia.cerb4.exporter.kayako.Driver;

public class Contact {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			
			String sql = "SELECT u.userid, u.fullname, ue.email, u.userpassword as pass, "+
					"c.mobiletelephonenumber, c.othertelephonenumber, c.businesstelephonenumber, c.hometelephonenumber, " +
					"companyname " +
					"FROM swusers u  "+
					"INNER JOIN swuseremails ue ON u.userid = ue.userid " +
					"LEFT JOIN swcontacts c ON ue.email = c.email1address "
					;
			
			s.execute(sql);
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				Document doc = DocumentHelper.createDocument();
				Element eContact = doc.addElement("contact");
				doc.setXMLEncoding(sExportEncoding);
				
				Integer iId = rs.getInt("userid");
				String sFullName = Driver.fixMagicQuotes(rs.getString("fullname"));
				String sEmail = Driver.fixMagicQuotes(rs.getString("email"));
				String sPassword = Driver.fixMagicQuotes(rs.getString("pass"));
				
				
				String sPhone = Driver.fixMagicQuotes(rs.getString("mobiletelephonenumber"));
				if(sPhone.trim().length() == 0) {
					sPhone = Driver.fixMagicQuotes(rs.getString("othertelephonenumber"));
					if(sPhone.trim().length() == 0) {
						sPhone = Driver.fixMagicQuotes(rs.getString("businesstelephonenumber"));
						if(sPhone.trim().length() == 0) {
							sPhone = Driver.fixMagicQuotes(rs.getString("hometelephonenumber"));
						}
					}
				}
				
				
				String sOrg = Driver.fixMagicQuotes(rs.getString("companyname"));

				String sFirstName, sLastName="";
				if(-1 != sFullName.indexOf(" ")) {
					sFirstName = sFullName.substring(0,sFullName.indexOf(" "));
					sLastName = sFullName.substring(sFullName.indexOf(" "));
				} else {
					sFirstName = sFullName;
				}
				
				
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
