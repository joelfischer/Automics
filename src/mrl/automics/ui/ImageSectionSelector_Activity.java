package mrl.automics.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import mrl.automics.R;
import mrl.automics.graphics.CropView;
import mrl.automics.graphics.CropView.MyRectF;
import mrl.automics.sensors.CloudPush_Service;
import mrl.automics.sensors.LastLocation;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class ImageSectionSelector_Activity extends Activity {
	
	private static final String TAG = "ImageSectionSelector_Activity";
	private static final float FACTOR = (648f/442f); 
	private static final float LANDSCAPE_RATIO = 4/3f;
	private static final float PORTRAIT_RATIO = 3/4f;
	private static final int LANDSCAPE_LANDSCAPE = 1;  //i.e. screen_photo
	private static final int LANDSCAPE_PORTRAIT = 2;
	private static final int PORTRAIT_LANDSCAPE = 3;
	private static final int PORTRAIT_PORTRAIT = 4;
	private int scalingFactor;
	
	private ProgressDialog progress;
//	private PinchImageView piv;
//	private WebImageView wiv;
	private String selectedImage;
	private Bitmap yourSelectedImage;
	private static final float MAX_SCALE = 10f;
	private FrameLayout fl;
	private ImageView iv;
	private CropView mCrop;
	private Bitmap croppedImage;
	private Bitmap toCrop;
	private String id = "";	//the image id/title as set when downloaded
	private String internalName; //the image title when cropped 
	private Uri oriUri;
	private Bundle timestamps;
	private SQLiteDatabase userdata;
	private long ts;
	private long tsNot;
	private LinearLayout cropContainer;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		
		setContentView(R.layout.image_section);
		
		Button cropBtn = (Button)findViewById(R.id.crop_btn);
		Button cancelBtn = (Button)findViewById(R.id.cancel_btn);
		
		cropBtn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				progress = ProgressDialog.show(ImageSectionSelector_Activity.this, "Cropping...", "Please wait", true);
//				new ToCropTask().execute(Uri.parse(selectedImage));		
//				addBubble = true;
				new CropThread().run();
			}
		});
		
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				fl.removeAllViews();
				ImageSectionSelector_Activity.this.onPause();
				ImageSectionSelector_Activity.this.finish();
			}
		});
	}
	
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		progress = ProgressDialog.show(ImageSectionSelector_Activity.this, "Loading...", "Please wait", true);
		
		iv = (ImageView) findViewById(R.id.selected_image);
		iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
				
		fl = (FrameLayout) findViewById(R.id.frame);
		cropContainer = (LinearLayout) findViewById(R.id.crop_view_container);
//		fl.setPinchView(piv);
		
//		wiv = (WebImageView) findViewById(R.id.pinch_web);
		
		Intent startingIntent = this.getIntent();
		selectedImage = startingIntent.getStringExtra("image");
		ts = startingIntent.getLongExtra("ts", 0);
		tsNot = startingIntent.getLongExtra("tsNot", 0);
		timestamps = new Bundle();
		timestamps.putLong("ts", ts);
		timestamps.putLong("tsNot", tsNot);
		Log.d(TAG, "selectedImage: "+selectedImage);

		new LoadSelectedImageTask().execute(Uri.parse(selectedImage));
	}
	
