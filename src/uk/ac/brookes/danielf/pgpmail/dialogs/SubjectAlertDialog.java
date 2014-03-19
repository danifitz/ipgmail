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

public class SubjectAlertDialog extends DialogFragment implements PGDialogListener {

	public static final String TAG = "SUBJ_DIALOG";
	private TextView warning;
	
	public static SubjectAlertDialog newInstance()
	{
		SubjectAlertDialog sad = new SubjectAlertDialog();
		return sad;
	}
	
	public SubjectAlertDialog() {}
	
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
	public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		// Use the Builder class for convenient dialog construction
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				
		// get the layout inflater
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		// Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
		View view = inflater.inflate(R.layout.subject_alert_dialog, null);
		builder.setView(view)
		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
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
			public void onClick(DialogInterface arg0, int arg1) {
				dismiss();
				return;
			}
		});
		
		warning = (TextView) view.findViewById(R.id.warning);
		
		return builder.create();
	}
	
	@Override
	public void onDialogDone(String tag, String pass, long keyId) {
		
	}

}
