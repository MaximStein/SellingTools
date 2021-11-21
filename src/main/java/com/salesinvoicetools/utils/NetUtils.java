package com.salesinvoicetools.utils;

import com.salesinvoicetools.Private;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NetUtils {

    public static HttpURLConnection openHttpURLConnection(String endpoint, String method, String content, String authToken, String contentType) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection http = (HttpURLConnection) url.openConnection();

        if(authToken != null)
            http.setRequestProperty("Authorization", "Bearer " + authToken);

        http.setRequestProperty("Content-Language", "en-US");
        //http.setRequestProperty("x-api-key", Private.ETSY_API_KEY);
        http.setRequestMethod(method == null ? "GET" : method);
        http.setDoOutput(true);
        http.setDoInput(true);

        if(contentType != null) {
            http.setRequestProperty("Content-Type", contentType);
        } else if (content != null) {
            http.setRequestProperty("Content-Type", "application/json; utf-8");
        }


        http.connect();

        System.out.println("sending content\'"+content+"\' to "+endpoint);

        if (content != null) {
            try (OutputStream os = http.getOutputStream()) {
                byte[] input = content.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.close();
            }
        } else {
        }

        return http;
    }

    public static String makeHttpCall(String endpoint, String method, String content, String authToken, String contentType) throws IOException {

        var conn = openHttpURLConnection(endpoint,method, content, authToken, contentType);
        return getHttpConnectionContent(conn);
    }

    public static String getHttpConnectionContent(HttpURLConnection conn) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = conn.getInputStream();
        } catch(IOException exception) {
            inputStream = conn.getErrorStream();
        }

        System.out.println("[" + conn.getResponseCode() + "]");

        try {
            String theString = IOUtils.toString(inputStream, "UTF-8");
            System.out.println(theString);
            return theString;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}
