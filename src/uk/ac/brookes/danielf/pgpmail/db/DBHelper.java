package uk.ac.brookes.danielf.pgpmail.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This DBHelper class is designed to help insert values
 * into the DB PGPMail.
 * @author danfitzgerald
 *
 */
public class DBHelper extends SQLiteOpenHelper {
	
	//if we change the db schema we must increment the db version no.
	public final static int DATABASE_VERSION = 1;
	public final static String DATABASE_NAME = "PGPMail.db";
	
	//constants for the public keys table
	public final static String TABLE_PUBLIC_KEYRING = "publickeyring"; //name of the table
	public final static String COLUMN_PUBLIC_KEYRING_ID = "_id"; //primary key
	public final static String COLUMN_PUBLIC_KEYRING_BLOB = "publicKeyRingCollectionBlob";//Binary Large Object
	
	//constants for the private keys table
	public final static String TABLE_PRIVATE_KEYRING = "privatekeyring"; //name of the table
	public final static String COLUMN_PRIVATE_KEYRING_ID = "_id"; //primary key
	public final static String COLUMN_PRIVATE_KEYRING_BLOB = "privateKeyRingCollectionBlob"; //Binary Large Object
	
	//the sql create statement
	public final static String DATABASE_CREATE_PUBLIC =
		//create public key table
		"CREATE TABLE " + TABLE_PUBLIC_KEYRING 
		+ "(" + COLUMN_PUBLIC_KEYRING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
		+ COLUMN_PUBLIC_KEYRING_BLOB + " BINARY(500) NOT NULL);";
	public final static String DATABASE_CREATE_PRIVATE =
		//create private key table
		"CREATE TABLE " + TABLE_PRIVATE_KEYRING
		+ "(" + COLUMN_PRIVATE_KEYRING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
		+ COLUMN_PRIVATE_KEYRING_BLOB + " BINARY(500) NOT NULL);";
	
	public DBHelper(Context context) 
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		db.execSQL(DATABASE_CREATE_PUBLIC);
		db.execSQL(DATABASE_CREATE_PRIVATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		Log.w(DBHelper.class.getName(),
		        "Upgrading database from version " + oldVersion + " to "
		        + newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_PUBLIC_KEYRING + ", " + TABLE_PRIVATE_KEYRING);
		onCreate(db);
	}

}
