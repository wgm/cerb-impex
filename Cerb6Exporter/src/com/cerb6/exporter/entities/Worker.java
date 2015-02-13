package com.cerb6.exporter.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb6.exporter.Database;
import com.cerb6.exporter.Driver;
import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;

public class Worker {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "UTF-8"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT id, first_name, last_name, title, email, is_superuser FROM worker ORDER BY id ASC");
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
				
				Integer iId = rs.getInt("id");
				String sFirstName = Driver.fixMagicQuotes(rs.getString("first_name"));
				String sLastName = Driver.fixMagicQuotes(rs.getString("last_name"));
				String sEmail = Driver.fixMagicQuotes(rs.getString("email"));
				String sTitle = Driver.fixMagicQuotes(rs.getString("title"));
				//String sPassword = rs.getString("pass");
				Integer isSuperuser = rs.getInt("is_superuser");
				
				if(0 == sEmail.length())
					continue;
				
				eWorker.addElement("first_name").addText(sFirstName);
				eWorker.addElement("last_name").addText(sLastName);
				eWorker.addElement("email").addText(sEmail);
				eWorker.addElement("title").addText(sTitle);
				eWorker.addElement("is_superuser").addText(isSuperuser.toString());
				
				String sXmlFileName = outputDir.getPath() + "/" + String.format("%06d", iId) + ".xml";

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
