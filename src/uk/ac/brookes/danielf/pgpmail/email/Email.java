package uk.ac.brookes.danielf.pgpmail.email;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Class to represent an email
 * 
 * @author danfitzgerald
 *
 */
public class Email implements Parcelable {
	
	private MimeMultipart email; //holds the different Mime parts that make up the email
	private BodyPart body; //holds the body part
	
	private String from[]; //who is the email from
	private String to[]; //who is the email to
	private String subject; //what is the emails subject
	private String msgBody; //what is the emails body
	
	private Date sentDate; //what date was it sent? only relevant for sent emails
	
	private int pgpMode; //what's the pgp mode?
	
	/**
	 * Constructor when we want to create an email from scratch
	 * with the purpose of sending it.
	 * @param from - who is the message from
	 * @param to - who is the message to
	 * @param subject - the subject of the message
	 * @param msgBody - the body of the message
	 * @param attachments - any email attachments
	 * @param pgpMode - is the email to be encrypted, encrypted and signed or signed only?
	 * @throws IOException
	 * @throws MessagingException 
	 * @throws Exception
	 */
	public Email(String from[], String to[], String subject, String msgBody, File attachments[], int pgpMode) throws IOException, MessagingException 
	{
		this.from = from;
		this.to = to;
		this.subject = subject;
		this.msgBody = msgBody;
		
		//create the parts of the email
		email = new MimeMultipart();
		body = new MimeBodyPart();
		body.setText(this.msgBody);
		email.addBodyPart(this.body);
		
		//if there are any attachments let's add them now
		if(attachments != null) 
		{
			for(File file : attachments)
				addAttachment(file.getCanonicalPath());
		}
		
		this.pgpMode = pgpMode;
	}
	
	/**
	 * Constructs a received email from a javax.mail message
	 * @param message - the javax.mail representation of an email
	 * @throws MessagingException 
	 * @throws IOException 
	 */
	public Email(Message message) throws MessagingException, IOException
	{
		this.from = getAddressesAsStringArray(message.getFrom());
		this.to = getAddressesAsStringArray(message.getAllRecipients());
		this.subject = message.getSubject();
		this.msgBody = message.getContent().toString();
		
		/*
		 * if the body of the message has a PGP header then
		 * set the pgp mode appropriately.
		 */
		if(msgBody.startsWith(PGP.PGP_ENCRYPTED_MESSAGE_HEADER))
			pgpMode = PGP.PGP_ENCRYPT_ONLY;
		else if(msgBody.startsWith(PGP.PGP_SIGNED_MESSAGE_HEADER))
			pgpMode = PGP.PGP_SIGN_ONLY;
		//doesn't begin with encryption or signature headers, this message is not encrypted
		else
			pgpMode = PGP.PGP_UNENCRYPTED;
	}
	
	/**
	 * This is needed for Android to be able to create
	 * new parcel-able Email objects (individually or as arrays).
	 * 
	 * It also allows us to use a normal style constructor.
	 */
	public static final Parcelable.Creator<Email> CREATOR =
		new Parcelable.Creator<Email>() {
			public Email createFromParcel(Parcel in)
			{
				return new Email(in);
			}
			
			public Email[] newArray(int size)
			{
				return new Email[size];
			}
	};
	
	/**
	 * Constructor to reinflate an email that's been parcelled
	 * @param in
	 */
	public Email(Parcel in)
	{
		readFromParcel(in);
	}
	
    public void addAttachment(String filename) throws IOException, MessagingException { 
    	BodyPart messageBodyPart = new MimeBodyPart(); 
	    DataSource source = new FileDataSource(filename); 
	    messageBodyPart.setDataHandler(new DataHandler(source)); 
	    messageBodyPart.setFileName(filename.substring(filename.lastIndexOf(File.pathSeparator) + 1)); 
	 
	    email.addBodyPart(messageBodyPart);
	  }
    
    /**
     * Takes an array of addresses and returns them as an array of strings
     * @param addresses
     * @return
     */
    public String[] getAddressesAsStringArray(Address addresses[])
    {
    	String recipients[] = null;
    	if(addresses != null)
    	{
    		//create a string array of recipients from Address[i]
    		recipients = new String[addresses.length];
    		
    		for(int i = 0; i < addresses.length; i++)
    			recipients[i] = addresses[i].toString();
    	}
    	else
    		return null;
		return recipients;
    }
    
    /**
     * Takes an array of Strings and returns them as a concatenation.
     * Each element is whitespace + comma seperated i.e "1, 2, 3".
     * @param array - an array of Strings
     * @return a concatenated, white space + comma seperated String
     */
    public String getStringArrayAsConcatString(String[] strings)
    {
    	String result = "";
    	for(int i = 0; i < strings.length; i++)
    	{
    		result += strings[i];
    		
    		//add a ", " up until the element before last
    		if(i < (strings.length - 1))
    		{
    			result += ", ";
    		}
    	}
    	return result;
    }
    
	@Override
	public void writeToParcel(Parcel out, int flags) {
		//write all the values to the parcel
		out.writeValue(email);
		out.writeValue(body);
		out.writeStringArray(from);
		out.writeStringArray(to);
		out.writeString(subject);
		out.writeString(msgBody);
		out.writeString(sentDate.toString());
		out.writeInt(pgpMode);
	}
	
	public void readFromParcel(Parcel in)
	{
		//read each field in the order they were parcelled
		email = (MimeMultipart) in.readValue(MimeMultipart.class.getClassLoader());
		body = (BodyPart) in.readValue(BodyPart.class.getClassLoader());
		in.readStringArray(from);
		in.readStringArray(to);
		subject = in.readString();
		msgBody = in.readString();
		try {
			sentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(in.readString());
		} catch (ParseException e) {
			Log.e("Email - reading from Parcel", "error parsing date", e);
			e.printStackTrace();
		}
		pgpMode = in.readInt();
	}
	
	public static String formatVerifiedEmail(String body)
	{
		return null;
	}
	
	public MimeMultipart getEmail() {
		return email;
	}

	public BodyPart getBody() {
		return body;
	}

	public String[] getFrom() {
		return from;
	}

	public String[] getTo() {
		return to;
	}

	public String getSubject() {
		return subject;
	}

	public String getMsgBody() {
		return msgBody;
	}

	public Date getSentDate() {
		return sentDate;
	}
	
	public int getPgpMode() {
		return pgpMode;
	}

	public void setPgpMode(int pgpMode) {
		this.pgpMode = pgpMode;
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
