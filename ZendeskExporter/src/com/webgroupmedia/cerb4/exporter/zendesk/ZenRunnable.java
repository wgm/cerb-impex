package com.webgroupmedia.cerb4.exporter.zendesk;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

public class ZenRunnable implements Runnable {

	private Document document;
	private IResponseHandler responseHandler;
	private HttpClient httpClient;
	private final GetMethod method;
	
	
	public interface IResponseHandler {
		public void onZenResponse(Document document);
	}
	
	public ZenRunnable(HttpClient httpClient, GetMethod method, IResponseHandler responseHandler) {
		this.httpClient = httpClient;
		this.method = method;
		this.responseHandler = responseHandler;
	}
	
	public void run() {
		requestDoc();
	}
	
	public Document requestDoc() {
		InputStream inputStream;
		try {
			
			
			httpClient.executeMethod(method);
			inputStream = method.getResponseBodyAsStream();
			
			if (HttpStatus.SC_OK != method.getStatusCode()) {
				String errorMessage = "Connection Error (code: " + method.getStatusCode() + "): " + method.getStatusText();
				System.out.println(errorMessage);
				return null;
			}
				
			
	        SAXReader reader = new SAXReader();
	        try {
				document = reader.read(inputStream);
				if(responseHandler != null) {
					responseHandler.onZenResponse(document);
				}
			} catch (DocumentException e) {
				e.printStackTrace();
			}
			
			
		} catch (HttpException e) {
			e.printStackTrace();
			//throw new ZenDocumentFetchException("HttpException accessing zendesk xml: "+ method.getURI().toString(), e);
		} catch (IOException e) {
			e.printStackTrace();
			//throw new ZenDocumentFetchException("IOException accessing zendesk xml: "+ method.getURI().toString(), e);
		}
		finally {
			if(method!=null) {
				method.releaseConnection();
			}
		}
		return document;

	}

}
