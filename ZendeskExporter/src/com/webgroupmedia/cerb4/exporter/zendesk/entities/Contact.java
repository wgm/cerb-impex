package com.webgroupmedia.cerb4.exporter.zendesk.entities;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.zendesk.ZendeskConnection;
import com.webgroupmedia.cerb4.exporter.zendesk.ZenRunnable.IResponseHandler;


public class Contact {
	
	private final String sExportEncoding = Configuration.get("exportEncoding", "ISO-8859-1");
	private final String cfgOutputDir = Configuration.get("outputDir", "output");
	private final String cfgInitialContactPassword = new String(Configuration.get("initialContactPassword", ""));
	
	private Map<Integer,String> orgMap;
	
	private boolean lastPageReached = false;
	
	
	private File outputDir;
	private int iCount=0;
	private int iSubDirCount=0;
	
	public void export() {
		
		initOrgsMap();
		Integer page = 0;
		
		while(!lastPageReached) {
			page++;
			Map<String,String> params = new HashMap<String,String>();
			params.put("role", "0");
			params.put("page", page.toString());
			ZendeskConnection.getInstance().requestZendeskDocumentAsync("users", null, params, new IResponseHandler() {
	
				public void onZenResponse(Document usersDoc) {
					Element usersElm = usersDoc.getRootElement();
					@SuppressWarnings("unchecked")
					List<Element> userElms = usersElm.elements("user");
					
					if(userElms==null || userElms.size() == 0) {
						lastPageReached = true;
						return;
					}
					for (Element userElm : userElms) {
						Integer userId = Integer.parseInt(userElm.elementText("id"));
						String name = userElm.elementText("name");
						String email = userElm.elementText("email");
						
						String firstName="", lastName="";
						if (-1 != name.indexOf(" ")) {
							firstName = name.substring(0, name.indexOf(" "));
							lastName = name.substring(name.indexOf(" "));
						} else {
							firstName = name;
						}
						
						Integer orgId;
						try {
							orgId = Integer.parseInt(userElm.elementText("organization-id"));
						}
						catch(NumberFormatException e) {
							orgId = -1;
						}
						
						String password="";
						if(cfgInitialContactPassword.length() > 0) {
							password = getMd5Digest(cfgInitialContactPassword);
						}
						
						Document doc = DocumentHelper.createDocument();
						Element eContact = doc.addElement("contact");
						doc.setXMLEncoding(sExportEncoding);
						
						eContact.addElement("first_name").addText(firstName);
						eContact.addElement("last_name").addText(lastName);
						eContact.addElement("email").addText(email);
						eContact.addElement("password").addText(password);
						eContact.addElement("phone").addText("");
						
						String orgName = orgMap.get(orgId);
						if(orgName == null) orgName = "";
						eContact.addElement("organization").addText(orgName);
						
						String sXmlFileName = getXmlWritePath(userId, doc);
						
						try {
							new XMLThread(doc, sXmlFileName).start();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				
			});
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		
	}

	private void initOrgsMap() {
		orgMap = new HashMap<Integer, String>();
		
		
		Map<String,String> params = new HashMap<String,String>();
		Document orgDoc = ZendeskConnection.getInstance().requestZendeskDocumentSerial("organizations", null, params);
		
		Element organizationsElm = orgDoc.getRootElement();
		
		@SuppressWarnings("unchecked")
		List<Element> orgElms = organizationsElm.elements("organization");

		for (Element orgElm : orgElms) {
			Integer orgId = Integer.parseInt(orgElm.elementText("id"));
			String name = orgElm.elementText("name");
			orgMap.put(orgId, name);
		}
	}
	
    private static String getMd5Digest(String input)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1,messageDigest);
            String md5Str = number.toString(16);
            if(md5Str.length() < 32) {
            	int fillCount = 32 - md5Str.length();
            	StringBuffer zeroBuffer = new StringBuffer();
            	for(int i=0; i < fillCount; i++) {
            		zeroBuffer.append("0");
            	}
            	md5Str = zeroBuffer.toString() + md5Str;
            }
            return md5Str;
        }
        catch(NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }

    private synchronized String getXmlWritePath(Integer userId, Document doc) {
		if (0 == iCount % 2000) {
			// Make the output subdirectory
			outputDir = new File(cfgOutputDir + "/03-contacts-" + String.format("%09d", ++iSubDirCount));
			outputDir.mkdirs();
		}

		iCount++;

		return outputDir.getPath() + "/" + String.format("%09d", userId) + ".xml";
    }
    
}
