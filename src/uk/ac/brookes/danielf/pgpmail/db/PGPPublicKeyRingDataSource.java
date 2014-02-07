package uk.ac.brookes.danielf.pgpmail.db;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.spongycastle.openpgp.PGPException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class PGPPublicKeyRingDataSource {

	//TODO: implement DB transactions!!
	
	//Database fields
	private SQLiteDatabase database;
	private DBHelper dbHelper;
	private String[] allColumns = 
		{DBHelper.COLUMN_PUBLIC_KEYRING_ID, DBHelper.COLUMN_PUBLIC_KEYRING_BLOB};
	
	public PGPPublicKeyRingDataSource(Context context) {
		dbHelper = new DBHelper(context);
	}
	
	public void openWritable() 
	{
		database = dbHelper.getWritableDatabase();
	}
	
	public void openReadable()
	{
		database = dbHelper.getReadableDatabase();
	}
	
	public void close()
	{
		dbHelper.close();
	}
	
	public PGPPublicKeyRingModel createPGPPublicKeyRing(
			byte[] publicKeyRingBlob) throws IOException, PGPException
	{
		Log.i(this.getClass().getCanonicalName(), "createPGPPublicKeyRing - entry");
		
		ContentValues values = new ContentValues();
		values.put(DBHelper.COLUMN_PUBLIC_KEYRING_BLOB
				, publicKeyRingBlob);
		long insertId = database.insert(DBHelper.TABLE_PUBLIC_KEYRING, null, values);
		Cursor cursor = database.query(DBHelper.TABLE_PUBLIC_KEYRING
				, allColumns, DBHelper.COLUMN_PUBLIC_KEYRING_ID + "=" + insertId
				, null, null, null, null);
		cursor.moveToFirst();
		PGPPublicKeyRingModel newPubKeyRing = 
				cursorToPublicKeyRing(cursor);
		cursor.close();
		
		Log.i(this.getClass().getCanonicalName(), "createPGPPublicKeyRing - exit");
		return newPubKeyRing;
	}
	
	public void deletePublicKeyRing(PGPPublicKeyRingModel pubKeyRing)
	{
		Log.i(this.getClass().getCanonicalName(), "deletePublicKeyRing - entry");
		
		long id = pubKeyRing.getId();
		Log.i("Public key ring  deleted with id = ", String.valueOf(id));
		database.delete(DBHelper.TABLE_PUBLIC_KEYRING,
				DBHelper.COLUMN_PUBLIC_KEYRING_ID + "=" + id, null);
		
		Log.i(this.getClass().getCanonicalName(), "deletePublicKeyRing - exit");
	}
	
	public List<PGPPublicKeyRingModel> getAllPublicKeyRings() 
			throws IOException, PGPException
	{
		Log.i(this.getClass().getCanonicalName(), "getAllPublicKeyRings - entry");
		
		List<PGPPublicKeyRingModel> pubKeyRingList = 
				new ArrayList<PGPPublicKeyRingModel>();
		
		Cursor cursor = database.query(DBHelper.TABLE_PUBLIC_KEYRING,
				allColumns, null, null, null, null, null);
		Log.d(this.getClass().getCanonicalName() + " getAllPublicKeyRings", "there are this many cursors " + String.valueOf(cursor.getCount()));
		cursor.moveToFirst();
		while(!cursor.isAfterLast())
		{
			PGPPublicKeyRingModel pubKeyRing = cursorToPublicKeyRing(cursor);
			pubKeyRingList.add(pubKeyRing);
			cursor.moveToNext();
		}
		//close the cursor
		cursor.close();
		
		Log.d(this.getClass().getCanonicalName(), "there are " + String.valueOf(pubKeyRingList.size()) + " pub keys in db");
		
		Log.i(this.getClass().getCanonicalName(), "getAllPublicKeyRings - exit");
		
		return pubKeyRingList;
	}
	
	private PGPPublicKeyRingModel cursorToPublicKeyRing(Cursor cursor) 
			throws IOException, PGPException
	{
		Log.i(this.getClass().getCanonicalName(), "cursorToPublicKeyRing - entry");
		
		PGPPublicKeyRingModel pubKeyRing = 
				new PGPPublicKeyRingModel(cursor.getLong(0), cursor.getBlob(1));
		
		Log.i(this.getClass().getCanonicalName(), "cursorToPublicKeyRing - exit");
		return pubKeyRing;
	}
}
