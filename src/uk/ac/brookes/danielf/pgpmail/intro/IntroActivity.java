package uk.ac.brookes.danielf.pgpmail.intro;

import uk.ac.brookes.danielf.pgpmail.activities.SettingsActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class IntroActivity extends Activity {

	private Button nextBtn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_intro);
		
		nextBtn = (Button) findViewById(R.id.nextbtn);
		nextBtn.setOnClickListener(new OnClickListener() 
		{

			@Override
			public void onClick(View arg0) {
				Intent myIntent = new Intent(IntroActivity.this, SettingsActivity.class);
				IntroActivity.this.startActivity(myIntent);
			}
			
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.intro, menu);
		return true;
	}

}
