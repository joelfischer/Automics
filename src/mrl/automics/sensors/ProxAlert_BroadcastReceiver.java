package mrl.automics.sensors;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;


public class ProxAlert_BroadcastReceiver extends BroadcastReceiver {
	
	private final static int WAKE_TIME = 5000;  //wake the device up for 5s to make sure notfication is delivered
	private final static String PROX_ALERT ="mrl.automics.sensors.PROX_ALERT";
	private static final String TAG = "ProxAlert_BroadcastReceiver";
	private final static String UPLOAD = "mrl.automics.sensors.UPLOAD";
	private final static String IMAGES_ALERT = "mrl.automics.sensors.IMAGES_ALERT";

	@Override
	public void onReceive(Context context, Intent intent) {
		
		PowerManager pm = (PowerManager) context.getSystemService(Service.POWER_SERVICE);
        PowerManager.WakeLock w = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ProxAlert_BroadcastReceiver");
        w.acquire(WAKE_TIME);
        
		Log.d(TAG,"fired");
		
		
		if (intent != null && intent.getAction() != null) {
			
			if (PROX_ALERT.compareToIgnoreCase(intent.getAction())==0) {
				//this is a proximity alert, delegate to DeliveryOpportunityManager
				Log.d(TAG, "this is a PROX_ALERT");
				Intent delegateIntent = new Intent(context, DeliveryOpportunityManager_Service.class);
				delegateIntent.putExtra("alertType", "prox");
				context.startService(delegateIntent.putExtras(intent.getExtras()));
			}
			//TODO: room for other intents, .e.g. inbox event (new photo)...
			
			if (UPLOAD.compareToIgnoreCase(intent.getAction())==0) {
				//this is a request to start the upload service
				Log.d(TAG, "handling request to start upload service");
				Intent uploadInt = new Intent(context, CloudPush_Service.class);
				context.startService(uploadInt.putExtras(intent.getExtras())); 
			}
			
			if (IMAGES_ALERT.compareToIgnoreCase(intent.getAction())==0) {
				Log.d(TAG, "new images");
				Bundle extras = intent.getExtras();
				extras.putString("alertType", "img");
				context.startService(new Intent(context, MessageDelivery_Service.class).putExtras(extras));
				
				
				
			}
		}
		
				
	}
	

}