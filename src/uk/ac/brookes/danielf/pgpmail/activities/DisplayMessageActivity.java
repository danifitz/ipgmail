package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.List;

import org.spongycastle.openpgp.PGPException;

import uk.ac.brookes.danielf.pgpmail.crypto.Crypto;
import uk.ac.brookes.danielf.pgpmail.crypto.Sign;
import uk.ac.brookes.danielf.pgpmail.dialogs.PGDialogListener;
import uk.ac.brookes.danielf.pgpmail.dialogs.SecretKeyPassphraseDialog;
import uk.ac.brookes.danielf.pgpmail.email.Email;
import uk.ac.brookes.danielf.pgpmail.exceptions.InvalidKeyException;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Pair;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class DisplayMessageActivity extends Activity implements PGDialogListener {

//	private final String LOG_TAG = "Display_Message_Activity";
	
	private TextView   fromTxt;
	private TextView   toTxt;
	private TextView   subjectTxt;
	private TextView   dateTxt;
	private TextView   bodyTxt;
	private TextView   attachmentLabel;
	
	private Button     button;
	
	private String     msg;
	private int        pgpMode;
	
	private List<File> attachments;
	
	private Email email;
	
	//TODO: how do i tell if a file is signed or encrypted and then what action to take?
	//TODO: handle attachments
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_message);
		
		//find views by Id
		fromTxt    = (TextView) findViewById(R.id.messageFrom);
		toTxt      = (TextView) findViewById(R.id.messageTo);
		subjectTxt = (TextView) findViewById(R.id.messageSubject);
		dateTxt    = (TextView) findViewById(R.id.messageDate);
		bodyTxt    = (TextView) findViewById(R.id.messageBody);
		button          = (Button)   findViewById(R.id.decryptBtn);
		attachmentLabel = (TextView) findViewById(R.id.attachments);
		
		//get the email 'parcel' extra from the intent
		Bundle b = getIntent().getExtras();
		email = b.getParcelable("com.example.uk.ac.brookes.danielf.pgpmail.email");
		attachments = email.getAttachments();
		
		//set all the text fields
		fromTxt.setText("From: " + Email.getStringArrayAsConcatString(email.getFrom()));
		toTxt.setText("To: " + Email.getStringArrayAsConcatString(email.getTo()));
		subjectTxt.setText("Subject: " + email.getSubject());
		dateTxt.setText("Sent: " + email.getSentDate().toString());
		bodyTxt.setText(email.getMsgBody());
		//set the body text view to be scrollable
		bodyTxt.setMovementMethod(new ScrollingMovementMethod());
		
		if(email.hasAttachments)
		{
			attachmentLabel.setText(attachments.size() + " attachments");
			attachments = email.getAttachments();
		}
		else
			attachmentLabel.setText("No attachments");
		
		pgpMode = email.getPgpMode();
		setButtonText(pgpMode);
		
		msg = email.getMsgBody();
		
		button.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View arg0)
			{
				//show the dialog to get the secret key passphrase from the user
				showPassphraseDialog();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.display_message, menu);
		return true;
	}
	
	/**
	 * Sets the text of the button based on
	 * whether the email needs to be verified, decrypted etc.
	 * @param pgpMode
	 */
	public void setButtonText(int pgpMode)
	{
		switch(pgpMode)
		{
			case PGP.PGP_ENCRYPT_ONLY:
				button.setText("Decrypt");
				break;
			case PGP.PGP_SIGN_ONLY:
				button.setText("Verify");
				break;
			case PGP.PGP_ENCRYPT_AND_SIGN:
				//TODO: figure out what we need to do here, we need to recall this once the decrypt step is done
				//so we can then verify the signature
			case PGP.PGP_UNENCRYPTED:
				button.setEnabled(false);
				break;
		}
	}
	
	private void showPassphraseDialog()
	{
		// Create an instance of the passphrase dialog and show it
		SecretKeyPassphraseDialog spd = SecretKeyPassphraseDialog.newInstance();
		spd.show(getFragmentManager(), SecretKeyPassphraseDialog.TAG);
	}
	
	private void showSignatureDialog(String sig, boolean verified)
	{
		
		AlertDialog ad = new AlertDialog.Builder(DisplayMessageActivity.this).create();
		ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				arg0.dismiss();
			}
		});
		if(verified)
			ad.setIcon(R.drawable.tick);
		else
			ad.setIcon(R.drawable.no);
		ad.setTitle("Verification status");
		ad.setMessage(sig);
		ad.show();
	}

	@Override
	public void onDialogDone(String tag, String pass, long keyId) {
		
		//handle the message itself
		//each mode requires different behaviour
		switch(pgpMode)
		{
			//let's decrypt
			case PGP.PGP_ENCRYPT_ONLY:
				try {
					//decrypt the message
					String clearTxt = new String(Crypto.decryptString(msg.getBytes(), PGP.getPrivateKeyRingCollection(), pass.toCharArray()), "UTF-8");

					//set the body of the email to the plain text
					bodyTxt.setText(clearTxt);
					
					//set the button text to reflect the content
					setButtonText(PGP.PGP_UNENCRYPTED);
					
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					AlertDialog ad = new AlertDialog.Builder(DisplayMessageActivity.this).create();
					
					//set the title and message
					ad.setTitle("Cannot Decrypt");
					ad.setMessage("No secret key found in keyring capable of decrypting.\n" +
									"Try importing the key for " + email.getFrom()[0]);
					
					ad.setIcon(R.drawable.no);
					
					ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							DisplayMessageActivity.this.finish();
						}
						
					});
					
					ad.show();
					
					e.printStackTrace();
				}
				break;
			//let's verify the signature
			case PGP.PGP_SIGN_ONLY:			
				//verify the signature
				Pair<String, Boolean> sigResult = new Pair<String, Boolean>(null,null);
				
				try {
					sigResult = Sign.verifyClearText(msg, PGP.getPubKeyRingCollection());
				} catch (SignatureException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (PGPException e1) {
					e1.printStackTrace();
				} catch (InvalidKeyException e1) {
					//key is invalid
					Toast.makeText(getApplicationContext(),
							"Private key used to sign the message is either expired or revoked", Toast.LENGTH_LONG).show();
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					AlertDialog ad = new AlertDialog.Builder(DisplayMessageActivity.this).create();
					
					//set the title and message
					ad.setTitle("Cannot Decrypt");
					ad.setMessage("No public key found in keyring capable of verifying signature.\n" +
									"Try importing the key for " + email.getFrom()[0]);
					
					ad.setIcon(R.drawable.no);
					
					ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							DisplayMessageActivity.this.finish();
						}
						
					});
					
					ad.show();
					
					e1.printStackTrace();
				}
				
				//show the signature dialog
				showSignatureDialog(sigResult.first, sigResult.second);
				break;
			//let's decrypt then verify
			case PGP.PGP_ENCRYPT_AND_SIGN:

				String signedTxt = "";
				try {
					//decrypt the message
					signedTxt = new String(Crypto.decryptString(msg.getBytes(), PGP.getPrivateKeyRingCollection(), pass.toCharArray()), "UTF-8");
				} catch (NoSuchProviderException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				}
				
				bodyTxt.setText(signedTxt);
				
				//verify the signature
				Pair<String, Boolean> encSigned = new Pair<String, Boolean>(null,null);
				
				try {
					encSigned = Sign.verifyClearText(msg, PGP.getPubKeyRingCollection());
				} catch (SignatureException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					//key is invalid
					Toast.makeText(getApplicationContext(), 
							"Key is either revoked or expired, choose another", Toast.LENGTH_LONG).show();
					e.printStackTrace();
				}
				
			//set the body of the email to the plain text
			bodyTxt.setText("verified!");
			
			//show the signature dialog
			showSignatureDialog(encSigned.first, encSigned.second);
			
			break;
		}
		
		//TODO: handle unencrypted files
		
		/*
		 * now handle the attachments, we are handling them independently
		 * of the message because i'm not sure if there might be a case where
		 * a message is encrypted and the attachments are signed or any other
		 * combination
		 */
		if(attachments.size() > 0)
		{
			Settings settings = new Settings(DisplayMessageActivity.this);
			
			//inform the user we have moved the attachments to the files dir
			AlertDialog ad = new AlertDialog.Builder(DisplayMessageActivity.this).create();
			
			//set the message
			ad.setMessage("Attachments moved to " + settings.getAttachDir());
			
			//set the icon
			ad.setIcon(R.drawable.tick);

			ad.setButton(DialogInterface.BUTTON_POSITIVE, "", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					//nothing...
				}
				
			});
			
			ad.show();
			
		}
	}
}