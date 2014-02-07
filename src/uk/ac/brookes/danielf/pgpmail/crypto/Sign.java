package uk.ac.brookes.danielf.pgpmail.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureGenerator;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import android.util.Log;

/**
 * Class that handles signing and verifying.
 * 
 * Code based on org.bouncycastle.openpgp.examples.ClearSignedFileProcessor
 * 
 * @author danfitzgerald
 */
public class Sign {

	public Sign() {
	}
    
    /*
     * verify a clear text signed file
     */
    public static String verifyFile(
        String msg,
        PGPPublicKeyRingCollection krc)
        throws Exception
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
        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider("SC"), publicKey);
        
        //get the message itself from the signature
        String bytes = new String(out.toByteArray());
        Log.d("string to substring! we want msg", bytes);

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
        
        String result = constructSignatureVerification(sig.verify(), publicKey, sig.getCreationTime(), new Date());
       	
        return result;
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
    	String result = PGP.PGP_SIGNATURE_STATUS + sigStatus +
    		PGP.PGP_SIGNATURE_SIGNER + ids +
    		PGP.PGP_SIGNATURE_SIGNED_DATE + cDate +
    		PGP.PGP_SIGNATURE_VERIFIED_DATE + vDate;
    		
    	return result;
    }
    
    /*
     * create a clear text signed file.
     */
    public static byte[] signFile(
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
        		.setProvider("BC").build(pass));
        
        PGPSignatureGenerator           sGen = new PGPSignatureGenerator(
        		new JcaPGPContentSignerBuilder(keyIn.getPublicKey().getAlgorithm(), 
        				digest).setProvider("SC"));
        
        PGPSignatureSubpacketGenerator  spGen = new PGPSignatureSubpacketGenerator();
        
        sGen.init(PGPSignature.CANONICAL_TEXT_DOCUMENT, pgpPrivKey);
        
        Iterator<String>    it = keyIn.getPublicKey().getUserIDs();
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
