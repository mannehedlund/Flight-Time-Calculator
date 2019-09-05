package com.manne.flighttimecalculator;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent my custom ArrayAdapter with AirportListing as
 * the List type parameter. It is used to filter all AirportListing items
 * from the supplied List and display them in the UI.
 */
class AirportListAdapter extends ArrayAdapter<AirportListing>
{
    // The full un-filtered AirportListing List loaded
    // with data from the airports data file.
    final private List<AirportListing> airportListFull;


    /**
     * A constructor.
     *
     * @param context       the required context
     * @param airportList   the AirportListing List to be filtered and displayed
     */
    AirportListAdapter(@NonNull Context context,
                       @NonNull List<AirportListing> airportList)
    {
        super(context, 0, airportList);
        airportListFull = new ArrayList<>(airportList);
    }


    /**
     * @return  the custom Filter instance
     */
    @NonNull
    @Override
    public Filter getFilter()
    {
        return airportFilter;
    }


    /**
     * The method used to display an item of the List in the UI.
     * It is called when the List is first displayed, the view of
     * the list changes or when the list itself changes.
     *
     * @param position      the position of the item in the List which is of interest
     * @param convertView   the View to use or re-use
     * @param parent        the parent ViewGroup into which the convertView can be inflated
     * @return              the modified convertView
     */
    @NonNull
    @Override
    public View getView(int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent)
    {
        // If the convertView is not already displayed, inflate it.
        if (convertView == null)
        {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.airport_listing_row, parent, false
            );
        }

        // The airport name of an AirportListing in the popup list.
        TextView airportNameTextView = convertView.findViewById(R.id.airportName);
        // The airport code of an AirportListing in the popup list.
        TextView airportCodeTextView = convertView.findViewById(R.id.airportCode);
        // The airport city and country of an AirportListing in the popup list.
        TextView airportCityCountryTextView = convertView.findViewById(R.id.airportCityCountry);

        // Fetch the AirportListing at the required position in the List.
        AirportListing airportItem = getItem(position);

        // If not null, place the name, code, city and country of an AirportListing
        // onto a row dedicated to that AirportListing.
        if (airportItem != null)
        {
            airportNameTextView.setText(airportItem.getName());

            airportCodeTextView.setText(airportItem.getCode());

            String airportCityCountry = airportItem.getCity() + ", " + airportItem.getCountry();

            airportCityCountryTextView.setText(airportCityCountry);
        }

        // Return the inflated View.
        return convertView;
    }


    /**
     * An instance of Filter with own method implementations to
     * provide the required filtering behaviour.
     */
    private final Filter airportFilter = new Filter()
    {
        /**
         * A method to filter the List in accordance with the inputted
         * sequence of characters. Invoked in a worker thread.
         *
         * @param stringInput   the inputted text which should constrain the List
         * @return              the result of the filtering process
         */
        @Override
        protected FilterResults performFiltering(CharSequence stringInput)
        {
            // A variable to store the results of the filtering process.
            FilterResults results = new FilterResults();

            // A List to store the filtered contents of the full AirportListings List.
            List<AirportListing> suggestions = new ArrayList<>();

            // If there are no inputted characters, suggest full List.
            if (stringInput == null || stringInput.length() == 0)
            {
                suggestions.addAll(airportListFull);
            }
            // Otherwise, the full List needs to be filtered.
            else
            {
                // A String of the inputted characters in lower-case and trimmed.
                String filteredInput = stringInput.toString().toLowerCase().trim();

                // Check for each AirportListing item in the full List.
                for (AirportListing item : airportListFull)
                {
                    // If the item's airport name, city, country or code (in lower-case)
                    // starts with the inputted String value. If so, add the item to the
                    // suggestions List.
                    if (
                            item.getName().toLowerCase().startsWith(filteredInput) ||
                            item.getCity().toLowerCase().startsWith(filteredInput) ||
                            item.getCountry().toLowerCase().startsWith(filteredInput) ||
                            item.getCode().toLowerCase().startsWith(filteredInput)
                        )
                    {
                        suggestions.add(item);
                    }
                    // Otherwise, split the item's airport name by hyphens or spaces
                    // and check if it matches any of the split words instead. If so,
                    // add the item to the suggestions List.
                    else
                    {
                        String[] splitAirportName = item.getName().split("[- ]");

                        for (String word : splitAirportName)
                        {
                            if (word.toLowerCase().startsWith(filteredInput))
                            {
                                suggestions.add(item);
                            }
                        }
                    }
                }
            }

            // Place the suggestions List in the FilterResults instance to be returned.
            results.values = suggestions;
            // Place the size of the suggestions List in the FilterResults as well.
            results.count = suggestions.size();

            // Return the FilterResults instance.
            return results;
        }


        /**
         * A method to display the filtering results. Invoked in the UI thread.
         *
         * @param stringInput   the inputted text which should constrain the List
         * @param results       the results of the filtering process to be displayed in the UI
         */
        @Override
        protected void publishResults(CharSequence stringInput,
                                      FilterResults results)
        {
            // Clears the List.
            clear();

            // Fills the List with the obtained filter results.
            addAll((List) results.values);

            // Call for the UI to be updated.
            notifyDataSetChanged();
        }



        /**
         * A method to convert a resulting AirportListing value from
         * the filtering process into a readable CharSequence.
         *
         * @param resultValue   the AirportListing to convert into a CharSequence
         * @return              a readable CharSequence representing the AirportListing
         */
        @Override
        public CharSequence convertResultToString(Object resultValue)
        {
            return resultValue.toString();
        }
    };
}
