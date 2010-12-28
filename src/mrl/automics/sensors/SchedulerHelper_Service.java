package mrl.automics.sensors;

import mrl.automics.storage.ToUploadImagesSQLHelper;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SchedulerHelper_Service extends Service {
	
	private PowerManager pm;
	private WakeLock wl;
	private static final int DELAY_60 = 60*1000;
	private static final int SCHEDULED = 1;
	private static final String TAG = "SchedulerHelper_Service";
	private String imageUri;
	private String title;
	private String internalName;
	private String type;
	private String bubbleMetaData = "";
	private SQLiteDatabase db;


	public void onCreate() {
		pm = (PowerManager)getSystemService(POWER_SERVICE);
		db = new ToUploadImagesSQLHelper(SchedulerHelper_Service.this).getWritableDatabase();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		if (!db.isOpen() || db == null) {
			db = new ToUploadImagesSQLHelper(SchedulerHelper_Service.this).getWritableDatabase();
		}
		
		if (wl != null && wl.isHeld())
			wl.release();
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		wl.acquire();
		
		imageUri = intent.getStringExtra("uri");
    	title = intent.getStringExtra("oriTitle");
    	internalName = intent.getStringExtra("bubblisedTitle");
    	type = intent.getStringExtra("img_type");
    	//type either one of ... 
    	if (type.equals("annotated"));
    		bubbleMetaData = intent.getStringExtra("bubble_meta");
    		
    	//check if this is already in our "toupload" db...
    	Cursor c = db.rawQuery("SELECT internal_name FROM toupload WHERE internal_name="+Long.parseLong(internalName), null);
    	if (c.getCount() > 0) {
    		//this file is already in the db, do nothing here
    	}
    	else if (c.getCount() == 0) {
    		//this is a new entry, insert.
    		db.execSQL("INSERT INTO toupload (title, internal_name, uri, type, meta)" +
    				" VALUES (" +
    				Long.parseLong(title)+","+
    				Long.parseLong(internalName)+",'"+
    				imageUri+"','"+
    				type+"','"+
    				bubbleMetaData+"'"+
    				")");
    	}
    	c.close();
    	db.close();
		
    	//only schedule again if not already scheduled...
    	if (!mHandler.hasMessages(SCHEDULED)) {
	    	mHandler.sendEmptyMessageDelayed(SCHEDULED, DELAY_60);	
			mHandler.postDelayed(new RunUploader(), DELAY_60);
			Toast.makeText(this, "scheduled to try upload again in 1min", Toast.LENGTH_LONG).show();
    	}
		
		
	}
	
	private class RunUploader implements Runnable {
		public void run() {
			
			if (!db.isOpen() || db == null) {
				db = new ToUploadImagesSQLHelper(SchedulerHelper_Service.this).getWritableDatabase();
			}
			Cursor c = db.rawQuery("SELECT title,internal_name,uri,type,meta FROM toupload", null);
	    	if (c.getCount() > 0) {
	    		//there are images not uploaded yet.
	    		c.moveToFirst();
	    		try {
	    			title = ""+c.getLong(0);
	    			internalName = ""+c.getLong(1);
	    			imageUri = c.getString(2);
	    			type = c.getString(3);
	    			bubbleMetaData = c.getString(4);
	    		} catch (Exception e) {
	    			Log.e(TAG, e.toString());
	    		}
	    		//start uploader with proper args...
				Intent cloudPush = new Intent(SchedulerHelper_Service.this, CloudPush_Service.class);
				cloudPush.putExtra("uri", imageUri);
				cloudPush.putExtra("oriTitle", title);
				cloudPush.putExtra("bubblisedTitle", internalName);
				cloudPush.putExtra("img_type", type);
				cloudPush.putExtra("bubble_meta", bubbleMetaData);
				startService(cloudPush);
				
				//delete this entry 
				db.execSQL("DELETE FROM toupload WHERE internal_name="+Long.parseLong(internalName));
	    	}
	    	while (c.moveToNext()) {
	    		try {
	    			title = ""+c.getLong(0);
	    			internalName = ""+c.getLong(1);
	    			imageUri = c.getString(2);
	    			type = c.getString(3);
	    			bubbleMetaData = c.getString(4);
	    		} catch (Exception e) {
	    			Log.e(TAG, e.toString());
	    		}
	    		//start uploader with proper args...
				Intent cloudPush = new Intent(SchedulerHelper_Service.this, CloudPush_Service.class);
				cloudPush.putExtra("uri", imageUri);
				cloudPush.putExtra("oriTitle", title);
				cloudPush.putExtra("bubblisedTitle", internalName);
				cloudPush.putExtra("img_type", type);
				cloudPush.putExtra("bubble_meta", bubbleMetaData);
				startService(cloudPush);
				
				//delete this entry 
				db.execSQL("DELETE FROM toupload WHERE internal_name="+Long.parseLong(internalName));
	    	}
	    	
	    	c.close();
	    	db.close();
			
			//release wake lock and finish
			if (!mHandler.hasMessages(SCHEDULED)) {
				Log.d(TAG, "no more delayed uploads scheduled");
				wl.release();
			}
			SchedulerHelper_Service.this.stopSelf();
		}
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private Handler mHandler = new Handler() {};

}
