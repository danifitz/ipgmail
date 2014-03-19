package uk.ac.brookes.danielf.pgpmail.email;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;

import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.app.ProgressDialog;
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
 * Additional code added by Daniel Fitzgerald
 * 
 * @author Jon Simon
 * 
 */
public class GetMail extends AsyncTask<Pair<Integer, Integer>, Void, ArrayList<Email>>{

	private final static String LOG_TAG = "GET_MAIL";
	
	private String user;
	private String host;
	private String pass;

	private Context context;
	ProgressDialog pd = null;

	public GetMail(Context context) {
		this.context = context;
	}
	
	public interface MailListener
	{
		public void onMailReady(ArrayList<Email> emails);
	}
	
	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		//runs on the main ui thread
		pd = new ProgressDialog(context);
		pd.setTitle("Inbox");
		pd.setMessage("Fetching mail...");
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pd.show();
	}
	
	@Override
	protected void onProgressUpdate(Void... progress)
	{

	}
	
	/**
	 * Get messages from the inbox in index positions start>end.
	 * Checks if the message is in plaintext format and returns
	 * a list containing those that are.
	 * 
	 * @param loadCount - the amount of emails already loaded in the inbox activity
	 * @param amount
	 * @return
	 */
	@Override
	protected ArrayList<Email> doInBackground(Pair<Integer, Integer>... pairs) {

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
		
		ArrayList<Email> emails = new ArrayList<Email>();
		/*
		 * Get mail!
		 */
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");

		Session session = Session.getInstance(props, null);
		Store store = null;
		Folder inbox = null;
		
		try {
			
			store = session.getStore();
			store.connect(host, user, pass);
			inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);
			
			int loadCount = pairs[0].first;
			int amount = pairs[0].second;
			int start = 0;
			int end = 0;
			
			//how many emails in the inbox
			int count = inbox.getMessageCount();
			Log.d(LOG_TAG, "there are " + count + " email(s) in the inbox");
			if(count > amount)
			{
				//get a load of emails from the inbox
				start = (count - (amount + loadCount));
				end = count - loadCount;
				Log.d(LOG_TAG, "start = " + start + " end = " + end);
			}
			else
			{
				//if the number of emails in the inbox
				//is less than amount then load
				//the whole inbox
				start = 1;
				end = count;
			}
			
			Message messages[] = inbox.getMessages(start, end);

			for (int i = 0; i < messages.length; i++) 
			{
				//if the message content is a string or it's in plain text add it to the list
				if(messages[i].getContent() instanceof String)
					emails.add(new Email(messages[i], context));
				else if(Email.isPlaintext((Multipart) messages[i].getContent()))
						emails.add(new Email(messages[i], context));
				
			}
			
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//TODO: think we need to reverse the order here to get the emails newest at the top
		Collections.reverse(emails);
		return emails;
	}
	
	@Override
	protected void onPostExecute(ArrayList<Email> result)
	{
		//runs on the main ui thread
		if(pd.isShowing())
		{
			pd.dismiss();
		}
		
		//deliver the mail via listener callback interface
		MailListener mListener = (MailListener) context;
		mListener.onMailReady(result);
	}
}
