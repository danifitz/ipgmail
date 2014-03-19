package uk.ac.brookes.danielf.pgpmail.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.spongycastle.util.io.Streams;

import uk.ac.brookes.danielf.pgpmail.exceptions.InvalidKeyException;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;

public class Crypto {
	
	public Crypto() {
	}

	public static byte[] encryptString(byte[] clearText,
			PGPPublicKey encryptionKey, boolean integrityCheck, boolean armor)
			throws IOException, PGPException, InvalidKeyException {
		if (!PGP.isKeyValid(encryptionKey))
			throw new InvalidKeyException();

		Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

		ByteArrayOutputStream encOut = new ByteArrayOutputStream();

		OutputStream out = encOut;
		if (armor) {
			out = new ArmoredOutputStream(out);
		}

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();

		PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
				PGPCompressedDataGenerator.ZIP);
		// open it with the final destination
		OutputStream cos = comData.open(bOut);

		PGPLiteralDataGenerator literalData = new PGPLiteralDataGenerator();

		// we want to generate compressed data so we pass
		// the literal data generator our compressed output stream
		OutputStream lOut = literalData.open(cos, // the compressed output
													// stream
				PGPLiteralData.UTF8, // binary format
				PGPLiteralData.CONSOLE, // the filename
				clearText.length, // the length of the clear text
				new Date()); // last modification time i.e now

		// write the clear text to the output stream
		lOut.write(clearText);

		// close the output streams for literal data and compressed data
		literalData.close();
		comData.close();

		JcePGPDataEncryptorBuilder jceDEB = new JcePGPDataEncryptorBuilder(
				PGPEncryptedData.TRIPLE_DES)
				.setSecureRandom(new SecureRandom()).setProvider("SC")
				.setWithIntegrityPacket(integrityCheck);

