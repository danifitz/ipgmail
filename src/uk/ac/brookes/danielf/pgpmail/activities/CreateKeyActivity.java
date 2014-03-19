package uk.ac.brookes.danielf.pgpmail.activities;

import java.util.Date;

import uk.ac.brookes.danielf.pgpmail.crypto.RSAKeyGenerator;
import uk.ac.brookes.danielf.pgpmail.dialogs.ConfirmKeyDetailsDialog;
import uk.ac.brookes.danielf.pgpmail.dialogs.PGDialogListener;
import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class CreateKeyActivity extends Activity implements PGDialogListener {

	private final String LOG_TAG = "CREATE_KEY";
	
	private EditText    passphraseTxt;
	private EditText    confirmTxt;
	private RadioGroup  keySizes;   //default checked is ID = 1 or keySize = 1024
	private EditText    expiryDate;
	private EditText    nameTxt;
	private EditText    emailTxt;
	private Button      create;
	
	private String passphrase;
	private int keySize;
	private Date expiry;
	private String name;
	private String email;
	private String id;
	
	//TODO: implement key expiration and adding name as well as email to the key id
	//must be done in the RSAKeyGenerator class i think.
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_key);
		
		//Find the views by ID
		passphraseTxt = (EditText) findViewById(R.id.passphrase);
		confirmTxt =    (EditText) findViewById(R.id.confirm);
		keySizes =      (RadioGroup) findViewById(R.id.keysizegroup);
		expiryDate =    (EditText) findViewById(R.id.expiration);
		nameTxt =       (EditText) findViewById(R.id.name);
		emailTxt =      (EditText) findViewById(R.id.email);
		create =        (Button) findViewById(R.id.createbtn);
		
		//Create button onClick listener
		create.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				//create the key
				create();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.create_key, menu);
		return true;
	}
	
	/**
	 * Simple routine to check if the user has set all
	 * the mandatory fields on the page
	 * 
	 * @return true is yes, false if no
	 */
	private boolean areFieldsSet() 
	{
		//check if user has filled in all the mandatory fields
		if((passphraseTxt.getText().toString().length()      > 0) 
				&& (confirmTxt.getText().toString().length() > 0)
				&& (expiryDate.getText().toString().length() > 0)
				&& (nameTxt.getText().toString().length()    > 0)
				&& (emailTxt.getText().toString().length()   > 0))
		{
			
			return true;
		}
		else {
			Toast.makeText(getApplicationContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
		}
		return false;
	}
	
	/**
	 * Creates the key using info provided by the user on this page
	 */
	private boolean create()
	{
		if(areFieldsSet()) 
		{
			//get the key size
			int keyId = keySizes.getCheckedRadioButtonId();
			if (keyId == -1) {
				//no selection
				Toast.makeText(getApplicationContext(), "Please select a key size", Toast.LENGTH_SHORT).show();
				return false; //lets not continue if there is no key size
			} else if (keyId == R.id.smallest) {
				//512 bit key
				keySize = 512;
			} else if (keyId == R.id.mediumer) {
				//1024 bit key
				keySize = 1024;
			} else if (keyId == R.id.larger) {
				//2048 bit key
				keySize = 2048;
			} else {
			}
			
			//do the passphrases match?
			passphrase = passphraseTxt.getText().toString();
			String confirmation = confirmTxt.getText().toString();
			
			//concatenate name + email to make Id string
			id = nameTxt.getText().toString() + " <" + emailTxt.getText().toString() + ">";
			if(passphrase.equals(confirmation))
			{
				//display the confirmation dialog, if the user is happy with
				//the info shown they will trigger onDialogPositiveClick()
				showConfirmationDialog(keySize, expiryDate.getText().toString(), 
						nameTxt.getText().toString(), emailTxt.getText().toString());
			}
			else
			{
				return false;
			}
		}
		return true;
	}
	
	private void showConfirmationDialog(int keyLength, String expiry, String name, String email)
	{
		// Create an instance of the confirm dialog and show it
		ConfirmKeyDetailsDialog ckdd = ConfirmKeyDetailsDialog.newInstance(keyLength, expiry, name, email);
		ckdd.show(getFragmentManager(), ConfirmKeyDetailsDialog.TAG);
	}

	@Override
	public void onDialogDone(String tag, String pass, long keyId) {
		RSAKeyGenerator rsaGen = new RSAKeyGenerator(this);
		try {
			rsaGen.generate(id, passphrase.toCharArray(), keySize);
		} catch (Exception e) {
			Log.e("CreateKeyActivity - create()", "Exception creating keys", e);
			e.printStackTrace();
		}
		
		Log.i(LOG_TAG, "attempting to create key");
		
		//if this is part of the first run dialog then head to the menu
		Settings settings = new Settings(getApplicationContext());
		if(settings.getFirstRun())
		{
			/*
			 * this is the end of the first run walkthrough
			 * so set first run to false
			 */
			settings.setFirstRun(false);
			
			//head to the menu
			Intent myIntent = new Intent(this, MenuActivity.class);
			this.startActivity(myIntent);
		}
		//this activity is over so let's finish and return to the previous activity
		super.finish();
	}
}
