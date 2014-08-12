package com.github.abusalam.android.projectaio.ajax;
/*
 * @author TonyIvanov (telamohn@gmail.com) 
 */

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class Request extends AsyncTask<Object,Void,Transport> {
	// Redundant Constants probably already defined at other places.
	public final static String GET ="GET";
	public final static String POST ="POST";
	
	public final static String CTYPE_JSON = "application/json";
	public final static String CTYPE_FORM = "application/x-www-form-urlencoded";
	public final static String CTYPE_PLAIN = "text/plain";	
	
	private HashMap<String,String> headers;
	//private static AndroidHttpClient client;
	BasicHttpContext context;
	private String method; 
	HttpRequestBase request;
	public Transport transport;
	
	private String url;
	private HttpEntity params;
	private IOException lastError;

	
	public Request(String url){
		context= new BasicHttpContext();

		this.url = url;
		headers = new HashMap<String,String>();
		headers.put("Accept",CTYPE_JSON);
	}
	
	/**
	 * Returns a threadsafe client.
	 * Courtesy of Jason Hudgins (http://foo.jasonhudgins.com/2010/03/http-connections-revisited.html)
	 * @return
	 */
	public static DefaultHttpClient getThreadSafeClient() {

	    DefaultHttpClient client = new DefaultHttpClient();
	    ClientConnectionManager mgr = client.getConnectionManager();
	    HttpParams params = client.getParams();

	    client = new DefaultHttpClient(
	        new ThreadSafeClientConnManager(params,
	            mgr.getSchemeRegistry()), params);

	    return client;
	}
	
	@Override
	protected Transport doInBackground(Object... args){
		method = ((String)args[0]).toUpperCase();
		if(args.length > 1 && method.equalsIgnoreCase(POST)){
			setParams(args[1]);
		}
		
		if(method.equalsIgnoreCase(POST)){
			request = new HttpPost(url);		
			((HttpPost)request).setEntity(params);
			
		} else { //Defaults to GET.
			request = new HttpGet(url);
		}

		for(String k:headers.keySet()){
			request.setHeader(k,headers.get(k));
			Log.d("Ajax.Request","Header: "+k+" : "+headers.get(k));
		}
		
		try {
			transport = new Transport(getThreadSafeClient().execute(request));
			//client.close();
		} catch (IOException e) {
			lastError = e;
			e.printStackTrace();
		}	
		return transport;
	}

	protected void onPostExecute(Transport transport){
		if(lastError == null){
			if(transport.getStatus() < 300 && transport.getStatus() >= 200){				
				onSuccess(transport);
			}else{
				onFailure(transport);
			}
			onComplete(transport);			
		}else{
			onError(lastError);
		}
	}
	
	/** 
	 * 	This callback gets executed when a request completes
	 * 	disregarding of http response code.
	 *  Override this method during instantiation
	 *  to add onComplete handling.
	 * @param transport Response data
	 */
	protected void onComplete(Transport transport){
		
	}
	/** 
	 * 	This callback gets executed when an IOException
	 *  is thrown.
	 *  Override this method during instantiation
	 *  to add onError handling.
	 * @param ex Thrown IOException.
	 */	
	protected void onError(IOException ex){
		
	}
	
	/** 
	 * 	This callback gets executed when a request succeeds
	 * 	(http responce code < 400)
	 *  Override this method during instantiation
	 *  to add onSuccess handling.
	 * @param transport Response data
	 */	
	protected void onSuccess(Transport transport){
		
	}
	/** 
	 * 	This callback gets executed when a request fails
	 * 	(http responce code >= 400)
	 *  Override this method during instantiation
	 *  to add onFailure handling.
	 * @param transport Response data
	 */	
	protected void onFailure(Transport transport){
		
	}
	
	/**
	 *  Auto-identifies parameters and serializes into compatible format.
	 *  Also automatically sets the 'Content-Type' and 'Accept' header to the format
	 *  detected. (Note: if you want to accept a different type of content than the
	 *  parameters. Please call accept(String mime) after the call to this function.
	 * @param parameters Can be a HashMap , JSONObject or XMLNode, everything else fallbacks to Object.toString();
	 */
	private void setParams(Object parameters) {
		if(parameters instanceof JSONObject){
			setJsonParams((JSONObject)parameters);
		}else{			
			setStringParams(parameters.toString());
		}
	}
	
	/**
	 *  Sets the supplied String:text as post data.
	 *  Also checks if the string starts with an XML indentation
	 *  and in that case sets the ContentType header to 'application/xml'
	 * @param text
	 */
	public void setStringParams(String text) {
		try {
			params = new StringEntity(text);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public Request setJsonParams(JSONObject jsonObject){
		setContentType(CTYPE_JSON);
		accept(CTYPE_JSON);
		setStringParams(jsonObject.toString());
		return this;
	}
	
	public Request setContentType(String ctype){
		setHeader("Content-Type",ctype);		
		return this;
	}
	public String getContentType(String ctype){
		return headers.get("Content-Type");
	}
	public Request setHeader(String key, String value){
		headers.put(key, value);
		return this;
	}
	/**
	 *  Modifies the Accept header field.
	 * @param contentType
	 * @return returns self for convenience.
	 */
	public Request accept(String contentType){
		setHeader("Accept",contentType);
		return this;
	}

	
	
	
}
