package mrl.automics.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import mrl.automics.R;
import mrl.automics.sensors.SensorManager_Service;
import mrl.automics.storage.LocationModelSQLHelper;
import mrl.automics.storage.LocationsSQLHelper;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ExperienceStarter_Activity extends Activity {
	
	private static final String TAG = "ExperienceStarter_Activity";
//	private static final String DIR = "/LocationTagger";
	private static final String DIR = "/GPSTriggers";
	private SQLiteDatabase db;
	private BufferedReader reader;
	private BufferedWriter bufferedWriter;
	private SQLiteDatabase userData;
	private TelephonyManager tm;
	TextView tv;
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loclistenerstarter);
        
        Button startButton = (Button) findViewById(R.id.startloc_button);
        Button stopButton = (Button) findViewById(R.id.stoploc_button);
        tv = (TextView)findViewById(R.id.state);
        tv.setTextSize(30);
        tv.setText("NOT RUNNING");
        
        startButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// TODO start service
				ExperienceStarter_Activity.this.startService(new Intent (ExperienceStarter_Activity.this, SensorManager_Service.class));
				ExperienceStarter_Activity.this.tv.setText("RUNNING");
				tv.setTextColor(Color.WHITE);
			}
        });
        
        stopButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// TODO stop service
				ExperienceStarter_Activity.this.stopService(new Intent (ExperienceStarter_Activity.this, SensorManager_Service.class));
				ExperienceStarter_Activity.this.tv.setText("STOPPED");
				ExperienceStarter_Activity.this.tv.setTextColor(Color.RED);
			}
        	
        });
        
        Button populateDb = (Button) findViewById(R.id.populate_button);
        
        populateDb.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				//do the work in a thread
				new WorkerThread().run(); 
			}
        });
        
    }
    public class WorkerThread implements Runnable {
		public void run() {
			//read in file from LocationTagger
			File sdCard = Environment.getExternalStorageDirectory();
			File store = new File(sdCard.getAbsolutePath()+DIR);
			
			db = new LocationModelSQLHelper(ExperienceStarter_Activity.this).getWritableDatabase();
			//make sure we have a clean db...
			db.execSQL("DELETE FROM gps_triggers");

			//TODO: change to LocationTags.csv !
//			File file = new File(store, "Debug_LocationTags.csv");
			File file = new File(store, "LocationTags.csv");
			if (!file.isFile()) {
				Toast.makeText(ExperienceStarter_Activity.this, "Cannot find neccessary file "+DIR+"/LocationTags.csv !", Toast.LENGTH_LONG).show();
			}
			else if (file.isFile()) {
				//we're good, start reading file...
				try {
					reader = new BufferedReader(new FileReader(file));
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), e.toString());
				}
				String line;
				StringTokenizer st;
				String token;
				boolean firstRow=true;
				try {
					while ((line = reader.readLine()) != null) {
						if (firstRow) {
							Log.d(TAG, "firstRow: "+line);
						}
						if (!firstRow) {
							Log.d(TAG, "line: "+line);
							st = new StringTokenizer (line, ",", false);
							int tokenCount = 0;
							while (st.hasMoreElements()) {
								token = st.nextToken();
								Log.d(TAG, "tokenCount:"+tokenCount+", token: "+ token);
								
								//TODO: write tokens to db
								switch (tokenCount){
									case 0: 
										db.execSQL("INSERT INTO gps_triggers (tagId) VALUES ("+Integer.parseInt(token)+")");
										break;
									case 1:
										db.execSQL("UPDATE gps_triggers SET type='"+token+"' WHERE id=(SELECT max(id) FROM gps_triggers)");
										break;
									case 2: 
										db.execSQL("UPDATE gps_triggers SET radius="+Integer.parseInt(token) +" WHERE id=(SELECT max(id) FROM gps_triggers)");
										break;
									case 3: 
										db.execSQL("UPDATE gps_triggers SET comment="+token+" WHERE id=(SELECT max(id) FROM gps_triggers)");
										break;
									case 4:
										db.execSQL("UPDATE gps_triggers SET lat="+Double.parseDouble(token)+" WHERE id=(SELECT max(id) FROM gps_triggers)");
										break;
									case 5: 
										db.execSQL("UPDATE gps_triggers SET long="+Double.parseDouble(token)+" WHERE id=(SELECT max(id) FROM gps_triggers)");
										break;
									default:
										//do nothing
								}	
								tokenCount++;
							}
						}
						firstRow = false;
					}
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), e.toString());
				}
				db.close();
				Toast.makeText(ExperienceStarter_Activity.this, "Successfully populated Location Model!", Toast.LENGTH_LONG).show();
			}
		}
	}
    
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
		 MenuItem item = menu.add(0, 0, 0, "Export all");
	 		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
	 			public boolean onMenuItemClick(MenuItem item) {
	 				//prepare writing csv+kml file to sd card
			        writeUserData();
			        writeKmlFile();
		 			return false;
	 			}
	 		});
	 		return true;
    }
    
    //all in seconds
    private long getTimeDiff (long before, long after) {
    	long timeDiff = (after - before)/1000;
    	return timeDiff;
    }
    
    private void writeUserData() {
    	File sdCard = Environment.getExternalStorageDirectory();
		File store = new File(sdCard.getAbsolutePath()+"/DITP/UserData");
		if (!store.isDirectory())
		{
			store.mkdir();
			Log.d(getClass().getSimpleName(), "just made dir");
		}
		
		//write userdata to .csv file
//			File file = new File(store, "Journey"+journeyNo+".csv");
		File file = new File(store, "BehaviouralData.csv");
		

		
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
			bufferedWriter.write("taskType,taskId,init,gpsTagId,notificationType,notificationTs,reminderCount," +
					"notficationAcceptTs,acceptanceTime,taskAcceptTs,taskAcceptTime,taskComp,taskCompTs," +
					"taskCompTime,responseTime,receptRating \n");
		}catch (IOException e) {
			Log.e(TAG, "writing to sdcard failed: "+e.toString());
		}
		
		String taskType = "";
		int taskId = 0;
		String init = "";
		int gpsTag = 0;
		String notType = "";
		long notTs = 0;
		int reminderCount = 0;
		long notATs = 0;
		long accT = 0;
		long taskAccTs = 0;
		long taskAccT = 0;
		int taskComp = 0; 
		long taskCompTs = 0;
		long taskCompT = 0;
		long responseT = 0;
		int recRat = 0;
		
		
		if (userData==null || !userData.isOpen()) {
			userData = new UserDataSQLHelper(ExperienceStarter_Activity.this).getReadableDatabase();
		}
		Cursor c = userData.rawQuery("SELECT task_type,task_id,init_type,gpstag_id,notification_type,notification_ts," +
				"reminder,notification_accept_ts,acceptance_time,task_acceptance_ts,task_acceptance_time,task_completed,task_completed_ts," +
					"task_completion_time,response_time,receptivity_rating FROM userdata", null);

		if (c.getCount() > 0) {
			c.moveToFirst();
			try {
				taskType = c.getString(0);
				taskId = c.getInt(1);
				init = c.getString(2);
				gpsTag = c.getInt(3);
				notType = c.getString(4);
				notTs = c.getLong(5);
				reminderCount = c.getInt(6);
				notATs = c.getLong(7);
				accT = c.getLong(8);
				taskAccTs = c.getLong(9);
				taskAccT = c.getLong(10);
				taskComp = c.getInt(11);
				taskCompTs = c.getLong(12);
				taskCompT = c.getLong(13);
				responseT = c.getLong(14);
				recRat = c.getInt(15);
			}catch (NullPointerException npe) {
				Log.d(TAG, npe.toString());
			}
			try { 
				bufferedWriter.write(taskType +","+ taskId +","+init +","+ gpsTag +","+ notType +","+ notTs +","+reminderCount +
						","+ notATs +","+getTimeDiff(notTs, notATs) +"," + taskAccTs + "," + getTimeDiff(notATs, taskAccTs) +"," +
						taskComp +"," + taskCompTs +"," + getTimeDiff(taskAccTs, taskCompTs) + "," +getTimeDiff(notTs, taskCompTs) + ","+
						recRat + "\n");
			}catch (IOException e) {
				Log.e(TAG, "writing to sdcard failed: "+e.toString());
			}
			Log.d(TAG, taskType +","+ taskId +","+init +","+ gpsTag +","+ notType +","+ notTs +","+reminderCount +
						","+ notATs +","+getTimeDiff(notTs, notATs) +"," + taskAccTs + "," + getTimeDiff(notATs, taskAccTs) +"," +
						taskComp +"," + taskCompTs +"," + getTimeDiff(taskAccTs, taskCompTs) + "," +getTimeDiff(notTs, taskCompTs) + ","+
						recRat);
			while (c.moveToNext()) {
				try {
					taskType = c.getString(0);
					taskId = c.getInt(1);
					init = c.getString(2);
					gpsTag = c.getInt(3);
					notType = c.getString(4);
					notTs = c.getLong(5);
					reminderCount = c.getInt(6);
					notATs = c.getLong(7);
					accT = c.getLong(8);
					taskAccTs = c.getLong(9);
					taskAccT = c.getLong(10);
					taskComp = c.getInt(11);
					taskCompTs = c.getLong(12);
					taskCompT = c.getLong(13);
					responseT = c.getLong(14);
					recRat = c.getInt(15);
				}catch (NullPointerException npe) {
					Log.d(TAG, npe.toString());
				}
				try { 
					bufferedWriter.write(taskType +","+ taskId +","+init +","+ gpsTag +","+ notType +","+ notTs +","+reminderCount +
							","+ notATs +","+getTimeDiff(notTs, notATs) +"," + taskAccTs + "," + getTimeDiff(notATs, taskAccTs) +"," +
							taskComp +"," + taskCompTs +"," + getTimeDiff(taskAccTs, taskCompTs) + "," +getTimeDiff(notTs, taskCompTs) + ","+
							recRat + "\n");
				}catch (IOException e) {
					Log.e(TAG, "writing to sdcard failed: "+e.toString());
				}
				Log.d(TAG, taskType +","+ taskId +","+init +","+ gpsTag +","+ notType +","+ notTs +","+reminderCount +
						","+ notATs +","+getTimeDiff(notTs, notATs) +"," + taskAccTs + "," + getTimeDiff(notATs, taskAccTs) +"," +
						taskComp +"," + taskCompTs +"," + getTimeDiff(taskAccTs, taskCompTs) + "," +getTimeDiff(notTs, taskCompTs) + ","+
						recRat);
			}
		}
		
		try {
			bufferedWriter.close();
		} catch (IOException e) {
			Log.e("StatsActivity",e.toString());
		}
		userData.close();
		c.close();
		Toast.makeText(ExperienceStarter_Activity.this, "successfully exported to sd card", Toast.LENGTH_SHORT).show();
    }
    
    private void writeKmlFile() {
    	
    	//write kml file to sdcard
    	
    	SQLiteDatabase db = new LocationsSQLHelper(ExperienceStarter_Activity.this).getWritableDatabase();
    	
    	tm = (TelephonyManager)getSystemService(Service.TELEPHONY_SERVICE);
    	String imei = tm.getDeviceId();
    	
    	File sdCard = Environment.getExternalStorageDirectory();
		File store = new File(sdCard.getAbsolutePath()+"/DITP/UserData");
		File kmlFile = new File(store, "GpsTrace.kml");
		if (!kmlFile.isFile())
		{
			try {
				kmlFile.createNewFile();
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), e.toString());
				Log.d(getClass().getSimpleName(), "just made kmlFile");
			}
		}
		try {
			 bufferedWriter = new BufferedWriter(new FileWriter(kmlFile));
		} catch (IOException e) {
			Log.e(getClass().getSimpleName(), e.toString());
		}
		
		String startDate ="";
		String endDate="";
		float dist = 0;
		String time = "";
		long ts = 0;
		double lng = 0;
		double lat = 0;
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		
		Cursor c = db.rawQuery("SELECT lat,long,distance,time,timestamp FROM locations", null);
		if (c.getCount()>0) {
			c.moveToFirst();
			try {
				startDate = c.getString(3);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
			c.moveToLast();
			try {
				endDate = c.getString(3);
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
			c.moveToFirst();
			do {
				try {
					dist += c.getFloat(2);
				} catch (Exception e) {
					Log.e(TAG, e.toString());
				}
				Log.d(TAG, "distance: " + dist);
			} while (c.moveToNext());
		}
    	
		//write "preamble"
		try {
			bufferedWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" 
					+ "<kml xmlns=\"http://www.opengis.net/kml/2.2\"> \n" 
					+ "<Document> \n" 
					+ "<name>GPS Trace for "+imei+"</name>\n"
					+ "<description>Start time: "+startDate +" \n"
					+ "End time: "+endDate +" \n"
					+ "Total distance: "+dist+" m \n "	
						+ "</description> \n" 
					+ "<Style id=\"yellowLineGreenPoly\"> \n"
					+ "<LineStyle>\n"
					+ "<color>7f00ffff></color> \n"
					+ "<width>3</width> \n"
					+ "</LineStyle> \n"
					+ "<PolyStyle>\n"
					+ "<color>7f00ff00</color> \n"
					+ "</PolyStyle>\n"
					+ "</Style> \n"
					);
    	} catch (IOException e) {
    		Log.e(getClass().getSimpleName(), e.toString());
    	}
    	
		if (c.getCount() > 0) {
			c.moveToFirst();
			try {
				lat = c.getLong(0)/1E6;
				lng = c.getLong(1)/1E6;
				time = c.getString(3);
				ts = c.getLong(4);
			}catch (NullPointerException npe) {
				Log.d("StatsActivity", npe.toString());
			}
			date.setTime(ts);
			String datetime = df.format(date);
			//write first placemark as starting point 
			try {
				bufferedWriter.write("<Placemark> \n"
						+ "<name>Start</name>"
						+ "<description>Time: "+time+" \n" 
						+ "</description> \n" 
						+ "<TimeStamp><when>"+datetime+ "</when></TimeStamp>\n" 
						+ "<Point><coordinates>"+lng+ "," +""+lat +",50</coordinates></Point> \n"
						+ "</Placemark> \n" 
						+ "<Placemark> \n " 
						+ "<name>path</name> \n "
						+ "<styleUrl>#yellowLineGreenPoly</styleUrl> \n" 
						+ "<LineString> \n"
						+ "<extrude>1</extrude> \n "
						+ "<tessellate>1</tessellate> \n"
						+ "<altitudeMode>relativeToGround</altitudeMode> \n"
						+ "<coordinates> \n"
						+ ""+lng + "," +""+lat +",50 \n");
			}catch (IOException e) {
				Log.e("OverviewMap", e.toString());
			}
			//add all the other waypoints
			while (c.moveToNext()) {
				try {
					lat = c.getLong(0)/1E6;
					lng = c.getLong(1)/1E6;
				}catch (NullPointerException npe) {
					Log.d("StatsActivity", npe.toString());
				}
				try {
					bufferedWriter.write(""+lng + "," +""+lat +",50 \n");	//displays altitude of line at 50
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), e.toString());
				}
			}

			try {
				bufferedWriter.write("</coordinates> \n" 
						+ "</LineString> \n"
						+ "</Placemark> \n"
//						+ "</Document> \n "
//						+ "</kml>"
						);
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), e.toString());
			}
			c.moveToFirst();
			while (c.moveToNext()) {
				try {
					lat = c.getLong(0)/1E6;
					lng = c.getLong(1)/1E6;
					time = c.getString(3);
					ts = c.getLong(4);
					
				}catch (NullPointerException npe) {
					Log.d("StatsActivity", npe.toString());
				}
				date.setTime(ts);
				datetime = df.format(date);
				try {
					bufferedWriter.write("<Placemark> \n " +
							"<description>Time: "+time+" \n" +
							"</description> \n" +
							"<TimeStamp><when>"+datetime+ "</when></TimeStamp>\n" +
							"<Point><coordinates>" +
							lng + "," +""+lat +",50</coordinates></Point>\n" +
							"</Placemark>\n");	
				} catch (IOException e) {
					Log.e(getClass().getSimpleName(), e.toString());
				}
			}
			//write closings
			try {
				bufferedWriter.write(
						  "</Document> \n "
						+ "</kml>"
						);
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(), e.toString());
			}
		}
		c.close();
		db.close();
				
		try {
			bufferedWriter.close();
		} catch (IOException e) {
			Log.e("StatsActivity",e.toString());
		}
		Toast.makeText(ExperienceStarter_Activity.this, "Successfully exported kml to sdcard", Toast.LENGTH_SHORT).show();
		
		
    }
    
    
    
}