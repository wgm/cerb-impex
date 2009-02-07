package com.cerb3.exporter.entities;

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

import com.cerb3.exporter.Database;
import com.cerb4.impex.Configuration;

public class Address {
	public void export() {
		Connection conn = Database.getInstance();
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));

		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT address_id, address_address, address_banned FROM address ORDER BY address_id ASC");
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File("output/addresses/" + String.format("%06d", iSubDir));
					outputDir.mkdirs();
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eAddress = doc.addElement("address");
				doc.setXMLEncoding(sExportEncoding);
				
				Integer iId = rs.getInt("address_id");
				String sEmail = rs.getString("address_address");
				Integer isBanned = rs.getInt("address_banned");
				
				eAddress.addElement("email").addText(sEmail);
				eAddress.addElement("is_banned").addText(isBanned.toString());
				
//				System.out.println(doc.asXML());
				
				OutputFormat format = OutputFormat.createPrettyPrint();
				format.setEncoding(sExportEncoding);
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
