package me.tagavari.airmessage;

import android.content.Context;
import android.widget.ListView;

class ViewSupport {
	/* public static class ExpandableListView extends ListView {
		boolean expanded = true;
		
		public ExpandableListView(Context context, AttributeSet attrs, int defaultStyle) {
			super(context, attrs, defaultStyle);
		}
		
		public boolean isExpanded() {
			return expanded;
		}
		
		public void setExpanded(boolean expanded) {
			this.expanded = expanded;
		}
		
		@Override
		public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			if(isExpanded()) {
				// Calculate entire height by providing a very large height hint.
				// View.MEASURED_SIZE_MASK represents the largest height possible.
				int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK, MeasureSpec.AT_MOST);
				super.onMeasure(widthMeasureSpec, expandSpec);
				
				ViewGroup.LayoutParams params = getLayoutParams();
				params.height = getMeasuredHeight();
			} else {
				super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			}
		}
	} */
	
	public static class MessagingStyleListView extends ListView {
		public MessagingStyleListView(Context context) {
			super(context);
		}
		
		
	}
}