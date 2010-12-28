package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MultChoice_Activity extends Activity {   
  
	NotificationManager nm;
	SQLiteDatabase db;
	boolean hasFocus;
	long ts;
	
 /** Called when the activity is first created. */
    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        setContentView(R.layout.mult_choice_question);  
        
        ts = this.getIntent().getLongExtra("ts", -1);

        // Watch for button clicks.
        Button button = (Button)findViewById(R.id.one);
        button.setOnClickListener(answer1Listener);
        button = (Button)findViewById(R.id.two);
        button.setOnClickListener(answer2Listener);
        button = (Button)findViewById(R.id.three);
        button.setOnClickListener(answer3Listener);
        button = (Button)findViewById(R.id.four);
        button.setOnClickListener(answer4Listener);
        button = (Button)findViewById(R.id.five);
        button.setOnClickListener(answer5Listener);        
    }
    
    protected void onResume() {
    super.onResume();
//    	if (hasFocus) {
//            db = (new MySQLiteOpenHelper(MultChoice_Activity.this)).getWritableDatabase();
//            Log.d("MultChoice_Activity","just created new DB");
//    	}
    }
    
    public void onWindowFocusChanged(boolean hasFocus) {
    	super.onWindowFocusChanged(hasFocus);
    	
    	this.hasFocus = hasFocus;
    	Log.d("MultChoice_Activity", "hasFocus="+hasFocus);
    	if (hasFocus) {
            db = (new UserDataSQLHelper(MultChoice_Activity.this)).getWritableDatabase();
            Log.d("MultChoice_Activity","just created new DB");
    	}
    	else if (!hasFocus) {
    		db.close();
    	}
    }

    private OnClickListener answer1Listener = new OnClickListener() {
        public void onClick(View v) {
            
            // Tell the user about what we did.
            Toast.makeText(MultChoice_Activity.this, R.string.one_txt,
                    Toast.LENGTH_LONG).show();       
//            nm.cancel(R.string.interruption_notification);
            
            updateDb(1);
            MultChoice_Activity.this.finish();
        }
    };
        
    private OnClickListener answer2Listener = new OnClickListener() {
        public void onClick(View v) {
            
            // Tell the user about what we did.
            Toast.makeText(MultChoice_Activity.this, R.string.two_txt,
                    Toast.LENGTH_LONG).show();      
//            nm.cancel(R.string.interruption_notification);
            
            updateDb(2);
            MultChoice_Activity.this.finish();
        }
    };
    
    private OnClickListener answer3Listener = new OnClickListener() {
        public void onClick(View v) {
            
            // Tell the user about what we did.
            Toast.makeText(MultChoice_Activity.this, R.string.three_txt,
                    Toast.LENGTH_LONG).show();     
//            nm.cancel(R.string.interruption_notification);
            
            updateDb(3);
            MultChoice_Activity.this.finish();
        }
    };
    
    private OnClickListener answer4Listener = new OnClickListener() {
        public void onClick(View v) {
            
            // Tell the user about what we did.
            Toast.makeText(MultChoice_Activity.this, R.string.four_txt,
                    Toast.LENGTH_LONG).show();  
//            nm.cancel(R.string.interruption_notification);
            
            updateDb(4);
            MultChoice_Activity.this.finish();
        }
    };
    
    private OnClickListener answer5Listener = new OnClickListener() {
        public void onClick(View v) {
            
            // Tell the user about what we did.
            Toast.makeText(MultChoice_Activity.this, R.string.five_txt,
                    Toast.LENGTH_LONG).show();  
//            nm.cancel(R.string.interruption_notification);
            
            updateDb(5);
            MultChoice_Activity.this.finish();
        }
    };
    
    private void updateDb(int rating) {
    	db.execSQL("UPDATE userdata SET receptivity_rating="+rating +
  		" WHERE notification_ts="+ts);
        db.close();
//        startActivity(new Intent (MultChoice_Activity.this, RateTaskActivity.class));
    }

    
}