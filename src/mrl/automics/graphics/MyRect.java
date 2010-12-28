package mrl.automics.graphics;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.Log;

public class MyRect extends RectF {
	 private Bitmap img; // the image of the ball
	 private int centerX = 0; // the x coordinate at the canvas
	 private int centerY = 0; // the y coordinate at the canvas
	 private int id; // gives every ball his own id, for now not necessary
	 private int count = 1;
	 private boolean goRight = true;
	 private boolean goDown = true;
	 private final static String TAG = "MyRect";
	 private int numberOfChars;
	 private float textWidth;
	 public String text = "";
	 public char[] textChars;
	 public float height = 0;
	 public float width = 0;
	 private TextPaint paint;
	 public Point linebreak;
	 public Path path;
	 public int stringCount = 0;
	 public String brokenText ="";
	 public float topLeftAlign = 0;
	 public int lineReturn;
	 public ArrayList <String> strings;
	 public boolean thought;
	 public boolean speech;
	 public boolean scene;
	 public static final int SPEECH = 1;
	 public static final int THOUGHT = 2;
	 public static final int SCENE = 3;
	 public static final int MAX_WIDTH = 500; //the max length of one line of text (ie. to force linebreak) 

 

	
	public MyRect(Context context, String text, Point point, TextPaint _paint, int type , int id) {
		
		this.id = id;
		
		
		this.text = text;
		this.textChars = text.toCharArray();
		this.paint = _paint;
    	this.textWidth = paint.measureText(text);
//    	Log.d(TAG, "textWidth: "+textWidth);
    	
    	this.numberOfChars = paint.breakText(text, true, 1000, null);
//    	Log.d(TAG, "numberOfChars: "+numberOfChars);
    	
    	Rect textBounds = new Rect();
    	paint.getTextBounds(text, 0, numberOfChars, textBounds);
//    	Log.d(TAG, "textBounds: "+textBounds.left +", "+textBounds.top +", "+textBounds.right+", "+textBounds.bottom);
    	this.left = textBounds.left+point.x;
    	
    	this.top = textBounds.top + point.y;
    	setTopLeftAlign();
    	this.bottom = this.topLeftAlign+paint.descent();
    	this.right = textBounds.right + point.x;
    	this.width = textBounds.right;
    	this.height = this.bottom - this.top;
    	
//    	Log.d(TAG, "height:"+this.height);
    	
    	this.stringCount++;
    	setLinebreak();
    	
    	this.brokenText = text;
    	
    	strings = new ArrayList<String>();
    	strings.add(0, text);
    	
    	switch (type) {
    	case SPEECH : this.speech = true; break;
    	case THOUGHT: this.thought = true; break;
    	case SCENE: this.scene = true; break;
    	}
    	
    	//if string too long init with linebreak
    	if (textWidth > MAX_WIDTH) {
    		setRight(600);
    		this.bottom = this.topLeftAlign+(paint.descent()+paint.getTextSize())*strings.size();
    		this.height = this.bottom - this.top;
    	}
    	
    	
	}
	
	public int getCount() {
		return this.count;
	}
	
	void setCenterX(int newValue) {
        this.centerX = newValue;
        this.left = newValue - (float)0.5*this.width;
        this.right = newValue + (float)0.5*this.width;
        setLinebreak();
        setTopLeftAlign();
    }
	
	public int getX() {
		return this.centerX;
	}

	void setCenterY(int newValue) {
        centerY = newValue;
        this.top = newValue - (float)0.5*this.height;
        this.bottom = newValue + (float)0.5*this.height;
        setLinebreak();
        setTopLeftAlign();
   }
	
	public void setBottom(int newValue) {
		this.bottom = newValue;
		this.height = this.bottom - this.top;
		setLinebreak();
		setTopLeftAlign();
	}
	
