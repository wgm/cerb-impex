package com.cerb3.exporter.entities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb3.exporter.Database;
import com.cerb3.exporter.Driver;
import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;

public class Ticket {
	public void export() {
		Connection conn = Database.getInstance();
		String cfgImportGroupName = Configuration.get("exportToGroup", "Import:Cerb3");
		String cfgOutputDir = Configuration.get("outputDir", "output");
		
		SimpleDateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

		Integer iCount = 0;
		Integer iSubDirCount = 0;		

		Boolean isVerbose = new Boolean(Configuration.get("verbose", "false"));
//		String sCfgTicketStartId = Configuration.get("exportTicketStartId", "1");
//		String sCfgTicketEndId = Configuration.get("exportTicketEndId", "");
		Boolean isCfgTicketExcludeOpen = new Boolean(Configuration.get("exportTicketExcludeOpen", "false"));
		Boolean isCfgTicketExcludeClosed = new Boolean(Configuration.get("exportTicketExcludeClosed", "false"));
		
		try {
			// Cache the number of tickets
//			Statement stmtMaxTicket = conn.createStatement();
//			ResultSet rsMaxTicket = stmtMaxTicket.executeQuery("SELECT max(ticket_id) AS max_id FROM ticket");
//			
//			Integer iMaxTicketId = 0;
//			if(rsMaxTicket.next())
//				iMaxTicketId = rsMaxTicket.getInt("max_id");
//			
////			System.out.println("Max Ticket ID: " + iMaxTicketId);
//			
//			rsMaxTicket.close();
//			stmtMaxTicket.close();
			
			Statement stmtTickets = conn.createStatement();
			ResultSet rsTickets = stmtTickets.executeQuery("SELECT t.ticket_id, t.ticket_subject, t.ticket_mask, UNIX_TIMESTAMP(t.ticket_date) as ticket_date, "+
				"UNIX_TIMESTAMP(ticket_last_date) as ticket_updated, t.is_waiting_on_customer, t.is_closed, q.queue_name, q.queue_reply_to "+
				"FROM ticket t "+
				"INNER JOIN queue q ON (q.queue_id=t.ticket_queue_id) "+
				"WHERE t.is_deleted = 0 "+ 
				"AND t.ticket_spam_trained != 2 "+
				(isCfgTicketExcludeOpen ? "AND t.is_closed = 1 " : "") +
				(isCfgTicketExcludeClosed ? "AND t.is_closed = 0 " : "") +
//				"AND t.ticket_id >= " + sCfgTicketStartId + " " +
//				(0 != sCfgTicketEndId.length() ? ("AND t.ticket_id <= " + sCfgTicketEndId + " ") : "") +
				"ORDER BY t.ticket_id DESC "+
				"");
	
			File outputDir = null;
			
			while(rsTickets.next()) {
				Integer iTicketId = rsTickets.getInt("ticket_id");
				String sSubject = Driver.fixMagicQuotes(rsTickets.getString("ticket_subject"));
				String sMask = Driver.fixMagicQuotes(rsTickets.getString("ticket_mask").trim());
				Integer iCreatedDate = rsTickets.getInt("ticket_date");
				Integer iUpdatedDate = rsTickets.getInt("ticket_updated");
				Integer isWaiting = rsTickets.getInt("is_waiting_on_customer");
				Integer isClosed = rsTickets.getInt("is_closed");
				String sQueueName = Driver.fixMagicQuotes(rsTickets.getString("queue_name"));
				String sQueueReplyTo = rsTickets.getString("queue_reply_to");
				
				if(0 == iCount % 2000 || 0 == iCount) {
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/02-tickets-" + String.format("%06d", ++iSubDirCount));
					outputDir.mkdirs();
	
					if(!isVerbose)
						System.out.println("Writing to " + outputDir.getAbsolutePath());
//					System.gc();
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eTicket = doc.addElement("ticket");
				doc.setXMLEncoding("ISO-8859-1");
				
				if(0 == sMask.length()) {
					sMask = Configuration.get("exportMaskPrefix", "CERB3") + String.format("-%d", iTicketId);
				}
				
				eTicket.addElement("subject").addText(sSubject);
				eTicket.addElement("group").addText(cfgImportGroupName);
				eTicket.addElement("bucket").addText(sQueueName);
				eTicket.addElement("mask").addText(sMask);
				eTicket.addElement("created_date").addText(iCreatedDate.toString());
				eTicket.addElement("updated_date").addText(iUpdatedDate.toString());
				eTicket.addElement("is_waiting").addText(isWaiting.toString());
				eTicket.addElement("is_closed").addText(isClosed.toString());
				
				Statement stmtRequesters = conn.createStatement();
				ResultSet rsRequesters = stmtRequesters.executeQuery("SELECT a.address_address "+
					"FROM requestor r "+
					"INNER JOIN address a ON (a.address_id=r.address_id) "+
					"WHERE r.ticket_id = " + iTicketId + " " 
					);
				
				Element eRequesters = eTicket.addElement("requesters");
				
				while(rsRequesters.next()) {
					String sRequesterAddy = rsRequesters.getString("address_address");
					eRequesters.addElement("address").setText(sRequesterAddy);
				}
				rsRequesters.close();
				stmtRequesters.close();
				
				Statement stmtMessages = conn.createStatement();
				ResultSet rsMessages = stmtMessages.executeQuery("SELECT thread_id, thread_message_id, thread_subject, thread_address_id, address.address_address as sender_from, "+
					"UNIX_TIMESTAMP(thread_date) as thread_date, is_agent_message "+
					"FROM thread "+
					"INNER JOIN address ON (thread.thread_address_id=address.address_id) "+
					"WHERE ticket_id = "  + iTicketId + " " +
					"ORDER BY thread_id ASC");
				
				Element eMessages = eTicket.addElement("messages");
				
				while(rsMessages.next()) {
					Integer iThreadId = rsMessages.getInt("thread_id");
					String sThreadSender = rsMessages.getString("sender_from");
					String sThreadSubject = Driver.fixMagicQuotes(rsMessages.getString("thread_subject"));
					String sThreadMsgId = Driver.fixMagicQuotes(rsMessages.getString("thread_message_id"));
					Long lThreadDate = rsMessages.getLong("thread_date");
					
					Element eMessage = eMessages.addElement("message");
					
					Element eMessageHeaders = eMessage.addElement("headers");
					
					String sMessageDate = rfcDateFormat.format(new Date(lThreadDate*1000));
					
					if(null != sMessageDate && 0 != sMessageDate.length())
						eMessageHeaders.addElement("date").addCDATA(sMessageDate);
					if(null != sQueueReplyTo && 0 != sQueueReplyTo.length())
						eMessageHeaders.addElement("to").addCDATA(sQueueReplyTo);
					if(null != sThreadSender && 0 != sThreadSender.length())
						eMessageHeaders.addElement("from").addCDATA(sThreadSender);
					if(null != sThreadSubject && 0 != sThreadSubject.length())
						eMessageHeaders.addElement("subject").addCDATA(sThreadSubject);
					if(null != sThreadMsgId && 0 != sThreadMsgId.length())
						eMessageHeaders.addElement("message-id").addCDATA(sThreadMsgId);
					
					// Content
					Statement stmtContents = conn.createStatement();
					ResultSet rsContents = stmtContents.executeQuery("SELECT thread_content_part "+
						"FROM thread_content_part "+
						"WHERE thread_id = " + iThreadId + " " +
						"ORDER BY content_id ASC");
					
					StringBuilder strContent = new StringBuilder();
					
					while(rsContents.next()) {
						String sContentPart = Driver.fixMagicQuotes(rsContents.getString("thread_content_part"));
						strContent.append(sContentPart);
						// [TODO] Ugly
						if(!rsContents.isLast() && 255 != sContentPart.length())
							strContent.append(" ");
					}
					rsContents.close();
					stmtContents.close();
					
					Element eMessageContent = eMessage.addElement("content");
					eMessageContent.addAttribute("encoding", "base64");
					eMessageContent.setText(new String(Base64.encodeBase64(strContent.toString().getBytes())));
					strContent = null;
					
					// Attachments
					Element eAttachments = eMessage.addElement("attachments");
					
					Statement stmtAttachments = conn.createStatement();
					ResultSet rsAttachments = stmtAttachments.executeQuery("SELECT file_id, file_name, file_size "+
						"FROM thread_attachments " +
						"WHERE file_name != 'message_source.xml' " + 
						"AND file_name != 'html_mime_part.html' " + 
						"AND file_name != 'message_headers.txt' " + 
						"AND thread_id = " + iThreadId + " " +
						"ORDER BY file_id ASC");
					
					while(rsAttachments.next()) {
						Integer iFileId = rsAttachments.getInt("file_id"); 
						String sFileName = Driver.fixMagicQuotes(rsAttachments.getString("file_name"));
						String sFileSize = rsAttachments.getString("file_size");
						
						Element eAttachment = eAttachments.addElement("attachment");
						eAttachment.addElement("name").setText(sFileName);
						eAttachment.addElement("size").setText(sFileSize);
						eAttachment.addElement("mimetype").setText("");
						
						Element eAttachmentContent = eAttachment.addElement("content");
						eAttachmentContent.addAttribute("encoding", "base64");
						
						// [TODO] Option to ignore huge attachments?
						
						Statement stmtAttachment = conn.createStatement();
						ResultSet rsAttachment = stmtAttachment.executeQuery("SELECT part_content FROM thread_attachments_parts WHERE file_id = " + iFileId);
						
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						
						while(rsAttachment.next()) {
							SerialBlob tempBlob = new SerialBlob(rsAttachment.getBlob("part_content"));
							
							if(null == tempBlob || 0 == tempBlob.length())
								continue;
							
							baos.write(tempBlob.getBytes(1, (int)tempBlob.length()));
						}
						rsAttachment.close();
						stmtAttachment.close();
						
						eAttachmentContent.addText(new String(Base64.encodeBase64(baos.toByteArray())));
						baos.close();
					}
					rsAttachments.close();
					stmtAttachments.close();
				}
				rsMessages.close();
				stmtMessages.close();
				
				// Comments

				Element eComments = eTicket.addElement("comments");
				
				Statement stmtComments = conn.createStatement();
				ResultSet rsComments = stmtComments.executeQuery("SELECT id, date_created, note, user.user_email as worker_email "+
						"FROM next_step "+
						"INNER JOIN user ON (user.user_id=next_step.created_by_agent_id) "+
						"WHERE ticket_id = " + iTicketId + " "+
						"ORDER BY id ASC");
				
				while(rsComments.next()) {
					Integer iCommentCreatedDate = rsComments.getInt("date_created");
					String sCommentAuthor = rsComments.getString("worker_email");
					String sCommentText = Driver.fixMagicQuotes(rsComments.getString("note"));
					
					Element eComment = eComments.addElement("comment");
					eComment.addElement("created_date").setText(iCommentCreatedDate.toString());
					eComment.addElement("author").setText(sCommentAuthor);
					
					Element eCommentContent = eComment.addElement("content");
					eCommentContent.addAttribute("encoding", "base64");
					eCommentContent.setText(new String(Base64.encodeBase64(sCommentText.getBytes())));
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
