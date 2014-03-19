package uk.ac.brookes.danielf.pgpmail.email;

import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.content.Context;
import android.os.AsyncTask;

public class GetMailStats extends AsyncTask<Void, Void, Integer> {
	
	private Context context;
	private String user, pass, host;
	
	public GetMailStats(Context context) {
		this.context = context;
	}
	
	@Override
	protected Integer doInBackground(Void... arg0) {
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
