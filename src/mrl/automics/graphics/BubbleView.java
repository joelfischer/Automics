package mrl.automics.graphics;

import mrl.automics.R;
import mrl.automics.ui.ChosenImage_Activity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.Path.Direction;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore.Images;
import android.text.TextPaint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class BubbleView extends View {
	
	private static int count = 0;
	private static final float CORRECT = 12.5f;
	
//   private ColorBall[] colorballs = new ColorBall[3]; // array that holds the balls
   private ArrayList <MyRect> myRects = new ArrayList<MyRect>();
   private ArrayList <MySickle> mySickles = new ArrayList<MySickle>();
   private ArrayList <MyThoughtBubbles> myThoughtBubbles = new ArrayList<MyThoughtBubbles>();
   private ArrayList <MyTriangle> myTriangles = new ArrayList <MyTriangle>();
//   private MySickle[] mySickles = new MySickle[1];
//   private MyThoughtBubbles[] myThoughtBubbles = new MyThoughtBubbles[1];
   
//   private MyRect mRect;
   private MySickle mSickle;
   private MyThoughtBubbles mThoughtBubbles;
   private MyTriangle mTri;
//   private int balID = 0; // variable to know what ball is being dragged
   private int rectId = 0;
   private int sickleId = 0;
   private int thoughtBubblesId = 0;
   private int triangleId;
   private static final String TAG = "BubbleView";
   private Paint rectPaint;
   private TextPaint textPaint;
   private Paint strokePaint;
   private boolean resize;
   private boolean sickle_resize;
   private boolean thoughtBubbles_resize;
   private boolean triangle_resize;
   private ArrayList<View> bubbles;
   private ArrayList<View> textViews;
   private boolean speech;
   private boolean thought;
   private boolean scene;
   private boolean showTooltips;
   private Path path;
   
   private Paint debugPaint;
   private Typeface tf;
   
   private int toSaveWidth;
   private int toSaveHeight;
   public ProgressDialog progress;
   private Context context;
   private RectF imageOnScreen;
   private ChosenImage_Activity cia;
   
   
    
    public BubbleView(Context context, ArrayList<String> bubbleTexts, ArrayList<Integer>bubbleTypes) {
        super(context);
        setFocusable(true); //necessary for getting the touch events
        
        // setting the start point for the bubble
        Point point1 = new Point();
        point1.x = 150;
        point1.y = 100;
        
        this.context = context;
        
        
        //init font
        tf = Typeface.createFromAsset(context.getAssets(), 
        								"fonts/blambotcustom.ttf");
        
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setColor(Color.WHITE);
        
        strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setColor(Color.BLACK);
        
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(25);
//        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        //custom font
        textPaint.setTypeface(tf);

        for (int i = 0; i<bubbleTexts.size(); i++) {
        	Log.d(TAG, "i: "+i +", size: "+bubbleTexts.size());
	        MyRect mRect =  new MyRect(context, bubbleTexts.get(i), point1, textPaint, bubbleTypes.get(i), i+1);
	        myRects.add(mRect);
	        
	        Log.d(TAG, "mRect ID: "+mRect.getID());
	        
	        //create and add them to array to simplify recall from array but only draw them later if they're the right type
	    	mSickle = new MySickle(context, mRect);
	    	mySickles.add(mSickle);
	    	
	    	mTri = new MyTriangle(context, mRect);
	    	myTriangles.add(mTri);
	    	
	        
	    	mThoughtBubbles = new MyThoughtBubbles(context, mRect);
	    	myThoughtBubbles.add(mThoughtBubbles);
        }

        
//        myRects[0] = new MyRect(context, "I am a scene", point1, textPaint, MyRect.SCENE);
//        myRects[1] = new MyRect(context, "I am a speech bubble", point2, textPaint, MyRect.SPEECH);
//        myRects[2] = new MyRect(context, "I am a thought bubble", point3, textPaint, MyRect.THOUGHT);
        
        //construct Sickles - one based on each rectangle
//        mySickles[0] = new MySickle(context, myRects[0]);
//        mySickles[1] = new MySickle(context, myRects[1]);
//        mySickles[2] = new MySickle(context, myRects[2]);
        
        //constructs thought bubbles (or the line for one...), one for each rectangle
//        myThoughtBubbles[0] = new MyThoughtBubbles(context, myRects[0]);
//        myThoughtBubbles[1] = new MyThoughtBubbles(context, myRects[1]);
//        myThoughtBubbles[2] = new MyThoughtBubbles(context, myRects[2]);
        
        path = new Path();
        
        debugPaint = new Paint();
		debugPaint.setColor(Color.RED);
        
		this.setFocusableInTouchMode(true);
		this.setFocusable(true);
        

    }
    
    public ArrayList <MyRect> getRects() {
    	return this.myRects;
    }
    
    public ArrayList <MyTriangle> getTriangles() {
    	return this.myTriangles;
    }
    
    public ArrayList <MyThoughtBubbles> getThoughtBubbles() {
    	return this.myThoughtBubbles;
    }
//    
//    public void addBubble(Context context, String bubbleText, int type) {
//    	Point point1 = new Point();
//        point1.x = 100;
//        point1.y = 100;
//        
//    	MyRect mRect =  new MyRect(context, bubbleText, point1, textPaint, type);
//        myRects.add(mRect);
//        
//    	mSickle = new MySickle(context, mRect);
//    	mySickles.add(mSickle);
//        
//    	mThoughtBubbles = new MyThoughtBubbles(context, mRect);
//    	myThoughtBubbles.add(mThoughtBubbles);
//    }
    
    
//    @Override protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
//    	Log.d(TAG, "width: "+ widthMeasureSpec + " , height: "+heightMeasureSpec);
//    	setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
//    }
    
  
    @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    	super.onLayout(changed, left, top, right, bottom);
    }
    
    // the method that draws the balls
    @Override protected void onDraw(Canvas canvas) {
    	
    	for (MyRect rect : myRects) {
    	
	    	if (rect.speech)
	    		drawTriangle(canvas, rect);
//				drawSickle(canvas, rect);
			else if (rect.thought)
				drawThoughtBubbles(canvas, rect);
			
			//draw oval according to String size...
			if (!rect.scene) {
				//underlying oval for "stroke" around the oval
				canvas.drawOval(new RectF(rect.left-40, rect.top-40, rect.right+40, rect.bottom+40), strokePaint);
				//the white text container bubble oval
				canvas.drawOval(new RectF(rect.left-38, rect.top-38, rect.right+38, rect.bottom+38), rectPaint);
			}
			//if type is scene draw rounded rect instead...
			else if (rect.scene) {
				canvas.drawRoundRect(new RectF(rect.left-32, rect.top-32, rect.right+32, rect.bottom+32), 5, 5, strokePaint);
				canvas.drawRoundRect(new RectF(rect.left-30, rect.top-30, rect.right+30, rect.bottom+30), 5, 5, rectPaint);
			}
			//add String to speech bubble...
			for (int i = 0; i<rect.strings.size(); i++) {
	//			Log.d(TAG, "rect.strings.size:" +rect.strings.size());
				if (i == 0) {
					canvas.drawText(rect.brokenText, rect.left, rect.topLeftAlign, textPaint);
				}
				else {
					//draw text if inside the rectangle
					if (rect.contains(rect.linebreak.x+1, rect.getLinebreakY(i))) { 
						canvas.drawText(rect.strings.get(i), rect.linebreak.x, rect.getLinebreakY(i), textPaint);
					}
				}
			}
			
			if (showTooltips)
				drawTooltips(canvas, rect);
    	}
    	
    	
		//background 
//    	canvas.drawColor(Color.GRAY);

    	
//    	for (MyRect rect : myRects) {
//    		
//    		if (rect.speech)
//    			drawSickle(canvas, rect);
//    		else if (rect.thought)
//    			drawThoughtBubbles(canvas, rect);
//    		
//    		//draw oval according to String size...
//    		if (!rect.scene)
//    			canvas.drawOval(new RectF(rect.left-30, rect.top-30, rect.right+30, rect.bottom+30), rectPaint);
//    		else if (rect.scene) 
//    			canvas.drawRoundRect(new RectF(rect.left-30, rect.top-30, rect.right+30, rect.bottom+30), 5, 5, rectPaint);
//    		
//    		//add String to speech bubble...
//    		for (int i = 0; i<rect.strings.size(); i++) {
////    			Log.d(TAG, "rect.strings.size:" +rect.strings.size());
//    			if (i == 0) {
//    				canvas.drawText(rect.text0, rect.left, rect.topLeftAlign, textPaint);
//    			}
//    			else {
//    				//draw text if inside the rectangle
//    				if (rect.contains(rect.linebreak.x+1, rect.getLinebreakY(i))) { 
//    					canvas.drawText(rect.strings.get(i), rect.linebreak.x, rect.getLinebreakY(i), textPaint);
//    				}
//    			}
//    		}
//    		//debug
////    		canvas.drawCircle(rect.right, rect.bottom, (float)5, debugPaint);
//    				
//    	}
    }
    
    /**
     * @param int height: the true height (where 1=1px) of the photo to draw on. 
     * @param int width: the true width (where 1=1px) of the photo to draw on.
     * @param Canvas canvas: the canvas to draw on, should already be inititalized with 
     * Canvas canvas = new Canvas(croppedImage);
     * @param Rect: the image bounds on the screen.
     */
    public void saveView(int h, int w, Canvas canvas, RectF rect, ChosenImage_Activity act) {
    	
    	progress = ProgressDialog.show(context, "Saving...", "Please wait", true);
    	toSaveWidth = w;
    	toSaveHeight = h;
    	imageOnScreen = rect;
    	cia = act;
    	new WorkerTask().execute(canvas);
    }
    
    private void drawTooltips (Canvas canvas, MyRect rect) {
    	
    	canvas.save();
    	
    	Bitmap tooltipMove = BitmapFactory.decodeResource(this.getResources(), R.drawable.move_small);
    	Bitmap tooltipResize = BitmapFactory.decodeResource(this.getResources(), R.drawable.resize_small);
    	
    	canvas.drawBitmap(tooltipMove, rect.centerX()-CORRECT, rect.centerY()-CORRECT, null);
    	canvas.drawBitmap(tooltipResize, rect.right-CORRECT, rect.bottom-CORRECT, null);
//    	canvas.drawCircle(rect.right, rect.bottom, 5, debugPaint);
//    	canvas.drawCircle(rect.centerX(), rect.centerY(), 5, debugPaint);
    	
    	if (rect.speech) {
    		//we need the triangle
    		mTri = myTriangles.get(rect.getID()-1);
//    		canvas.drawCircle(mTri.bottomX, mTri.bottomY, 5, debugPaint); 
    		
    		canvas.drawBitmap(tooltipMove, mTri.bottomX-CORRECT, mTri.bottomY-CORRECT, null);
    	}
    	if (rect.thought) {
    		mThoughtBubbles = myThoughtBubbles.get(rect.getID()-1);
//    		canvas.drawCircle(mThoughtBubbles.endX, mThoughtBubbles.endY, 5, debugPaint);
    		canvas.drawBitmap(tooltipMove, mThoughtBubbles.endX-CORRECT, mThoughtBubbles.endY-CORRECT, null);

    	}
    	
    	canvas.restore();
    }
    
    private void drawTriangle(Canvas canvas, MyRect rect) {
    	canvas.save();
    	
    	int triangleId = rect.getID();
    	mTri = myTriangles.get(triangleId - 1);
    	
    	path.reset();
    	
		path.moveTo(mTri.topLeftX, mTri.topLeftY);
		path.lineTo(mTri.topRightX, mTri.topRightY);
		path.lineTo(mTri.bottomX, mTri.bottomY);
		path.close();
		
		Paint triPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		triPaint.setColor(Color.BLACK);
		triPaint.setStyle(Paint.Style.STROKE);
		triPaint.setStrokeWidth(2);
		canvas.drawPath(path, rectPaint);
		canvas.drawPath(path, triPaint);
		
		canvas.restore();
    	
    	
    }
    
    private void drawSickle(Canvas canvas, MyRect rect) {
    	//add "sickle" to speech bubble
		canvas.save();
//		canvas.translate(rect.left, rect.bottom);
		int sickleId = rect.getID();
		mSickle = mySickles.get(sickleId-1);
		
		//reset path before (re-) drawing
		path.reset();
		
		//add offset oval to path for subtraction from Oval to create "sickle"
		path.addOval(new RectF(mSickle.left-20, 
					mSickle.top,
					mSickle.right-20,
					mSickle.bottom), Direction.CCW);
			
//		path.addOval(new RectF(mySickles[sickleId-1].left-20, 
//								mySickles[sickleId-1].top,
//								mySickles[sickleId-1].right-20,
//								mySickles[sickleId-1].bottom), Direction.CCW);
		//add more clipping...
		path.addRect(mSickle.left-30,
				mSickle.top-5, 
				mSickle.right, 
				rect.bottom + 10, Direction.CCW);
		
//		path.addRect(mySickles[sickleId-1].left-30,
//				mySickles[sickleId-1].top-5, 
//				mySickles[sickleId-1].right, 
//				rect.bottom + 10, Direction.CCW);

		canvas.clipPath(path, Region.Op.DIFFERENCE);
		//draw the actual oval, the path will be clipped from it already
		//first a black "stroke" one underneath
		canvas.drawOval(new RectF(mSickle.left, 
				mSickle.top,
				mSickle.right+2,
				mSickle.bottom+1), strokePaint);
		//the white "sickle filling"
		canvas.drawOval(new RectF(mSickle.left, 
				mSickle.top,
				mSickle.right,
				mSickle.bottom), rectPaint);
		//we need another oval to the left to get the left side of the stroke...
		canvas.drawOval(new RectF(mSickle.left,
				mSickle.top, 
				mSickle.right-19, 
				mSickle.bottom), strokePaint);
		
//		canvas.drawOval(new RectF(mySickles[sickleId-1].left, 
//								mySickles[sickleId-1].top,
//								mySickles[sickleId-1].right,
//								mySickles[sickleId-1].bottom), rectPaint);
		//debug
//		canvas.drawCircle(mySickles[sickleId-1].right-mySickles[sickleId-1].width()/2,
//				mySickles[sickleId-1].bottom, (float)5, debugPaint);
		canvas.restore();

    	
    }
    
    private void drawThoughtBubbles(Canvas canvas, MyRect rect) {
    	//add "thought bubbles" to oval
    	canvas.save();
    	
    	int thoughtBubblesId = rect.getID();
    	
    	//reset path before (re-) drawing
		path.reset();
		
		mThoughtBubbles = myThoughtBubbles.get(thoughtBubblesId-1);
		
		float length = mThoughtBubbles.length;
		float startX = mThoughtBubbles.startX;
		float startY = mThoughtBubbles.startY;
		float endX = mThoughtBubbles.endX;
		float endY = mThoughtBubbles.endY;
		
//		float length = myThoughtBubbles[thoughtBubblesId-1].length;
//		float startX = myThoughtBubbles[thoughtBubblesId-1].startX;
//		float startY = myThoughtBubbles[thoughtBubblesId-1].startY;
//		float endX = myThoughtBubbles[thoughtBubblesId-1].endX;
//		float endY = myThoughtBubbles[thoughtBubblesId-1].endY;
		
//		canvas.drawLine(startX, startY, endX, endY, rectPaint);
		
		float X = startX;
		float Y = startY;
		
		//constant distance between the bubbles...
		final float K = 10;
		
		//radius of initial circle
		float radius = 20;
		
		while (length > 0 && radius > 0) {
			
			//draw bubbles along the line
			//start with the initial one and then decrease them in size depending on lenght of line
			//the black "stroke"
			canvas.drawCircle(X, Y, radius+2, strokePaint);
			//the white "circle"
			canvas.drawCircle(X, Y, radius, rectPaint);
			
			//find the coordinates for the next circle...()
			float distanceToNewCenter = radius + K + radius - 7;
			
			//linear interpolation! get the larp...
			double newX = (X + ((endX - X) * distanceToNewCenter/length ));
			double newY = (Y + ((endY - Y) * distanceToNewCenter/length ));
			
			X = (float) newX;
			Y = (float) newY;
			
			//new radius
			radius = radius - 3;
			
			//new length 
			length = length - distanceToNewCenter;
			
//			Log.d(TAG, "length:  "+length);
		}		
		canvas.restore();
    }
    
    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();         
        
