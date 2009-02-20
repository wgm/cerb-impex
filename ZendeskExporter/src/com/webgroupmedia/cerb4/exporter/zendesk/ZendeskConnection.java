package com.webgroupmedia.cerb4.exporter.zendesk;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dom4j.Document;

import com.cerb4.impex.Configuration;
import com.webgroupmedia.cerb4.exporter.zendesk.ZenRunnable.IResponseHandler;

public class ZendeskConnection {
	
	private MultiThreadedHttpConnectionManager connectionManager;
	
	private static ZendeskConnection instance = new ZendeskConnection();
	
	private final static String sZendeskUser = new String(Configuration.get("zendeskUser", ""));
	private final static String sZendeskPassword = new String(Configuration.get("zendeskPassword", ""));
	
	private final Integer MAX_HTTP_CONNECTIONS = Integer.parseInt(Configuration.get("maxHttpConnections", "7"));
	
	private static Cookie loginCookie;
	
	public static ZendeskConnection getInstance() {
		return instance;
	}

	
	public ZendeskConnection() {
		connectionManager = new MultiThreadedHttpConnectionManager();
		
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();
		params.setMaxTotalConnections(MAX_HTTP_CONNECTIONS);

		connectionManager.setParams(params);
		
	}
	
	public void requestZendeskDocumentAsync(String objectType, String objectId, Map<String,String> params,
			IResponseHandler responseHandler) {
		
		//since this is an async request, the document returned by the method below is always null (so we don't return it)
		requestZendeskDocument(objectType, objectId, params, true, responseHandler); 
	}
	
	public Document requestZendeskDocumentSerial(String objectType, String objectId, Map<String,String> params) {
		return requestZendeskDocument(objectType, objectId, params, false, null); 
	}
	
	private Document requestZendeskDocument(String objectType, String objectId, Map<String,String> params,
			boolean isAsync, IResponseHandler responseHandler) {

		
		String cfgHelpdeskURL = Configuration.get("zendeskURL", "");
		
		if(!cfgHelpdeskURL.endsWith("/")) {
			cfgHelpdeskURL = cfgHelpdeskURL + "/";
		}
		
		String url;
		if(objectId == null) {
			url = cfgHelpdeskURL + objectType + ".xml";
		}
		else {
			url = cfgHelpdeskURL + objectType + "/" + objectId + ".xml";
		}
		
		
		if(params != null && params.size() > 0) {
			StringBuffer buffer = new StringBuffer("?");
			boolean firstParam = true;
			for (Entry<String,String> paramsEntry : params.entrySet()) {
				if(!firstParam) 
					buffer.append("&");
				else 
					firstParam = false;
				
				paramsEntry.getKey();
				buffer.append(paramsEntry.getKey());
				buffer.append("=");
				buffer.append(paramsEntry.getValue());
			}
			url += buffer.toString();
		}
		
		return requestZendeskDocumentByUrl(url, isAsync, responseHandler);
	}

	
	private Document requestZendeskDocumentByUrl(String url, boolean isAsync, IResponseHandler responseHandler) { 
		Document zenDoc = null;
		
		HttpClient client = new HttpClient(connectionManager);
		HttpState httpState = new HttpState();

		AuthScope authScope = new AuthScope(null, -1);
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(sZendeskUser, sZendeskPassword);
		httpState.setCredentials(authScope, credentials);
		
		client.setState(httpState);
		System.out.println(url);
		GetMethod method = new GetMethod(url);
			
		ZenRunnable zenRunnable = new ZenRunnable(client, method, responseHandler); 
		if(isAsync) {
			Thread thread = new Thread(zenRunnable);
			thread.start();
		}
		else {
			zenDoc = zenRunnable.requestDoc();
		}
		return zenDoc;
	}
	
	public byte[] requestZendeskAttachment(Integer attachmentId) throws HttpException, IOException {
		
		String cfgHelpdeskURL = Configuration.get("zendeskURL", "");
		if(!cfgHelpdeskURL.endsWith("/")) {
			cfgHelpdeskURL = cfgHelpdeskURL + "/";
		}
		
		String url = cfgHelpdeskURL + "attachments/" + attachmentId;
		
		HttpClient httpClient = new HttpClient(connectionManager);
		HttpState httpState = new HttpState();

		
		AuthScope authScope = new AuthScope(null, -1);
		System.out.println("credential:"+sZendeskUser);
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(sZendeskUser, sZendeskPassword);
		httpState.setCredentials(authScope, credentials);
		
		//Zen doesn't let us download attachments with only the simple auth. We have to login to their website
		Cookie loginCookie = getZendeskCookie();
		System.out.println("cookie:"+loginCookie.getName()+":"+loginCookie.getValue());
		httpState.addCookie(loginCookie);
		
		httpClient.setState(httpState);
		System.out.println(url);
		GetMethod method = new GetMethod(url);
		
		httpClient.executeMethod(method);
		
		InputStream in=method.getResponseBodyAsStream();
		ByteArrayOutputStream baos= new ByteArrayOutputStream();
		int c;
		while ((c = in.read()) != -1) {
			baos.write(c);
		}
		return baos.toByteArray();
	}
	
	public static Cookie getZendeskCookie() {
		if(loginCookie != null) {
			return loginCookie;
		}
		
		Cookie cookieCopy = null;
		
		String cfgHelpdeskURL = Configuration.get("zendeskURL", "");
		if(!cfgHelpdeskURL.endsWith("/")) {
			cfgHelpdeskURL = cfgHelpdeskURL + "/";
		}

		String url="";
		if(cfgHelpdeskURL.startsWith("http:")) {
			url = "https" + cfgHelpdeskURL.substring(4);
		}
		url +="access/login";
		//url="https://extraice.zendesk.com/access/login";
		
		HttpClient httpClient = new HttpClient();
		
		httpClient.getParams().setParameter(HttpMethodParams.USER_AGENT, 
		"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.6pre) Gecko/2009011606 Firefox/3.1");

		
		PostMethod method = new PostMethod(url);
		method.addParameter("user[email]", sZendeskUser);
		method.addParameter("user[password]", sZendeskPassword);
		method.addParameter("commit", "&nbsp;Login&nbsp;");
		
		try {
			method.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
			
			System.out.println("executing:"+url);
			
			httpClient.executeMethod(method);
			Cookie[] cookies = httpClient.getState().getCookies();
			for (Cookie cookie : cookies) {
				if(cookie.getName().equals("_love_your_new_zendesk_session")) {
					cookieCopy = new Cookie(cookie.getDomain(), cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getExpiryDate(), cookie.getSecure());
					break;
				}
			}
			
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		loginCookie = cookieCopy;
		return cookieCopy;
	}
	
}
