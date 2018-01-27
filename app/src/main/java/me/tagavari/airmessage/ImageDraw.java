package me.tagavari.airmessage;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class ImageDraw extends AppCompatActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the content view
		setContentView(R.layout.activity_imagedraw);
		
		//((ImageView) findViewById(R.id.imagedraw_mainimage)).setImageURI();
	}
}