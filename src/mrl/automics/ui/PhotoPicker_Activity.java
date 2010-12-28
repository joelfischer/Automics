package mrl.automics.ui;

import mrl.automics.R;
import java.io.File;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class PhotoPicker_Activity extends Activity {
	
	private static final String TAG = "PhotoPicker_Activity";
	private static final int PICK_IMAGE = 1;
	private static final int TAKE_PHOTO = 2;
	private Intent galleryIntent;
	private Uri takenPhotoUri;


	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
//		galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
		galleryIntent.setType("image/*");
		
		Button btn = (Button) findViewById(R.id.btn_launch_gallery);
		btn.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				
				Log.d(TAG, "here");
				startActivityForResult(galleryIntent, PICK_IMAGE);
				
			}
			
		}); 
		
		Button cam = (Button) findViewById(R.id.btn_launch_camera);
		cam.setOnClickListener(new OnClickListener() {
			
			public void onClick(View v) {
				
				Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				//TODO: think about image naming and storage dir
				String filename = String.valueOf(System.currentTimeMillis()) ;
				ContentValues values = new ContentValues();
				values.put(Images.Media.TITLE, filename);
				values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
				values.put(Images.Media.MIME_TYPE, "image/jpeg");

				takenPhotoUri = getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
//				File toBeTaken = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis()+"");
//				takenPhotoUri = Uri.fromFile(toBeTaken);
				camIntent.putExtra(MediaStore.EXTRA_OUTPUT, takenPhotoUri);
				
				startActivityForResult (camIntent, TAKE_PHOTO);
				
			}
		});
		
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

	    switch(requestCode) { 
	    case PICK_IMAGE:
	        if(resultCode == RESULT_OK){  
	            Uri selectedImage = imageReturnedIntent.getData();
	            
//	            String[] filePathColumn = {MediaStore.Images.Media.DATA};
//
//	            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
//	            cursor.moveToFirst();
//
//	            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//	            String filePath = cursor.getString(columnIndex);
//	            cursor.close();
//
//	            Bitmap yourSelectedImage = BitmapFactory.decodeFile(filePath);
//	            
//	            Log.d(TAG, "Image: "+yourSelectedImage.toString());
	            
	            
//	            startActivity(new Intent(PhotoPicker_Activity.this, ChosenImage_Activity.class).setData(selectedImage));
	            
	            //change to this..
//	            startActivity(new Intent(PhotoPicker_Activity.this, BubbleEditor.class).putExtra("selected", selectedImage.toString()));
	            
	            startActivity(new Intent(PhotoPicker_Activity.this, ImageSectionSelector_Activity.class).putExtra("image", selectedImage.toString()));
	        }
	        break;
	    case TAKE_PHOTO:
	    	if (resultCode == RESULT_OK) {
//	    		startActivity(new Intent(PhotoPicker_Activity.this, BubbleEditor.class).putExtra("selected", takenPhotoUri.toString()));
	    		startActivity(new Intent(PhotoPicker_Activity.this, ImageSectionSelector_Activity.class).putExtra("image", takenPhotoUri.toString()));
	    	}
	    	break;
	    }
	}

}
