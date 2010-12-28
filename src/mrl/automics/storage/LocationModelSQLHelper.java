package mrl.automics.storage;
/**
 * @author jef@cs.nott.ac.uk 
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LocationModelSQLHelper extends SQLiteOpenHelper {
	
	 private static final String DATABASE_NAME = "location_model.db"; 	
 	 private static final int DATABASE_VERSION = 2;
	 private static final String TABLE_NAME = "gps_triggers";   
	 private static final String TAG = "UserDataSQLHelper";
	
	 public LocationModelSQLHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME +
				" (" + 
				" id INTEGER PRIMARY KEY AUTOINCREMENT," +
				" tagId INTEGER," +
				" type VARCHAR(20)," +
				" radius INTEGER," +
				" comment VARCHAR(200)," +
				" lat BIGINT," +
				" long BIGINT" +
				");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS notes");
        onCreate(db);
		
	}

}
