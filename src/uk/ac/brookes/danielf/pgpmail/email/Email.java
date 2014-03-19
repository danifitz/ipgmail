package uk.ac.brookes.danielf.pgpmail.email;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

/**
 * Class to represent an email
 * 
 * @author danfitzgerald
 *
 */
public class Email implements Parcelable {
	
	private final static String LOG_TAG = "EMAIL";
	
	private MimeMultipart email; //holds the different Mime parts that make up the email
	private BodyPart body; //holds the body part
	
	private String from[]; //who is the email from
	private String to[]; //who is the email to
	private String subject; //what is the emails subject
	private String msgBody; //what is the emails body
	
	private Date sentDate; //what date was it sent? only relevant for sent emails
	
	private int pgpMode; //what's the pgp mode?
	
	private ArrayList<File> attachments;
	public boolean hasAttachments;
	
	/**
	 * Constructor when we want to create an email from scratch
	 * with the purpose of sending it.
	 * @param from - who is the message from
	 * @param to - who is the message to
	 * @param subject - the subject of the message
	 * @param msgBody - the body of the message
	 * @param attachments - any email attachments
	 * @param pgpMode - is the email to be encrypted, encrypted and signed or signed only?
	 * 
	 * @throws IOException
	 * @throws MessagingException 
	 * @throws Exception
	 */
	public Email(String from[], String to[], String subject, String msgBody, ArrayList<File> attachments, int pgpMode) throws IOException, MessagingException 
	{
		this.from = from;
		this.to = to;
		this.subject = subject;
		this.msgBody = msgBody;
		
		//create the parts of the email
		email = new MimeMultipart();
		body = new MimeBodyPart();
		//set the content of the email, making sure the content-type is set to plain text
		body.setContent(this.msgBody, "text/plain");
		email.addBodyPart(this.body);
		
		//if there are any attachments let's add them now
		if(attachments != null)
		{
			this.attachments = attachments;
			hasAttachments = true;
		}
		
		this.pgpMode = pgpMode;
	}
	
	/**
	 * Constructs a received email from a javax.mail message
	 * 
	 * @param message - the javax.mail representation of an email
	 * 
	 * @throws MessagingException 
	 * @throws IOException 
	 */
	public Email(Message message, Context context) throws MessagingException, IOException
	{
		this.from = getAddressesAsStringArray(message.getFrom());
		this.to = getAddressesAsStringArray(message.getAllRecipients());
		this.subject = message.getSubject();
		this.sentDate = message.getSentDate();
		
		/*
		 * get the content object
		 * 
		 * It can be made of lots of different parts
		 * so let's parse through them and obtain what
		 * we're interested in.
		 */
		Object content = message.getContent();
		
		//get the body
		if(content instanceof String)
		{
			this.msgBody = (String) content;
		}		
		else if(content instanceof Multipart)
		{
			this.msgBody = getBodyFromMultipart((Multipart) content);
			
			//TODO: be perhaps a bit more clever...maybe need to delve a bit deeper into the multipart
			attachments = getAttachments((Multipart) content, context);
		}
		
		/*
		 * if the body of the message has a PGP header then
		 * set the pgp mode appropriately.
		 */
		if(msgBody.startsWith(PGP.PGP_ENCRYPTED_MESSAGE_HEADER))
			this.pgpMode = PGP.PGP_ENCRYPT_ONLY;
		else if(msgBody.startsWith(PGP.PGP_SIGNED_MESSAGE_HEADER))
			this.pgpMode = PGP.PGP_SIGN_ONLY;
		//doesn't begin with encryption or signature headers, this message is not encrypted
		else
			this.pgpMode = PGP.PGP_UNENCRYPTED;
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
    
    private ArrayList<File> getAttachments(Multipart mp, Context context) throws FileNotFoundException, MessagingException, IOException
    {
    	ArrayList<File> attachments = new ArrayList<File>();
		
		for(int i = 0; i < mp.getCount(); i++)
		{
			BodyPart bp = mp.getBodyPart(i);
			//removed the not (!) in the first bit
			if(Part.ATTACHMENT.equalsIgnoreCase(bp.getDisposition()) &&
					!TextUtils.isEmpty(bp.getFileName()))
			{
				hasAttachments = true;
				
				InputStream is = bp.getInputStream();
				
				//obtain the attachment directory from settings
				Settings settings = new Settings(context);
				String dest = settings.getAttachDir() + File.separator + bp.getFileName();
				
				Log.d(LOG_TAG, "moving attachment to " + dest);
				
				File file = new File(dest);
				FileOutputStream fos = new FileOutputStream(file);
				byte[] buffer = new byte[4096];
				int read;
				while((read = is.read(buffer)) != -1)
				{
					fos.write(buffer, 0 , read);
				}
				fos.close();
				
				attachments.add(file);
			}
		}
		return attachments;
    }
    
    /**
     * Turns out there are loads of weird combinations
     * of part, bodyparts & multiparts (let's not forget mimemultiparts...seriously Oracle!?)
     * So we'll have to do something rather clever to obtain the body
     * 
     * @param mp
     * @throws MessagingException 
     * @throws IOException 
     */
    private String getBodyFromMultipart(Multipart mp) throws MessagingException, IOException
    {
    	String body = "";
    	//let's have a little loop through the various parts of the multipart
    	for(int i = 0; i < mp.getCount(); i++)
    	{
    		//get the body part
    		BodyPart bp = mp.getBodyPart(i);
    		
    		//get the disposition
    		String dispos = bp.getDisposition();
    		
    		//not an attachment & instanceof a mimebody part...urgh
    		if(dispos == null && bp instanceof MimeBodyPart)
    		{
    			MimeBodyPart mbp = (MimeBodyPart) bp;
    			
    			/*
    			 * sometimes the message text will be hidden in
    			 * yet another multipart...why, we will never know!
    			 * but we should be prepared for this situation!
    			 */
    			if(mbp.getContent() instanceof Multipart)
    			{
    				//wooo....recursion
    				getBodyFromMultipart((Multipart) mbp.getContent());
    			}
    			else
    			{
    				/*
    				 * finally we are down to getting hold of the body
    				 * 
    				 * it's either in text/plain or text/html format
    				 */
    				if(mbp.isMimeType("text/plain"))
    				{
    					body = (String) mbp.getContent();
    				}
    				else if(mbp.isMimeType("text/html"))
    				{
    					body = (String) mbp.getContent();
    				}
    			}
    		}
    	}
    	return body;
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
    	
		return recipients;
    }
    
    /**
     * Takes an array of Strings and returns them as a concatenation.
     * Each element is whitespace + comma seperated i.e "1, 2, 3".
     * @param array - an array of Strings
     * @return a concatenated, white space + comma seperated String
     */
    public static String getStringArrayAsConcatString(String[] strings)
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
		out.writeList(attachments);
		out.writeStringArray(from);
		out.writeStringArray(to);
		out.writeString(subject);
		out.writeString(msgBody);
		out.writeString(sentDate.toString());
		out.writeInt(pgpMode);
		
		//if there are attachments write a 1 if not a 0
		if(hasAttachments)
			out.writeInt(1);
		else
			out.writeInt(0);
	}
	
