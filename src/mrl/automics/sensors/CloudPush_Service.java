package mrl.automics.sensors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import mrl.automics.R;
import mrl.automics.storage.ImageDataSQLHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;


public class CloudPush_Service extends Service {
	
	private static final String TAG = "CloudPush_Service";
	private static final int DELAY_60 = 60*1000;
	private static final String SERVER_URL = "http://www.automics.net/v8/upload.php";
	private static final String SERVERGET_URL = "http://www.automics.net/v8/userfiles/";
	private static final int NETWORK_TYPE_HSDPA = 8;	//T-Mobile's 3G network
//	private static final String SERVER_URL = "http://www.automics.net/lysesoft/process.php";
	private String GROUP;
	private Uri imageUri;
	private String imagePath;
	private String type;
	private String bubbleMetaData = "";
	private Bitmap image;
	private DefaultHttpClient client;
	private HttpPost httpPost;
	private HttpGet httpGet;
//	private ResponseHandler <String> responseHandler;
	private NotificationManager mNM;
	private TelephonyManager tm;
	private ConnectivityManager cm;
	private long deviceId;
	private BufferedWriter bufferedWriter;
	private int idOnServer;
	private String title;
	private String internalName;
	private String groupname;
	private int version;
	private long originator;
	private long timestamp;
	private SQLiteDatabase db;
	private PowerManager pm;
	private WakeLock wl;
	private File textFile;
	private String sourceType;
	
	@Override
    public void onCreate() {
       
		mNM = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
		tm = (TelephonyManager)getSystemService(Service.TELEPHONY_SERVICE);
		pm = (PowerManager)getSystemService(Service.POWER_SERVICE);
		cm = (ConnectivityManager)getSystemService(Service.CONNECTIVITY_SERVICE);
        deviceId = Long.parseLong(tm.getDeviceId()); //IMEI
//        db = new ImageDataSQLHelper(CloudPush_Service.this).getWritableDatabase();
        Log.d(TAG , "IMEI: "+deviceId);		
        GROUP = getResources().getString(R.string.groupname);
    }
    

    @Override
    public void onDestroy() {
    	
    	Log.d(TAG, "onDestroy()");
    }
    
    @Override
    public void onStart(Intent intent, int startId) {
    	
    	showNotification();
    	
    	imagePath = intent.getStringExtra("image_path");
    	
    	imagePath = getRealPathFromURI(Uri.parse(intent.getStringExtra("uri")));
    	title = intent.getStringExtra("oriTitle");
    	internalName = "";
    	type = intent.getStringExtra("img_type");
    	//type either one of ... 
    	if (type.equals("annotated") || type.equals("cropped")) {
//    		bubbleMetaData = intent.getStringExtra("bubble_meta");
    		internalName = intent.getStringExtra("internalTitle");
    	}

    	Log.d(TAG, "title: "+title+", internalName: "+internalName);
//    	if (title.contains("-") || title.contains("_")) {
//    		//images was not downloded, but shot with this device,
//    		//otherwise title = timestamp
//    		title = internalName;
//    	}
    	if (!internalName.equals("")) {
    		//we know this is a cropped image, so make the cropped image's title (internalName) 
    		//the title of the image
    		title = internalName;
    	}
    	
    	if (internalName.equals("")) {
    		internalName = ""+System.currentTimeMillis();;
    	}
//    	if (title.equals("")) {
//    		title = internalName;
//    	}
    	
      	imageUri = Uri.parse(imagePath);
		
		client = new DefaultHttpClient();
        
        new CheckNetworkThread().run();
        
        this.stopSelf();		
    	
    }
    

    @Override
    public IBinder onBind(Intent intent) {

        
    	return null;
    }
    
 // And to convert the image URI to the direct file system path of the image file  
	public String getRealPathFromURI(Uri contentUri) {  
	  
	  // can post image  
	    String [] proj={MediaStore.Images.Media.DATA};  
	    Cursor cursor = this.getContentResolver().query(contentUri,  
	            proj, 		// Which columns to return  
	            null,       // WHERE clause; which rows to return (all rows)  
	            null,       // WHERE clause selection arguments (none)  
	            null); // Order-by clause (ascending by name)  
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);  
	    cursor.moveToFirst();  
	  
