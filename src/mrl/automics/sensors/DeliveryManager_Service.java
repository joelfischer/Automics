package mrl.automics.sensors;

import java.io.File;

import mrl.automics.storage.TriggerHistorySQLHelper;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;

/**
 * @author jef
 * The DeliveryManager_Service negotiates availability of tasks/messages and sensed opportunities of delivery. 
 * It considers (task) history - checks other sources (e.g. inbox) - suggests type of task/message. 
 * to be delivered based on type of trigger and available/prescribed task.  
 * By now, it is clear that we want to send a new task now (i.e. the responsibility of the calling service 
 * @see DeliveryOpportunityManager_Service), the question this Service tries to solve is *which task*? 
 */

public class DeliveryManager_Service extends Service {

	private static final String TAG = "DeliveryManager_Service";
	public static final int TASK_PHOTO = 1;
	public static final int TASK_ANNOTATE = 2;
	public static final int TASK_PICKER = 3;
	private static final int OBL_Q1 = 3;
	private static final int OBL_Q2 = 4;
	private static final int SONIC_Q = 56;	
	private String alertType;
	private String type;
	private int id;
	private int radius;
	private String comment;
	private Bundle extras;
	private String taskType;
//	private SQLiteDatabase triggerhistory;
	
	public void onCreate() {
		super.onCreate();
		
//        triggerhistory = new TriggerHistorySQLHelper(this).getWritableDatabase();
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
			
			if (type.equals("photo opportunity") || type.equals("end of ride")) {
				
				extras.putInt("taskType", TASK_PHOTO);
			}
			
			if (type.equals("lunch/break")) {
				//this should be a "picker" task - the users select 2 pics they've already annotated for the photo story 
				
				//TODO: constraint: 1. have they been on one of the 3 rides yet?
				//					--> check if GPS triggers have fired/put into db earlier
				//problem is: battle galleons ride has no Q trigger...
				
//				if (triggerhistory == null || !triggerhistory.isOpen()) 
//			        triggerhistory = new TriggerHistorySQLHelper(this).getWritableDatabase();
//
//				Cursor c = triggerhistory.rawQuery("SELECT tagId FROM triggerhistory WHERE tagId = ", null);
				
				//					2. are there enough annotated/cropped photos available online yet (for the ride)?
				//					--> assume yes if they've been on ride and there are more than 2 photos available.
				
				Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
	            String[] filePathColumn = {MediaStore.Images.Media.DATA};
	            Cursor cursor = getContentResolver().query(imagesUri, filePathColumn, null, null, null);
	            int imageCount = cursor.getCount();
	            cursor.close();
	            
	            if (imageCount>=2) {
	            	//we have at least two photo available, go ahead with the task...
	            	extras.putInt("taskType", TASK_PICKER);
	            }
	            else if (imageCount<2) {
	            	//not enough photos availabe yet, force photo first, then annotate...
	            	extras.putInt("taskType", TASK_PICKER);
	            	extras.putBoolean("forcePhoto", true);
	            }					
			}
			
			if (type.equals("Q-Zone")) {
				//check constraint: are there enough images available yet? 
//			      File sdCard = Environment.getExternalStorageDirectory();
			      Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		            String[] filePathColumn = {MediaStore.Images.Media.DATA};
		            Cursor cursor = getContentResolver().query(imagesUri, filePathColumn, null, null, null);
		            int imageCount = cursor.getCount();
		            cursor.close();
		            
		            if (imageCount>=1) {
		            	//we have at least one photo available, go ahead with the task...
		            	extras.putInt("taskType", TASK_ANNOTATE);
		            }
		            else if (imageCount<1) {
		            	//no photos availabe yet, force photo first, then annotate...
		            	extras.putInt("taskType", TASK_ANNOTATE);
		            	extras.putBoolean("forcePhoto", true);
		            }
			}
			
			//TODO: delegate to MessageDelivery_Service
			startService(new Intent(this, MessageDelivery_Service.class).putExtras(extras));
			
			this.stopSelf();
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
