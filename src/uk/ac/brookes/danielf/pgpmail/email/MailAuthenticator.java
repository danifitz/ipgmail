package uk.ac.brookes.danielf.pgpmail.email;

import java.util.Properties;

import javax.mail.PasswordAuthentication;

/**
 * Class to authenticate with a mail server
 * @author danfitzgerald
 *
 */
public class MailAuthenticator extends javax.mail.Authenticator {

	private String _user; 
	private String _pass;
	
	private String _port; 
	private String _sport;
	
	private String _host;  
	  
	private boolean _auth; 
	   
	private boolean _debuggable;
	
	/**
	 * Constructor allowing all fields to be set
	 * @param host - the mail host
	 * @param user - the user name
	 * @param pass - the password
	 * @param debuggable - debuggable? - default off
	 * @param auth - authenticate? default on
	 */
	public MailAuthenticator(String host, String user, String pass, boolean debuggable, boolean auth) {
 
		_host = host; // default smtp server 
		_port = "465"; // default smtp port 
		_sport = "465"; // default socketfactory port 
		 
		_user = user; // username 
		_pass = pass; // password 
		 
		_debuggable = debuggable; // debug mode on or off - default off 
		_auth = auth; // smtp authentication - default on
	}
	
	  @Override 
	  public PasswordAuthentication getPasswordAuthentication() { 
	    return new PasswordAuthentication(_user, _pass); 
	  } 
	 
	  public Properties _setProperties() { 
	    Properties props = new Properties(); 
	 
	    props.put("mail.smtp.host", _host); 
	 
	    if(_debuggable) { 
	      props.put("mail.debug", "true"); 
	    }
	 
	    if(_auth) { 
	      props.put("mail.smtp.auth", "true"); 
	    } 
	 
	    props.put("mail.smtp.port", _port); 
	    props.put("mail.smtp.socketFactory.port", _sport); 
	    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
	    props.put("mail.smtp.socketFactory.fallback", "false"); 
	 
	    return props; 
	  }

}
