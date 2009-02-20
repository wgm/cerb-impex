package com.webgroupmedia.cerb4.exporter.zendesk.entities;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.zendesk.ZendeskConnection;


public class Org {
	
	public void export() {
		String sExportEncoding = Configuration.get("exportEncoding", "ISO-8859-1");
		String cfgOutputDir = Configuration.get("outputDir", "output");
		
		Map<String,String> params = new HashMap<String,String>();
		Document orgDoc = ZendeskConnection.getInstance().requestZendeskDocumentSerial("organizations", null, params);
		
		Element organizationsElm = orgDoc.getRootElement();
		
		@SuppressWarnings("unchecked")
		List<Element> orgElms = organizationsElm.elements("organization");
		int iCount=0;
		int iSubDir = 0;
		File outputDir = null;
		
		for (Element orgElm : orgElms) {
			Integer orgId = Integer.parseInt(orgElm.elementText("id"));
			String name = orgElm.elementText("name");
			
			Document doc = DocumentHelper.createDocument();
			Element eContact = doc.addElement("organization");
			doc.setXMLEncoding(sExportEncoding);
			
			eContact.addElement("name").addText(name);
			eContact.addElement("street").addText("");
			eContact.addElement("city").addText("");
			eContact.addElement("province").addText("");
			eContact.addElement("postal").addText("");
			eContact.addElement("country").addText("");
			eContact.addElement("phone").addText("");
			eContact.addElement("fax").addText("");
			eContact.addElement("website").addText("");
			
			if (0 == iCount % 2000) {
				iSubDir++;

				// Make the output subdirectory
				outputDir = new File(cfgOutputDir + "/02-orgs-"	+ String.format("%09d", iSubDir));
				outputDir.mkdirs();
			}

			String sXmlFileName = outputDir.getPath() + "/"	+ String.format("%09d", orgId) + ".xml";

			try {
				new XMLThread(doc, sXmlFileName).start();
			} catch (Exception e) {
				e.printStackTrace();
			}

			iCount++;
		}
		
	}
}
