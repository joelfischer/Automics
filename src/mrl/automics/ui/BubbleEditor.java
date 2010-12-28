package mrl.automics.ui;

import mrl.automics.R;
import mrl.automics.graphics.MyRect;

import java.util.ArrayList;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView.OnEditorActionListener;

public class BubbleEditor extends Activity {
	
	private final static String TAG = "BubbleEditor";
	private ImageView iv;
	private TextView enteredText;
	private EditText editor;
	private String selectedImage;
	private int type;
	private boolean addMore;
	private ArrayList<String> bubbleTexts;
	private ArrayList<Integer> bubbleTypes;
	private String oriTitle;
	private String croppedTitle;
	private boolean annotated;
	private Bundle timestamps;
	private Intent nextAct;
	private String imageType;
	
	public void onCreate(Bundle savedInstanceState) {
		
		Log.d(TAG, "onCreate()");
		
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.bubble_editor);
		
		Intent startingIntent = this.getIntent();
		selectedImage = startingIntent.getStringExtra("selected");
		oriTitle = startingIntent.getStringExtra("oriTitle");
		croppedTitle = startingIntent.getStringExtra("croppedTitle");
		imageType = startingIntent.getStringExtra("img_type");
		
		Log.d(TAG, "selectedImage: "+selectedImage.toString());
		
		timestamps = startingIntent.getExtras();

		Log.d(TAG, "nTs: "+timestamps.getLong("tsNot")+", ts: "+timestamps.getLong("ts"));
		
		iv = (ImageView) findViewById(R.id.chosen_bubble);
		iv.setImageResource(R.drawable.thought_sample);  
		
		nextAct = new Intent(BubbleEditor.this, ChosenImage_Activity.class);
        nextAct.putExtra("selected", selectedImage);
        nextAct.putExtra("annotated", true);
        nextAct.putExtra("oriTitle", oriTitle);
        nextAct.putExtra("croppedTitle", croppedTitle);
        nextAct.putExtra("timestamps", timestamps);
        nextAct.putExtra("img_type", imageType);
        nextAct.setData(Uri.parse(""+System.currentTimeMillis()));
		
		enteredText = (TextView) findViewById(R.id.bubble_text);
		enteredText.setTextSize(20);
		enteredText.setTypeface(Typeface.createFromAsset(this.getAssets(), 
									"fonts/blambotcustom.ttf"));
		enteredText.setText("TEXT");
				
		editor = (MyEditText) findViewById(R.id.bubble_edit);
			
		editor.setOnKeyListener(new OnKeyListener() {
		    public boolean onKey(View v, int keyCode, KeyEvent event) {
				Log.d(TAG, "view: "+ v+", keyCode: "+keyCode +", event: "+event);
		        // If the event is a key-down event on the "enter" button
		        if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
		            (keyCode == KeyEvent.KEYCODE_ENTER)) {
		          // Perform action on key press
		          String input = editor.getText().toString();
		        	enteredText.setText(input);		          
		          return true;
		        }
//		        if (event.getAction() == KeyEvent.ACTION_DOWN) {
//			         // Perform action on key press 
//			        String input = editor.getText().toString();
//			        enteredText.setText(input);
//			        return true;
//		        }
		        return false;
		    }
		});	
		
		editor.setOnEditorActionListener(new OnEditorActionListener() {

			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				Log.d(TAG, "view: "+ v+", actionId: "+actionId +", event: "+event);
				
				if (actionId == KeyEvent.KEYCODE_ENDCALL) {
					 // Perform action on key press "done"
			        String input = editor.getText().toString();
			        enteredText.setText(input);
					return false;
				}
				 
				return false;
			}
			
		});

		
		Gallery g = (Gallery) findViewById(R.id.bubble_gallery);
	    g.setAdapter(new ImageAdapter(this));

	    g.setOnItemClickListener(new OnItemClickListener() {
	        public void onItemClick(AdapterView parent, View v, int position, long id) {
//	            Log.d(TAG, "pos: "+position);
//	            Log.d(TAG, "id: "+id);
	        }
	    });
	    
	    g.setOnItemSelectedListener(new OnItemSelectedListener() {
	        public void onItemSelected(AdapterView parent, View v, int position, long id) {
	            Log.d(TAG, "pos: "+position);
	            
	            switch (position) {
		            case 0: iv.setImageResource(R.drawable.scene_empty); 
		            		type = MyRect.SCENE; 
		            		break;
		            case 1: iv.setImageResource(R.drawable.speech_empty); 
		            		type = MyRect.SPEECH; 
		            		break;
		            case 2:iv.setImageResource(R.drawable.thought_empty); 
		            		type = MyRect.THOUGHT;
		            		break;
	            }
	        }
	            

			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
				
			}
	    });
	    
	    Button doneBtn = (Button) findViewById(R.id.done_btn);
	    doneBtn.setOnClickListener(new OnClickListener () {

			public void onClick(View arg0) {
				
//		        Toast.makeText(BubbleEditor.this, "Creating bubble...", Toast.LENGTH_SHORT).show();
				bubbleTypes.add(type);
		        nextAct.putIntegerArrayListExtra("types", bubbleTypes);
		        bubbleTexts.add(enteredText.getText().toString());
		        nextAct.putStringArrayListExtra("texts", bubbleTexts);
	            startActivity(nextAct);
	            BubbleEditor.this.finish();
			}
	    	
	    });

	}
	

	
