package uk.ac.brookes.danielf.pgpmail.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRingCollection;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.openpgp.PGPSignature;

import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingDataSource;
import uk.ac.brookes.danielf.pgpmail.db.PGPPrivateKeyRingModel;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingDataSource;
import uk.ac.brookes.danielf.pgpmail.db.PGPPublicKeyRingModel;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
	
	//TODO: add debug log statements to the beginning and end of every method
	
	//TODO: write comments for every method & class
	//new line constant
	public static final String NEW_LINE = System.getProperty("line.separator");
	
	public static final String PGP_VERSION_DECLARATION =
			"Version: PGPMail " + String.valueOf(PGP.PGP_VERSION) + " available for Android in the Play Store." + NEW_LINE;
	
	//the version number
	public static final int PGP_VERSION = 1;
	
	//the public key ring collection
	private static PGPPublicKeyRingCollection pubKeyRingCollection;
	//the private key ring collection
	private static PGPSecretKeyRingCollection privateKeyRingCollection;
	
	//Some DB constants
	public static final int PUBLIC_KEY_COLLECTION_INSERT      		 = 0;
	public static final int PUBLIC_KEY_COLLECTION_DELETE      		 = 1;
	public static final int PUBLIC_KEY_COLLECTION_MODIFY      		 = 2;
	public static final int PRIVATE_KEY_COLLECTION_INSERT     		 = 3;
	public static final int PRIVATE_KEY_COLLECTION_DELETE     		 = 4;
	public static final int PRIVATE_KEY_COLLECTION_MODIFY     		 = 5;
	
	//PGP modes
	public static final int PGP_ENCRYPT_ONLY             		     = 6;
	public static final int PGP_ENCRYPT_AND_SIGN 		             = 7;
	public static final int PGP_SIGN_ONLY		                     = 8;
	public static final int PGP_UNENCRYPTED                          = 9;
	
	//PGP message constants
	public static final String PGP_ENCRYPTED_MESSAGE_HEADER   		 = "-----BEGIN PGP MESSAGE-----" + NEW_LINE;
	public static final String PGP_ENCRYPTED_MESSAGE_FOOTER   		 = NEW_LINE + "-----END PGP MESSAGE-----";
	
	public static final String PGP_SIGNED_MESSAGE_HEADER      		 = "-----BEGIN PGP SIGNED MESSAGE-----" + NEW_LINE + NEW_LINE;    
	public static final String PGP_SIGNATURE_HEADER           		 = "-----BEGIN PGP SIGNATURE-----" + NEW_LINE;
	public static final String PGP_SIGNATURE_FOOTER           		 = NEW_LINE + "-----END PGP SIGNATURE-----";
	
	public static final String PGP_SIGNATURE_STATUS           		 = NEW_LINE + "*** PGP SIGNATURE STATUS: ";
	public static final String PGP_SIGNATURE_SIGNER           		 = NEW_LINE + "*** SIGNER: ";
	public static final String PGP_SIGNATURE_SIGNED_DATE      		 = NEW_LINE + "*** SIGNED: ";
	public static final String PGP_SIGNATURE_VERIFIED_DATE    		 = NEW_LINE + "*** VERIFIED: ";
	public static final String PGP_SIGNATURE_VERIFIED_MESSAGE_HEADER = NEW_LINE + "*** BEGIN PGP VERIFIED MESSAGE ***" + NEW_LINE + NEW_LINE;
	public static final String PGP_SIGNATURE_VERIFIED_MESSAGE_FOOTER = NEW_LINE + NEW_LINE + "*** END PGP VERIFIED MESSAGE ***";
	
	public static final String PGP_EXAMPLE_SIGNED_MESSAGE            =
			"-----BEGIN PGP SIGNED MESSAGE-----" + "\n" +
			"\n" +
			"this is a test message, " + "\n" +
			"signed with pgp using Test User's public key" + "\n" +
			"\n" +
			"adios." +
			"\n" +
			"-----BEGIN PGP SIGNATURE-----" + "\n" +
			"Version: PGPfreeware 6.5.8 for non-commercial use <http://www.pgp.com>" + "\n" +
			"\n" +
			"iQEVAwUBPT4oB+C7envFYmALAQG9swgAo1+IKwaObHsPHd43ekD6wZYEJ8xl6qfR" + "\n" +
			"AZp86aRCj3Pg49mS1BU2Yiq6QJPM0QTn7yCh2dWdr/1SvBvXavBvQfSmJTN4VU+j" + "\n" +
			"IcNoHsZqmpnWhuLnoeQ9/HqCOWw50NcY1wU/1CTZYKT/D0ZqgP9eyonn9kf0JOGz" + "\n" +
			"9PT/AK7MM+BFuO6CzTl0lXc0To3VPzRA87WU8IjTfEf/UGNWn3iysl6z/TQSKo1w" + "\n" +
			"zq5EP7endZIPy6aal8B6buB6ql24s0bcklFALj6Ux4HIjjh6IEfd5kiJjtJPiArd" + "\n" +
			"/xeY0fw0G39RpI5SrlhZNUCRR4m1wmQZX1d2L9Y9yoVjb2dq5xCDMA==" + "\n" +
			"=oBys" + "\n" +
			"-----END PGP SIGNATURE-----";	
	
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
				break;
			case PUBLIC_KEY_COLLECTION_DELETE:
				//delete an entry in the db
				pkrDataSource.deletePublicKeyRing(pubKeyRing);
				PGPPublicKeyRingCollection.removePublicKeyRing(pubKeyRingCollection, pubKeyRing);
				break;
			case PUBLIC_KEY_COLLECTION_MODIFY:
				//I think we need to add a new method into the DataSource classes
				//to handle modifying an existing record, whether we just delete and replace
				//is another matter....
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
				break;
			case PRIVATE_KEY_COLLECTION_DELETE:
				//delete an entry in the db
				skrDataSource.deletePrivateKeyRing(secKeyRing);
				PGPSecretKeyRingCollection.removeSecretKeyRing(privateKeyRingCollection, secKeyRing);
				break;
			case PRIVATE_KEY_COLLECTION_MODIFY:
				//I think we need to add a new method into the DataSource classes
				//to handle modifying an existing record, whether we just delete and replace
				//is another matter....
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
	 * Routine to check if a key has been revoked or has expired
	 * 
	 * @param key - the key to check
	 * @return
	 */
	public static boolean isKeyValid(PGPPublicKey key)
	{
		boolean revoked = true;
		boolean expired = true;
		
		//first let's check if the key has expired
		if(key.getValidDays() <= 0)
		{
			if(key.getValidSeconds() <= 0)
			{
				expired = true;
			}
		}
		else
		{
			expired = false;
		}
		
		Iterator<PGPSignature> itr = key.getSignaturesOfType(PGPSignature.KEY_REVOCATION);
		if(itr.hasNext())
			revoked = true;
		
		boolean valid = revoked && expired;
		
		//TODO: pretty sure this a super simplified view of what i'm trying to do here
		//maybe we need to be more advanced in the way we check revocation and expiration.
		
		return valid;
	}
}
