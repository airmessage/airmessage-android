package me.tagavari.airmessage.util;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * A view holder that keeps track of a {@link CompositeDisposable} for background processes
 */
public abstract class DisposableViewHolder extends RecyclerView.ViewHolder {
	private final CompositeDisposable compositeDisposable = new CompositeDisposable();
	
	public DisposableViewHolder(@NonNull View itemView) {
		super(itemView);
	}
	
	public CompositeDisposable getCompositeDisposable() {
		return compositeDisposable;
	}
}
