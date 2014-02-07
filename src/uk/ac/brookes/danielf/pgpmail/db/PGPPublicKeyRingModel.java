package uk.ac.brookes.danielf.pgpmail.db;

import java.io.IOException;
import java.io.Serializable;

import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;


/**
 * Model to represent public key stored in DB. Serializable so
 * we can easily pass this object between android activities
 * @author danfitzgerald
 *
 */
public class PGPPublicKeyRingModel extends PGPPublicKeyRing implements Serializable {

	/*
	 * TODO: implement Parcelable rather than Serializable
	 * and figure out how to fill out the two methods
	 */
	
	private static final long serialVersionUID = -1733683017885841855L;
	private long id;
	private byte[] publicKeyRingBlob;
	
	public PGPPublicKeyRingModel(long id, byte[] publicKeyRingBlob) throws IOException 
	{
		super(publicKeyRingBlob, new JcaKeyFingerprintCalculator());
		setId(id);
		setPublicKeyRingBlob(publicKeyRingBlob);
	}

	public long getId() {
		return id;
	}

	public byte[] getPublicKeyRingBlob() {
		return publicKeyRingBlob;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setPublicKeyRingBlob(byte[] publicKeyRingBlob) {
		this.publicKeyRingBlob = publicKeyRingBlob;
	}
	
}
