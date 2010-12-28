package mrl.automics.graphics;

import android.content.Context;
import android.graphics.RectF;
import android.util.Log;

public class MyTriangle {
	
	private static final String TAG = "MyTriangle";
	public final static float OFFSET = 20;

	private int id;	
	//3 corners of the triangle
	public float topLeftX;
	public float topLeftY;
	public float topRightX;
	public float topRightY;
	public float bottomX;
	public float bottomY;
	
	public MyTriangle(Context context, MyRect rect) {
		this.id = rect.getID();
		//set initial values based on text rect
		//top two corners left and right of the bottom center of the rect
		this.topLeftX = rect.right - rect.width/2 - OFFSET;
		this.topLeftY = rect.bottom;
		this.topRightX = rect.right - rect.width/2 + OFFSET;
		this.topRightY = rect.bottom;
		
		//init bottom corner, to point towards mouth
		this.bottomX = rect.right - rect.width/2 - 40;
		this.bottomY = rect.bottom + 100;
		
	}
	
	public void setBottomX(int newValue) {
		this.bottomX = newValue;
	}
	
	public void setBottomY(int newValue) {
		this.bottomY = newValue;
	}
	
	public void setId (int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setTopCornersX(float newValue) {
		this.topLeftX = newValue;
		this.topRightX = newValue + 2*OFFSET;
	}
	
	public void setTopCornersY(float newValue) {
		this.topLeftY = newValue;
		this.topRightY = newValue;
	}

}
