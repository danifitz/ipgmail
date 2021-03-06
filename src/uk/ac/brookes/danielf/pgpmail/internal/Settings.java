package uk.ac.brookes.danielf.pgpmail.internal;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

	// default smtp server
	// default imap server
	// default mode - encrypt & sign, encrypt, sign
	// email username
	// email password
	// application first run?
	// attachment directory
	// decoded files directory
	
	private SharedPreferences settings;
	private SharedPreferences.Editor settingsEditor;
	
	public Settings(Context context, int defaultMode, String smtpServer) 
	{
		settings = context.getSharedPreferences("settings",
				Context.MODE_PRIVATE);
		setDefaultMode(defaultMode);
		setSMTPServer(smtpServer);
	}
	
	public Settings(Context context)
	{
		settings = context.getSharedPreferences("settings",
				Context.MODE_PRIVATE);	
	}

	public int getDefaultMode() {
		int defaultMode = settings.getInt("defaultMode", -1);
		return defaultMode;
	} 

	public void setDefaultMode(int defaultMode) {
		settingsEditor = settings.edit();
		settingsEditor.putInt("defaultMode", defaultMode);
		settingsEditor.commit();
	}
	
	public String getSMTPServer()
	{
		String smtpServer = settings.getString("smtpserver", null);
		return smtpServer;
	}
	
	public void setSMTPServer(String smtpServer)
	{
		settingsEditor = settings.edit();
		settingsEditor.putString("smtpserver", smtpServer);
		settingsEditor.commit();
	}
	
	public String getIMAPServer()
	{
		String imapServer = settings.getString("imapserver", null);
		return imapServer;
	}

	public void setIMAPServer(String imapServer)
	{
		settingsEditor = settings.edit();
		settingsEditor.putString("imapserver", imapServer);
		settingsEditor.commit();
	}
	
	public void setEmailUsername(String username)
	{
		settingsEditor = settings.edit();
		settingsEditor.putString("username", username);
		settingsEditor.commit();
	}
	
	public String getEmailUsername()
	{
		String username = settings.getString("username", null);
		return username;
	}
	
	public void setEmailPassword(String pass)
	{
		settingsEditor = settings.edit();
		settingsEditor.putString("password", pass);
		settingsEditor.commit();
	}
	
	public String getEmailPassword()
	{
		String pass = settings.getString("password", null);
		return pass;
	}
	
	public boolean getFirstRun()
	{
		boolean firstRun = settings.getBoolean("firstrun", true);
		return firstRun;
	}
	
	public void setFirstRun(boolean firstRun)
	{
		settingsEditor = settings.edit();
		settingsEditor.putBoolean("firstrun", firstRun);
		settingsEditor.commit();
	}
	
	public void setAttachDir(String attachDir)
	{
		settingsEditor = settings.edit();
		settingsEditor.putString("attachdir", attachDir);
		settingsEditor.commit();
	}
	
	public String getAttachDir()
	{
		String attachDir = settings.getString("attachdir", null);
		return attachDir;
	}
	
	public void setDecodeDir(String decodeDir)
	{
		settingsEditor = settings.edit();
		settingsEditor.putString("decodedir", decodeDir);
		settingsEditor.commit();
	}
	
	public String getDecodeDir()
	{
		String decodeDir = settings.getString("decodedir", null);
		return decodeDir;
	}
}
