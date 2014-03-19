package uk.ac.brookes.danielf.pgpmail.activities;

import java.util.Date;
import java.util.List;

import org.spongycastle.openpgp.PGPPublicKey;

import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class ManageKeysActivity extends Activity implements OnItemClickListener {

	private Switch   keySwitch;
	private ListView keyList;
	
	/*
	 * This button will either start the create key activity
	 * or the import key activity depending on whether
	 * public or private is selected in the switch.
	 */
	private Button   button;
	
	private List<PGPPublicKeyRingModel>  pubKeyList;
	private List<PGPPrivateKeyRingModel> secKeyList;
	KeyAdapter ka;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manage_keys);
		
		//do the listing thang
		listKeys();
		
		//register a listener for checked change events
		keySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
			{
				if(isChecked)
				{
					//set the button text to 'import keys'
					button.setText("Import keys");
					
					//notify the adapter the dataset has changed (from private to public)
					ka.notifyDataSetInvalidated();
					
					//populate list with public keys
					Log.i("ManageKeys", "Listing public keys");
				}
				else
				{
					//set the button text to 'create keys'
					button.setText("Create keys");
					
					//notify the adapter the dataset has changed (from public to private)
					//TODO: don't we actually need to set the adapter to use the sec key list????
					ka.notifyDataSetInvalidated();
					
					
					//populate list with private keys
					Log.i("ManageKeys", "Listing private keys");
				}
			}
			
		});

		/*
		 * find the keylist and populate it with private keys,
		 * this is because the default state of the key switch
		 * above is set to private
		 */
		keyList = (ListView) findViewById(R.id.keysview);
		
		button.setText("Create keys");
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent keyIntent;
				
				//if we've got public selected let's import a key
				if(keySwitch.isChecked())
					keyIntent = new Intent(ManageKeysActivity.this, ImportKeysActivity.class);
				//or if private selected let's create some keys
				else
					keyIntent = new Intent(ManageKeysActivity.this, CreateKeyActivity.class);
	
				ManageKeysActivity.this.startActivity(keyIntent);
			}
		});
	}
	
	@Override
	public void onRestart()
	{
		super.onRestart();
		
		/*
		 * When we return to this activity after creating a key
		 * we must refresh the key list so the new key is displayed
		 */
		listKeys();
	}
	
	public void listKeys()
	{
		//get the public & private keys
		PGP.populateKeyRingCollections(this);
		pubKeyList = PGP.getPublicKeys();
		secKeyList = PGP.getPrivateKeys();
		Log.i("how many public keys?", String.valueOf(pubKeyList.size()));
		Log.i("how many private keys?", String.valueOf(secKeyList.size()));
		
		//find the views by Id
		keySwitch = (Switch) findViewById(R.id.keyswitch);
		button = (Button) findViewById(R.id.createkey);
		keyList = (ListView) findViewById(R.id.keysview);
		
		//set listeners/adapters for the list
		ka = new KeyAdapter();
		keyList.setAdapter(ka);
		keyList.setOnItemClickListener(this);
		keyList.setItemsCanFocus(true);
		
		//set it initially to OFF and disable the server textbox
		keySwitch.setChecked(false);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.manage_keys, menu);
		return true;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent myIntent = new Intent(ManageKeysActivity.this, KeyInfo.class);
		
		long keyid = 0;
		int algorithm = 0;
		int keylength = 0;
		Date created = null;
		int expiration = 0;
		byte[] trustdata = null;
		boolean isMasterKey = false;
		
		//we only display info about the key if it's a public key
		if(keySwitch.isChecked())
		{
			PGPPublicKeyRingModel keyring = pubKeyList.get(position);
			PGPPublicKey master = PGP.getMasterKeyFromKeyRing(keyring);
			keyid = master.getKeyID();
			algorithm = master.getAlgorithm();
			keylength = master.getBitStrength();
			created = master.getCreationTime();
			expiration = master.getValidDays();
			trustdata = master.getTrustData();
			isMasterKey = master.isMasterKey();

			//put the key info in extra's to be passed into the next activity
			myIntent.putExtra("keyid", keyid);
			myIntent.putExtra("algorithm", algorithm);
			myIntent.putExtra("keylength", keylength);
			myIntent.putExtra("created", created.toString());
			myIntent.putExtra("expiration", expiration);
			myIntent.putExtra("trustdata", trustdata);
			myIntent.putExtra("keytype", isMasterKey);

			//start the activity
			ManageKeysActivity.this.startActivity(myIntent);
		}
	}
	
	class KeyAdapter extends BaseAdapter
	{
		LayoutInflater inflater;
		TextView keyIdentity, keyInfo;
		
		KeyAdapter()
		{
			inflater = (LayoutInflater) ManageKeysActivity.this
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public int getCount() {
			if(keySwitch.isChecked())
				return pubKeyList.size();
			else
				return secKeyList.size();
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
			View view = inflater.inflate(R.layout.pgp_key_row, null);
			
			keyIdentity = (TextView) view.findViewById(R.id.keyidentity);
			keyInfo =     (TextView) view.findViewById(R.id.keyinfo);
			
			//set the text with public key info if checked
			if(keySwitch.isChecked())
			{
				if(pubKeyList.size() > 0)
				{
					PGPPublicKey masterKey = PGP.getMasterKeyFromKeyRing(pubKeyList.get(position));
					keyIdentity.setText(PGP.getUserIdsFromPublicKey(masterKey));
					keyInfo.setText(PGP.getAlgorithmAsString(masterKey.getAlgorithm()));
				}
				else
				{
					keyIdentity.setText("No public keys found");
				}
			}
			//set the text with secret key info if not checked
			else
			{
				if(secKeyList.size() > 0)
				{
					PGPPrivateKeyRingModel secKey = secKeyList.get(position);
					keyIdentity.setText(PGP.getUserIdsFromSecretKey(secKey.getSecretKey()));
					keyInfo.setText(PGP.getAlgorithmAsString(secKey.getSecretKey().getPublicKey().getAlgorithm()));
				}
				else
				{
					keyIdentity.setText("No private keys found");
				}
			}
			
			return view;
		}	
	}
}
