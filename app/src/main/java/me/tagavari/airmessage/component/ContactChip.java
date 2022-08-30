package me.tagavari.airmessage.component;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.transition.ChangeBounds;
import android.transition.Fade;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.chip.Chip;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import me.tagavari.airmessage.MainApplication;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.helper.ContactHelper;
import me.tagavari.airmessage.helper.ResourceHelper;

public class ContactChip {
    private final String display;
    private final String address;

    public ContactChip(String display, String address) {
        //Setting the data
        this.display = display;
        this.address = address;
    }

    public String getDisplay() {
        return display;
    }

    public String getAddress() {
        return address;
    }

    public View getView(Context context, Consumer<ContactChip> onRemove) {
        //Setting the view
        Chip chip = new Chip(context);
        chip.setText(display);
        
        //Setting the view's click listener
        chip.setOnClickListener(click -> {
            //Inflating the view
            View popupView = LayoutInflater.from(context).inflate(R.layout.popup_userchip, null);
            TextView labelView = popupView.findViewById(R.id.label_member);
            ImageView profileDefault = popupView.findViewById(R.id.profile_default);
            ImageView profileImage = popupView.findViewById(R.id.profile_image);
        
            //Setting the default information
            labelView.setText(display);
            profileDefault.setColorFilter(context.getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
        
            //Filling in the information
            final CompositeDisposable compositeDisposable = new CompositeDisposable();
            compositeDisposable.add(MainApplication.getInstance().getUserCacheHelper().getUserInfo(context, display)
                    .onErrorComplete()
                    .subscribe((userInfo) -> {
                        //Setting the label to the user's display name
                        labelView.setText(userInfo.getContactName());
                    
                        //Adding a sub-label with the user's address
                        TextView addressView = popupView.findViewById(R.id.label_address);
                        addressView.setText(display);
                        addressView.setVisibility(View.VISIBLE);
                    
                        //Loading the user's icon
                        Glide.with(context)
                                .load(ContactHelper.getContactImageURI(userInfo.getContactID()))
                                .listener(new RequestListener<Drawable>() {
                                    @Override
                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                        return false;
                                    }
                                
                                    @Override
                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                        //Swapping to the profile view
                                        profileDefault.setVisibility(View.GONE);
                                        profileImage.setVisibility(View.VISIBLE);
                                    
                                        return false;
                                    }
                                })
                                .into(profileImage);
                    }));
        
            //Creating the window
            final PopupWindow popupWindow = new PopupWindow(popupView, ResourceHelper.dpToPx(300), ResourceHelper.dpToPx(56));
        
            popupWindow.setOutsideTouchable(true);
            popupWindow.setElevation(ResourceHelper.dpToPx(2));
            popupWindow.setEnterTransition(new ChangeBounds());
            popupWindow.setExitTransition(new Fade());
        
            //Setting the remove listener
            popupView.findViewById(R.id.button_remove).setOnClickListener(view -> {
                //Removing this chip
                onRemove.accept(this);
            
                //Dismissing the popup
                popupWindow.dismiss();
            });
        
            //Showing the popup
            popupWindow.showAsDropDown(chip);
        
            //Cancel running background tasks on dismiss
            popupWindow.setOnDismissListener(compositeDisposable::clear);
        });
        
        return chip;
    }
}