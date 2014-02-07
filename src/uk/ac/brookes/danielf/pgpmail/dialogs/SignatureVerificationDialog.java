package uk.ac.brookes.danielf.pgpmail.dialogs;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class SignatureVerificationDialog extends DialogFragment {

	private TextView sig;
	
	private String sigInfo;
	
	public SignatureVerificationDialog(String sigInfo) {
		this.sigInfo = sigInfo;
	}
	
	@Override
	public void onAttach(Activity activity)
	{
		
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Verification status");
        
        // get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		// Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
		View view = inflater.inflate(R.layout.sig_verification, null);
		builder.setView(view)
		
        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				//User dismissed the dialog
				SignatureVerificationDialog.this.getDialog().dismiss();
			}
		});
		
		//set the text view
		sig = (TextView) view.findViewById(R.id.sig);
		sig.setText(sigInfo);
		
        //create the alert dialog and return it
        return builder.create();
	}
}
