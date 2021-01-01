package me.tagavari.airmessage.messaging.viewholder;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * A view holder that contains a label for an action message
 */
public class VHMessageAction extends RecyclerView.ViewHolder {
	public final TextView label;
	
	public VHMessageAction(View itemView, TextView label) {
		super(itemView);
		
		this.label = label;
	}
}