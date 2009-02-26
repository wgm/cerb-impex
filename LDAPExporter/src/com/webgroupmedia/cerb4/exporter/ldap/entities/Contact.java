package com.webgroupmedia.cerb4.exporter.ldap.entities;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;


public class Contact {
	
	private final String sExportEncoding = Configuration.get("exportEncoding", "ISO-8859-1");
	private final String cfgOutputDir = Configuration.get("outputDir", "output");
	private final String cfgInitialContactPassword = Configuration.get("initialContactPassword", "");
	
	private final String LDAP_HOST = Configuration.get("LDAPHost", "");
	private final String LDAP_LOGIN_DN = Configuration.get("LDAPLoginDN", "output");
	private final String LDAP_PASSWORD = Configuration.get("LDAPPassword", "");
	private final String LDAP_SEARCH_BASE = Configuration.get("LDAPSearchBase", "");
	private final String LDAP_SEARCH_FILTER = Configuration.get("LDAPSearchFilter", "(objectClass=*)");
	
	private File outputDir;
	private int iCount=0;
	private int iSubDirCount=0;
	
	public void export() {
		
		
	     int ldapPort = LDAPConnection.DEFAULT_PORT;

		int searchScope = LDAPConnection.SCOPE_ONE;
		int ldapVersion = LDAPConnection.LDAP_V3;
		;
		boolean attributeOnly = false;
		String attrs[] = { LDAPConnection.ALL_USER_ATTRS /*NO_ATTRS*/ };
		//String attrs[] = {"uid","sn","cn","o","name","userPassword"};
		
		LDAPConnection lc = new LDAPConnection();

		try {
			// connect to the server
			lc.connect(LDAP_HOST, ldapPort);
			// bind to the server
			
			
			LDAPSearchConstraints constraint = new LDAPSearchConstraints();
			constraint.setBatchSize(0);
			
			lc.bind(ldapVersion, LDAP_LOGIN_DN, LDAP_PASSWORD.getBytes("UTF8"));
			LDAPSearchResults searchResults = lc.search(LDAP_SEARCH_BASE, 
					searchScope, 
					LDAP_SEARCH_FILTER,
					attrs, 
					attributeOnly,
					constraint);
			
			//System.out.println(searchResults.hasMore());
			while (searchResults.hasMore()) {
				LDAPEntry nextEntry = null;
				try {
					nextEntry = searchResults.next();
					//System.out.println(nextEntry.getAttribute("uid").getStringValue());
//						LDAPAttributeSet attributeSet = nextEntry.getAttributeSet();
//						for (Object object : attributeSet) {
//							System.out.println("atr:"+object);
//						}
					
					String firstName="", lastName="", fullName="";
					
					LDAPAttribute fullNameAttribute;
					fullNameAttribute = nextEntry.getAttribute("displayName");
					if(fullNameAttribute == null) {
						fullNameAttribute = nextEntry.getAttribute("name");
						if(fullNameAttribute == null) {
							fullNameAttribute = nextEntry.getAttribute("uid");
						}
					}
					
					
					if(fullNameAttribute != null) {
						fullName = fullNameAttribute.getStringValue();
						
						if (-1 != fullName.indexOf(" ")) {
							firstName = fullName.substring(0, fullName.indexOf(" "));
							lastName = fullName.substring(fullName.indexOf(" "));
						} else {
							firstName = fullName;
						}
					}

					LDAPAttribute surnameAttribute = nextEntry.getAttribute("sn");
					if(surnameAttribute != null) {
						lastName = surnameAttribute.getStringValue();
					}
					
					
					LDAPAttribute givenNameAttribute = nextEntry.getAttribute("givenName");
					if(givenNameAttribute != null) {
						firstName = givenNameAttribute.getStringValue();
					}


					
					String email = "";
					LDAPAttribute emailAttribute = nextEntry.getAttribute("mail");
					if(emailAttribute != null) {
						email = emailAttribute.getStringValue();
					}
					
					String password = "";

					
					if(cfgInitialContactPassword.length() > 0) {
						password = getMd5Digest(cfgInitialContactPassword);
					}
					else {
						LDAPAttribute passwordAttribute = nextEntry.getAttribute("userPassword");
						if(passwordAttribute != null) {
							password = passwordAttribute.getStringValue();
						}
						if(password.length() != 0) {
							password = getMd5Digest(password);
						}
					}
					
					String phone = null;
					LDAPAttribute telephoneNumberAttribute = nextEntry.getAttribute("telephoneNumber");
					if(telephoneNumberAttribute != null) {
						phone = telephoneNumberAttribute.getStringValue();
					}
					
					if(phone == null) {
						LDAPAttribute mobilePhoneAttribute = nextEntry.getAttribute("mobile");
						if(mobilePhoneAttribute != null) {
							phone = mobilePhoneAttribute.getStringValue();
						}
						else {
							phone = "";
						}
						
					}
					
					String orgName = "";
					LDAPAttribute orgAttribute = nextEntry.getAttribute("o");
					if(orgAttribute != null) {
						orgName = orgAttribute.getStringValue();
					}
					
					if(email == null || email.length() == 0) {
						continue;
					}
				
					Document doc = DocumentHelper.createDocument();
					Element eContact = doc.addElement("contact");
					doc.setXMLEncoding(sExportEncoding);

					eContact.addElement("first_name").addText(firstName);
					eContact.addElement("last_name").addText(lastName);
					eContact.addElement("email").addText(email);
					eContact.addElement("password").addText(password);
					eContact.addElement("phone").addText(phone);
					eContact.addElement("organization").addText(orgName);
					
					if (0 == iCount % 2000) {
						// Make the output subdirectory
						outputDir = new File(cfgOutputDir + "/03-contacts-" + String.format("%09d", ++iSubDirCount));
						outputDir.mkdirs();
					}

					String sXmlFileName =  outputDir.getPath() + "/" + String.format("%09d", iCount+1) + ".xml";
					
					
					try {
						new XMLThread(doc, sXmlFileName).start();
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					
					iCount++;
					
					//System.out.println(nextEntry);
				}
				catch (LDAPException e) {
					System.out.println("Error: " + e.toString());
					// Exception is thrown, go for next entry
					continue;
				}
				
				//System.out.println("\n" + nextEntry.getDN());
			}

			lc.disconnect();
		}
		catch (LDAPException e) {
			System.out.println("Error: " + e.toString());
		}
		catch (UnsupportedEncodingException e) {
			System.out.println("Error: " + e.toString());
		}        
		
	}

	
    private static String getMd5Digest(String input)
    {
    	if(1==1) return input;
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
