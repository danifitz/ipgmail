package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingModel;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

/**
 * This activity should display information about a specific key.
 * It will be displayed if a user selects a key from the manage
 * keys activity.
 * @author danfitzgerald
 *
 */
public class KeyInfo extends Activity {

	private TextView keyIdTxt;
	private TextView algorithmTxt;
	private TextView keyLengthTxt;
	private TextView createdTxt;
	private TextView expirationTxt;
	private TextView trustDataTxt;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_key_info);
		
		//find the views by Id
		keyIdTxt =      (TextView) findViewById(R.id.keyid);
		algorithmTxt =  (TextView) findViewById(R.id.algorithm);
		keyLengthTxt =  (TextView) findViewById(R.id.keylength);
		createdTxt =    (TextView) findViewById(R.id.created);
		expirationTxt = (TextView) findViewById(R.id.expiry);
		trustDataTxt =  (TextView) findViewById(R.id.trustlevel);
		
		Intent myIntent = getIntent();
		Object keyObj = myIntent.getSerializableExtra("key");
		PGPPublicKeyRingModel pubKey;
		PGPPrivateKeyRingModel privKey;
		
		long keyId = 0;
		int algorithm = 0;
		int keyLength = 0;
		Date created = null;
		String expiration = "";
		String trustData = "";
		
		if(keyObj instanceof PGPPublicKeyRingModel)
		{
			pubKey = (PGPPublicKeyRingModel) keyObj;
			keyId = pubKey.getId();
			algorithm = pubKey.getPublicKey().getAlgorithm();
			keyLength = pubKey.getPublicKey().getBitStrength();
			created = pubKey.getPublicKey().getCreationTime();
			expiration = createExpirationDate(pubKey.getPublicKey().getValidDays());
			try {
				trustData = new String(pubKey.getPublicKey().getTrustData(), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		else
		{
			privKey = (PGPPrivateKeyRingModel) keyObj;
			keyId = privKey.getId();
			algorithm = privKey.getPublicKey().getAlgorithm();
			keyLength = privKey.getPublicKey().getBitStrength();
			created = privKey.getPublicKey().getCreationTime();
			expiration = createExpirationDate(privKey.getPublicKey().getValidDays());
			//trust levels are not applicable to secret/private keys
			trustData = "N/A";
		}
		
		keyIdTxt		.setText(String.valueOf(keyId));
		algorithmTxt	.setText(algorithm);
		keyLengthTxt	.setText(keyLength);
		createdTxt		.setText(created.toString());
		expirationTxt	.setText(expiration);
		trustDataTxt    .setText(trustData);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.key_info, menu);
		return true;
		
	}
	
	public String createExpirationDate(int validDays)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		c.add(Calendar.DATE, validDays);
		String out = sdf.format(c.getTime());
		return out;
	}

}