	public void setRight(int newValue) {
		this.right = newValue;
		this.width = this.right - this.left;
		
		if (this.width <= this.textWidth) {
			
			//we need to break the text to next line
			int index = paint.breakText(this.text, true, width, null);
						
//			Log.d(TAG, "index: "+index);
			String oldText = this.text;
			this.brokenText = oldText.substring(0, index);
//			Log.d(TAG, "oldText.substring(0, index):"+ oldText.substring(0, index));
			//clear old lines from array
			strings.clear();
			strings.add(0, brokenText);
			
//			Log.d(TAG, "oldText.substring(index):"+ oldText.substring(index));
			
			if (index+1 <= oldText.length() && index != 0)		
				linebreak(oldText.substring(index), index);			
			
			if (index == oldText.length()-1) {
				this.brokenText = oldText;
				strings.clear();
				strings.add(0, this.text);
			}

			setLinebreak();
			setTopLeftAlign();			
		}
	}
	
	private void linebreak(String leftover, int index) {
//		Log.d(TAG, "leftover: "+leftover);
		int charsLeft = leftover.length();
//		Log.d(TAG, "charsLeft: "+leftover.length()+" index: "+index);
		int linesNeeded = 0;
		if (index > 0) 
			linesNeeded = charsLeft/index;
//		Log.d(TAG, "linesNeeded: "+linesNeeded);

		if (charsLeft > 0) {
			for (int i = 0; i<=linesNeeded; i++) {
				if (strings.size()>i+1)
					strings.set((i+1), leftover);
				else 
					strings.add((i+1), leftover);
				
//				Log.d(TAG, "indexOfleftover: "+ strings.indexOf(leftover));
				
				//if true means it's too long for our rectangle, shorten
				if (leftover.length() > index) {
					String nextLine = leftover.substring(0, index);
					strings.add(strings.indexOf(leftover), nextLine);
					Log.d(TAG, "nextLine: "+ nextLine);
					charsLeft = nextLine.length() - index + 1;
					leftover = leftover.substring(index);
					Log.d(TAG, "leftover: "+ leftover);
				}
				else {
					charsLeft = leftover.length() - index + 1;
					break;
				}
			}
		}
	}
	
	
	private void newLine (String toShorten) {
		int index1 = paint.breakText(toShorten, true, this.width, null);
//		Log.d(TAG, "index1: "+index1);
		String shortened;
		String newLine = "";
		shortened = toShorten.substring(0, index1);
		strings.add(strings.indexOf(toShorten), shortened);
//		Log.d(TAG, "arrayInd: "+strings.indexOf(toShorten));
		
//		Log.d(TAG, "toShorten.len:"+toShorten.length());
		if (index1 <= toShorten.length()) {
			newLine = toShorten.substring(index1);
//			this.stringCount = 3;
//			this.lineReturn = 2;
			strings.add(strings.indexOf(toShorten)+1, newLine);
		}
		if (shortened.length() < newLine.length() ) {
			//means we need to now shorten the newLine -> recursively call ourselves!
			newLine(newLine);
		}
	}
	
	public void setLinebreak() {
		linebreak = new Point();
    	this.linebreak.x = (int)this.left;
    	this.linebreak.y = (int)this.topLeftAlign+((int)paint.getTextSize()+(int)paint.descent());
//    	this.linebreak.y = (int)this.topLeftAlign+((int)paint.getTextSize()+(int)paint.descent())*this.lineReturn;
   	}
	
	public int getLinebreakY(int multiplier) {
		int linebreakY = (int)this.topLeftAlign+((int)paint.getTextSize()+(int)paint.descent())*multiplier;
		return linebreakY;
	}
	
	private void setTopLeftAlign() {
		this.topLeftAlign = this.top + paint.getTextSize()+paint.descent(); 
	}
	
	
	public int getY() {
		return this.centerY;
	}
	
	public int getID() {
		return id;
	}
	
	public Bitmap getBitmap() {
		return img;
	}
	
	public void moveRect(int goX, int goY) {
		// check the borders, and set the direction if a border has reached
		if (centerX > 270){
			goRight = false;
		}
		if (centerX < 0){
			goRight = true;
		}
		if (centerY > 400){
			goDown = false;
		}
		if (centerY < 0){
			goDown = true;
		}
		// move the x and y 
		if (goRight){
			centerX += goX;
		}else
		{
			centerX -= goX;
		}
		if (goDown){
			centerY += goY;
		}else
		{
			centerY -= goY;
		}
		
	}
	
}
