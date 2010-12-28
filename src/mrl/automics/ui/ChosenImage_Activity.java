package mrl.automics.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import mrl.automics.R;
import mrl.automics.graphics.BubbleView;
import mrl.automics.graphics.ImageFrame;
import mrl.automics.graphics.MyRect;
import mrl.automics.graphics.MyThoughtBubbles;
import mrl.automics.graphics.MyTriangle;
import mrl.automics.sensors.CloudPull_Service;
import mrl.automics.sensors.CloudPush_Service;
import mrl.automics.sensors.LastLocation;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class ChosenImage_Activity extends Activity {
	
	private final static String TAG = "ChosenImage_Activity";
	private final static String MMS_GATEWAY = "07725202020";
	private final static int MMS_SENT = 1;
	private final static String UPLOAD = "mrl.automics.sensors.UPLOAD";
	private ImageView iv;
	private Bitmap yourSelectedImage;
	private ProgressDialog progress;
	private BubbleView bubbleView;
	private boolean addMore;
	private String selectedImage;
//	private PowerManager pm;
//	private WakeLock wl;
	private ArrayList<String> bubbleTexts;
	private ArrayList<Integer> bubbleTypes;
	private ImageFrame frame;
	private Bitmap imageWithBubbles;
	private String oriTitle;
	private String croppedTitle;
	private String bubblisedTitle;
	private boolean annotated;
	private Bundle timestamps;
	private SQLiteDatabase userdata;
	private LinearLayout bubbleViewContainer;
	private String imageType;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(TAG, "onCreate()");

		setContentView(R.layout.chosen_image);
		
		progress = ProgressDialog.show(ChosenImage_Activity.this, "Loading...", "Please wait", true);
		
		iv = (ImageView) findViewById(R.id.chosen_image);
		iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
      		
		bubbleViewContainer = (LinearLayout) findViewById (R.id.bubble_view_container);
		
		Button addBtn = (Button) findViewById(R.id.add_btn);
		Button saveBtn = (Button) findViewById(R.id.save_btn);
		Button cancelBtn = (Button) findViewById(R.id.cancel_btn);
		
		addBtn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				addMore = true;
				Intent intent = new Intent(ChosenImage_Activity.this, BubbleEditor.class);
				intent.putExtra("more", true);
				intent.putExtra("selected", selectedImage);
				intent.putExtra("croppedTitle", croppedTitle);
				intent.putExtra("oriTitle", oriTitle);
				intent.putExtra("img_type", imageType);
				
				if (bubbleTexts!=null) {
					Log.d(TAG, "bubbleTexts.size:" +bubbleTexts.size());
					intent.putStringArrayListExtra("texts", ChosenImage_Activity.this.bubbleTexts);
					intent.putIntegerArrayListExtra("types", ChosenImage_Activity.this.bubbleTypes);
				}
				intent.putExtras(timestamps);
				//to make each intent unique
				intent.putExtra(""+System.currentTimeMillis(),System.currentTimeMillis());
				
				startActivity(intent);		
				
				ChosenImage_Activity.this.finish();
			}
		});
		
		saveBtn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {			
				new WorkerThread().run();	
			}
		});
		
		cancelBtn.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				ChosenImage_Activity.this.finish();				
			}
		});

	}
	
	
	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		super.onCreateOptionsMenu(menu);
