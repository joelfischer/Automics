package mrl.automics.sensors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.StringTokenizer;

import mrl.automics.R;
import mrl.automics.storage.ImageDataSQLHelper;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CloudPull_Service extends Service {
	
	private static final String TAG = "CloudPull_Service";
	private static final String SERVER_URL = "http://www.automics.net/v8/userfiles/";
	private static final int NETWORK_TYPE_HSDPA = 8;	//as a constant in Android from platform L5 
//	private static final String SERVER_URL = "http://www.automics.net/lysesoft/process.php";
	private String GROUP;
	private final static String IMAGES_ALERT = "mrl.automics.sensors.IMAGES_ALERT";
	private Uri imageUri;
	private String imagePath;
	private String type;
	private String bubbleMetaData;
	private Bitmap image;
	private DefaultHttpClient client;
	private HttpGet httpGet;
	private ResponseHandler <String> responseHandler;
	private NotificationManager mNM;
	private TelephonyManager tm;
	private String deviceId;
	private BufferedWriter bufferedWriter;
	private SQLiteDatabase db;
	private ConnectivityManager cm;
	private Bitmap downloadedImage;
	private boolean emergencyStop;
	public static boolean activeThread;

	
	
	@Override
    public void onCreate() {
       
		mNM = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
		tm = (TelephonyManager)getSystemService(Service.TELEPHONY_SERVICE);
		cm = (ConnectivityManager)getSystemService(Service.CONNECTIVITY_SERVICE);
        deviceId = tm.getDeviceId(); //IMEI
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
    	
    	//TODO: pass in groupname, or get here to match 
    	//..userfiles/groupname dir.
    	
		
		client = new DefaultHttpClient();
        responseHandler = new BasicResponseHandler();
        
        int networkType = tm.getNetworkType();
        boolean hasWifi = false;
        if (cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState()==NetworkInfo.State.CONNECTED) {
			hasWifi = true;
			Log.d(TAG, "Connected to WiFi");
		}
        
        Log.d(TAG, "network type (see TelephonyManager): "+networkType + "hasWifi: "+hasWifi);
        
        //only attempt to download when we have UMTS/3G
        if (networkType == TelephonyManager.NETWORK_TYPE_UMTS || hasWifi ||
        		networkType == NETWORK_TYPE_HSDPA ||
        		networkType == TelephonyManager.NETWORK_TYPE_GPRS || 
        		networkType == TelephonyManager.NETWORK_TYPE_EDGE) {
        	 //do the work in a thread 
            new Thread(new WorkerThread()).start();
        }
        
        this.stopSelf();		
    	
    }
    

    @Override
    public IBinder onBind(Intent intent) {

        
    	return null;
    }
    
    public class WorkerThread implements Runnable {
    	     		
		public void run() {
			
			activeThread = true;
			
			mNM = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);			
	        db = new ImageDataSQLHelper(CloudPull_Service.this).getWritableDatabase();
			
			//TODO: get groupname from somewhere...
			String groupname = GROUP;
			
		    client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		    
			//check which image has been last uploaded to the server 
			int lastUploadedImage = getLastUploadedImageId(groupname);
			
			if (lastUploadedImage > 0) {
				//note: will be 0 when there are images on server but none from 
				//this device, works though...
				int mostRecentOnBoard = getMostRecentOnBoard();
				
				//download the ones > lastDownloaded/onboard image 
				int oldestToDownload = mostRecentOnBoard +1;
				
				if (lastUploadedImage > mostRecentOnBoard) {
					
					for (int i = oldestToDownload; i <= lastUploadedImage; i++) {
						
						if (!emergencyStop) {
						
							String filename = String.valueOf(System.currentTimeMillis()) ;
							
							/**
							 * 1. download image and store to sd card as jpeg. 
							 * @rparam filename is for internal storage so we can always
							 * associate an image with a db-entry
							 */
							//download and write to sd storage
							downloadImage(groupname, i, filename);
							
							/**
							 * 1. download text file and store to db. Condition: 
							 * successful downloading and saving to sdcard of image (!emergencyStop).
							 * @param groupname.
							 * @param i - the filename to download.
							 * @param filename - the timestamp is for internal storage 
							 * so we can always associate an image with a db-entry
							 */
							if (!emergencyStop)
								downloadTextFile(groupname, i, filename);
						}
						else 
							Log.w(TAG, "emergency STOP");
					}
					//let's delegate to notify the user we have new images available
					int numberDownloaded = lastUploadedImage-mostRecentOnBoard;
					Log.d(TAG, "just downloaded "+numberDownloaded);
					notifyUser(numberDownloaded);
				}
			}
			else {
					//TODO: there are no uploads yet, nothing to download.
					
			}
			db.close();
			mNM.cancel(R.string.cloud_pull_started);	
			client.getConnectionManager().shutdown();
			activeThread = false;
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

//	public File writeToFile(String name, String metaData) {
//		
//		//prepare writing csv+kml file to sd card
//        File sdCard = Environment.getExternalStorageDirectory();
//		File store = new File(sdCard.getAbsolutePath()+"/DITP");
//		if (!store.isDirectory())
//		{
//			store.mkdir();
//			Log.d(TAG, "just made dir");
//		}
//		Log.d(TAG, "sdCard:"+sdCard.toString());
//		
//		//write userdata to .csv file
////		File file = new File(store, "Journey"+journeyNo+".csv");
//		File file = new File(store, name);
//
//		try {
//			file.createNewFile();
//		} catch (IOException e) {
//			Log.e(getClass().getSimpleName(), e.toString());
//			Log.d(getClass().getSimpleName(), "just made file");
//		}
//		try {
//			 bufferedWriter = new BufferedWriter(new FileWriter(file));
//		} catch (IOException e) {
//			Log.e(getClass().getSimpleName(), e.toString());
//		}
//		try {
//			bufferedWriter.write("deviceId,bubbleMetaData,timestamp,\n"
//					+deviceId +","+ metaData +","+System.currentTimeMillis());
//		}catch (IOException e) {
//			Log.e(TAG, "writing to sdcard failed: "+e.toString());
//		}
//		try {
//			bufferedWriter.close();
//		} catch (IOException e) {
//			Log.e("StatsActivity",e.toString());
//		}
//		
//		return file;
//	}
	
	public int getLastUploadedImageId(String groupname) {
		Log.d(TAG, "getting last uploaded image (last.txt)");

		HttpResponse response = null;
		
		InputStream returnedStream;

		//get last uploaded image, incl. random number to avoid caching
		httpGet = new HttpGet(SERVER_URL+groupname+"/last.txt?"+System.currentTimeMillis());
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
					//if it does only contain digits
					String line = br.readLine().trim();
					if (! line.contains("Not"))
						lastUploadedImage = Integer.parseInt(line);
					Log.d(TAG, "lastUploadedImage: "+lastUploadedImage);
					
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
			}
		}
		return lastUploadedImage;
	}
	
	public static String getBucketId(String bucketName) {
		 bucketName = bucketName.toLowerCase();
		 if (bucketName.charAt(bucketName.length() - 1) == '/') {
		  bucketName = bucketName.substring(0, bucketName.length() - 1);
		 }
		 return Integer.toString(bucketName.hashCode());
		}
	
	public void downloadImage(String groupname, int i, String filename) {
		Log.d(TAG, "DOWNLOADING new images");
		//get the image
		httpGet = new HttpGet(SERVER_URL+groupname+"/"+i+".jpg");
		HttpResponse response = null;
		HttpEntity resEntity;
		InputStream returnedStream;
		
		try {
//			Log.d("DataUploader_Service", "executing httpGet");
//			responseBody = client.execute(httpPost, responseHandler);
			Log.d(TAG, "executing GET: " + httpGet.getRequestLine());
			response = client.execute(httpGet);
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		
		Log.d(TAG, "response: "+response);
		Log.d(TAG, "statusLine: "+response.getStatusLine());
		
		if (response != null) {
			resEntity = response.getEntity();
			if (resEntity != null) {
				try {
					//the InpuStream holding the image data
					returnedStream = resEntity.getContent();

					//OK, write downloaded image to SD card
					
					//prepare writing file to sd card
			        File sdCard = Environment.getExternalStorageDirectory();
					File store = new File(sdCard.getAbsolutePath()+"/DITP/Shared/");
					if (!store.isDirectory())
					{
						store.mkdir();
						Log.d(TAG, "just made dir");
					}
					String path = store.getAbsolutePath().toLowerCase();
					String name = store.getName().toLowerCase();
					ContentValues values = new ContentValues(6);
					values.put(Images.Media.TITLE, filename);
					values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
					values.put(Images.Media.MIME_TYPE, "image/jpeg");
					values.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
					values.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
					values.put("_data", path+"/"+	filename+".jpg");
//					Uri url = MediaStore.Images.Media.getContentUri(path);
//					Uri test = Uri.fromFile(new File(path+"/"+filename+".jpg"));
//					Log.d(TAG, "SD URI: "+Images.Media.EXTERNAL_CONTENT_URI +", test: "+test + ", url: "+url);
					Uri uri = CloudPull_Service.this.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI.
							buildUpon().appendQueryParameter("bucketId", getBucketId(path)).build(), values);
					Log.d(TAG, "URI: "+uri.toString());
//					Uri test = Uri.fromFile(new File(path+filename+".jpg"));
					try {
					  OutputStream outStream = CloudPull_Service.this.getContentResolver().openOutputStream(uri);
					  //create a Bitmap from the InputStream, check the size first
					  BitmapFactory.Options opts = new BitmapFactory.Options();
					  opts.inJustDecodeBounds = true;
					  int decodeFactor = 1;
					  try {
						  downloadedImage = BitmapFactory.decodeStream(returnedStream, null, opts);
					  } catch (java.lang.OutOfMemoryError e) {
						  Log.e(TAG, "bitmap too large: "+e.toString());
						  emergencyStop = true;
					  }
					  Log.d(TAG, "image width: "+opts.outWidth);
					  if (opts.outWidth > 1600) {
						  //we know this image is not cropped, let's scale it by half...
						  decodeFactor = 2;
//						  opts.outWidth = opts.outWidth/2;
					  }
//					  new Rect(0,0,w/decodeFactor, h/decodeFactor)
					  int w = opts.outWidth;
					  int h = opts.outHeight;
					  opts = new BitmapFactory.Options();
					  opts.inSampleSize = decodeFactor;
					  if (!emergencyStop) {
						  try {
							  downloadedImage = BitmapFactory.decodeStream(client.execute(httpGet).getEntity().getContent(), null , opts);
							  downloadedImage.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
						  } catch (java.lang.OutOfMemoryError e) {
							  Log.e(TAG, "bitmap too large: "+e.toString());
							  emergencyStop = true;
						  }
					  }
					  outStream.flush();
					  outStream.close();
					  if (downloadedImage != null)
						  downloadedImage.recycle();
					  if (!emergencyStop)
						  Log.d(TAG,"done saving downloaded image to SD");
					} catch (Exception e) {
					   Log.e(TAG, e.toString());
					} 
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					Log.e(TAG, e.toString());
				}
			}
		}
	}
	
	public void downloadTextFile(String groupname, int i, String filename) {
		Log.d(TAG, "downloading info text file");
		//next, also get the accompanying text file
		httpGet = new HttpGet(SERVER_URL+groupname+"/"+i+".txt");
		HttpResponse response = null;
		HttpEntity resEntity;
		InputStream returnedStream;
		boolean failed = false;
		
		try {
			Log.d(TAG, "executing GET text file: " + httpGet.getRequestLine());
			response = client.execute(httpGet);
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.toString());
		} catch (IOException e) {
			Log.e(TAG, e.toString());
		}
		
		Log.d(TAG, "response: "+response);
		Log.d(TAG, "statusLine: "+response.getStatusLine());
		
		if (response != null) {
			resEntity = response.getEntity();
			if (resEntity != null) {
				try {
					//the InpuStream holding the text file data
					returnedStream = resEntity.getContent();
					//TODO: read lines and write to db...
					BufferedReader br = new BufferedReader(new InputStreamReader(returnedStream));
					String line;
					StringTokenizer st;
					String token;
					boolean firstRow=true;
					int tokenCount = 0;
					try {
						//read the lines and write to db
						while ((line = br.readLine()) != null) {
							if (firstRow) {
								//ignore the first row, this will hold the column names
								Log.d(TAG, "firstRow: "+line);
							}
							if (!firstRow) {
								Log.d(TAG, "line: "+line);
								st = new StringTokenizer (line, ",", false);
								while (st.hasMoreElements()) {
									token = st.nextToken();
									Log.d(TAG, "writing to DB: tokenCount: "+tokenCount+", token: "+ token);
									
									//TODO: write tokens to db
									switch (tokenCount){
										case 0: 
											db.execSQL("INSERT INTO imagedata (source_type) VALUES ('"+token+"')");
											break;
										case 1: 
											db.execSQL("UPDATE imagedata SET server_name="+i+" WHERE id=(SELECT max(id) FROM imagedata)");
											break;
										case 2:
											db.execSQL("UPDATE imagedata SET imei="+Long.parseLong(token)+" WHERE id=(SELECT max(id) FROM imagedata)");
											break;
										case 3: 
											db.execSQL("UPDATE imagedata SET title="+Long.parseLong(filename) +" WHERE id=(SELECT max(id) FROM imagedata)");
											break;
										case 4: 
											db.execSQL("UPDATE imagedata SET internal_name="+Long.parseLong(filename) +" WHERE id=(SELECT max(id) FROM imagedata)");
											break;
										case 5: 
											db.execSQL("UPDATE imagedata SET group_name='"+token+"' WHERE id=(SELECT max(id) FROM imagedata)");
											break;
										case 6:
											db.execSQL("UPDATE imagedata SET version="+Integer.parseInt(token)+" WHERE id=(SELECT max(id) FROM imagedata)");
											break;
										case 7: 
											db.execSQL("UPDATE imagedata SET originator="+Long.parseLong(token)+" WHERE id=(SELECT max(id) FROM imagedata)");
											break;
										case 8: 
											db.execSQL("UPDATE imagedata SET timestamp="+Long.parseLong(token)+" WHERE id=(SELECT max(id) FROM imagedata)");
										default:
											//do nothing
									}	
									tokenCount++;
								}
								Log.d(TAG, "total tokenCount: "+tokenCount);
								if (tokenCount==1) {
									//we know this was a raw picsolve image, add additional fields to db.
									Log.d(TAG, "write picsolve imagedata");
									db.execSQL("UPDATE imagedata SET " +
											"server_name="+i+", " +
											"imei=0," +
											"title="+Long.parseLong(filename)+"," +
											"internal_name="+Long.parseLong(filename)+"," +
											"group_name='"+GROUP +"'," +
											"version=1," +
											"originator=0," +
											"timestamp="+Long.parseLong(filename)+
											" WHERE id=(SELECT max(id) FROM imagedata)");
								}
							}
							firstRow = false;
						}	
					} catch (Exception e) {
							Log.e(TAG, e.toString());
							Log.e(TAG, "FAILED reading file/writing to DB");
							failed = true;
					}											
					
				} catch (Exception e) {
					Log.e(TAG, e.toString());
					Log.e(TAG, "FAILED getting reader from InputStream");
					failed = true;
				}
			}
			else if (resEntity==null) {
				//there is no attached text file
				Log.d(TAG, "NO TEXT FILE FOUND");
			}
		}
		
