package com.cerb4.impex;

import java.io.FileWriter;

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class XMLThread extends Thread {
	Document doc = null;
	String fileName = "";
	
	public XMLThread(Document doc, String filename) {
		this.doc = doc;
		this.fileName = filename;
	}
	
	@Override
	public synchronized void start() {
		Boolean isVerbose = new Boolean(Configuration.get("verbose", "false"));
		
		OutputFormat format = OutputFormat.createPrettyPrint();
		format.setEncoding("ISO-8859-1");
		format.setOmitEncoding(false);
		
		try {
			XMLWriter writer = new XMLWriter(new FileWriter(this.fileName), format);
			writer.write(doc);
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			doc.clearContent();
			doc = null;
		}
		
		if(isVerbose)
			System.out.println("Wrote " + this.fileName);
		
		this.interrupt();
	}
	
}
