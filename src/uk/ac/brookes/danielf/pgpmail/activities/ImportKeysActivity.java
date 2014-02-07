package uk.ac.brookes.danielf.pgpmail.activities;

import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class ImportKeysActivity extends Activity {

	private SearchView search;
	private EditText   serverTxt;
	private ListView   resultsListView;
	
	private String     serverUri;
	/*
	 * TODO: parameterize this list, however we don't know what
	 * type we will put the results from the server into yet,
	 * let's leave it blank for now. it will most likely look
	 * some thing like List<PGPPublicKey>.
	 */
	private List       resultsList;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_import_keys);
		
		//find views by Id
		search = (SearchView)        findViewById(R.id.search);
		serverTxt = (EditText)       findViewById(R.id.serveruri);
		resultsListView = (ListView) findViewById(R.id.results);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.import_keys, menu);
		return true;
	}

}