	    return cursor.getString(column_index);  
	}  
    
    public class CheckNetworkThread implements Runnable {
    	public void run() {
    		int networkType = tm.getNetworkType();
    		boolean hasWifi = false;
    		if (cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState()==NetworkInfo.State.CONNECTED) {
    			hasWifi = true;
    			Log.d(TAG, "Connected to WiFi");
    		}
            
            Log.d(TAG, "network type (see TelephonyManager): "+networkType);
            
            //only attempt to download when we have data 
            if (networkType == TelephonyManager.NETWORK_TYPE_UMTS || hasWifi || 
            		networkType == NETWORK_TYPE_HSDPA || 
            		networkType == TelephonyManager.NETWORK_TYPE_GPRS || 
            		networkType == TelephonyManager.NETWORK_TYPE_EDGE) {
            	 //do the work in a thread 
                new Thread(new WorkerThread()).start();
            }
            else {
    			Log.d(TAG, "no 3G, scheduled to try upload again in 60s");
            	scheduleDelayedUploading();
            }
    	}
    	
    }
    
    private void scheduleDelayedUploading() {
    	//schedule to try again in 60s
    	//start SchedulerHelper with proper args...
		Intent delayUpload = new Intent(CloudPush_Service.this, SchedulerHelper_Service.class);
		delayUpload.putExtra("uri", imageUri);
		delayUpload.putExtra("oriTitle", title);
		delayUpload.putExtra("bubblisedTitle", internalName);
		delayUpload.putExtra("img_type", type);
		delayUpload.putExtra("bubble_meta", bubbleMetaData);
		startService(delayUpload);
    }
    
    public class WorkerThread extends Thread implements Runnable {
    	     		
		public void run() {
			
			mNM = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
			
		    client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
			
//			if (db == null || (!db.isOpen())) 
		    db = new ImageDataSQLHelper(CloudPush_Service.this).getWritableDatabase();
		    
			//TODO: get programmatically
			groupname = GROUP;
			
			timestamp = System.currentTimeMillis();
			
			int lastUploadedImage = getLastUploadedImageId(groupname);
			int mostRecentOnBoard = getMostRecentOnBoard();
			
			if (mostRecentOnBoard>lastUploadedImage) {
				//there is a queue of images on this device that need to be uploaded, thus:
				idOnServer = mostRecentOnBoard+1;
			}
			else if (mostRecentOnBoard<=lastUploadedImage) {
				idOnServer = lastUploadedImage+1;
			}
			
			Log.d(TAG, "lastUploaded: "+lastUploadedImage);
			Log.d(TAG, "idOnServer: "+idOnServer);
			
			//get version and originator
			long imei = checkVersion();
			Log.d(TAG, "originator="+originator +", deviceId="+deviceId+", imei="+imei +", type: "+type);
			if (!  //only upload this image if not:
					( 		//the origin of this photo is not this phone & the type is raw - 
							//means the image has previously been already downloaded from server and 
							//there are no further annotations. 
							(originator != deviceId && type.equals("raw")) || 
							//the origin is this device & it was previously uploaded & the type is raw - 
							//so there can be no further annotations.
							(originator == deviceId && imei == deviceId && type.equals("raw")) 
					)  
				)  {
				
			
				//info file for meta data etc.
			    textFile = writeToFile("info_file.txt");
			    writeToDb();
			    //upload the image
			    boolean success = uploadImage(groupname, textFile);
				
				if (!success) {
					//in case uploading fails, remove entry for this image from db 
					//and schedule delayed uploading. 
					deleteFromDb(); 
					scheduleDelayedUploading();
				}
				
			}
			db.close();
	    	mNM.cancel(R.string.cloud_push_started);
		}
    }
    
    private int getMostRecentOnBoard() {
		Log.d(TAG, "getting most recent on board");
		int mostRecentOnBoard = 0;
		//check which image files we have on board
		Cursor c = db.rawQuery("SELECT server_name FROM imagedata", null);
		if (c.getCount()>0) {
			c.moveToLast();
			try {
				mostRecentOnBoard = c.getInt(0);
			}
			catch (Exception e) {
				Log.e(TAG, e.toString());
			}
		}
		else {
			Log.i(TAG, "there are images on server but none appear to come from this device.");
		}
		c.close();
		Log.d(TAG, "mostRecentOnBoard: "+mostRecentOnBoard);
		return mostRecentOnBoard;
	}

	public long checkVersion() {
		
		version = 1;
		originator = deviceId;
		long imei = 0;
		
		//check if there already is an entry for this image by original title
		if (! ( title.contains("_") || title.contains("-")) ) {
			Cursor c = db.rawQuery("SELECT version,originator,imei,source_type FROM imagedata WHERE title="+Long.parseLong(title), null);
			if (c.getCount() == 0) {
				//this image has no entry and was thus created on this device and not uploaded yet. 
			}
			else if (c.getCount() > 0) {
				//the image was downloaded from the server or a version of it has already been uploaded
				c.moveToFirst();
				try {
					originator = c.getLong(1);
					version = c.getInt(0);
					imei =  c.getLong(2);
					sourceType = c.getString(3);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
				//increment version
				version++;
			}
			c.close();
		}
		else {
			Cursor c = db.rawQuery("SELECT version,originator,imei,source_type FROM imagedata WHERE title='"+title+"'", null);
			if (c.getCount() == 0) {
				//this image has no entry and was thus created on this device and not uploaded yet. 
			}
			else if (c.getCount() > 0) {
				Log.d(TAG, "string matching worked");
				//the image was downloaded from the server or a version of it has already been uploaded
				c.moveToFirst();
				try {
					originator = c.getLong(1);
					version = c.getInt(0);
					imei =  c.getLong(2);
					sourceType = c.getString(3);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
				//increment version
				version++;
			}
			c.close();
		}
		if (sourceType==null) {
			sourceType = "u";
		}
		Log.d(TAG, "sourceType: "+sourceType);
		
		return imei;
	}
	
	private void writeToDb() {
		Log.d(TAG, "writing imagedata to DB");
		db.execSQL("INSERT INTO imagedata (server_name,imei,title,internal_name,group_name,version,originator,source_type,timestamp)" +
				" VALUES (" +
				 +idOnServer+"," 
				 +deviceId+","
				 +"'"+title+"',"
				 +internalName+","
				 +"'"+groupname+"',"
				 +version+","
				 +originator+","
				 +"'"+sourceType+"',"+
				 +timestamp+
					")" );
		
		Log.d(TAG, "write to db: "+idOnServer+"," 
				 +deviceId+","
				 +title+","
				 +internalName+","
				 +"'"+groupname+"',"
				 +version+","
				 +originator+","
				 +sourceType+","
				 +timestamp);
	}
	
	private void deleteFromDb() {
		db.execSQL("DELETE FROM imagedata WHERE timestamp="+timestamp);
	}

	public File writeToFile(String name) {
		
		Log.d(TAG, "write info_file.txt"); 
		//prepare writing csv+kml file to sd card
        File sdCard = Environment.getExternalStorageDirectory();
		File store = new File(sdCard.getAbsolutePath()+"/DITP");
		if (!store.isDirectory())
		{
			store.mkdir();
			Log.d(TAG, "just made dir");
		}
		Log.d(TAG, "sdCard:"+sdCard.toString());
		
		//write userdata to .csv file
//		File file = new File(store, "Journey"+journeyNo+".csv");
		File file = new File(store, name);

		try {
			file.createNewFile();
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), e.toString());
			Log.d(getClass().getSimpleName(), "just made file");
		}
		try {
			 bufferedWriter = new BufferedWriter(new FileWriter(file));
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), e.toString());
		}
		try {
			bufferedWriter.write("source_type,nameOnServer,IMEI,title,internalName,group,version,originatorIMEI,timestamp \n"
					+sourceType+","+idOnServer +","+ deviceId +","+title+","+internalName+","+groupname+","+version+","
					+originator+","+timestamp);
		}catch (IOException e) {
			Log.e(TAG, "writing to sdcard failed: "+e.toString());
		}
		try {
			bufferedWriter.close();
		} catch (IOException e) {
			Log.e("StatsActivity",e.toString());
		}
		
		return file;
	}
	
	public int getLastUploadedImageId(String groupname) {
		Log.d(TAG, "get last uploaded from server...(last.txt)");

		HttpResponse response = null;
		
		InputStream returnedStream;

		//get last uploaded image, incl. random number to avoid caching
		httpGet = new HttpGet(SERVERGET_URL+groupname+"/last.txt?"+System.currentTimeMillis());
		int lastUploadedImage = 0;		
		
		int statusCode;
		
		//get the name of the last image uploaded
		try {
			Log.d(TAG, "executing GET: " + httpGet.getRequestLine());
			response = client.execute(httpGet);
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		
		Log.d(TAG, "response: "+response);

		
		if (response != null) {
			Log.d(TAG, "response not null: "+response);
//			responseHandler.handleResponse(response);
			statusCode = response.getStatusLine().getStatusCode();
			Log.d(TAG, "statusLine: "+response.getStatusLine());
			Log.d(TAG, "statusCode: "+statusCode);
			
			HttpEntity resEntity = response.getEntity();			
			
			if (resEntity != null) {
				try {
					returnedStream = resEntity.getContent();
					BufferedReader br = new BufferedReader(new InputStreamReader(returnedStream));
					String line = br.readLine().trim();
					if (! line.contains("Not"))
						lastUploadedImage = Integer.parseInt(line);
					Log.d(TAG, "lastUploadedImage: "+lastUploadedImage);
					
				} catch (IOException e) {
					Log.e(TAG, e.toString());
				}
			}
		}
		return lastUploadedImage;
	}
	
	public boolean uploadImage(String groupname, File textFile) {
		Log.d(TAG, "UPLOADING image...");
		boolean success = false;
		String responseBody = null;
		HttpResponse response = null;
		httpPost = new HttpPost(SERVER_URL);
		
		File imageFile = new File(imagePath);
		
		Log.d(TAG, "file to upload: "+imageFile.toString() );
		
		//405 error when using FileEntity
//		HttpEntity entity = new FileEntity(imageFile, "image/jpeg");
		
		MultipartEntity entity = new MultipartEntity();
	    ContentBody imageFileBody = new FileBody(imageFile, "image/jpeg");
	    entity.addPart("image_file", imageFileBody);
	    
	    //TODO: get group name from db...
	    StringBody sb = null;
		try {
			sb = new StringBody(groupname);
		} catch (UnsupportedEncodingException e1) {
			Log.e(TAG, e1.toString());
		}
	    entity.addPart("group_name", sb);
	    
	    ContentBody infoFileBody = new FileBody(textFile, "text/plain");
	    entity.addPart("info_file", infoFileBody);
		
	    
//		InputStream is = null;
//		try {
//			is = new FileInputStream(imageFile);
//		} catch (FileNotFoundException e1) {
//			Log.e(TAG, e1.toString());
//		}
//		byte[] data = null;
//		try {
//			data = IOUtils.toByteArray(is);
//		} catch (IOException e1) {
//			Log.e(TAG, e1.toString());
//		}
//		
//		InputStreamBody isb = new InputStreamBody(new ByteArrayInputStream(data),"uploadedFile");
//		
//		MultipartEntity entity = new MultipartEntity();
//	    entity.addPart("Upload Image", isb);
	    
	    httpPost.setEntity(entity);		    
		
		int statusCode = 0;
		
		//TODO: postpone upload when we don't have 3G/Edge
		
		try {
//			Log.d("DataUploader_Service", "executing httpGet");
//			responseBody = client.execute(httpPost, responseHandler);
			Log.d(TAG, "executing request " + httpPost.getRequestLine());
			response = client.execute(httpPost);
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		
		if (response != null) {
			Log.d(TAG, "response not null: "+response);
//			responseHandler.handleResponse(response);
			statusCode = response.getStatusLine().getStatusCode();
			Log.d(TAG, "statusLine: "+response.getStatusLine());
			Log.d(TAG, "statusCode: "+statusCode);
			
			HttpEntity resEntity = response.getEntity();
			if (resEntity != null) {
				try {
					resEntity.consumeContent();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e(TAG, e.toString());
				}
			}
		}
		
		if (statusCode==200) {
			success = true;
		}
		
		Log.d(TAG, "response: "+response);
		client.getConnectionManager().shutdown();
		return success;
	}
	
	//also works
	private class AsyncUpload extends AsyncTask<Void, Integer, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {
//	        responseHandler = new BasicResponseHandler();
			Log.d(TAG, "UPLOADING image...");
			boolean success = false;
			String responseBody = null;
			HttpResponse response = null;
			httpPost = new HttpPost(SERVER_URL);
			
			File imageFile = new File(imagePath);
			
			Log.d(TAG, "file to upload: "+imageFile.toString() );
			
			//405 error when using FileEntity
//			HttpEntity entity = new FileEntity(imageFile, "image/jpeg");
			
			MultipartEntity entity = new MultipartEntity();
		    ContentBody imageFileBody = new FileBody(imageFile, "image/jpeg");
		    entity.addPart("image_file", imageFileBody);
		    
		    //TODO: get group name from db...
		    StringBody sb = null;
			try {
				sb = new StringBody(groupname);
			} catch (UnsupportedEncodingException e1) {
				Log.e(TAG, e1.toString());
			}
		    entity.addPart("group_name", sb);
		    
		    
		    
		    ContentBody infoFileBody = new FileBody(textFile, "text/plain");
		    entity.addPart("info_file", infoFileBody);
			
		    
		    httpPost.setEntity(entity);		    
			
			int statusCode = 0;
			
			//TODO: postpone upload when we don't have 3G/Edge
			
			try {
//				Log.d("DataUploader_Service", "executing httpGet");
//				responseBody = client.execute(httpPost, responseHandler);
				Log.d(TAG, "executing request " + httpPost.getRequestLine());
				response = client.execute(httpPost);
			} catch (ClientProtocolException e) {
				Log.e(TAG, e.toString());
			} catch (IOException e) {
				Log.e(TAG, e.toString());
			}
			
			if (response != null) {
				Log.d(TAG, "response not null: "+response);
//				responseHandler.handleResponse(response);
				statusCode = response.getStatusLine().getStatusCode();
				Log.d(TAG, "statusLine: "+response.getStatusLine());
				Log.d(TAG, "statusCode: "+statusCode);
				
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					try {
						resEntity.consumeContent();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						Log.e(TAG, e.toString());
					}
				}
			}
			
			if (statusCode==200) {
				success = true;
			}
			
			Log.d(TAG, "response: "+response);
			client.getConnectionManager().shutdown();
			return Activity.RESULT_OK;
		}
		
	}

/**
 * Show a notification while this service is running.
 */
	private void showNotification() {
	    // In this sample, we'll use the same text for the ticker and the expanded notification
	    CharSequence text = getText(R.string.cloud_push_started);
	
	    // Set the icon, scrolling text and timestamp
	    Notification notification = new Notification(R.drawable.uploading, text,
	            System.currentTimeMillis());
	
	    // The PendingIntent to launch our activity if the user selects this notification
	    PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
	            new Intent(this, Intent.CATEGORY_HOME.getClass()), 0);
	
	    // Set the info for the views that show in the notification panel.
	    notification.setLatestEventInfo(this, getText(R.string.cloud_push_label),
	                   text, contentIntent);
	
	    // Send the notification.
	    // We use a string id because it is a unique number.  We use it later to cancel.
	    mNM.notify(R.string.cloud_push_started, notification);
	}
	

	private Handler mHandler = new Handler() {};

}