//		int id = -1;
//		if (!failed) {
//		//get internal db id and return
//			Cursor c = db.rawQuery("SELECT id FROM imagedata WHERE id=(SELECT max(id) FROM imagedata)", null);
//			if (c.getCount()>0) {
//				c.moveToFirst();
//				id = c.getInt(0);
//			}
//		}
	}

/**
 * Show a notification while this service is running.
 */
	private void showNotification() {
	    // In this sample, we'll use the same text for the ticker and the expanded notification
	    CharSequence text = getText(R.string.cloud_pull_started);
	
	    // Set the icon, scrolling text and timestamp
	    Notification notification = new Notification(R.drawable.downloading, text,
	            System.currentTimeMillis());
	
	    // The PendingIntent to launch our activity if the user selects this notification
	    PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
	            new Intent(this, Intent.CATEGORY_HOME.getClass()), 0);
	
	    // Set the info for the views that show in the notification panel.
	    notification.setLatestEventInfo(this, getText(R.string.cloud_pull_label),
	                   text, contentIntent);
	
	    // Send the notification.
	    // We use a string id because it is a unique number.  We use it later to cancel.
	    mNM.notify(R.string.cloud_pull_started, notification);
	}
	
	private void notifyUser(int num) {
		//send a broadcast to notify user of how many images we've downloaded
		Intent imgAlert = new Intent(IMAGES_ALERT);
		imgAlert.putExtra("num", num);
		CloudPull_Service.this.sendBroadcast(imgAlert);
		
	}
	

	public class PatchInputStream extends FilterInputStream {
		  public PatchInputStream(InputStream in) {
		    super(in);
		  }
		  public long skip(long n) throws IOException {
		    long m = 0L;
		    while (m < n) {
		      long _m = in.skip(n-m);
		      if (_m == 0L) break;
		      m += _m;
		    }
		    return m;
		  }
	}

}
