package uk.ac.brookes.danielf.pgpmail.db;

import java.io.IOException;

import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;


/**
 * Model to represent public key stored in DB. Serializable so
 * we can easily pass this object between android activities
 * @author danfitzgerald
 *
 */
public class PGPPublicKeyRingModel extends PGPPublicKeyRing {

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
