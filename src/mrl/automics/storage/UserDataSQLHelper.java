package mrl.automics.storage;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class UserDataSQLHelper extends SQLiteOpenHelper {
	
	 private static final String DATABASE_NAME = "user_data.db";
	 private static final int DATABASE_VERSION = 2;
	 private static final String TABLE_NAME = "userdata";   
	 private static final String TAG = "UserDataSQLHelper";
	
	public UserDataSQLHelper (Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_NAME +
				" (" + 
				" id INTEGER PRIMARY KEY AUTOINCREMENT," +
				" task_type VARCHAR(50)," +		//photo, annotate, story
				" task_id INTEGER," +
				" init_type VARCHAR(50)," +		//user vs. system (per notification)
				" gpstag_id INTEGER," +			
				" notification_type VARCHAR(50)," + //one shot vs. repeated vs. (ringing?)
				" notification_ts BIGINT," +
				" reminder INTEGER," +
				" reminder_timedout BOOL," +
				" notification_accept_ts BIGINT," +	//more than one?
				" acceptance_time BIGINT," +
				" task_acceptance_ts BIGINT," +
				" task_acceptance_time BIGINT," +
				" task_completed BOOL," +
				" task_completed_ts BIGINT," +
				" task_completion_time BIGINT," +
				" response_time BIGINT," +
				" receptivity_rating INTEGER" +
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
