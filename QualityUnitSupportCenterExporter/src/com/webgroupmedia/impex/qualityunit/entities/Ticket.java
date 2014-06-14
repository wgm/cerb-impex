package com.webgroupmedia.impex.qualityunit.entities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.impex.qualityunit.Database;
import com.webgroupmedia.impex.qualityunit.Driver;

public class Ticket {

	public static SimpleDateFormat RFC822DATEFORMAT = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);
	
	public void export() {
		Connection conn = Database.getInstance();
		
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "UTF-8"));
		
		Integer iCount = 0;
		Integer iSubDirCount = 0;		

		Boolean isVerbose = new Boolean(Configuration.get("verbose", "false"));
		
		try {
			Statement stmtDepartmentEmails = conn.createStatement();
			
			String sQueueToEmailSQL = "SELECT queue_id, name, queue_email FROM queues";
			ResultSet rsQueueToEmail = stmtDepartmentEmails.executeQuery(sQueueToEmailSQL);
			
			Map<String,String> mapQueueToEmail = new HashMap<String,String>();
			
			while(rsQueueToEmail.next()) {
				String sQueueEmailAddress = Driver.fixMagicQuotes(rsQueueToEmail.getString("queue_email"));
				String sQueueId = rsQueueToEmail.getString("queue_id");
				
				if(!mapQueueToEmail.containsKey(sQueueId)) {
					mapQueueToEmail.put(sQueueId, sQueueEmailAddress);
				}
			}
			
			Statement stmtTickets = conn.createStatement();

			String sql = "select t.ticket_id, t.subject_ticket_id AS mask, t.first_subject AS subject, t.queue_id, t.status, UNIX_TIMESTAMP(t.created) AS created, UNIX_TIMESTAMP(t.last_update) AS updated, q.name AS queue_name, u.email AS user_email FROM tickets t INNER JOIN queues q ON (q.queue_id=t.queue_id) INNER JOIN users u ON (u.user_id=t.customer_id)";
			
			ResultSet rsTickets = stmtTickets.executeQuery(sql);
			
			File outputDir = null;
			
			while(rsTickets.next()) {
				Integer iTicketId = rsTickets.getInt("ticket_id");
				String sMask = Driver.fixMagicQuotes(rsTickets.getString("mask"));
				String sSubject = Driver.fixMagicQuotes(rsTickets.getString("subject"));
				
				String ticketQueueId = rsTickets.getString("queue_id");
				
				Integer iCreatedDate = rsTickets.getInt("created");
				Integer iUpdatedDate = rsTickets.getInt("updated");
				
				String status = rsTickets.getString("status");
				Integer isClosed = 0;
				Integer isWaiting = 0;
				
				if(status.equals("a")) {
					isWaiting = 1;
				} else if(
					status.equals("r")
					|| status.equals("d")
					|| status.equals("s")
				) {
					isClosed = 1;
				}
				
				String sQueueName = Driver.fixMagicQuotes(rsTickets.getString("queue_name"));
				
				if(0 == iCount % 2000 || 0 == iCount) {
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/04-tickets-" + String.format("%06d", ++iSubDirCount));
					outputDir.mkdirs();
	
					if(!isVerbose)
						System.out.println("Writing to " + outputDir.getAbsolutePath());
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eTicket = doc.addElement("ticket");
				doc.setXMLEncoding(sExportEncoding);
				
				eTicket.addElement("subject").addText(sSubject);
				eTicket.addElement("group").addText(sQueueName); // cfgImportGroupName
				eTicket.addElement("bucket").addText("");
				eTicket.addElement("mask").addText(sMask);
				eTicket.addElement("created_date").addText(iCreatedDate.toString());
				eTicket.addElement("updated_date").addText(iUpdatedDate.toString());
				eTicket.addElement("is_waiting").addText(isWaiting.toString());
				eTicket.addElement("is_closed").addText(isClosed.toString());
				
				Element eRequesters = eTicket.addElement("requesters");
				String requesterEmail = rsTickets.getString("user_email");
				eRequesters.addElement("address").setText(requesterEmail);
				
				Statement stmtMessages = conn.createStatement();
				
				String sMessageSQL = "SELECT m.mail_id, m.body, m.created, u.email AS sender_email FROM mails m INNER JOIN mail_users ON (mail_users.mail_id=m.mail_id AND mail_users.mail_role = 'from') INNER JOIN users u ON (mail_users.user_id=u.user_id) WHERE m.ticket_id = " + iTicketId + " ORDER BY m.created";
				
				ResultSet rsMessages = stmtMessages.executeQuery(sMessageSQL);
				
				Element eMessages = eTicket.addElement("messages");
				
				while(rsMessages.next()) {
					String sMailId = rsMessages.getString("mail_id");
					String sContent = rsMessages.getString("body");
					
					Element eMessage = eMessages.addElement("message");
					Element eMessageHeaders = eMessage.addElement("headers");
					
					// [TODO] Identify worker sent messages
					
					//boolean isWorkerReply = (rsMessages.getInt("worker_reply") == 1);
					
					/*
					if(isWorkerReply) {
						eMessageHeaders.addElement("from").addCDATA(rsMessages.getString("email"));
					}
					else {
						eMessageHeaders.addElement("from").addCDATA(requesterEmail);
					}
					*/
					
					eMessageHeaders.addElement("to").addCDATA(mapQueueToEmail.get(ticketQueueId));
					eMessageHeaders.addElement("from").addCDATA(rsMessages.getString("sender_email"));
					eMessageHeaders.addElement("subject").addCDATA(sSubject);
					eMessageHeaders.addElement("date").addCDATA(rsMessages.getString("created"));
					
					Element eMessageContent = eMessage.addElement("content");
					eMessageContent.addAttribute("encoding", "base64");
					if(null != sContent)
						eMessageContent.setText(new String(Base64.encodeBase64(sContent.getBytes(sExportEncoding))));
					sContent = null;
					
					// Attachments
					Element eAttachments = eMessage.addElement("attachments");
					
					Statement stmtAttachments = conn.createStatement();
					String attachmentsSQL = "SELECT f.file_id, f.filename, f.filesize, f.filetype FROM files f INNER JOIN mail_attachments ma ON (ma.file_id=f.file_id) WHERE ma.mail_id = '" + sMailId + "'";
					ResultSet rsAttachments = stmtAttachments.executeQuery(attachmentsSQL);

					while(rsAttachments.next()) {
						String fileId = rsAttachments.getString("file_id");
						String fileName = rsAttachments.getString("filename");
						String fileSize = rsAttachments.getString("filesize");
						String fileType = rsAttachments.getString("filetype");
						
						Element eAttachment = eAttachments.addElement("attachment");
						eAttachment.addElement("name").setText(fileName);
						eAttachment.addElement("size").setText(fileSize);
						eAttachment.addElement("mimetype").setText(fileType);
					
						Element eAttachmentContent = eAttachment.addElement("content");
						eAttachmentContent.addAttribute("encoding", "base64");
						
						Statement stmtAttachmentContent = conn.createStatement();
						String attachmentContentSQL = "SELECT content FROM file_contents WHERE file_id = '" + fileId + "' ORDER BY content_nr";
						ResultSet rsAttachmentContents = stmtAttachmentContent.executeQuery(attachmentContentSQL);
						
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						
						while(rsAttachmentContents.next()) {
							Blob content = rsAttachmentContents.getBlob("content");
							
							byte[] bytes = new byte[1024000];
							
							InputStream is = content.getBinaryStream();
							
							int length = is.read(bytes);
							
							while(length != -1) {
								baos.write(bytes, 0, length);
								length = is.read(bytes);
							}
							
							is.close();
						}
						
						eAttachmentContent.addText(new String(Base64.encodeBase64(baos.toByteArray())));
						
						baos.close();
					}

				}
					
				rsMessages.close();
				stmtMessages.close();
				
				// [TODO] Comments
				
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