//		
//		MenuItem item1 = menu.add(0, 0, 0, "add more bubbles");
//		MenuItem item2 = menu.add(0, 0, 0, "save & finish");
////		MenuItem item3 = menu.add(1, 0, 0, "cloud pull");
//		
//		item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//			
//			public boolean onMenuItemClick(MenuItem item) {
//				addMore = true;
//				Intent intent = new Intent(ChosenImage_Activity.this, BubbleEditor.class);
//				intent.putExtra("more", true);
//				intent.putExtra("selected", selectedImage);
//				intent.putExtra("croppedTitle", croppedTitle);
//				intent.putExtra("oriTitle", oriTitle);
//				
//				intent.putStringArrayListExtra("texts", bubbleTexts);
//				intent.putIntegerArrayListExtra("types", bubbleTypes);
//				intent.putExtras(timestamps);
//
//				startActivity(intent);				
//				
//				return false;
//			}
//		});
//		
//		item2.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//			
//			public boolean onMenuItemClick(MenuItem item) {
//				
//				new WorkerThread().run();
////				
//				return true;
//			}
//		});
//		
////		item3.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
////			
////			public boolean onMenuItemClick(MenuItem item) {
////				
////				new PullThread().run();
////				
////				return false;
////			}
////		});
//		
//		return true;
//	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) { 
	    super.onActivityResult(requestCode, resultCode, imageReturnedIntent); 

	    switch(requestCode) { 
	    case MMS_SENT:
	        if(resultCode == RESULT_OK){  
	        	Log.d(TAG, "RESULT_OK");
	        }
	    }
	}
	
	// And to convert the image URI to the direct file system path of the image file  
//	public String getRealPathFromURI(Uri contentUri) {  
//	  
//	  // can post image  
//	    String [] proj={MediaStore.Images.Media.DATA};  
//	    Cursor cursor = managedQuery( contentUri,  
//	            proj, 		// Which columns to return  
//	            null,       // WHERE clause; which rows to return (all rows)  
//	            null,       // WHERE clause selection arguments (none)  
//	            null); // Order-by clause (ascending by name)  
//	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);  
//	    cursor.moveToFirst();  
//	  
//	    return cursor.getString(column_index);  
//	}  
	        
	
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");

		Intent startingIntent = this.getIntent();
		selectedImage = startingIntent.getStringExtra("selected");
		Log.d(TAG, "selectedImage: "+selectedImage);
		oriTitle = startingIntent.getStringExtra("oriTitle");
		croppedTitle = startingIntent.getStringExtra("croppedTitle");
		annotated = startingIntent.getBooleanExtra("annotated", false);
		timestamps = startingIntent.getBundleExtra("timestamps");
		imageType = startingIntent.getStringExtra("img_type");
		Log.d(TAG, "annotated: "+annotated);
		Log.d(TAG, "nTs: "+timestamps.getLong("tsNot")+", ts: "+timestamps.getLong("ts"));
		if (annotated) {
			this.bubbleTexts = startingIntent.getStringArrayListExtra("texts");
			this.bubbleTypes = startingIntent.getIntegerArrayListExtra("types");
			Log.d(TAG, "bubbleTexts: "+bubbleTexts.size());
			imageType = "annotated";
		}
		
		if (yourSelectedImage!=null)
			if(yourSelectedImage.isRecycled())
				yourSelectedImage.prepareToDraw();
		if (imageWithBubbles!=null)
			if (imageWithBubbles.isRecycled())
				imageWithBubbles.prepareToDraw();
		
		new WorkerTask().execute(Uri.parse(selectedImage));
	}
	

	
	
	
