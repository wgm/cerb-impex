package com.cerb4.impex.exporters;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.cerb4.impex.Database;

public class Worker {
	public void export() {
		Connection conn = Database.getInstance();

		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT user_id, user_name, user_email, user_password, user_superuser FROM user ORDER BY user_id ASC");
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File("output/00-workers-" + String.format("%06d", iSubDir));
					outputDir.mkdirs();
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eWorker = doc.addElement("worker");
				doc.setXMLEncoding("ISO-8859-1");
				
				Integer iId = rs.getInt("user_id");
				String sName = rs.getString("user_name");
				String sEmail = rs.getString("user_email");
				String sPassword = rs.getString("user_password");
				Integer isSuperuser = rs.getInt("user_superuser");
				
				// Split name
				String sFirstName = sName.substring(0,sName.lastIndexOf(" "));
				String sLastName = sName.substring(sName.lastIndexOf(" "));
				
				eWorker.addElement("first_name").addText(sFirstName);
				eWorker.addElement("last_name").addText(sLastName);
				eWorker.addElement("email").addText(sEmail);
				eWorker.addElement("password").addText(sPassword);
				eWorker.addElement("is_superuser").addText(isSuperuser.toString());
				
				OutputFormat format = OutputFormat.createPrettyPrint();
				format.setEncoding("ISO-8859-1");
				format.setOmitEncoding(false);
				XMLWriter writer = new XMLWriter(new FileWriter(outputDir.getPath() + "/" + iId + ".xml"), format); 
				writer.write(doc);
				writer.close();
				
				iCount++;
			}
			
			rs.close();
			s.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
