package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import org.spongycastle.openpgp.PGPException;

import uk.ac.brookes.danielf.pgpmail.crypto.Crypto;
import uk.ac.brookes.danielf.pgpmail.crypto.Sign;
import uk.ac.brookes.danielf.pgpmail.dialogs.PGDialogListener;
import uk.ac.brookes.danielf.pgpmail.dialogs.SecretKeyPassphraseDialog;
import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import ar.com.daidalos.afiledialog.FileChooserActivity;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class DecodeVerify extends Activity implements PGDialogListener{

	private TextView path;
	private Button choose;
	private Button action;

	//path of the chosen file
	private String filePath;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_decode_verify);

		//find views by id
		path = (TextView) findViewById(R.id.filepath);
		choose = (Button) findViewById(R.id.choose);
		action = (Button) findViewById(R.id.actionbtn);

		choose.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent myIntent = new Intent(DecodeVerify.this,
						FileChooserActivity.class);
				DecodeVerify.this.startActivityForResult(myIntent, 0);
			}

		});

		action.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showPassphraseDialog();
			}

		});
	}

	/*
	 * Get the file path back from the File chooser activity.
	 * 
	 * Code based on example from aFileDialog User Guide at:
	 * https://code.google.com/p/afiledialog/
	 * 
	 * @see android.app.Activity#onActivityResult(int, int,
	 * android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			boolean fileCreated = false;

			Bundle bundle = data.getExtras();
			if (bundle != null) {
				if (bundle
						.containsKey(FileChooserActivity.OUTPUT_NEW_FILE_NAME)) {
					fileCreated = true;
					File folder = (File) bundle
							.get(FileChooserActivity.OUTPUT_FILE_OBJECT);
					String name = bundle
							.getString(FileChooserActivity.OUTPUT_NEW_FILE_NAME);
					filePath = folder.getAbsolutePath() + "/" + name;
				} else {
					fileCreated = false;
					File file = (File) bundle
							.get(FileChooserActivity.OUTPUT_FILE_OBJECT);
					filePath = file.getAbsolutePath();
				}

				// set the file path text view to the file we chose
				path.setText(filePath);

				String message = fileCreated ? "File created" : "File opened";
				message += ": " + filePath;
				Log.i("Attachment picker", message);
			}
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

		AlertDialog ad = new AlertDialog.Builder(DecodeVerify.this).create();
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
	
	private void showNoKeyDialog()
	{
		AlertDialog ad = new AlertDialog.Builder(DecodeVerify.this).create();
		
		//set the title and message
		ad.setTitle("Cannot Decrypt");
		ad.setMessage("No public key found in keyring capable of verifying signature.");
		
		ad.setIcon(R.drawable.no);
		
		ad.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				DecodeVerify.this.finish();
			}
			
		});
		
		ad.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.decode_verify, menu);
		return true;
	}

	@Override
	public void onDialogDone(String tag, String pass, long keyId) {
		
		//get the default dir to decode to from settings
		Settings settings = new Settings(DecodeVerify.this);
		
		//the string to hold the sig verification status
		Pair<String, Boolean> result = new Pair<String, Boolean>(null, null);
		
		//create a new file to store the decrypted file in 
		File decryptedFile = null;
		
		if (filePath != null) {
			boolean encrypted = false;
			boolean signed = false;
			File file = new File(filePath);
			try {
				
				//get the name of the file without the last extension such as '.ipg' or '.gpg'
				String newfilename = file.getName().substring(0, file.getName().lastIndexOf("."));
				decryptedFile = new File(settings.getDecodeDir() + File.separator + newfilename);
				
				Crypto.decryptFile(file.getAbsolutePath(),
						pass.toCharArray(), decryptedFile);
				
				encrypted = true;
			} catch (NoSuchProviderException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				
				showNoKeyDialog();
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				// if we catch the exception due to the file being
				// signed then let's verify the file instead
				if (e.getMessage().contains("unknown object")) {
					FileInputStream fis = null;
					
					//file to output to
					File outFile = new File(settings.getDecodeDir() + File.separator + file.getName().substring(0, file.getName().lastIndexOf(".")));
					try {
						fis = new FileInputStream(file);
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					
					try {
						result = Sign.verifyFile(fis, outFile);
						
						signed = true;
					} catch (SignatureException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (PGPException e1) {
						e1.printStackTrace();
					} catch (IllegalArgumentException e1) {
						
					}
				}
			} catch (PGPException e) {
				
				// if we catch the exception due to the file being
				// signed then let's verify the file instead
				if (e.getMessage().contains("signed message")) {
					FileInputStream fis = null;
					
					//file to output to
					File outFile = new File(settings.getDecodeDir() + File.separator + file.getName().substring(0, file.getName().lastIndexOf(".")));
					try {
						fis = new FileInputStream(file);
					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
					
					try {
						result = Sign.verifyFile(fis, outFile);
						
						signed = true;
					} catch (SignatureException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (PGPException e1) {
						e1.printStackTrace();
					} catch (IllegalArgumentException e1) {
						showNoKeyDialog();
						
						e1.printStackTrace();
					}
				}
			}
			
			if(encrypted)
			{
				AlertDialog ad = new AlertDialog.Builder(DecodeVerify.this).create();
				
				//set the button
				ad.setButton(DialogInterface.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						DecodeVerify.this.finish();
					}
					
				});
				
				//set the message & title
				ad.setTitle("Decrypting complete");
				ad.setMessage(file.getName() + " decrypted to " + decryptedFile);
				
				ad.setIcon(R.drawable.tick);
				
				ad.show();
			}
			else if(signed)
			{
				showSignatureDialog(result.first, result.second);
			}
		} 
		else {
			Toast.makeText(
					getApplicationContext(),
					"Please choose a file first", 
					Toast.LENGTH_SHORT).show();
		}
	}
}
