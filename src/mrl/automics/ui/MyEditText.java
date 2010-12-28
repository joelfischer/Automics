package mrl.automics.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;

public class MyEditText extends EditText {

	public static final String TAG = "MyEditText";
	
	public MyEditText(Context context) {
		super(context);
	}
	
	public MyEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		
	}
	
	public MyEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
	}
	

	@Override 
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		
		Log.d(TAG, "keyCode: "+ keyCode + "event: "+event);
		
		super.onKeyDown(keyCode, event);
		
		return false;
		
	}
	


}
