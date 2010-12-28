package mrl.automics.graphics;

import android.content.Context;
import android.graphics.RectF;
import android.util.Log;

public class MySickle extends RectF {
	
	private static final String TAG = "MySickle";
	private int id;
	private float height;
	
	public MySickle(Context context, MyRect rect) {
		this.id = rect.getID();
		//set initial values based on text rect
		//top right corner in the center of the text rect
		this.right = rect.right - rect.width/2;
		this.top = rect.bottom - rect.height/2;
		
		//init height with 70, user changeable
		this.height = 70;
		
		//static x dimension of oval/sickle 
		this.left = right - 200;
		
		//initial y dimension
		this.bottom = top + height;
		
	}
	
	public void setBottom(int newValue) {
		this.bottom = newValue;
		this.height = this.bottom - this.top;
		Log.d(TAG, "bottom: "+newValue +", top: "+top);
	}
	
	public void setId (int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setRight(float newValue) {
		this.right = newValue;
		this.left = right - 200;
	}
	
	public void setTop(float newValue) {
		this.top = newValue;
		this.bottom = top + height;
	}

}
