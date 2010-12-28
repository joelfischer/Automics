 package mrl.automics.graphics;
 
 import mrl.automics.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Paint.Style;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

 // This class is used by CropImage to display a highlighted cropping rectangle
 // overlayed with the image. There are two coordinate spaces in use. One is
 // image, another is screen. computeLayout() uses mMatrix to map from image
 // space to screen space.
 
public class CropView extends View {

	private static final String TAG = "CropView";
	private static final float CORRECT = 12.5f;
	MyRectF selection;  // in screen space
	private Paint paint;
	private Path path;
	private boolean showTooltips;
	private boolean move; 
	private boolean resize;
	private static final int LANDSCAPE_LANDSCAPE = 1;  //i.e. screen_photo
	private static final int LANDSCAPE_PORTRAIT = 2;
	private static final int PORTRAIT_LANDSCAPE = 3;
	private static final int PORTRAIT_PORTRAIT = 4;
	private int maxWidth;
	private int minWidth;	//the min width of the crop rect on screen
	private static final int MIN_PX_WIDTH = 800; //the minimum width in pixels of the resulting cropped image
	private static final float LANDSCAPE_RATIO = 4/3f;
	private static final float PORTRAIT_RATIO = 3/4f;
	private Paint transparentGray;
	private RectF imageRect;
	public float onScreenWidth;
	public float onScreenHeight;
	public float left, top, right, bottom;
	  
