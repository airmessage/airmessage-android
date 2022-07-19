package me.tagavari.airmessage.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.transition.ChangeBounds;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleEmitter;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.LanguageHelper;
import me.tagavari.airmessage.helper.ResourceHelper;
import me.tagavari.airmessage.helper.ThemeHelper;

import java.util.List;

public class LocationPicker extends AppCompatActivity {
	public static final String intentParamLocation = "location";
	public static final String intentParamAddress = "address";
	public static final String intentParamName = "name";
	
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
		labelCoordinates.setText(LanguageHelper.coordinatesToString(viewModel.mapPosition));
	};
	private OnMapReadyCallback mapCallback = googleMap -> {
		//Setting the map
		this.googleMap = googleMap;
		
		//Configuring the map
		googleMap.getUiSettings().setRotateGesturesEnabled(false);
		googleMap.getUiSettings().setTiltGesturesEnabled(false);
		googleMap.getUiSettings().setMapToolbarEnabled(false);
		googleMap.setBuildingsEnabled(true);
		googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, ThemeHelper.isNightMode(getResources()) ? R.raw.map_dark : R.raw.map_light));
		
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
		getWindow().getDecorView().setSystemUiVisibility(systemUIFlags);
		
		//Getting the view model
		viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
			@NonNull
			@Override
			public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
				//Getting the parameters
				Location location = getIntent().getParcelableExtra(intentParamLocation);
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
			labelLocation.setText(name != null ? name : "");
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
		{
			String address = viewModel.getMapPositionAddress();
			labelLocation.setCurrentText(address != null ? address : "");
		}
		labelCoordinates.setCurrentText(LanguageHelper.coordinatesToString(viewModel.mapPosition));
		
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
				Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());

				//Setting the close button margins
				{
					int insetStart = ResourceHelper.isLTR(getResources()) ? systemInsets.left : systemInsets.right;
					
					fabParams.setMarginStart(fabMarginStart + insetStart);
					fabParams.topMargin = fabMarginTop + systemInsets.top;
					fabClose.setLayoutParams(fabParams);
				}
				
				//Setting the selection container margins
				{
					containerParams.leftMargin = containerMarginLeft + systemInsets.left;
					containerParams.bottomMargin = containerMarginBottom + systemInsets.bottom;
					containerParams.rightMargin = containerMarginRight + systemInsets.right;
					containerSelection.setLayoutParams(containerParams);
				}
				
				//Updating the map
				googleMapBasePadding.left = systemInsets.left;
				googleMapBasePadding.right = systemInsets.right;
				googleMapBasePadding.top = fabParams.topMargin + fabClose.getHeight() + fabParams.bottomMargin;
				googleMapBasePadding.bottom = 0;
				updateMapPadding();
				
				//Updating the system bar protections
				systemProtectionTop.getLayoutParams().height = systemInsets.top;
				systemProtectionBottom.getLayoutParams().height = systemInsets.bottom;
				
				return WindowInsetsCompat.CONSUMED;
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
		returnIntent.putExtra(intentParamLocation, viewModel.mapPosition);
		returnIntent.putExtra(intentParamAddress, viewModel.getMapPositionAddress());
		returnIntent.putExtra(intentParamName, viewModel.getMapPositionName());
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
		
		private Disposable geocoderSubscription = null;
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
			mapPositionAddress.setValue(null);
			mapPositionName = null;
			
			if(geocoderSubscription != null && !geocoderSubscription.isDisposed()) geocoderSubscription.dispose();
			//Log and ignore
			geocoderSubscription = Single.create((SingleEmitter<Pair<String, String>> emitter) -> {
				List<Address> results = geocoder.getFromLocation(mapPosition.latitude, mapPosition.longitude, 1);
				
				if(results == null || results.isEmpty()) {
					emitter.onError(new RuntimeException("No addresses returned from Geocoder"));
					return;
				}
				Address address = results.get(0);
				
				String locationAddress = address.getAddressLine(0);
				
				String locationName;
				if(address.getSubThoroughfare() != null && address.getThoroughfare() != null) locationName = address.getSubThoroughfare() + " " + address.getThoroughfare();
				else locationName = address.getFeatureName();
				emitter.onSuccess(new Pair<>(locationAddress, locationName));
			}).subscribeOn(Schedulers.io())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe((address) -> {
						//Setting the values
						mapPositionAddress.setValue(address.first);
						mapPositionName = address.second;
					}, Throwable::printStackTrace);
		}

		@Nullable
		String getMapPositionAddress() {
			return mapPositionAddress.getValue();
		}

		@Nullable
		String getMapPositionName() {
			return mapPositionName;
		}
	}

	public static class Result {
		private final LatLng location;
		@Nullable private final String address;
		@Nullable private final String name;

		public Result(LatLng location, @Nullable String address, @Nullable String name) {
			this.location = location;
			this.address = address;
			this.name = name;
		}

		public LatLng getLocation() {
			return location;
		}

		@Nullable
		public String getAddress() {
			return address;
		}

		@Nullable
		public String getName() {
			return name;
		}
	}

	public static class ResultContract extends ActivityResultContract<Location, Result> {
		@NonNull
		@Override
		public Intent createIntent(@NonNull Context context, Location input) {
			return new Intent(context, LocationPicker.class)
					.putExtra(LocationPicker.intentParamLocation, input);
		}

		@Override
		public Result parseResult(int resultCode, @Nullable Intent result) {
			if(resultCode != Activity.RESULT_OK || result == null) {
				return null;
			}

			return new Result(
					result.getParcelableExtra(LocationPicker.intentParamLocation),
					result.getStringExtra(LocationPicker.intentParamAddress),
					result.getStringExtra(LocationPicker.intentParamName)
			);
		}
	}
}