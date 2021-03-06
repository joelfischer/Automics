package mrl.automics.storage;
/**
 * @author jef@cs.nott.ac.uk 
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class ImageDataSQLHelper extends SQLiteOpenHelper {
	
	 private static final String DATABASE_NAME = "imagedata.db"; 	
 	 private static final int DATABASE_VERSION = 2;
	 private static final String TABLE_NAME = "imagedata";   
	 private static final String TAG = "ToUploadImagesSQLHelper";
	
	public ImageDataSQLHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME +
				" (" + 
				" id INTEGER PRIMARY KEY AUTOINCREMENT," +
				" server_name INTEGER," +
				" imei BIGINT," +
				" title VARCHAR(200)," +
				" internal_name BIGINT,"+
				" group_name VARCHAR(200)," +
				" version INTEGER," +
				" originator BIGINT," +
				" source_type VARCHAR(10)," +
				" time DATETIME DEFAULT CURRENT_TIMESTAMP,"+
				" timestamp BIGINT" +
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
