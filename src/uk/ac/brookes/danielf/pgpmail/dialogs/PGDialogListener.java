package uk.ac.brookes.danielf.pgpmail.dialogs;

/**
 * An interface that allows a dialog to report back to it's
 * activity upon completion
 * @author danfitzgerald
 *
 */
public interface PGDialogListener {

	public void onDialogDone(String tag, String pass, long keyId);

}
