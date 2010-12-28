package mrl.automics.sensors;

import mrl.automics.R;
import mrl.automics.storage.TriggerHistorySQLHelper;
import mrl.automics.storage.TriggerTimingSQLHelper;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class DeliveryOpportunityManager_Service extends Service {
	
	private static final String TAG = "DeliveryOpportunityManager_Service";
	private static final long MIN_TIME = 60*1000*10; //in ms;
	private static final long TIMEOUT = 60*1000*60; //60 minutes in ms;
	private String alertType;
	private String type;
	private int id;
	private int radius;
	private String comment;
	private SQLiteDatabase db;
	private SQLiteDatabase triggerhistory;
	private Bundle extras;

	
	public void onCreate() {
		super.onCreate();
		
		//init
		db = new TriggerTimingSQLHelper(this).getWritableDatabase();
		triggerhistory = new TriggerHistorySQLHelper(this).getWritableDatabase();

	}
	
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		//get the argumments
		extras = intent.getExtras();
		alertType = extras.getString("alertType");
		if (alertType.equals("prox")) {
			//this is a proximity alert, get the other extras
			type = extras.getString("type");
			id = extras.getInt("id");
			radius = extras.getInt("radius");
			comment = extras.getString("comment");
			
			Log.d(TAG, "type: "+type+", id: "+id);
			
			
			
			if (!type.equals("lunch/break")) {
				
				//check the history db if the user had already collected this potential message
				boolean alreadyCollected = checkGpsTriggerHistory();
				Log.d(TAG, "collected: "+alreadyCollected);
				removeTimingEntry();
				
				if (!alreadyCollected) {
					writeToGpsTriggerHistory();
					startService(new Intent(this, DeliveryManager_Service.class).putExtras(extras));
				}
			}
			else if (type.equals("lunch/break")) 
				evaluateTiming();
		}
		triggerhistory.close();
		this.stopSelf();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void removeTimingEntry() {
		//removes any entries (should only be one in there at a time...)
		if (db==null || !db.isOpen()) 
			db = new TriggerTimingSQLHelper(this).getWritableDatabase();
		db.execSQL("DELETE FROM triggertiming");
//		db.close();
	}
	
	private void evaluateTiming() {
		/**
		 * for lunch/break gps trigger zones, we want to 
		 * 1. wait another 10 minutes to make sure they are not 
		 * just passing through, still getting food etc... we want to notify them 
		 * when they've spend at least 10 minutes "in" the trigger zone.
		 * 2. make sure we haven't already fired in the last hr, to avoid 
		 * repeated firing. 
		 */

		
		//1. Check if this trigger is already in the db
		if (db==null || !db.isOpen()) 
			db = new TriggerTimingSQLHelper(this).getWritableDatabase();
		
		long timestamp = 0;
		
		Cursor c = db.rawQuery("SELECT timestamp FROM triggertiming WHERE triggerId="+id, null);
		if (c.getCount()>0) {
			//we already have this trigger in the db, means the user has been here
			c.moveToFirst();
			try {
				timestamp = c.getLong(0);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
			//debug
			long timespend = (System.currentTimeMillis() - timestamp)/1000;
//			Toast.makeText(this, "Have been here for "+ timespend +" s", Toast.LENGTH_SHORT).show();
			//2. Check if we have been here for MIN_TIME...
			if (System.currentTimeMillis() - timestamp >= MIN_TIME) {
				
				//we have been here for at least MIN_TIME, let's 
				//check if we have recently (within 1hr) already fired here...
				
//				Toast.makeText(this, "OK, "+ timespend +" s!", Toast.LENGTH_SHORT).show();
				
				if (triggerhistory == null || !triggerhistory.isOpen()) 
    				triggerhistory = new TriggerHistorySQLHelper(DeliveryOpportunityManager_Service.this).getWritableDatabase();
    			
				Cursor c2 = triggerhistory.rawQuery("SELECT timestamp FROM triggerhistory WHERE tagId="+id, null);
				if (c2.getCount()>0) {
//					Toast.makeText(this, "we have been here before", Toast.LENGTH_SHORT).show();
					
					//we already have fired a notification for this location, let's see
					//if it was at least 60min ago, otherwise ignore
					//check the most recent entry, if there are more for this trigger already in db...
					c.moveToLast();
					long lastFired = 0;
					try {
						lastFired = c.getLong(0);
					} catch (Exception e) {
						Log.e(TAG, e.toString());
					}
					timespend = (System.currentTimeMillis() - lastFired)/1000;
					Toast.makeText(this, "we have been here "+timespend+ " s ago", Toast.LENGTH_SHORT).show();
					if (System.currentTimeMillis() - lastFired >= TIMEOUT) {
//						Toast.makeText(this, "Long enough ago, fire!", Toast.LENGTH_SHORT).show();
						//ok, longer than 1hr ago, let's fire again...
						//note in triggerhistory...
						writeToGpsTriggerHistory();	
						removeTimingEntry();
						startService(new Intent(this, DeliveryManager_Service.class).putExtras(extras));
					}
				}
				
				else if (c2.getCount()==0) {
//					Toast.makeText(this, "No entry, fire!", Toast.LENGTH_SHORT).show();
					//we have not fired for this one within the last hr yet, fire...
	    			writeToGpsTriggerHistory();
										
					//TODO: instead of showing notification from here, delegate to DeliveryManager_Service
	//				showNotification();
					removeTimingEntry();
					startService(new Intent(this, DeliveryManager_Service.class).putExtras(extras));
				}
				c2.close();
			}
		}
		else if (c.getCount()==0) {
			//we do not have an entry for this trigger yet.
			//remove old triggers
			removeTimingEntry();
			//Add this trigger.
			db.execSQL("INSERT INTO triggertiming (triggerId,timestamp) VALUES ("+id +","+System.currentTimeMillis()+")");
//			Toast.makeText(this, "Entered break zone, counting down...", Toast.LENGTH_SHORT).show();
		}
		c.close();
		db.close();		
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		if (db!=null || db.isOpen())
			db.close();
		
		if (triggerhistory != null || triggerhistory.isOpen())
			triggerhistory.close();
	}
	
	private boolean checkGpsTriggerHistory() {
		if (triggerhistory == null || !triggerhistory.isOpen()) 
			triggerhistory = new TriggerHistorySQLHelper(DeliveryOpportunityManager_Service.this).getWritableDatabase();
		boolean collected = false;
		int isCollected = 0;
		
		Cursor c = triggerhistory.rawQuery("SELECT collected FROM triggerhistory WHERE tagId="+id, null);
		if (c.getCount()>0) {
			c.moveToFirst();
			try {
				isCollected = c.getInt(0);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
			if (isCollected==1) {
				collected = true;
			}
		}
		c.close();
		return collected;
	}
	
	private void writeToGpsTriggerHistory() {
		
		if (triggerhistory == null || !triggerhistory.isOpen()) 
			triggerhistory = new TriggerHistorySQLHelper(DeliveryOpportunityManager_Service.this).getWritableDatabase();
	
		triggerhistory.execSQL("INSERT INTO triggerhistory (tagId,type,comment,collected,timestamp) VALUES(" +
				id +","+
				"'"+type+"',"+
				"'"+comment+"',"+
				0+","+
				System.currentTimeMillis()+
				")");		
	}
	
	
	private void showNotification() {
		
		NotificationManager nm = (NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
		
		CharSequence text ="";
//		//cancel the new activity request notification
//      nm.cancel(R.string.app_reminder);
//      nm.cancel(R.string.diary_reminder);
//		
		Log.d("ProxAlert_BroadcastReceiver", "type: "+type);
		text = this.getText(R.string.proxalert)+" Type: "+type+ ", "+comment;
       
       
        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.eye_con, this.getText(R.string.proxalert),
                System.currentTimeMillis());
        notification.defaults = Notification.DEFAULT_ALL;

        PendingIntent contentIntent;
        //TODO: select activity/task first? Or generic message activity that selects proper task? 
        // The PendingIntent to launch our activity if the user selects this notification
        
	    contentIntent = PendingIntent.getActivity(this, 0,
	            new Intent(Intent.CATEGORY_HOME), 0);
        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, this.getText(R.string.proxalerttitle),
                    text, contentIntent);
	     // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.proxalert, notification);
    }

}