//	public boolean onCreateOptionsMenu(Menu menu) {
//		super.onCreateOptionsMenu(menu);
//		
//		MenuItem item1 = menu.add(0, 0, 0, "Cancel");
//		MenuItem item2 = menu.add(0, 0, 1, "Crop & finish");
//		MenuItem item3 = menu.add(0, 0, 2, "Crop & add a speech bubble");
//		
//		item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//			
//			public boolean onMenuItemClick(MenuItem item) {
//				//do nothing
//				
//				return false;
//			}
//		});
//		
//		item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//			
//			public boolean onMenuItemClick(MenuItem item) {	
//				progress = ProgressDialog.show(ImageSectionSelector_Activity.this, "Cropping...", "Please wait", true);
////				new ToCropTask().execute(Uri.parse(selectedImage));		
////				addBubble = false;
//				new CropThread().run();
//				return false;
//			}
//		});
//		
//		item3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//			
//			public boolean onMenuItemClick(MenuItem item) {	
//				progress = ProgressDialog.show(ImageSectionSelector_Activity.this, "Cropping...", "Please wait", true);
////				new ToCropTask().execute(Uri.parse(selectedImage));		
////				addBubble = true;
//				new CropThread().run();
//				return false;
//			}
//		});
//		
//		
//		return true;
//	}
	
		
	private class LoadSelectedImageTask extends AsyncTask<Uri, Integer, Integer> {
	     protected Integer doInBackground(Uri... params) {
	    	String[] filePathColumn = {MediaStore.Images.Media.DATA,
	    								MediaStore.Images.Media.TITLE};
	    	
   	 		oriUri = params[0];
	        Cursor cursor = getContentResolver().query(oriUri, filePathColumn, null, null, null);
	        cursor.moveToFirst();

	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	        String filePath = cursor.getString(columnIndex);
	        columnIndex = cursor.getColumnIndex(filePathColumn[1]);
		    id = cursor.getString(columnIndex);
	        cursor.close();
	        
	        Log.d(TAG, "image id: "+id+", filePath: "+filePath);
	        
	        toCrop = BitmapFactory.decodeFile(filePath);
	        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
	        scalingFactor = 1;
	        if (toCrop.getWidth() > 1600 || toCrop.getHeight() > 1600) {
	        	scalingFactor = 4;
	        }
	        if (toCrop.getWidth() > 1000 || toCrop.getHeight() > 1000) {
	        	scalingFactor = 2;
	        }
	        decodeOpts.inSampleSize = scalingFactor;
			
//			decodeOpts.inSampleSize = SCALING_FACTOR;
			//TODO: fix runtime problem with bitmap recycling
			if (yourSelectedImage != null) {
				Log.d(TAG, "bitmap not null");
				if (yourSelectedImage.isRecycled())
					yourSelectedImage = Bitmap.createBitmap(BitmapFactory.decodeFile(filePath, decodeOpts));
			}
			if (yourSelectedImage == null) {
				yourSelectedImage = BitmapFactory.decodeFile(filePath, decodeOpts);
				Log.d(TAG, "bitmap null");
			}
						
			Message msg = new Message();
			msg.obj = yourSelectedImage;
			
			handler.sendMessage(msg);
			
			return RESULT_OK;
	     }

	     protected void onProgressUpdate(Integer... progress) {
//	         setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(Integer... result) {
	    	 //never called?
	    	 Log.d(TAG, "result: "+result[0]+", RESULT_OK: "+RESULT_OK);
	    	 if (result[0] == RESULT_OK && ImageSectionSelector_Activity.this.progress != null) {
//	    		wiv.setImageBitmap(yourSelectedImage);
				ImageSectionSelector_Activity.this.progress.dismiss();
	    	 }
	    	 
	         
	     }
	 }
	
