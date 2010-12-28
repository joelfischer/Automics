package mrl.automics.sensors;

import java.io.File;

import mrl.automics.R;
import mrl.automics.storage.UserDataSQLHelper;
import mrl.automics.ui.MessageDisplay_Activity;
import mrl.automics.ui.NotificationCancel_Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;

public class MessageDelivery_Service extends Service {
	
	private static final String TAG = "MessageDelivery_Service";
	public static final int TASK_PHOTO = 1;
	public static final int TASK_ANNOTATE = 2;
	public static final int TASK_PICKER = 3;
	private String alertType;
	private String type;
	private int id;
	private int radius;
	private String comment;
	private Bundle extras;
	private int taskType;
	private boolean forcePhoto;
	private NotificationManager nm;
	private SQLiteDatabase userdata;
	private long ts;
	
	
	public void onCreate() {
		super.onCreate();
		
		nm = (NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);
	}
	
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		extras = intent.getExtras();
		alertType = extras.getString("alertType");
		ts = System.currentTimeMillis();
		if (alertType.equals("prox")) {
			//this is a proximity alert, get the other extras
			type = extras.getString("type");
			id = extras.getInt("id");
			radius = extras.getInt("radius");
			comment = extras.getString("comment");
			taskType = extras.getInt("taskType");
			forcePhoto = extras.getBoolean("forcePhoto", false);
			extras.putLong("ts", ts);
			Log.d(TAG, "id: "+id+", type: "+type);
			
			createNotification();
			logToUserDb(ts);
			
			//we need the id of this entry to keep track of the state of 
			//the notification for the reminders, e.g. if it has been 
			//"collected" and to cancel it etc.
			//Only alert repeatedly for queueing tasks for now...
			if (taskType == DeliveryManager_Service.TASK_ANNOTATE) 
				scheduleReminder(getUniqueId());
			
		}
		
		if (alertType.equals("img")) {
			//get the number of downloaded images
			int num = extras.getInt("num");
			type = "newImages";
			taskType = 4;
			id = 0;
			logToUserDb(ts);
			newImagesNotification(num);
		}
		
		
	}
	
	private void logToUserDb(long timestamp) {
		userdata = new UserDataSQLHelper(this).getWritableDatabase();
		userdata.execSQL("INSERT INTO userdata (task_type,task_id,init_type,gpstag_id,notification_ts,reminder) " +
				"VALUES (" +
				"'"+type+"',"+
				taskType +","+
				"'sys'," +
				id+","+
				timestamp + "," +
				0 +
				")"
				);
	}
	
	private int getUniqueId() {
		int id = 0;
		Cursor c = userdata.rawQuery("SELECT id FROM userdata WHERE id=(SELECT max(id) FROM userdata)", null);
		Log.d(TAG, "c.getCount(): "+c.getCount());
		if (c.getCount()>0) {
			c.moveToFirst();
			try {
				id = c.getInt(0);
			} catch (Exception e) {
				Log.d(TAG, e.toString());
			}
		}
		c.close();
		userdata.close();
		Log.d(TAG, "uniqueId: "+id);
		return id;
	}
	
	private void createNotification() {
		
		Notification notification;
        PendingIntent contentIntent = null;
		CharSequence text ="";
		Intent msgDisplay = new Intent(this, MessageDisplay_Activity.class).putExtras(extras);
		//to make the pending intent unique, we need to set a 
		msgDisplay.setData(Uri.parse(""+System.currentTimeMillis()));
//		msgDisplay.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
//		msgDisplay.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

		
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
	        notification.defaults = Notification.DEFAULT_ALL;
	        
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
	
	private void newImagesNotification(int num) {
		
		Log.d(TAG, "number of downloaded images: "+num);
		
		//get the Uri for which folder to show...
		File sdCard = Environment.getExternalStorageDirectory();
//		File store = new File(sdCard.getAbsolutePath()+"/DITP/Shared");
//		String path = store.getAbsolutePath().toLowerCase();
//		String name = store.getName().toLowerCase();
//		ContentValues values = new ContentValues();
//		values.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
//		values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
//
//		Uri uri = this.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
		
		Intent helperAct = new Intent(MessageDelivery_Service.this, NotificationCancel_Activity.class).putExtra("id", ts);

		
		// Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.photo_picker_icon_small, this.getText(R.string.new_images_alert),
                System.currentTimeMillis());
        notification.defaults = Notification.DEFAULT_LIGHTS;
        notification.defaults = Notification.DEFAULT_VIBRATE;
        notification.number = num;
        File sound = new File(sdCard.getAbsolutePath()+"/DITP/sound/bing.m4a");
        notification.sound = Uri.fromFile(sound);
        
        // The PendingIntent to launch our activity if the user selects this notification
        	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
        			helperAct.putExtra("nId", R.string.new_images_alert), PendingIntent.FLAG_UPDATE_CURRENT);
        
	    
	 // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, this.getText(R.string.new_images_alert_title),
                    this.getText(R.string.new_images_alert)+" "+num+" images", contentIntent);
	     // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        nm.notify(R.string.new_images_alert, notification);
		
		
	}
	
	private void scheduleReminder(int id) {
		extras.putInt("uniqueId", id);
		startService(new Intent(MessageDelivery_Service.this, TaskReminder_Service.class).putExtras(extras));
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
