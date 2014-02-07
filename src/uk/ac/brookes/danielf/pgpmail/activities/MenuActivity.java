package uk.ac.brookes.danielf.pgpmail.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class MenuActivity extends Activity {
	
	ListView menu;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		
		menu = (ListView) findViewById(R.id.menu);
		menu.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position,
					long id) 
			{
				Intent myIntent = null;
				switch(position)
				{
				//compose
				case 0:
					myIntent = new Intent(MenuActivity.this, ComposeActivity.class);
					break;
				//decode
				case 1:
					myIntent = new Intent(MenuActivity.this, DecodeActivity.class);
					break;
				//settings
				case 2:
					myIntent = new Intent(MenuActivity.this, SettingsActivity.class);
					break;
				//manage keys
				case 3:
					myIntent = new Intent(MenuActivity.this, ManageKeysActivity.class);
					break;
				default:
					//do nothing
					break;
				}
				MenuActivity.this.startActivity(myIntent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

}
