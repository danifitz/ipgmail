package uk.ac.brookes.danielf.pgpmail.activities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import uk.ac.brookes.danielf.pgpmail.email.Email;
import uk.ac.brookes.danielf.pgpmail.email.GetMail;
import uk.ac.brookes.danielf.pgpmail.internal.ScrollListView;
import uk.ac.brookes.danielf.pgpmail.internal.ScrollListView.OnBottomReachedListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class InboxActivity extends Activity implements GetMail.MailListener {

	private final static String LOG_TAG = "INBOX";
	
	private ArrayList<Email> emailList = new ArrayList<Email>();
	private EmailAdapter ma;
	
	private ScrollListView list;
	
	//we keep track of how many emails we've loaded from the inbox
	private int counter = 0;
	//number of emails to load at a time
	public final static Integer EMAILS_TO_LOAD = 24;
	
	//is it the first lot of emails we're loading
	private boolean first = true;
	
	//TODO: progress bar for loading emails!!!
	
	//TODO: catch authenticationfailedexception if the username or password is wrong
	
	/*
	 * In this activity we are loading 24 emails at a time
	 * into the list view. when the user scrolls to the bottom
	 * of the list we will load another 24 and so on.
	 */
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_decode);
		
		//find the list by id
		list = (ScrollListView) findViewById(R.id.listView);
		
		//if we are connected to the internet then fetch emails
		if(isDeviceConnected())
		{
			Pair<Integer, Integer> params = new Pair<Integer, Integer>(0, EMAILS_TO_LOAD);
			new GetMail(InboxActivity.this).execute(params);
			//increment counter
			counter += EMAILS_TO_LOAD;
			
			
			list.setOnBottomReachedListener(new OnBottomReachedListener() {

				@Override
				public void onBottomReached() {
					
					Pair<Integer, Integer> params = new Pair<Integer, Integer>(counter, EMAILS_TO_LOAD);
					new GetMail(InboxActivity.this).execute(params);
					counter += EMAILS_TO_LOAD;
				}
			});
			
			list.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					
					/*
					 * we want to start the display message activity so the user
					 * can look at the message. we need to pass the activity the email
					 * as an extra!
					 */
					Intent myIntent = new Intent(InboxActivity.this, DisplayMessageActivity.class);
					myIntent.putExtra("com.example.uk.ac.brookes.danielf.pgpmail.email", emailList.get(position));
					InboxActivity.this.startActivity(myIntent);
				}
				
			});
			
		}
		//if we're not connected to the internet then warn user
		else
		{
			Toast.makeText(getApplicationContext(), "Device is not connected to the internet", 
					Toast.LENGTH_SHORT).show();
			super.finish();
		}
	}
	
	@Override
	public void onMailReady(ArrayList<Email> emails) {
		emailList.addAll(emails);
		
		//if it's the first lot of emails we're loading
		if(first)
		{
			ma = new EmailAdapter();
			list.setAdapter(ma);
			first = false;
		}
		else
		{
			ma.notifyDataSetChanged();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.decode, menu);
		return true;
	}
	
	class EmailAdapter extends BaseAdapter 
	{
		LayoutInflater inflater;
		TextView from, subject, bodyPreview;
		
		EmailAdapter() 
		{
			inflater = (LayoutInflater) InboxActivity.this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);	
		}
		
		@Override
		public int getCount() {
			return emailList.size();
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
			View view = inflater.inflate(R.layout.email_folder_row, null);
				
			from = (TextView) view.findViewById(R.id.from_address);
			subject = (TextView) view.findViewById(R.id.subject);
			bodyPreview = (TextView) view.findViewById(R.id.body_preview);
				
			String fromtxt = emailList.get(position).getFrom()[0];
			if(fromtxt != null)
				from.setText(emailList.get(position).getFrom()[0]);
			else
				from.setText("none");
			
			subject.setText(emailList.get(position).getSubject());
			
			//get the body, if it's longer than 42 characters shorten it
			String bodyPrev = emailList.get(position).getMsgBody();
			if(bodyPrev.length() > 42)
				bodyPrev = bodyPrev.subSequence(0, 39).toString() + "...";
			
			bodyPreview.setText(bodyPrev);
				
			return view;
		}
		
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