	public void readFromParcel(Parcel in)
	{
		//read each field in the order they were parcelled
		
		email = (MimeMultipart) in.readValue(MimeMultipart.class.getClassLoader());
		body = (BodyPart) in.readValue(BodyPart.class.getClassLoader());
		
		//TODO: is this right?
		attachments = (ArrayList) in.readArrayList(ArrayList.class.getClassLoader());
		
		//get the from and to recipients
		from = in.createStringArray();
		to = in.createStringArray();
		
		subject = in.readString();
		msgBody = in.readString();
		try {
			sentDate = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH).parse(in.readString());
		} catch (ParseException e) {
			Log.e("Email - reading from Parcel", "error parsing date", e);
			e.printStackTrace();
		}
		pgpMode = in.readInt();
		
		int i = in.readInt();
		if(i == 1)
			hasAttachments = true;
		else
			hasAttachments = false;
	}
	
	/**
	 * Similar to the getBodyFromMultipart() routine except when we get
	 * down to the body we check if the body is in plain text
	 * @param message
	 * @return
	 * @throws MessagingException 
	 * @throws IOException 
	 */
	public static boolean isPlaintext(Multipart mp) throws MessagingException, IOException
	{
		boolean isPlaintext = false;
    	//let's have a little loop through the various parts of the multipart
    	for(int i = 0; i < mp.getCount(); i++)
    	{
    		//get the body part
    		BodyPart bp = mp.getBodyPart(i);
    		
    		//get the disposition
    		String dispos = bp.getDisposition();
    		
    		//not an attachment & instanceof a mimebody part...urgh
    		if(dispos == null && bp instanceof MimeBodyPart)
    		{
    			MimeBodyPart mbp = (MimeBodyPart) bp;
    			
    			/*
    			 * sometimes the message text will be hidden in
    			 * yet another multipart...why, we will never know!
    			 * but we should be prepared for this situation!
    			 */
    			if(mbp.getContent() instanceof Multipart)
    			{
    				//wooo....recursion
    				isPlaintext((Multipart) mbp.getContent());
    			}
    			else
    			{
    				/*
    				 * finally we are down to getting hold of the body
    				 * 
    				 * it's either in text/plain or text/html format
    				 */
    				if(mbp.isMimeType("text/plain"))
    				{
    					isPlaintext = true;
    				}
    				else if(mbp.isMimeType("text/html"))
    				{
    					isPlaintext = false;
    				}
    			}
    		}
    	}
    	return isPlaintext;
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
	
	public ArrayList<File> getAttachments()
	{
		return attachments;
	}

	@Override
	public int describeContents() {
		return 0;
	}
}
