package com.cerb4.exporter.entities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.exporter.Database;
import com.cerb4.exporter.Driver;
import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;

public class Ticket {
	public void export() {
		Connection conn = Database.getInstance();
		//String cfgImportGroupName = Configuration.get("exportToGroup", "Import:Cerb4");
		String cfgCerb4HomeDir = Configuration.get("cerb4HomeDir", "");
		
		String cfgOutputDir = Configuration.get("outputDir", "output");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		//SimpleDateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

		Integer iCount = 0;
		Integer iSubDirCount = 0;		

		Boolean isVerbose = new Boolean(Configuration.get("verbose", "false"));
		Boolean isCfgTicketExcludeOpen = new Boolean(Configuration.get("exportTicketExcludeOpen", "false"));
		Boolean isCfgTicketExcludeClosed = new Boolean(Configuration.get("exportTicketExcludeClosed", "false"));
		
		try {
			Statement stmtTickets = conn.createStatement();
			ResultSet rsTickets = stmtTickets.executeQuery("SELECT t.id as ticket_id, t.subject, "+
				"t.mask, t.created_date, "+
				"t.updated_date, t.is_waiting, t.is_closed, "+
				"team.name as team_name, c.name as category_name " +
				"FROM ticket t "+
				"INNER JOIN team ON (team.id = t.team_id) "+
				"LEFT JOIN category c ON (t.category_id = c.id) "+
				"WHERE t.is_deleted = 0 "+ 
				"AND t.spam_training != 'S' "+
				(isCfgTicketExcludeOpen ? "AND t.is_closed = 1 " : "") +
				(isCfgTicketExcludeClosed ? "AND t.is_closed = 0 " : "") +
				"ORDER BY t.id DESC "+
				"");
	
			File outputDir = null;
			
			while(rsTickets.next()) {
				Integer iTicketId = rsTickets.getInt("ticket_id");
				String sSubject = Driver.fixMagicQuotes(rsTickets.getString("subject"));
				String sMask = Driver.fixMagicQuotes(rsTickets.getString("mask").trim());
				Integer iCreatedDate = rsTickets.getInt("created_date");
				Integer iUpdatedDate = rsTickets.getInt("updated_date");
				Integer isWaiting = rsTickets.getInt("is_waiting");
				Integer isClosed = rsTickets.getInt("is_closed");
				
				String sTeamName = Driver.fixMagicQuotes(rsTickets.getString("team_name"));
				String sCategoryName = Driver.fixMagicQuotes(rsTickets.getString("category_name"));
				
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
				
				if(0 == sMask.length()) {
					sMask = Configuration.get("exportMaskPrefix", "CERB4") + String.format("-%d", iTicketId);
				}
				
				eTicket.addElement("subject").addText(sSubject);
				eTicket.addElement("group").addText(sTeamName);
				eTicket.addElement("bucket").addText(sCategoryName);
				eTicket.addElement("mask").addText(sMask);
				eTicket.addElement("created_date").addText(iCreatedDate.toString());
				eTicket.addElement("updated_date").addText(iUpdatedDate.toString());
				eTicket.addElement("is_waiting").addText(isWaiting.toString());
				eTicket.addElement("is_closed").addText(isClosed.toString());
				
				Statement stmtRequesters = conn.createStatement();
				ResultSet rsRequesters = stmtRequesters.executeQuery("SELECT a.email "+
					"FROM requester r "+
					"INNER JOIN address a ON (a.id=r.address_id) "+
					"WHERE r.ticket_id = " + iTicketId + " " 
					);
				
				Element eRequesters = eTicket.addElement("requesters");
				
				while(rsRequesters.next()) {
					String sRequesterAddy = rsRequesters.getString("email");
					eRequesters.addElement("address").setText(sRequesterAddy);
				}
				rsRequesters.close();
				stmtRequesters.close();
				
				Statement stmtMessages = conn.createStatement();
				
				ResultSet rsMessages = stmtMessages.executeQuery("SELECT m.id as message_id, " +
						"mc.content "+
						"FROM message m "+
						"INNER JOIN message_content mc ON (m.id=mc.message_id) "+
						"WHERE m.ticket_id = "  + iTicketId + " " +
						"ORDER BY m.id ASC");

				
				
				Element eMessages = eTicket.addElement("messages");
				
				while(rsMessages.next()) {
					Integer messageId = rsMessages.getInt("message_id");
					String strContent = rsMessages.getString("content"); 
					
					Element eMessage = eMessages.addElement("message");
					Element eMessageHeaders = eMessage.addElement("headers");

					Statement stmtHeaders = conn.createStatement();
					ResultSet rsHeaders = stmtHeaders.executeQuery("SELECT header_name, header_value " +
							"FROM message_header "+
							"WHERE message_id = "  + messageId + " " +
							"ORDER BY message_id ASC");
					
					while(rsHeaders.next()) {
						String sHeaderName = rsHeaders.getString("header_name");
						String sHeaderValue = rsHeaders.getString("header_value");
						
						if(sHeaderName.equals("date") 
								|| sHeaderName.equals("to")
								|| sHeaderName.equals("from")
								|| sHeaderName.equals("subject")
								|| sHeaderName.equals("message-id")) {
							eMessageHeaders.addElement(sHeaderName).addCDATA(sHeaderValue);
						}						
					}
					
					
					Element eMessageContent = eMessage.addElement("content");
					eMessageContent.addAttribute("encoding", "base64");
					eMessageContent.setText(new String(Base64.encodeBase64(strContent.getBytes(sExportEncoding))));
					strContent = null;
					
					// Attachments
					Element eAttachments = eMessage.addElement("attachments");
					
					Statement stmtAttachments = conn.createStatement();
					ResultSet rsAttachments = stmtAttachments.executeQuery("SELECT id, display_name, filepath, file_size, mime_type "+
						"FROM attachment " +
						"WHERE display_name != 'message_source.xml' " + 
						"AND display_name != 'html_mime_part.html' " + 
						"AND display_name != 'message_headers.txt' " + 
						"AND message_id = " + messageId + " " +
						"ORDER BY id ASC");
					
					while(rsAttachments.next()) {
						//Integer iFileId = rsAttachments.getInt("id"); 
						String sFileName = Driver.fixMagicQuotes(rsAttachments.getString("display_name"));
						String sFileSize = rsAttachments.getString("file_size");
						String sFilePath = rsAttachments.getString("filepath");
						String sMimeType = rsAttachments.getString("mime_type");
						
						Element eAttachment = eAttachments.addElement("attachment");
						eAttachment.addElement("name").setText(sFileName);
						eAttachment.addElement("size").setText(sFileSize);
						eAttachment.addElement("mimetype").setText(sMimeType);
						
						
						Element eAttachmentContent = eAttachment.addElement("content");
						eAttachmentContent.addAttribute("encoding", "base64");
						
						// [TODO] Option to ignore huge attachments?
						if(cfgCerb4HomeDir.charAt(cfgCerb4HomeDir.length()-1) != File.separatorChar) {
							cfgCerb4HomeDir += File.separatorChar;
						}
						
						
						String filePath = cfgCerb4HomeDir + "storage/attachments/" + sFilePath;
						File attachmentFile = new File(filePath);
						
						
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
					
					rsAttachments.close();
					stmtAttachments.close();
				}
				rsMessages.close();
				stmtMessages.close();
				
				// Comments
				Element eComments = eTicket.addElement("comments");
				
				Statement stmtComments = conn.createStatement();
				ResultSet rsComments = stmtComments.executeQuery("SELECT tc.id comment_id, tc.created, tc.comment, a.email as worker_email "+
						"FROM ticket_comment tc "+
						"INNER JOIN address a ON (tc.address_id=a.id) "+
						"WHERE ticket_id = " + iTicketId + " "+
						"ORDER BY tc.id ASC");
				
				while(rsComments.next()) {
					Integer iCommentCreatedDate = rsComments.getInt("created");
					String sCommentAuthor = rsComments.getString("worker_email");
					String sCommentText = Driver.fixMagicQuotes(rsComments.getString("comment"));
					
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
