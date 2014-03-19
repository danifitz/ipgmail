package uk.ac.brookes.danielf.pgpmail.email;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class SendMail extends AsyncTask<Email, Void, Boolean> {

	private final static String LOG_TAG = "SENDMAIL";
	
	private Context context;

	private String user;
	private String host;
	private String pass;
	private boolean debuggable = false;
	private boolean auth = true;

	private Multipart _multipart;

	public SendMail(Context context) {
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(Email... emails) {

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
		host = settings.getSMTPServer();

		if (!user.equals("") && !pass.equals("")) {

			MailAuthenticator authenticator = new MailAuthenticator(host, user,
					pass, debuggable, auth);
			Properties props = authenticator._setProperties();
			Session session = Session.getInstance(props, authenticator);

			MimeMessage msg = new MimeMessage(session);

			try {
				msg.setFrom(new InternetAddress(emails[0].getFrom()[0]));

				InternetAddress[] addressTo = new InternetAddress[emails[0]
						.getTo().length];
				for (int i = 0; i < emails[0].getTo().length; i++) {
					addressTo[i] = new InternetAddress(emails[0].getTo()[i]);
				}
				msg.setRecipients(MimeMessage.RecipientType.TO, addressTo);

				msg.setSubject(emails[0].getSubject());
				msg.setSentDate(new Date());

				//setup message body
				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setText(emails[0].getMsgBody());
				_multipart = new MimeMultipart();
				_multipart.addBodyPart(messageBodyPart);
				
				//setup attachments
				if(emails[0].hasAttachments)
				{
					Iterator<File> itr = emails[0].getAttachments().iterator();
					while(itr.hasNext())
					{
				    	File file = itr.next();
						MimeBodyPart attachBodyPart = new MimeBodyPart();
					    DataSource source = new FileDataSource(file.getAbsolutePath());
					    attachBodyPart.setDataHandler(new DataHandler(source));
					    String name = file.getName();
					    Log.d(LOG_TAG, "attaching file " + name);
					    attachBodyPart.setFileName(name);
					 
					    _multipart.addBodyPart(attachBodyPart);
					}
				}

				// Put parts in message
				msg.setContent(_multipart);

				// send email
				Transport.send(msg);
			} catch (AddressException e) {
				e.printStackTrace();
			} catch (MessagingException e) {
				e.printStackTrace();
			}

			return true;
		} else {
			return false;
		}
	}
}
