package uk.ac.brookes.danielf.pgpmail.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.spongycastle.bcpg.ArmoredInputStream;
import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.BCPGOutputStream;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.openpgp.PGPCompressedData;
import org.spongycastle.openpgp.PGPCompressedDataGenerator;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPLiteralData;
import org.spongycastle.openpgp.PGPLiteralDataGenerator;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPOnePassSignatureList;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.PGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.exceptions.InvalidKeyException;
import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import android.content.Context;
import android.util.Log;
import android.util.Pair;

/**
 * Class that handles signing and verifying.
 * 
 * Code based on org.bouncycastle.openpgp.examples.ClearSignedFileProcessor
 * 
 * @author danfitzgerald
 */
public class Sign {

	public final static String LOG_TAG = "SIGN";
	
	public Sign() {}
    
    /*
     * verify a clear text signed file
     */
    public static Pair<String, Boolean> verifyClearText(
        String msg,
        PGPPublicKeyRingCollection krc) throws IOException, PGPException, InvalidKeyException, SignatureException
    {
    	Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    	
    	InputStream in = new ByteArrayInputStream(msg.getBytes());
    	ArmoredInputStream    aIn = new ArmoredInputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        /*
         * write out signed section using the local line separator.
         * note: trailing white space needs to be removed from the end of
         * each line RFC 4880 Section 7.1
         */
        ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
        int                   lookAhead = readInputLine(lineOut, aIn);
        byte[]                lineSep = getLineSeparator();
        
        //we're not entering this block because isClearText is false...but why?
        //it may be because there is no newline after 'Hash: SHA1' and 'Version: BCPG v@RELEASE_NAME@'
        //this was discovered by running gpg --verify on the output of signFile()
        if (lookAhead != -1 && aIn.isClearText())
        {
            byte[] line = lineOut.toByteArray();
            out.write(line, 0, getLengthWithoutSeparatorOrTrailingWhitespace(line));
            out.write(lineSep);

            while (lookAhead != -1 && aIn.isClearText())
            {
            	lookAhead = readInputLine(lineOut, lookAhead, aIn);
                
                line = lineOut.toByteArray();
                out.write(line, 0, getLengthWithoutSeparatorOrTrailingWhitespace(line));
                out.write(lineSep);
            }
        }

        out.close();
        
        //obtain the signature list from the armored input
        PGPObjectFactory           pgpFact = new PGPObjectFactory(aIn);
        PGPSignatureList           p3 = (PGPSignatureList) pgpFact.nextObject();
        PGPSignature               sig = p3.get(0);

        PGPPublicKey publicKey = krc.getPublicKey(sig.getKeyID());
        if(publicKey == null)
        {
        	throw new IllegalArgumentException("No public key found capable of verifying signature");
        }
        
        if(!PGP.isKeyValid(publicKey))
			throw new InvalidKeyException();
        
        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("SC"), publicKey);

        /*
         * read the input, making sure we ignore the last newline.
         */
        InputStream sigIn = new ByteArrayInputStream(out.toByteArray());

        lookAhead = readInputLine(lineOut, sigIn);

        processLine(sig, lineOut.toByteArray());

        if (lookAhead != -1)
        {
            do
            {
                lookAhead = readInputLine(lineOut, lookAhead, sigIn);

                sig.update((byte)'\r');
                sig.update((byte)'\n');

                processLine(sig, lineOut.toByteArray());
            }
            while (lookAhead != -1);
        }

        sigIn.close();
        
        boolean verified = sig.verify();
        String result = constructSignatureVerification(verified, publicKey, sig.getCreationTime(), new Date());
       	
