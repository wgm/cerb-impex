package com.cerberusweb.cerb2.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.cerberusweb.cerb2.Database;
import com.cerberusweb.cerb2.Driver;

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
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		String cfgKbRoot = Configuration.get("exportKbRoot", ""); 
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		HashMap<Integer,KbCategory> mapKbCategories = new HashMap<Integer,KbCategory>();
		
		try {
			// Load and cache the knowledgebase tree
			Statement stmtCategories = conn.createStatement();
			stmtCategories.execute(
					"SELECT kb_category_id, kb_category_name, kb_category_parent_id FROM knowledgebase_categories"
				);
			ResultSet rsCategories = stmtCategories.getResultSet();
			
			while(rsCategories.next()) {
				Integer iCatId = rsCategories.getInt("kb_category_id");
				String sCatName= Driver.fixMagicQuotes(rsCategories.getString("kb_category_name"));
				Integer iCatParentId = rsCategories.getInt("kb_category_parent_id");
				
				KbCategory kbCat = new KbCategory(iCatId, sCatName, iCatParentId);
				mapKbCategories.put(iCatId, kbCat);
			}
			rsCategories.close();
			stmtCategories.close();
			
			Statement stmtArticles = conn.createStatement();
			stmtArticles.execute("SELECT kb.kb_id, unix_timestamp(kb.kb_entry_date) as kb_created_date, kb.kb_keywords, kb.kb_public_views, "+
					"kb.kb_category_id, kb.kb_public, kbp.kb_problem_summary, kbp.kb_problem_text, kbp.kb_problem_text_is_html, "+
					"kbs.kb_solution_text, kbs.kb_solution_text_is_html "+
					"FROM `knowledgebase` kb "+
					"inner join `knowledgebase_problem` kbp ON (kbp.kb_id=kb.kb_id) "+
					"inner join `knowledgebase_solution` kbs ON (kbs.kb_id=kb.kb_id) "+
					"ORDER BY kb.kb_id ASC"
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
				
				Integer iId = rsArticles.getInt("kb_id");
				String sTitle = Driver.fixMagicQuotes(rsArticles.getString("kb_problem_summary"));
				Integer iCreatedDate = rsArticles.getInt("kb_created_date");
				Integer iCategoryId = rsArticles.getInt("kb_category_id");
				String sKeywords = Driver.fixMagicQuotes(rsArticles.getString("kb_keywords"));
				String sProblemText = Driver.fixMagicQuotes(rsArticles.getString("kb_problem_text"));
				Integer iProblemTextIsHTML = rsArticles.getInt("kb_problem_text_is_html");
				String sSolutionText = Driver.fixMagicQuotes(rsArticles.getString("kb_solution_text"));
				Integer iSolutionTextIsHTML = rsArticles.getInt("kb_solution_text_is_html");

				if(0 == sTitle.length())
					continue;
				
				eKbArticle.addElement("title").addText(sTitle);
				eKbArticle.addElement("created_date").addText(iCreatedDate.toString());
				Element eKbArticleCategories = eKbArticle.addElement("categories");

				// Category
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
				
				// Build the article text
				StringBuilder sContent = new StringBuilder();
				
				sContent.append("<h2>Problem:</h2>\r\n");
				
				// Turn into simple HTML (nl2br)
				if(0 != iProblemTextIsHTML)
					sProblemText = sProblemText.replaceAll("/\n/", "<br>");
				
				sContent.append(sProblemText);

//				sContent.append("<hr>");
				
				sContent.append("<h2>Solution:</h2>\r\n");			

				// Turn into simple HTML (nl2br)
				if(0 != iSolutionTextIsHTML)
					sSolutionText = sSolutionText.replaceAll("/\n/", "<br>");
				
				sContent.append(sSolutionText);				

				if(0 != sKeywords.length()) {
					sContent.append("<br><br><b>Keywords:</b> " + sKeywords);
				}
				
				Element eKbArticleContent = eKbArticle.addElement("content");
				eKbArticleContent.addAttribute("encoding", "base64");
				eKbArticleContent.setText(new String(Base64.encodeBase64(sContent.toString().getBytes(sExportEncoding))));
				
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
