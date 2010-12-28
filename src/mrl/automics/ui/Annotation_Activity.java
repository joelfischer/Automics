package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.sensors.MessageDelivery_Service;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
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

public class Annotation_Activity extends Activity {
	
	private static final String TAG = "Annotation_Activity";
	private static final int PICK_IMAGE = 1;
	private static final int TAKE_PHOTO = 2;
	private Uri takenPhotoUri;
	private Intent cameraIntent;
	private Intent galleryIntent;
	private SQLiteDatabase userdata;
	private long ts;
	
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		//TODO
		setContentView(R.layout.messageview);
		
		ts = System.currentTimeMillis();
		logToUserDb(ts);
		
		LinearLayout ll = (LinearLayout)findViewById(R.id.message_layout);
		ll.setGravity(Gravity.CENTER_HORIZONTAL);
		TextView tv = (TextView)findViewById(R.id.message_txt);
		
		ll.setBackgroundResource(R.drawable.q_activity_background);
		tv.setText(R.string.annotation_task_text);
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
		
//		Button launchCam = new Button(this);
//		launchCam.setText("Take a photo now");
//		launchCam.setTextSize(20);
//		launchCam.setGravity(Gravity.CENTER_HORIZONTAL);
//		launchCam.setLayoutParams(params);
//		ll.addView(launchCam);
//		launchCam.setOnClickListener(new View.OnClickListener() {
//			
//			public void onClick(View v) {
//				startActivityForResult (cameraIntent, TAKE_PHOTO);
//				
//			}
//		});
			
		Button launchGallery = new Button(this);
		launchGallery.setText("Pick a photo from gallery");
		launchGallery.setTextSize(20);
		launchGallery.setGravity(Gravity.CENTER_HORIZONTAL);
		launchGallery.setLayoutParams(params);
		ll.addView(launchGallery);
		launchGallery.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				startActivityForResult(galleryIntent, PICK_IMAGE);
			}
		});
	}
	
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent()");
		super.onNewIntent(intent);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

	    switch(requestCode) { 
	    case PICK_IMAGE:
	        if(resultCode == RESULT_OK){  
	            Uri selectedImage = imageReturnedIntent.getData();
	            Intent imageCropper = new Intent(Annotation_Activity.this, ImageSectionSelector_Activity.class);
	            imageCropper.putExtra("ts", ts);
	            startActivity(imageCropper.putExtra("image", selectedImage.toString()));
	            this.finish();
	        }
	        break;
	    case TAKE_PHOTO:
	    	if (resultCode == RESULT_OK) {
//	    		String filename = String.valueOf(System.currentTimeMillis()) ;
//	    		ContentValues values = new ContentValues();
//	    		values.put(Images.Media.TITLE, filename);
//	    		values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
//	    		values.put(Images.Media.MIME_TYPE, "image/jpeg");
//	    		takenPhotoUri = getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
//	    		cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, takenPhotoUri);
//	    		
//	    		startActivityForResult(galleryIntent, PICK_IMAGE);
	    	}
	    	break;
	    }
	}	
	
	private void logToUserDb(long timestamp) {
		userdata = new UserDataSQLHelper(this).getWritableDatabase();
		userdata.execSQL("INSERT INTO userdata (task_type,task_id,init_type,task_acceptance_ts) " +
				"VALUES (" +
				"'annotate',"+
				MessageDelivery_Service.TASK_ANNOTATE +","+
				"'user'," +
				timestamp +
				")"
				);
		userdata.close();
	}

}
