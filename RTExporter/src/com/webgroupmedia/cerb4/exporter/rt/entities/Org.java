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

public class Org {
	
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		String cfgExportRTGroup = new String(Configuration.get("exportOrgsRTGroup", "Unprivileged"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			
			Statement s = conn.createStatement();
			
			
			
			String sql = "SELECT DISTINCT u.Organization  " + 
					"FROM Users u " +
					"INNER JOIN GroupMembers gm on gm.MemberId = u.id " + 
					"INNER JOIN Groups g ON gm.GroupId = g.id " +
					"WHERE 1=1 ";
			String sqlLastCondition = "";
			boolean exportUnprivileged =  (cfgExportRTGroup.equals("Unprivileged"));
			if(exportUnprivileged) {
				sqlLastCondition = "AND g.Type = 'Unprivileged' ";
			}
			else {
				String groupsQueryStr = Driver.generateGroupsListSQL(cfgExportRTGroup);
				sqlLastCondition = "AND g.Domain='UserDefined' AND g.Name IN (" + groupsQueryStr+") ";
			}
			
			sql += sqlLastCondition;
			//System.out.println(sql);
			
			s.execute(sql);
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				String orgName = Driver.fixMagicQuotes(rs.getString("Organization"));
				if(orgName==null || orgName.trim().length()==0) {
					continue;
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eContact = doc.addElement("organization");
				doc.setXMLEncoding(sExportEncoding);

				
				
				eContact.addElement("name").addText(orgName);
				eContact.addElement("street").addText("");
				eContact.addElement("city").addText("");
				eContact.addElement("province").addText("");
				eContact.addElement("postal").addText("");
				eContact.addElement("country").addText("");
				eContact.addElement("phone").addText("");
				//eContact.addElement("fax").addText(sFax);
				eContact.addElement("website").addText("");
				
				
				if (0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;

					// Make the output subdirectory
					outputDir = new File(cfgOutputDir + "/02-orgs-"	+ String.format("%09d", iCount));
					outputDir.mkdirs();
				}

				String sXmlFileName = outputDir.getPath() + "/" + String.format("%09d", iCount) + ".xml";

				try {
					new XMLThread(doc, sXmlFileName).start();
				} catch (Exception e) {
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
	
//	public void export() {
//		Connection conn = Database.getInstance();
//		String cfgOutputDir = Configuration.get("outputDir", "output");
//		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
//		
//		Integer iCount = 0;
//		Integer iSubDir = 0;
//		
//		try {
//			Statement s = conn.createStatement();
//			s.execute("SELECT Organization, Address1, Address2, City, State, Zip, Country,  WorkPhone "+
//				"FROM Users "
//			);
//			ResultSet rs = s.getResultSet();
//	
//			
//			Set<String> companySet = new HashSet<String>();
//			File outputDir = null;
//			while(rs.next()) {
//				
//				Document doc = DocumentHelper.createDocument();
//				Element eContact = doc.addElement("organization");
//				doc.setXMLEncoding(sExportEncoding);
//				
//				Integer iId = iCount;
//				String sName = Driver.fixMagicQuotes(rs.getString("companyname"));
//				
//				if(companySet.contains(sName)) {
//					continue;
//				}
//				companySet.add(sName);
//				
//				String street1 = Driver.fixMagicQuotes(rs.getString("Address1"));
//				String street2 = Driver.fixMagicQuotes(rs.getString("Address2"));
//				String street = street1 + street2;
//				
//				String city = Driver.fixMagicQuotes(rs.getString("City"));
//				
//				String state = Driver.fixMagicQuotes(rs.getString("State"));
//				
//				String zip = Driver.fixMagicQuotes(rs.getString("Zip"));
//				
//				String country = Driver.fixMagicQuotes(rs.getString("Country"));
//				String workPhone = Driver.fixMagicQuotes(rs.getString("WorkPhone"));
//				
//
//				eContact.addElement("name").addText(sName);
//				eContact.addElement("street").addText(street);
//				eContact.addElement("city").addText(city);
//				eContact.addElement("province").addText(state);
//				eContact.addElement("postal").addText(zip);
//				eContact.addElement("country").addText(country);
//				eContact.addElement("phone").addText(workPhone);
//				//eContact.addElement("fax").addText(sFax);
//				eContact.addElement("website").addText("");
//				
//				if(0 == iCount % 2000 || 0 == iCount) {
//					iSubDir++;
//					
//					// Make the output subdirectory
//					outputDir = new File(cfgOutputDir+"/02-orgs-" + String.format("%09d", iId));
//					outputDir.mkdirs();
//				}
//				
//				String sXmlFileName = outputDir.getPath() + "/" + String.format("%09d",iId) + ".xml";
//				
//				try {
//					new XMLThread(doc, sXmlFileName).start();
//				} catch(Exception e) {
//					e.printStackTrace();
//				}
//				
//				iCount++;
//			}
//			
//			rs.close();
//			s.close();
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		
//	}
}