        return new Pair<String, Boolean>(result, verified);
    }
    
    private static String constructSignatureVerification(boolean verified, PGPPublicKey key, Date creationDate, Date verifyDate)
    {
    	//work out the signature status
    	String sigStatus;
    	if(verified)
    		sigStatus = "good";
    	else
    		sigStatus = "bad";
    	
    	//format the creation date
    	SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss");
    	String cDate = sdf.format(creationDate);
    	String vDate = sdf.format(verifyDate);
    	
    	//get the user ids from the public key
    	String ids = PGP.getUserIdsFromPublicKey(key);
    	
    	//begin creating the signature verification block
    	String result = PGP.PGP_SIGNATURE_STATUS + sigStatus + "\n" +
    		PGP.PGP_SIGNATURE_SIGNER + ids + "\n" +
    		PGP.PGP_SIGNATURE_SIGNED_DATE + cDate + "\n" +
    		PGP.PGP_SIGNATURE_VERIFIED_DATE + vDate;
    		
    	return result;
    }
    
    /*
     * create a clear text signed file.
     */
    public static byte[] signClearText(
        String          		msg,
        PGPSecretKey    		keyIn,
        char[]          		pass,
        String          		digestName)
        throws IOException, NoSuchAlgorithmException, NoSuchProviderException, PGPException, SignatureException
    {    
    	Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    	
    	int digest;
        
        if (digestName.equals("SHA256"))
        {
            digest = PGPUtil.SHA256;
        }
        else if (digestName.equals("SHA384"))
        {
            digest = PGPUtil.SHA384;
        }
        else if (digestName.equals("SHA512"))
        {
            digest = PGPUtil.SHA512;
        }
        else if (digestName.equals("MD5"))
        {
            digest = PGPUtil.MD5;
        }
        else if (digestName.equals("RIPEMD160"))
        {
            digest = PGPUtil.RIPEMD160;
        }
        else
        {
            digest = PGPUtil.SHA1;
        }
        
        PGPPrivateKey                   pgpPrivKey = 
        		keyIn.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
        		.setProvider("SC").build(pass));
        
        //TODO: what if passphrase is wrong, we need to tell user
        //and let them have another go!
        
        PGPSignatureGenerator           sGen = new PGPSignatureGenerator(
        		new JcaPGPContentSignerBuilder(keyIn.getPublicKey().getAlgorithm(), 
        				digest).setProvider("SC"));
        
        PGPSignatureSubpacketGenerator  spGen = new PGPSignatureSubpacketGenerator();
        
        sGen.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, pgpPrivKey);
        
        Iterator<String> it = keyIn.getPublicKey().getUserIDs();
        if (it.hasNext())
        {
            spGen.setSignerUserID(false, (String)it.next());
            sGen.setHashedSubpackets(spGen.generate());
        }
        
        InputStream fIn = new ByteArrayInputStream(msg.getBytes());
        ByteArrayOutputStream sigOut = new ByteArrayOutputStream();
        ArmoredOutputStream aOut = new ArmoredOutputStream(sigOut);
        
        aOut.beginClearText(digest);

        // note the last \n/\r/\r\n in the message is ignored
        ByteArrayOutputStream lineOut = new ByteArrayOutputStream();
        int lookAhead = readInputLine(lineOut, fIn);

        processLine(aOut, sGen, lineOut.toByteArray());

        if (lookAhead != -1)
        {
            do
            {
                lookAhead = readInputLine(lineOut, lookAhead, fIn);

                sGen.update((byte)'\r');
                sGen.update((byte)'\n');

                processLine(aOut, sGen, lineOut.toByteArray());
            }
            while (lookAhead != -1);
        }

        fIn.close();

        aOut.endClearText();
        
        BCPGOutputStream            bOut = new BCPGOutputStream(aOut);
        
        sGen.generate().encode(bOut);

        aOut.close();
        
        return sigOut.toByteArray();
    }
    
    /**
     * Certify the key against another key - is this enough to verify identity or do
     * we need to sign against a user id?
     * 
     * @param context
     * @param keyIdToSign
     * @param keyIdToSignWith
     * @throws SignatureException
     * @throws PGPException
     */
    public static void signKey(Context context, long keyIdToSign, long keyIdToSignWith, char[] pass) throws SignatureException, PGPException
    {
    	Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    	
    	Log.i(LOG_TAG + " signKey()", "entry");
    	
    	//get the key to sign and the key to sign with
    	PGPPublicKey keyToSign = PGP.getKeyById(keyIdToSign);
    	PGPPublicKey keyToSignWith = PGP.getKeyById(keyIdToSignWith);
    	
    	//get the keyring that the key belongs to
    	PGPPublicKeyRingModel keyring = PGP.getKeyRingByKey(keyToSign);
    	
    	//remove the public key we are about to sign (we'll put the newly signed key back later)
    	PGPPublicKeyRingModel.removePublicKey(keyring, keyToSign);
    	
    	//create a content signer builder
    	PGPContentSignerBuilder pcsb = new JcaPGPContentSignerBuilder(keyToSign.getAlgorithm(), HashAlgorithmTags.SHA1);
    	
    	//create a signature generator passing in the content signer builder
    	PGPSignatureGenerator siggen = new PGPSignatureGenerator(pcsb);
    	
    	//get the secret key
    	PGPSecretKey secKey = PGP.getCorrespondingSecretKey(keyToSign);
    	
    	//decrypt the secret key
    	BcPGPDigestCalculatorProvider sha256Calc = 
				new BcPGPDigestCalculatorProvider();
        PBESecretKeyDecryptor pskd = 
        		(new BcPBESecretKeyDecryptorBuilder
        				(sha256Calc)).build(pass);
    	
    	//initialize
    	siggen.init(PGPSignature.POSITIVE_CERTIFICATION, secKey.extractPrivateKey(pskd));
    	
    	//generate the certificate
    	PGPSignature certification = siggen.generateCertification(keyToSignWith, keyToSign);
    	
    	//add the certificate to the public key
    	PGPPublicKey.addCertification(keyToSign, certification);
    	
    	/*
    	 * remember we need to update the key in the db
    	 * 
    	 * 1) First we delete the keyring from the db
    	 * 2) Then we add the newly signed key to the keyring
    	 * 3) Then we insert the updated keyring into the db
    	 */
    	PGP.modifyPubKeyRingCollection(context, PGP.PUBLIC_KEY_COLLECTION_DELETE, keyring);
    	PGPPublicKeyRingModel.insertPublicKey(keyring, keyToSign);
    	PGP.modifyPubKeyRingCollection(context, PGP.PUBLIC_KEY_COLLECTION_INSERT, keyring);
    }
    
    public static void signFile(
            String          fileName,
            PGPSecretKey    pgpSec,
            OutputStream    out,
            char[]          pass,
            boolean         armor)
            throws IOException, NoSuchAlgorithmException, NoSuchProviderException, PGPException, SignatureException
        {
            
    	Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);	
    	
    	if (armor)
            {
                out = new ArmoredOutputStream(out);
            }

            PGPPrivateKey               pgpPrivKey = pgpSec.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("SC").build(pass));
            PGPSignatureGenerator       sGen = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(pgpSec.getPublicKey().getAlgorithm(), PGPUtil.SHA1).setProvider("SC"));
            
            sGen.init(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);
            
            Iterator<String>    it = pgpSec.getPublicKey().getUserIDs();
            if (it.hasNext())
            {
                PGPSignatureSubpacketGenerator  spGen = new PGPSignatureSubpacketGenerator();
                
                spGen.setSignerUserID(false, (String)it.next());
                sGen.setHashedSubpackets(spGen.generate());
            }
            
            PGPCompressedDataGenerator  cGen = new PGPCompressedDataGenerator(
                                                                    PGPCompressedData.ZIP);
            
            BCPGOutputStream            bOut = new BCPGOutputStream(cGen.open(out));
            
            sGen.generateOnePassVersion(false).encode(bOut);
            
            File                        file = new File(fileName);
            PGPLiteralDataGenerator     lGen = new PGPLiteralDataGenerator();
            OutputStream                lOut = lGen.open(bOut, PGPLiteralData.BINARY, file);
            FileInputStream             fIn = new FileInputStream(file);
            int                         ch;
            
            while ((ch = fIn.read()) >= 0)
            {
                lOut.write(ch);
                sGen.update((byte)ch);
            }

            lGen.close();

            sGen.generate().encode(bOut);

            cGen.close();

            if (armor)
            {
                out.close();
            }
        }
    
    public static Pair<String, Boolean> verifyFile(
            InputStream        in,
            File			   outputFile) throws IOException, PGPException, SignatureException
    {
    	Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    	
    	in = PGPUtil.getDecoderStream(in);
        
        PGPObjectFactory            pgpFact = new PGPObjectFactory(in);

        PGPCompressedData           c1 = (PGPCompressedData)pgpFact.nextObject();

        pgpFact = new PGPObjectFactory(c1.getDataStream());
            
        PGPOnePassSignatureList     p1 = (PGPOnePassSignatureList)pgpFact.nextObject();
            
        PGPOnePassSignature         ops = p1.get(0);
            
        PGPLiteralData              p2 = (PGPLiteralData)pgpFact.nextObject();

        InputStream                 dIn = p2.getInputStream();
        int                         ch;

        PGPPublicKey                key = PGP.getKeyById(ops.getKeyID());
        if(key == null)
        {
        	throw new IllegalArgumentException("cannot find key to verify");
        }
        
        FileOutputStream            out = new FileOutputStream(outputFile);

        ops.init(new JcaPGPContentVerifierBuilderProvider().setProvider("SC"), key);
            
        while ((ch = dIn.read()) >= 0)
        {
            ops.update((byte)ch);
            out.write(ch);
        }

        out.close();
        
        PGPSignatureList            p3 = (PGPSignatureList)pgpFact.nextObject();
        
        boolean verify = ops.verify(p3.get(0));
        String result = constructSignatureVerification(verify, key, p3.get(0).getCreationTime(), new Date());

        return new Pair<String, Boolean>(result, verify);
    }
    
    public static void revokeKey(Context context, long keyIdToRevoke, char[] pass) throws SignatureException, PGPException
    {
    	Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    	
    	/*
    	 * We want to revoke all the keys on the keyring
    	 * so we'll iterate through all the keys on the
    	 * public keyring and add a revocation certificate
    	 * to each one.
    	 */
    	
    	//get the key to revoke
    	PGPPublicKey key = PGP.getKeyById(keyIdToRevoke);
    	
		PGPPublicKeyRingModel pkrm = PGP.getKeyRingByKey(key);
		
		//we need to delete the keyring from the db, we'll put it back once we're finished
		//operating on it
		PGP.modifyPubKeyRingCollection(context, PGP.PUBLIC_KEY_COLLECTION_DELETE, pkrm);
		
		Iterator<PGPPublicKey> itr = pkrm.getPublicKeys();
		while(itr.hasNext())
		{
			PGPPublicKey pk = itr.next();
			
			//remove the public key we are about to revoke (we'll put the revoked key back later)
        	PGPPublicKeyRingModel.removePublicKey(pkrm, pk);
        	
        	//create a content signer builder
        	PGPContentSignerBuilder pcsb = new JcaPGPContentSignerBuilder(key.getAlgorithm(), HashAlgorithmTags.SHA1);
        	
        	//create a signature generator passing in the content signer builder
        	PGPSignatureGenerator siggen = new PGPSignatureGenerator(pcsb);
        	
        	//get the secret key
        	PGPSecretKey secKey = PGP.getCorrespondingSecretKey(pk);
        	
        	//decrypt the secret key
        	BcPGPDigestCalculatorProvider sha256Calc = 
    				new BcPGPDigestCalculatorProvider();
            PBESecretKeyDecryptor pskd = 
            		(new BcPBESecretKeyDecryptorBuilder
            				(sha256Calc)).build(pass);
        	
        	/*
        	 * initialize the signature generator
        	 * 
        	 * if the key is a master key then use the PGPSignature.KEY_REVOCATION type
        	 * if it's a subkey then use the PGPSignature.SUBKEY_REVOCATION type
        	 */
            if(pk.isMasterKey())
            {
            	siggen.init(PGPSignature.KEY_REVOCATION, secKey.extractPrivateKey(pskd));
            }
            else
            {
            	siggen.init(PGPSignature.SUBKEY_REVOCATION, secKey.extractPrivateKey(pskd));
            }
        	
        	//generate the certificate
        	PGPSignature revokCert = siggen.generateCertification(pk);
        	
        	//add the revocation certificate to the key
        	PGPPublicKey.addCertification(pk, revokCert);
        	
        	//insert the newly revoked key back into the keyring
        	PGPPublicKeyRingModel.insertPublicKey(pkrm, pk);
		}
		
		//insert the revoked keyring back into the db
		PGP.modifyPubKeyRingCollection(context, PGP.PUBLIC_KEY_COLLECTION_INSERT, pkrm);
    }
    
    private static void processLine(PGPSignature sig, byte[] line)
            throws SignatureException, IOException
        {
            int length = getLengthWithoutWhiteSpace(line);
            if (length > 0)
            {
                sig.update(line, 0, length);
            }
        }

    private static void processLine(OutputStream aOut, PGPSignatureGenerator sGen, byte[] line)
        throws SignatureException, IOException
    {
        // note: trailing white space needs to be removed from the end of
        // each line for signature calculation RFC 4880 Section 7.1
        int length = getLengthWithoutWhiteSpace(line);
        if (length > 0)
        {
            sGen.update(line, 0, length);
        }

        aOut.write(line, 0, line.length);
    }
    
    private static int getLengthWithoutWhiteSpace(byte[] line)
    {
        int    end = line.length - 1;

        while (end >= 0 && isWhiteSpace(line[end]))
        {
            end--;
        }

        return end + 1;
    }
    
    private static boolean isWhiteSpace(byte b)
    {
        return isLineEnding(b) || b == '\t' || b == ' ';
    }
    
    private static boolean isLineEnding(byte b)
    {
        return b == '\r' || b == '\n';
    }
    
    private static int readInputLine(ByteArrayOutputStream bOut, InputStream fIn)
            throws IOException
        {
            bOut.reset();

            int lookAhead = -1;
            int ch;

            while ((ch = fIn.read()) >= 0)
            {
                bOut.write(ch);
                if (ch == '\r' || ch == '\n')
                {
                    lookAhead = readPassedEOL(bOut, ch, fIn);
                    break;
                }
            }

            return lookAhead;
        }

    private static int readInputLine(ByteArrayOutputStream bOut, int lookAhead, InputStream fIn)
        throws IOException
    {
        bOut.reset();

        int ch = lookAhead;

        do
        {
            bOut.write(ch);
            if (ch == '\r' || ch == '\n')
            {
                lookAhead = readPassedEOL(bOut, ch, fIn);
                break;
            }
        }
        while ((ch = fIn.read()) >= 0);

        if (ch < 0)
        {
            lookAhead = -1;
        }
        
        return lookAhead;
    }

    private static int readPassedEOL(ByteArrayOutputStream bOut, int lastCh, InputStream fIn)
        throws IOException
    {
        int lookAhead = fIn.read();

        if (lastCh == '\r' && lookAhead == '\n')
        {
            bOut.write(lookAhead);
            lookAhead = fIn.read();
        }

        return lookAhead;
    }
    
    private static byte[] getLineSeparator()
    {
        String nl = System.getProperty("line.separator");
        byte[] nlBytes = new byte[nl.length()];

        for (int i = 0; i != nlBytes.length; i++)
        {
            nlBytes[i] = (byte)nl.charAt(i);
        }

        return nlBytes;
    }
    
    private static int getLengthWithoutSeparatorOrTrailingWhitespace(byte[] line)
    {
        int    end = line.length - 1;

        while (end >= 0 && isWhiteSpace(line[end]))
        {
            end--;
        }

        return end + 1;
    }
}
