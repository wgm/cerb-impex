package com.cerb6.exporter.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb6.exporter.Database;
import com.cerb6.exporter.Driver;
import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;

public class Knowledgebase { 	
	private class KbCategory {
		public Integer id = 0;
		public String name = "";
		public Integer parentId = 0;
		
		public KbCategory(Integer id, String name, Integer parentId) {
			this.id = id;
			this.name = name;
			this.parentId = parentId;
		}
	};
	
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "UTF-8"));
		String cfgKbRoot = Configuration.get("exportKbRoot", ""); 
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		HashMap<Integer,KbCategory> mapKbCategories = new HashMap<Integer,KbCategory>();
		
		try {
			// Load and cache the knowledgebase tree
			Statement stmtCategories = conn.createStatement();
			stmtCategories.execute(
					"SELECT id, name, parent_id FROM kb_category"
				);
			ResultSet rsCategories = stmtCategories.getResultSet();
			
			while(rsCategories.next()) {
				Integer iCatId = rsCategories.getInt("id");
				String sCatName= Driver.fixMagicQuotes(rsCategories.getString("name"));
				Integer iCatParentId = rsCategories.getInt("parent_id");
				
				KbCategory kbCat = new KbCategory(iCatId, sCatName, iCatParentId);
				mapKbCategories.put(iCatId, kbCat);
			}
			rsCategories.close();
			stmtCategories.close();
			
			Statement stmtArticles = conn.createStatement();
			stmtArticles.execute("SELECT id, title, views, content "+
					"FROM `kb_article` "+
					"ORDER BY id ASC"
				);
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
				
				Integer iId = rsArticles.getInt("id");
				String sTitle = Driver.fixMagicQuotes(rsArticles.getString("title"));
				String sContent = Driver.fixMagicQuotes(rsArticles.getString("content"));
				Integer iCreatedDate = new Integer((int)(new Date().getTime()/1000));

				if(0 == sTitle.length())
					continue;
				
				eKbArticle.addElement("title").addText(sTitle);
				eKbArticle.addElement("created_date").addText(iCreatedDate.toString());

				// Category
				
				Statement stmtKbCategories = conn.createStatement();
				stmtKbCategories.execute(String.format("SELECT kbc.kb_article_id, kbc.kb_category_id "+
						"FROM `kb_article_to_category` kbc "+
						"WHERE kbc.kb_article_id = %d",
						iId
					));
				ResultSet rsKbCategories = stmtKbCategories.getResultSet();
				
				while(rsKbCategories.next()) {
					Integer iCategoryId = rsKbCategories.getInt("kb_category_id");
					Element eKbArticleCategories = eKbArticle.addElement("categories");
					
					ArrayList<String> kbBreadcrumbs = new ArrayList<String>();
					if(mapKbCategories.containsKey(iCategoryId)) {
						Integer iSubParentId = iCategoryId;
						
						do {
							KbCategory currentCat = mapKbCategories.get(iSubParentId);
							kbBreadcrumbs.add(currentCat.name);
							iSubParentId = currentCat.parentId;
							
							// Chain is invalid
							if(!mapKbCategories.containsKey(iSubParentId))
								break;
							
						} while(iSubParentId > 0);
						
						// Put our list in order from the root node
						Collections.reverse(kbBreadcrumbs);
						
						// Does the exporter want a new root?
						if(cfgKbRoot.length() > 0) {
							eKbArticleCategories.addElement("category").setText(cfgKbRoot);
						}
						
						// Add breadcrumb
						for(String sSubKbName : kbBreadcrumbs) {
							eKbArticleCategories.addElement("category").setText(sSubKbName);
						}
					}
				}
				
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
