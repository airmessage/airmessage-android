package me.tagavari.airmessage.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import me.tagavari.airmessage.R;

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