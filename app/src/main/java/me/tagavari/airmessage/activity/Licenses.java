package me.tagavari.airmessage.activity;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mikepenz.aboutlibraries.LibsBuilder;

import me.tagavari.airmessage.R;
import me.tagavari.airmessage.composite.AppCompatCompositeActivity;
import me.tagavari.airmessage.compositeplugin.PluginQNavigation;

public class Licenses extends AppCompatCompositeActivity {
	private static final String keyFragment = "fragment";
	
	Fragment fragment;
	
	public Licenses() {
		addPlugin(new PluginQNavigation());
	}
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_licenses);
		
		if(savedInstanceState == null) {
			//Initializing the fragment
			fragment = new LibsBuilder()
					.withFields(R.string.class.getFields())
					.withAboutMinimalDesign(true)
					.withVersionShown(false)
					.withAboutIconShown(false)
					.withEdgeToEdge(true)
					.supportFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commit();
		} else {
			fragment = getSupportFragmentManager().getFragment(savedInstanceState, keyFragment);
		}
		
		//Enabling up navigation
		setSupportActionBar(findViewById(R.id.toolbar));
		getSupportActionBar().setTitle(R.string.screen_licenses);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		
		//Saving the fragment
		if(fragment != null && fragment.isAdded()) getSupportFragmentManager().putFragment(outState, keyFragment, fragment);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//Up button
		if(item.getItemId() == android.R.id.home) {
			//Finishing the activity
			finish();
			return true;
		}
		
		return false;
	}
}