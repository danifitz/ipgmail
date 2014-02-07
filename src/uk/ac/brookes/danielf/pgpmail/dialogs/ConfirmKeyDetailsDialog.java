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

public class ConfirmKeyDetailsDialog extends DialogFragment {

	private int keyLength;
	private String expiry, name, email;
	
	private TextView keyLengthTxt;
	private TextView expiryTxt;
	private TextView nameTxt;
	private TextView emailTxt;
	
	
	public ConfirmKeyDetailsDialog(int keyLength, String expiry, String name, String email) {
		this.keyLength = keyLength;
		this.expiry = expiry;
		this.name = name;
		this.email = email;
	}
	
	/* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    
    // Use this instance of the interface to deliver action events
    NoticeDialogListener kListener;
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity)
    {
    	super.onAttach(activity);
    	// Verify that the host activity implements the callback interface

    	try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            kListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }

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
					// Send the positive button event back to the host activity
	                kListener.onDialogPositiveClick(ConfirmKeyDetailsDialog.this);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int id) {
					// Send the negative button event back to the host activity
	                kListener.onDialogNegativeClick(ConfirmKeyDetailsDialog.this);

					//User cancelled the dialog
	                ConfirmKeyDetailsDialog.this.getDialog().cancel();
				}
			});
		
		keyLengthTxt = (TextView) view.findViewById(R.id.keyleng);
		expiryTxt    = (TextView) view.findViewById(R.id.expir);
		nameTxt      = (TextView) view.findViewById(R.id.nameconf);
		emailTxt     = (TextView) view.findViewById(R.id.emailconf);
		
		keyLengthTxt.setText(String.valueOf(keyLength));
		expiryTxt.setText(expiry.toString());
		nameTxt.setText(name);
		emailTxt.setText(email);
		
		return builder.create();
	}

}
