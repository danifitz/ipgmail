package uk.ac.brookes.danielf.pgpmail.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.spongycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;

import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingDataSource;
import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingDataSource;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingModel;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

/**
 * This class will hold constants and static
 * variables to be used throughout the program as well as
 * convenience methods for various tasks commonly used
 * throughout the application.
 * @author danfitzgerald
 *
 */
public class PGP {
	
	static {
	    Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
	}
	
	//TODO: add debug log statements to the beginning and end of every method
	
	//TODO: write comments for every method & class
	
	//new line constant
	public static final String NEW_LINE = System.getProperty("line.separator");
	
	//the public key ring collection
	private static PGPPublicKeyRingCollection pubKeyRingCollection;
	//the private key ring collection
	private static PGPSecretKeyRingCollection privateKeyRingCollection;
	
	//Some DB constants
	public static final int PUBLIC_KEY_COLLECTION_INSERT      		 = 0;
	public static final int PUBLIC_KEY_COLLECTION_DELETE      		 = 1;
	public static final int PRIVATE_KEY_COLLECTION_INSERT     		 = 2;
	public static final int PRIVATE_KEY_COLLECTION_DELETE     		 = 3;
	
	//PGP modes
	public static final int PGP_ENCRYPT_ONLY             		     = 4;
	public static final int PGP_ENCRYPT_AND_SIGN 		             = 5;
	public static final int PGP_SIGN_ONLY		                     = 6;
	public static final int PGP_UNENCRYPTED                          = 7;
	
	//PGP message constants
	public static final String PGP_ENCRYPTED_MESSAGE_HEADER   		 = "-----BEGIN PGP MESSAGE-----";
	public static final String PGP_ENCRYPTED_MESSAGE_FOOTER   		 = "-----END PGP MESSAGE-----";
	
	public static final String PGP_SIGNED_MESSAGE_HEADER      		 = "-----BEGIN PGP SIGNED MESSAGE-----";    
	public static final String PGP_SIGNATURE_HEADER           		 = "-----BEGIN PGP SIGNATURE-----";
	public static final String PGP_SIGNATURE_FOOTER           		 = "-----END PGP SIGNATURE-----";
	
	public static final String PGP_SIGNATURE_STATUS           		 = "PGP SIGNATURE STATUS: ";
	public static final String PGP_SIGNATURE_SIGNER           		 = "SIGNER: ";
	public static final String PGP_SIGNATURE_SIGNED_DATE      		 = "SIGNED: ";
	public static final String PGP_SIGNATURE_VERIFIED_DATE    		 = "VERIFIED: ";
	
	public PGP() {}
	
