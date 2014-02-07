package uk.ac.brookes.danielf.pgpmail.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class SecretKeyPassphraseDialog extends DialogFragment {

	private EditText passTxt;
	private EditText confPassTxt;

	private String pass, confirmation;
	
	public SecretKeyPassphraseDialog() {
		
	}
	
	/* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface NoticeDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
        public void getPass(String pass);
        public void getConfPass(String confPass);
    }
    
    // Use this instance of the interface to deliver action events
    NoticeDialogListener pListener;
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity)
    {
    	super.onAttach(activity);
    	// Verify that the host activity implements the callback interface

    	try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            pListener = (NoticeDialogListener) activity;
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
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					//send the passes back to the host activity
	                pListener.getPass(pass = passTxt.getText().toString());
	                pListener.getConfPass(confPassTxt.getText().toString());
					
					// Send the positive button event back to the host activity
	                pListener.onDialogPositiveClick(SecretKeyPassphraseDialog.this);
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					// Send the negative button event back to the host activity
	                pListener.onDialogNegativeClick(SecretKeyPassphraseDialog.this);

					//User cancelled the dialog
	                SecretKeyPassphraseDialog.this.getDialog().cancel();
				}
			});
		
		//find views by Id
		passTxt = (EditText) view.findViewById(R.id.passtxt);
		confPassTxt = (EditText) view.findViewById(R.id.passconftxt);
		
		return builder.create();
	}
	
	public String getPass() 
	{
		return pass;
	}

	public String getConfirmation() 
	{
		return confirmation;
	}

}
