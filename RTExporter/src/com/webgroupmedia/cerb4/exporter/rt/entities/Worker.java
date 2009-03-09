package com.webgroupmedia.cerb4.exporter.rt.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.rt.Database;
import com.webgroupmedia.cerb4.exporter.rt.Driver;

public class Worker {
	
	private Set<Integer> superuserSet;
	
	public void export() {
		Connection conn = Database.getInstance();
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		String cfgExportRTGroup = new String(Configuration.get("exportWorkersRTGroup", "Privileged"));
		
		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {

			
			initSuperuserSet();
			
			
			Statement s = conn.createStatement();
			
			
			
			String sql = "SELECT u.id, u.RealName, u.EmailAddress, u.Password  " + 
					"FROM Users u " +
					"INNER JOIN GroupMembers gm on gm.MemberId = u.id " + 
					"INNER JOIN Groups g ON gm.GroupId = g.id " +
					"WHERE 1=1 ";
			
			String sqlLastCondition = "";
			boolean exportPrivileged =  (cfgExportRTGroup.equals("Privileged"));
			if(exportPrivileged) {
				sqlLastCondition = "AND g.Type = 'Privileged' ";
			}
			else {
				sqlLastCondition = "AND g.Domain='UserDefined' AND g.Name = '" + cfgExportRTGroup+"' ";
			}
			
			String sqlOrder = "ORDER BY u.id ";
			sql += sqlLastCondition + sqlOrder;
			
			s.execute(sql);
			ResultSet rs = s.getResultSet();
	
			File outputDir = null;
			
			while(rs.next()) {
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/00-workers-" + String.format("%06d", iSubDir));
					outputDir.mkdirs();
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eWorker = doc.addElement("worker");
				doc.setXMLEncoding(sExportEncoding);
				
				Integer userId = rs.getInt("id");
				Integer isSuperuser = superuserSet.contains(userId) ? 1 : 0;
				
				
				String sName = Driver.fixMagicQuotes(rs.getString("RealName"));
				String sEmail = Driver.fixMagicQuotes(rs.getString("EmailAddress"));
				String sPassword = rs.getString("Password");
				
				String sFirstName, sLastName="";
				if(-1 != sName.indexOf(" ")) {
					sFirstName = sName.substring(0,sName.indexOf(" "));
					sLastName = sName.substring(sName.indexOf(" "));
				} else {
					sFirstName = sName;
				}
				
				if(0 == sEmail.length())
					continue;
				
				eWorker.addElement("first_name").addText(sFirstName);
				eWorker.addElement("last_name").addText(sLastName);
				eWorker.addElement("email").addText(sEmail);
				eWorker.addElement("password").addText(sPassword);
				eWorker.addElement("is_superuser").addText(isSuperuser.toString());
				
				String sXmlFileName = outputDir.getPath() + "/" + String.format("%06d", userId) + ".xml";

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
	
	private void initSuperuserSet() {
		Connection conn = Database.getInstance();
		
		//figure out what user ids are superusers
		String sqlSuperuser = "SELECT u.id " +
				"FROM Users u " + 
				"INNER JOIN GroupMembers gmacl ON gmacl.MemberId = u.id " +
				"LEFT JOIN ACL acl ON gmacl.GroupId = acl.PrincipalId " +  
				"WHERE acl.RightName = 'SuperUser' " +
				"ORDER BY u.id";
		
		Statement statementSuperuser;
		try {
			statementSuperuser = conn.createStatement();
		
		statementSuperuser.execute(sqlSuperuser);
		ResultSet rsSuperuser =statementSuperuser.getResultSet();
		
		superuserSet = new HashSet<Integer>();
		while(rsSuperuser.next()) {
			Integer userId = rsSuperuser.getInt("id");
			superuserSet.add(userId);
		}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
