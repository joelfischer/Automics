package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.sensors.MessageDelivery_Service;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PhotoStory_Activity extends Activity {
	
	private static final String TAG = "PhotoStory_Activity";
	private SQLiteDatabase userdata;
	private long ts;
	
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
		
		ll.setBackgroundResource(R.drawable.break_activity_background);
		tv.setText(R.string.photo_story_task_text);
		tv.setTextSize(20);
		tv.setTextColor(Color.WHITE);
		tv.setPadding(5, 10, 5, 10);
		tv.setGravity(Gravity.CENTER_HORIZONTAL);
	
		LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	
		Button goOnline = new Button(this);
		goOnline.setText("Choose Photos");
		goOnline.setTextSize(20);
		goOnline.setGravity(Gravity.CENTER_HORIZONTAL);
		goOnline.setLayoutParams(params);
		ll.addView(goOnline);
		goOnline.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(PhotoStory_Activity.this, OnlineTask.class).putExtra("ts", ts));
				PhotoStory_Activity.this.finish();
			}
		});

	}
	
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent()");
	}	
	
	private void logToUserDb(long timestamp) {
		userdata = new UserDataSQLHelper(this).getWritableDatabase();
		userdata.execSQL("INSERT INTO userdata (task_type,task_id,init_type,task_acceptance_ts) " +
				"VALUES (" +
				"'photo_story',"+
				MessageDelivery_Service.TASK_PICKER +","+
				"'user'," +
				timestamp +
				")"
				);
		userdata.close();
	}

}
