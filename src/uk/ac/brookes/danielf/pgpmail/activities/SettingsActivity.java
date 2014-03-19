package uk.ac.brookes.danielf.pgpmail.activities;

import java.util.ArrayList;

import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import uk.ac.brookes.danielf.pgpmail.intro.KeysIntroActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class SettingsActivity extends Activity {

	private Settings settings;
	
	private RelativeLayout layout;
	private Spinner pgpModeSpinner;
	private EditText smtpTxt;
	private EditText imapTxt;
	private EditText usernameTxt;
	private EditText passwordTxt;
	
	//spinner constants
	private final static int SPINNER_ENCRYPT          = 0;
	private final static int SPINNER_ENCRYPT_AND_SIGN = 1;
	private final static int SPINNER_SIGN_ONLY        = 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		settings = new Settings(this);
		
		//find views by Id
		layout         = (RelativeLayout) findViewById(R.id.settings_layout);
		pgpModeSpinner = (Spinner)        findViewById(R.id.pgpmode);
		smtpTxt        = (EditText)       findViewById(R.id.smtpserver);
		imapTxt        = (EditText)       findViewById(R.id.imapserver);
		usernameTxt    = (EditText)       findViewById(R.id.username);
		passwordTxt    = (EditText)       findViewById(R.id.password);
		
		/*
		 * if this is the first run we want to add a next
		 * button as this activity is part of a wider walkthrough
		 * rather than just the normal settings screen
		 */
		Button nextBtn = new Button(this);
		if(settings.getFirstRun())
		{
			nextBtn.setText("Next");
			
			//set layout params to place the button at the bottom right of the activity
			RelativeLayout.LayoutParams l_params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			l_params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			l_params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			
			//set the layout params of the button and add the view
			nextBtn.setLayoutParams(l_params);
			layout.addView(nextBtn);
		}
		
		nextBtn.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0) {
				Intent myIntent = new Intent(SettingsActivity.this, KeysIntroActivity.class);
				SettingsActivity.this.startActivity(myIntent);
			}
			
		});
		
		//fill the text boxes with settings
		String imap = settings.getIMAPServer();
		String smtp = settings.getSMTPServer();
		String user = settings.getEmailUsername();
		String pass = settings.getEmailPassword();
		
		if(imap != null)
			imapTxt.setText(imap);
		if(smtp != null)
			smtpTxt.setText(smtp);
		if(user != null)
			usernameTxt.setText(user);
		if(pass != null)
			passwordTxt.setText(pass);
		
		//populate the spinner and register a listener
		populateSpinner(settings.getDefaultMode());
		pgpModeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() 
		{

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int position, long id)
			{
				switch(position)
				{
					case SPINNER_ENCRYPT:
						settings.setDefaultMode(PGP.PGP_ENCRYPT_ONLY);
						break;
					case SPINNER_ENCRYPT_AND_SIGN:
						settings.setDefaultMode(PGP.PGP_ENCRYPT_AND_SIGN);
						break;
					case SPINNER_SIGN_ONLY:
						settings.setDefaultMode(PGP.PGP_SIGN_ONLY);
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		smtpTxt.addTextChangedListener(new TextWatcher() 
		{

			@Override
			public void afterTextChanged(Editable text) {
				settings.setSMTPServer(text.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {}
			
		});
		
		imapTxt.addTextChangedListener(new TextWatcher() 
		{

			@Override
			public void afterTextChanged(Editable text) 
			{
				settings.setIMAPServer(text.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {}
			
		});
		
		usernameTxt.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void afterTextChanged(Editable text) {
				String email = text.toString().toLowerCase();
				settings.setEmailUsername(email);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {}
		});
		
		passwordTxt.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void afterTextChanged(Editable text) {
				settings.setEmailPassword(text.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}
	
	/**
	 * Populates the default pgp mode spinner with options
	 */
	public void populateSpinner(int defaultMode) {
		ArrayList<String> pgpModeOptions = new ArrayList<String>();
		pgpModeOptions.add("Encrypt Only");
		pgpModeOptions.add("Encrypt & Sign");
		pgpModeOptions.add("Sign Only");
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, pgpModeOptions);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		pgpModeSpinner.setAdapter(spinnerAdapter);
		
		//set the spinner selection based on previous choice
		switch(defaultMode)
		{
			case PGP.PGP_ENCRYPT_ONLY:
				pgpModeSpinner.setSelection(0);
				break;
			case PGP.PGP_ENCRYPT_AND_SIGN:
				pgpModeSpinner.setSelection(1);
				break;
			case PGP.PGP_SIGN_ONLY:
				pgpModeSpinner.setSelection(2);
				break;
			default:
				//do nothing
		}
	}
}
