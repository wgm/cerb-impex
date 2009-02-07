package com.webgroupmedia.cerb4.exporter.osTicket.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.osTicket.Database;
import com.webgroupmedia.cerb4.exporter.osTicket.Driver;

public class Knowledgebase {
	
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		String cfgKbRoot = Configuration.get("exportKbRoot", "ost_KB"); 
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		HashMap<Integer,String> mapKbCategories = new HashMap<Integer,String>();
		
		try {
			// Load and cache the knowledgebase tree
			Statement stmtDepartments = conn.createStatement();
			stmtDepartments.execute(
					"SELECT dept_id, dept_name FROM ost_department"
				);
			ResultSet rsDepartments = stmtDepartments.getResultSet();
			
			while(rsDepartments.next()) {
				Integer deptId = rsDepartments.getInt("dept_id");
				String deptName= Driver.fixMagicQuotes(rsDepartments.getString("dept_name"));

				mapKbCategories.put(deptId, deptName);
			}
			mapKbCategories.put(0, "All");
			rsDepartments.close();
			stmtDepartments.close();
			
			Statement stmtArticles = conn.createStatement();
			stmtArticles.execute("SELECT premade_id, title, answer, dept_id, created FROM ost_kb_premade ");
			ResultSet rsArticles = stmtArticles.getResultSet();
	
			File outputDir = null;
			
			while(rsArticles.next()) {
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/10-kbarticle-" + String.format("%06d", iSubDir));
					outputDir.mkdirs();
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eKbArticle = doc.addElement("kbarticle");
				doc.setXMLEncoding(sExportEncoding);
				
				Integer iId = rsArticles.getInt("premade_id");
				String sTitle = Driver.fixMagicQuotes(rsArticles.getString("title"));
				String sContent = Driver.fixMagicQuotes(rsArticles.getString("answer"));
				Long iCreatedDate = rsArticles.getDate("created").getTime()/1000;
				Integer deptId = rsArticles.getInt("dept_id");

				if(0 == sTitle.length())
					continue;
				
				eKbArticle.addElement("title").addText(sTitle);
				eKbArticle.addElement("created_date").addText(iCreatedDate.toString());

				// Category
				Element eKbArticleCategories = eKbArticle.addElement("categories");
				eKbArticleCategories.addElement("category").setText(cfgKbRoot);
				eKbArticleCategories.addElement("category").setText(mapKbCategories.get(deptId));
				

				Element eKbArticleContent = eKbArticle.addElement("content");
				eKbArticleContent.addAttribute("encoding", "base64");
				eKbArticleContent.setText(new String(Base64.encodeBase64(sContent.getBytes(sExportEncoding))));
				
				String sXmlFileName = outputDir.getPath() + "/" + String.format("%06d", iId) + ".xml";

				try {
					new XMLThread(doc, sXmlFileName).start();
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				iCount++;
			}
			
			rsArticles.close();
			stmtArticles.close();
			mapKbCategories.clear();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
