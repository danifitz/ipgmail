package uk.ac.brookes.danielf.pgpmail.activities;

import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

/**
 * This class should be the first displayed
 * and kick off all the initialization procedures
 * such as importing keys from the DB.
 * @author danfitzgerald
 *
 */
public class InitializeActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_initialize);
		
		PGP.populateKeyRingCollections(this);
		
		Intent myIntent = new Intent(InitializeActivity.this, MenuActivity.class);
		InitializeActivity.this.startActivity(myIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.initialize, menu);
		return true;
	}

}
