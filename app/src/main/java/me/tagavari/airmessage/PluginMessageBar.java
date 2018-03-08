package me.tagavari.airmessage;

import android.support.annotation.DrawableRes;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.pascalwelsch.compositeandroid.activity.ActivityPlugin;

import org.jetbrains.annotations.NotNull;

class PluginMessageBar extends ActivityPlugin {
	//Creating the view values
	private ViewGroup parentView;
	
	//Creating the state values
	private int infoBarsRegistered = 0;
	private int infoBarsOpen = 0;
	
	void setParentView(ViewGroup view) {
		parentView = view;
	}
	
	InfoBar create(int icon, String text) {
		return new InfoBar(infoBarsRegistered++, icon, text);
	}
	
	private void updateAppBar() {
		/* if(infoBarsOpen == 0) ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(Constants.dpToPx(4));
		else ((AppCompatActivity) getActivity()).getSupportActionBar().setElevation(0); */
	}
	
	class InfoBar {
		//Creating the configuration values
		@DrawableRes
		private int icon;
		private String text;
		private String buttonText;
		private View.OnClickListener buttonClickListener;
		
		//Creating the view values
		private View view = null;
		
		private ImageView iconView;
		private TextView textView;
		private Button buttonView;
		
		//Creating the state values
		private int infoBarIndex;
		private boolean infoBarVisible = false;
		
		//Creating the other values
		private int infoBarColor = -1;
		
		InfoBar(int index, int icon, String text) {
			//Setting the values
			infoBarIndex = index;
			this.icon = icon;
			this.text = text;
		}
		
		void setText(String text) {
			this.text = text;
		}
		
		void setText(int textRes) {
			setText(getActivity().getResources().getString(textRes));
		}
		
		void setIcon(int icon) {
			this.icon = icon;
		}
		
		void setButton(@NotNull String text, @NotNull View.OnClickListener onClick) {
			//Setting the values
			buttonText = text;
			buttonClickListener = onClick;
		}
		
		void setButton(int textRes, @NotNull View.OnClickListener onClick) {
			setButton(getActivity().getResources().getString(textRes), onClick);
		}
		
		void removeButton() {
			buttonText = null;
		}
		
		void setColor(int color) {
			if(view == null) infoBarColor = color;
			else buttonView.setTextColor(color);
		}
		
		private void prepareView() {
			//Checking if the view is invalid
			if(view == null) {
				//Inflating the view
				view = getActivity().getLayoutInflater().inflate(R.layout.layout_infobar, parentView, false);
				
				//Placing the view
				if(parentView.getChildCount() == 0) parentView.addView(view);
				else {
					boolean viewAdded = false;
					for(int i = 0; i < parentView.getChildCount(); i++) {
						if((int) parentView.getChildAt(i).getTag() < infoBarIndex) continue;
						parentView.addView(view, i);
						viewAdded = true;
						break;
					}
					if(!viewAdded) parentView.addView(view, parentView.getChildCount());
				}
				
				//Tagging the view
				view.setTag(infoBarIndex);
				
				//Getting the view components
				iconView = view.findViewById(R.id.icon);
				textView = view.findViewById(R.id.text);
				buttonView = view.findViewById(R.id.button);
				if(infoBarColor != -1) buttonView.setTextColor(infoBarColor);
			}
			
			//Filling in the view details
			if(icon == -1) iconView.setVisibility(View.GONE);
			else {
				iconView.setImageResource(icon);
				iconView.setVisibility(View.VISIBLE);
			}
			
			textView.setText(text);
			
			if(buttonText == null) buttonView.setVisibility(View.GONE);
			else {
				buttonView.setText(buttonText);
				buttonView.setVisibility(View.VISIBLE);
				buttonView.setOnClickListener(buttonClickListener);
			}
		}
		
		void show() {
			//Returning if the parent view is invalid
			if(parentView == null) return;
			
			//Preparing the view
			prepareView();
			
			//Returning if the view is already visible
			if(infoBarVisible) return;
			
			//Hiding the view
			view.setVisibility(View.GONE);
			
			//Showing the view
			view.setVisibility(View.VISIBLE);
			
			//Setting the view as visible
			infoBarVisible = true;
			
			//Updating the app bar
			infoBarsOpen++;
			updateAppBar();
		}
		
		void hide() {
			//Returning if the view is invalid or is already hidden
			if(view == null || !infoBarVisible) return;
			
			//Hiding the view
			view.setVisibility(View.GONE);
			
			//Setting the view as hidden
			infoBarVisible = false;
			
			//Updating the app bar
			infoBarsOpen--;
			updateAppBar();
		}
	}
}