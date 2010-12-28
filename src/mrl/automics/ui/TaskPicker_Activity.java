package mrl.automics.ui;

import mrl.automics.R;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class TaskPicker_Activity extends Activity {
		
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.task_picker);
		
		Button photo = (Button)findViewById(R.id.btn_photo_task);
		Button annotate = (Button)findViewById(R.id.btn_annotation_task);
		Button story = (Button)findViewById(R.id.btn_photostory_task);
		
		photo.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				startActivity(new Intent(TaskPicker_Activity.this, Photo_Activity.class));
				
			}
		});
		
		annotate.setOnClickListener(new View.OnClickListener() {
					
			public void onClick(View v) {
						startActivity(new Intent(TaskPicker_Activity.this, Annotation_Activity.class));
						
			}
		});

		story.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				startActivity(new Intent(TaskPicker_Activity.this, PhotoStory_Activity.class));
				
			}
		});
				
		
	}
	
}
