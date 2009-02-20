package com.webgroupmedia.cerb4.exporter.zendesk.entities;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpException;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;
import com.webgroupmedia.cerb4.exporter.zendesk.ZendeskConnection;
import com.webgroupmedia.cerb4.exporter.zendesk.ZenRunnable.IResponseHandler;

public class Ticket {
	
	private final int WAIT_TIME_BETWEEN_TICKET_PAGE_REQUESTS = 5000;

	private Map<Integer,String> groupsMap;

	private Map<Integer,String> agentMap;
	
	public static SimpleDateFormat RFC822DATEFORMAT = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);
	
	private File outputDir;
	private int iCount=0;
	private int iSubDirCount=0;

	private boolean lastTicketsPageHit = false;

	private final String cfgOutputDir = Configuration.get("outputDir", "output");
	private final String cfgImportGroupName = Configuration.get("exportToGroup", "Import:Zendesk");
	private final String cfgExportEncoding = new String(Configuration.get("exportEncoding", "ISO-8859-1"));
	private final String cfgZendeskEmail = new String(Configuration.get("zendeskEmailAddress", ""));
	private final String cfgTicketViewId = Configuration.get("zenTicketViewId", "");
	
	public void export() {

		try {

			initGroupsMap();
			
			initAgentMap() ;
			
			Integer page = 0;
			
			//We keep getting pages until we hit the last page
			//However, the end of the loop has a Thread.sleep to slow down the page fetching a bit
			while(!lastTicketsPageHit) {
				//System.out.println("lastTicketsPageHit:" +lastTicketsPageHit);
				++page;
				
				Map<String,String> ticketListParams = new HashMap<String,String>();
				ticketListParams.put("page", page.toString());
				
				//request a page of tickets
				ZendeskConnection.getInstance().requestZendeskDocumentAsync("rules", cfgTicketViewId, ticketListParams, new IResponseHandler() {
	
					
					public void onZenResponse(Document ticketsDocument) {
						//A page of tickets received
						Element ticketsElm = ticketsDocument.getRootElement();
						
						@SuppressWarnings("unchecked")
						List<Element> ticketElms = ticketsElm.elements("ticket");
						
						
						
						if(ticketElms == null || ticketElms.size() == 0) {
							System.out.println("Last Page"); 
							lastTicketsPageHit = true;
							return;
						}
						else {
							System.out.println("Page had "+ticketElms.size());
						}
						
						for (final Element ticketElm : ticketElms) {
	
							
							//get groups
							final Integer ticketId = Integer.parseInt(ticketElm.elementText("nice-id"));
							//System.out.println("Setting ticketId to "+ticketId);
							
							
							final String subject = ticketElm.elementText("subject");
							//cfgImportGroupName
	
							final String groupName = cfgImportGroupName;
							
							Integer groupId = Integer.parseInt(ticketElm.elementText("group-id"));
							final String categoryName = groupsMap.get(groupId);
							
							final String mask = Configuration.get("exportMaskPrefix", "ZEN") + String.format("-%d", ticketId);
							
							final DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
							dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
							String createdDateStr = ticketElm.elementText("created-at");
							//"2007-02-26 20:15:00 +0200"
							
							String recipient = ticketElm.elementText("recipient");
							if(recipient.length() == 0) {
								recipient = cfgZendeskEmail;
							}
							final String ticketToAddress = recipient;
							
							Date tmpCreateDate;;
							try {
								tmpCreateDate = dfm.parse(createdDateStr);
							} catch (ParseException e) {
								e.printStackTrace();
								tmpCreateDate = new Date();
							}
							final Date createDate = tmpCreateDate; 
	
							String updatedDateStr = ticketElm.elementText("updated-at");
							Date tmpUpdatedDate;
							try {
								tmpUpdatedDate = dfm.parse(updatedDateStr);
							} catch (ParseException e) {
								e.printStackTrace();
								tmpUpdatedDate = new Date();
							}
							final Date updatedDate = tmpUpdatedDate;
							
							final Integer isWaiting, isClosed;
							
	//						New=0, Open=1, Pending=2, Solved=3, Closed=4
							Integer statusCode = Integer.parseInt(ticketElm.elementText("status-id")); 
							if(statusCode == 3 || statusCode == 4) {
								isClosed = 1;
								isWaiting = 0;
							}
							else if(statusCode == 2) {
								isClosed = 0;
								isWaiting = 1;
							}
							else {
								isClosed = 0;
								isWaiting = 0;
							}
	
							
							final Integer requesterId = Integer.parseInt(ticketElm.elementText("requester-id"));
							
							//Get the requester's email address from the server
							Document requesterDocument = ZendeskConnection.getInstance().requestZendeskDocumentSerial("users", requesterId.toString(), null);

							Element userElm = requesterDocument.getRootElement();
							final String requesterEmail = userElm.elementText("email");
							

							//Request the ticket document for the currently looped ticket
							ZendeskConnection.getInstance().requestZendeskDocumentAsync("tickets", ticketId.toString(), null, new IResponseHandler() {

								public void onZenResponse(Document ticketDoc) {
									//Ticket document obtained for the currently looped ticketid
									
									//finally start creating our export xml document
									Document doc = DocumentHelper.createDocument();
									doc.setXMLEncoding(cfgExportEncoding);

									//System.out.println("doc created");
									Element eTicket = doc.addElement("ticket");
									//System.out.println("ticket element added to doc");
									
									
									eTicket.addElement("subject").addText(subject);
									eTicket.addElement("group").addText(groupName);
									eTicket.addElement("bucket").addText(categoryName);
									eTicket.addElement("mask").addText(mask);
									
									eTicket.addElement("created_date").addText(Long.toString(createDate.getTime()/1000));
									eTicket.addElement("updated_date").addText(Long.toString(updatedDate.getTime()/1000));
									eTicket.addElement("is_waiting").addText(isWaiting.toString());
									eTicket.addElement("is_closed").addText(isClosed.toString());
									
									Element eRequesters = eTicket.addElement("requesters");
									eRequesters.addElement("address").setText(requesterEmail);
									
									Element eMessages = eTicket.addElement("messages");
									Element eComments = eTicket.addElement("comments");
									
									Element ticketDetailElm = ticketDoc.getRootElement();
																		
									//comments in zendesk contain cerb equivalent of comments AND messages
									Element commentsElm = ticketDetailElm.element("comments");
									@SuppressWarnings("unchecked")
									List<Element> commentElms = commentsElm.elements("comment");
									
									for (Element commentElm : commentElms) {
										String content = commentElm.elementText("value");
										
										String commentCreatedDateStr = commentElm.elementText("created-at");
										Date commentCreatedDate;
										try {
											commentCreatedDate = dfm.parse(commentCreatedDateStr);
										} catch (ParseException e) {
											e.printStackTrace();
											commentCreatedDate = new Date();
										}
										
										Integer authorId = Integer.parseInt(commentElm.elementText("author-id"));
										String author;
										if(authorId.equals(requesterId)) {
											author = requesterEmail;
										}
										else {
											author = agentMap.get(authorId); 
										}
										boolean isPublic = new Boolean(commentElm.elementText("is-public"));

										if(isPublic) {
											Element eMessage = eMessages.addElement("message");
											Element eMessageHeaders = eMessage.addElement("headers");

											String rfcCommentDate = RFC822DATEFORMAT.format(commentCreatedDate);
											eMessageHeaders.addElement("date").addCDATA(rfcCommentDate);
											eMessageHeaders.addElement("to").addCDATA(ticketToAddress);
											eMessageHeaders.addElement("subject").addCDATA(subject);
											
											eMessageHeaders.addElement("from").addCDATA(author);

											Element eMessageContent = eMessage.addElement("content");
											eMessageContent.addAttribute("encoding", "base64");
											try {
												eMessageContent.setText(new String(Base64.encodeBase64(content.toString().getBytes(cfgExportEncoding))));
											} catch (UnsupportedEncodingException e) {
												e.printStackTrace();
											}

											//We only care about attachments on zen-comments -> cerb-messages (not zen-comments -> cerb-comments)
											Element attachmentsElm = commentElm.element("attachments");
											Element eAttachments = eMessage.addElement("attachments");
											
											@SuppressWarnings("unchecked")
											List<Element> attachmentElms = attachmentsElm.elements("attachment");
											
											for (Element attachmentElm : attachmentElms) {
												Integer attachmentId = Integer.parseInt(attachmentElm.elementText("id"));
												String filename = attachmentElm.elementText("filename");
												String filesize = attachmentElm.elementText("size");
												String mimetype = attachmentElm.elementText("content-type");
												
												byte[] attachmentContent=null;
												try {
													attachmentContent = ZendeskConnection.getInstance().requestZendeskAttachment(attachmentId);
												} catch (HttpException e) {
													e.printStackTrace();
												} catch (IOException e) {
													e.printStackTrace();
												}
												
												Element eAttachment = eAttachments.addElement("attachment");
												eAttachment.addElement("name").setText(filename);
												eAttachment.addElement("size").setText(filesize);
												eAttachment.addElement("mimetype").setText(mimetype);
												
												Element eAttachmentContent = eAttachment.addElement("content");
												eAttachmentContent.addAttribute("encoding", "base64");
												
												eAttachmentContent.addText(new String(Base64.encodeBase64(attachmentContent)));
											}
											
											
										}
										else {
											Element eComment = eComments.addElement("comment");
											
											String commentCreateStr = Long.toString(commentCreatedDate.getTime()/1000);
											eComment.addElement("created_date").setText(commentCreateStr);
											
											eComment.addElement("author").addCDATA(author);
											
											Element eCommentContent = eComment.addElement("content");
											eCommentContent.addAttribute("encoding", "base64");
											try {
												eCommentContent.setText(new String(Base64.encodeBase64(content.getBytes(cfgExportEncoding))));
											} catch (UnsupportedEncodingException e) {
												e.printStackTrace();
											}
											
										}
										
									}
									
									String sXmlFileName = getXmlWritePath(ticketId, doc);
									try {
										new XMLThread(doc, sXmlFileName).start();
									} catch(Exception e) {
										e.printStackTrace();
									}
								}
								
							});	
							
							

						}
					}
					
				});
				Thread.sleep(WAIT_TIME_BETWEEN_TICKET_PAGE_REQUESTS);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	
	private void initGroupsMap() {
		groupsMap = new HashMap<Integer,String>();

		Document document = ZendeskConnection.getInstance().requestZendeskDocumentSerial("groups", null, null);

		Element groupsElm = document.getRootElement();
		
		@SuppressWarnings("unchecked")
		List<Element> groupElms = groupsElm.elements("group");
		
		for (Element groupElm : groupElms) {
			Integer groupId = Integer.parseInt(groupElm.elementText("id"));
			String groupName = groupElm.elementText("name");
			groupsMap.put(groupId, groupName);
		}
	}
	
	private void initAgentMap() {
		agentMap = new HashMap<Integer,String>();
		
		initAgentMapForRole(2);//administrators
		initAgentMapForRole(4);//agents
	}
	
	private void initAgentMapForRole(int role) {
		Map<String,String> params = new HashMap<String,String>();
		params.put("role", Integer.toString(role));
		Document document = ZendeskConnection.getInstance().requestZendeskDocumentSerial("users", null, params);
		Element usersElm = document.getRootElement();
		
		if(!usersElm.getName().equals("users")) {
			return;
		}
		
		@SuppressWarnings("unchecked")
		List<Element> userElms = usersElm.elements("user");
		
		for (Element userElm : userElms) {
			Integer userId = Integer.parseInt(userElm.elementText("id"));
			String email = userElm.elementText("email");
			System.out.println("putting agentMap:"+ userId + ":"+email);
			agentMap.put(userId, email);
		}
	}
	
    private synchronized String getXmlWritePath(Integer ticketId, Document doc) {
		if(0 == iCount % 2000) {
			// Make the output subdirectory
			outputDir = new File(cfgOutputDir+"/04-tickets-" + String.format("%06d", ++iSubDirCount));
			outputDir.mkdirs();

			System.out.println("Writing to " + outputDir.getAbsolutePath());
		}
		iCount++;

		return outputDir.getPath() + "/" + String.format("%09d",ticketId) + ".xml";
    }
    
}
