 package mrl.automics.graphics;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Paint.Style;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

 // This class creates two black rectangles to overlay the BubbleView so the 
 //bubbles will be placed on the actual image. 
 
public class ImageFrame extends View {

	private static final String TAG = "ImageFrame";
	private Paint paint;
	private static final int LANDSCAPE_LANDSCAPE = 1;  //i.e. screen_photo
	private static final int LANDSCAPE_PORTRAIT = 2;
	private static final int PORTRAIT_LANDSCAPE = 3;
	private static final int PORTRAIT_PORTRAIT = 4;
	private static final float LANDSCAPE_RATIO = 4/3f;
	private static final float PORTRAIT_RATIO = 3/4f;
	private int type;
	public float onScreenWidth;
	public float onScreenHeight;
	public RectF rect1;
	public RectF rect2;
	  
	  
  	public ImageFrame(Context context, int imageW, int imageH, int imageViewW, int imageViewH) {
  		super(context);
  		
  		//init according to image view w, h
  		if (imageViewW > imageViewH) {
			//screen in landscape orientation
        	if (imageH > imageW ) {
				//image is portrait, thus
				this.type = LANDSCAPE_PORTRAIT;
			}
			else if (imageH < imageW ) {
				//image is landscape
				this.type = LANDSCAPE_LANDSCAPE;
			}
        }
		else if (imageViewW < imageViewH) {
			//screen in portrait orientation
			if (imageH > imageW ) {
				//image is portrait
				this.type = PORTRAIT_PORTRAIT;
			}
			else if(imageH < imageW ) {
				//image is landscape 
				this.type = PORTRAIT_LANDSCAPE;
			}
		}

        paint = new Paint();
        paint.setColor(Color.BLACK);
                        
        float scaleY;
        float scaleX;
        float x1;
        float x2;
        float y1;
        float y2;
        
        //draw black borders so we make sure the user places 
        //the bubbles on the photo, use a combination of original image size, 
        //screen and photo orientation 
        switch (type) {        	
	        case LANDSCAPE_LANDSCAPE: 
	        	scaleY = (imageH-imageViewH) * LANDSCAPE_RATIO;
				onScreenWidth = imageW - scaleY;
				onScreenHeight = imageViewH;
				x1 = (imageViewW - onScreenWidth)/2;
				x2 = x1 + onScreenWidth;
				rect1 = new RectF(0, 0, x1, imageViewH);
				rect2 = new RectF(x2, 0, imageViewW, imageViewH);
	        	break;
	        case LANDSCAPE_PORTRAIT:
	        	scaleY = (imageH-imageViewH) * PORTRAIT_RATIO;
				onScreenWidth = imageW - scaleY;
				onScreenHeight = imageViewH;
				x1 = (imageViewW - onScreenWidth)/2;
				x2 = x1 + onScreenWidth;
				rect1 = new RectF(0, 0, x1, imageViewH);
				rect2 = new RectF(x2, 0, imageViewW, imageViewH);
	        	break;
	        case PORTRAIT_LANDSCAPE: 
	        	scaleX = (imageW-imageViewW) * PORTRAIT_RATIO;
	        	onScreenHeight = imageH-scaleX;
	        	onScreenWidth = imageViewW;
	        	y1 = (imageViewH - onScreenHeight) /2;
	        	y2 = y1 + onScreenHeight;
	        	rect1 = new RectF(0, 0, imageViewW, y1);
	        	rect2 = new RectF(0, y2, imageViewW, imageViewH);
	        	break;
	        case PORTRAIT_PORTRAIT:
	        	scaleX = (imageW-imageViewW) * LANDSCAPE_RATIO;
	        	onScreenHeight = imageH-scaleX;
	        	onScreenWidth = imageViewW;
	        	y1 = (imageViewH - onScreenHeight) /2;
	        	y2 = y1 + onScreenHeight;
	        	rect1 = new RectF(0, 0, imageViewW, y1);
	        	rect2 = new RectF(0, y2, imageViewW, imageViewH);
	        	break;
        }
        
  	}
  	
  	@Override
  	protected void onDraw(Canvas canvas) {

  		canvas.drawRect(rect1, paint);
  		canvas.drawRect(rect2, paint);
  		
  	} 
  	

 
   
 }
    


