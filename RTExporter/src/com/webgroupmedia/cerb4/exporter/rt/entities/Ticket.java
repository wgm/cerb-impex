package com.webgroupmedia.cerb4.exporter.rt.entities;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.rt.Database;
import com.webgroupmedia.cerb4.exporter.rt.Driver;


public class Ticket {
	private Map<String, String> queueDefaultEmailsMap;

	public void export() {
		initQueueAddressMap();		
		
		Connection conn = Database.getInstance();

		String cfgOutputDir = Configuration.get("outputDir", "output");
		String cfgImportGroupName = Configuration.get("exportToGroup", "Import:RT");
		String sExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));


		Integer iCount = 0;
		Integer iSubDirCount = 0;		

		Boolean isVerbose = new Boolean(Configuration.get("verbose", "false"));
		Boolean isCfgTicketExcludeOpen = new Boolean(Configuration.get("exportTicketExcludeOpen", "false"));
		Boolean isCfgTicketExcludeClosed = new Boolean(Configuration.get("exportTicketExcludeClosed", "false"));
		
		try {
			Statement stmtTickets = conn.createStatement();
			
			String sqlTickets = "SELECT t.id as ticket_id, t.Subject, "+
					"t.Created, " +
					"t.LastUpdated, t.Status, " + //3
					"q.Name as queue_name " +
					"FROM Tickets t " + 
					"INNER JOIN Queues q ON t.Queue = q.id " +
					"WHERE t.Status <> 'deleted' " +
					(isCfgTicketExcludeOpen ? " AND t.Status = 'Closed' " : "") +
					(isCfgTicketExcludeClosed ? " AND t.Status <> 'Closed' " : "") 
			;
			
			ResultSet rsTickets = stmtTickets.executeQuery(sqlTickets);
	
			File outputDir = null;
//			
			while(rsTickets.next()) {
				Integer iTicketId = rsTickets.getInt("ticket_id");
				String ticketSubject = Driver.fixMagicQuotes(rsTickets.getString("Subject"));
				
				String sMask = Configuration.get("exportMaskPrefix", "RT") + String.format("-%d", iTicketId);
				Long iCreatedDate = rsTickets.getDate("Created").getTime()/1000;
				Long iUpdatedDate = rsTickets.getDate("LastUpdated").getTime()/1000;

				Integer isClosed = 0, isWaiting = 0;
				String status = rsTickets.getString("Status");

				if(status.equals("resolved") || status.equals("rejected")) {
					isClosed = 1;
				}
				else if(status.equals("stalled")) {
					isWaiting = 1;
				}
				//if status is 'new' or 'open' than it will be imported as not closed, and not waiting
				

				
				
				String sTeamName = cfgImportGroupName;
				String sCategoryName = Driver.fixMagicQuotes(rsTickets.getString("queue_name"));

				
				Document doc = DocumentHelper.createDocument();
				Element eTicket = doc.addElement("ticket");
				doc.setXMLEncoding(sExportEncoding);
				
//				
				eTicket.addElement("subject").addText(ticketSubject);
				eTicket.addElement("group").addText(sTeamName);
				eTicket.addElement("bucket").addText(sCategoryName);
				eTicket.addElement("mask").addText(sMask);
				eTicket.addElement("created_date").addText(iCreatedDate.toString());
				eTicket.addElement("updated_date").addText(iUpdatedDate.toString());
				eTicket.addElement("is_waiting").addText(isWaiting.toString());
				eTicket.addElement("is_closed").addText(isClosed.toString());

				//TODO get requesters				

				Element eRequesters = eTicket.addElement("requesters");

//				//the original requester is not in the recipients table, so add it from the value from the tickets table
//				eRequesters.addElement("address").setText(originalRequesterEmail);

				Statement stmtRequesters = conn.createStatement();
				
				String requesterSQL = "SELECT DISTINCT Users.EmailAddress " + 
						"FROM Users JOIN Principals Principals  ON ( Principals.id = Users.id ) " + 
						"JOIN GroupMembers  ON ( GroupMembers.MemberId = Principals.id )   " +
						"JOIN Groups ON Groups.id = GroupMembers.GroupId " +
						"WHERE (Principals.Disabled = '0') " + 
						"AND (Principals.PrincipalType = 'User') " +
						"AND Groups.Domain='RT::Ticket-Role' " + 
						"AND Groups.Type='Requestor' " + 
						"AND Groups.Instance="+iTicketId+" ";
				
				ResultSet rsRequesters = stmtRequesters.executeQuery(requesterSQL);
				
				
				while(rsRequesters.next()) {
					String sRequesterAddy = rsRequesters.getString("EmailAddress");
					eRequesters.addElement("address").setText(sRequesterAddy);
				}
				rsRequesters.close();
				stmtRequesters.close();
				
				Statement stmtMessages = conn.createStatement();

				
				String messageSQL = "SELECT DISTINCT a.id, a.Content, a.Headers, a.Subject, a.MessageId, a.Created, u.EmailAddress, a_parent.MessageId ParentMessageId " +
						"FROM Attachments a " +
						"JOIN Transactions tr  ON ( tr.id = a.TransactionId ) " + 
						"JOIN Tickets t ON ( t.id = tr.ObjectId ) " + 
						"JOIN Users u ON a.Creator = u.id " + 
						"LEFT JOIN Attachments a_parent ON a.Parent = a_parent.id " + 
						"WHERE t.EffectiveId = '"+iTicketId+"' " +
						"AND tr.ObjectType = 'RT::Ticket' " +
						"AND (a.ContentType = 'text/plain' OR a.ContentType LIKE 'message/%' OR a.ContentType = 'text') " +  
						"AND a.Content <> '' " +
						"AND (tr.Type='Create' OR tr.Type='Correspond') " +
						"ORDER BY a.id ASC";

				ResultSet rsMessages = stmtMessages.executeQuery(messageSQL);

				Element eMessages = eTicket.addElement("messages");
				
				while(rsMessages.next()) {
					
					Integer messageId = rsMessages.getInt("id");

					String strContent = Driver.fixMagicQuotes(rsMessages.getString("Content"));
					String creatorEmail = Driver.fixMagicQuotes(rsMessages.getString("EmailAddress"));
					
					String subject = rsMessages.getString("Subject");
					if(subject.trim().length()==0) {
						subject = ticketSubject;
					}
					String messageIdHeader = rsMessages.getString("MessageId");
					if(messageIdHeader == null || messageIdHeader.length() ==0) {
						messageIdHeader = rsMessages.getString("ParentMessageId");
					}
					
					Long createTimestamp = rsMessages.getTimestamp("Created").getTime();

					SimpleDateFormat RFC822DATEFORMAT = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);
					String rfcCreateDate = RFC822DATEFORMAT.format(createTimestamp);

					String headers = rsMessages.getString("Headers");
					String[] headersArr = headers.split("[\r\n]");
					
					String headerDate=rfcCreateDate;
					String headerTo="";//will equal the header value, or else later fall back on a queue email mapping from the config file
					String headerFrom=creatorEmail;//if no from header in the message, use the creator's email address
					
					for (String header : headersArr) {
						String[] headerArr = header.split(":", 2);

						if(headerArr.length > 0) {
							String headerName = headerArr[0].trim();
							String headerVal="";
							if(headerArr.length > 1) {
								headerVal=headerArr[1].trim();
							}
							if(headerName.equals("Date")) {
								headerDate = headerVal;
							}
							else if(headerName.equals("To")) {
								headerTo = headerVal;
							}
							else if(headerName.equals("From")) {
								headerFrom = headerVal;
							}
						}
					}
					
					if(headerTo.length()==0) {
						String queueEmailLookup = queueDefaultEmailsMap.get(sCategoryName);//lookup queue email by queuename
						if(queueEmailLookup != null) {
							headerTo = queueEmailLookup;
						}
					}
					
					Element eMessage = eMessages.addElement("message");
					Element eMessageHeaders = eMessage.addElement("headers");
					
					eMessageHeaders.addElement("date").addCDATA(headerDate);
					eMessageHeaders.addElement("to").addCDATA(headerTo);
					eMessageHeaders.addElement("from").addCDATA(headerFrom);
					eMessageHeaders.addElement("subject").addCDATA(subject);
					eMessageHeaders.addElement("message-id").addCDATA(messageIdHeader);
					
					Element eMessageContent = eMessage.addElement("content");
					eMessageContent.addAttribute("encoding", "base64");
					eMessageContent.setText(new String(Base64.encodeBase64(strContent.getBytes())));
					strContent = null;
					
					
					
//					
					// Attachments
					Element eAttachments = eMessage.addElement("attachments");
					Statement stmtAttachments = conn.createStatement();
					
					String attachmentSQL = "SELECT a.id, a.Filename, a.ContentType, a.Content " +
							"FROM Attachments m "+
							"JOIN Attachments a ON m.TransactionId = a.TransactionId "+ 
							"AND a.Filename <> '' " +
							"AND m.id = "+messageId+" ";						

					ResultSet rsAttachments = stmtAttachments.executeQuery(attachmentSQL);
					
					while(rsAttachments.next()) {
						//Integer iFileId = rsAttachments.getInt("id"); 
						String sFileName = Driver.fixMagicQuotes(rsAttachments.getString("Filename"));
						String sFileType = Driver.fixMagicQuotes(rsAttachments.getString("ContentType"));
						
						Element eAttachment = eAttachments.addElement("attachment");
						eAttachment.addElement("name").setText(sFileName);
						
						eAttachment.addElement("mimetype").setText(sFileType);
						
						Element eAttachmentContent = eAttachment.addElement("content");
						eAttachmentContent.addAttribute("encoding", "base64");
						
						SerialBlob tempBlob = new SerialBlob(rsAttachments.getBlob("content"));
						
						long blobLength = tempBlob.length();
						
						if(null == tempBlob || 0 == blobLength)
							continue;
							
						eAttachmentContent.addText(new String(Base64.encodeBase64(tempBlob.getBytes(1, (int)tempBlob.length()))));
						
						eAttachment.addElement("size").setText(Long.toString(blobLength));
						
					}
					rsAttachments.close();
					stmtAttachments.close();
						
				}
				rsMessages.close();
				stmtMessages.close();
						
				// Comments
				Element eComments = eTicket.addElement("comments");
				
				Statement stmtComments = conn.createStatement();
				
				String commentSQL = "SELECT DISTINCT a.id, a.Content, a.Headers, a.Subject, a.MessageId, a.Created, u.EmailAddress " +
				"FROM Attachments a " +
				"JOIN Transactions tr  ON ( tr.id = a.TransactionId ) " + 
				"JOIN Tickets t ON ( t.id = tr.ObjectId ) " +  
				"LEFT JOIN Users u ON a.Creator = u.id " +
				"WHERE t.EffectiveId = '"+iTicketId+"' " +
				"AND tr.ObjectType = 'RT::Ticket' " +
				"AND (a.ContentType = 'text/plain' OR a.ContentType LIKE 'message/%' OR a.ContentType = 'text') " +  
				"AND a.Content <> '' " +
				"AND tr.Type='Comment'  " +
				"ORDER BY a.id ASC";
				
				ResultSet rsComments = stmtComments.executeQuery(commentSQL);
				
				while(rsComments.next()) {
					Long commentCreatedDate = rsComments.getTimestamp("Created").getTime()/1000;
					String commentAuthor = rsComments.getString("EmailAddress");
					String commentText = Driver.fixMagicQuotes(rsComments.getString("Content"));
					
					Element eComment = eComments.addElement("comment");
					eComment.addElement("created_date").setText(commentCreatedDate.toString());
					eComment.addElement("author").setText(commentAuthor);
					
					Element eCommentContent = eComment.addElement("content");
					eCommentContent.addAttribute("encoding", "base64");
					eCommentContent.setText(new String(Base64.encodeBase64(commentText.getBytes())));
				}
				rsComments.close();
				stmtComments.close();	
				
				
				if(0 == iCount % 2000) {
					// Make the output subdirectory
					outputDir = new File(cfgOutputDir+"/04-tickets-" + String.format("%06d", ++iSubDirCount));
					outputDir.mkdirs();
	
					if(!isVerbose)
						System.out.println("Writing to " + outputDir.getAbsolutePath());
				}
				
				
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
	
	private void initQueueAddressMap() {
		queueDefaultEmailsMap = new HashMap<String,String>();
		String queueEmailsStr = new String(Configuration.get("queueEmails", ""));		
		
		String[] queueEmailMappings = queueEmailsStr.split(",");
		for (String queueEmailEntry : queueEmailMappings) {
			String[] queueEmailArr = queueEmailEntry.trim().split(":",2);
			
			if(queueEmailArr.length == 2) {
				queueDefaultEmailsMap.put(queueEmailArr[0], queueEmailArr[1]);
			}
		}
	}
}
