package uk.ac.brookes.danielf.pgpmail.intro;

import uk.ac.brookes.danielf.pgpmail.activities.CreateKeyActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class KeysIntroActivity extends Activity {

	private Button next2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_keys_intro);
		
		next2 = (Button) findViewById(R.id.next2);
		
		next2.setOnClickListener(new OnClickListener() 
		{

			@Override
			public void onClick(View arg0) {
				//start the key creation activity
				Intent myIntent = new Intent(KeysIntroActivity.this, CreateKeyActivity.class);
				KeysIntroActivity.this.startActivity(myIntent);
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.keys_intro, menu);
		return true;
	}

}
