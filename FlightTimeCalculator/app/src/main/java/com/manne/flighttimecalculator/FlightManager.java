package com.manne.flighttimecalculator;

import android.widget.AutoCompleteTextView;
import android.widget.EditText;

/**
 * A class to represent a collection of the various UI text input elements
 * associated with a flight. In addition to all departure and arrival
 * inputs, it will also store (not at instantiation) the String values
 * contained within these inputs.
 */
class FlightManager
{
    // The input fields.
    final private AutoCompleteTextView depAirportInput;
    final private AutoCompleteTextView arrAirportInput;
    final private EditText arrTimeInput;
    final private EditText depTimeInput;
    final private EditText depDateInput;
    final private EditText arrDateInput;
    final private String[] flightDetails;


    /**
     * A constructor.
     *
     * @param depAirportInput   the input View for the departure airport of this flight
     * @param arrAirportInput   the input View for the arrival airport of this flight
     * @param depDateInput      the input View for the departure date of this flight
     * @param arrDateInput      the input View for the arrival date of this flight
     * @param depTimeInput      the input View for the departure time of this flight
     * @param arrTimeInput      the input View for the arrival time of this flight
     */
    FlightManager(AutoCompleteTextView depAirportInput,
                  AutoCompleteTextView arrAirportInput,
                  EditText depDateInput,
                  EditText arrDateInput,
                  EditText depTimeInput,
                  EditText arrTimeInput)
    {
        this.depAirportInput = depAirportInput;
        this.arrAirportInput = arrAirportInput;
        this.depDateInput = depDateInput;
        this.arrDateInput = arrDateInput;
        this.depTimeInput = depTimeInput;
        this.arrTimeInput = arrTimeInput;

        // A String array that will store the fetched values from all the
        // inputs above so that they can be processed in the worker thread.
        flightDetails = new String[6];
    }


    /**
     * @return  the departure airport input View
     */
    AutoCompleteTextView getDepAirportInput()
    {
        return depAirportInput;
    }


    /**
     * @return  the arrival airport input View
     */
    AutoCompleteTextView getArrAirportInput()
    {
        return arrAirportInput;
    }


    /**
     * @return  the departure date input View
     */
    EditText getDepDateInput()
    {
        return depDateInput;
    }


    /**
     * @return  the arrival date input View
     */
    EditText getArrDateInput()
    {
        return arrDateInput;
    }


    /**
     * @return  the departure time input View
     */
    EditText getDepTimeInput()
    {
        return depTimeInput;
    }


    /**
     * @return  the arrival time input View
     */
    EditText getArrTimeInput()
    {
        return arrTimeInput;
    }


    /**
     * @return  the String array storing the String values of all flight detail inputs.
     */
    String[] getFlightDetails()
    {
        return flightDetails;
    }


    /**
     * @return  whether or not the departure airport input View is filled
     */
    boolean hasDepAirportInput()
    {
        return !getDepAirportInput().getText().toString().equals("");
    }


    /**
     * @return  whether or not the arrival airport input View is filled
     */
    boolean hasArrAirportInput()
    {
        return !getArrAirportInput().getText().toString().equals("");
    }


    /**
     * @return  whether or not the departure date input View is filled
     */
    boolean hasDepDateInput()
    {
        return !getDepDateInput().getText().toString().equals("");
    }


    /**
     * @return  whether or not the arrival date input View is filled
     */
    boolean hasArrDateInput()
    {
        return !getArrDateInput().getText().toString().equals("");
    }


    /**
     * @return  whether or not the departure time input View is filled
     */
    boolean hasDepTimeInput()
    {
        return !getDepTimeInput().getText().toString().equals("");
    }


    /**
     * @return  whether or not the arrival time input View is filled
     */
    boolean hasArrTimeInput()
    {
        return !getArrTimeInput().getText().toString().equals("");
    }


    /**
     * @return  whether or not all the input Views are filled
     */
    boolean isFilled()
    {
        return hasDepAirportInput() &&
               hasArrAirportInput() &&
               hasDepDateInput() &&
               hasArrDateInput() &&
               hasDepTimeInput() &&
               hasArrTimeInput();
    }


    /**
     * A method to fetch all the String values from the input Views and place
     * them in the flightDetails String array instance variable. This allows
     * access to the information from worker threads.
     */
    void updateFlightDetails()
    {
        flightDetails[0] = getDepAirportInput().getText().toString();
        flightDetails[1] = getArrAirportInput().getText().toString();
        flightDetails[2] = getDepDateInput().getText().toString();
        flightDetails[3] = getArrDateInput().getText().toString();
        flightDetails[4] = getDepTimeInput().getText().toString();
        flightDetails[5] = getArrTimeInput().getText().toString();
    }
}
