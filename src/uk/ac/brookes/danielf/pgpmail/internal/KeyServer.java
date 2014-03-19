package uk.ac.brookes.danielf.pgpmail.internal;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Properties;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

public class KeyServer extends AsyncTask<Properties, Integer, Pair<Integer, String>> {
	
	private final static String LOG_TAG = "KEY_SERVER";
	private final String USER_AGENT                  = "Mozilla/5.0";
	
	public final static String ACTION_SEARCH_USER    = "SEARCHUSER";
	public final static String ACTION_RETRIEVE_KEY   = "SEARCHKEY";
	public final static String ACTION_SUBMIT_KEY     = "SUBMIT";
	
	private final String GET_BASE_ADDRESS            = "http://pgp.mit.edu/pks/lookup?search=";
	private final String POST_BASE_ADDRESS           = "http://pgp.mit.edu/pks/add/";
	
	public final static int KEY_SERVER_INDEX         = 2;
	public final static int KEY_SERVER_INDEX_VERBOSE = 3;
	
	//GET url params
	private final String INDEX                       = "&op=index";
	private final String VERBOSE_INDEX               = "&op=vindex";
	private final String FINGERPRINT                 = "&fingerprint=on";
	private final String EXACT_MATCH                 = "&exact=on";
	
	ProgressDialog pd                                = null;
	Context context;
	
	public KeyServer(Context context)
	{
		this.context = context;
	}
	
	@Override
	protected Pair<Integer, String> doInBackground(Properties... properties) {
		
		Properties props = properties[0];
		String action = props.getProperty("action");
		
		Pair<Integer, String> result = new Pair<Integer, String>(null, null);
		if(action.equals(ACTION_SEARCH_USER))
		{
			String term = props.getProperty("term");
			int indexMode = Integer.valueOf(props.getProperty("indexmode"));
			boolean asFingerprint = Boolean.valueOf(props.getProperty("fingerprint"));
			boolean exactMatch = Boolean.valueOf(props.getProperty("exactmatch"));
			result = searchKeys(term, indexMode, asFingerprint, exactMatch);
		}
		else if(action.equals(ACTION_SUBMIT_KEY))
		{
			byte[] key = (byte[]) props.get("key");
			result = submitKey(key);
		}
		else if(action.equals(ACTION_RETRIEVE_KEY))
		{
			String keyparams = props.getProperty("keyparams");
			result = retrieveKey(keyparams);
		}
		
		return result;
	}
	
	private Pair<Integer, String> searchKeys(String searchTerm, int indexMode, boolean asFingerprint, boolean exactMatch)
	{
		//we need to add a %20 instead of any whitespace in the search term...
		searchTerm = searchTerm.replaceAll(" ", "%20");
		
		//construct the address
		String address = GET_BASE_ADDRESS + searchTerm;
		switch(indexMode)
		{
			case KEY_SERVER_INDEX:
				address += INDEX;
				break;
			case KEY_SERVER_INDEX_VERBOSE:
				address += VERBOSE_INDEX;
				break;
		}
		if(asFingerprint)
			address += FINGERPRINT;
		if(exactMatch)
			address += EXACT_MATCH;
		
		URL url = null;
		HttpURLConnection conn = null;
		
		String result = "";
		
		int responseCode = 0;
		try {
			url = new URL(address);
			
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("GET");
			
			conn.setRequestProperty("User-Agent", USER_AGENT);
			
			responseCode = conn.getResponseCode();
			if(responseCode != 200)
			{
				result = String.valueOf(responseCode);
				return new Pair<Integer, String>(responseCode, null);
			}
			
			Log.d(LOG_TAG , "Sending GET request to url: " + address);
			Log.d(LOG_TAG, "Response code " + String.valueOf(responseCode));
			
			BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String input;
			StringBuffer sb = new StringBuffer();
			
			while((input = br.readLine()) != null)
			{
				sb.append(input);
			}
			br.close();
			
			//log the result
			result = sb.toString();
			Log.d(LOG_TAG , "GET request result " + result);
			
		} catch (MalformedURLException e) {
			//we should never encounter this
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new Pair<Integer, String>(responseCode, result);
	}
	
	private Pair<Integer, String> retrieveKey(String keyparams)
	{
		String address = "http://pgp.mit.edu" + keyparams;
		URL url = null;
		HttpURLConnection conn = null;
		
		String result = "";
		
		int responseCode = 0;
		try {
			url = new URL(address);
			
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("GET");
			
			conn.setRequestProperty("User-Agent", USER_AGENT);
			
			responseCode = conn.getResponseCode();
			Log.d(LOG_TAG, "Sending GET request to url: " + address);
			Log.d(LOG_TAG,  "Response code " + String.valueOf(responseCode));
			
			BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String input;
			StringBuffer sb = new StringBuffer();
			
			while((input = br.readLine()) != null)
			{
				sb.append(input);
				
			}
			br.close();
			
			//log the result
			result = sb.toString();
			Log.d(LOG_TAG, "GET request result" + result);
			
		} catch (MalformedURLException e) {
			//we should never encounter this
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new Pair<Integer, String>(responseCode, result);
	}
	
	private Pair<Integer, String> submitKey(byte[] keyBytes)
	{
		String address = POST_BASE_ADDRESS;
		
		HttpURLConnection conn = null;
		
		String result = "";
		
		String keyBlock = "";
		try {
			keyBlock = new String(keyBytes, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}
		//for some reason the key block is spat out without the newline it
		//needs to be accepted by the key server.
		String begin = keyBlock.substring(0, keyBlock.indexOf("@RELEASE_NAME@") + "@RELEASE_NAME@".length());
		String end = keyBlock.substring(keyBlock.indexOf("@RELEASE_NAME@") + "@RELEASE_NAME@".length(), keyBlock.length());
		String key = "";
		try {
			key = new String((begin + "\r\n" + "\r\n" + end).getBytes(), "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		int responseCode = 0;
		try {
			URL url = new URL(address);
			conn = (HttpURLConnection) url.openConnection();
			
			//set the request headers
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", USER_AGENT);
			conn.setRequestProperty("Accept-Language", "en-UK,en;q=0.5");
			
			//send the request
			conn.setDoOutput(true);
			DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
			dos.writeBytes(key);
			dos.flush();
			dos.close();
			
			responseCode = conn.getResponseCode();
			Log.d(LOG_TAG, "Sending POST request to: " + address);
			Log.d(LOG_TAG, "POST params: " + key);
			Log.d(LOG_TAG, "POST response code: " + String.valueOf(responseCode));
			
			BufferedReader br = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			String input;
			StringBuffer sb = new StringBuffer();
			
			while((input = br.readLine()) != null)
			{
				sb.append(input);
			}
			br.close();
			
			result = sb.toString();
			//log the result
			Log.d(LOG_TAG, "POST request result: " + result);
			
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new Pair<Integer, String>(responseCode, result);
	}
}
