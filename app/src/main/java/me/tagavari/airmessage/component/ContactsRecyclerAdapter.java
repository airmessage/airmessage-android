package me.tagavari.airmessage.component;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.reactivex.rxjava3.disposables.Disposable;
import me.tagavari.airmessage.R;
import me.tagavari.airmessage.constants.ColorConstants;
import me.tagavari.airmessage.helper.AddressHelper;
import me.tagavari.airmessage.helper.ContactHelper;
import me.tagavari.airmessage.task.ContactsTask;
import me.tagavari.airmessage.util.AddressInfo;
import me.tagavari.airmessage.util.ContactInfo;

public class ContactsRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    //Creating the type constants
    private static final int TYPE_HEADER_DIRECT = 0;
    private static final int TYPE_ITEM = 1;

    //Creating the parameters
    private final Context context;
    private final Consumer<String> selectionCallback;
    
    //Creating the list values
    private final List<ContactInfo> originalItems;
    private final List<ContactInfo> filteredItems = new ArrayList<>();

    //Creating the task values
    private Disposable searchDisposable;

    //Creating the other values
		/* private final boolean serviceSelectorHeaderEnabled;
		private boolean serviceSelectorHeaderVisible; */
    private boolean directAddHeaderVisible = false;
    private String lastFilterText = "";
    //Filter out phone numbers, for the sake of non-iMessage services
    private boolean filterPhoneOnly = false;

    /**
     * A recycler adapter that allows the user to select from a list of contacts
     * @param context The context to use
     * @param selectionCallback A callback invoked with the human-readable address of a selected contact
     * @param items The list of contacts to display
     */
    public ContactsRecyclerAdapter(Context context, List<ContactInfo> items, Consumer<String> selectionCallback) {
        //Setting the items
        this.context = context;
        originalItems = items;
        filteredItems.addAll(items);
        this.selectionCallback = selectionCallback;
    }

    /**
     * Maps an index from the source list to its recycler view index
     */
    public int mapSourceListIndex(int index) {
        if(directAddHeaderVisible) return index + 1;
        else return index;
    }

    /**
     * Notifies the addition of the item at the specified index
     */
    public void onItemAdded(int additionIndex) {
        //If we're currently searching, ignore the item for now
        if(lastFilterText.isEmpty()) {
            filteredItems.add(originalItems.get(additionIndex));
            notifyItemInserted(mapSourceListIndex(additionIndex));
        }
    }

    /**
     * Notifies the update of the item at the specified index
     */
    public void onItemUpdated(int updateIndex) {
        if(lastFilterText.isEmpty()) {
            //Updating the item in the standard list view
            notifyItemChanged(mapSourceListIndex(updateIndex));
        } else {
            //Updating the item in the search view
            int searchIndex = filteredItems.indexOf(originalItems.get(updateIndex));
            if(searchIndex != -1) notifyItemChanged(mapSourceListIndex(searchIndex));
        }
    }

    /**
     * Refreshes the entire list
     */
    public void onListUpdated() {
        filterList(lastFilterText);
    }

    /**
     * Filters the list with the specified text.
     * Specify a blank string to disable filtering.
     */
    public void filterList(String filter) {
        //Setting the last filter text
        lastFilterText = filter;

        //Cleaning the filter
        filter = filter.trim();

        //Cancelling the current task
        if(searchDisposable != null && !searchDisposable.isDisposed()) searchDisposable.dispose();

        boolean filterEmpty = filter.isEmpty();

        //Checking if the filter is empty
        if(filterEmpty && !filterPhoneOnly) {
            //Hiding the direct add header
            directAddHeaderVisible = false;

            //Adding all of the items
            filteredItems.clear();
            filteredItems.addAll(originalItems);

            //Notifying the adapter
            notifyDataSetChanged();
        } else {
            //Updating the direct add header's visibility
            directAddHeaderVisible = !filterEmpty && AddressHelper.validateAddress(filter);

            //Filtering and updating
            filteredItems.clear();
            notifyDataSetChanged();
            searchDisposable = ContactsTask.searchContacts(originalItems, filter, filterPhoneOnly).subscribe(item -> {
                int insertionIndex = filteredItems.size();
                filteredItems.add(item);
                notifyItemInserted(mapSourceListIndex(insertionIndex));
            });
        }
    }

    /**
     * Sets whether this list will filter out non-phone number entries
     */
    public void setFilterPhoneOnly(boolean value) {
        //Returning if the requested value is already the current value
        if(filterPhoneOnly == value) return;

        //Setting the value
        filterPhoneOnly = value;

        //Updating the list
        onListUpdated();
    }

    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch(viewType) {
            case TYPE_HEADER_DIRECT:
                return new HeaderDirectViewHolder(LayoutInflater.from(context).inflate(R.layout.listitem_contact_sendheader, parent, false));
            case TYPE_ITEM:
                return new ItemViewHolder(LayoutInflater.from(context).inflate(R.layout.listitem_contact, parent, false));
            default:
                throw new IllegalArgumentException("Invalid view type received, got " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        switch(getItemViewType(position)) {
            case TYPE_HEADER_DIRECT: {
                //Casting the view holder
                HeaderDirectViewHolder itemVH = (HeaderDirectViewHolder) viewHolder;

                //Setting the label
                itemVH.label.setText(context.getResources().getString(R.string.action_sendto, lastFilterText));

                //Setting the click listener
                itemVH.itemView.setOnClickListener(view -> {
                    selectionCallback.accept(lastFilterText.trim());
                });

                //Breaking
                break;
            }
            case TYPE_ITEM: {
                //Casting the view holder
                ItemViewHolder itemVH = (ItemViewHolder) viewHolder;

                //Getting the item
                ContactInfo contactInfo = getItemAtIndex(position);

                //Populating the view
                itemVH.contactName.setText(contactInfo.getName());

                int addressCount = contactInfo.getAddresses().size();
                String firstAddress = contactInfo.getAddresses().get(0).getAddress();
                if(addressCount == 1) itemVH.contactAddress.setText(firstAddress);
                else itemVH.contactAddress.setText(context.getResources().getQuantityString(R.plurals.message_multipledestinations, addressCount, firstAddress, addressCount - 1));

                //Showing / hiding the section header
                boolean showHeader;
                //if(!lastFilterText.isEmpty()) showHeader = false;
                if(position > getHeaderCount()) {
                    ContactInfo contactInfoAbove = filteredItems.get(position - getHeaderCount() - 1);
                    showHeader = contactInfoAbove == null || !stringsHeaderEqual(contactInfo.getName(), contactInfoAbove.getName());
                } else showHeader = true;

                if(showHeader) {
                    itemVH.header.setVisibility(View.VISIBLE);
                    itemVH.headerLabel.setText(Character.toString(getNameHeader(contactInfo.getName())));
                } else itemVH.header.setVisibility(View.GONE);

                //Resetting the image view
                itemVH.profileDefault.setVisibility(View.VISIBLE);
                itemVH.profileDefault.setColorFilter(context.getResources().getColor(R.color.colorPrimary, null), android.graphics.PorterDuff.Mode.MULTIPLY);
                itemVH.profileImage.setImageBitmap(null);

                //Assigning the contact's image
                Glide.with(context)
                        .load(contactInfo.getThumbnailURI())
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                //Hiding the default view
                                itemVH.profileDefault.setVisibility(View.INVISIBLE);

                                //Setting the bitmap
                                itemVH.profileImage.setImageDrawable(resource);

                                return true;
                            }
                        })
                        .into(itemVH.profileImage);

                //Setting the click listener
                itemVH.contentArea.setOnClickListener(clickView -> {
                    //Checking if there is only one label
                    if(contactInfo.getAddresses().size() == 1) {
                        //Returning the address
                        AddressInfo address = contactInfo.getAddresses().get(0);
                        selectionCallback.accept(address.getAddress());
                    } else {
                        //Creating the dialog
                        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                                .setTitle(R.string.imperative_selectdestination)
                                .setItems(contactInfo.getAddressDisplayList(context.getResources()).toArray(new String[0]), (dialogInterface, index) -> {
                                    //Adding the selected address
                                    AddressInfo address = contactInfo.getAddresses().get(index);
                                    selectionCallback.accept(address.getAddress());
                                })
                                .create();

                        //Disabling any unavailable items (email addresses, when only phone numbers can be used)
                        if(filterPhoneOnly) {
                            dialog.getListView().setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                                @Override
                                public void onChildViewAdded(View parent, View child) {
                                    //Getting the item
                                    int index = ((ViewGroup) parent).indexOfChild(child);
                                    AddressInfo addressInfo = contactInfo.getAddresses().get(index);

                                    //Validating the address
                                    boolean enabled = AddressHelper.validatePhoneNumber(addressInfo.getNormalizedAddress());

                                    //Updating the child's status
                                    if(!enabled) {
                                        child.setEnabled(false);
                                        child.setAlpha(ColorConstants.disabledAlpha);
                                        child.setOnClickListener(null);
                                    }
                                }

                                @Override
                                public void onChildViewRemoved(View parent, View child) {

                                }
                            });
                        }

                        //Showing the dialog
                        dialog.show();
                    }
                });

                //Breaking
                break;
            }
        }
    }

    /**
     * Gets the contact info at the specified index, taking headers into account
     */
    private ContactInfo getItemAtIndex(int index) {
        return filteredItems.get(index - getHeaderCount());
    }

    @Override
    public int getItemCount() {
        return filteredItems.size() + getHeaderCount();
    }

    @Override
    public int getItemViewType(int position) {
        //Checking if the item is a header
        if(position < getHeaderCount()) return TYPE_HEADER_DIRECT;

        //Returning the item
        return TYPE_ITEM;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        //Cancelling the background task
        if(searchDisposable != null && !searchDisposable.isDisposed()) searchDisposable.dispose();
    }

    private int getHeaderCount() {
        int offset = 0;
        if(directAddHeaderVisible) offset++;
        return offset;
    }

    private char getNameHeader(String name) {
        if(name == null || name.isEmpty()) return '?';
        char firstChar = name.charAt(0);
        if(Character.isDigit(firstChar) || firstChar == '(') return '#';
        return firstChar;
    }

    private boolean stringsHeaderEqual(String string1, String string2) {
        if(string1 == null || string1.isEmpty()) return string2 == null || string2.isEmpty();
        return getNameHeader(string1) == getNameHeader(string2);
    }

    private static class HeaderDirectViewHolder extends RecyclerView.ViewHolder {
        //Creating the view values
        private final TextView label;

        HeaderDirectViewHolder(View view) {
            //Calling the super method
            super(view);

            //Setting the views
            label = view.findViewById(R.id.label);
        }
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        //Creating the view values
        private final TextView contactName;
        private final TextView contactAddress;

        private final View header;
        private final TextView headerLabel;

        private final ImageView profileDefault;
        private final ImageView profileImage;

        private final View contentArea;

        private ItemViewHolder(View view) {
            //Calling the super method
            super(view);

            //Getting the views
            contactName = view.findViewById(R.id.label_name);
            contactAddress = view.findViewById(R.id.label_address);

            header = view.findViewById(R.id.header);
            headerLabel = view.findViewById(R.id.header_label);

            profileDefault = view.findViewById(R.id.profile_default);
            profileImage = view.findViewById(R.id.profile_image);

            contentArea = view.findViewById(R.id.area_content);
        }
    }
}
