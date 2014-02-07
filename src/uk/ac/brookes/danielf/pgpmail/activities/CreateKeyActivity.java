package uk.ac.brookes.danielf.pgpmail.activities;

import java.util.Date;

import uk.ac.brookes.danielf.pgpmail.crypto.RSAKeyGenerator;
import uk.ac.brookes.danielf.pgpmail.dialogs.ComposeEmailAlertDialog;
import uk.ac.brookes.danielf.pgpmail.dialogs.ConfirmKeyDetailsDialog;
import android.app.Activity;
import android.app.DialogFragment;
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

public class CreateKeyActivity extends Activity implements uk.ac.brookes.danielf.pgpmail.dialogs.ConfirmKeyDetailsDialog.NoticeDialogListener {

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
			switch(keyId)
			{
				case -1:
					//no selection
					Toast.makeText(getApplicationContext(), "Please select a key size", Toast.LENGTH_SHORT).show();
					return false; //lets not continue if there is no key size
				case R.id.small:
					//512 bit key
					keySize = 512;
					break;
				case R.id.medium:
					//1024 bit key
					keySize = 1024;
					break;
				case R.id.large:
					//2048 bit key
					keySize = 2048;
					break;
				default:
					//do nothing
					break;
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
        DialogFragment dialog = new ConfirmKeyDetailsDialog(keyLength, expiry, name, email);
        dialog.show(getFragmentManager(), "ConfirmKeyDetailsDialog");
	}

	@Override
	public void onDialogPositiveClick(DialogFragment dialog) {

		RSAKeyGenerator rsaGen = new RSAKeyGenerator(this);
		try {
			rsaGen.generate(id, passphrase.toCharArray(), keySize);
		} catch (Exception e) {
			Log.e("CreateKeyActivity - create()", "Exception creating keys", e);
			e.printStackTrace();
		}
		
		Log.i("CreateKeyActivity", "attempting to create key");
		
		//this activity is over so let's finish and return to the previous activity
		super.finish();
	}

	@Override
	public void onDialogNegativeClick(DialogFragment dialog) {
		//do nothing
	}

}
