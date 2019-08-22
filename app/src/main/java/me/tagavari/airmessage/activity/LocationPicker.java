package me.tagavari.airmessage.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.util.List;

import me.tagavari.airmessage.util.Constants;
import me.tagavari.airmessage.R;

public class LocationPicker extends AppCompatActivity {
	private ActivityViewModel viewModel;
	
	private FloatingActionButton fabClose;
	private CardView containerSelection;
	private TextSwitcher labelLocation;
	private TextSwitcher labelCoordinates;
	private View systemProtectionTop, systemProtectionBottom;
	
	private final Rect googleMapBasePadding = new Rect(); //Padding for the Google Map view, minus the height of the bottom selection container
	private GoogleMap googleMap;
	private MapView mapView;
	private Marker mapMarker;
	private GoogleMap.OnMapClickListener mapClickListener = tapPosition -> {
		//Setting the position
		viewModel.mapPosition = tapPosition;
		
		//Updating the marker
		mapMarker.setPosition(tapPosition);
		
		//Updating the labels
		viewModel.loadMapAddress();
		labelCoordinates.setText(Constants.locationToString(viewModel.mapPosition));
	};
	private OnMapReadyCallback mapCallback = googleMap -> {
		//Setting the map
		this.googleMap = googleMap;
		
		//Configuring the map
		googleMap.getUiSettings().setRotateGesturesEnabled(false);
		googleMap.getUiSettings().setTiltGesturesEnabled(false);
		googleMap.getUiSettings().setMapToolbarEnabled(false);
		googleMap.setBuildingsEnabled(true);
		googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, Constants.isNightMode(getResources()) ? R.raw.map_dark : R.raw.map_light));
		
		updateMapPadding();
		
		googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(viewModel.mapPosition, 15));
		MarkerOptions markerOptions = new MarkerOptions().position(viewModel.mapPosition);
		mapMarker = googleMap.addMarker(markerOptions);
		googleMap.setOnMapClickListener(mapClickListener);
	};
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		//Calling the super method
		super.onCreate(savedInstanceState);
		
		//Setting the layout
		setContentView(R.layout.activity_locationpicker);
		
		//Setting the window
		int systemUIFlags = getWindow().getDecorView().getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
		/* if(!Constants.isNightMode(getResources())) {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) systemUIFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
			else systemUIFlags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
		} */
		getWindow().getDecorView().setSystemUiVisibility(systemUIFlags);
		
		//Getting the view model
		viewModel = ViewModelProviders.of(this, new ViewModelProvider.Factory() {
			@NonNull
			@Override
			public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
				//Getting the parameters
				Location location = getIntent().getParcelableExtra(Constants.intentParamData);
				LatLng mapPosition = new LatLng(location.getLatitude(), location.getLongitude());
				
				return (T) new ActivityViewModel(getApplication(), mapPosition);
			}
		}).get(ActivityViewModel.class);
		
		//Setting the listeners
		viewModel.mapPositionAddress.observe(this, name -> {
			ChangeBounds transition = new ChangeBounds();
			transition.addListener(new Transition.TransitionListener() {
				@Override
				public void onTransitionStart(Transition transition) {}
				
				@Override
				public void onTransitionEnd(Transition transition) {
					updateMapPadding();
				}
				
				@Override
				public void onTransitionCancel(Transition transition) {}
				
				@Override
				public void onTransitionPause(Transition transition) {}
				
				@Override
				public void onTransitionResume(Transition transition) {}
			});
			TransitionManager.go(new Scene(containerSelection), transition);
			labelLocation.setText(name);
		});
		
		//Getting the views
		mapView = findViewById(R.id.mapview);
		mapView.onCreate(savedInstanceState);
		mapView.getMapAsync(mapCallback);
		fabClose = findViewById(R.id.fab_close);
		containerSelection = findViewById(R.id.container_selection);
		labelLocation = containerSelection.findViewById(R.id.label_location);
		labelCoordinates = containerSelection.findViewById(R.id.label_coordinates);
		systemProtectionTop = findViewById(R.id.view_protection_top);
		systemProtectionBottom = findViewById(R.id.view_protection_bottom);
		
		//Configuring the views
		containerSelection.setOnTouchListener((view, event) -> true); //To prevent touches from going through to the map
		{
			labelLocation.setInAnimation(AnimationUtils.loadAnimation(this,android.R.anim.slide_in_left));
			labelCoordinates.setInAnimation(AnimationUtils.loadAnimation(this,android.R.anim.slide_in_left));
			labelLocation.setOutAnimation(AnimationUtils.loadAnimation(this,android.R.anim.slide_out_right));
			labelCoordinates.setOutAnimation(AnimationUtils.loadAnimation(this,android.R.anim.slide_out_right));
		}
		labelLocation.setCurrentText(viewModel.getMapPositionAddress());
		labelCoordinates.setCurrentText(Constants.locationToString(viewModel.mapPosition));
		
		//Hiding the navigation bar protection on Android Q
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) systemProtectionBottom.setVisibility(View.GONE);
		
		//Listening for window insets
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), new OnApplyWindowInsetsListener() {
			ViewGroup.MarginLayoutParams fabParams = (ViewGroup.MarginLayoutParams) fabClose.getLayoutParams();
			int fabMarginStart = fabParams.getMarginStart();
			int fabMarginTop = fabParams.topMargin;
			
			ViewGroup.MarginLayoutParams containerParams = (ViewGroup.MarginLayoutParams) containerSelection.getLayoutParams();
			int containerMarginLeft = containerParams.leftMargin;
			int containerMarginBottom = containerParams.bottomMargin;
			int containerMarginRight = containerParams.rightMargin;
			
			@Override
			public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
				//Setting the close button margins
				{
					int insetStart = Constants.isLTR(getResources()) ? insets.getSystemWindowInsetLeft() : insets.getSystemWindowInsetRight();
					
					fabParams.setMarginStart(fabMarginStart + insetStart);
					fabParams.topMargin = fabMarginTop + insets.getSystemWindowInsetTop();
					fabClose.setLayoutParams(fabParams);
				}
				
				//Setting the selection container margins
				{
					containerParams.leftMargin = containerMarginLeft + insets.getSystemWindowInsetLeft();
					containerParams.bottomMargin = containerMarginBottom + insets.getSystemWindowInsetBottom();
					containerParams.rightMargin = containerMarginRight + insets.getSystemWindowInsetRight();
					containerSelection.setLayoutParams(containerParams);
				}
				
				//Updating the map
				googleMapBasePadding.left = insets.getSystemWindowInsetLeft();
				googleMapBasePadding.right = insets.getSystemWindowInsetLeft();
				googleMapBasePadding.top = fabParams.topMargin + fabClose.getHeight() + fabParams.bottomMargin;
				googleMapBasePadding.bottom = 0;
				updateMapPadding();
				
				//Updating the system bar protections
				systemProtectionTop.getLayoutParams().height = insets.getSystemWindowInsetTop();
				systemProtectionBottom.getLayoutParams().height = insets.getSystemWindowInsetBottom();
				
				return insets.consumeSystemWindowInsets();
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}
	
	public void onClickClose(View view) {
		setResult(Activity.RESULT_CANCELED);
		finish();
	}
	
	public void onClickConfirm(View view) {
		Intent returnIntent = new Intent();
		returnIntent.putExtra(Constants.intentParamData, viewModel.mapPosition);
		returnIntent.putExtra(Constants.intentParamAddress, viewModel.getMapPositionAddress());
		returnIntent.putExtra(Constants.intentParamName, viewModel.getMapPositionName());
		setResult(Activity.RESULT_OK, returnIntent);
		finish();
	}
	
	private void updateMapPadding() {
		if(googleMap == null) return;
		
		ViewGroup.MarginLayoutParams selectionLayoutParams = (ViewGroup.MarginLayoutParams) containerSelection.getLayoutParams();
		int selectionHeight = containerSelection.getHeight() + selectionLayoutParams.bottomMargin + selectionLayoutParams.topMargin;
		googleMap.setPadding(googleMapBasePadding.left, googleMapBasePadding.top, googleMapBasePadding.right, googleMapBasePadding.bottom + selectionHeight);
	}
	
	private static class ActivityViewModel extends AndroidViewModel {
		private LatLng mapPosition;
		private final MutableLiveData<String> mapPositionAddress = new MutableLiveData<>();
		private String mapPositionName;
		
		private AsyncTask geocoderTask = null;
		private Geocoder geocoder;
		
		ActivityViewModel(Application application, LatLng mapPosition) {
			super(application);
			
			//Initializing the Geocoder
			geocoder = new Geocoder(application);
			
			//Setting the values
			this.mapPosition = mapPosition;
			
			//Loading the map address
			loadMapAddress();
		}
		
		@SuppressLint("StaticFieldLeak")
		void loadMapAddress() {
			//Returning if the geocoder is unavailable
			if(!Geocoder.isPresent()) return;
			
			//Loading the map position
			mapPositionAddress.setValue("");
			if(geocoderTask != null) geocoderTask.cancel(false);
			geocoderTask = new AsyncTask<LatLng, Void, Constants.Tuple2<String, String>>() {//Returns the full address, and the feature name / street address
				@Override
				protected Constants.Tuple2<String, String> doInBackground(LatLng... arguments) {
					LatLng mapPosition = arguments[0];
					try {
						List<Address> results = geocoder.getFromLocation(mapPosition.latitude, mapPosition.longitude, 1);
						if(results == null || results.isEmpty()) return null;
						Address address = results.get(0);
						
						String locationAddress = address.getAddressLine(0);
						
						String locationName;
						if(address.getSubThoroughfare() != null && address.getThoroughfare() != null) locationName = address.getSubThoroughfare() + " " + address.getThoroughfare();
						else locationName = address.getFeatureName();
						return new Constants.Tuple2<>(locationAddress, locationName);
					} catch(IOException | IllegalArgumentException exception) {
						exception.printStackTrace();
						return null;
					}
				}
				
				@Override
				protected void onPostExecute(Constants.Tuple2<String, String> address) {
					//Setting the values
					if(address != null) {
						mapPositionAddress.setValue(address.item1);
						mapPositionName = address.item2;
					}
				}
			}.execute(mapPosition);
		}
		
		String getMapPositionAddress() {
			String value = mapPositionAddress.getValue();
			if(value == null) return "";
			return value;
		}
		
		String getMapPositionName() {
			return mapPositionName;
		}
	}
}