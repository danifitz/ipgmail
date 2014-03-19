package uk.ac.brookes.danielf.pgpmail.db;

import java.io.IOException;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;

/**
 * Model to represent private key in the DB.
 * @author danfitzgerald
 *
 */
public class PGPPrivateKeyRingModel extends PGPSecretKeyRing {

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
	
	private void setPrivateKeyRingBlob(byte[] privateKeyRingBlob)
	{
		this.privateKeyRingBlob = privateKeyRingBlob;
	}
}
