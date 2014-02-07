package uk.ac.brookes.danielf.pgpmail.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPEncryptedDataGenerator;
import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

import android.util.Log;

public class Crypto {

	public Crypto() {
	}
	
	public static byte[] encrypt(
			byte[] clearText, 
			PGPPublicKey encryptionKey, 
			boolean integrityCheck, 
			boolean armor) throws IOException, PGPException
	{
		Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
		
		ByteArrayOutputStream encOut = new ByteArrayOutputStream();
		
		OutputStream out = encOut;
		if(armor)
		{
			out = new ArmoredOutputStream(out);
		}
		
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		
		PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
				PGPCompressedDataGenerator.ZIP);
		//open it with the final destination
		OutputStream cos = comData.open(bOut);
		
		PGPLiteralDataGenerator literalData = new PGPLiteralDataGenerator();
		
		//we want to generate compressed data so we pass
		//the literal data generator our compressed output stream
		OutputStream lOut = literalData.open(cos, //the compressed output stream
				PGPLiteralData.UTF8, //binary format
				PGPLiteralData.CONSOLE, //the filename
				clearText.length, //the length of the clear text
				new Date()); //last modification time i.e now
		
		//write the clear text to the output stream
		lOut.write(clearText);
		
		//close the output streams for literal data and compressed data
		literalData.close();
		comData.close();
		
		JcePGPDataEncryptorBuilder jceDEB = new JcePGPDataEncryptorBuilder(PGPEncryptedData.TRIPLE_DES)
		.setSecureRandom(new SecureRandom())
		.setProvider("SC")
		.setWithIntegrityPacket(integrityCheck);
		
		PGPEncryptedDataGenerator edg = new PGPEncryptedDataGenerator(jceDEB);
		edg.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encryptionKey));
		
		byte[] bytes = bOut.toByteArray();
		
		OutputStream cOut = edg.open(out, bytes.length);
		cOut.write(bytes); //write the actual bytes from the compressed stream
		cOut.close();
		
		out.close();
		
		return encOut.toByteArray();
	}
	
	public static byte[] decrypt(byte[] encryptedText, 
			PGPSecretKeyRingCollection keyRing, 
			char[] password) throws IOException, PGPException, NoSuchProviderException
	{
		Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
		
		InputStream in = new ByteArrayInputStream(encryptedText);
		in.close(); //remove this if it stops working
		in = PGPUtil.getDecoderStream(in);
		
		PGPObjectFactory objFact = new PGPObjectFactory(in);
		PGPEncryptedDataList enc = null;
		Object o = objFact.nextObject();
		
		//the first object might be a pgp marker packet
		if(o instanceof PGPEncryptedDataList)
		{
			enc = (PGPEncryptedDataList) o;
		}
		else
		{
			enc = (PGPEncryptedDataList) objFact.nextObject();
		}
		
		//find the secret key
		Iterator<PGPPublicKeyEncryptedData> itr = enc.getEncryptedDataObjects();
		PGPPrivateKey sKey = null;
		PGPPublicKeyEncryptedData pked = null;
		
		while(sKey == null && itr.hasNext())
		{
			pked = (PGPPublicKeyEncryptedData) itr.next();
			
			sKey = findSecretKey(keyRing, pked.getKeyID(), password);
		}
		
		if(sKey == null)
		{
			throw new IllegalArgumentException("no secret key found in keyring capable of decrypting");
		}
		
		//get the clear text data stream
		InputStream clear = pked.getDataStream(
				new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("SC").build(sKey));
		
		//assemble the pgp objects from the input stream
		PGPObjectFactory pgpObjFact = new PGPObjectFactory(clear);
		
		//get the compressed data object
		PGPCompressedData compressedData = (PGPCompressedData) pgpObjFact.nextObject();
		
		//create new pgp objects from the newly uncompressed input stream
		pgpObjFact = new PGPObjectFactory(compressedData.getDataStream());
		
		//obtain the pgp literal data from the object factory
		PGPLiteralData literalData = (PGPLiteralData) pgpObjFact.nextObject();
		
		InputStream uncompressed = literalData.getInputStream();
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int ch;
		
		while((ch = uncompressed.read()) >= 0)
		{
			out.write(ch);
		}
		
		byte[] decryptedBytes = out.toByteArray();
		out.close();
		return decryptedBytes;
	}
	
	private static PGPPrivateKey findSecretKey(
            PGPSecretKeyRingCollection pgpSec, 
            long keyID, 
            char[] pass)
            throws PGPException, NoSuchProviderException {
        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

        if (pgpSecKey == null) {
            Log.d("Decrypt - findSecretKey()", "no secret key found in keyring matching id: " + String.valueOf(keyID));
        	return null;
        }

        BcPGPDigestCalculatorProvider sha256Calc = 
				new BcPGPDigestCalculatorProvider();
        PBESecretKeyDecryptor pskd = 
        		(new BcPBESecretKeyDecryptorBuilder
        				(sha256Calc)).build(pass);
        
        return pgpSecKey.extractPrivateKey(pskd);
    }

}
