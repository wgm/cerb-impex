package com.cerb4.impex.exporters;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.cerb4.impex.Database;

public class Ticket {
	public void export() {
		Connection conn = Database.getInstance();

		Integer iCount = 0;
		Integer iSubDir = 0;
		
		try {
			ResultSet rsTickets = conn.createStatement().executeQuery("SELECT t.ticket_id, t.ticket_subject, t.ticket_mask, UNIX_TIMESTAMP(t.ticket_date) as ticket_date, "+
				"UNIX_TIMESTAMP(ticket_last_date) as ticket_updated, t.is_waiting_on_customer, t.is_closed "+
				"FROM ticket t "+
				"WHERE t.is_deleted = 0 AND t.ticket_id=1473 "+ // don't export deleted
				"ORDER BY t.ticket_id ASC "+
				"LIMIT 0,50");
	
			File outputDir = null;
			
			while(rsTickets.next()) {
				
				if(0 == iCount % 2000 || 0 == iCount) {
					iSubDir++;
					
					// Make the output subdirectory
					outputDir = new File("output/tickets/" + String.format("%06d", iSubDir));
					outputDir.mkdirs();
				}
				
				Document doc = DocumentHelper.createDocument();
				Element eTicket = doc.addElement("ticket");
				
				Integer iTicketId = rsTickets.getInt("ticket_id");
				String sSubject = rsTickets.getString("ticket_subject");
				String sMask = rsTickets.getString("ticket_mask");
				Integer iCreatedDate = rsTickets.getInt("ticket_date");
				Integer iUpdatedDate = rsTickets.getInt("ticket_updated");
				Integer isWaiting = rsTickets.getInt("is_waiting_on_customer");
				Integer isClosed = rsTickets.getInt("is_closed");
				
				if(sMask.isEmpty()) {
					// [TODO] Make this prefix configurable
					sMask = String.format("CERB3-%06d", iTicketId);
				}
				
				eTicket.addElement("subject").addText(sSubject);
				eTicket.addElement("mask").addText(sMask);
				eTicket.addElement("created_date").addText(iCreatedDate.toString());
				eTicket.addElement("updated_date").addText(iUpdatedDate.toString());
				eTicket.addElement("is_waiting").addText(isClosed.toString());
				eTicket.addElement("is_closed").addText(isWaiting.toString());
				
				ResultSet rsMessages = conn.createStatement().executeQuery("SELECT thread_id, thread_message_id, thread_address_id, address.address_address as sender_from, "+
					"UNIX_TIMESTAMP(thread_date) as thread_date, is_agent_message "+
					"FROM thread "+
					"INNER JOIN address ON (thread.thread_address_id=address.address_id) "+
					"WHERE ticket_id = "  + iTicketId + " " +
					"ORDER BY thread_id ASC");
				
				Element eMessages = eTicket.addElement("messages");
				
				while(rsMessages.next()) {
					Integer iThreadId = rsMessages.getInt("thread_id");
					String sThreadSender = rsMessages.getString("sender_from");
					Integer iThreadDate = rsMessages.getInt("thread_date");
//					Integer iThreadWorker = rsMessages.getInt("is_agent_message");
					
					Element eMessage = eMessages.addElement("message");
					eMessage.addElement("created_date").addText(iThreadDate.toString());
					eMessage.addElement("from").addText(sThreadSender);
//					eMessage.addElement("").addText("");
					
					// Content
					ResultSet rsContents = conn.createStatement().executeQuery("SELECT thread_content_part "+
						"FROM thread_content_part "+
						"WHERE thread_id = " + iThreadId + " " +
						"ORDER BY content_id ASC");
					
					StringBuilder strContent = new StringBuilder();
					
					while(rsContents.next()) {
						strContent.append(rsContents.getString("thread_content_part"));
					}
					
					eMessage.addElement("content").setText(strContent.toString());
					
					// Attachments
					Element eAttachments = eMessage.addElement("attachments");
					
					ResultSet rsAttachments = conn.createStatement().executeQuery("SELECT file_id, file_name, file_size "+
						"FROM thread_attachments " +
						"WHERE thread_id = " + iThreadId + " " +
						"ORDER BY file_id ASC");
					
					while(rsAttachments.next()) {
						Integer iFileId = rsAttachments.getInt("file_id"); 
						String sFileName = rsAttachments.getString("file_name");
						String sFileSize = rsAttachments.getString("file_size");
						
						Element eAttachment = eAttachments.addElement("attachments");
						eAttachment.addElement("name").setText(sFileName);
						eAttachment.addElement("size").setText(sFileSize);
						
						Element eAttachmentContent = eAttachment.addElement("content");
						eAttachmentContent.addAttribute("encoding", "base64");
						
						ResultSet rsAttachment = conn.createStatement().executeQuery("SELECT part_content FROM thread_attachments_parts WHERE file_id = " + iFileId);
						
						StringBuilder str = new StringBuilder();
						
						while(rsAttachment.next()) {
							str.append(rsAttachment.getString("part_content"));
						}
						
						eAttachmentContent.setText(new String(Base64.encodeBase64(str.toString().getBytes())));
					}
				}
				
				// Comments

				Element eComments = eTicket.addElement("comments");
				
				ResultSet rsComments = conn.createStatement().executeQuery("SELECT id, date_created, note, user.user_email as worker_email "+
						"FROM next_step "+
						"INNER JOIN user ON (user.user_id=next_step.created_by_agent_id) "+
						"WHERE ticket_id = " + iTicketId + " "+
				"ORDER BY id ASC");
				
				while(rsComments.next()) {
					Integer iCommentCreatedDate = rsComments.getInt("date_created");
					String sCommentAuthor = rsComments.getString("worker_email");
					String sCommentText = rsComments.getString("note");
					
					Element eComment = eComments.addElement("comment");
					eComment.addElement("created_date").setText(iCommentCreatedDate.toString());
					eComment.addElement("author").setText(sCommentAuthor);
					eComment.addElement("text").setText(sCommentText);
				}
				
				System.out.println(doc.asXML());
				
				XMLWriter writer = new XMLWriter(new FileWriter(outputDir.getPath() + "/" + iTicketId + ".xml"), OutputFormat.createPrettyPrint()); 
				writer.write(doc);
				writer.close();
				
				iCount++;
			}
			
			rsTickets.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}
