package uk.ac.brookes.danielf.pgpmail.crypto;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;

import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.spongycastle.bcpg.sig.Features;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.crypto.generators.RSAKeyPairGenerator;
import org.spongycastle.crypto.params.RSAKeyGenerationParameters;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.bc.BcPGPKeyPair;

import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingDataSource;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingDataSource;
import android.content.Context;

public class RSAKeyGenerator {

	private PGPPublicKeyRingDataSource  pubKeyRingDataSource;
	private PGPPrivateKeyRingDataSource privateKeyRingDataSource;
	private Context 				    context;
	
	public RSAKeyGenerator(Context context)
	{
		this.context = context;
	}
	
	public void generate(String id, char pass[], int keySize) throws Exception 
	{
		
		Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
		
		PGPKeyRingGenerator krgen = generateKeyRingGenerator
				(id, pass, keySize);
		
		//generate public key ring, insert into DB
		PGPPublicKeyRing pkr = krgen.generatePublicKeyRing();
		pubKeyRingDataSource = new PGPPublicKeyRingDataSource(context);
		pubKeyRingDataSource.openWritable();
		pubKeyRingDataSource.createPGPPublicKeyRing(pkr.getEncoded());
		pubKeyRingDataSource.close();
		
		//generate private key ring, insert into DB
		PGPSecretKeyRing skr = krgen.generateSecretKeyRing();
		privateKeyRingDataSource = new PGPPrivateKeyRingDataSource(context);
		privateKeyRingDataSource.openWritable();
		privateKeyRingDataSource.createPGPPrivateKeyRing(skr.getEncoded());
		privateKeyRingDataSource.close();
	}
	
	public final static PGPKeyRingGenerator generateKeyRingGenerator
		(String id, char pass[], int keySize) throws Exception
	{
		return generateKeyRingGenerator(id, pass, keySize, 0xc0);
	}
	
	// Note: s2kcount is a number between 0 and 0xff that controls the
    // number of times to iterate the password hash before use. More
    // iterations are useful against offline attacks, as it takes more
    // time to check each password. The actual number of iterations is
    // rather complex, and also depends on the hash function in use.
    // Refer to Section 3.7.1.3 in rfc4880.txt. Bigger numbers give
    // you more iterations.  As a rough rule of thumb, when using
    // SHA256 as the hashing function, 0x10 gives you about 64
    // iterations, 0x20 about 128, 0x30 about 256 and so on till 0xf0,
    // or about 1 million iterations. The maximum you can go to is
    // 0xff, or about 2 million iterations.  I'll use 0xc0 as a
    // default -- about 130,000 iterations.
	
	public final static PGPKeyRingGenerator generateKeyRingGenerator
		(String id, char pass[], int keySize, int s2kcount) throws Exception
	{
		//This object generates individual key pairs
		RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();
		
		//Standard RSA parameters
		//publicExponent should be a Fermat number, 0x10001 is known to be secure
		kpg.init(new RSAKeyGenerationParameters
				(BigInteger.valueOf(0x10001), new SecureRandom(), keySize, 12));
		
		//First create the master (signing) key with the generator
		PGPKeyPair rsakp_sign = new BcPGPKeyPair
				(PGPPublicKey.RSA_SIGN, kpg.generateKeyPair(), new Date());
		
		//Then an encryption subkey
		PGPKeyPair rsakp_enc = new BcPGPKeyPair
				(PGPPublicKey.RSA_ENCRYPT, kpg.generateKeyPair(), new Date());
		
		//Add a self signature on the id
		PGPSignatureSubpacketGenerator signhashgen =
				new PGPSignatureSubpacketGenerator();
		
		/*
		 * Add signed metadata to the signature
		 * 1) Declare it's purpose
		 */
		signhashgen.setKeyFlags(false, KeyFlags.SIGN_DATA|KeyFlags.CERTIFY_OTHER);
		
		/*
		 * 2) Set preferences for secondary crypto algo's to use
		 * when sending messages to this key
		 */
		signhashgen.setPreferredSymmetricAlgorithms(false, new int[] {
				SymmetricKeyAlgorithmTags.TRIPLE_DES
		});
		signhashgen.setPreferredHashAlgorithms(false, new int[] {
				HashAlgorithmTags.SHA1,
				HashAlgorithmTags.SHA256
		});
		
		/*
		 * 3) Request senders add additional checksums to
		 * the message (useful when verifying unsigned messages)
		 */
		signhashgen.setFeature
			(false, Features.FEATURE_MODIFICATION_DETECTION);
		
		//Create a signature on the encryption subkey
		PGPSignatureSubpacketGenerator enchashgen =
				new PGPSignatureSubpacketGenerator();
		//Add metadata to declare its purpose
		enchashgen.setKeyFlags
			(false, KeyFlags.ENCRYPT_COMMS|KeyFlags.ENCRYPT_STORAGE);
		
		//Objects used to encrypt the secret key
		PGPDigestCalculator sha1Calc = 
				new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
		PGPDigestCalculator sha256Calc = 
				new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);
		
		PBESecretKeyEncryptor pske =
				(new BcPBESecretKeyEncryptorBuilder
						(PGPEncryptedData.AES_256, sha256Calc)).build(pass);
		
		//Finally, create the keyring itself. The constructor
        //takes parameters that allow it to generate the self
        //signature.
		PGPKeyRingGenerator keyRingGen =
				new PGPKeyRingGenerator
				(PGPSignature.POSITIVE_CERTIFICATION, rsakp_sign,
						id, sha1Calc, signhashgen.generate(), null,
						new BcPGPContentSignerBuilder(rsakp_sign.getPublicKey().getAlgorithm(),
								HashAlgorithmTags.SHA1), pske);
		
		//Add our encryption subkey, along with its signature
		keyRingGen.addSubKey(rsakp_enc, enchashgen.generate(), null);
		
		return keyRingGen;
	}
}
