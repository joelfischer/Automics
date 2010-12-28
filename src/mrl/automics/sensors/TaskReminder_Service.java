package mrl.automics.sensors;

import mrl.automics.R;
import mrl.automics.storage.UserDataSQLHelper;
import mrl.automics.ui.MessageDisplay_Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;

public class TaskReminder_Service extends Service {   
  
	NotificationManager nm;
	private HandlerThread thread = null;
	
	private volatile Looper looper;
	private volatile Handler handler;
	private final static long DELAY_TIME = 1000*30*1;	//ms*s*min (=30sec)
	private final static long WAKE_TIME = 1000*35*1;	//ms*s*min (=35sec)
	
 /** Called when the activity is first created. */
    @Override
	public void onCreate() {
                
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);        
        Log.d("TaskReminder_Service", "created");
       
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
    	
    	 Log.d("TaskReminder_Service", "started");
    	 
    	 Bundle extras = intent.getExtras();
//    	
    	//make sure a running thread is stopped first, this reminder must override old ones...
//    	if (thread != null) {
//    		looper = thread.getLooper();
//    		looper.quit();
//    		thread = null;
//    		Log.d("TaskReminder_Service", "thread stopped");
//    	}
    	 RunThread mTask = new RunThread();
    	 mTask.setExtras(extras);
    	 thread = new HandlerThread("TaskReminder_Service");
         thread.start();
         
         looper = thread.getLooper();
         handler = new Handler(looper);
         //schedule 1st task reminder to be exec after DELAY_TIME
         handler.postDelayed(mTask, DELAY_TIME);
         //we need a wake lock so the device doesn't go to sleep to avoid that the reminder is never executed during sleep
         PowerManager pm = (PowerManager) getSystemService(TaskReminder_Service.POWER_SERVICE);
         PowerManager.WakeLock w = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TaskReminder_Service");
         w.acquire(WAKE_TIME);   
         
