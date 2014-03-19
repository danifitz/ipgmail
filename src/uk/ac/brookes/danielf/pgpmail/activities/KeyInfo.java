package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;

import uk.ac.brookes.danielf.pgpmail.crypto.Sign;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.dialogs.KeyPickerDialog;
import uk.ac.brookes.danielf.pgpmail.dialogs.PGDialogListener;
import uk.ac.brookes.danielf.pgpmail.dialogs.SecretKeyPassphraseDialog;
import uk.ac.brookes.danielf.pgpmail.internal.KeyServer;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

/**
 * This activity should display information about a specific key.
 * It will be displayed if a user selects a key from the manage
 * keys activity.
 * @author danfitzgerald
 *
 */
public class KeyInfo extends Activity implements PGDialogListener {

	private final String LOG_TAG = "KEY_INFO";
	
	private TextView keyIdTxt;
	private TextView algorithmTxt;
	private TextView keyLengthTxt;
	private TextView createdTxt;
	private TextView expirationTxt;
	private TextView trustDataTxt;
	private TextView keyType;
	private Button   sign;
	private Button   revoke;
	private Button   export;
	
	private String   pass;
	
	long keyId;
	long keyIdToSignWith;
	
	private boolean signing;
	private boolean revoking;
	
	/*
	 * ADD A BUTTON TO REVOKE A KEY!
	 * 
	 * can you revoke a private key? or just a public one?
	 * 
	 * when we revoke a key should we remove it from the db?
	 * 
	 * when we revoke a key should there be the option to
	 * upload that key to a keyserver, making sure the revocation
	 * is propagated to the outside world?
	 * 
	 * When revoking a key how do we know that the
	 * key belongs to us? Can we revoke any old key?
	 */
	
	/*
	 * ADD A BUTTON TO SIGN A KEY
	 * 
	 * OK, we would only want to sign a public key.
	 * This is related to trust i.e if we trust the
	 * key we add our signature to it. More signatures
	 * the more trusted it is. Remember if we're modifying
	 * a key i.e adding a signature then we need to remember
	 * to update the key in the db.
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_key_info);
		
		//find the views by Id
		keyIdTxt      =  (TextView) findViewById(R.id.keyid);
		algorithmTxt  =  (TextView) findViewById(R.id.algorithm);
		keyLengthTxt  =  (TextView) findViewById(R.id.keylength);
		createdTxt    =  (TextView) findViewById(R.id.created);
		expirationTxt =  (TextView) findViewById(R.id.expiry);
		trustDataTxt  =  (TextView) findViewById(R.id.trustlevel);
		keyType        = (TextView) findViewById(R.id.keytype);
		sign          =  (Button)   findViewById(R.id.sign);
		revoke        =  (Button)   findViewById(R.id.decryptBtn);
		export        =  (Button)   findViewById(R.id.export);
		
		Intent myIntent = getIntent();
		
		//TODO: must find out how to handle this
		byte[] trustData = myIntent.getByteArrayExtra("trustdata");
		
		keyId = myIntent.getLongExtra("keyid", 0);
		int algo = myIntent.getIntExtra("algorithm", 0);
		int keyleng = myIntent.getIntExtra("keylength", 0);
		String created = myIntent.getStringExtra("created");
		String expiration = createExpirationDate(myIntent.getIntExtra("expiration", 0));
		boolean isMasterKey = myIntent.getBooleanExtra("keytype", false);
		String type = isMasterKey? "Master" : "Sub-key";
		
		keyIdTxt		.setText(String.valueOf(keyId));
		algorithmTxt	.setText(PGP.getAlgorithmAsString(algo));
		keyLengthTxt    .setText(String.valueOf(keyleng));
		createdTxt		.setText(created);
		expirationTxt	.setText(expiration);
		trustDataTxt    .setText("trust data");
		keyType         .setText(type);
		
		sign.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0) {
				signing = true;
				showKeyPickerDialog();
			}
			
		});
		
		revoke.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View arg0) {
				revoking = true;
				showPassphraseDialog();
			}
			
		});
		
		export.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				//get the key
				PGPPublicKey key = PGP.getKeyById(keyId);
				
				//get the keyring, we're going to export the whole public keyring (master and all sub keys)
				//we want the key exported as
				PGPPublicKeyRingModel pkrm = PGP.getKeyRingByKey(key);
				
				byte[] keyBytes = null;
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ArmoredOutputStream aos = new ArmoredOutputStream(baos);
				try {
					pkrm.encode(aos);
					aos.flush();
					baos.flush();
					aos.close();
					keyBytes = baos.toByteArray();
					baos.close();
				} catch (IOException e2) {
					Log.d(LOG_TAG, "IOException exporting key with id " + keyId);
					e2.printStackTrace();
				}
				
				Properties params = new Properties();
				params.put("action", KeyServer.ACTION_SUBMIT_KEY);
				params.put("key", keyBytes);
				
				try {
					new KeyServer(KeyInfo.this).execute(params).get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.key_info, menu);
		return true;
	}
	
	private void showPassphraseDialog()
	{
		// Create an instance of the passphrase dialog and show it
		SecretKeyPassphraseDialog spd = SecretKeyPassphraseDialog.newInstance();
		spd.show(getFragmentManager(), SecretKeyPassphraseDialog.TAG);
	}
	
	private void showKeyPickerDialog()
	{
		//create an instance of the key picker dialog and show it
		KeyPickerDialog kpd = KeyPickerDialog.newInstance(KeyInfo.this);
		kpd.show(getFragmentManager(), KeyPickerDialog.TAG);
	}
	
	public String createExpirationDate(int validDays)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.HOUR, (validDays * 24));
		String out = sdf.format(c.getTime());
		return out;
	}

	@Override
	public void onDialogDone(String tag, String pass, long keyIdToSignWith) {
		//do the signing of the key
		if(signing)
		{
			if(tag.equals(KeyPickerDialog.TAG))
			{
				this.keyIdToSignWith = keyIdToSignWith;
				showPassphraseDialog();
			}
			else if(tag.equals(SecretKeyPassphraseDialog.TAG))
			{
				//I know this isn't pretty, sorry
				final long keyID = keyId;
				
				Log.d(LOG_TAG, "signing key with id = "+ keyID + " with key id  = " + this.keyIdToSignWith);
				
				//add a signature on the key
				try {
					Sign.signKey(getApplicationContext(), keyID, this.keyIdToSignWith, pass.toCharArray());
				} catch (SignatureException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				}
				
				signing = false;
				
				Toast.makeText(getApplicationContext(), "Key certified", Toast.LENGTH_SHORT).show();
			}
		}
		
		/*
		 * When we handle the exceptions I need to figure out if
		 * we're not allowed to revoke the key if it will throw a sig or pgp exception
		 * then we can handle it and tell the user they are not allowed to revoke the
		 * key (probably because it doesn't belong to them!)
		 */
		//do the revocation
		else if(revoking)
		{
			if(tag.equals(SecretKeyPassphraseDialog.TAG))
			{
				try {
					Sign.revokeKey(getApplicationContext(), this.keyId, pass.toCharArray());
				} catch (SignatureException e) {
					//TODO: show a toast explaining why it failed
					e.printStackTrace();
				} catch (PGPException e) {
					//TODO: show a toast explaining why it failed 1
					e.printStackTrace();
				}
				
				revoking = false;
				
				Toast.makeText(getApplicationContext(), "Key revoked", Toast.LENGTH_SHORT).show();
			}
		}
	}
}