		PGPEncryptedDataGenerator edg = new PGPEncryptedDataGenerator(jceDEB);
		edg.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(
				encryptionKey));

		byte[] bytes = bOut.toByteArray();

		OutputStream cOut = edg.open(out, bytes.length);
		cOut.write(bytes); // write the actual bytes from the compressed stream
		cOut.close();

		out.close();

		return encOut.toByteArray();
	}

	public static byte[] decryptString(byte[] encryptedText,
			PGPSecretKeyRingCollection keyRing, char[] password)
			throws IOException, PGPException, NoSuchProviderException {
		
		Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);

		InputStream in = new ByteArrayInputStream(encryptedText);
		in.close(); // remove this if it stops working
		in = PGPUtil.getDecoderStream(in);

		PGPObjectFactory objFact = new PGPObjectFactory(in);
		PGPEncryptedDataList enc = null;
		Object o = objFact.nextObject();

		// the first object might be a pgp marker packet
		if (o instanceof PGPEncryptedDataList) {
			enc = (PGPEncryptedDataList) o;
		} else {
			enc = (PGPEncryptedDataList) objFact.nextObject();
		}

		// find the secret key
		Iterator<PGPPublicKeyEncryptedData> itr = enc.getEncryptedDataObjects();
		PGPPrivateKey sKey = null;
		PGPPublicKeyEncryptedData pked = null;

		while (sKey == null && itr.hasNext()) {
			pked = (PGPPublicKeyEncryptedData) itr.next();

			sKey = PGP.findSecretKey(keyRing, pked.getKeyID(), password);
		}

		if (sKey == null) {
			throw new IllegalArgumentException(
					"no secret key found in keyring capable of decrypting");
		}

		// get the clear text data stream
		InputStream clear = pked
				.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder()
						.setProvider("SC").build(sKey));

		// assemble the pgp objects from the input stream
		PGPObjectFactory pgpObjFact = new PGPObjectFactory(clear);

		// get the compressed data object
		PGPCompressedData compressedData = (PGPCompressedData) pgpObjFact
				.nextObject();

		// create new pgp objects from the newly uncompressed input stream
		pgpObjFact = new PGPObjectFactory(compressedData.getDataStream());

		// obtain the pgp literal data from the object factory
		PGPLiteralData literalData = (PGPLiteralData) pgpObjFact.nextObject();

		InputStream uncompressed = literalData.getInputStream();

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int ch;

		while ((ch = uncompressed.read()) >= 0) {
			out.write(ch);
		}

		byte[] decryptedBytes = out.toByteArray();
		out.close();
		return decryptedBytes;
	}
	
	/**
	 * Code taken from org.bouncycastle.openpgp.examples.KeyBasedFileProcessor
	 * @throws InvalidKeyException 
	 */
    public static void encryptFile(
            String          outputFileName,
            String          inputFileName,
            PGPPublicKey    pubKey,
            boolean         armor,
            boolean         withIntegrityCheck)
            throws IOException, NoSuchProviderException, PGPException, InvalidKeyException
        {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFileName));
            encryptFile(out, inputFileName, pubKey, armor, withIntegrityCheck);
            out.close();
        }
    
    /**
	 * Code taken from org.bouncycastle.openpgp.examples.KeyBasedFileProcessor
     * @throws InvalidKeyException 
	 */
	private static void encryptFile(OutputStream out, String fileName,
			PGPPublicKey encKey, boolean armor, boolean withIntegrityCheck)
			throws IOException, NoSuchProviderException, InvalidKeyException {
		
		Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
		
		if (!PGP.isKeyValid(encKey))
			throw new InvalidKeyException();
		
		if (armor) {
			out = new ArmoredOutputStream(out);
		}

		try {
			byte[] bytes = compressFile(fileName, CompressionAlgorithmTags.ZIP);

			PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(
					new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
							.setWithIntegrityPacket(withIntegrityCheck)
							.setSecureRandom(new SecureRandom())
							.setProvider("SC"));

			encGen.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(
					encKey).setProvider("SC"));

			OutputStream cOut = encGen.open(out, bytes.length);

			cOut.write(bytes);
			cOut.close();

			if (armor) {
				out.close();
			}
		} catch (PGPException e) {
			System.err.println(e);
			if (e.getUnderlyingException() != null) {
				e.getUnderlyingException().printStackTrace();
			}
		}
	}
	
	/**
	 * Code taken from org.bouncycastle.openpgp.examples.KeyBasedFileProcessor
	 * @throws PGPException 
	 */
	public static void decryptFile(
	        String inputFileName,
	        char[] passwd,
	        File   outputFile)
	        throws IOException, NoSuchProviderException, PGPException
	    {
	        InputStream in = new BufferedInputStream(new FileInputStream(inputFileName));
	        PGPSecretKeyRingCollection keyRing = PGP.getPrivateKeyRingCollection();
	        decryptFile(in, keyRing, passwd, outputFile);
	        in.close();
	    }
	
    /**
     * decrypt the passed in message stream
     * 
     * Code taken from org.bouncycastle.openpgp.examples.KeyBasedFileProcessor
     */
    private static void decryptFile(
        InputStream in,
        PGPSecretKeyRingCollection keyRing,
        char[]      passwd,
        File        outputFile)
        throws IOException, NoSuchProviderException, PGPException
    {
    	Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    	
    	in = PGPUtil.getDecoderStream(in);
        
        try
        {
            PGPObjectFactory pgpF = new PGPObjectFactory(in);
            PGPEncryptedDataList    enc;

            Object                  o = pgpF.nextObject();
            //
            // the first object might be a PGP marker packet.
            //
            if (o instanceof PGPEncryptedDataList)
            {
                enc = (PGPEncryptedDataList)o;
            }
            else
            {
                enc = (PGPEncryptedDataList)pgpF.nextObject();
            }
            
            //
            // find the secret key
            //
            Iterator<PGPPublicKeyEncryptedData> itr = enc.getEncryptedDataObjects();
            PGPPrivateKey sKey = null;
    		PGPPublicKeyEncryptedData pked = null;

    		while (sKey == null && itr.hasNext()) {
    			pked = (PGPPublicKeyEncryptedData) itr.next();

    			sKey = PGP.findSecretKey(keyRing, pked.getKeyID(), passwd);
    		}

    		if (sKey == null) {
    			throw new IllegalArgumentException(
    					"no secret key found in keyring capable of decrypting");
    		}
    
            InputStream         clear = pked.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("SC").build(sKey));
            
            PGPObjectFactory    plainFact = new PGPObjectFactory(clear);
            
            Object              message = plainFact.nextObject();
    
            if (message instanceof PGPCompressedData)
            {
                PGPCompressedData   cData = (PGPCompressedData)message;
                PGPObjectFactory    pgpFact = new PGPObjectFactory(cData.getDataStream());
                
                message = pgpFact.nextObject();
            }
            
            if (message instanceof PGPLiteralData)
            {
                PGPLiteralData ld = (PGPLiteralData)message;

                InputStream unc = ld.getInputStream();
                OutputStream fOut = new BufferedOutputStream(new FileOutputStream(outputFile));

                Streams.pipeAll(unc, fOut);

                fOut.close();
            }
            else if (message instanceof PGPOnePassSignatureList)
            {
                throw new PGPException("encrypted message contains a signed message - not literal data.");
            }
            else
            {
                throw new PGPException("message is not a simple encrypted file - type unknown.");
            }

            if (pked.isIntegrityProtected())
            {
                if (!pked.verify())
                {
                    System.err.println("message failed integrity check");
                }
                else
                {
                    System.err.println("message integrity check passed");
                }
            }
            else
            {
                System.err.println("no message integrity check");
            }
        }
        catch (PGPException e)
        {
            System.err.println(e);
            if (e.getUnderlyingException() != null)
            {
                e.getUnderlyingException().printStackTrace();
            }
        }
    }
    
    /**
	 * Code taken from org.bouncycastle.openpgp.examples.PGPExampleUtil
	 */
	private static byte[] compressFile(String fileName, int algorithm)
			throws IOException {
		
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
				algorithm);
		PGPUtil.writeFileToLiteralData(comData.open(bOut),
				PGPLiteralData.BINARY, new File(fileName));
		comData.close();
		return bOut.toByteArray();
	}
}
