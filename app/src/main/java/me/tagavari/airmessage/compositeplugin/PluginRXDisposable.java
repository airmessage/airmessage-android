package me.tagavari.airmessage.compositeplugin;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import me.tagavari.airmessage.composite.AppCompatActivityPlugin;

/**
 * Provides and handles the lifecycle of a ReactiveX CompositeDisposable
 */
public class PluginRXDisposable extends AppCompatActivityPlugin {
	//Creating the listener values
	private final CompositeDisposable compositeDisposableUI = new CompositeDisposable(); //For the start-stop activity lifecycle
	private final CompositeDisposable compositeDisposableActivity = new CompositeDisposable(); //For the create-destroy activity lifecycle
	
	/**
	 * Gets the {@link CompositeDisposable} for this activity's start-stop lifecycle
	 */
	public CompositeDisposable ui() {
		return compositeDisposableUI;
	}
	
	/**
	 * Gets the {@link CompositeDisposable} for this activity's create-destroy lifecycle
	 */
	public CompositeDisposable activity() {
		return compositeDisposableActivity;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		//Unsubscribing from start-stop tasks
		compositeDisposableUI.clear();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		//Unsubscribing from create-destroy tasks
		compositeDisposableActivity.clear();
	}
}