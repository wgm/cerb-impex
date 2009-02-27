package com.webgroupmedia.cerb4.exporter.zendesk.entities;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.zendesk.ZendeskConnection;


public class Worker {
	

	public void export() {
		final String cfgOutputDir = Configuration.get("outputDir", "output");
		final String cfgExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		final String cfgInitialWorkerPassword = new String(Configuration.get("initialWorkerPassword", "changeme"));
		
		
		List<Element> userList = new ArrayList<Element>();
		
		List<Element> agentList;
		int agentPageNum=1;
		do {
			agentList = null;
			agentList = getAgentsForRole(4, agentPageNum++);
			userList.addAll(agentList);
		}
		while(agentList.size()==15);

		List<Element> adminList;
		int adminPageNum=1;
		do {
			adminList = null;
			adminList = getAgentsForRole(2, adminPageNum++);
			userList.addAll(adminList);
		}
		while(adminList.size()==15);
		

		Integer iCount = 0;
		Integer iSubDir = 0;
		File outputDir = null;
		
		for (Element userElm : userList) {
			Integer userId = Integer.parseInt(userElm.elementText("id"));
			String name = userElm.elementText("name");
			String email = userElm.elementText("email");
			
			Integer role = Integer.parseInt(userElm.elementText("roles"));
			String isSuperuser = (role == 2) ? "1" : "0";
			
			String firstName="", lastName="";
			if (-1 != name.indexOf(" ")) {
				firstName = name.substring(0, name.indexOf(" "));
				lastName = name.substring(name.indexOf(" "));
			} else {
				firstName = name;
			}
			
			Document doc = DocumentHelper.createDocument();
			doc.setXMLEncoding(cfgExportEncoding);
			
			Element eWorker = doc.addElement("worker");
			eWorker.addElement("first_name").addText(firstName);
			eWorker.addElement("last_name").addText(lastName);
			eWorker.addElement("email").addText(email);
			eWorker.addElement("password").addText(getMd5Digest(cfgInitialWorkerPassword));
			eWorker.addElement("is_superuser").addText(isSuperuser);
			
			
			if (0 == iCount % 2000) {
				iSubDir++;

				// Make the output subdirectory
				outputDir = new File(cfgOutputDir + "/00-workers-" + String.format("%06d", iSubDir));
				outputDir.mkdirs();
			}
			
			String sXmlFileName = outputDir.getPath() + "/" + String.format("%06d", userId) + ".xml";

			try {
				new XMLThread(doc, sXmlFileName).start();
			} catch (Exception e) {
				e.printStackTrace();
			}

			iCount++;
			
		} 
	}
	

	private List<Element> getAgentsForRole(int role, Integer page) {
		List<Element> agentsList = new ArrayList<Element>();
		Map<String,String> params = new HashMap<String,String>();
		
		params.put("role", Integer.toString(role));
		params.put("page", page.toString());
		Document document = ZendeskConnection.getInstance().requestZendeskDocumentSerial("users", null, params);
		
		Element usersElm = document.getRootElement();
		
		if(!usersElm.getName().equals("users")) {
			return agentsList;
		}
		
		@SuppressWarnings("unchecked")
		List<Element> userElms = usersElm.elements("user");
		for (Element userElm : userElms) {
			agentsList.add(userElm);
		}
		return agentsList;
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
	
}
