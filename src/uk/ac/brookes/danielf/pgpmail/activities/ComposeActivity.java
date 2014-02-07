 package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
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
import uk.ac.brookes.danielf.pgpmail.dialogs.ComposeEmailAlertDialog;
import uk.ac.brookes.danielf.pgpmail.email.Email;
import uk.ac.brookes.danielf.pgpmail.email.SendMail;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
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

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class ComposeActivity extends Activity implements ComposeEmailAlertDialog.NoticeDialogListener {

	private String 				 messageBody;
	
	private boolean				 hasBody;
	private boolean				 hasSenderReceiver;
	
	private Spinner				 pubKeySpinner, privateKeySpinner;
	private EditText			 subjectTxt;
	private AutoCompleteTextView message;
	private Button 				 send;
	
	private List<PGPPublicKeyRingModel>  pubKeyList;
	private List<PGPPrivateKeyRingModel> secKeyList;
	private List<String> toList;
	private List<String> fromList;
	private Email email;
	
	//TODO: handle attachments!
	
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
		
		//add the keys to the spinners
		addKeysToSpinners();
		
		//get the views from the id file
		message =    (AutoCompleteTextView) findViewById(R.id.composebody);
		subjectTxt = (EditText) 			findViewById(R.id.subjecttxt);
		send =       (Button) 				findViewById(R.id.send);
		
		/*
		 * Send Button Listener
		 */
		send.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				/*
				 * if the device is connected let's proceed with constructing
				 * and sending the email
				 */
				Log.d("is device connected?", String.valueOf(isDeviceConnected()));
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
							
							//get the default pgp mode from settings
							Settings settings = new Settings(getApplicationContext());
							int pgpMode = settings.getDefaultMode();
							
							//perform the cryptography - encrypt, sign or encrypt and sign.
							String pgpified = performCryptographicFunction(pgpMode, messageBody);
							
							//construct the email with the 'PGP-ified' body
							email = new Email(from, to, subject, pgpified, null, pgpMode);
							
							//if there is no subject then ask the user if they want
							//to send the email without one
							if(subject.length() == 0)
							{
								showEmailDialog();
							}
							//if there is a subject then send the email
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
					Toast.makeText(getApplicationContext(), "No network connectivity", Toast.LENGTH_SHORT)
						.show();
				}
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
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
	
	public void sendEmail()
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
	
	public void showEmailDialog()
	{
		// Create an instance of the dialog fragment and show it
        DialogFragment dialog = new ComposeEmailAlertDialog();
        dialog.show(getFragmentManager(), "ComposeEmailAlertDialog");
	}

	// The dialog fragment receives a reference to this Activity through the
    // Fragment.onAttach() callback, which it uses to call the following methods
    // defined by the NoticeDialogFragment.NoticeDialogListener interface
	@Override
	public void onDialogPositiveClick(DialogFragment dialog)
	{
		sendEmail();
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		//do nothing, let user enter a subject and hit send again
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
		//add a newline to the end of the body of the msg
		String body = msg + "\r\n";
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
				PGPPublicKey pubKey = (PGPPublicKey) PGP.getSubKeyFromKeyRing(pubKeyList.get(0), PGPPublicKey.RSA_ENCRYPT);
				try {
					result = new String(Crypto.encrypt(body.getBytes(), pubKey, false, true), "UTF-8");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (PGPException e1) {
					e1.printStackTrace();
				}
				break;
			case PGP.PGP_SIGN_ONLY:
				/*
				 * create the body of the message (sign)
				 */
				
				//get the private key to sign with
				PGPSecretKey secKey = secKeyList.get(0).getSecretKey();
				try {
					//TODO: not sure if we need to create a UTF-8 string here or not!?
					//TODO: show passphrase dialog to obtain passphrase to sign
					result = new String(Sign.signFile(body, secKey, "hhhhhhhh".toCharArray(), "SHA1"), "UTF-8");
					
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
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
				PGPPublicKey encKey = (PGPPublicKey) PGP.getSubKeyFromKeyRing(pubKeyList.get(0), PGPPublicKey.RSA_ENCRYPT);
				
				//get the private key to sign with
				//TODO: is this right? think i should improve the getsubkeyfromkeyring method
				PGPSecretKey signKey = secKeyList.get(0).getSecretKey();
				try {
					
					/*
					 * create the body of the message (encrypt)
					 */
					result = new String(Crypto.encrypt(body.getBytes(), encKey, false, true), "UTF-8");
					
					/*
					 * then sign the body of the message
					 */
					//TODO: not sure if we need to create a UTF-8 string here or not!?
					//TODO: show passphrase dialog to obtain passphrase to sign
					result = new String(Sign.signFile(result, signKey, "hhhhhhhh".toCharArray(), "SHA1"), "UTF-8");
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
				}
				break;
		}
		
		//TODO: remove this
		Log.d("ComposeActivity - message body = ", result);
		return result;
	}
	
	/**
	 * Checks if the device is connected to the Internet
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
}
