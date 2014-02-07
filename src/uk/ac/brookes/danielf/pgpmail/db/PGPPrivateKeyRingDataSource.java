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

public class PGPPrivateKeyRingDataSource {

	//TODO: implement DB transactions!!
	
	//database fields
	private SQLiteDatabase database;
	private DBHelper dbHelper;
	private String[] allColumns = 
		{DBHelper.COLUMN_PRIVATE_KEYRING_ID, DBHelper.COLUMN_PRIVATE_KEYRING_BLOB};
	
	public PGPPrivateKeyRingDataSource(Context context) 
	{
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
	
	public PGPPrivateKeyRingModel createPGPPrivateKeyRing(
			byte[] privateKeyRingBlob) throws IOException, PGPException
	{
		Log.i(this.getClass().getCanonicalName(), "createPGPPrivateKeyRing - entry");
		
		ContentValues values = new ContentValues();
		values.put(DBHelper.COLUMN_PRIVATE_KEYRING_BLOB, privateKeyRingBlob);
		long insertId = database.insert(DBHelper.TABLE_PRIVATE_KEYRING
				, null, values);
		Cursor cursor = database.query(DBHelper.TABLE_PRIVATE_KEYRING
				, allColumns, DBHelper.COLUMN_PRIVATE_KEYRING_ID + "=" + insertId, 
				null, null, null, null);
		cursor.moveToFirst();
		PGPPrivateKeyRingModel privateKeyRing = cursorToPrivateKeyRing(cursor);
		cursor.close();
		
		Log.i(this.getClass().getCanonicalName(), "createPGPPrivateKeyRing - exit");
		
		return privateKeyRing;
	}
	
	public void deletePrivateKeyRing(
			PGPPrivateKeyRingModel privateKeyRing)
	{
		Log.i(this.getClass().getCanonicalName(), "deletePrivateKeyRing - entry");
		
		long id = privateKeyRing.getId();
		Log.i("Deleting private key ring collection with id = ", String.valueOf(id));
		database.delete(DBHelper.TABLE_PRIVATE_KEYRING
				, DBHelper.COLUMN_PRIVATE_KEYRING_ID + "=" + id, null);
		
		Log.i(this.getClass().getCanonicalName(), "deletePrivateKeyRing - exit");
	}
	
	public List<PGPPrivateKeyRingModel> getAllPrivateKeyRings() throws IOException, PGPException
	{
		Log.i(this.getClass().getCanonicalName(), "getAllPrivateKeyRings - entry");
		
		List<PGPPrivateKeyRingModel> privateKeyRingList = 
				new ArrayList<PGPPrivateKeyRingModel>();
		
		Cursor cursor = database.query(DBHelper.TABLE_PRIVATE_KEYRING,
				allColumns, null, null, null, null, null);
		
		cursor.moveToFirst();
		while(!cursor.isAfterLast())
		{
			PGPPrivateKeyRingModel privateKeyRing = cursorToPrivateKeyRing(cursor);
			privateKeyRingList.add(privateKeyRing);
			cursor.moveToNext();
		}
		//close the cursor
		cursor.close();
		
		Log.d(this.getClass().getCanonicalName(), "there are " + String.valueOf(privateKeyRingList.size()) + " private keys in db");
		Log.i(this.getClass().getCanonicalName(), "getAllPrivateKeyRings - exit");
		
		return privateKeyRingList;
	}
	
	private PGPPrivateKeyRingModel cursorToPrivateKeyRing(Cursor cursor) throws IOException, PGPException
	{
		Log.i(this.getClass().getCanonicalName(), "cursorToPrivateKeyRing - entry");
		
		PGPPrivateKeyRingModel privateKeyRing = 
				new PGPPrivateKeyRingModel(cursor.getLong(0), cursor.getBlob(1));
		
		Log.i(this.getClass().getCanonicalName(), "cursorToPrivateKeyRing - exit");
		
		return privateKeyRing;
	}
}
