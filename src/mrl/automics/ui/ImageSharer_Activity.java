package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.graphics.CropView;
import mrl.automics.sensors.CloudPush_Service;
import mrl.automics.sensors.MessageDelivery_Service;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

public class ImageSharer_Activity extends Activity {
	
	private static final String TAG = "ImageSharer_Activity";
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
	private MyFrameLayout fl;
	private ImageView iv;
	private CropView mCrop;
	private Bitmap croppedImage;
	private String id = "";	//the image id/title as set when downloaded
	private String internalName; //the image title when cropped 
	private Uri oriUri;
	private boolean justShare;
	private long tsNotification;
	private SQLiteDatabase userdata;
	private long ts;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate()");
		
		setContentView(R.layout.image_sharer);
	}
	
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		progress = ProgressDialog.show(ImageSharer_Activity.this, "Loading...", "Please wait", true);
		
		iv = (ImageView) findViewById(R.id.selected_image);
		iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
				
		fl = (MyFrameLayout) findViewById(R.id.frame);
		
		Intent startingIntent = this.getIntent();
		selectedImage = startingIntent.getStringExtra("image");
		justShare = startingIntent.getBooleanExtra("justShare", false);
		ts = startingIntent.getLongExtra("ts", 0);

		tsNotification = startingIntent.getLongExtra("tagId", 0);
		
		Log.d(TAG, "tsNotific: "+tsNotification +", ts: "+ts);
		Log.d(TAG, "selectedImage: "+selectedImage);

		new LoadSelectedImageTask().execute(Uri.parse(selectedImage));
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		
		MenuItem item1 = null; 
		if (justShare)
			item1 = menu.add(0, 0, 0, "Share & finish");
		MenuItem item2 = null;
		if (!justShare)
			item2 = menu.add(0, 0, 1, "Share & use for photo story");
		
		if (justShare) {
			item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				
				public boolean onMenuItemClick(MenuItem item) {
	//				new RunServiceThread().run();
//					progress = ProgressDialog.show(ImageSharer_Activity.this, "Sharing...", "Please wait", true);
					new RunServiceTask().execute();
					logToUserDb();
					if (tsNotification > 0) {
						//we know this is issued by the system...
						//if this is a photo task this task is finished here, write to db
						startActivity(new Intent(ImageSharer_Activity.this, MultChoice_Activity.class).putExtra("ts",tsNotification));
					}
					ImageSharer_Activity.this.finish();
					return false;
				}
			});
		}
			
		if (!justShare) {
			item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
				
				public boolean onMenuItemClick(MenuItem item) {						
	//				new RunServiceThread().run();
//					progress = ProgressDialog.show(ImageSharer_Activity.this, "Sharing...", "Please wait", true);
					Intent startNext = new Intent(ImageSharer_Activity.this, ImageSectionSelector_Activity.class);
		        	startNext.putExtra("image", selectedImage);
		        	Bundle b = new Bundle();
		        	b.putLong("ts", ts);
		        	b.putLong("tsNot", tsNotification);
		        	startNext.putExtras(b);
		        	startActivity(startNext);
					new RunServiceTask().execute();
					ImageSharer_Activity.this.finish();
					return false;
				}
			});
		}	
		
		return true;
	}
	
	private class RunServiceThread implements Runnable {
		public void run() {
			progress = ProgressDialog.show(ImageSharer_Activity.this, "Sharing...", "Please wait", true);
		}
	}
	
	private class RunServiceTask extends AsyncTask<Void, Integer, Integer> {
		
		@Override
		protected Integer doInBackground(Void... params) {
			Intent cloudPush = new Intent(ImageSharer_Activity.this, CloudPush_Service.class);
			cloudPush.putExtra("uri", selectedImage);
			cloudPush.putExtra("img_type", "raw");
			cloudPush.putExtra("oriTitle", id);
			startService(cloudPush);
			Message msg = new Message();
			msg.arg1 = 1;
			handler.sendMessage(msg);
			
			return RESULT_OK;
		}
	}
	
		
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
	        
	        BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
	        scalingFactor = 2;
	        decodeOpts.inSampleSize = scalingFactor;
	        yourSelectedImage = BitmapFactory.decodeFile(filePath, decodeOpts);
			
	        if (yourSelectedImage.getWidth() < 800 || yourSelectedImage.getHeight() < 800) {
	        	scalingFactor = 1;
	        	yourSelectedImage = BitmapFactory.decodeFile(filePath, decodeOpts);
	        }
	        
////			decodeOpts.inSampleSize = SCALING_FACTOR;
//			//TODO: fix runtime problem with bitmap recycling
//			if (yourSelectedImage != null) {
//				Log.d(TAG, "bitmap not null");
//				if (yourSelectedImage.isRecycled())
//					yourSelectedImage = Bitmap.createBitmap(BitmapFactory.decodeFile(filePath, decodeOpts));
//			}
//			if (yourSelectedImage == null) {
//				yourSelectedImage = BitmapFactory.decodeFile(filePath, decodeOpts);
//				Log.d(TAG, "bitmap null");
//			}
						
			Message msg = new Message();
			msg.arg1 = 0;
			handler.sendMessage(msg);
			
			return RESULT_OK;
	     }

	     protected void onProgressUpdate(Integer... progress) {
//	         setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(Integer... result) {
	    	 //never called?
	    	 Log.d(TAG, "result: "+result[0]+", RESULT_OK: "+RESULT_OK);
	    	 if (result[0] == RESULT_OK && ImageSharer_Activity.this.progress != null) {
//	    		wiv.setImageBitmap(yourSelectedImage);
				ImageSharer_Activity.this.progress.dismiss();
	    	 }
	    	 
	         
	     }
	 }
	

	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			//set the image 
//			yourSelectedImage = (Bitmap) msg.obj;
			if (msg.arg1 == 0) {
				iv.setImageBitmap(yourSelectedImage);
				if (progress.isShowing())
					progress.dismiss();
			}
			if (msg.arg1==1) {
				if (progress.isShowing())
					progress.dismiss();
			}
			Log.d(TAG, "cancelled dialog");
		}
		
		
	};
	

	
	protected void onDestroy() {
		
		Log.d(TAG, "onDestroy()");
		super.onDestroy();
		iv.destroyDrawingCache();
		if (yourSelectedImage != null) {
			yourSelectedImage.recycle();
			yourSelectedImage = null;
		}
	}
	
	public void onPause() {
		super.onPause();
		Log.d(TAG, "onPause()");

		if (yourSelectedImage != null) {
			yourSelectedImage.recycle();
			yourSelectedImage = null;
		}
		
	}
	
	private void logToUserDb() {
		if (tsNotification == 0) {
			//we know this is not a task by the system...
			userdata = new UserDataSQLHelper(this).getWritableDatabase();
			userdata.execSQL("UPDATE userdata SET task_completed_ts="+System.currentTimeMillis()+
					" WHERE task_acceptance_ts="+ts);
			userdata.close();
		}
		else if (tsNotification > 0) {
			userdata = new UserDataSQLHelper(this).getWritableDatabase();
			userdata.execSQL("UPDATE userdata SET task_completed_ts="+System.currentTimeMillis()+
					" WHERE notification_ts="+tsNotification);
			userdata.close();
		}
	}
	
	@Override
	public void onWindowFocusChanged (boolean hasFocus) {
		Log.d(TAG, "hasFocus: "+hasFocus);
	}
	

}
