package com.milkliver.samples.scdfexecutor01.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SendRequest {

	private static final Logger log = LoggerFactory.getLogger(SendRequest.class);

	/*
	 * http("http://127.0.0.1:8084/id/A", "POST", 1000,5000);
	 * https("https://127.0.0.1:8443/id/A", "POST", 1000,5000);
	 */

	public Map http(String connectUrl, String hostname, String method, int connectTimeOut, int readTimeOut,
			Map bodyMap) {
		log.info(this.getClass().toString() + " http Url: " + connectUrl + " connectTimeOut: " + connectTimeOut
				+ " readTimeOut: " + readTimeOut + " ...");
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
		URL url;
		HttpURLConnection con;
		Map returnInfos = new HashMap();
		int responseCode = 0;

		try {
			url = new URL(connectUrl);

			con = (HttpURLConnection) url.openConnection();
			if (hostname != null && (!hostname.trim().equals(""))) {
				con.setRequestProperty("HOST", hostname);
			}

			// 設定方法為GET
			con.setRequestMethod(method);
			con.setConnectTimeout(connectTimeOut);
			con.setReadTimeout(readTimeOut);
			con.setUseCaches(false);
			con.setDoOutput(true);

//			con.setRequestProperty("HOST", hostname);
			log.info(con.getRequestProperty("HOST"));

			ObjectMapper requestJsonOM = new ObjectMapper();
			String requestBodyStr = requestJsonOM.writeValueAsString(bodyMap);
			log.info("sned request body json: " + requestBodyStr);

			OutputStream os = con.getOutputStream();
			DataOutputStream writer = new DataOutputStream(os);
			writer.write(requestBodyStr.getBytes());
			writer.flush();
			writer.close();
			os.close();

//			InputStream is = con.getInputStream();
			// Send Request and get ResponseCode
			responseCode = con.getResponseCode();
			returnInfos.put("statusCode", responseCode);

			BufferedReader responseBr = null;

			log.info("connectUrl: " + connectUrl);
			log.info("response Code: " + String.valueOf(responseCode));
			if (String.valueOf(responseCode).substring(0, 1).equals("4")
					|| String.valueOf(responseCode).substring(0, 1).equals("5")) {
				responseBr = new BufferedReader(new InputStreamReader(con.getErrorStream()));
				returnInfos.put("status", false);
			} else {
				returnInfos.put("status", true);
				responseBr = new BufferedReader(new InputStreamReader(con.getInputStream()));
			}

			StringBuilder resSb = new StringBuilder();
			String line;
			while ((line = responseBr.readLine()) != null) {
				resSb.append(line);
			}
			returnInfos.put("responseContent", resSb.toString());

			log.info(this.getClass().toString() + " http Url: " + connectUrl + " connectTimeOut: " + connectTimeOut
					+ " readTimeOut: " + readTimeOut + " finish");

			return returnInfos;

		} catch (MalformedURLException e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("status", false);
			return returnInfos;
		} catch (IOException e) {
			log.error("responseCode: " + String.valueOf(responseCode));
			log.error("connect http " + connectUrl + " fail");
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("statusCode", 408);
			returnInfos.put("status", false);
			return returnInfos;
		} catch (Exception e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("status", false);
			return returnInfos;
		}
	}

	public Map https(String connectUrl, String hostname, String method, int connectTimeOut, int readTimeOut,
			Map bodyMap) {
		log.info(this.getClass().toString() + " https Url: " + connectUrl + " connectTimeOut: " + connectTimeOut
				+ " readTimeOut: " + readTimeOut + " ...");
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

		SSLContext sslcontext;
		HttpsURLConnection con;
		Map returnInfos = new HashMap();
		try {
			sslcontext = SSLContext.getInstance("SSL", "SunJSSE");

			sslcontext.init(null, new TrustManager[] { new MyX509TrustManager() }, new java.security.SecureRandom());
			URL url = new URL(connectUrl);
			HostnameVerifier ignoreHostnameVerifier = new HostnameVerifier() {
				public boolean verify(String s, SSLSession sslsession) {
					return true;
				}
			};
			HttpsURLConnection.setDefaultHostnameVerifier(ignoreHostnameVerifier);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
			// 之後任何Https協議網站皆能正常訪問
			con = (HttpsURLConnection) url.openConnection();
			if (hostname != null && (!hostname.trim().equals(""))) {
				con.setRequestProperty("HOST", hostname);
			}
			con.setRequestMethod(method);
			con.setRequestProperty("Content-type", "application/json");
			// 必須設置為false，否則會自動redirect到重定向後的地址
			con.setInstanceFollowRedirects(false);
			con.setConnectTimeout(connectTimeOut);
			con.setReadTimeout(readTimeOut);
			con.setUseCaches(false);
			con.setDoOutput(true);

			log.info(con.getRequestProperty("HOST"));

			ObjectMapper requestJsonOM = new ObjectMapper();
			String requestBodyStr = requestJsonOM.writeValueAsString(bodyMap);
			log.info("sned request body json: " + requestBodyStr);

			OutputStream os = con.getOutputStream();
			DataOutputStream writer = new DataOutputStream(os);
			writer.write(requestBodyStr.getBytes());
			writer.flush();
			writer.close();
			os.close();

			int responseCode = con.getResponseCode();
			returnInfos.put("statusCode", responseCode);
			if (String.valueOf(responseCode).substring(0, 1).equals("4")
					|| String.valueOf(responseCode).substring(0, 1).equals("5")) {
				returnInfos.put("status", false);
			}
			returnInfos.put("status", true);
//			con.connect();

			log.info(this.getClass().toString() + " https Url: " + connectUrl + " connectTimeOut: " + connectTimeOut
					+ " readTimeOut: " + readTimeOut + " finish");

			return returnInfos;

		} catch (NoSuchAlgorithmException e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("status", false);
			return returnInfos;
		} catch (NoSuchProviderException e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("status", false);
			return returnInfos;
		} catch (KeyManagementException e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("status", false);
			return returnInfos;
		} catch (MalformedURLException e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("status", false);
			return returnInfos;
		} catch (IOException e) {
			log.info("connect https " + connectUrl + " fail");
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("statusCode", 408);
			returnInfos.put("status", false);
			return returnInfos;
		} catch (Exception e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			returnInfos.put("status", false);
			return returnInfos;
		}
	}

	public static boolean tcp(String ipAddress, int port, int connectTimeout, int readTimeout) {
		log.info("start connect tcp " + ipAddress + ":" + port + " ...");
		try {
			Socket socket = new Socket();
			socket.setSoTimeout(readTimeout);
			socket.connect(new InetSocketAddress(ipAddress, port), connectTimeout);
//			InputStream inFromServer = socket.getInputStream();
//			DataInputStream in = new DataInputStream(inFromServer);
//			inFromServer.read();
			log.info("connect tcp " + ipAddress + ":" + port + " finish");
			return true;
		} catch (IOException e) {
			log.info("connect tcp " + ipAddress + ":" + port + " fail");
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			return false;
		} catch (Exception e) {
			log.error(e.getMessage());
			for (StackTraceElement elem : e.getStackTrace()) {
				log.error(elem.toString());
			}
			return false;
		}
	}

}