//	private class ToCropTask extends AsyncTask<Uri, Integer, Integer> {
//	     protected Integer doInBackground(Uri... params) {
//	    	String[] filePathColumn = {MediaStore.Images.Media.DATA};
//	    	 
//
//  	 		Uri selectedImage = params[0];
//	        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
//	        cursor.moveToFirst();
//
//	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//	        String filePath = cursor.getString(columnIndex);
//	        cursor.close();
//			
//			toCrop = BitmapFactory.decodeFile(filePath);
//			
//			emptyHandler.sendEmptyMessage(0);
//			
//			return RESULT_OK;
//	     }
//
//	     protected void onProgressUpdate(Integer... progress) {
////	         setProgressPercent(progress[0]);
//	     }
//
//	     protected void onPostExecute(Integer... result) {
//	    	 //never called?
//	    	 Log.d(TAG, "result: "+result[0]+", RESULT_OK: "+RESULT_OK);
//	    	 if (result[0] == RESULT_OK && ImageSectionSelector_Activity.this.progress != null) {
//				ImageSectionSelector_Activity.this.progress.dismiss();
//	    	 }
//	    	 
//	         
//	     }
//	 }
	
	private class CropThread implements Runnable {
		public void run() {
			//we don't need it any more
			yourSelectedImage.recycle();
			yourSelectedImage = null;
			//crop the image
			MyRectF cropRect = mCrop.getCropRect();
			
//			Rect imageBounds = iv.getDrawable().getBounds();
			Rect imageBounds = new Rect(0, 0, toCrop.getWidth(), toCrop.getHeight());
			
			
			float[] imageSpecs = getOnImageOffset(imageBounds);
			float dx = imageSpecs[0];
			float dy = imageSpecs[1];
			float factor = imageSpecs[2];
			
			
			Log.d(TAG, "factor: "+ factor);
			//				
//			Rect srcRect = new Rect( (int)((cropRect.left-dx)*factor),  (int)((cropRect.top-dy)*factor), (int)((cropRect.right-dx)*factor), (int)((cropRect.bottom-dy)*factor) );
			Rect srcRect = new Rect( (int)((cropRect.left)*factor),  (int)((cropRect.top)*factor), (int)((cropRect.right)*factor), (int)((cropRect.bottom)*factor) );

			
			Log.d(TAG, "srcRect.w: "+srcRect.width());
						
			if (srcRect.width() >= 1600) {
				//cropRect is too large, leads to VM error, do some scaling before creating new bitmap 
				//half the size is good enough, no loss as we scale down to 800 later anyway..
				croppedImage = Bitmap.createBitmap(srcRect.width()/2, srcRect.height()/2, Bitmap.Config.RGB_565);
				toCrop = Bitmap.createScaledBitmap(toCrop, toCrop.getWidth()/2, toCrop.getHeight()/2, true);
				
				//we need the new offsets to get the right size for the rect we want to crop from the (now scaled down by /2) ori bitmap
				imageSpecs = getOnImageOffset(new Rect(0,0, toCrop.getWidth(), toCrop.getHeight()));
				dx = imageSpecs[0];
				dy = imageSpecs[1];
				factor = imageSpecs[2];
				
//				srcRect = new Rect( (int)((cropRect.left-dx)*factor),  (int)((cropRect.top-dy)*factor), (int)((cropRect.right-dx)*factor), (int)((cropRect.bottom-dy)*factor) );
				srcRect = new Rect( (int)((cropRect.left)*factor),  (int)((cropRect.top)*factor), (int)((cropRect.right)*factor), (int)((cropRect.bottom)*factor) );
				
				Canvas canvas = new Canvas(croppedImage);
				Rect dstRect = new Rect(0,0, srcRect.width(), srcRect.height());
				
				canvas.drawBitmap(toCrop, srcRect, dstRect, null);
//				canvas.drawBitmap(croppedImage, null, new Rect(0,0,800,600), null);
			}
			
			else if (srcRect.width() < 1600) {
				croppedImage = Bitmap.createBitmap(srcRect.width(), srcRect.height(), Bitmap.Config.RGB_565);
				Canvas canvas = new Canvas(croppedImage);
				Rect dstRect = new Rect(0,0, srcRect.width(), srcRect.height());
				
				canvas.drawBitmap(toCrop, srcRect, dstRect, null);
				
			}
			
			//scaling diminishes the image quality, no work around so far though.
			Bitmap scaledImage = Bitmap.createScaledBitmap(croppedImage, 800, 600, false);
			cropContainer.removeView(mCrop);
			iv.setImageBitmap(scaledImage);
			
			//save to SD card...
			internalName = String.valueOf(System.currentTimeMillis());
			File sdCard = Environment.getExternalStorageDirectory();
			ContentValues values = new ContentValues();

			File store = new File(sdCard.getAbsolutePath()+"/DITP/Cropped");
			if (!store.isDirectory())
			{
				store.mkdir();
				Log.d(TAG, "just made dir");
			}
			String path = store.getAbsolutePath().toLowerCase();
			String name = store.getName().toLowerCase();
			values.put(Images.Media.TITLE, internalName);
			values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
			values.put(Images.Media.MIME_TYPE, "image/jpeg");
			values.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
			values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
			values.put("_data", path+"/"+internalName+".jpg");
			values.put(Images.Media.LATITUDE, LastLocation.lat);
			values.put(Images.Media.LONGITUDE, LastLocation.lng);
		
			
//			if (!addBubble) {
//				File store = new File(sdCard.getAbsolutePath()+"/DITP/Shared");
//				if (!store.isDirectory())
//				{
//					store.mkdir();
//					Log.d(TAG, "just made dir");
//				}
//				String path = store.getAbsolutePath().toLowerCase();
//				String name = store.getName().toLowerCase();
//				values.put(Images.Media.TITLE, internalName);
//				values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
//				values.put(Images.Media.MIME_TYPE, "image/jpeg");
//				values.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
//				values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
//				values.put("_data", path+"/"+internalName+".jpg");
//			}

			Uri uri = ImageSectionSelector_Activity.this.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
			try {
			  OutputStream outStream = ImageSectionSelector_Activity.this.getContentResolver().openOutputStream(uri);
//			  returnedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
//			  Bitmap scaledImage = Bitmap.createScaledBitmap(croppedImage, 589, 442, false);
//			  croppedImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
			  scaledImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
			  outStream.flush();
			  outStream.close();
			  Log.d(TAG,"done exporting to SD");
			} catch (FileNotFoundException e) {
			   Log.e(TAG, e.toString());
			} catch (IOException e) {
			  e.printStackTrace();
			}
			
//			 long endTime = System.currentTimeMillis() + 10*1000;
//	            while (System.currentTimeMillis() < endTime) {
//	            	//show the cropped/scaled image for 1s
//	            }
	     
			Intent startNext = new Intent(ImageSectionSelector_Activity.this, ChosenImage_Activity.class);
        	startNext.putExtra("selected", uri.toString());
        	Log.d(TAG, "selected: "+uri.toString() +" internalName: "+internalName);
        	startNext.putExtra("oriTitle", id);
        	startNext.putExtra("croppedTitle", internalName);
        	startNext.putExtra("timestamps", timestamps);
        	startNext.putExtra("img_type", "cropped");
        	startActivity(startNext);
			
//			else if (!addBubble) {
//				Intent cloudPush = new Intent(ImageSectionSelector_Activity.this, CloudPush_Service.class);
//				cloudPush.putExtra("uri", uri.toString());
//				cloudPush.putExtra("img_type", "cropped");
//				cloudPush.putExtra("oriTitle", id);
//				cloudPush.putExtras(timestamps);
//				startService(cloudPush);
//				logToUserDb();
//				if (timestamps.getLong("tsNot", 0) > 0) {
//					//we know this is issued by the system...
//					startActivity(new Intent(ImageSectionSelector_Activity.this, MultChoice_Activity.class).putExtra("ts",timestamps.getLong("tsNot")));
//				}
//			}
	        ImageSectionSelector_Activity.this.finish();
	        	
			
			
		}
	}

	private float[] getOnImageOffset(Rect imageBounds) {
		float[] imageSpecs = new float[3];
		
		float factor = 0;
		float dy = 0;
		float dx = 0;
		
		//find out if screen and photo are in portrait or landscape orientation:
		if (iv.getRight() > iv.getBottom()) {
			//screen in landscape orientation
			if (imageBounds.height() > imageBounds.width() ) {
				//image is portrait, thus
				float scaleY = (imageBounds.height()-iv.getBottom()) * PORTRAIT_RATIO;
				float actualWidth = imageBounds.width() - scaleY;
				
				Log.d(TAG, "adjusted for portrait photo in landscape screen orientation");
				dy = 0;
				dx = (iv.getRight() - actualWidth) /2f;
				
			}
			else if (imageBounds.height() < imageBounds.width() ) {
				//image is landscape
				float scaleY = (imageBounds.height()-iv.getBottom()) * LANDSCAPE_RATIO;
				float actualWidth = imageBounds.width() - scaleY;
				
				Log.d(TAG, "adjusted for landscape photo in landscape screen orientation");
				dy = 0;
				dx = (iv.getRight() - actualWidth) /2f;
			}
			//need to adjust rect back to coordinate system that our original bitmap uses (e.g. 648x486).
			factor = (float)imageBounds.height()/(float)iv.getBottom(); 
		}
		else if (iv.getRight() < iv.getBottom()) {
			//screen in portrait orientation
			if (imageBounds.height() > imageBounds.width() ) {
				//image is portrait
				float scaleX = (imageBounds.width()-iv.getRight()) * LANDSCAPE_RATIO;
				float actualHeight = imageBounds.height() - scaleX;
				
				Log.d(TAG, "adjusted for portrait photo in portrait screen orientation");
				//debug
//				Log.d(TAG, "actual h: "+actualHeight);
				dy = (iv.getBottom() - actualHeight) /2f;
				dx = 0;
			}
			else if (imageBounds.height() < imageBounds.width() ) {
				//image is landscape
				float scaleX = (imageBounds.width()-iv.getRight()) * PORTRAIT_RATIO;
				float actualHeight = imageBounds.height() - scaleX;
				
				Log.d(TAG, "adjusted for landscape photo in portrait screen orientation");
				//debug
//				Log.d(TAG, "actual h: "+actualHeight);
				dy = (iv.getBottom() - actualHeight) /2f;
				dx = 0;
			}
			//need to adjust rect back to coordinate system that our original bitmap uses (e.g. 648x486).
			factor = (float)imageBounds.width()/(float)iv.getWidth(); 
		}
		
		imageSpecs[0] = dx;
		imageSpecs[1] = dy;
		imageSpecs[2] = factor;
		
		return imageSpecs;
	}

	
	
	private class AddCropViewThread implements Runnable {
		public void run() {
			//now the crop view
			DisplayMetrics metrics = new DisplayMetrics();
	        getWindowManager().getDefaultDisplay().getMetrics(metrics);
	        
			Log.d(TAG, "ImageView w: "+iv.getWidth() +", h: "+iv.getHeight());
	        
	        int ivHeight = iv.getBottom();
	        int ivWidth = iv.getRight();
			int width = yourSelectedImage.getWidth();
            int height = yourSelectedImage.getHeight();
            int type = 0;
            int originalWidth = width * scalingFactor;
            if (iv.getRight() > iv.getBottom()) {
				//screen in landscape orientation
            	if (height > width ) {
					//image is portrait, thus
					type = LANDSCAPE_PORTRAIT;
				}
				else if (height < width ) {
					//image is landscape
					type = LANDSCAPE_LANDSCAPE;
				}
            }
			else if (iv.getRight() < iv.getBottom()) {
				//screen in portrait orientation
				if (height >width ) {
					//image is portrait
					type = PORTRAIT_PORTRAIT;
				}
				else if(height < width ) {
					//image is landscape 
					type = PORTRAIT_LANDSCAPE;
				}
			}
			
            //only crop if bitmap width  > 800, that's our minimum...
            if (originalWidth > 800) {
	            mCrop = new CropView(ImageSectionSelector_Activity.this, width, height, type, metrics, originalWidth, ivHeight, ivWidth);
	            LayoutParams params = new LayoutParams((int)mCrop.onScreenWidth, (int)mCrop.onScreenHeight);
	            mCrop.setLayoutParams(params);
	            cropContainer.addView(mCrop);
            }
            else {
            	Intent startNext = new Intent(ImageSectionSelector_Activity.this, ChosenImage_Activity.class);
	        	startNext.putExtra("selected", oriUri.toString());
	        	startNext.putExtra("img_type", "raw");
	        	startNext.putExtra("oriTitle", id);
	        	startNext.putExtra("croppedTitle", "");
	        	startNext.putExtra("timestamps", timestamps);
	        	startActivity(startNext);
            	ImageSectionSelector_Activity.this.finish();
            }
		}
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//set the image 
			yourSelectedImage = (Bitmap) msg.obj;
			iv.setImageBitmap(yourSelectedImage);
			//create the crop overlay
			new AddCropViewThread().run();
			if (progress.isShowing())
				progress.dismiss();
			
			Log.d(TAG, "cancelled dialog");
		}
	};
	
