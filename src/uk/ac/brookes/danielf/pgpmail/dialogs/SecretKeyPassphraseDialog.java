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
import android.widget.Toast;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

public class SecretKeyPassphraseDialog extends DialogFragment {

	public static final String TAG = "PASS_DIALOG";
	private EditText passTxt;
	private EditText confPassTxt;

	public String pass, confirmation;
	
	public static SecretKeyPassphraseDialog newInstance()
	{
		SecretKeyPassphraseDialog skpd = new SecretKeyPassphraseDialog();
		return skpd;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
	public SecretKeyPassphraseDialog() {}
    
    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
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
		View view = inflater.inflate(R.layout.secret_key_passphrase_dialog, null);
		
		//find views by Id
		passTxt = (EditText) view.findViewById(R.id.passtxt);
		confPassTxt = (EditText) view.findViewById(R.id.passconftxt);
		
		builder.setView(view)
		
		// Add buttons
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				//send the passes back to the host activity
				if(passTxt.getText().toString().equals(confPassTxt.getText().toString()))
				{
					PGDialogListener pListener = (PGDialogListener) getActivity();
					
					pass = passTxt.getText().toString();
					// Send the pass back to the activity
	                pListener.onDialogDone(getTag(), pass, 0);
	                
	                //dismiss the dialog
	                dismiss();
	                return;
				}
				else
				{
					Toast.makeText(getActivity().getApplicationContext(), 
							"Passphrases don't match, try again", Toast.LENGTH_SHORT).show();
					passTxt.setText("");
					confPassTxt.setText("");
				}
			}
		})
		.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				//User cancelled the dialog
                dismiss();
			}
		})
		
		//set the title of the dialog
		.setTitle("Enter passphrase")
		//set the message of the dialog
		.setMessage("Enter your passphrase to decrypt the secret key");
		
		return builder.create();
	}
}
