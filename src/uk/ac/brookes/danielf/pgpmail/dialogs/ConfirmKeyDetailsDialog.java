package uk.ac.brookes.danielf.pgpmail.dialogs;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class ConfirmKeyDetailsDialog extends DialogFragment implements PGDialogListener {
	
	public static final String TAG = "CONFIRM_DIALOG";
	private TextView keyLengthTxt;
	private TextView expiryTxt;
	private TextView nameTxt;
	private TextView emailTxt;	
	
	public static ConfirmKeyDetailsDialog newInstance(
			int keyLength, 
			String expiry, 
			String name, 
			String email)
	{
		ConfirmKeyDetailsDialog ckdd = new ConfirmKeyDetailsDialog();
		Bundle bundle = new Bundle();
		bundle.putInt("keylength", keyLength);
		bundle.putString("expiry", expiry);
		bundle.putString("name", name);
		bundle.putString("email", email);
		ckdd.setArguments(bundle);
		return ckdd;
	}
	
	public ConfirmKeyDetailsDialog() {}
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity)
    {
    	super.onAttach(activity);
    	// Verify that the host activity implements the callback interface

    	try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            PGDialogListener kListener = (PGDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement PGDialogListener");
        }

    }
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
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
		View view = inflater.inflate(R.layout.key_info_dialog, null);
		builder.setView(view)
		
		// Add buttons
			.setPositiveButton("Generate", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int id) {
					PGDialogListener kListener = (PGDialogListener) getActivity();
					
					// Send the positive button event back to the host activity
	                kListener.onDialogDone(getTag(), null, 0);
	                
	                //dismiss the dialog
	                dismiss();
	                return;
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int id) {
					//User cancelled the dialog
	                dismiss();
	                return;
				}
			});
		
		keyLengthTxt = (TextView) view.findViewById(R.id.keyleng);
		expiryTxt    = (TextView) view.findViewById(R.id.expir);
		nameTxt      = (TextView) view.findViewById(R.id.nameconf);
		emailTxt     = (TextView) view.findViewById(R.id.emailconf);
		
		Bundle bundle = getArguments();
		keyLengthTxt.setText(String.valueOf(bundle.getInt("keylength")));
		expiryTxt.setText(bundle.getString("expiry"));
		nameTxt.setText(bundle.getString("name"));
		emailTxt.setText(bundle.getString("email"));
		
		return builder.create();
	}


	@Override
	public void onDialogDone(String tag, String pass, long keyId) {
		// TODO Auto-generated method stub
		
	}

}
