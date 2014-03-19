package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import uk.ac.brookes.danielf.pgpmail.internal.KeyServer;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import uk.ac.brookes.danielf.pgpmail.internal.ResultsParser;
import uk.ac.brookes.danielf.pgpmail.internal.SearchResult;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class ImportKeysActivity extends Activity implements ResultsParser.ResultsListener {

	private EditText searchBox;
	private Button   searchBtn;
	private ListView resultsList;
	
	private ArrayList<SearchResult> searchResults;
	private ResultsAdapter ra;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_import_keys);
		
		//find the views by Id
		searchBox   = (EditText) findViewById(R.id.search);
		searchBtn   = (Button)   findViewById(R.id.searchbtn);
	    resultsList = (ListView) findViewById(R.id.results);
	    
	    searchBtn.setOnClickListener(new OnClickListener()
	    {

			@Override
			public void onClick(View arg0) {
				
				String term = searchBox.getText().toString();
				if(term.isEmpty() | term.trim().isEmpty())
					Toast.makeText(getApplicationContext(), "Please enter a search term",
							Toast.LENGTH_SHORT).show();
				else
				{
					if(isDeviceConnected())
					{	
						//set the properties for the type of query
						Properties props = new Properties();
						//put the action we wish to perform
						props.put("action", KeyServer.ACTION_SEARCH_USER);
						//put the search term
						props.put("term", searchBox.getText().toString());
						//verbose or normal index? verbose is horrible!
						props.put("indexmode", String.valueOf(KeyServer.KEY_SERVER_INDEX));
						//search with fingerprint on?
						props.put("fingerprint", "false");
						//only display exact matches to the search term?
						props.put("exactmatch", "false");
						
						Pair<Integer, String> result = new Pair<Integer, String>(null, null);
						try {
							
							//do the search
							result = new KeyServer(ImportKeysActivity.this).execute(props).get();
							
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						}
						
						//check the response code
						if(result.first == 200)
						{
							new ResultsParser(ImportKeysActivity.this).execute(result.second);
							
						}
						//no results found
						else if(result.first == 404)
						{
	    					Toast.makeText(getApplicationContext(), "No results found"
	    							, Toast.LENGTH_SHORT).show();
	    					
	    					//empty the search term box
	    					searchBox.setText("");
						}
						//some other unknown error
						else
						{
							Toast.makeText(getApplicationContext(), "Unknown error occurred - try again later"
									, Toast.LENGTH_SHORT).show();
							
							//empty the search term box
	    					searchBox.setText("");
						}
					}
				}
			}
	    });
	    
	    resultsList.setOnItemClickListener(new OnItemClickListener()
	    {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				SearchResult key = searchResults.get(position);
				Properties props = new Properties();
				props.put("action", KeyServer.ACTION_RETRIEVE_KEY);
				props.put("keyparams", key.getKeyLink());
				String keyText = "";
				try {
					keyText = new KeyServer(ImportKeysActivity.this).execute(props).get().second;
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				
				//grab the pgp key block from the returned xml
				String keys = keyText.substring(
						keyText.indexOf("-----BEGIN"), keyText.lastIndexOf("</pre>"));
				
				//replace all the newline characters
				keys.replaceAll("\n", "");
				keys.replaceAll("\r\n", "");
				
				//parse the key string into a public key
				try {
					PGP.parseKeyString(keys, getApplicationContext());
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				Toast.makeText(getApplicationContext(), 
						"Key imported", Toast.LENGTH_SHORT).show();
				
				//finish the activity and return to the previous activity
				ImportKeysActivity.this.finish();
			}
	    	
	    });
	    
	}
	
	//call back to receive the results
	@Override
	public void onResultsParsed(ArrayList<SearchResult> resultsList) {
		this.searchResults = resultsList;
		
		//create the adapter
		ra = new ResultsAdapter();
		
		this.resultsList.setAdapter(ra);
	}
	
	class ResultsAdapter extends BaseAdapter
	{
		
		LayoutInflater inflater;
		TextView type, keyid, date, userid;
		
		ResultsAdapter()
		{
			super();
			inflater = (LayoutInflater) ImportKeysActivity.this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public int getCount() {
			return searchResults.size();
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//setup the view for each row of the list
			
			View view = inflater.inflate(R.layout.key_search_result_row, null);
			
			type =   (TextView) view.findViewById(R.id.typetxt);
			keyid =  (TextView) view.findViewById(R.id.keyidtxt);
			date =   (TextView) view.findViewById(R.id.datetxt);
			userid = (TextView) view.findViewById(R.id.useridtxt);
			
			SearchResult result = searchResults.get(position);
			
			type.setText(result.getType());
			keyid.setText(result.getKeyId());
			date.setText(result.getDate());
			userid.setText(result.getUserId());
			
			return view;
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.import_keys, menu);
		return true;
	}
	
	/**
	 * Checks if the device is connected to the Internet
	 * @return
	 */
	public boolean isDeviceConnected()
	{
		/*
		 * Let's check if we have an internet connection
		 */
		ConnectivityManager cm = (ConnectivityManager) getApplicationContext()
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo network = cm.getActiveNetworkInfo();
		boolean isConnected = network != null && network.isConnected();
		return isConnected;
	}
}
