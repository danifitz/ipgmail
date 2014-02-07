package uk.ac.brookes.danielf.pgpmail.activities;

import com.example.uk.ac.brookes.danielf.pgpmail.R;
import com.example.uk.ac.brookes.danielf.pgpmail.R.layout;
import com.example.uk.ac.brookes.danielf.pgpmail.R.menu;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

/**
 * This activity should be used to list keys such as when a
 * user selects the public or private key to be used
 * when composing an email, all public or private keys
 * should be displayed. Expects 2 extras in the bundle when
 * starting this activity, the activity class that started it so
 * we can return there once a key is chosen (there might be an 
 * Android mechanism that means we don't need to do this,
 * and whether public or private keys should be listed).
 */
public class ListKeys extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_keys);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.list_keys, menu);
		return true;
	}

}
