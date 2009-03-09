package com.webgroupmedia.cerb4.exporter.rt.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.rt.Database;
import com.webgroupmedia.cerb4.exporter.rt.Driver;


public class Contact {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		String cfgExportRTGroup = new String(Configuration.get("exportContactsRTGroup", "Unprivileged"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		System.out.println("contacts");
		
		try {
			
			Statement s = conn.createStatement();
			
			
			
			String sql = "SELECT u.id, u.RealName, u.EmailAddress, u.Password, u.Organization  " + 
					"FROM Users u " +
					"INNER JOIN GroupMembers gm on gm.MemberId = u.id " + 
					"INNER JOIN Groups g ON gm.GroupId = g.id " +
					"WHERE 1=1 ";
			
			String sqlLastCondition = "";
			boolean exportPrivileged =  (cfgExportRTGroup.equals("Unprivileged"));
			if(exportPrivileged) {
				sqlLastCondition = "AND g.Type = 'Unprivileged' ";
			}
			else {
				sqlLastCondition = "AND g.Domain='UserDefined' AND g.Name = '" + cfgExportRTGroup+"' ";
			}
			
			String sqlOrder = "ORDER BY u.id ";
			sql += sqlLastCondition + sqlOrder;
			
			s.execute(sql);
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				Document doc = DocumentHelper.createDocument();
				Element eWorker = doc.addElement("worker");
				doc.setXMLEncoding(sExportEncoding);
				
				Integer userId = rs.getInt("id");
				
				
				String sName = Driver.fixMagicQuotes(rs.getString("RealName"));
				String sEmail = Driver.fixMagicQuotes(rs.getString("EmailAddress"));
				String sPassword = Driver.fixMagicQuotes(rs.getString("Password"));
				
				String orgName = Driver.fixMagicQuotes(rs.getString("Organization"));
				
				String sFirstName, sLastName="";
				if(-1 != sName.indexOf(" ")) {
					sFirstName = sName.substring(0,sName.indexOf(" "));
					sLastName = sName.substring(sName.indexOf(" "));
				} else {
					sFirstName = sName;
				}
				
				if(0 == sEmail.length())
					continue;
				
				eWorker.addElement("first_name").addText(sFirstName);
				eWorker.addElement("last_name").addText(sLastName);
				eWorker.addElement("email").addText(sEmail);
				eWorker.addElement("password").addText(sPassword);
				eWorker.addElement("organization").addText(orgName);
				
				
				if(0 == iCount % 2000) {
					iSubDir++;
				
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/03-contacts-" + String.format("%09d", userId));
					outputDir.mkdirs();
				}
				
				String sXmlFileName = outputDir.getPath() + "/" + String.format("%06d", userId) + ".xml";

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