//        Log.d(TAG, "isFocusableInTouchMode:" + this.isFocusableInTouchMode());

        
        int X = (int)event.getX(); 
        int Y = (int)event.getY(); 
        
        switch (eventaction) { 

        case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on element

        	rectId = 0;
        	sickleId = 0;
        	thoughtBubblesId = 0;
        	triangleId = 0;
        	
//        	//check if we want to perform a resize of the bubble
//    		//condition: if X/Y is within a radius of 30px of lower right corner
//    		double radCircle  = Math.sqrt( (double) (((mRect.right-X)*(mRect.right-X)) + (mRect.bottom-Y)*(mRect.bottom-Y)));
//    		        		
//    		if (radCircle < 30){
//    			rectId = mRect.getID();
//    			resize = true;
//    			Log.d(TAG, "resize");
//                break;
//    		}
//    		        		
//    		//check if we're inside the rectangle and want to reposition it
//    		boolean insideRect = mRect.contains(X, Y);
//    		if (insideRect) {
//    			rectId = mRect.getID();
//    			break;
//    		}
        	for (MyRect rect : myRects) {
        		
        		//check if we want to perform a resize of the bubble
        		//condition: if X/Y is within a radius of 30px of lower right corner
        		double radCircle  = Math.sqrt( (double) (((rect.right-X)*(rect.right-X)) + (rect.bottom-Y)*(rect.bottom-Y)));
        		        		
        		if (radCircle < 40){
        			rectId = rect.getID();
        			resize = true;
        			Log.d(TAG, "resize");
                    break;
        		}
        		        		
        		//check if we're inside the rectangle and want to drag it
        		boolean insideRect = rect.contains(X, Y);
        		if (insideRect) {
        			rectId = rect.getID();
        			break;
        		}
        	
        		
        		//substitute sickle by triangle...
//	    		if (rect.speech) {
//		    		//check if we want to perform a resize of the sickle
//		    		//condition: if X/Y is within a radius of 30px of half the width of bottom (tip of sickle)
//	    			mSickle = mySickles.get(rect.getID()-1);
//	    			
//		    		radCircle  = Math.sqrt( (double) (((mSickle.right-mSickle.width()/2-X)*
//		    				(mSickle.right-mSickle.width()/2-X)) + 
//		    				(mSickle.bottom-Y)*(mSickle.bottom-Y)));
//		    		        		
//		    		if (radCircle < 30){
//		    			rectId = rect.getID();
//		    			sickle_resize = true;
//		    			sickleId = mSickle.getId();
//		    			Log.d(TAG, "sickle resize");
//		                break;
//		    		}
//	    		}
	    		
	    		if (rect.speech) {
		    		//check if we want to perform a resize of the triangle
		    		//condition: if X/Y is within a radius of 30px of the bottom corner of the triangle
	    			mTri = myTriangles.get(rect.getID()-1);
	    			
		    		radCircle  = Math.sqrt( (double) (((mTri.bottomX-X)*(mTri.bottomX-X)) + 
		    				(mTri.bottomY-Y)*(mTri.bottomY-Y)));
		    		        		
		    		if (radCircle < 40){
		    			rectId = rect.getID();
		    			triangle_resize = true;
		    			triangleId = mTri.getId();
		    			Log.d(TAG, "triangle resize");
		                break;
		    		}
	    		}
    		
//        	for (MySickle sickle : mySickles) {
//    		//check if we want to perform a resize
//    		//condition: if X/Y is within a radius of 20px of half the width of bottom (tip of sickle)
//    		double radCircle  = Math.sqrt( (double) (((sickle.right-sickle.width()/2-X)*
//    				(sickle.right-sickle.width()/2-X)) + 
//    				(sickle.bottom-Y)*(sickle.bottom-Y)));
//    		        		
//    		if (radCircle < 20){
//    			sickle_resize = true;
//    			sickleId = sickle.getId();
//    			Log.d(TAG, "sickle resize");
//                break;
//    		}
//    	}
    	
    	
	    	if (rect.thought) {
	    		//check if we want to perform a resize of the thought bubbles
	    		//condition: if X/Y is within a radius of 30px of the endpoint of the line along the bubbles
	    		mThoughtBubbles = myThoughtBubbles.get(rect.getID()-1);
	    		
	    		radCircle  = Math.sqrt( (double) (((mThoughtBubbles.endX-X)*(mThoughtBubbles.endX-X)) + 
	    												(mThoughtBubbles.endY-Y)*(mThoughtBubbles.endY-Y)));
	    		        		
	    		if (radCircle < 40){
	    			rectId = rect.getID();
	    			thoughtBubbles_resize = true;
	    			thoughtBubblesId = mThoughtBubbles.getId();
	    			Log.d(TAG, "thoughtBubbles resize");
	                break;
	    		}
	    	}
        	

//        	
//        	for (MyThoughtBubbles tb : myThoughtBubbles) {
//        		//check if we want to perform a resize
//        		//condition: if X/Y is within a radius of 20px of the endpoint of the line along the bubbles
//        		double radCircle  = Math.sqrt( (double) (((tb.endX-X)*(tb.endX-X)) + 
//        												(tb.endY-Y)*(tb.endY-Y)));
//        		        		
//        		if (radCircle < 20){
//        			thoughtBubbles_resize = true;
//        			thoughtBubblesId = tb.getId();
//        			Log.d(TAG, "thoughtBubbles resize");
//                    break;
//        		}
//        	}
//             
//    	if (rectId == 0) {
//    		//maybe user touches another view/bubble
//    		Log.d(TAG, "rectId: "+rectId);
//    		
//    		this.clearFocus();
    		
//    		FocusFinder ff = FocusFinder.getInstance();
//    		FrameLayout root = (FrameLayout) findViewById(R.id.root);
//    		
//    		int[] deltas = new int[2];
//    		deltas[0] = 12;
//    		
////    		View nearest = new View(this.getContext());
////    		try {
////    			nearest = ff.findNearestTouchable(root, X, Y, View.FOCUS_FORWARD, deltas);
////    		}catch (NullPointerException npe) {
////    			Log.e(TAG, npe.toString());
////    		}
////    		
////    		if (nearest != null) {
////    			Log.d(TAG, "nearestView: "+nearest);
////    			nearest.requestFocus();
////    		}
//    		
//    		//no focus!
//    		View focussed = this.findFocus();
//    		
//    		View touch = this.findViewWithTag("bubble");
//    		View touch = this.findViewById(102);
//    		int testid = touch.getId();
//    		
//    		Log.d(TAG, "view: "+touch.toString() + " ID: "+testid);
//    		
//    		touch.dispatchTouchEvent(event);
//    		touch.onTouchEvent(event);

    		
//    	}
        	}
        	
        	if (rectId == 0) {
        		//the user hasn't touched any of the responsive points, 
        		//show some "tool tips" 
        		showTooltips = true;
        		break;
        	}
        	else if (rectId != 0) {
        		showTooltips = false;
        		break;
        	}
    	
             break; 


        case MotionEvent.ACTION_MOVE:   // touch drag the element
        	
        	//we have a touch if...
        	if (rectId > 0) {
        		MyRect mRect = myRects.get(rectId-1);
        	

	        	/**
	        	 * True if we want to resize the bubble oval, 
	        	 * also reposition the relative thought bubble circles and speech bubble triangle
	        	 */
	        	if (rectId > 0 && resize) {
	        		
	        		mRect.setBottom(Y);
	        		mRect.setRight(X);
	        		
	        		//substituted by triangle
//	        		if (mRect.speech & !sickle_resize) {
//	        		//also reposition the sickle relative to the oval/text rect
//	        			mSickle = mySickles.get(mRect.getID()-1);
//		        		mSickle.setRight(mRect.right - mRect.width/2);
//		            	mSickle.setTop(mRect.bottom - mRect.height/2);
//	        		}
	        		if (mRect.speech & ! triangle_resize) {
		        		//also reposition the triangle relative to the oval/text rect
	        			mTri = myTriangles.get(mRect.getID()-1);
		        		mTri.setTopCornersX(mRect.right - mRect.width/2 - MyTriangle.OFFSET);
		            	mTri.setTopCornersY(mRect.bottom);
		        		}
	        		if (mRect.thought & !thoughtBubbles_resize) {
	            	//also reposition the thought bubbles relative to the oval/text rec
	    	    		mThoughtBubbles = myThoughtBubbles.get(mRect.getID()-1);
		            	mThoughtBubbles.setStartX(mRect.right - mRect.width/2);
		            	mThoughtBubbles.setStartY(mRect.bottom);
	        		}
	    		} 
	        		
	            
	//        	if (rectId > 0 && resize) {
	//        		try {
	//	        		myRects[rectId-1].setBottom(Y-25);
	//	        		myRects[rectId-1].setRight(X-25);
	//	        		
	//	        		//also reposition the sickle relative to the oval/text rect
	//	        		mySickles[rectId-1].setRight(myRects[rectId-1].right - myRects[rectId-1].width/2);
	//	            	mySickles[rectId-1].setTop(myRects[rectId-1].bottom - myRects[rectId-1].height/2);
	//	            	
	//	            	//also reposition the thought bubbles relative to the oval/text rec
	//	            	myThoughtBubbles[rectId-1].setStartX(myRects[rectId-1].right - myRects[rectId-1].width/2);
	//	            	myThoughtBubbles[rectId-1].setStartY(myRects[rectId-1].bottom);
	//        		} 
	//        		catch (ArrayIndexOutOfBoundsException e) {
	//            		Log.e(TAG, e.toString());
	//            	}
	//        	}
	        	
	        	/**
	        	 * True if we want to drag the entire object
	        	 */
	        	//substituted by triangle
//	        	if (rectId > 0 && !resize & !sickle_resize & !thoughtBubbles_resize) {
	        	if (rectId > 0 && !resize & !triangle_resize & !thoughtBubbles_resize) {
	        		mRect.setCenterX(X);
	        		mRect.setCenterY(Y);
	            	
	        		//substituted by triangle
//	        		if (mRect.speech) {
//	            		//also reposition the sickle relative to the oval/text rect
//	        			mSickle = mySickles.get(mRect.getID()-1);
//		        		mSickle.setRight(mRect.right - mRect.width/2);
//		            	mSickle.setTop(mRect.bottom - mRect.height/2);
//	        		}
	        		if (mRect.speech) {
	        			//also reposition the triangle relative to the oval/text rect
	        			mTri = myTriangles.get(mRect.getID()-1);
		        		mTri.setTopCornersX(mRect.right - mRect.width/2 - MyTriangle.OFFSET);
		            	mTri.setTopCornersY(mRect.bottom);
	        		}
	        		if (mRect.thought) {
	                	//also reposition the thought bubbles relative to the oval/text rec
	    	    		mThoughtBubbles = myThoughtBubbles.get(mRect.getID()-1);
		            	mThoughtBubbles.setStartX(mRect.right - mRect.width/2);
		            	mThoughtBubbles.setStartY(mRect.bottom);
	        		}
	            }
	        	
	//            if (rectId > 0 && !resize) {
	//            	try {
	//	            	myRects[rectId-1].setCenterX(X-25);
	//	            	myRects[rectId-1].setCenterY(Y-25);
	//	            	
	//	            	//also reposition the sickle relative to the oval/text rect
	//	            	mySickles[rectId-1].setRight(myRects[rectId-1].right - myRects[rectId-1].width/2);
	//	            	mySickles[rectId-1].setTop(myRects[rectId-1].bottom - myRects[rectId-1].height/2);
	//	            	
	//	            	//also reposition the thought bubbles relative to the oval/text rec
	//	            	myThoughtBubbles[rectId-1].setStartX(myRects[rectId-1].right - myRects[rectId-1].width/2);
	//	            	myThoughtBubbles[rectId-1].setStartY(myRects[rectId-1].bottom);
	//            	} 
	//            	catch (ArrayIndexOutOfBoundsException e) {
	//            		Log.e(TAG, e.toString());
	//            	}
	//            }
	        	

	        	/**
	        	 * True if we want to resize/drag the speech triangle or 
	        	 * the thought bubbles
	        	 */
	        	
	        	//substituted by triangle
//	        	if (mRect.speech && sickleId > 0 && sickle_resize) {
//        			mSickle = mySickles.get(mRect.getID()-1);
//            		mSickle.setBottom(Y-25);
//	            }
	        	if (mRect.speech && triangleId > 0 && triangle_resize) {
        			mTri = myTriangles.get(mRect.getID()-1);
            		mTri.setBottomY(Y);
            		mTri.setBottomX(X);
	            }
	            
	            if (mRect.thought && thoughtBubblesId > 0 && thoughtBubbles_resize) {
	            	mThoughtBubbles = myThoughtBubbles.get(mRect.getID()-1);
            		mThoughtBubbles.setEndX(X);
            		mThoughtBubbles.setEndY(Y);	
	            }
	            
	//            if (sickleId > 0 && sickle_resize) {
	//            	try {
	//            		mySickles[sickleId-1].setBottom(Y-25);
	//            	}
	//            	catch (ArrayIndexOutOfBoundsException e) {
	//            		Log.e(TAG, e.toString());
	//            	}
	//            }
	//            
	//            if (thoughtBubblesId > 0 && thoughtBubbles_resize) {
	//            	try {
	//            		myThoughtBubbles[thoughtBubblesId-1].setEndX(X-25);
	//            		myThoughtBubbles[thoughtBubblesId-1].setEndY(Y-25);	
	//            	} 
	//            	catch (ArrayIndexOutOfBoundsException e) {
	//            		Log.e(TAG, e.toString());
	//            	}
	//            }
        	}
        	
            break; 

        case MotionEvent.ACTION_UP: 
       		// touch drop - just do things here after dropping
        	resize = false;
        	sickle_resize = false;
        	thoughtBubbles_resize = false;
        	triangle_resize = false;
             break; 
        } 
        // redraw the canvas
        invalidate(); 
        return true; 
	
    }
    
    private class WorkerTask extends AsyncTask<Canvas, Integer, Integer> {
	     protected Integer doInBackground(Canvas... params) {
	    	 
	    	Canvas canvas = params[0];
	    	
	    	//the factors we have to multiply the x and y coordinates with, we know 
	    	//both photo and screen are landscape, so: 
	    	float factorX = (float)toSaveWidth/(float)imageOnScreen.width();
	    	float factorY = (float)toSaveHeight/(float)imageOnScreen.height();
	    	
//	    	factorX = 1;
//	    	factorY = 1;
	    	
	    	Log.d(TAG, "factorX: "+factorX +", factorY: " +factorY);
	    	
	    	//increse the font size
	    	textPaint.setTextSize(28);
	    	
	    	//do the same drawing as in onDraw(), except this time scaled for 
	    	//the size we actually want to save the image in 
	    	for (MyRect rect : myRects) {
	    		
	    		//also subtract the "black bars" for X on screen first...
//	    		rect.left -= imageOnScreen.left;
	    		rect.left *= factorX;
//	    		rect.right -= imageOnScreen.left;
	    		rect.right *= factorX;
	    		rect.bottom *= factorY;
	    		rect.top *= factorY;
	        	
		    	if (rect.speech) {
		    		canvas.save();
		        	
		        	int triangleId = rect.getID();
		        	mTri = myTriangles.get(triangleId - 1);
//		        	mTri.topLeftX -= imageOnScreen.left;
		        	mTri.topLeftX *= factorX;
		        	mTri.topLeftY *= factorY;
//		        	mTri.bottomX -= imageOnScreen.left;
		        	mTri.bottomX *= factorX;
		        	mTri.bottomY *= factorY;
//		        	mTri.topRightX -=imageOnScreen.left;
		        	mTri.topRightX *= factorX;
		        	mTri.topRightY *= factorY;
		        	
		        	path.reset();
		        			        	
		    		path.moveTo(mTri.topLeftX, mTri.topLeftY);
		    		path.lineTo(mTri.topRightX, mTri.topRightY);
		    		path.lineTo(mTri.bottomX, mTri.bottomY);
		    		path.close();
		    		
		    		Paint triPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		    		triPaint.setColor(Color.BLACK);
		    		triPaint.setStyle(Paint.Style.STROKE);
		    		triPaint.setStrokeWidth(2);
		    		canvas.drawPath(path, rectPaint);
		    		canvas.drawPath(path, triPaint);
		    		
		    		canvas.restore();
		    	}
//					
				else if (rect.thought) {
					//add "thought bubbles" to oval
			    	canvas.save();
			    	
			    	int thoughtBubblesId = rect.getID();
			    	
			    	//reset path before (re-) drawing
					path.reset();
					
					mThoughtBubbles = myThoughtBubbles.get(thoughtBubblesId-1);
					
					float startX = (mThoughtBubbles.startX)*factorX;
					float startY = mThoughtBubbles.startY*factorY;
					float endX = (mThoughtBubbles.endX)*factorX;
					float endY = mThoughtBubbles.endY*factorY;
					
					//the length of the line is the hypothenuse c=sqrt(a^2+b^2)
					double length = Math.sqrt(  (double) (endX - startX)*
							(endX - startX) + 
									(endY - startY)*
									(endY - startY)
									);
					
					float X = startX;
					float Y = startY;
					
					//constant distance between the bubbles...
					final float K = 10*factorY;
					
					//radius of initial circle
					float radius = 20*factorY;
					
					while (length > 0 && radius > 0) {
						
						//draw bubbles along the line
						//start with the initial one and then decrease them in size depending on lenght of line
						//the black "stroke"
						canvas.drawCircle(X, Y, radius+2, strokePaint);
						//the white "circle"
						canvas.drawCircle(X, Y, radius, rectPaint);
						
						//find the coordinates for the next circle...()
						float distanceToNewCenter = radius + K + radius - 7;
						
						//linear interpolation! get the larp...
						double newX = (X + ((endX - X) * distanceToNewCenter/length ));
						double newY = (Y + ((endY - Y) * distanceToNewCenter/length ));
						
						X = (float) newX;
						Y = (float) newY;
						
						//new radius
						radius = radius - 3*factorY;
						
						//new length 
						length = length - distanceToNewCenter;
						
//						Log.d(TAG, "length:  "+length);
					}		
					canvas.restore();
				}
				
				//draw oval according to String size...
				if (!rect.scene) {
					//underlying oval for "stroke" around the oval
					canvas.drawOval(new RectF(rect.left-40, rect.top-40, rect.right+40, rect.bottom+40), strokePaint);
					//the white text container bubble oval
					canvas.drawOval(new RectF(rect.left-38, rect.top-38, rect.right+38, rect.bottom+38), rectPaint);
				}
				//if type is scene draw rounded rect instead...
				else if (rect.scene) {
					canvas.drawRoundRect(new RectF(rect.left-32, rect.top-32, rect.right+32, rect.bottom+32), 5, 5, strokePaint);
					canvas.drawRoundRect(new RectF(rect.left-30, rect.top-30, rect.right+30, rect.bottom+30), 5, 5, rectPaint);
				}
				//add String to speech bubble...
				for (int i = 0; i<rect.strings.size(); i++) {
					Log.d(TAG, "rect.strings.size:" +rect.strings.size());
					if (i == 0) {
						canvas.drawText(rect.brokenText, rect.left, rect.topLeftAlign*factorY, textPaint);
						Log.d(TAG, "text Y: "+rect.topLeftAlign*factorY);
					}
					else {
						canvas.drawText(rect.strings.get(i), rect.left, (rect.getLinebreakY(i))*factorY, textPaint);
						Log.d(TAG, "text Y2: "+ (rect.getLinebreakY(i))*factorY);
						
					}
				}
	    	}
	    	
//	    	//save to SD card...
//	    	String filename = String.valueOf(System.currentTimeMillis()) ;
////			String filename = "FIRST";
//			ContentValues values = new ContentValues();
//			values.put(Images.Media.TITLE, filename);
//			values.put(Images.Media.DATE_ADDED, System.currentTimeMillis());
//			values.put(Images.Media.MIME_TYPE, "image/jpeg");
//
//			Uri uri = context.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
//			try {
//			  OutputStream outStream = context.getContentResolver().openOutputStream(uri);
//			  returnedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
//			  outStream.flush();
//			  outStream.close();
//			  Log.d(TAG,"done exporting to SD");
//			} catch (FileNotFoundException e) {
//			   Log.e(TAG, e.toString());
//			} catch (IOException e) {
//			  e.printStackTrace();
//			}
	       
			
			
			//done, notify handler to dismiss dialog...
			handler.sendEmptyMessage(0);
			cia.recipient.sendEmptyMessage(0);
			
			return Activity.RESULT_OK;
	     }

	     protected void onProgressUpdate(Integer... progress) {
//	         setProgressPercent(progress[0]);
	     }

	     protected void onPostExecute(Integer... result) {
	    	 //never called?
	    	 Log.d(TAG, "result: "+result[0]+", RESULT_OK: "+Activity.RESULT_OK);
	    
	    	 
	    	 
	         
	     }
	 }
    
    private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (progress.isShowing())
				progress.dismiss();
			Log.d(TAG, "cancelled dialog");
		}
	};
    
    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
    	super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
    	
    	Log.d(TAG, "gained focus: "+gainFocus);
    }
}