//	private Handler emptyHandler = new Handler() {
//		@Override
//		public void handleMessage(Message msg) {
//			if (progress.isShowing())
//				progress.dismiss();
//			new CropThread().run();
//			Log.d(TAG, "cancelled dialog");
//		}
//	};
	
	protected void onDestroy() {
		
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
		iv.destroyDrawingCache();
//		this.finish();
//		iv.setImageURI(null);
//		iv = null;	
		if (progress!=null || progress.isShowing())
			progress.dismiss();
		

	}
	
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause()");
		if (croppedImage != null) {
			croppedImage.recycle();
			croppedImage = null;
		}
		if (toCrop != null) {
			toCrop.recycle();
			toCrop = null;
		}
		if (yourSelectedImage != null) {
			yourSelectedImage.recycle();
			yourSelectedImage = null;
		}
//		if (handler != null)
//			handler = null;
	}
	
	private void logToUserDb() {
	
		Log.d(TAG, "nTs: "+tsNot+", ts: "+ts);
		if (tsNot == 0) {
			//we know this is not a task by the system...
			userdata = new UserDataSQLHelper(this).getWritableDatabase();
			userdata.execSQL("UPDATE userdata SET task_completed_ts="+System.currentTimeMillis()+
					" WHERE task_acceptance_ts="+ts);
			userdata.close();
		}
		else if (tsNot > 0) {
			userdata = new UserDataSQLHelper(this).getWritableDatabase();
			userdata.execSQL("UPDATE userdata SET task_completed_ts="+System.currentTimeMillis()+
					" WHERE notification_ts="+tsNot);
			userdata.close();
		}
	}
	
	
	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.d(TAG, "hasFocus: "+hasFocus);
	}
	

}
