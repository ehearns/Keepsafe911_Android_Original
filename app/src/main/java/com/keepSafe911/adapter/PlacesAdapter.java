package com.keepSafe911.adapter;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import com.keepSafe911.utils.Utils;

import java.util.ArrayList;

public class PlacesAdapter extends ArrayAdapter implements Filterable {
    private ArrayList resultList;
    private Context context;

    public PlacesAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
        this.context = context;
    }

    @Override
    public int getCount() {
        return resultList.size();
    }

    @Override
    public Object getItem(int position) {
        if (resultList.size() > 0) {
            return resultList.get(position);
        }
        return position;
    }

    @Override
    public Filter getFilter() {
        Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();

                try {
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        if (Utils.INSTANCE.AutocompletePlaces(context,constraint.toString()) != null && Utils.INSTANCE.AutocompletePlaces(context,constraint.toString()).size() > 0) {
                            resultList = Utils.INSTANCE.AutocompletePlaces(context,constraint.toString());
                        }
                        Log.e("ResultList", "" + resultList);

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return filter;
    }
}