         Log.d("TaskReminder_Service", "scheduled first reminder");
    }
    
    class RunThread extends Thread implements Runnable {
    	
    	private Bundle extras;
    	SQLiteDatabase db;
    	
    	public void setExtras(Bundle extras) {
    		this.extras = extras;
    	}
    	
        public void run() {
        	
            db = new UserDataSQLHelper(TaskReminder_Service.this).getWritableDatabase();

	       	int id = extras.getInt("uniqueId", -1);
	       	boolean forcePhoto = extras.getBoolean("forcePhoto", false);
        	
        	synchronized (mBinder) {
        		
        		//get reminder count from DB
        		int reminder = -1;
        		int taskId = 0;
        		long accTs = 0;
        		
        		Cursor c = db.rawQuery("SELECT"  +
        				" reminder," +
        				" task_id," +
        				" notification_accept_ts" +
        				" FROM userdata WHERE id="+id, null);
        		
        		if (c.getCount()!=0) {
        			c.moveToFirst();
	        		try {
	        			reminder = c.getInt(0);
	        		} catch (NullPointerException npe) {Log.e(getClass().getSimpleName(), npe.toString());}
	        		try {
	        			taskId = c.getInt(1);
	        		} catch (NullPointerException npe) {Log.e(getClass().getSimpleName(), npe.toString());}
	           		try {
	        			accTs = c.getLong(2);
	        		} catch (NullPointerException npe) {Log.e(getClass().getSimpleName(), npe.toString());}
        		}
        		c.close();
        		Log.d("TaskReminder_Service","reminder:"+reminder+", taskId: "+taskId);
        		//increment reminder first...
        		reminder = reminder+1;
        		Log.d("TaskReminder_Service","reminder:"+reminder);
        		
        		/**
        		 * cases when to remind: 
        		 * - user has not accepted the notification yet
        		 */
        		if (accTs == 0) {
        			if (reminder > 0) {
            			//true for the first and second reminder (after DELAY_TIME and DELAY_TIME*2)
            			showNotification(taskId, forcePhoto, extras);   
            			
            			Log.d("TaskReminder_Service", "reminder fired");
            			
            			db.execSQL("UPDATE userdata SET " +
                				"reminder=reminder +1" +
            					" WHERE id="+id);
            			handler.postDelayed(this, DELAY_TIME);
            			Log.d("TaskReminder_Service", "scheduled second reminder");
            			//we need a wake lock so the device doesn't go to sleep to avoid that the reminder is never executed during sleep
            			PowerManager pm = (PowerManager) getSystemService(TaskReminder_Service.POWER_SERVICE);
            	        PowerManager.WakeLock w2 = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TaskReminder_Service");
            	        w2.acquire(WAKE_TIME);
//            	        if (reminder==1) {
//                	        startService(new Intent(TaskReminder_Service.this, ActivityMonitor_Service.class).putExtra("moment", "b"));
//            	        }
//            	        if (reminder==2) {
//                	        startService(new Intent(TaskReminder_Service.this, ActivityMonitor_Service.class).putExtra("moment", "c"));
//            	        }
            		}
            		
//            		if (reminder == 3) {
//            			//true when the notification times out (at the time of the third reminder)
//            			nm.cancel(R.string.interruption_reminder);
//            		
//            			db.execSQL("UPDATE behaviour SET " +
//                				"reminder_timedout=1" +
//            					" WHERE id=(SELECT max(id) FROM behaviour)");
//            			db.close();
//            			Log.d("TaskReminder_Service", "notification timed out...");
//            			TaskReminder_Service.this.stopSelf();
//            		}
        		}
        		else {
        			Log.d("TaskReminder_Service", "cancelled");
        			TaskReminder_Service.this.stopSelf();
        		}
        		if (db!=null || db.isOpen())
            		db.close();
        	}
        }
    };
    
    
    private void showNotification(int taskType, boolean forcePhoto, Bundle extras) {
    	Notification notification;
        PendingIntent contentIntent = null;
		CharSequence text ="";
		Intent msgDisplay = new Intent(this, MessageDisplay_Activity.class).putExtras(extras);
		msgDisplay.setData(Uri.parse(""+System.currentTimeMillis()));
		
		if (taskType == DeliveryManager_Service.TASK_PHOTO) {
			//this is a photo task
			
			// Set the icon, scrolling text and timestamp
	        notification = new Notification(R.drawable.camera_icon_small, this.getText(R.string.photo_task_alert),
	                System.currentTimeMillis());
	        notification.defaults = Notification.DEFAULT_ALL;
	        
	        // The PendingIntent to launch our activity if the user selects this notification
		    contentIntent = PendingIntent.getActivity(this, 0,
		            msgDisplay.putExtra("nId", R.string.photo_task_alert), PendingIntent.FLAG_UPDATE_CURRENT);
		    
		 // Set the info for the views that show in the notification panel.
			text = this.getText(R.string.photo_task_alert);
	        notification.setLatestEventInfo(this, this.getText(R.string.photo_task_alert_title),
	                    text, contentIntent);
		     // Send the notification.
	        // We use a layout id because it is a unique number.  We use it later to cancel.
	        nm.notify(R.string.photo_task_alert, notification);
		}
		
		if (taskType == DeliveryManager_Service.TASK_ANNOTATE) {
			//this is an annotation task
	
			// Set the icon, scrolling text and timestamp
	        notification = new Notification(R.drawable.photo_story_icon_small, this.getText(R.string.annotation_task_alert),
	                System.currentTimeMillis());
	        notification.defaults = Notification.DEFAULT_LIGHTS;
	        notification.defaults = Notification.DEFAULT_SOUND;
	        long[] vibrator = {0, 2000, 1000, 2000, 1000, 2000};
	        notification.vibrate = vibrator;
	        
	        // The PendingIntent to launch our activity if the user selects this notification
	        if (forcePhoto) {
	        	contentIntent = PendingIntent.getActivity(this, 0,
	        			msgDisplay.putExtra("nId", R.string.annotation_task_alert), PendingIntent.FLAG_UPDATE_CURRENT);
	        }
	        else if (!forcePhoto) {
	        	contentIntent = PendingIntent.getActivity(this, 0,
	        			msgDisplay.putExtra("nId", R.string.annotation_task_alert), PendingIntent.FLAG_UPDATE_CURRENT);
	        }
		    
		 // Set the info for the views that show in the notification panel.
	        notification.setLatestEventInfo(this, this.getText(R.string.annotation_task_alert_title),
	                    this.getText(R.string.annotation_task_alert), contentIntent);
		     // Send the notification.
	        // We use a layout id because it is a unique number.  We use it later to cancel.
	        nm.notify(R.string.annotation_task_alert, notification);
		}
		
		if (taskType == DeliveryManager_Service.TASK_PICKER) {
			//this is an annotation task
		
			// Set the icon, scrolling text and timestamp
	        notification = new Notification(R.drawable.photo_picker_icon_small, this.getText(R.string.picker_task_alert),
	                System.currentTimeMillis());
	        notification.defaults = Notification.DEFAULT_ALL;
	        
	        // The PendingIntent to launch our activity if the user selects this notification
	        if (forcePhoto) {
	        	contentIntent = PendingIntent.getActivity(this, 0,
	        			msgDisplay.putExtra("nId", R.string.picker_task_alert), PendingIntent.FLAG_UPDATE_CURRENT);
	        }
	        else if (!forcePhoto) {
	        	contentIntent = PendingIntent.getActivity(this, 0,
	        			msgDisplay.putExtra("nId", R.string.picker_task_alert), PendingIntent.FLAG_UPDATE_CURRENT);
	        }
		    
		 // Set the info for the views that show in the notification panel.
	        notification.setLatestEventInfo(this, this.getText(R.string.picker_task_alert_title),
	                    this.getText(R.string.picker_task_alert), contentIntent);
		     // Send the notification.
	        // We use a layout id because it is a unique number.  We use it later to cancel.
	        nm.notify(R.string.picker_task_alert, notification);
		}
    
    }
    
    public void onDestroy() {
    	super.onDestroy();
    }

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	  /**
     * This is the object that receives interactions from clients.  See RemoteService
     * for a more complete example.
     */
    private final IBinder mBinder = new Binder() {
        @Override
		protected boolean onTransact(int code, Parcel data, Parcel reply,
		        int flags) throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    }; 
}