	public static void populateKeyRingCollections(Context context) 
	{
		Log.i("PGP", "populateKeyRingCollection - entry");
		
		//public keys
		List<PGPPublicKeyRingModel> publicKeyRingList = null;
		PGPPublicKeyRingDataSource pubKeyRingDataSource = 
				new PGPPublicKeyRingDataSource(context);
		
		try {
			pubKeyRingDataSource.openReadable();
			publicKeyRingList = pubKeyRingDataSource.getAllPublicKeyRings();
			pubKeyRingDataSource.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PGPException e) {
			e.printStackTrace();
		}
		//put the public key rings into the collection
		try {
			pubKeyRingCollection = new PGPPublicKeyRingCollection(publicKeyRingList);
		} catch (IOException e) {
			Log.e("PGP", "while trying to create a public key collection an exception reading the stream of public keys occured", e);
			e.printStackTrace();
		} catch (PGPException e) {
			Log.e("PGP", "while trying to create a public key collection a non pgp public key was found", e);
			e.printStackTrace();
		}
		
		//private keys
		List<PGPPrivateKeyRingModel> privateKeyRingList = null;
		PGPPrivateKeyRingDataSource privateKeyRingDataSource =
				new PGPPrivateKeyRingDataSource(context);
		
		try {
			privateKeyRingDataSource.openReadable();
			privateKeyRingList = privateKeyRingDataSource.getAllPrivateKeyRings();
			privateKeyRingDataSource.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (PGPException e) {
			e.printStackTrace();
		}
		//put the private key rings into the collection
		try {
			privateKeyRingCollection = new PGPSecretKeyRingCollection(privateKeyRingList);
		} catch (IOException e) {
			Log.e("PGP", "while trying to create a private key collection an exception reading the stream of private keys occured", e);
			e.printStackTrace();
		} catch (PGPException e) {
			Log.e("PGP", "while trying to create a private key collection a non pgp private key was found", e);
			e.printStackTrace();
		}
		
		Log.i("PGP", "populateKeyRingCollection - exit");
	}
	
	/**
	 * Handles modifying the public key ring collection.
	 * Modifies the copy in memory as well as inserting/deleting/modifying
	 * the DB. 
	 * @param operation - indicates the type of modification operation.
	 * 					  Use one of the constants such as:
	 * 					  		PGP.PUBLIC_KEY_COLLECTION_UPDATE
	 * 
	 * @param pubKeyRing - the public key ring to insert/delete/update
	 */
	public static void modifyPubKeyRingCollection(Context context, int operation, PGPPublicKeyRingModel pubKeyRing) 
	{
		Log.i("PGP", "modifyPubKeyRingCollection - entry");
		
		PGPPublicKeyRingDataSource pkrDataSource = new PGPPublicKeyRingDataSource(context);
		pkrDataSource.openWritable();
		switch(operation)
		{
			case PUBLIC_KEY_COLLECTION_INSERT:
				try {
					//create a new entry in the db
					pkrDataSource.createPGPPublicKeyRing(pubKeyRing.getEncoded());
					//add the new key ring to the collection in memory
					PGPPublicKeyRingCollection.addPublicKeyRing(pubKeyRingCollection, pubKeyRing);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				}
				
				//refresh the key ring collection
				populateKeyRingCollections(context);
				break;
			case PUBLIC_KEY_COLLECTION_DELETE:
				//delete an entry in the db
				PGPPublicKeyRingCollection.removePublicKeyRing(pubKeyRingCollection, pubKeyRing);
				pkrDataSource.deletePublicKeyRing(pubKeyRing);
				
				//refresh the key ring collection
				populateKeyRingCollections(context);
				break;
			default:
				//do nothing
		}
		pkrDataSource.close();
		
		Log.i("PGP", "modifyPubKeyRingCollection - exit");
	}
	
	public static void modifyPrivateKeyRingCollection(Context context, int operation, PGPPrivateKeyRingModel secKeyRing)
	{
		Log.i("PGP", "modifyPrivateKeyRingCollection - entry");
		
		PGPPrivateKeyRingDataSource skrDataSource = new PGPPrivateKeyRingDataSource(context);
		skrDataSource.openWritable();
		switch(operation)
		{
			case PRIVATE_KEY_COLLECTION_INSERT:
				try {
					//create a new entry in the db
					skrDataSource.createPGPPrivateKeyRing(secKeyRing.getEncoded());
					//add the new key ring to the collection in memory
					PGPSecretKeyRingCollection.addSecretKeyRing(privateKeyRingCollection, secKeyRing);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (PGPException e) {
					e.printStackTrace();
				}
				
				//refresh the key ring collection
				populateKeyRingCollections(context);
				break;
			case PRIVATE_KEY_COLLECTION_DELETE:
				//delete an entry in the db
				skrDataSource.deletePrivateKeyRing(secKeyRing);
				PGPSecretKeyRingCollection.removeSecretKeyRing(privateKeyRingCollection, secKeyRing);
				
				//refresh the key ring collection
				populateKeyRingCollections(context);
				break;
			default:
				//do nothing
		}
		skrDataSource.close();
		
		Log.i("PGP", "modifyPrivateKeyRingCollection - exit");
	}

	public static PGPPublicKeyRingCollection getPubKeyRingCollection() {
		Log.i("PGP", "getPubKeyRingCollection - entry");
		return pubKeyRingCollection;
	}

	public static PGPSecretKeyRingCollection getPrivateKeyRingCollection() {
		Log.i("PGP", "getPrivateKeyRingCollection - entry");
		return privateKeyRingCollection;
	}
	
	public static List<PGPPublicKeyRingModel> getPublicKeys()
	{
		Log.i("PGP", "getPublicKeys - entry");
		
		List<PGPPublicKeyRingModel> pubKeyList = new ArrayList<PGPPublicKeyRingModel>();
		if(pubKeyRingCollection != null)
		{
			Iterator<PGPPublicKeyRingModel> itr = pubKeyRingCollection.getKeyRings();
			while(itr.hasNext())
			{
				pubKeyList.add((PGPPublicKeyRingModel) itr.next());
			}
		}
		
		Log.i("PGP", "getPublicKeys - exit");
		return pubKeyList;
	}
	
	public static List<PGPPrivateKeyRingModel> getPrivateKeys()
	{
		Log.i("PGP", "getPrivateKeys - entry");
		
		List<PGPPrivateKeyRingModel> secKeyList = new ArrayList<PGPPrivateKeyRingModel>();
		if(privateKeyRingCollection != null)
		{
			Iterator<PGPPrivateKeyRingModel> itr = privateKeyRingCollection.getKeyRings();
			while(itr.hasNext())
			{
				secKeyList.add((PGPPrivateKeyRingModel) itr.next());
			}
		}
		
		Log.i("PGP", "getPrivateKeys - exit");
		return secKeyList;
	}
	
	public static List<String> getUserIdsFromKeyringCollection(Object keyringList)
	{
		Log.i("PGP", "getIdentityFromKey - entry");
		
		//a list to hold the user id's pulled from the list of keyrings
		List<String> identities = new ArrayList<String>();
		
		//we are dealing with a list of either public/private key rings
		if(keyringList instanceof List)
		{
			Iterator<PGPKeyRing> itr = ((List) keyringList).iterator();
			//iterate over the list of keyrings
			while(itr.hasNext())
			{
				Object keyring = itr.next();
				//if the keyring is a public keyring
				if(keyring instanceof PGPPublicKeyRingModel)
				{
					Iterator<PGPPublicKey> itr1 = ((PGPPublicKeyRingModel) keyring).getPublicKeys();
					//iterate over the public keys in the keyring
					while(itr1.hasNext())
					{
						PGPPublicKey pk = (PGPPublicKey) itr1.next();
						Iterator<String> itr2 = pk.getUserIDs();
						//iterate over the user id's 
						while(itr2.hasNext())
						{
							String address = itr2.next();
							Log.d("getting identity from public key with id: " + pk.getKeyID(), address);
							identities.add(address);
						}
					}
				}
				//if the keyring is a private keyring
				else if(keyring instanceof PGPPrivateKeyRingModel)
				{
					Iterator<PGPSecretKey> itr1 = ((PGPPrivateKeyRingModel) keyring).getSecretKeys();
					//iterate over the secret keys in the keyring
					while(itr1.hasNext())
					{
						PGPSecretKey sk = itr1.next();
						Iterator<String> itr2 = sk.getUserIDs();
						//iterate over the user id's
						while(itr2.hasNext())
						{
							String address = itr2.next();
							Log.d("getting identity from private key with id: " + sk.getKeyID(), address);
							identities.add(address);
						}
					}
				}
			}
		}
		Log.i("PGP", "getIdentityFromKey - exit");
		return identities;
	}
	
	/**
	 * Method to return a whitespace separated string of user ids'
	 * associated with the passed in PGPPublicKey
	 * 
	 * @param - key - the public key to get user id's from
	 */
	public static String getUserIdsFromPublicKey(PGPPublicKey key)
	{
		Iterator<String> itr = key.getUserIDs();
		String ids = "";
		while(itr.hasNext())
		{
			//if it's not the first pass and not the last then separate each id with whitespace
			if(!ids.equals("") && itr.hasNext())
				ids += " ";
			ids += itr.next();
		}
		return ids;
	}
	
	/**
	 * Method to return a whitespace separated string of user ids'
	 * associated with the passed in PGPSecretKey
	 * 
	 * @param - key - the secret key to get user id's from
	 */
	public static String getUserIdsFromSecretKey(PGPSecretKey key)
	{
		Iterator<String> itr = key.getUserIDs();
		String ids = "";
		while(itr.hasNext())
		{
			//if it's not the first pass and not the last then separate each id with whitespace
			if(!ids.equals("") && itr.hasNext())
				ids += " ";
			ids += itr.next();
		}
		return ids;
	}
	
	/**
	 * Get a sub-key from the supplied keyring, this routine will
	 * return the first key it finds of the type specified by keyType.
	 * @param keyring - the keyring to get a sub-key from
	 * @param keyType - the type of sub-key to get i.e PGPPublicKey.RSA_ENCRYPT
	 * @return - a sub-key
	 */
	public static Object getSubKeyFromKeyRing(Object keyring, int keyType)
	{
		if(keyring instanceof PGPPublicKeyRingModel)
		{
			Iterator<PGPPublicKey> itr = ((PGPPublicKeyRingModel) keyring).getPublicKeys();
			while(itr.hasNext())
			{
				PGPPublicKey pubKey = itr.next();
				if(pubKey.getAlgorithm() == keyType)
					return pubKey;
			}
		}
//		if(keyring instanceof PGPPrivateKeyRingModel)
//		{
//			Iterator<PGPPrivateKey> itr = ((PGPPrivateKeyRingModel) keyring).getSecretKeys();
//			while(itr.hasNext())
//			{
//				PGPSecretKey secKey = itr.next();
//				if(secKey.)
//			}
//		}
		return null;
	}
	
	/** 
	 * Routine to check if a key has expired or been revoked
	 * 
	 * @param key - the key to check
	 * @return - has the key expired or been revoked?
	 */
	public static boolean isKeyValid(PGPPublicKey key)
	{
		boolean expired = false;
		
		//first let's check if the key has expired
		if(key.getValidDays() < 0)
		{
			expired = true;
		}
		//TODO: we need to check if the key has expired properly...
		if(!key.isRevoked())
			return true;
		else
			return false;
	}
	
	public static String getAlgorithmAsString(int algorithm)
	{
		String algo = "";
		
		switch(algorithm)
		{
			case PublicKeyAlgorithmTags.DIFFIE_HELLMAN:
				algo = "Diffie Hellman";
				break;
			case PublicKeyAlgorithmTags.DSA:
				algo = "DSA";
				break;
			case PublicKeyAlgorithmTags.EC:
				algo = "Elliptical Curve";
				break;
			case PublicKeyAlgorithmTags.ECDSA:
				algo = "Elliptical Curve with DSA";
				break;
			case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
				algo = "El Gamal-Encrypt";
				break;
			case PublicKeyAlgorithmTags.ELGAMAL_GENERAL:
				algo = "El Gamal-General";
				break;
			case PublicKeyAlgorithmTags.RSA_ENCRYPT:
				algo = "RSA-Encrypt";
				break;
			case PublicKeyAlgorithmTags.RSA_GENERAL:
				algo = "RSA-General";
				break;
			case PublicKeyAlgorithmTags.RSA_SIGN:
				algo = "RSA-Sign";
				break;
		}
		
		return algo;
	}
	
	public static PGPPublicKey getKeyById(long id)
	{
		PGPPublicKey key = null;
		try {
			key = pubKeyRingCollection.getPublicKey(id);
		} catch (PGPException e) {
			e.printStackTrace();
		}
		return key;
	}
	
	public static PGPPublicKeyRingModel getKeyRingByKey(PGPPublicKey key)
	{
		Iterator<PGPPublicKeyRingModel> itr = pubKeyRingCollection.getKeyRings();
		while(itr.hasNext())
		{
			PGPPublicKeyRingModel keyring = itr.next();
			if(keyring.getPublicKey(key.getKeyID()) != null)
			{
				return keyring;
			}
		}
		return null;
	}
	
	public static PGPPrivateKey findSecretKey(
            PGPSecretKeyRingCollection pgpSec, 
            long keyID, 
            char[] pass)
            throws PGPException, NoSuchProviderException {
        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);

        if (pgpSecKey == null) {
            Log.d("PGP - findSecretKey()", "no secret key found in keyring matching id: " + String.valueOf(keyID));
        	return null;
        }

        BcPGPDigestCalculatorProvider sha256Calc = 
				new BcPGPDigestCalculatorProvider();
        PBESecretKeyDecryptor pskd = 
        		(new BcPBESecretKeyDecryptorBuilder
        				(sha256Calc)).build(pass);
        
        return pgpSecKey.extractPrivateKey(pskd);
    }
	
	/**
	 * Get the secret key that corresponds to the passed in public key
	 * 
	 * @param pubKey
	 */
	public static PGPSecretKey getCorrespondingSecretKey(PGPPublicKey pubKey)
	{
		PGPSecretKey secKey = null;
		
		//get the fingerprint from the pub key
		byte[] pubFingerprint = pubKey.getFingerprint();
		
		//get a list of the private keyrings
		List<PGPPrivateKeyRingModel> secKeyList = getPrivateKeys();
		
		//iterate through them
		ListIterator<PGPPrivateKeyRingModel> listItr = secKeyList.listIterator();
		while(listItr.hasNext())
		{
			/*
			 * We'll try and match the fingerprint of the passed-in
			 * pub key with secKey.getPublicKey().getFingerprint();
			 */
			PGPPrivateKeyRingModel pkr = listItr.next();
			Iterator<PGPSecretKey> secItr = pkr.getSecretKeys();
			while(secItr.hasNext())
			{
				secKey = secItr.next();
				byte[] secFingerprint = secKey.getPublicKey().getFingerprint();
				if(pubFingerprint.equals(secFingerprint));
					return secKey;
			}
		}
		return null;
	}
	
	/**
	 * Creates a PGPPublicKeyRing object from a key
	 * string and adds the new keyring to the collection + db
	 * @param keyText - the string containing the key
	 * @param context
	 * @throws IOException 
	 */
	public static void parseKeyString(String keyText, Context context) throws IOException
	{
		//strip the headers
		String key = keyText.substring(
				keyText.indexOf("pgp.mit.edu") + 11, keyText.indexOf("-----END PGP"));
		
		Log.d("PGP - Parse Key String", "parsing " + key);
		
		//get an inputstream from the string
		InputStream is = new ByteArrayInputStream(key.getBytes());
		
		//instantiate the keyring
		PGPPublicKeyRing pkr = null;
		
		//new pgp object factory
		PGPObjectFactory objFact = new PGPObjectFactory(PGPUtil.getDecoderStream(is));
		
		//get the next object
		Object o = objFact.nextObject();
		
		//we should have a pgppublickeyring
		if(o instanceof PGPPublicKeyRing)
		{
			pkr = (PGPPublicKeyRing) o;
			
			//setting the id as 0 because when we insert it into the db the id should be set by the db.
			PGPPublicKeyRingModel pkrm = new PGPPublicKeyRingModel(0, pkr.getEncoded());
			
			//add the newly formed keyring to the keyring collection
			modifyPubKeyRingCollection(context, PUBLIC_KEY_COLLECTION_INSERT, pkrm);
		}
		else
		{
			throw new IllegalArgumentException("Supplied text doesn't contain a PGP Public Key!");
		}
	}
	
	public static PGPPublicKey getMasterKeyFromKeyRing(PGPPublicKeyRingModel keyring)
	{
		Iterator<PGPPublicKey> itr = keyring.getPublicKeys();
		PGPPublicKey master = null;
		while(itr.hasNext())
		{
			master = itr.next();
			if(master.isMasterKey())
				return master;
		}
		return null;
	}
	
	public static int getNumOfSigsOnPubKey(PGPPublicKey key)
	{
		Iterator<PGPSignature> sigs = key.getSignatures();
		int i = 0;
		while(sigs.hasNext())
		{
			i++;
		}
		return i;
	}
	
	/**
	 * Checks if the device has writable external storage
	 * available on the current device
	 */
	public static boolean hasExternalStorage()
	{
		boolean externalStorageAvailable;
		String state = Environment.getExternalStorageState();
		
		if(Environment.MEDIA_MOUNTED.equals(state))
		{
			//we can read and write the media
			externalStorageAvailable =  true;
		}
		else
		{
			//we can neither read nor write, something is a bit iffy
			externalStorageAvailable = false;
		}
		return externalStorageAvailable;
	}
	
	/**
	 * Returns the external 'Download' directory
	 * @return
	 */
	public static File getExternalDownloadsDir()
	{
		File downloads = null;
		if(hasExternalStorage())
		{
			File root = Environment.getExternalStorageDirectory();
			Log.d("PGP", "think this is root dir " + root.getAbsolutePath());
			
			//now we need to get to external download dir
			downloads = new File(root.getAbsolutePath() + "/Download/");
			if(downloads.exists())
			{
				Log.d("PGP", "successfully obtained downloads dir");
			}
		}
		return downloads;
	}
	
}
