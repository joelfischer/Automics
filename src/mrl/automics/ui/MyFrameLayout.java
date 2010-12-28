package mrl.automics.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class MyFrameLayout extends FrameLayout {
	
//	private PinchImageView piv;
	private final static String TAG = "MyFrameLayout";
	
	public MyFrameLayout(Context context) {
		super(context);
	}
	public MyFrameLayout(Context context, AttributeSet atts) {
		super(context, atts);
	}

		
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		
		return true;
		
	}
	
//	@Override 
//	public boolean onInterceptTouchEvent (MotionEvent event) {
//		super.onInterceptTouchEvent(event);
//		
//		Log.d(TAG, "interceptEvent: "+event.toString());
//		
//		piv.onTouchEvent(event);
//		
////		View bubble1 = this.findViewById(101);
////		View bubble2 = this.findViewById(102);
////		
//////		ListArray touchables = this.addTouchables(touchables);
////		
//////		bubble2.dispatchTouchEvent(event);
//////		bubble2.dispatchTouchEvent(event);
////		bubble2.onTouchEvent(event);
//
//		
//		return true;
//	}
		

}
