package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.IOException;
import java.security.NoSuchProviderException;

import org.spongycastle.openpgp.PGPException;

import uk.ac.brookes.danielf.pgpmail.crypto.Crypto;
import uk.ac.brookes.danielf.pgpmail.crypto.Sign;
import uk.ac.brookes.danielf.pgpmail.dialogs.SecretKeyPassphraseDialog;
import uk.ac.brookes.danielf.pgpmail.dialogs.SignatureVerificationDialog;
import uk.ac.brookes.danielf.pgpmail.email.Email;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class DisplayMessageActivity extends Activity implements uk.ac.brookes.danielf.pgpmail.dialogs.SecretKeyPassphraseDialog.NoticeDialogListener {

	private TextView fromTxt;
	private TextView toTxt;
	private TextView subjectTxt;
	private TextView dateTxt;
	private TextView bodyTxt;
	
	private Button   button;
	
	private String msg;
	private int pgpMode;
	
	private String pass;
	private String confPass;
	
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
		button     = (Button)   findViewById(R.id.button1);
		
		//get the email 'parcel' extra from the intent
		Bundle b = getIntent().getExtras();
		Email email = b.getParcelable("email");
		
		//set all the text fields
		fromTxt.setText(email.getStringArrayAsConcatString(email.getFrom()));
		toTxt.setText(email.getStringArrayAsConcatString(email.getTo()));
		subjectTxt.setText(email.getSubject());
		dateTxt.setText(email.getSentDate().toString());
		bodyTxt.setText(email.getMsgBody());
		
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
	
	public void showPassphraseDialog()
	{
		// Create an instance of the passphrase dialog and show it
		DialogFragment dialog = new SecretKeyPassphraseDialog();
		dialog.show(getFragmentManager(), "SecretKey Passphrase Dialog");
	}
	
	public void showSignatureDialog(String sig)
	{
		//Create an instance of the signature dialog and show it
		DialogFragment dialog = new SignatureVerificationDialog(sig);
		dialog.show(getFragmentManager(), "Signature Verification Dialog");
	}

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {
		if(pass.equals(confPass))
		{
			//each mode requires different behaviour
			switch(pgpMode)
			{
				//let's decrypt
				case PGP.PGP_ENCRYPT_ONLY:
					try {
						//decrypt the message
						String clearTxt = new String(Crypto.decrypt(msg.getBytes(), PGP.getPrivateKeyRingCollection(), pass.toCharArray()), "UTF-8");

						//set the body of the email to the plain text
						bodyTxt.setText(clearTxt);
					} catch (NoSuchProviderException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (PGPException e) {
						e.printStackTrace();
					}
					break;
				//let's verify the signature
				case PGP.PGP_SIGN_ONLY:					
					//verify the signature
					String signed = "";
					try {
						signed = Sign.verifyFile(msg, PGP.getPubKeyRingCollection());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					//set the body of the email to the plain text
					bodyTxt.setText("verified!");
					
					//show the signature dialog
					showSignatureDialog(signed);
					break;
				//let's decrypt then verify
				case PGP.PGP_ENCRYPT_AND_SIGN:

					String signedTxt = "";
					try {
						//decrypt the message
						signedTxt = new String(Crypto.decrypt(msg.getBytes(), PGP.getPrivateKeyRingCollection(), pass.toCharArray()), "UTF-8");
					} catch (NoSuchProviderException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (PGPException e) {
						e.printStackTrace();
					}
					
					bodyTxt.setText(signedTxt);
					
					//verify the signature
					String encSigned = "";
					try {
						encSigned = Sign.verifyFile(msg, PGP.getPubKeyRingCollection());
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					//set the body of the email to the plain text
					bodyTxt.setText("verified!");
					
					//show the signature dialog
					showSignatureDialog(encSigned);
					
					break;
			}
		}
		//passwords don't match so let's display the dialog again
		else
		{
			Toast.makeText(getApplicationContext(), "Passphrases don't match, try again", 
					Toast.LENGTH_SHORT).show();
			showPassphraseDialog();
		}
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		
	}

	@Override
	public void getPass(String pass) {
		this.pass = pass;
	}

	@Override
	public void getConfPass(String confPass) {
		this.confPass = confPass;
	}
}
