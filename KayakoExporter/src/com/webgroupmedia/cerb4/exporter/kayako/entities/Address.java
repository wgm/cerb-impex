package com.webgroupmedia.cerb4.exporter.kayako.entities;

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

import com.cerb4.impex.Configuration;
import com.webgroupmedia.cerb4.exporter.kayako.Database;



public class Address {
	public void export() {
		Connection conn = Database.getInstance();
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT ticketemailid as id, email FROM swticketemails ORDER BY id ASC");
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
				
				Integer iId = rs.getInt("id");
				String sEmail = rs.getString("email");
				Integer isBanned = rs.getInt("is_banned");
				
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
