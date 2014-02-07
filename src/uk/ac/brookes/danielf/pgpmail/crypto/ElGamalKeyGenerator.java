package uk.ac.brookes.danielf.pgpmail.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.ElGamalKeyGenerationParameters;
import org.spongycastle.crypto.params.ElGamalKeyParameters;
import org.spongycastle.openpgp.PGPKeyRingGenerator;

import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingDataSource;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingDataSource;
import android.content.Context;

public class ElGamalKeyGenerator {

	private PGPPublicKeyRingDataSource  pubKeyRingDataSource;
	private PGPPrivateKeyRingDataSource privateKeyRingDataSource;
	private Context 				    context;
	
	public ElGamalKeyGenerator(Context context) {
		this.context = context;
	}
	
	public void generate(String id, char[] pass, int keySize)
	{
		
	}
	
	public final static PGPKeyRingGenerator generateKeyRingGenerator
	(String id, char pass[], int keySize) throws Exception
	{
		return generateKeyRingGenerator(id, pass, keySize, 0xc0);
	}
	
	public final static PGPKeyRingGenerator generateKeyRingGenerator(String id, char[] pass, int keySize, int s2kCount)
	{
		//This object generates individual key pairs
		RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();
		
		//Standard El-Gamal parameters
		//publicExponent should be a Fermat number, 0x10001 is known to be secure
		
		
		return null;
	}
}
