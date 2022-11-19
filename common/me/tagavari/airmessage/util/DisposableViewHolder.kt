package me.tagavari.airmessage.util

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.disposables.CompositeDisposable

/**
 * A view holder that keeps track of a [CompositeDisposable] for background processes
 */
abstract class DisposableViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
	val compositeDisposable = CompositeDisposable()
}