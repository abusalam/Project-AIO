package com.github.abusalam.android.projectaio.ajax;
/*
 * @author TonyIvanov (telamohn@gmail.com) 
 */

import com.github.abusalam.android.projectaio.ajax.Request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;


public class Transport {
    private HttpResponse response;
    private String contentType;
    private String responseText = "";
    private JSONObject responseJson = null;

    private String contentEncoding;
    private long contentLength;

    /**
     * Reads input stream into a string and calls consume content.
     * Not very smart incase of a binary object.
     *
     * @param resp
     * @throws IllegalStateException
     * @throws IOException
     */
    public Transport(HttpResponse resp) throws IllegalStateException, IOException {
        response = resp;
        if (response.getEntity().getContentType() != null) {
            contentType = response.getEntity().getContentType().getValue();
        } else {
            contentType = Request.CTYPE_PLAIN;
        }
        if (response.getEntity().getContentEncoding() != null) {
            contentEncoding = response.getEntity().getContentEncoding().getValue();
        } else {
            contentEncoding = "UTF-8"; // used internally for toString()
        }

        contentLength = response.getEntity().getContentLength();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream content = response.getEntity().getContent();
		int b=0;
		while((b = content.read())!= -1){
			baos.write(b);
		}
        response.getEntity().writeTo(baos);

        responseText = baos.toString(contentEncoding);

        response.getEntity().consumeContent();

    }

    /**
     * Returns the original response object.
     *
     * @return httpResponse - might be consumed.
     */
    public HttpResponse getResponse() {
        return response;
    }

    public String getContentType() {
        return contentType;
    }

    public String getContentEncoding() {
        return contentEncoding;
    }

    public long getContentLength() {
        return contentLength;
    }

    public JSONObject getResponseJson() {
        if (responseJson == null) {
            if (contentType.equalsIgnoreCase(Request.CTYPE_JSON)) {

            }
            try {
                responseJson = new JSONObject(responseText);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return responseJson;
    }

    public int getStatus() {

        return response.getStatusLine().getStatusCode();
    }

    public String getResponseText() {
        return responseText;
    }
}
