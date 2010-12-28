package mrl.automics.graphics;

import android.content.Context;
import android.graphics.RectF;

public class MyThoughtBubbles {
	
	private int id;
	public float length;
	public float startX;
	public float startY;
	public float endX;
	public float endY;
	
	public MyThoughtBubbles(Context context, MyRect rect) {
		this.id = rect.getID();
		//set initial values based on text rect
		//start point in the center of the text rect
		this.startX = rect.right - rect.width/2;
		this.startY = rect.bottom - rect.height/2;
		
		//init length with 200, user changeable
		this.length = 160;
		
		//init values
		this.endX = startX;
		this.endY = startY+length;
				
	}
	
	public void setId (int id) {
		this.id = id;
	}
	
	public int getId() {
		return this.id;
	}
	
	public void setStartX(float newValue) {
		float deltaX = startX - newValue;
//		setDeltaEndX(deltaX);
		this.startX = newValue;
		setLength();
	}
	//moves the thought bubbles with the body of the bubble
	private void setDeltaEndX(float delta) {
		this.endX = endX - delta;
		setLength();
	}
	
	public void setStartY(float newValue) {
		float deltaY = startY - newValue;
//		setDeltaEndY(deltaY);
		this.startY = newValue;
		setLength();
	}
	//moves the thought bubbles with the body of the bubble
	private void setDeltaEndY(float delta) {
		this.endY = endY - delta;
		setLength();
	}
	
	
	public void setEndX(float newValue) {
		this.endX = newValue;
		setLength();
	}
	
	public void setEndY(float newValue) {
		this.endY = newValue;
		setLength();
	}
	
	private void setLength() {
		//the length of the line is the hypothenuse c=sqrt(a^2+b^2)
		double length = Math.sqrt(  (double) (this.endX - this.startX)*
									(this.endX - this.startX) + 
											(this.endY - this.startY)*
											(this.endY - this.startY)
											);
		this.length = (float)length;
	}

}
