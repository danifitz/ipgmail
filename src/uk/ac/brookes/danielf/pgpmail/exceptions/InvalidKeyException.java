package uk.ac.brookes.danielf.pgpmail.exceptions;

public class InvalidKeyException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5533388619614205969L;

	public InvalidKeyException() {
		super("Key is invalid and cannot be used for crypto!");
	}
	
	public InvalidKeyException(String msg)
	{
		super(msg);
	}

}
