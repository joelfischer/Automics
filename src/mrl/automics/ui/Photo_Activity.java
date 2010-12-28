package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.sensors.CloudPull_Service;
import mrl.automics.sensors.LastLocation;
import mrl.automics.sensors.MessageDelivery_Service;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
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

public class Photo_Activity extends Activity {
	
	private static final String TAG = "Photo_Activity";
	private static final int TAKE_PHOTO = 2;
	private static final int PICK_PHOTO = 1;
	private final static String UPLOAD = "mrl.automics.sensors.UPLOAD";

	private Uri takenPhotoUri;
	private Intent cameraIntent;
	private Intent galleryIntent;
	private SQLiteDatabase userdata;
	private long ts;
	private String filename;
	
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		//TODO
		setContentView(R.layout.messageview);
		
		LinearLayout ll = (LinearLayout)findViewById(R.id.message_layout);
		ll.setGravity(Gravity.CENTER_HORIZONTAL);
		TextView tv = (TextView)findViewById(R.id.message_txt);
		
		ts = System.currentTimeMillis();
		logToUserDb(ts);
		
		ll.setBackgroundResource(R.drawable.photo_opp_background);
		tv.setText(R.string.photo_task_text);
		tv.setTextSize(20);
		tv.setTextColor(Color.WHITE);
		tv.setPadding(5, 10, 5, 10);
		tv.setGravity(Gravity.CENTER_HORIZONTAL);
		
		//the intent to launch the gallery
		galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryIntent.setType("image/*");
		
		//the intent to launch the camera and setup for file storage
		cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		
		LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				
		Button launchCam = new Button(this);
			launchCam.setText("Take a photo now");
			launchCam.setTextSize(20);
			launchCam.setGravity(Gravity.CENTER_HORIZONTAL);
			launchCam.setLayoutParams(params);
			ll.addView(launchCam);
			launchCam.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					prepareAndLaunch();
				}
			});
	}
	
	public void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy()");
	}
	
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent()");
	}
	
	private void prepareAndLaunch() {
		//save to SD card...
		ContentValues values = new ContentValues();
		filename = String.valueOf(System.currentTimeMillis()) ;
		values.put(Images.Media.TITLE, filename);
		values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
		values.put(Images.Media.MIME_TYPE, "image/jpeg");
		values.put(Images.Media.LATITUDE, LastLocation.lat);
		values.put(Images.Media.LONGITUDE, LastLocation.lng);
		takenPhotoUri = Photo_Activity.this.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, takenPhotoUri);
		startActivityForResult (cameraIntent, Photo_Activity.TAKE_PHOTO);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

	    switch(requestCode) { 
	    case TAKE_PHOTO:
	    	if (resultCode == RESULT_OK) {
	    		
//	    		Log.d(TAG, "imageReturnedIntent: "+imageReturnedIntent.getDataString());
	    		Log.d(TAG, "takenPhotoUri: "+ Photo_Activity.this.takenPhotoUri.toString());
//    			startActivityForResult(galleryIntent, PICK_PHOTO);
	    		//see if we need to download anything first...
	    		startService(new Intent(Photo_Activity.this, CloudPull_Service.class));
	    		//then upload the new image
	    		Intent uploadInt = new Intent(UPLOAD);
	    		uploadInt.putExtra("uri", Photo_Activity.this.takenPhotoUri.toString());
	    		uploadInt.putExtra("img_type", "raw");
	    		uploadInt.putExtra("oriTitle", filename);
	    		Photo_Activity.this.sendBroadcast(uploadInt);
	    		Photo_Activity.this.finish();
	    	}
	    	break;
	    case PICK_PHOTO: 
	    	if (resultCode == RESULT_OK) {
	    		Uri selectedImage = imageReturnedIntent.getData();   
	    		Intent photoSharer = new Intent(Photo_Activity.this, ImageSharer_Activity.class);
	    		photoSharer.putExtra("justShare", true);
	    		photoSharer.putExtra("ts", ts);
	            startActivity(photoSharer.putExtra("image", selectedImage.toString()));
	            this.finish();
	    	}
	    	break;
	    }
	}
	
	private void logToUserDb(long timestamp) {
		userdata = new UserDataSQLHelper(this).getWritableDatabase();
		userdata.execSQL("INSERT INTO userdata (task_type,task_id,init_type,task_acceptance_ts) " +
				"VALUES (" +
				"'photo',"+
				MessageDelivery_Service.TASK_PHOTO +","+
				"'user'," +
				timestamp +
				")"
				);
		userdata.close();
	}
}
