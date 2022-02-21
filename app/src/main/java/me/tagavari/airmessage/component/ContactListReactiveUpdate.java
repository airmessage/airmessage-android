package me.tagavari.airmessage.component;

import androidx.annotation.NonNull;

/**
 * Represents an update payload for the contacts list
 */
public abstract class ContactListReactiveUpdate {
    /**
     * Updates the adapter with this reactive change
     */
    public abstract void updateAdapter(@NonNull ContactsRecyclerAdapter adapter);

    /**
     * Represents the addition of a new item
     */
    public static class Addition extends ContactListReactiveUpdate {
        private final int position;

        public Addition(int position) {
            this.position = position;
        }

        @Override
        public void updateAdapter(@NonNull ContactsRecyclerAdapter adapter) {
            adapter.onItemAdded(position);
        }
    }

    /**
     * Represents the update of an existing item
     */
    public static class Change extends ContactListReactiveUpdate {
        private final int position;

        public Change(int position) {
            this.position = position;
        }

        @Override
        public void updateAdapter(@NonNull ContactsRecyclerAdapter adapter) {
            adapter.onItemUpdated(position);
        }
    }
}