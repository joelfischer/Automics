package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.sensors.CloudPull_Service;
import mrl.automics.sensors.DeliveryManager_Service;
import mrl.automics.sensors.LastLocation;
import mrl.automics.storage.TriggerHistorySQLHelper;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MessageDisplay_Activity extends Activity {
	
	private static final String TAG = "MessageDisplay_Activity";
	private static final int PICK_IMAGE = 1;
	private static final int TAKE_PHOTO = 2;
	private final static String UPLOAD = "mrl.automics.sensors.UPLOAD";
	private String alertType;
	private String type;
	private int id;
	private int radius;
	private String comment;
	private Bundle extras;
	private int taskType;
	private boolean forcePhoto;
	private Uri takenPhotoUri;
	private Intent cameraIntent;
	private Intent galleryIntent;
	private LinearLayout ll;
	private TextView tv;
	private SQLiteDatabase userdata;
	private long ts;
	private NotificationManager nm;
	private String filename;
	
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		//TODO
		setContentView(R.layout.messageview);		
		ll = (LinearLayout)findViewById(R.id.message_layout);

		nm = (NotificationManager)getSystemService(Service.NOTIFICATION_SERVICE);

		extras = this.getIntent().getExtras();
		int notificationId = this.getIntent().getIntExtra("nId", 0);
		nm.cancel(notificationId);
		populateLayout();

	}
	
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent()");
		super.onNewIntent(intent);
		String type = intent.getExtras().getString("type");
		Log.d(TAG, "new intent type: "+type);
		extras = intent.getExtras();
		int notificationId = intent.getIntExtra("nId", 0);
		nm.cancel(notificationId);
		//remove any old views if this view is not created from fresh...
		ll.removeAllViews();
		populateLayout();
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

	    switch(requestCode) { 
	    case PICK_IMAGE:
	        if(resultCode == RESULT_OK) {  
//	        	if (taskType == DeliveryManager_Service.TASK_PHOTO) {
//		            Uri selectedImage = imageReturnedIntent.getData();       
////		            Intent photoSharer = new Intent(MessageDisplay_Activity.this, ImageSharer_Activity.class);
////		    		photoSharer.putExtra("justShare", true);
////		    		photoSharer.putExtra("tagId", ts);
////		            startActivity(photoSharer.putExtra("image", selectedImage.toString()));
//		            Intent imageCropper = new Intent(MessageDisplay_Activity.this, ImageSectionSelector_Activity.class);
//		            imageCropper.putExtra("ts", ts);
//		            startActivity(imageCropper.putExtra("image", selectedImage.toString()));
//		            this.finish();
//		            
//	        	}
	        	if (taskType == DeliveryManager_Service.TASK_ANNOTATE) {
	        		Uri selectedImage = imageReturnedIntent.getData();   
	        		Intent imageCropper = new Intent(MessageDisplay_Activity.this, ImageSectionSelector_Activity.class);
	 	            imageCropper.putExtra("tsNot", ts);
	 	            startActivity(imageCropper.putExtra("image", selectedImage.toString()));
//	        		Intent photoSharer = new Intent(MessageDisplay_Activity.this, ImageSharer_Activity.class);
//		    		photoSharer.putExtra("tagId", ts);
//		            startActivity(photoSharer.putExtra("image", selectedImage.toString()));
		            this.finish();
	        	}
	        }
	        break;
	    case TAKE_PHOTO:
	    	if (resultCode == RESULT_OK) {
//	    			startActivityForResult(galleryIntent, PICK_IMAGE);
	    			//see if there's something to download first...
	    			startService(new Intent(MessageDisplay_Activity.this, CloudPull_Service.class));
	    			Intent uploadInt = new Intent(UPLOAD);
		    		uploadInt.putExtra("uri", takenPhotoUri.toString());
		    		uploadInt.putExtra("img_type", "raw");
		    		uploadInt.putExtra("oriTitle", filename);
		    		MessageDisplay_Activity.this.sendBroadcast(uploadInt);
		    		logCompletionToUserDb(ts);
		    		startActivity(new Intent(MessageDisplay_Activity.this, MultChoice_Activity.class).putExtra("ts", ts));
		    		MessageDisplay_Activity.this.finish();
	    	}
	    	break;
	    }
	}
	
	private void populateLayout() {		
		
		//log timestamp
		ts = extras.getLong("ts", 0);
		logToUserDb(ts);
		alertType = extras.getString("alertType");
		if (alertType.equals("prox")) {
			//this is a proximity alert, get the other extras
			type = extras.getString("type");
			id = extras.getInt("id");
			radius = extras.getInt("radius");
			comment = extras.getString("comment");
			taskType = extras.getInt("taskType");
			forcePhoto = extras.getBoolean("forcePhoto", false);
			logToGpsTriggerHistory();
		}
		Log.d(TAG, "id: "+ ts+", type: "+type);
				
//		tv = (TextView)findViewById(R.id.message_txt);
		tv = new TextView(this);
		
		ll.setBackgroundResource(determineBackground(id));
		ll.setGravity(Gravity.CENTER_HORIZONTAL);
		tv.setText(determineTaskText(id));
		tv.setTextSize(20);
		tv.setTextColor(Color.WHITE);
		tv.setPadding(5, 10, 5, 10);
		tv.setGravity(Gravity.CENTER_HORIZONTAL);
		ll.addView(tv);
		
		//the intent to launch the gallery
		galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryIntent.setType("image/*");
		
		//the intent to launch the camera and setup for file storage
		cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);		
		
		if (taskType == DeliveryManager_Service.TASK_ANNOTATE) {
			if (forcePhoto) {
				Button launchCam = new Button(this);
				launchCam.setText("Take a photo now");
				launchCam.setTextSize(20);
				launchCam.setGravity(Gravity.CENTER_HORIZONTAL);
				launchCam.setLayoutParams(params);
				launchCam.setPadding(15, 10, 15, 10);
				ll.addView(launchCam);
				launchCam.setOnClickListener(new View.OnClickListener() {
					
					public void onClick(View v) {
						logClickToUserDb(ts);
						filename = String.valueOf(System.currentTimeMillis()) ;
						ContentValues values = new ContentValues();
						values.put(Images.Media.TITLE, filename);
						values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
						values.put(Images.Media.MIME_TYPE, "image/jpeg");
						values.put(Images.Media.LATITUDE, LastLocation.lat);
						values.put(Images.Media.LONGITUDE, LastLocation.lng);
						takenPhotoUri = MessageDisplay_Activity.this.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
						cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, takenPhotoUri);
						startActivityForResult(cameraIntent, TAKE_PHOTO);
						
					}
				});
			}
			
			else if (!forcePhoto) {
				Button launchGallery = new Button(this);
				launchGallery.setText("Pick a photo from gallery");
				launchGallery.setTextSize(20);
				launchGallery.setGravity(Gravity.CENTER_HORIZONTAL);
				launchGallery.setLayoutParams(params);
				launchGallery.setPadding(15, 10, 15, 10);
				ll.addView(launchGallery);
				launchGallery.setOnClickListener(new View.OnClickListener() {
					
					public void onClick(View v) {
						logClickToUserDb(ts);
						startActivityForResult(galleryIntent, PICK_IMAGE);
					}
				});
			}
			
		}
		
		if (taskType == DeliveryManager_Service.TASK_PHOTO) {
			Button launchCam = new Button(this);
			launchCam.setText("Take a photo now");
			launchCam.setTextSize(20);
			launchCam.setGravity(Gravity.CENTER_HORIZONTAL);
			launchCam.setLayoutParams(params);
			launchCam.setPadding(15, 10, 15, 10);
			ll.addView(launchCam);
			launchCam.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					logClickToUserDb(ts);
					filename = String.valueOf(System.currentTimeMillis()) ;
					ContentValues values = new ContentValues();
					values.put(Images.Media.TITLE, filename);
					values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
					values.put(Images.Media.MIME_TYPE, "image/jpeg");
					values.put(Images.Media.LATITUDE, LastLocation.lat);
					values.put(Images.Media.LONGITUDE, LastLocation.lng);
					takenPhotoUri = MessageDisplay_Activity.this.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
					cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, takenPhotoUri);
					startActivityForResult (cameraIntent, TAKE_PHOTO);
				}
			});
		}
		
		if (taskType == DeliveryManager_Service.TASK_PICKER) {
			Button goOnline = new Button(this);
			goOnline.setText("Choose Photos");
			goOnline.setTextSize(20);
			goOnline.setGravity(Gravity.CENTER_HORIZONTAL);
			goOnline.setLayoutParams(params);
			goOnline.setPadding(15, 10, 15, 10);
			ll.addView(goOnline);
			goOnline.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					logClickToUserDb(ts);
					startActivity(new Intent(MessageDisplay_Activity.this, OnlineTask.class).putExtra("tsNot", ts));
					
				}
			});
		}
	}
	
	private int determineBackground(int id) {
		int resId = 0;
		
		switch (id) {
			case 1: resId = R.drawable.oblivion_background; break;
			case 2: resId = R.drawable.oblivion_background; break;
			case 3: resId = R.drawable.oblivion_background; break;
			case 4: resId = R.drawable.oblivion_background; break;
			case 5: resId = R.drawable.q_activity_background; break;
			case 6: resId = R.drawable.break_activity_background; break;
			case 7: resId = R.drawable.q_activity_background; break;
			case 8: resId = R.drawable.photo_opp_background; break;
			case 9: resId = R.drawable.break_activity_background; break;
			case 10: resId = R.drawable.photo_opp_background; break;
			case 11: resId = R.drawable.photo_opp_background; break;
			case 12: resId = R.drawable.q_activity_background; break;
			case 13: resId = R.drawable.q_activity_background; break;
			case 14: resId = R.drawable.q_activity_background; break;
			case 15: resId = R.drawable.photo_opp_background; break;
			case 16: resId = R.drawable.photo_opp_background; break;
			case 17: resId = R.drawable.break_activity_background; break;
			case 18: resId = R.drawable.break_activity_background; break;
			case 19: resId = R.drawable.break_activity_background; break;
			case 20: resId = R.drawable.break_activity_background; break;
			case 21: resId = R.drawable.photo_opp_background; break;
			case 22: resId = R.drawable.q_activity_background; break;
			case 23: resId = R.drawable.photo_opp_background; break;
			case 24: resId = R.drawable.break_activity_background; break;
			case 25: resId = R.drawable.q_activity_background; break;
			case 26: resId = R.drawable.break_activity_background; break;
			case 27: resId = R.drawable.break_activity_background; break;
			case 28: resId = R.drawable.photo_opp_background; break;
			case 29: resId = R.drawable.break_activity_background; break;
			case 30: resId = R.drawable.q_activity_background; break;
			case 31: resId = R.drawable.photo_opp_background; break;
			case 33: resId = R.drawable.break_activity_background; break;
			case 34: resId = R.drawable.q_activity_background; break;
			case 36: resId = R.drawable.q_activity_background; break;
			case 37: resId = R.drawable.photo_opp_background; break;
			case 38: resId = R.drawable.photo_opp_background; break;
			case 39: resId = R.drawable.sonic2_background; break;
			case 40: resId = R.drawable.oblivion_background; break;
			case 47: resId = R.drawable.oblivion_background; break;
			case 48: resId = R.drawable.break_activity_background; break;
			case 49: resId = R.drawable.break_activity_background; break;
			case 50: resId = R.drawable.sonic2_background; break;
			case 54: resId = R.drawable.break_activity_background; break;
			case 55: resId = R.drawable.sonic2_background; break;
			case 56: resId = R.drawable.sonic2_background; break;
			case 57: resId = R.drawable.q_activity_background; break;
			case 58: resId = R.drawable.q_activity_background; break;
			case 59: resId = R.drawable.photo_opp_background; break;
			default: resId = R.drawable.default_background; break;
		}
		return resId;

	}
	
	private int determineTaskText(int id) {
		int resId = 0;
		
		switch (id) {
			case 1: resId = R.string.oblivion_seat_photo_text; break;
			case 2: resId = R.string.oblivion_break_text; break;
			case 3: resId = R.string.oblivion_break_text; break;
			case 4: resId = R.string.oblivion_q_text; break;
			case 5: resId = R.string.q_activity_text; break;
			case 6: resId = R.string.break_activity_text; break;
			case 7: resId = R.string.q_activity_text; break;
			case 8: resId = R.string.photo_opp_text; break;
			case 9: resId = R.string.break_activity_text; break;
			case 10: resId = R.string.photo_opp_text; break;
			case 11: resId = R.string.photo_opp_text; break;
			case 12: resId =  R.string.q_activity_text; break;
			case 13: resId =  R.string.q_activity_text; break;
			case 14: resId =  R.string.q_activity_text; break;
			case 15: resId =  R.string.photo_opp_text; break;
			case 16: resId =  R.string.photo_opp_text; break;
			case 17: resId =  R.string.break_activity_text; break;
			case 18: resId =  R.string.break_activity_text; break;
			case 19: resId =  R.string.break_activity_text; break;
			case 20: resId =  R.string.break_activity_text; break;
			case 21: resId =  R.string.photo_opp_text; break;
			case 22: resId =  R.string.q_activity_text; break;
			case 23: resId =  R.string.photo_opp_text; break;
			case 24: resId =  R.string.break_activity_text; break;
			case 25: resId =  R.string.q_activity_text; break;
			case 26: resId =  R.string.break_activity_text; break;
			case 27: resId =  R.string.break_activity_text; break;
			case 28: resId =  R.string.photo_opp_text; break;
			case 29: resId =  R.string.break_activity_text; break;
			case 30: resId =  R.string.q_activity_text; break;
			case 31: resId =  R.string.photo_opp_text; break;
			case 33: resId =  R.string.break_activity_text; break;
			case 34: resId =  R.string.q_activity_text; break;
			case 36: resId =  R.string.q_activity_text; break;
			case 37: resId =  R.string.photo_opp_text; break;
			case 38: resId =  R.string.photo_opp_text; break;
			case 39: resId =  R.string.sonic_exit_text; break;
			case 40: resId =  R.string.oblivion_exit_text; break;
			case 47: resId =  R.string.oblivion_drop_photo_text; break;
			case 48: resId =  R.string.break_activity_text; break;
			case 49: resId =  R.string.break_activity_text; break;
			case 50: resId =  R.string.sonic_break_text; break;
			case 54: resId =  R.string.break_activity_text; break;
			case 55: resId =  R.string.sonic_photo_text; break;
			case 56: resId =  R.string.sonic_q_text; break;
			case 57: resId =  R.string.q_activity_text; break;
			case 58: resId =  R.string.q_activity_text; break;
			case 59: resId =  R.string.photo_opp_text; break;
			default: resId =  R.string.default_text; break;
		}
		return resId;
	}
	
	private void logToUserDb(long ts) {
		userdata = new UserDataSQLHelper(this).getWritableDatabase();
		userdata.execSQL("UPDATE userdata SET notification_accept_ts="+System.currentTimeMillis()+
				" WHERE notification_ts="+ts);
		userdata.close();
	}
	
	private void logClickToUserDb(long ts) {
		userdata = new UserDataSQLHelper(this).getWritableDatabase();
		userdata.execSQL("UPDATE userdata SET task_acceptance_ts="+System.currentTimeMillis()+
				" WHERE notification_ts="+ts);
		userdata.close();
	}
	
	private void logToGpsTriggerHistory() {
		SQLiteDatabase db =  new TriggerHistorySQLHelper(this).getWritableDatabase();
		db.execSQL("UPDATE triggerhistory SET collected="+1+ 
				" WHERE tagId="+id);
		db.close();
		Log.d(TAG, "logged to trigger history, tagId: "+id+", taskT: "+taskType);
	}
	
	private void logCompletionToUserDb(long ts) {
		userdata = new UserDataSQLHelper(this).getWritableDatabase();
		userdata.execSQL("UPDATE userdata SET task_completed_ts="+System.currentTimeMillis()+
				" WHERE notification_ts="+ts);
		userdata.close();
	}
	
		

}
