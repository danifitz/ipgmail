package uk.ac.brookes.danielf.pgpmail.dialogs;

import java.util.List;

import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class KeyPickerDialog extends DialogFragment implements OnItemClickListener {

	private ListView keyList;
	
	private List<PGPPublicKeyRingModel> pubKeyList;
	
	Context context;
	
	public static final String TAG = "KEY_PICKER_DIALOG";
	private static final String LOG_TAG = "KEY_PICKER";
	
	public KeyPickerDialog(Context context) {
		this.context = context;
		pubKeyList = PGP.getPublicKeys();
	}
	
	public static KeyPickerDialog newInstance(Context context)
	{
		KeyPickerDialog kpd = new KeyPickerDialog(context);
		return kpd;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public void onAttach(Activity activity)
	{
    	// Verify that the host activity implements the callback interface
    	
    	PGDialogListener pListener = (PGDialogListener) activity;
    	try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            pListener = (PGDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PGDialogListener");
        }
    	super.onAttach(activity);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				
		// get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		// Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
		View view = inflater.inflate(R.layout.key_picker_dialog, null);
		
		//find views by id
		keyList = (ListView) view.findViewById(R.id.keylist);
		
		//setup the key adapter
		KeyAdapter ka = new KeyAdapter();
		keyList.setAdapter(ka);
		//set the onitemclick listener
		keyList.setOnItemClickListener(this);
		
		//no buttons on this dialog, simply 
		builder.setView(view)
		
		.setTitle("Choose a key");
		
		return builder.create();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		//return the callback event through the listener
		PGDialogListener kpListener = (PGDialogListener) getActivity();
		kpListener.onDialogDone(TAG, null, pubKeyList.get(position).getPublicKey().getKeyID());
		
		//once a key is chosen dismiss the dialog
		dismiss();
	}
	
	class KeyAdapter extends BaseAdapter
	{
		LayoutInflater inflater;
		TextView keyIdentity, keyInfo;
		
		KeyAdapter()
		{
			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		@Override
		public int getCount() {
			return pubKeyList.size();
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
			
			//set the text with key info
			if(pubKeyList.size() > 0)
			{
				PGPPublicKeyRingModel pubKey = pubKeyList.get(position);
				
				String ident = PGP.getUserIdsFromPublicKey(pubKey.getPublicKey());
				String info = PGP.getAlgorithmAsString(pubKey.getPublicKey().getAlgorithm());
				if(ident.isEmpty())
					ident = "No user id available";
				
				keyIdentity.setText(ident);
				keyInfo.setText(info);
			}
			else
			{
				keyIdentity.setText("No public keys found");
			}
			
			return view;
		}	
	}
}