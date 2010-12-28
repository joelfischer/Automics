package mrl.automics.graphics;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;

public class MyOval extends OvalShape {
	
	Rect padding;
	ShapeDrawable sd;
	
	MyOval(MyRect myPadding) {
		
		ShapeDrawable sd = new ShapeDrawable(this);
		padding = new Rect((int)myPadding.left, (int)myPadding.top, (int) myPadding.right, (int)myPadding.bottom);
		this.padding = padding;
//		this.setPadding(padding);
		sd.setPadding(padding);
		this.sd = sd;
		
	}
	
	public void setPadding(MyRect myPadding) {
		if (sd == null) {
			sd = new ShapeDrawable(this);
		}
		padding = new Rect((int)myPadding.left, (int)myPadding.top, (int) myPadding.right, (int)myPadding.bottom);
		this.padding = padding;
		sd.setPadding(padding);
		
	}

}
