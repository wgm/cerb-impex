package com.webgroupmedia.cerb4.exporter.jira.entities;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.codec.binary.Base64;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.cerb4.impex.Configuration;
import com.cerb4.impex.XMLThread;

public class Ticket {
	
	public static SimpleDateFormat RFC822DATEFORMAT = new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z", Locale.US);
	
	private File outputDir;
	private int iCount=0;
	private int iSubDirCount=0;

	private final String cfgOutputDir = Configuration.get("outputDir", "output");
	private final String cfgImportGroupName = Configuration.get("exportToGroup", "support");
	private final String cfgExportEncoding = new String(Configuration.get("exportEncoding", "UTF-8"));
	private final String cfgInputXml = new String(Configuration.get("jiraXmlFile", "input.xml"));
	
	public void export() {
			Document document;
	        SAXReader reader = new SAXReader();
	        try {System.out.println("export");
	        	
	        	File file = new File(cfgInputXml);
	        	
				document = reader.read(file);
				
				System.out.println(document.getText());
				
				Element rootElm = document.getRootElement();
				Element channelElm = rootElm.element("channel");
				
				@SuppressWarnings("unchecked")
				List<Element> itemElms = channelElm.elements("item");
				for (Element itemElm : itemElms) {
					String key = itemElm.elementText("key");

					System.out.println("importing item: " + key);
					Document doc = DocumentHelper.createDocument();
					doc.setXMLEncoding(cfgExportEncoding);

					String title = itemElm.elementText("title");
					String description = itemElm.elementText("description");
					String summary = itemElm.elementText("summary");
					String component = itemElm.elementText("component");
					Integer ticketId = Integer.parseInt(itemElm.element("key").attributeValue("id"));
					
					String isClosed = "0";
					//Open, In Progress, Reopened, Resolved, Closed
					String status = itemElm.elementText("status");
					if(status.equals("Closed") || status.equals("Resolved")) {
						isClosed = "1";
					}
					
					//Unresolved, Fixed, Won't Fix, Duplicate, Incomplete, Cannot Reproduce
//					String resolution = itemElm.elementText("resolution");
					
					
					String rfcCreatedStr = itemElm.elementText("created");
					Date createdDate;
					try {
						createdDate = RFC822DATEFORMAT.parse(rfcCreatedStr);
					} catch (ParseException e1) {
						e1.printStackTrace();
						createdDate =new Date();
					}
					
					String rfcUpdatedStr = itemElm.elementText("updated");
					Date updatedDate;
					try {
						updatedDate = RFC822DATEFORMAT.parse(rfcUpdatedStr);
					} catch (ParseException e1) {
						e1.printStackTrace();
						updatedDate =new Date();
					}
					
					Element eTicket = doc.addElement("ticket");
					
					
					eTicket.addElement("subject").addText(title);
					eTicket.addElement("group").addText(cfgImportGroupName);
					
					if(component==null) {
						component="";
					}
					eTicket.addElement("bucket").addText(component);
					eTicket.addElement("mask").addText(key);
					
					eTicket.addElement("created_date").addText(Long.toString(createdDate.getTime()/1000));
					eTicket.addElement("updated_date").addText(Long.toString(updatedDate.getTime()/1000));
					
					eTicket.addElement("is_waiting").addText("0");
					eTicket.addElement("is_closed").addText(isClosed);
					
					Element eRequesters = eTicket.addElement("requesters");//support@cardlogicgroup.com
					//???? asignee field has username and full name, no email
					eRequesters.addElement("address").setText("support@cardlogicgroup.com");
					
					Element eMessages = eTicket.addElement("messages");
					Element eComments = eTicket.addElement("comments");
					
					
					Element eMessage = eMessages.addElement("message");
					Element eMessageHeaders = eMessage.addElement("headers");
					
					eMessageHeaders.addElement("date").addCDATA(rfcCreatedStr);
					eMessageHeaders.addElement("to").addCDATA("support@cardlogicgroup.com");
					eMessageHeaders.addElement("subject").addCDATA(title);
					eMessageHeaders.addElement("from").addCDATA("support@cardlogicgroup.com");
					
					Element eMessageContent = eMessage.addElement("content");
					eMessageContent.addAttribute("encoding", "base64");
					try {
						
						eMessageContent.setText(new String(Base64.encodeBase64(summary.getBytes(cfgExportEncoding))));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					
					
					Element eMessage2 = eMessages.addElement("message");
					Element eMessageHeaders2 = eMessage2.addElement("headers");
					
					eMessageHeaders2.addElement("date").addCDATA(rfcCreatedStr);
					eMessageHeaders2.addElement("to").addCDATA("support@cardlogicgroup.com");
					eMessageHeaders2.addElement("subject").addCDATA(title);
					eMessageHeaders2.addElement("from").addCDATA("support@cardlogicgroup.com");
					
					Element eMessageContent2 = eMessage2.addElement("content");
					eMessageContent2.addAttribute("encoding", "base64");
					try {
						eMessageContent2.setText(new String(Base64.encodeBase64(description.getBytes(cfgExportEncoding))));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					
					
					
					
					Element commentsElm = itemElm.element("comments");
					if(commentsElm!=null) {

						@SuppressWarnings("unchecked")
						List<Element> commentElms = commentsElm.elements("comment");
						for (Element commentElm : commentElms) {
							
							String commentCreatedStr = commentElm.attributeValue("created");
							Date commentCreatedDate;
							try {
								commentCreatedDate = RFC822DATEFORMAT.parse(commentCreatedStr);
							} catch (ParseException e1) {
								e1.printStackTrace();
								commentCreatedDate = new Date();
							}
							
							String commentText = commentElm.getTextTrim();
							
							
							Element eComment = eComments.addElement("comment");
							
							
							String commentCreateStr = Long.toString(commentCreatedDate.getTime()/1000);
							eComment.addElement("created_date").setText(commentCreateStr);
							
							eComment.addElement("author").addCDATA("support@cardlogicgroup.com");
							
							Element eCommentContent = eComment.addElement("content");
							eCommentContent.addAttribute("encoding", "base64");
							try {
								eCommentContent.setText(new String(Base64.encodeBase64(commentText.getBytes(cfgExportEncoding))));
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
													
						}
					}

					
					
					eMessage.addElement("attachments");
//					Element eAttachments = eMessage.addElement("attachments");
//					Element eAttachment = eAttachments.addElement("attachment");
//					eAttachment.addElement("name").setText(filename);
//					eAttachment.addElement("size").setText(filesize);
//					eAttachment.addElement("mimetype").setText(mimetype);
//					Element eAttachmentContent = eAttachment.addElement("content");
//					eAttachmentContent.addAttribute("encoding", "base64");
//					eAttachmentContent.addText(new String(Base64.encodeBase64(attachmentContent)));
					
					
					String sXmlFileName = getXmlWritePath(ticketId, doc);
					try {
						new XMLThread(doc, sXmlFileName).start();
					} catch(Exception e) {
						e.printStackTrace();
					}
					
				}
				
			} catch (DocumentException e) {
				e.printStackTrace();
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
