package com.cerb4.exporter.entities;

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

import com.cerb4.exporter.Database;



public class Address {
	public void export() {
		Connection conn = Database.getInstance();

		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT id, email, is_banned FROM address ORDER BY id ASC");
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
				doc.setXMLEncoding("ISO-8859-1");
				
				Integer iId = rs.getInt("id");
				String sEmail = rs.getString("email");
				Integer isBanned = rs.getInt("is_banned");
				
				eAddress.addElement("email").addText(sEmail);
				eAddress.addElement("is_banned").addText(isBanned.toString());
				
//				System.out.println(doc.asXML());
				
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
