package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.storage.UserDataSQLHelper;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class OnlineTask extends Activity {
	
	private final static String TAG = "OnlineTask";
	WebView mWebView;
	private Bundle timestamps;
	private SQLiteDatabase userdata;
	long ts;
	long tsNot;
	private TelephonyManager tm;
	String deviceId;
	private String GROUP;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    	    
	    getWindow().requestFeature(Window.FEATURE_PROGRESS);
	    setContentView(R.layout.onlinetask);
	    setProgressBarVisibility(true);
	    
	    GROUP = getResources().getString(R.string.groupname);
	    
	    tm = (TelephonyManager)getSystemService(Service.TELEPHONY_SERVICE);
	    deviceId = tm.getDeviceId(); //IMEI
	    
	    ts = this.getIntent().getLongExtra("ts", 0);
	    tsNot = this.getIntent().getLongExtra("tsNot", 0);
	    
	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setJavaScriptEnabled(true);
	    
	    final Activity activity = this;
	    mWebView.setWebChromeClient(new WebChromeClient() {
	    	   public void onProgressChanged(WebView view, int progress) {
	    	     // Activities and WebViews measure progress with different scales.
	    	     // The progress meter will automatically disappear when we reach 100%
//	    		   Log.d("OnlineTask", "progress: "+progress);
	    		   activity.setProgress(progress * 100);
	    	   }
	    	   public void onConsoleMessage(String message, int lineNumber, String sourceID) {
	    		    Log.d("OnlineTask", message + " -- From line " + lineNumber + " of " + sourceID);
	    		  }

	    	 });

	    mWebView.setWebViewClient(new OnlineTaskWebViewClient());
	    
	    mWebView.loadUrl("http://www.automics.net/v8/author/get-ride.php?id="+deviceId+"/&group="+GROUP);
	
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
	        mWebView.goBack();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	public boolean onCreateOptionsMenu(android.view.Menu menu) {
		 MenuItem item = menu.add(0, 0, 0, "Continue");
	 		item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
	 			public boolean onMenuItemClick(MenuItem item) {
	 				//TODO: figure out what to do, 
	 				if (tsNot>0) {
	 					//we know this is a task from the system, go to mult choice q
	 					startActivity(new Intent(OnlineTask.this, MultChoice_Activity.class).putExtra("ts", tsNot));
	 				}
	 				else if (tsNot == 0) {
	 					
	 				}
	 				
	 				logToUserDb();
 					OnlineTask.this.finish();
	 				return false;
	 			}
	 		});
	 		return true;
	}

	
	private class OnlineTaskWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        view.loadUrl(url);
	        return true;
	    }
	    
	    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
	        Toast.makeText(OnlineTask.this, "Oh no! " + description, Toast.LENGTH_SHORT).show();
	      }

	    
	    public boolean onKeyDown(int keyCode, KeyEvent event) {
	        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
	            mWebView.goBack();
	            return true;
	        }
	        return false;
	    }
	}
	
	private void logToUserDb() {

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
}