	/**
	 * @param context - the instance of the calling activity
	 * @param w - width of the bitmap on screen (scaled - but not = screen size!)
	 * @param h - height of the bitmap on screen (scaled - but not = screen size!)
	 * @param type - either one of the four combinations of LANDSCAPE_PORTRAIT
	 * @param metrics - DisplayMetrics 
	 * @param oriW - the width of the original image to be cropped (unscaled)
	 * @param ivHeight - the height of the image view displaying the image on screen
	 */
  	public CropView(Context context, int w, int h, int type, DisplayMetrics metrics, 
  			int oriW, int ivHeight, int ivWidth) {
  		super(context);
  		
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.MAGENTA);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2);
        
        //overlay the rest of the image with transparent gray
        transparentGray = new Paint();
  		transparentGray.setColor(Color.LTGRAY);
  		transparentGray.setAlpha(150);
        
        path = new Path();
        
        showTooltips = true;
        
        float scaleY;
        float scaleX;
        onScreenWidth = 0;
        onScreenHeight = 0;
                
        //constrain the min size of the cropping rect, and thereby the 
        //resulting cropped image size from a combination of original image size, 
        //screen and photo orientation 
        switch (type) {        	
	        case LANDSCAPE_LANDSCAPE: 
	        	scaleY = (h-ivHeight) * LANDSCAPE_RATIO;
				onScreenWidth = w - scaleY;
				onScreenHeight = ivHeight;
//				left = (ivWidth - onScreenWidth)/2;
				left = 0;
				top = 0;
				right = left + onScreenWidth;
				bottom = ivHeight;
				imageRect = new RectF(left, top, right, bottom);	
				this.minWidth = (int) (onScreenWidth * (float)MIN_PX_WIDTH/(float)oriW);
	        	Log.d(TAG, "minWidth: "+minWidth);
	        	break;
	        case LANDSCAPE_PORTRAIT:
	        	scaleY = (h-ivHeight) * PORTRAIT_RATIO;
				onScreenWidth = w - scaleY;
				onScreenHeight = ivHeight;
//				left = (ivWidth - onScreenWidth)/2;
				left = 0;
				top = 0;
				right = left + onScreenWidth;
				bottom = ivHeight;
				imageRect = new RectF(left, top, right, bottom);	
				this.minWidth = (int) (onScreenWidth * (float)MIN_PX_WIDTH/(float)oriW);
	        	Log.d(TAG, "minWidth: "+minWidth);
	        	break;
	        case PORTRAIT_LANDSCAPE: 
	        	scaleX = (w-ivWidth) * PORTRAIT_RATIO;
	        	onScreenHeight = h-scaleX;
	        	onScreenWidth = ivWidth;
	        	left = 0;
				top = (ivHeight-onScreenHeight)/2;;
				right = ivWidth;
				bottom = top + onScreenHeight;
				imageRect = new RectF(left, top, right, bottom);	
	        	this.maxWidth = metrics.widthPixels;
	        	this.minWidth = (int) (metrics.widthPixels * (float)MIN_PX_WIDTH/(float)oriW);
	        	Log.d(TAG, "minWidth: "+minWidth);
	        	break;
	        case PORTRAIT_PORTRAIT:
	        	scaleX = (w - ivWidth) * LANDSCAPE_RATIO;
	        	onScreenHeight = h-scaleX;
	        	onScreenWidth = ivWidth;
	        	left = 0;
				top = (ivHeight-onScreenHeight)/2;;
				right = ivWidth;
				bottom = top + onScreenHeight;
				imageRect = new RectF(left, top, right, bottom);
	        	this.maxWidth = metrics.widthPixels;
	        	this.minWidth = (int) (metrics.widthPixels * (float)MIN_PX_WIDTH/(float)oriW);
	        	Log.d(TAG, "minWidth: "+minWidth);
	        	break;
        }
      //init according to the image's on screen w, h
        
        int cropWidth = Math.min((int)onScreenWidth, (int)onScreenHeight) * 4 / 5;
        if (cropWidth < minWidth) 
        	cropWidth = minWidth;
  		
        int cropHeight = cropWidth * 3/4;
          
        int x = (int)imageRect.left + 10;
        int y = (int)imageRect.top + 10;
        selection = new MyRectF(x, y, x + cropWidth, y + cropHeight);
        
  	}
  	
  	@Override
  	protected void onDraw(Canvas canvas) {

  		canvas.save();
  		canvas.clipRect(selection, Region.Op.DIFFERENCE);
  		canvas.drawPaint(transparentGray);
  		canvas.drawRect(selection, paint);
  		canvas.restore();
  		if (showTooltips)
  			drawTooltips(canvas, selection);
    } 
  	
  	private void drawTooltips (Canvas canvas, RectF selection) {
    	
    	canvas.save();
    	
    	Bitmap tooltipMove = BitmapFactory.decodeResource(this.getResources(), R.drawable.move_small);
    	Bitmap tooltipResize = BitmapFactory.decodeResource(this.getResources(), R.drawable.resize_small);
    	
    	canvas.drawBitmap(tooltipMove, selection.centerX()-CORRECT, selection.centerY()-CORRECT, null);
    	canvas.drawBitmap(tooltipResize, selection.right-CORRECT, selection.bottom-CORRECT, null);
//    	canvas.drawCircle(rect.right, rect.bottom, 5, paint);
//    	canvas.drawCircle(rect.centerX(), rect.centerY(), 5, paint);
    	
    	canvas.restore();
    }

     
     
     @Override
     public boolean onTouchEvent(MotionEvent event) {
    	 
    	 showTooltips = true;
    	 
    	 int X = (int)event.getX(); 
         int Y = (int)event.getY(); 
         
         //debug
//         Log.d(TAG, "X: "+X +", Y: "+Y);
         
    	 switch (event.getAction()) {
    	 	case MotionEvent.ACTION_DOWN: 
    	 		double radCircle  = Math.sqrt( (double) (((selection.right-X)*(selection.right-X)) 
    	 				+ (selection.bottom-Y)*(selection.bottom-Y)));
        		
        		if (radCircle < 40){
        			resize = true;
        			Log.d(TAG, "resize");
                    break;
        		}
        		        		
        		//check if we're inside the rectangle and want to drag it
        		boolean insideRect = selection.contains(X, Y);
	        		if (insideRect) {
	        			move = true;
	        			Log.d(TAG, "move");
	        			break;
	        		}
    	 		break;
    	 		
    	 	case MotionEvent.ACTION_UP:
    	 		resize = false;
    	 		move = false;
    	 		
    	 		break;
    	 	
    	 	case MotionEvent.ACTION_MOVE:
    	 		//we have a touch if
    	 		Log.d(TAG, "ACTION_MOVE");
    	 		showTooltips = false;
    	 		
	 			if (resize) {
	 				
	 				float w = X - selection.left;
	 	            float _right = X;
	 	            float _bottom = Y;
	 				if (_bottom < imageRect.bottom &&
	 					_right < imageRect.right &&
	 					w >= this.minWidth) {
		 				selection.right = X;
		 				//maintain rect with 4/3 aspect ratio
		 				selection.bottom = selection.top + 
		 									(selection.width() * 3f/4f);
		 				//TODO: constrain max width, avoid resizing out of bounds
	 				}
	 				
	 			}
	 			else if (move) {
	 				float width = selection.width;
	 	    		float height = selection.height;
	 	            float left = X - (float)0.5*width;
	 	            float right = X + (float)0.5*width;
	 	            float top = Y - (float)0.5*height;
	 	            float bottom = Y + (float)0.5*height;
	 				if (top >= imageRect.top &&
	 						left >= imageRect.left &&
	 						bottom <= imageRect.bottom &&
	 						right <= imageRect.right) {
	 					selection.setCenter(X, Y);
	 				}
	 			}
    	 		
    	 		break;
    	 }
    	 
    	 invalidate();
    	 return true;
     }
     
     public MyRectF getCropRect() {
    	 return this.selection;
     }
   
     
    public class MyRectF extends RectF {
    	
    	private float width;
    	private float height;
    	
    	public MyRectF (int x, int y, int w, int h) {
    		super(x, y, w, h);
    	}
    	
    	public void setCenter(float X, float Y) {
    		width = width();
    		height = height();
            this.left = X - (float)0.5*this.width;
            this.right = X + (float)0.5*this.width;
            this.top = Y - (float)0.5*this.height;
            this.bottom = Y + (float)0.5*this.height;
    	}
    	
    }
   
 }
    


