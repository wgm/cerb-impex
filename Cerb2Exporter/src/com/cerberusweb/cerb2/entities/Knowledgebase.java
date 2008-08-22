package com.cerberusweb.cerb2.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.cerberusweb.cerb2.Database;

public class Knowledgebase { 	
	
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			Statement s = conn.createStatement();
			s.execute("SELECT kb.kb_id, unix_timestamp(kb.kb_entry_date) as kb_created_date, kb.kb_keywords, kb.kb_public_views, "+
					"kb.kb_public, kbp.kb_problem_summary, kbp.kb_problem_text, kbp.kb_problem_text_is_html, "+
					"kbs.kb_solution_text, kbs.kb_solution_text_is_html "+
					"FROM `knowledgebase` kb "+
					"inner join `knowledgebase_problem` kbp ON (kbp.kb_id=kb.kb_id) "+
					"inner join `knowledgebase_solution` kbs ON (kbs.kb_id=kb.kb_id) "+
					"ORDER BY kb.kb_id ASC"
				);
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/10-kb-" + String.format("%06d", iSubDir));
					outputDir.mkdirs();
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eKbArticle = doc.addElement("knowledgebase");
				doc.setXMLEncoding("ISO-8859-1");
				
				Integer iId = rs.getInt("kb_id");
				String sTitle = rs.getString("kb_problem_summary");
				Integer iCreatedDate = rs.getInt("kb_created_date");
				String sProblemText = rs.getString("kb_problem_text");
				Integer iProblemTextIsHTML = rs.getInt("kb_problem_text_is_html");
				String sSolutionText = rs.getString("kb_solution_text");
				Integer sSolutionTextIsHTML = rs.getInt("kb_solution_text_is_html");

				if(0 == sTitle.length())
					continue;
								
				eKbArticle.addElement("title").addText(sTitle);
				eKbArticle.addElement("created_date").addText(iCreatedDate.toString());
				
				// [TODO] Append problem+solution+keywords, and check is_html
				// eCommentContent.setText(new String(Base64.encodeBase64(sCommentText.getBytes())));
				
				eKbArticle.addElement("content").addText("");
				
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
