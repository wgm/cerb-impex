package com.webgroupmedia.cerb4.exporter.kayako.entities;

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

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.kayako.Database;
import com.webgroupmedia.cerb4.exporter.kayako.Driver;

public class Ticket {
	public void export() {
		Connection conn = Database.getInstance();

		String cfgOutputDir = Configuration.get("outputDir", "output");
		String cfgImportGroupName = Configuration.get("exportToGroup", "Import:Kayako");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
		
		SimpleDateFormat rfcDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");

		Integer iCount = 0;
		Integer iSubDirCount = 0;		

		Boolean isVerbose = new Boolean(Configuration.get("verbose", "false"));
		Boolean isCfgTicketExcludeOpen = new Boolean(Configuration.get("exportTicketExcludeOpen", "false"));
		Boolean isCfgTicketExcludeClosed = new Boolean(Configuration.get("exportTicketExcludeClosed", "false"));
		
		try {
			Statement stmt = conn.createStatement();
			stmt.execute("set names utf8");
			stmt.close();
			
			
			Statement stmtTickets = conn.createStatement();
			
			String sqlTickets = "SELECT t.ticketid as ticket_id, t.subject, "+
					"t.ticketmaskid as mask, t.dateline as created_date, " +
					"lastactivity as updated_date, t.ticketstatusid, " + //3
					"d.title as department_name, q.email queue_email, t.email original_requester_email " +
					"FROM swtickets t " + 
					"INNER JOIN swdepartments d ON t.departmentid = d.departmentid " +
					"INNER JOIN swemailqueues q ON t.emailqueueid = q.emailqueueid " +
					"WHERE 1=1 " +
					(isCfgTicketExcludeOpen ? " AND t.ticketstatusid = 3 " : "") +
					(isCfgTicketExcludeClosed ? " AND t.ticketstatusid <> 3 " : "") 
			;
			
//			"SELECT t.id as ticket_id, t.subject, "+
//			"t.mask, t.created_date, "+
//			"t.updated_date, t.is_waiting, t.is_closed, "+
//			"team.name as team_name, c.name as category_name " +
//			"FROM ticket t "+
//			"INNER JOIN team ON (team.id = t.team_id) "+
//			"LEFT JOIN category c ON (t.category_id = c.id) "+
//			"WHERE t.is_deleted = 0 "+ 
//			"AND t.spam_training != 'S' "+
//			(isCfgTicketExcludeOpen ? "AND t.is_closed = 1 " : "") +
//			(isCfgTicketExcludeClosed ? "AND t.is_closed = 0 " : "") +
//			"ORDER BY t.id DESC "+
//			""			
			
			
			ResultSet rsTickets = stmtTickets.executeQuery(sqlTickets);
	
			File outputDir = null;
			
			while(rsTickets.next()) {
				Integer iTicketId = rsTickets.getInt("ticket_id");
				String sSubject = Driver.fixMagicQuotes(rsTickets.getString("subject"));
				String sMask = Driver.fixMagicQuotes(rsTickets.getString("mask").trim());
				Integer iCreatedDate = rsTickets.getInt("created_date");
				Integer iUpdatedDate = rsTickets.getInt("updated_date");
				Integer isWaiting = 0;
				Integer isClosed = (rsTickets.getInt("ticketstatusid") == 3) ? 1 : 0;
				String queueEmail = Driver.fixMagicQuotes(rsTickets.getString("queue_email").trim());
				String originalRequesterEmail = Driver.fixMagicQuotes(rsTickets.getString("original_requester_email").trim());
				
				
				String sTeamName = cfgImportGroupName;
				String sCategoryName = Driver.fixMagicQuotes(rsTickets.getString("department_name"));
				
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
					sMask = iTicketId.toString();
				}
				sMask = Configuration.get("exportMaskPrefix", "KYKO") + String.format("-%s", sMask);
				
				eTicket.addElement("subject").addText(sSubject);
				eTicket.addElement("group").addText(sTeamName);
				eTicket.addElement("bucket").addText(sCategoryName);
				eTicket.addElement("mask").addText(sMask);
				eTicket.addElement("created_date").addText(iCreatedDate.toString());
				eTicket.addElement("updated_date").addText(iUpdatedDate.toString());
				eTicket.addElement("is_waiting").addText(isWaiting.toString());
				eTicket.addElement("is_closed").addText(isClosed.toString());
				

				Element eRequesters = eTicket.addElement("requesters");

				//the original requester is not in the recipients table, so add it from the value from the tickets table
				eRequesters.addElement("address").setText(originalRequesterEmail);

				Statement stmtRequesters = conn.createStatement();
				String requesterSQL = "SELECT e.email FROM swticketrecipients r "+
						"INNER JOIN swticketemails e ON r.ticketemailid = e.ticketemailid " +
						"WHERE r.ticketid = " + iTicketId + " ";
				
				ResultSet rsRequesters = stmtRequesters.executeQuery(requesterSQL);
				
				
				while(rsRequesters.next()) {
					String sRequesterAddy = rsRequesters.getString("email");
					eRequesters.addElement("address").setText(sRequesterAddy);
				}
				rsRequesters.close();
				stmtRequesters.close();
				
				Statement stmtMessages = conn.createStatement();
				
				String messageSQL = "SELECT p.ticketpostid, p.contents, p.email from_email, p.subject, " +
						"p.dateline, i.messageid " +
						"FROM swticketposts p " +
						"LEFT JOIN swticketmessageids i ON p.ticketpostid = i.ticketpostid " +
						"WHERE p.ticketid = " + iTicketId + " " + 
						"ORDER BY p.ticketpostid";
				
				ResultSet rsMessages = stmtMessages.executeQuery(messageSQL);

				Element eMessages = eTicket.addElement("messages");
				
				while(rsMessages.next()) {
					Integer messageId = rsMessages.getInt("ticketpostid");
					String strContent = Driver.fixMagicQuotes(rsMessages.getString("contents"));
					
					Integer messageDate = rsMessages.getInt("dateline");
					String sMessageDate = rfcDateFormat.format(new Date(messageDate*1000));
					
					String emailFrom = rsMessages.getString("from_email");
					String subject = rsMessages.getString("subject");
					String messageIdHeader = rsMessages.getString("messageid");
					if(messageIdHeader == null) {
						messageIdHeader = "";
					}
					
					Element eMessage = eMessages.addElement("message");
					Element eMessageHeaders = eMessage.addElement("headers");
					
					eMessageHeaders.addElement("date").addCDATA(sMessageDate);
					eMessageHeaders.addElement("to").addCDATA(queueEmail);
					eMessageHeaders.addElement("from").addCDATA(emailFrom);
					eMessageHeaders.addElement("subject").addCDATA(subject);
					eMessageHeaders.addElement("message-id").addCDATA(messageIdHeader);

					
					
					Element eMessageContent = eMessage.addElement("content");
					eMessageContent.addAttribute("encoding", "base64");
					eMessageContent.setText(new String(Base64.encodeBase64(strContent.getBytes(sExportEncoding))));
					strContent = null;
					
					// Attachments
					Element eAttachments = eMessage.addElement("attachments");
					
					
					
					/////////////////
					
					
					Statement stmtAttachments = conn.createStatement();
					
					String attachmentSQL = "SELECT attachmentid, filename, filesize, filetype "+
							"FROM swattachments " +
							"WHERE ticketpostid = " + messageId + " " +
							"ORDER BY attachmentid ASC";
					
					ResultSet rsAttachments = stmtAttachments.executeQuery(attachmentSQL);
					
					while(rsAttachments.next()) {
						Integer iFileId = rsAttachments.getInt("attachmentid"); 
						String sFileName = Driver.fixMagicQuotes(rsAttachments.getString("filename"));
						String sFileSize = rsAttachments.getString("filesize");
						String sFileType = Driver.fixMagicQuotes(rsAttachments.getString("filetype"));
						
						Element eAttachment = eAttachments.addElement("attachment");
						eAttachment.addElement("name").setText(sFileName);
						eAttachment.addElement("size").setText(sFileSize);
						eAttachment.addElement("mimetype").setText(sFileType);
						
						Element eAttachmentContent = eAttachment.addElement("content");
						eAttachmentContent.addAttribute("encoding", "base64");
						
						// [TODO] Option to ignore huge attachments?
						
						Statement stmtAttachment = conn.createStatement();
						ResultSet rsAttachment = stmtAttachment.executeQuery("SELECT contents FROM swattachmentchunks WHERE attachmentid = " + iFileId + " ORDER BY chunkid ");
						
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						
						while(rsAttachment.next()) {
							SerialBlob tempBlob = new SerialBlob(rsAttachment.getBlob("contents"));
							
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
					
					
					///////
				}
				rsMessages.close();
				stmtMessages.close();
				
				
				// Comments
				Element eComments = eTicket.addElement("comments");
				
				Statement stmtComments = conn.createStatement();
				
				String commentSQL = "SELECT n.ticketnoteid, n.dateline, n.notes, s.email " +
						"FROM swticketnotes n " +
						"INNER JOIN swstaff s ON n.bystaffid = s.staffid " +
						"WHERE n.typeid = " + iTicketId + " "+
						"ORDER BY n.ticketnoteid";
				
				ResultSet rsComments = stmtComments.executeQuery(commentSQL);
				
				while(rsComments.next()) {
					Integer iCommentCreatedDate = rsComments.getInt("dateline");
					String sCommentAuthor = rsComments.getString("email");
					String sCommentText = Driver.fixMagicQuotes(rsComments.getString("notes"));
					
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
