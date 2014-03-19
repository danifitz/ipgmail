package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.mail.MessagingException;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;

import uk.ac.brookes.danielf.pgpmail.crypto.Crypto;
import uk.ac.brookes.danielf.pgpmail.crypto.Sign;
import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.dialogs.PGDialogListener;
import uk.ac.brookes.danielf.pgpmail.dialogs.SecretKeyPassphraseDialog;
import uk.ac.brookes.danielf.pgpmail.dialogs.SubjectAlertDialog;
import uk.ac.brookes.danielf.pgpmail.email.Email;
import uk.ac.brookes.danielf.pgpmail.email.SendMail;
import uk.ac.brookes.danielf.pgpmail.exceptions.InvalidKeyException;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import ar.com.daidalos.afiledialog.FileChooserActivity;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class ComposeActivity extends Activity implements PGDialogListener {

	private final static String LOG_TAG = "COMPOSE";
	
	private String 				 messageBody;
	
	private boolean				 hasBody;
	private boolean				 hasSenderReceiver;
	private boolean				 hasAttachment = false;
	
	private Spinner 			 pubKeySpinner, privateKeySpinner;
	private EditText			 subjectTxt;
	private AutoCompleteTextView message;
	private Button 				 send;
	private Button               addAttachment;
	
	private List<PGPPublicKeyRingModel>  pubKeyList;
	private List<PGPPrivateKeyRingModel> secKeyList;
	private List<String> toList;
	private List<String> fromList;
	private Email email;
	private String filePath;
	private ArrayList<File> attachments;
	
	private String passphrase;
	private int pgpMode;
	
	Settings settings;
	
	//TODO: need to be able to send an email to more than ONE recipient
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_compose);
		
		//get list of public/private keys
		PGP.populateKeyRingCollections(this);
		pubKeyList = PGP.getPublicKeys();
		secKeyList = PGP.getPrivateKeys();
		
		//extract the user id's (contacts) from keys held in the public/private collections
		toList = PGP.getUserIdsFromKeyringCollection(pubKeyList);
		fromList = PGP.getUserIdsFromKeyringCollection(secKeyList);
		
		settings = new Settings(this);
		
		//add the keys to the spinners
		addKeysToSpinners();
		
		//get the default pgp mode from settings
		pgpMode = settings.getDefaultMode();
		
		//get the views from the id file
		message       = (AutoCompleteTextView) findViewById(R.id.composebody);
		subjectTxt    = (EditText) 			   findViewById(R.id.subjecttxt);
		send          = (Button) 			   findViewById(R.id.send);
		addAttachment = (Button)               findViewById(R.id.attachbutton);
		
		/*
		 * the add attachment button will bring up a dialog to select a file
		 */
		addAttachment.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0) {
				Intent myIntent = new Intent(ComposeActivity.this, FileChooserActivity.class);
				ComposeActivity.this.startActivityForResult(myIntent, 0);
			}
			
		});
		
		/*
		 * Send Button Listener
		 */
		send.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0)
			{	
				
				//show the pass dialog to obtain the secret passphrase
				if(pgpMode == PGP.PGP_SIGN_ONLY | pgpMode == PGP.PGP_ENCRYPT_AND_SIGN)
				{
					showPassDialog();
				}
				else
				{
					prepareEmail(pgpMode);
				}
			}
			
		});
	}
	
	/*
	 * Get the file path back from the File chooser activity.
	 * 
	 * Code based on example from aFileDialog User Guide at: 
	 * 	   https://code.google.com/p/afiledialog/
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == Activity.RESULT_OK)
		{
			boolean fileCreated = false;
			
			Bundle bundle = data.getExtras();
			if(bundle != null)
			{
				if(bundle.containsKey(FileChooserActivity.OUTPUT_NEW_FILE_NAME))
				{
					fileCreated = true;
					File folder = (File) bundle.get(FileChooserActivity.OUTPUT_FILE_OBJECT);
					String name = bundle.getString(FileChooserActivity.OUTPUT_NEW_FILE_NAME);
					filePath = folder.getAbsolutePath() + "/" + name;
				}
				else
				{
					fileCreated = false;
					File file = (File) bundle.get(FileChooserActivity.OUTPUT_FILE_OBJECT);
					filePath = file.getAbsolutePath();
				}
				hasAttachment = true;
				
				//disable attachment button...for now (maybe we'll allow the user to add another button).
				addAttachment.setEnabled(false);
				
				String message = fileCreated? "File created" : "File opened";
				message += ": " + filePath;
				Log.i("Attachment picker", message);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.compose, menu);
		return true;
	}
	
	public void addKeysToSpinners()
	{
		//public keys
		pubKeySpinner = (Spinner) findViewById(R.id.selectpublic);
		ArrayAdapter<String> pubKeyAdapter =
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, toList);
		pubKeyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		pubKeySpinner.setAdapter(pubKeyAdapter);
		
		//private keys
		privateKeySpinner = (Spinner) findViewById(R.id.selectprivate);
		ArrayAdapter<String> privateKeyAdapter =
				new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fromList);
		privateKeyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		privateKeySpinner.setAdapter(privateKeyAdapter);
	}
	
	public void prepareEmail(int pgpMode)
	{
		/*
		 * if the device is connected let's proceed with constructing
		 * and sending the email
		 */
		Log.d(LOG_TAG, "is this device connected? " + String.valueOf(isDeviceConnected()));
		if(isDeviceConnected())
		{
			/*
			 * Let's get the data from the fields and check if the user
			 * has filled in all the mandatory fields
			 */
			messageBody = message.getText().toString();
			hasBody = messageBody.length() > 0;
			
			String to[] = {(String) pubKeySpinner.getSelectedItem()};
			String from[] = {(String) privateKeySpinner.getSelectedItem()};
			hasSenderReceiver = to[0].length() > 0 && from[0].length() > 0;
			
			String subject = subjectTxt.getText().toString();
			
			if(hasBody && hasSenderReceiver)
			{
				//send the email
				try {
					
					//is there an attachment?
					if(hasAttachment)
					{
						prepareAttachment();
					}
					
					//perform the cryptography - encrypt, sign or encrypt and sign.
					String pgpified = performCryptographicFunction(pgpMode, messageBody);
					
					//construct the email with the 'PGP-ified' body
					email = new Email(from, to, subject, pgpified, attachments, pgpMode);
					
					if(subject.isEmpty())
					{
						showSubjectDialog();
					}
					else
					{
						sendEmail();
					}	
				} catch (IOException e) {
					e.printStackTrace();
				} catch (MessagingException e) {
					e.printStackTrace();
				}
			}
			else
			{
				Toast.makeText(getApplicationContext(), "Please fill all mandatory fields", Toast.LENGTH_SHORT)
					.show();
			}
		}
		else
		{
			Toast.makeText(getApplicationContext(), "Please connect to the internet", Toast.LENGTH_SHORT)
				.show();
		}
	}
	
	private void prepareAttachment()
	{
		//encrypt the attachment
		String fileName = filePath.substring(filePath.lastIndexOf("/"), filePath.length());
		Log.d(LOG_TAG, "preparing to attach file " + filePath + " to email");
		
		//where to output the file
		String output = getApplicationContext().getCacheDir() + fileName + ".ipg";
		Log.d(LOG_TAG, "outputting encrypted file here " + output);
		
		//get the encryption subkey from the keyring
		PGPPublicKey pubKey = (PGPPublicKey) PGP.getSubKeyFromKeyRing(
				pubKeyList.get(pubKeySpinner.getSelectedItemPosition()), PGPPublicKey.RSA_ENCRYPT);
		
		//get the private key to sign with
		PGPSecretKey secKey = secKeyList.get(privateKeySpinner.getSelectedItemPosition()).getSecretKey();
		
		//output stream for signed file
		OutputStream out = null;
		switch(pgpMode)
		{
			case PGP.PGP_SIGN_ONLY:
				
				try {
					out = new FileOutputStream(output);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				try {
					Sign.signFile(filePath, secKey, out, passphrase.toCharArray(), true);
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (NoSuchProviderException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SignatureException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (PGPException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
			case PGP.PGP_ENCRYPT_ONLY:
				
				try {
					Crypto.encryptFile(output, filePath, pubKey, true, true);
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					Toast.makeText(getApplicationContext(), 
							"Key is either revoked or expired, choose another", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case PGP.PGP_ENCRYPT_AND_SIGN:
				//get the encryption subkey from the keyring
				PGPPublicKey pubKey1 = (PGPPublicKey) PGP.getSubKeyFromKeyRing(
						pubKeyList.get(pubKeySpinner.getSelectedItemPosition()), PGPPublicKey.RSA_ENCRYPT);
				try {
					Crypto.encryptFile(output, filePath, pubKey, true, true);
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					Toast.makeText(getApplicationContext(), 
							"Key is either revoked or expired, choose another", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				try {
					File encSignedFile = new File(getApplicationContext().getCacheDir().getAbsolutePath() + fileName + ".ipg");
					out = new FileOutputStream(output);
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				try {
					Sign.signFile(output, secKey, out, passphrase.toCharArray(), true);
				} catch (NoSuchAlgorithmException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (NoSuchProviderException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SignatureException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (PGPException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				break;
		}
		
		File att = new File(output);
		
		//add the file to the attachments array
		attachments = new ArrayList<File>();
		attachments.add(att);
	}
	
	private void sendEmail()
	{
		Boolean success = false;
		try {
			success = (Boolean) new SendMail(this).execute(email).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		//notify user of success
		if(success)
			Toast.makeText(getApplicationContext(), "sent email successfully"
					, Toast.LENGTH_SHORT).show();
		//end the activity and return to the previous one
		super.finish();
	}

	/**
	 * Performs a cryptographic function such as Encryption,
	 * Signing or Encrypting and Signing.
	 * 
	 * @param pgpMode - what type of function to perform
	 * @param msg - the msg to perform the function upon
	 * @return - the result of the crypto function
	 */
	private String performCryptographicFunction(int pgpMode, String msg)
	{
	
		//add a newline to the beginning and end of the msg
		String body = PGP.NEW_LINE + msg + PGP.NEW_LINE;
		String result = "";
		
		/*
		 * we need to perform different crypto functions
		 * depending on the pgp mode. we will either
		 * encrypt, sign or encrypt AND sign.
		 */
		switch(pgpMode)
		{
			case PGP.PGP_ENCRYPT_ONLY:
				/*
				 * create the body of the message (encrypt)
				 */
				
				//get the public key to encrypt with
				
				PGPPublicKey pubKey = (PGPPublicKey) PGP.getSubKeyFromKeyRing(
						pubKeyList.get(pubKeySpinner.getSelectedItemPosition()), PGPPublicKey.RSA_ENCRYPT);
				try {
					result = new String(Crypto.encryptString(body.getBytes(), pubKey, false, true), "UTF-8");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (PGPException e1) {
					e1.printStackTrace();
				} catch (InvalidKeyException e) {
					//key is invalid
					Toast.makeText(getApplicationContext(), 
							"Key is either revoked or expired, choose another", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
				break;
			case PGP.PGP_SIGN_ONLY:
				
				//get the private key to sign with
				PGPSecretKey secKey = secKeyList.get(privateKeySpinner.getSelectedItemPosition()).getSecretKey();
				try {
					
					result = new String(Sign.signClearText(body, secKey, passphrase.toCharArray(), "SHA1"), "UTF-8");
					
				} catch (SignatureException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case PGP.PGP_ENCRYPT_AND_SIGN:

				//get the public key to encrypt with
				PGPPublicKey encKey = (PGPPublicKey) PGP.getSubKeyFromKeyRing(pubKeyList.get(
						pubKeySpinner.getSelectedItemPosition()), PGPPublicKey.RSA_ENCRYPT);
				
				//get the private key to sign with
				//TODO: is this right? think i should improve the getsubkeyfromkeyring method to get the signing key
				PGPSecretKey signKey = secKeyList.get(privateKeySpinner.getSelectedItemPosition()).getSecretKey();
				try {
					
					/*
					 * create the body of the message (encrypt)
					 */
					result = new String(Crypto.encryptString(body.getBytes(), encKey, false, true), "UTF-8");
					
					/*
					 * then sign the body of the message
					 */
					//TODO: not sure if we need to create a UTF-8 string here or not!?
					result = new String(Sign.signClearText(result, signKey, passphrase.toCharArray(), "SHA1"), "UTF-8");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (PGPException e1) {
					e1.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
				} catch (SignatureException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					//key is invalid
					Toast.makeText(getApplicationContext(), 
							"Key is either revoked or expired, choose another", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
				break;
		}
		
		//TODO: remove this
		Log.d(LOG_TAG, "ComposeActivity - message body = " + result);
		return result;
	}
	
	/**
	 * Checks if the device is connected to the Internet
	 * 
	 * @return
	 */
	public boolean isDeviceConnected()
	{
		/*
		 * Let's check if we have an internet connection
		 */
		ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo network = cm.getActiveNetworkInfo();
		boolean isConnected = network != null && network.isConnected();
		return isConnected;
	}
	
	public void showPassDialog()
	{
		// Create an instance of the confirm dialog and show it
		SecretKeyPassphraseDialog skpd = SecretKeyPassphraseDialog.newInstance();
		skpd.show(getFragmentManager(), SecretKeyPassphraseDialog.TAG);
	}
	
	public void showSubjectDialog()
	{
		//Create an instance of the confirm dialog and show it
		SubjectAlertDialog sad = SubjectAlertDialog.newInstance();
		sad.show(getFragmentManager(), SubjectAlertDialog.TAG);
	}

	@Override
	public void onDialogDone(String tag, String pass, long keyId) {
		
		//if we are receiving the call back from the PASS dialog
		if(tag.equals(SecretKeyPassphraseDialog.TAG))
		{
			//obtain the passphrase from the dialog callback
			this.passphrase = pass;
			prepareEmail(pgpMode);
		}
		//if we are receiving the call back from the SUBJ dialog
		else if(tag.equals(SubjectAlertDialog.TAG))
		{
			sendEmail();
		}
		
	}
}
