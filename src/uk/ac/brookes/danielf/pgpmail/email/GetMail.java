package uk.ac.brookes.danielf.pgpmail.email;

import java.io.IOException;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

/**
 * This is a wrapper class written by Jon Simon for the Android port of JavaMail
 * API. It makes it much easier to send emails and add attachments as well as
 * addressing some of the irregularities in the port of the library. More info
 * can be found at:
 * http://www.jondev.net/articles/Sending_Emails_without_User_Intervention_
 * %28no_Intents%29_in_Android
 * 
 * Additional code added by Me (Daniel Fitzgerald)
 * 
 * @author Jon Simon
 * 
 */
public class GetMail extends AsyncTask<Pair<Integer, Pair<Integer, Integer>>, Void, Object> {

	private String user;
	private String host;
	private String pass;

	private Context context;
	
	public final static int GET_MESSAGES = 0;
	public final static int GET_MESSAGE_COUNT = 1;

	public GetMail(Context context) {
		this.context = context;
	}

	@Override
	protected Object doInBackground(Pair<Integer, Pair<Integer, Integer>>... pairs) {

		// There is something wrong with MailCap, javamail can not find a
		// handler for the multipart/mixed part, so this bit needs to be added.
		MailcapCommandMap mc = (MailcapCommandMap) CommandMap
				.getDefaultCommandMap();
		mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
		mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
		mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
		mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
		mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
		CommandMap.setDefaultCommandMap(mc);

		// obtain the username and password from settings
		Settings settings = new Settings(context);
		user = settings.getEmailUsername();
		pass = settings.getEmailPassword();
		host = settings.getIMAPServer();
		
		int action = pairs[0].first;
		Email[] emails;
		Integer count;
		
		if(action == GET_MESSAGES)
		{
			emails = getInbox(pairs[0].second.first, pairs[0].second.second);
			return emails;
		}	
		else if(action == GET_MESSAGE_COUNT)
		{
			count = getEmailCount();
			return count;
		}
		
		return null;
	}
	
	private Email[] getInbox(int start, int end)
	{
		/*
		 * Get mail!
		 */
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");

		Session session = Session.getInstance(props, null);
		Store store = null;
		Folder inbox = null;
		
		Email emails[] = null;
		try {
			
			store = session.getStore();
			store.connect(host, user, pass);
			inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);
			
			Log.i("Mail - getting inbox, there are this emails in inbox: ",
					String.valueOf(inbox.getMessageCount()));
			
			Message messages[] = inbox.getMessages(start, end);
			emails = new Email[messages.length];

			for (int i = 0; i < messages.length; i++) 
			{
				emails[i] = new Email(messages[i]);
			}
			
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return emails;
	}
	
	private Integer getEmailCount()
	{
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");

		Session session = Session.getInstance(props, null);
		Store store = null;
		Folder inbox = null;
		
		Integer count = null;
		try {
			
			store = session.getStore();
			store.connect(host, user, pass);
			inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);
			
			count = Integer.valueOf(inbox.getMessageCount());
			
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return count;
	}
}
