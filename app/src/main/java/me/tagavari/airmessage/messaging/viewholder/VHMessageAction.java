package me.tagavari.airmessage.messaging.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import me.tagavari.airmessage.util.DisposableViewHolder;

/**
 * A view holder that contains a label for an action message
 */
public class VHMessageAction extends DisposableViewHolder {
	public final TextView label;
	
	public VHMessageAction(View itemView, TextView label) {
		super(itemView);
		
		this.label = label;
	}
}