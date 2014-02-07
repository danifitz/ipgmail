package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.mail.MessagingException;

import uk.ac.brookes.danielf.pgpmail.email.Email;
import uk.ac.brookes.danielf.pgpmail.email.GetMail;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class DecodeActivity extends Activity implements OnItemClickListener {

	private List<Email> emailList = new ArrayList<Email>();
	private EmailAdapter ma;
	
	private ListView list;
	
	//we keep track of how many emails we've loaded from the inbox
	private int counter;
	//we keep track of how many emails are in the inbox
	private Integer emailCount;
	//number of emails to load at a time
	private final static int EMAILS_TO_LOAD = 24;
	
	//TODO: progress bar for loading emails!!!
	
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
		
		//set email count to 0
		counter = 0;
		
		//if we are connected to the internet then fetch emails
		if(isDeviceConnected())
		{
			try {
				
				//get the total num. of messages in the inbox
				emailCount = getNumberOfEmails();
				Log.d("Inbox activity", "there are " + emailCount + " emails in the inbox");
				
				//get a load of emails from the inbox
				getEmailsFromInbox(emailCount, (emailCount - EMAILS_TO_LOAD));
				
				//update the counter
				counter += EMAILS_TO_LOAD;
				
			} catch (MessagingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			list = (ListView) findViewById(R.id.listView);
			ma = new EmailAdapter();
			list.setAdapter(ma);
			list.setOnItemClickListener(this);
			
			//set the scroll listener, if the user scrolls to the bottom of the list
			//of emails we should load another lot.
			list.setOnScrollListener(new OnScrollListener() {

				@Override
				public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
					// TODO Auto-generated method stub
				}

				@Override
				public void onScrollStateChanged(AbsListView arg0, int arg1) {
					// TODO Auto-generated method stub
				}
			});
			
		}
		//if we're not connected to the internet then warn user
		else
		{
			Toast.makeText(getApplicationContext(), "Device is not connected to the internet", 
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.decode, menu);
		return true;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//toggle the checkbox
		ma.toggle(position);
		
		/*
		 * we want to start the display message activity so the user
		 * can look at the message. we need to pass the activity the email
		 * as an extra!
		 */
		Intent myIntent = new Intent(this, DisplayMessageActivity.class);
		myIntent.putExtra("email", emailList.get(position));
		DecodeActivity.this.startActivity(myIntent);
	}
	
	class EmailAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener 
	{
		private SparseBooleanArray checkStates;
		LayoutInflater inflater;
		TextView from, subject, bodyPreview;
		CheckBox checkbox;
		
		EmailAdapter() 
		{
			checkStates = new SparseBooleanArray(emailList.size());
			inflater = (LayoutInflater) DecodeActivity.this
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
			checkbox = (CheckBox) view.findViewById(R.id.checkBox_id);
				
			String fromtxt = emailList.get(position).getFrom()[0];
			if(fromtxt != null)
				from.setText(emailList.get(position).getFrom()[0]);
			else
				from.setText("none");
			
			subject.setText(emailList.get(position).getSubject());
			bodyPreview.setText(emailList.get(position).getMsgBody().subSequence(0, 26).toString());
			checkbox.setTag(position);
			checkbox.setChecked(checkStates.get(position, false));
			checkbox.setOnCheckedChangeListener(this);
				
			return view;
		}
		public boolean isChecked(int position)
		{
			return checkStates.get(position, false);
		}
		
		public void setChecked(int position, boolean isChecked)
		{
			checkStates.put(position, isChecked);
			notifyDataSetChanged();
		}
		
		public void toggle(int position) 
		{
			setChecked(position, !isChecked(position));
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			checkStates.put((Integer) buttonView.getTag(), isChecked);
		}
		
	}
	
	/**
	 * Routine to fetch emails from inbox and add them to the email list
	 * used to populate the listView in this activity
	 */
	public void getEmailsFromInbox(int start, int end) throws MessagingException, IOException 
	{
		Email emails[] = null;
		
		Pair<Integer, Pair<Integer, Integer>> params = 
				new Pair<Integer, Pair<Integer, Integer>>
		(GetMail.GET_MESSAGES, new Pair<Integer, Integer>(start, end));
		
		try {
			emails = (Email[]) new GetMail(this).execute(params).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		for(Email email : emails)
			emailList.add(email);
	}
	
	/**
	 * Gets the number of emails in the inbox
	 * @return
	 */
	public Integer getNumberOfEmails()
	{
		Pair<Integer, Pair<Integer, Integer>> params = 
				new Pair<Integer, Pair<Integer, Integer>>
		(GetMail.GET_MESSAGE_COUNT, new Pair<Integer, Integer>(0,0));
		
		int count = 0;
		try {
			count = (Integer) new GetMail(this).execute(params).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		
		return count;
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
