package com.webgroupmedia.cerb4.exporter.osTicket.entities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.osTicket.Database;
import com.webgroupmedia.cerb4.exporter.osTicket.Driver;

public class Ticket {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgImportGroupName = Configuration.get("exportToGroup", "Import:osTicket");
		//String cfgCerb4HomeDir = Configuration.get("cerb4HomeDir", "");
		
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		//SimpleDateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

		Integer iCount = 0;
		Integer iSubDirCount = 0;		

		Boolean isVerbose = new Boolean(Configuration.get("verbose", "false"));
//		Boolean isCfgTicketExcludeOpen = new Boolean(Configuration.get("exportTicketExcludeOpen", "false"));
//		Boolean isCfgTicketExcludeClosed = new Boolean(Configuration.get("exportTicketExcludeClosed", "false"));
		
		try {
			Statement stmtDepartmentEmails = conn.createStatement();
			
			String deptEmailsSQL = "SELECT dept_id, email FROM ost_email ";
			ResultSet rsDeptEmails = stmtDepartmentEmails.executeQuery(deptEmailsSQL);
			
			Map<Integer,String> deptEmailMap = new HashMap<Integer,String>();
			
			while(rsDeptEmails.next()) {
				String departmentEmailAddy = Driver.fixMagicQuotes(rsDeptEmails.getString("email"));
				Integer deptId = rsDeptEmails.getInt("dept_id");
				
				if(!deptEmailMap.containsKey(deptId)) {
					deptEmailMap.put(deptId, departmentEmailAddy);
				}
			}
			
			Statement stmtTickets = conn.createStatement();

			String sql= "SELECT t.ticket_id, t.ticketID as mask_id, t.subject, t.created, t.updated, t.status, t.email, "+
			"d.dept_name, d.dept_id " +
			"FROM ost_ticket t " +
			"INNER JOIN ost_department d ON t.dept_id = d.dept_id ";
			ResultSet rsTickets = stmtTickets.executeQuery(sql);
			
			File outputDir = null;
			
			Integer ticketDeptId;
			
			while(rsTickets.next()) {
				Integer iTicketId = rsTickets.getInt("ticket_id");
				String sSubject = Driver.fixMagicQuotes(rsTickets.getString("subject"));
				Integer sMaskId = rsTickets.getInt("mask_id");
				
				String sMask = Configuration.get("exportMaskPrefix", "OST") + String.format("-%d", sMaskId);
				
				ticketDeptId = rsTickets.getInt("dept_id");
				
				Long iCreatedDate = rsTickets.getDate("created").getTime()/1000;
				
				//osTicket sets the updated date to 0000-00-00 00:00:00 upon creation, which breaks jdbc getDate calls.
				//so catch them
				Long iUpdatedDate;
				try {
					iUpdatedDate = rsTickets.getDate("updated").getTime()/1000;
				}
				catch(SQLException e) {
					iUpdatedDate = iCreatedDate.longValue();
				}

				String status = rsTickets.getString("status");
				Integer isClosed = status.equals("closed") ? 1 : 0;
				Integer isWaiting = 0;
				
				String sTeamName = Driver.fixMagicQuotes(rsTickets.getString("dept_name"));
				
				if(0 == iCount % 2000 || 0 == iCount) {
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/02-tickets-" + String.format("%06d", ++iSubDirCount));
					outputDir.mkdirs();
	
					if(!isVerbose)
						System.out.println("Writing to " + outputDir.getAbsolutePath());
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eTicket = doc.addElement("ticket");
				doc.setXMLEncoding(sExportEncoding);
				
				eTicket.addElement("subject").addText(sSubject);
				eTicket.addElement("group").addText(cfgImportGroupName);
				eTicket.addElement("bucket").addText(sTeamName);
				eTicket.addElement("mask").addText(sMask);
				eTicket.addElement("created_date").addText(iCreatedDate.toString());
				eTicket.addElement("updated_date").addText(iUpdatedDate.toString());
				eTicket.addElement("is_waiting").addText(isWaiting.toString());
				eTicket.addElement("is_closed").addText(isClosed.toString());
				
				Element eRequesters = eTicket.addElement("requesters");
				String requesterEmail = rsTickets.getString("email");
				eRequesters.addElement("address").setText(requesterEmail);
				
				Statement stmtMessages = conn.createStatement();
				
				String messageSql = "SELECT message as content, created, headers, 0 as worker_reply, '' as email, msg_id " +
				"FROM ost_ticket_message m "+
				"WHERE  m.ticket_id = "+ iTicketId + " " +
				" UNION ALL " +
				"SELECT r.response as content, r.created, '' as headers, 1 as worker_reply, s.email, 0 as msg_id "+
				"FROM ost_ticket_response r "+
				"INNER JOIN ost_staff s ON r.staff_id = s.staff_id "+
				"WHERE  r.ticket_id = " + iTicketId + " ";
				
				ResultSet rsMessages = stmtMessages.executeQuery(messageSql);				
				
				Element eMessages = eTicket.addElement("messages");
				
				while(rsMessages.next()) {
					//Integer messageId = rsMessages.getInt("message_id");
					String strContent = rsMessages.getString("content"); 
					Integer msgId = rsMessages.getInt("msg_id");
					
					Element eMessage = eMessages.addElement("message");
					Element eMessageHeaders = eMessage.addElement("headers");
					
					
//				TODO eventually we may want to actually parse their raw headers, when they are actually present
//				(Note, they are only present in customer emails AND only if they don't have the save raw headers configuration setting off)					

//						if(sHeaderName.equals("date") 
//								|| sHeaderName.equals("to")
//								|| sHeaderName.equals("from")
//								|| sHeaderName.equals("subject")
//								|| sHeaderName.equals("message-id")) {
//							eMessageHeaders.addElement(sHeaderName).addCDATA(sHeaderValue);
					
					boolean isWorkerReply = (rsMessages.getInt("worker_reply") == 1);
					if(isWorkerReply) {
						eMessageHeaders.addElement("to").addCDATA(deptEmailMap.get(ticketDeptId));
						eMessageHeaders.addElement("from").addCDATA(rsMessages.getString("email"));
						eMessageHeaders.addElement("subject").addCDATA(sSubject);			
					}
					else {
						eMessageHeaders.addElement("to").addCDATA(deptEmailMap.get(ticketDeptId));
						eMessageHeaders.addElement("from").addCDATA(requesterEmail);
						eMessageHeaders.addElement("subject").addCDATA(sSubject);			
					}

					Element eMessageContent = eMessage.addElement("content");
					eMessageContent.addAttribute("encoding", "base64");
					eMessageContent.setText(new String(Base64.encodeBase64(strContent.getBytes(sExportEncoding))));
					strContent = null;
					
					// Attachments
					Element eAttachments = eMessage.addElement("attachments");
					
					Statement stmtAttachPath = conn.createStatement();
					
					String attachPathSQL = "SELECT upload_dir FROM ost_config";
					
					ResultSet rsAttachPath = stmtAttachPath.executeQuery(attachPathSQL);
					String strUploadDir = "";
					if(rsAttachPath.next()) {
						strUploadDir = rsAttachPath.getString("upload_dir"); 
					}
					
					Statement stmtAttachments = conn.createStatement();
					String attachmentsSQL = "SELECT file_key, file_name, file_size FROM ost_ticket_attachment WHERE ref_id = " + msgId;
					ResultSet rsAttachments = stmtAttachments.executeQuery(attachmentsSQL);

					while(rsAttachments.next()) {
						String fileKey = rsAttachments.getString("file_key");
						String fileName = rsAttachments.getString("file_name");
						String attachPath = strUploadDir + fileKey + "_" + fileName;
						String fileSize = rsAttachments.getString("file_size");
						
						
						Element eAttachment = eAttachments.addElement("attachment");
						eAttachment.addElement("name").setText(fileName);
						eAttachment.addElement("size").setText(fileSize);
						//eAttachment.addElement("mimetype").setText(sMimeType);
					
						Element eAttachmentContent = eAttachment.addElement("content");
						eAttachmentContent.addAttribute("encoding", "base64");
						
						File attachmentFile = new File(attachPath);
						
						
				        InputStream is = new FileInputStream(attachmentFile);
				        long length = attachmentFile.length();
				        if (length > Integer.MAX_VALUE) {
				            // File is too large
				        	throw new Exception("File is too large");
				        }
				        byte[] bytes = new byte[(int)length];
				    
				        int offset = 0;
				        int numRead = 0;
				        while (offset < bytes.length
				               && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				            offset += numRead;
				        }
				        // Ensure all the bytes have been read in
				        if (offset < bytes.length) {
				            throw new IOException("Could not completely read file "+attachmentFile.getName());
				        }
				        is.close();
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						baos.write(bytes);
						eAttachmentContent.addText(new String(Base64.encodeBase64(baos.toByteArray())));

					}

				}
				rsMessages.close();
				stmtMessages.close();
				
				// Comments
				Element eComments = eTicket.addElement("comments");
				
				Statement stmtComments = conn.createStatement();
				
				String sqlComments = "SELECT n.created, n.note, s.email "+
				"FROM ost_ticket_note n "+
				"INNER JOIN ost_staff s ON n.staff_id = s.staff_id ";
				
				ResultSet rsComments = stmtComments.executeQuery(sqlComments);
				
				while(rsComments.next()) {
					Long iCommentCreatedDate = rsComments.getDate("created").getTime()/1000;

					String sCommentAuthor = rsComments.getString("email");
					String sCommentText = Driver.fixMagicQuotes(rsComments.getString("note"));
					
					Element eComment = eComments.addElement("comment");
					eComment.addElement("created_date").setText(iCommentCreatedDate.toString());
					eComment.addElement("author").setText(sCommentAuthor);
					
					Element eCommentContent = eComment.addElement("content");
					eCommentContent.addAttribute("encoding", "base64");
					eCommentContent.setText(new String(Base64.encodeBase64(sCommentText.getBytes(sExportEncoding))));
					sCommentText = null;
				}
				rsComments.close();
				stmtComments.close();
				
//				System.out.println(doc.asXML());
				String sXmlFileName = outputDir.getPath() + "/" + String.format("%09d",iTicketId) + ".xml";

				try {
					new XMLThread(doc, sXmlFileName).start();
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				iCount++;
			}
			rsTickets.close();
			stmtTickets.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
