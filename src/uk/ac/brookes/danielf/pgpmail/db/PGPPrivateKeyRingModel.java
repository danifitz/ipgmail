package uk.ac.brookes.danielf.pgpmail.db;

import java.io.IOException;
import java.io.Serializable;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

/**
 * Model to represent private key in the DB. Serializable so we easily
 * pass this object between android activities.
 * @author danfitzgerald
 *
 */
public class PGPPrivateKeyRingModel extends PGPSecretKeyRing implements Serializable {

	/*
	 * TODO: implement Parcelable rather than Serializable
	 * and figure out how to fill out the two methods
	 */
	
	private static final long serialVersionUID = -4754018016657220126L;
	private long id;
	private byte[] privateKeyRingBlob;
	
	public PGPPrivateKeyRingModel(long id, byte[] privateKeyRingBlob) throws IOException, PGPException 
	{
		super(privateKeyRingBlob, new JcaKeyFingerprintCalculator());
		setId(id);
		setPrivateKeyRingBlob(privateKeyRingBlob);
	}

	public long getId() {
		return id;
	}

	public byte[] getPrivateKeyRingBlob() {
		return privateKeyRingBlob;
	}

	private void setId(long id) {
		this.id = id;
	}
	
	private void setPrivateKeyRingBlob(byte[] secretKeyRingBlob)
	{
		this.privateKeyRingBlob = secretKeyRingBlob;
	}
}