//	@Override
//	public boolean onCreateOptionsMenu(Menu menu) {
//		super.onCreateOptionsMenu(menu);
//		
//		MenuItem item1 = menu.add(0, 0, 0, "Continue without annotating");
//		
//		item1.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//			
//			public boolean onMenuItemClick(MenuItem item) {
//				Intent intent = new Intent(BubbleEditor.this, ChosenImage_Activity.class);
//		        intent.putExtra("image", selectedImage);
//		        intent.putExtra("annotated", false);
//		        intent.putExtra("oriTitle", oriTitle);
//		        intent.putExtra("croppedTitle", croppedTitle);
//	      
//	            startActivity(intent);			
//				
//				return false;
//			}
//		});
//		return true;
//	}
	
	public void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		
		addMore = getIntent().getBooleanExtra("more", false);
		try {
			bubbleTexts = getIntent().getStringArrayListExtra("texts");
		} catch (Exception npe) {
			Log.e(TAG, npe.toString());
			
		}
		if (bubbleTexts == null) {
			bubbleTexts = new ArrayList<String>();
			Log.d(TAG, "created new TextArray.");
		}
		try {
			bubbleTypes = getIntent().getIntegerArrayListExtra("types");
		} catch (Exception npe) {
			Log.e(TAG, npe.toString());
		}
		if (bubbleTypes == null) {
			bubbleTypes = new ArrayList<Integer>();
			Log.d(TAG, "created new TypeArray.");

		}
			
		
		
	}
	
	public class ImageAdapter extends BaseAdapter {
	    int mGalleryItemBackground;
	    private Context mContext;

	    private Integer[] mImageIds = {
	            R.drawable.scene_sample,
	            R.drawable.speech_sample,
	            R.drawable.thought_sample,
	    };

	    public ImageAdapter(Context c) {
	        mContext = c;
	        //not supported in Android 1.6!
//	        TypedArray a = obtainStyledAttributes(android.R.styleable.Theme);
//	        mGalleryItemBackground = a.getResourceId(
//	                android.R.styleable.Theme_galleryItemBackground, 0);
//	        a.recycle();
	    }

	    public int getCount() {
	        return mImageIds.length;
	    }

	    public Object getItem(int position) {
	        return position;
	    }

	    public long getItemId(int position) {
	        return position;
	    }

	    public View getView(int position, View convertView, ViewGroup parent) {
	        ImageView i = new ImageView(mContext);

	        i.setImageResource(mImageIds[position]);
//	        i.setLayoutParams(new Gallery.LayoutParams(150, 100));
	        i.setScaleType(ImageView.ScaleType.FIT_XY);
//	        i.setBackgroundResource(mGalleryItemBackground);
	        
	        i.setAdjustViewBounds(true);
//            i.setLayoutParams(new Gallery.LayoutParams(
//                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            i.setLayoutParams(new Gallery.LayoutParams(300, 200));
            i.setBackgroundResource(R.drawable.picture_frame);

	        return i;
	    }
	}

}
