/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bare;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * You can use this adapter to provide views for an {@link AdapterView},
 * Returns a view for each object in a collection of data objects you
 * provide, and can be used with list-based user interface widgets such as
 * {@link ListView} or {@link Spinner}.
 * <p>
 * By default, the array adapter creates a view by calling {@link Object#toString()} on each
 * data object in the collection you provide, and places the result in a TextView.
 * You may also customize what type of view is used for the data object in the collection.
 * To customize what type of view is used for the data object,
 * override {@link #getView(int, View, ViewGroup)}
 * and inflate a view resource.
 * </p>
 * <p>
 * For an example of using an array adapter with a ListView, see the
 * <a href="{@docRoot}guide/topics/ui/declaring-layout.html#AdapterViews">
 * Adapter Views</a> guide.
 * </p>
 * <p>
 * For an example of using an array adapter with a Spinner, see the
 * <a href="{@docRoot}guide/topics/ui/controls/spinner.html">Spinners</a> guide.
 * </p>
 * <p class="note"><strong>Note:</strong>
 * If you are considering using array adapter with a ListView, consider using
 * {@link android.support.v7.widget.RecyclerView} instead.
 * RecyclerView offers similar features with better performance and more flexibility than
 * ListView provides.
 * See the
 * <a href="https://developer.android.com/guide/topics/ui/layout/recyclerview.html">
 * Recycler View</a> guide.</p>
 */
public class OrderAdapter extends BaseAdapter implements Filterable {
    /**
     * Lock used to modify the content of {@link #mObjects}. Any write operation
     * performed on the array should be synchronized on this lock. This lock is also
     * used by the filter (see {@link #getFilter()} to make a synchronized copy of
     * the original array of data.
     */
    private final Object mLock = new Object();

    private final LayoutInflater mInflater;

    private final Context mContext;

    /**
     * The resource indicating what views to inflate to display the content of this
     * array adapter.
     */
    private final int mResource;

    /**
     * The resource indicating what views to inflate to display the content of this
     * array adapter in a drop down widget.
     */
    private int mDropDownResource;

    /**
     * Contains the list of objects that represent the data of this OrderAdapter.
     * The content of this list is referred to as "the array" in the documentation.
     */
    private List<Order> mObjects;

    /**
     * Indicates whether the contents of {@link #mObjects} came from static resources.
     */
    private boolean mObjectsFromResources;

    /**
     * Indicates whether or not {@link #notifyDataSetChanged()} must be called whenever
     * {@link #mObjects} is modified.
     */
    private boolean mNotifyOnChange = true;

    // A copy of the original mObjects array, initialized from and then used instead as soon as
    // the mFilter ArrayFilter is used. mObjects will then only contain the filtered values.
    private ArrayList<Order> mOriginalValues;
    private ArrayFilter mFilter;

    /** Layout inflater used for {@link #getDropDownView(int, View, ViewGroup)}. */
    private LayoutInflater mDropDownInflater;

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     */
    public OrderAdapter(@NonNull Context context, @LayoutRes int resource) {
        this(context, resource, new ArrayList<>());
    }

    /**
     * Constructor. This constructor will result in the underlying data collection being
     * immutable, so methods such as {@link #clear()} will throw an exception.
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    public OrderAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull Order[] objects) {
        this(context, resource, Arrays.asList(objects));
    }

    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param objects The objects to represent in the ListView.
     */
    public OrderAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Order> objects) {
        this(context, resource, objects, false);
    }

    private OrderAdapter(@NonNull Context context, @LayoutRes int resource,
                        @NonNull List<Order> objects, boolean objsFromResources) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mResource = mDropDownResource = resource;
        mObjects = objects;
        mObjectsFromResources = objsFromResources;
    }

    /**
     * Adds the specified object at the end of the array.
     *
     * @param object The object to add at the end of the array.
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void add(@Nullable Order object) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.add(object);
            } else {
                mObjects.add(object);
            }
            mObjectsFromResources = false;
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * Adds the specified Collection at the end of the array.
     *
     * @param collection The Collection to add at the end of the array.
     * @throws UnsupportedOperationException if the <tt>addAll</tt> operation
     *         is not supported by this list
     * @throws ClassCastException if the class of an element of the specified
     *         collection prevents it from being added to this list
     * @throws NullPointerException if the specified collection contains one
     *         or more null elements and this list does not permit null
     *         elements, or if the specified collection is null
     * @throws IllegalArgumentException if some property of an element of the
     *         specified collection prevents it from being added to this list
     */
    public void addAll(@NonNull Collection<? extends Order> collection) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.addAll(collection);
            } else {
                mObjects.addAll(collection);
            }
            mObjectsFromResources = false;
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * Adds the specified items at the end of the array.
     *
     * @param items The items to add at the end of the array.
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void addAll(Order... items) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                Collections.addAll(mOriginalValues, items);
            } else {
                Collections.addAll(mObjects, items);
            }
            mObjectsFromResources = false;
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * Inserts the specified object at the specified index in the array.
     *
     * @param object The object to insert into the array.
     * @param index The index at which the object must be inserted.
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void insert(@Nullable Order object, int index) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.add(index, object);
            } else {
                mObjects.add(index, object);
            }
            mObjectsFromResources = false;
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * Removes the specified object from the array.
     *
     * @param object The object to remove.
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void remove(@Nullable Order object) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.remove(object);
            } else {
                mObjects.remove(object);
            }
            mObjectsFromResources = false;
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * Remove all elements from the list.
     *
     * @throws UnsupportedOperationException if the underlying data collection is immutable
     */
    public void clear() {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                mOriginalValues.clear();
            } else {
                mObjects.clear();
            }
            mObjectsFromResources = false;
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    /**
     * Sorts the content of this adapter using the specified comparator.
     *
     * @param comparator The comparator used to sort the objects contained
     *        in this adapter.
     */
    public void sort(@NonNull Comparator<? super Order> comparator) {
        synchronized (mLock) {
            if (mOriginalValues != null) {
                Collections.sort(mOriginalValues, comparator);
            } else {
                Collections.sort(mObjects, comparator);
            }
        }
        if (mNotifyOnChange) notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mNotifyOnChange = true;
    }

    /**
     * Control whether methods that change the list ({@link #add}, {@link #addAll(Collection)},
     * {@link #addAll(Object[])}, {@link #insert}, {@link #remove}, {@link #clear},
     * {@link #sort(Comparator)}) automatically call {@link #notifyDataSetChanged}.  If set to
     * false, caller must manually call notifyDataSetChanged() to have the changes
     * reflected in the attached view.
     *
     * The default is true, and calling notifyDataSetChanged()
     * resets the flag to true.
     *
     * @param notifyOnChange if true, modifications to the list will
     *                       automatically call {@link
     *                       #notifyDataSetChanged}
     */
    public void setNotifyOnChange(boolean notifyOnChange) {
        mNotifyOnChange = notifyOnChange;
    }

    /**
     * Returns the context associated with this array adapter. The context is used
     * to create views from the resource passed to the constructor.
     *
     * @return The Context associated with this adapter.
     */
    public @NonNull Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return mObjects.size();
    }

    @Override
    public @Nullable
    Order getItem(int position) {
        return mObjects.get(position);
    }

    /**
     * Returns the position of the specified item in the array.
     *
     * @param item The item to retrieve the position of.
     *
     * @return The position of the specified item.
     */
    public int getPosition(@Nullable Order item) {
        return mObjects.indexOf(item);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public @NonNull View getView(int position, @Nullable View convertView,
                                 @NonNull ViewGroup parent) {
        return createViewFromResource(mInflater, position, convertView, parent, mResource);
    }

    private @NonNull View createViewFromResource(@NonNull LayoutInflater inflater, int position,
                                                 @Nullable View convertView, @NonNull ViewGroup parent, int resource) {
        final View view;
        final TextView nameView;
        final TextView descView;
        final TextView priceView;
        final TextView totalView;
        final ImageView avatarView;
        final Button addButton;
        final Button removeButton;

        if (convertView == null) {
            view = inflater.inflate(resource, parent, false);
        } else {
            view = convertView;
        }

        try {
            nameView = view.findViewById(R.id.nameOrderView);
            if (nameView == null) {
                throw new RuntimeException("Failed to find view with ID "
                        + mContext.getResources().getResourceName(R.id.nameOrderView)
                        + " in item layout");
            }
        } catch (ClassCastException e) {
            Log.e("OrderAdapter", "You must supply a name resource ID for a TextView");
            throw new IllegalStateException(
                    "OrderAdapter requires the name resource ID to be a TextView", e);
        }

        try {
            descView = view.findViewById(R.id.descOrderView);
            if (descView == null) {
                throw new RuntimeException("Failed to find view with ID "
                        + mContext.getResources().getResourceName(R.id.descOrderView)
                        + " in item layout");
            }
        } catch (ClassCastException e) {
            Log.e("OrderAdapter", "You must supply a desc resource ID for a TextView");
            throw new IllegalStateException(
                    "OrderAdapter requires the desc resource ID to be a TextView", e);
        }

        try {
            priceView = view.findViewById(R.id.priceOrderView);
            if (priceView == null) {
                throw new RuntimeException("Failed to find view with ID "
                        + mContext.getResources().getResourceName(R.id.priceOrderView)
                        + " in item layout");
            }
        } catch (ClassCastException e) {
            Log.e("OrderAdapter", "You must supply a price resource ID for a TextView");
            throw new IllegalStateException(
                    "OrderAdapter requires the price resource ID to be a TextView", e);
        }

        try {
            totalView = view.findViewById(R.id.totalOrderView);
            if (totalView == null) {
                throw new RuntimeException("Failed to find view with ID "
                        + mContext.getResources().getResourceName(R.id.totalOrderView)
                        + " in item layout");
            }
        } catch (ClassCastException e) {
            Log.e("OrderAdapter", "You must supply a total resource ID for a TextView");
            throw new IllegalStateException(
                    "OrderAdapter requires the total resource ID to be a TextView", e);
        }

        try {
            avatarView = view.findViewById(R.id.avatarOrderView);
            if (avatarView == null) {
                throw new RuntimeException("Failed to find view with ID "
                        + mContext.getResources().getResourceName(R.id.avatarOrderView)
                        + " in item layout");
            }
        } catch (ClassCastException e) {
            Log.e("OrderAdapter", "You must supply a avatar resource ID for a ImageView");
            throw new IllegalStateException(
                    "OrderAdapter requires the name resource ID to be a ImageView", e);
        }

        try {
            addButton = view.findViewById(R.id.addOrderBtn);
            if (addButton == null) {
                throw new RuntimeException("Failed to find view with ID "
                        + mContext.getResources().getResourceName(R.id.addOrderBtn)
                        + " in item layout");
            }
        } catch (ClassCastException e) {
            Log.e("OrderAdapter", "You must supply a addBtn resource ID for a Button");
            throw new IllegalStateException(
                    "OrderAdapter requires the addBtn resource ID to be a Button", e);
        }

        try {
            removeButton = view.findViewById(R.id.removeOrderBtn);
            if (removeButton == null) {
                throw new RuntimeException("Failed to find view with ID "
                        + mContext.getResources().getResourceName(R.id.removeOrderBtn)
                        + " in item layout");
            }
        } catch (ClassCastException e) {
            Log.e("OrderAdapter", "You must supply a removeBtn resource ID for a Button");
            throw new IllegalStateException(
                    "OrderAdapter requires the removeBtn resource ID to be a Button", e);
        }

        final Order item = getItem(position);
        assert item != null;
        nameView.setText(item.getName());
        descView.setText(item.getDesc());
        DecimalFormat decimalFormat = new DecimalFormat(Order.getPriceFormat());
        String priceTag = item.getUnit() + " " + decimalFormat.format(item.getPrice());
        priceView.setText(priceTag);
        totalView.setText(String.valueOf(item.getTotal()));
        avatarView.setImageResource(item.getAvatarResId());

        addButton.setOnClickListener(v -> {
            int total = item.getTotal();
            if (total >= 0) {
                item.setTotal(total + 1);
                totalView.setText(String.valueOf(item.getTotal()));
                if (total == 0) {
                    removeButton.animate().alpha(1);
                    totalView.animate().alpha(1);
                }
            }
        });

        removeButton.setOnClickListener(v -> {
            int total = item.getTotal();
            if (total > 0) {
                item.setTotal(total - 1);
                totalView.setText(String.valueOf(item.getTotal()));
                if (total == 1) {
                    removeButton.animate().alpha(0);
                    totalView.animate().alpha(0);
                }
            }
        });

        removeButton.setOnLongClickListener(v -> {
            item.setTotal(0);
            totalView.setText(String.valueOf(item.getTotal()));
            removeButton.animate().alpha(0);
            totalView.animate().alpha(0);
            return true;
        });

        return view;
    }

    /**
     * <p>Sets the layout resource to create the drop down views.</p>
     *
     * @param resource the layout resource defining the drop down views
     * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
     */
    public void setDropDownViewResource(@LayoutRes int resource) {
        this.mDropDownResource = resource;
    }

    /**
     * Sets the {@link Resources.Theme} against which drop-down views are
     * inflated.
     * <p>
     * By default, drop-down views are inflated against the theme of the
     * {@link Context} passed to the adapter's constructor.
     *
     * @param theme the theme against which to inflate drop-down views or
     *              {@code null} to use the theme from the adapter's context
     * @see #getDropDownView(int, View, ViewGroup)
     */
    public void setDropDownViewTheme(@Nullable Resources.Theme theme) {
        if (theme == null) {
            mDropDownInflater = null;
        } else if (theme == mInflater.getContext().getTheme()) {
            mDropDownInflater = mInflater;
        } else {
            final Context context = new ContextThemeWrapper(mContext, theme);
            mDropDownInflater = LayoutInflater.from(context);
        }
    }

    public @Nullable Resources.Theme getDropDownViewTheme() {
        return mDropDownInflater == null ? null : mDropDownInflater.getContext().getTheme();
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        final LayoutInflater inflater = mDropDownInflater == null ? mInflater : mDropDownInflater;
        return createViewFromResource(inflater, position, convertView, parent, mDropDownResource);
    }

    @Override
    public @NonNull Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    /**
     * {@inheritDoc}
     *
     * @return values from the string array used by {@link #createFromResource(Context, int, int)},
     * or {@code null} if object was created otherwsie or if contents were dynamically changed after
     * creation.
     */
    @Override
    public CharSequence[] getAutofillOptions() {
        // First check if app developer explicitly set them.
        final CharSequence[] explicitOptions = super.getAutofillOptions();
        if (explicitOptions != null) {
            return explicitOptions;
        }

        // Otherwise, only return options that came from static resources.
        if (!mObjectsFromResources || mObjects == null || mObjects.isEmpty()) {
            return null;
        }
        final int size = mObjects.size();
        final CharSequence[] options = new CharSequence[size];
        mObjects.toArray(options);
        return options;
    }

    /**
     * <p>An array filter constrains the content of the array adapter with
     * a prefix. Each item that does not start with the supplied prefix
     * is removed from the list.</p>
     */
    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            final FilterResults results = new FilterResults();

            if (mOriginalValues == null) {
                synchronized (mLock) {
                    mOriginalValues = new ArrayList<>(mObjects);
                }
            }

            if (prefix == null || prefix.length() == 0) {
                final ArrayList<Order> list;
                synchronized (mLock) {
                    list = new ArrayList<>(mOriginalValues);
                }
                results.values = list;
                results.count = list.size();
            } else {
                final String prefixString = prefix.toString().toLowerCase();

                final ArrayList<Order> values;
                synchronized (mLock) {
                    values = new ArrayList<>(mOriginalValues);
                }

                final int count = values.size();
                final ArrayList<Order> newValues = new ArrayList<>();

                for (int i = 0; i < count; i++) {
                    final Order value = values.get(i);
                    final String valueText = value.toString().toLowerCase();

                    // First match against the whole, non-splitted value
                    if (valueText.startsWith(prefixString)) {
                        newValues.add(value);
                    } else {
                        final String[] words = valueText.split(" ");
                        for (String word : words) {
                            if (word.startsWith(prefixString)) {
                                newValues.add(value);
                                break;
                            }
                        }
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            mObjects = (List<Order>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
}