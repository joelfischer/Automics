package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

public class NotificationCancel_Activity extends Activity {
	
	private NotificationManager nm;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		nm = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
	}
	
	public void onStart() {
		super.onStart();
				
		nm.cancel(R.string.new_images_alert);
		
		Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
		galleryIntent.setType("image/*");
		startActivity(galleryIntent);
		Intent startingIntent = this.getIntent();
		long ts = startingIntent.getLongExtra("id", 0);
		//record to db that this notification was accepted
		logToUserDb(ts);
		this.finish();
	}
	
	private void logToUserDb(long ts) {
		SQLiteDatabase userdata = new UserDataSQLHelper(this).getWritableDatabase();
		userdata.execSQL("UPDATE userdata SET notification_accept_ts="+System.currentTimeMillis()+
				" WHERE notification_ts="+ts);
		userdata.close();
	}

}
