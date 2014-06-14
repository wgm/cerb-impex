package com.webgroupmedia.impex.qualityunit.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.impex.qualityunit.Database;
import com.webgroupmedia.impex.qualityunit.Driver;

public class Worker {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "UTF-8"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT u.user_id, u.name, u.email, u.password, u.user_type FROM users u WHERE user_type IN ('a', 'g')");
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/00-workers-" + String.format("%06d", iSubDir));
					outputDir.mkdirs();
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eWorker = doc.addElement("worker");
				doc.setXMLEncoding(sExportEncoding);
				
				String sId = Driver.fixMagicQuotes(rs.getString("user_id"));
				String sName = Driver.fixMagicQuotes(rs.getString("name"));
				String sEmail = Driver.fixMagicQuotes(rs.getString("email"));
				String sPassword = rs.getString("password");
				String sUserType = Driver.fixMagicQuotes(rs.getString("user_type"));
				Integer isSuperuser = (sUserType.equals("a") ? 1 : 0);
				
				if(0 == sEmail.length())
					continue;

				String sFirstName = sName;
				String sLastName = "";
				
				// Split names on the first space
				if(sName.indexOf(" ") != -1) {
					sFirstName = sName.substring(0, sName.indexOf(" "));
					sLastName = sName.substring(sName.indexOf(" "));
				}
				
				eWorker.addElement("first_name").addText(sFirstName);
				eWorker.addElement("last_name").addText(sLastName);
				eWorker.addElement("email").addText(sEmail);
				eWorker.addElement("password").addText(sPassword);
				eWorker.addElement("is_superuser").addText(isSuperuser.toString());
				
				String sXmlFileName = outputDir.getPath() + "/" + String.format("%s", sId) + ".xml";

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