//	public class HelperThread extends Thread {
//		
//		public void run(Uri selectedImage) {
//			
//			String[] filePathColumn = {MediaStore.Images.Media.DATA};
//
//	        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
//	        cursor.moveToFirst();
//
//	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
//	        String filePath = cursor.getString(columnIndex);
//	        cursor.close();
//	        
//	        
//	        	try {
//					this.sleep(5000);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//
//	        
//			BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
//			decodeOpts.inSampleSize = 4;
//			iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
//			yourSelectedImage = BitmapFactory.decodeFile(filePath, decodeOpts);
//			//throws OutOfMemoryError
////			yourSelectedImage = BitmapFactory.decodeFile(filePath);
//			
//			iv.setImageBitmap(yourSelectedImage);
//			
//			
//			progress.dismiss();
//			
//		}
//		
//	}
	
	private class HelperThread implements Runnable {
		public void run() {
			
			frame = new ImageFrame(ChosenImage_Activity.this, yourSelectedImage.getWidth(), yourSelectedImage.getHeight(), iv.getWidth(), iv.getHeight());
//			fl.addView(frame);
			
			if (annotated) {
				bubbleView = new BubbleView(ChosenImage_Activity.this, bubbleTexts, bubbleTypes);
				LayoutParams params = new LayoutParams((int)frame.onScreenWidth, (int)frame.onScreenHeight);
				bubbleView.setLayoutParams(params);
				bubbleViewContainer.addView(bubbleView);
			}
			
			
		}
	}
	
	private class PullThread implements Runnable {
		public void run() {
			Intent intent = new Intent(ChosenImage_Activity.this, CloudPull_Service.class);
			startService(intent);				
		}
	}
	
	private class WorkerThread implements Runnable {
		public void run() {
			
//			progress = ProgressDialog.show(ChosenImage_Activity.this, "Sending...", "Please wait", true);

			Log.d(TAG, "&&yourSelImage w: "+yourSelectedImage.getWidth()+", yourSelImageH: "+yourSelectedImage.getHeight());

			//make a mutable bitmap...
			imageWithBubbles = yourSelectedImage.copy(Bitmap.Config.RGB_565, true);
			
			Canvas canvas = new Canvas(imageWithBubbles);
			
			//get true image bounds in on-screen coordinate space
			RectF imageBounds = new RectF(frame.rect1.right, frame.rect1.top, frame.rect2.left, frame.rect1.height());
			
			Log.d(TAG, "imageBounds left: "+imageBounds.left +", top: "+imageBounds.top +", bottom: "+imageBounds.bottom+", right: "+imageBounds.right);
			Log.d(TAG, "final size, w: "+imageWithBubbles.getWidth()+", h: "+imageWithBubbles.getHeight());
			
			//this should draw everything onto the canvas and then return to SaveAndSend
			if (annotated) 
				bubbleView.saveView(imageWithBubbles.getHeight(), imageWithBubbles.getWidth(), canvas, imageBounds, ChosenImage_Activity.this);
			
			else if (!annotated)
				new SaveAndSend().run();
			
			
			//debug...
			//the factors we have to multiply the x and y coordinates with, we know 
	    	//both photo and screen are landscape, so: 
//	    	float factorX = (float)imageWithBubbles.getWidth()/(float)imageBounds.width();
//	    	float factorY = (float)imageWithBubbles.getHeight()/(float)imageBounds.height();
//	    	
//	    	Log.d(TAG, "factorX: "+factorX +", factorY" +factorY);
//	    	
//	    	for (MyRect rect : bubbleView.myRects ) {
//	    		rect.left *= factorX;
//	    		rect.right *= factorX;
//	    		rect.bottom *= factorY;
//	    		rect.top *= factorY;
//	    		
//	    		Paint paint = new Paint();
//	    		paint.setColor(Color.WHITE);
//	    		canvas.drawRect(rect, paint);
//	    	}
//	    	
//	    	recipient.sendEmptyMessage(0);
			
			
//			//create an empty bitmap
//			Bitmap returnedBitmap =  Bitmap.createBitmap(iv.getWidth(), iv.getHeight(),Bitmap.Config.ARGB_8888);
//			//we need a canvas associated with that bmp
//			Canvas canvas = new Canvas(returnedBitmap);
//			
//			//draw the contents of the layout (all child views) into that canvas
//			fl.draw(canvas);
//			
//			//now the bmp will hold the view of this activity...
//			ImageView saved = new ImageView(ChosenImage_Activity.this);
//			saved.setImageBitmap(returnedBitmap);
//			
//			fl.removeAllViews();
//			fl.addView(saved);
		}
	}
	
	private class SaveAndSend implements Runnable {
		public void run() {
			Log.d(TAG, "SaveAndSend");
			if (croppedTitle.equals(""))
				bubblisedTitle = oriTitle;
			else 
				bubblisedTitle = croppedTitle;
			
			Uri uri = Uri.parse(selectedImage);
			//save only if annotated or cropped
			
			if (!imageType.equals("raw")) {
				 File sdCard = Environment.getExternalStorageDirectory();
					File store = new File(sdCard.getAbsolutePath()+"/DITP/Shared");
					if (!store.isDirectory())
					{
						store.mkdir();
						Log.d(TAG, "just made dir");
					}
					String path = store.getAbsolutePath().toLowerCase();
					String name = store.getName().toLowerCase();
					ContentValues values = new ContentValues();
					values.put(Images.Media.TITLE, bubblisedTitle);
					values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
					values.put(Images.Media.MIME_TYPE, "image/jpeg");
					values.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
					values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
					values.put("_data", path+"/"+bubblisedTitle+".jpg");
					values.put(Images.Media.LATITUDE, LastLocation.lat);
					values.put(Images.Media.LONGITUDE, LastLocation.lng);
	
				uri  = ChosenImage_Activity.this.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
				try {
				  OutputStream outStream = ChosenImage_Activity.this.getContentResolver().openOutputStream(uri);
	//			  returnedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
				  imageWithBubbles.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
				  outStream.flush();
				  outStream.close();
				  Log.d(TAG,"done exporting to SD");
				} catch (FileNotFoundException e) {
				   Log.e(TAG, e.toString());
				} catch (IOException e) {
				  e.printStackTrace();
				}
			}
			
			String bubbleMetaData = getBubbleMetaData();
			
			Log.d(TAG, "bubbleMeta: "+ bubbleMetaData);
			
			
//			//will prompt user to choose app that responds ACTION_SEND intent, 
//			//e.g. gmail, SMS etc. 
//			Intent sendIntent = new Intent(Intent.ACTION_SEND); 
//			sendIntent.putExtra("sms_body", "Automics "+bubbleMetaData); 
//			sendIntent.putExtra("subject", "Type 'Automics' in the 'To' field above");
////			sendIntent.setData(uri);
//			sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
//			sendIntent.setType("image/jpeg"); 
//			startActivity(sendIntent);
			
			//will not return as it is not intended to return a result...
//			startActivityForResult(sendIntent, MMS_SENT);
			
			
			//send MMS programmatically in AsyncTask -- doesn't work currently, MMS not supported in android-sdk :-(
//			new MMSTask().execute(uri);
			
			//before we push we need to pull for new images
			new PullThread().run();
			
			
			//implement with CloudPush_Service
			Intent cloudPush = new Intent(UPLOAD);
			cloudPush.putExtra("uri", uri.toString());
			cloudPush.putExtra("oriTitle", oriTitle);
			cloudPush.putExtra("internalTitle", bubblisedTitle);
			cloudPush.putExtra("img_type", imageType);
			cloudPush.putExtra("bubble_meta", bubbleMetaData);
//			startService(cloudPush);
			ChosenImage_Activity.this.sendBroadcast(cloudPush);
			logToUserDb();
			if (timestamps.getLong("tsNot", 0) > 0) {
				//we know this is issued by the system...
				startActivity(new Intent(ChosenImage_Activity.this, MultChoice_Activity.class).putExtra("ts", timestamps.getLong("tsNot")));
			}
			ChosenImage_Activity.this.finish();
		}
	}
	
	private void logToUserDb() {
		long notTs = timestamps.getLong("tsNot");
		long ts = timestamps.getLong("ts");
		Log.d(TAG, "nTs: "+notTs+", ts: "+ts);
		if (notTs == 0) {
			//we know this is not a task by the system...
			userdata = new UserDataSQLHelper(this).getWritableDatabase();
			userdata.execSQL("UPDATE userdata SET task_completed_ts="+System.currentTimeMillis()+
					" WHERE task_acceptance_ts="+ts);
			userdata.close();
		}
		else if (notTs > 0) {
			userdata = new UserDataSQLHelper(this).getWritableDatabase();
			userdata.execSQL("UPDATE userdata SET task_completed_ts="+System.currentTimeMillis()+
					" WHERE notification_ts="+notTs);
			userdata.close();
		}
	}
	
	private class WorkerTask extends AsyncTask<Uri, Integer, Integer> {
	     protected Integer doInBackground(Uri... params) {
	    	String[] filePathColumn = {MediaStore.Images.Media.DATA};
	    	 
//	    	iv = (ImageView) findViewById(R.id.chosen_image);
//	    	iv = (ImageView) ChosenImage_Activity.this.findViewById(R.id.chosen_image);

    	 	Uri selectedImage = params[0];
	        Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
	        cursor.moveToFirst();

	        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	        String filePath = cursor.getString(columnIndex);
	        cursor.close();
	        
			BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
//			decodeOpts.inSampleSize = 4;
//			decodeOpts.outHeight = 600;
//			decodeOpts.outWidth = 800;
			
//			iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			yourSelectedImage = BitmapFactory.decodeFile(filePath, decodeOpts);
			Log.d(TAG, "yourSelImage w: "+yourSelectedImage.getWidth()+", yourSelImageH: "+yourSelectedImage.getHeight());
			//throws OutOfMemoryError
//				yourSelectedImage = BitmapFactory.decodeFile(filePath);
			
			
			Message msg = new Message();
			msg.arg1 = 1;
			
			handler.sendMessage(msg);
			
			return RESULT_OK;
	     }

	     protected void onProgressUpdate(Integer... progress) {
//	         setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(Integer... result) {
	    	 //never called?
	    	 Log.d(TAG, "result: "+result[0]+", RESULT_OK: "+RESULT_OK);
	    	 if (result[0] == RESULT_OK && ChosenImage_Activity.this.progress != null) {
//	    		 iv.setImageBitmap(yourSelectedImage);
				ChosenImage_Activity.this.progress.dismiss();
	    	 }
	    	 
	         
	     }
	 }
	
	private class MMSTask extends AsyncTask<Uri, Integer, Integer> {
	     protected Integer doInBackground(Uri... params) {
	    	 
	    	String SENT = "SMS_SENT";
	        String DELIVERED = "SMS_DELIVERED"; 
	    	 
   	 		Uri readyImage = params[0];
   	 		
//	   	 	Intent sendIntent = new Intent(Intent.ACTION_SEND);
   	 		Intent sendIntent = new Intent(SENT);
//			sendIntent.putExtra("sms_body", "some text"); 
			sendIntent.putExtra(Intent.EXTRA_STREAM, readyImage);
			sendIntent.setType("image/jpeg"); 
//			startActivity(sendIntent);
			
			PendingIntent toSentIntent = PendingIntent.getActivity(ChosenImage_Activity.this, 0, sendIntent, 0);
			PendingIntent deliveryIntent = PendingIntent.getActivity(ChosenImage_Activity.this, 0, new Intent(DELIVERED), 0);
	       
			//---when the SMS has been sent---
	        registerReceiver(new BroadcastReceiver(){
	            @Override
	            public void onReceive(Context arg0, Intent arg1) {
	                switch (getResultCode())
	                {
	                    case Activity.RESULT_OK:
	                        Toast.makeText(getBaseContext(), "SMS sent", 
	                                Toast.LENGTH_SHORT).show();
	                        break;
	                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
	                        Toast.makeText(getBaseContext(), "Generic failure", 
	                                Toast.LENGTH_SHORT).show();
	                        break;
	                    case SmsManager.RESULT_ERROR_NO_SERVICE:
	                        Toast.makeText(getBaseContext(), "No service", 
	                                Toast.LENGTH_SHORT).show();
	                        break;
	                    case SmsManager.RESULT_ERROR_NULL_PDU:
	                        Toast.makeText(getBaseContext(), "Null PDU", 
	                                Toast.LENGTH_SHORT).show();
	                        break;
	                    case SmsManager.RESULT_ERROR_RADIO_OFF:
	                        Toast.makeText(getBaseContext(), "Radio off", 
	                                Toast.LENGTH_SHORT).show();
	                        break;
	                }
	            }
	        }, new IntentFilter(SENT));
	 
	        //---when the SMS has been delivered---
	        registerReceiver(new BroadcastReceiver(){
	            @Override
	            public void onReceive(Context arg0, Intent arg1) {
	                switch (getResultCode())
	                {
	                    case Activity.RESULT_OK:
	                        Toast.makeText(getBaseContext(), "SMS delivered", 
	                                Toast.LENGTH_SHORT).show();
	                        break;
	                    case Activity.RESULT_CANCELED:
	                        Toast.makeText(getBaseContext(), "SMS not delivered", 
	                                Toast.LENGTH_SHORT).show();
	                        break;                        
	                }
	            }
	        }, new IntentFilter(DELIVERED));        

			
			
   	 		SmsManager smsMngr = SmsManager.getDefault();
   	 		smsMngr.sendTextMessage(MMS_GATEWAY, null, "Automic", toSentIntent, deliveryIntent);
			
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
	    	 if (result[0] == RESULT_OK && ChosenImage_Activity.this.progress != null) {
				ChosenImage_Activity.this.progress.dismiss();
	    	 }
	    	 
	         
	     }
	 }


	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1==1)
				iv.setImageBitmap(yourSelectedImage);
			new HelperThread().run();
			if (progress.isShowing())
				progress.dismiss();
			
			Log.d(TAG, "cancelled dialog");
		}
	};
	
	public Handler recipient = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			new SaveAndSend().run();
		}
	};
	
	public void onPause() {
		Log.d(TAG, "onPause()");
		super.onPause();
//		if (addMore) {
//			if (yourSelectedImage!= null)
//				yourSelectedImage.recycle();
//			if (imageWithBubbles!=null)
//				imageWithBubbles.recycle();
//		}
	}
	
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
		//TODO: unbind to avoid memory leak...
		super.onDestroy();
		iv.destroyDrawingCache();
		if (yourSelectedImage!= null)
			yourSelectedImage.recycle();
		if (imageWithBubbles!=null)
			imageWithBubbles.recycle();
	}
	
	private String getBubbleMetaData() {
		String bubbleMetaData = "";
		if (annotated) {
			//get bubble coordinates etc. to also send to server
			ArrayList <MyRect> rects =  bubbleView.getRects();
			for (int i = 0; i < rects.size(); i++) {
				MyRect rect = rects.get(i);
				int type = 0;
				String text = rect.text;
				float centerX = rect.centerX();
				float centerY = rect.centerY();
				float bottomX = rect.right;
				float bottomY = rect.bottom;
				float topX = rect.left;
				float topY = rect.top;
				float endX = 0;
				float endY = 0;
				if (rect.speech) {
					MyTriangle tri = bubbleView.getTriangles().get(i);
					endX = tri.bottomX;
					endY = tri.bottomY;
					type = 1;
				}
				if (rect.thought) {
					MyThoughtBubbles mTB = bubbleView.getThoughtBubbles().get(i);
					endX = mTB.endX;
					endY = mTB.endY;
					type = 2;
				}
				if (rect.scene)
					type = 3;
				
				bubbleMetaData += "<bubble type="+"\""+type+"\""
				+ " text="+"\""+text +"\""
				+ " centre="+"\""+centerX +"/"+centerY+ "\""
				+ " topLeft="+"\""+topX +"/"+topY+ "\""
				+ " bottomRight="+"\""+bottomX +"/"+bottomY+ "\""
				+ " end="+"\""+endX +"/"+endY+ "\""
				+ "/>"
				;
			}
		}
		return bubbleMetaData;
	}

}
