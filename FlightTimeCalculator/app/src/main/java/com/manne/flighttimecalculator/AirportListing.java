/*
 * Copyright 2019 Manne Hedlund
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

package com.manne.flighttimecalculator;

import android.app.Activity;
import androidx.annotation.NonNull;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class to represent the details of an airport. Each instance will
 * store the name of an airport, its location in the form of a city,
 * country and geographical coordinates. It will also store a timezone
 * which will be assigned 78.9 upon instantiation to represent its absence.
 **/
class AirportListing
{
    // The airport details.
    final private String name;
    final private String city;
    final private String country;
    final private String code;
    final private double lat;
    final private double lon;
    private double timezone;


    /**
     * A constructor.
     *
     * @param name      the name of the airport
     * @param city      the city in which the airport is located
     * @param country   the country in which the airport is located
     * @param code      the 3 letter IATA code used as the airport's identifier
     * @param lat       the latitude of the airports geographical position
     * @param lon       the longitude of the airports geographical position
     */
    AirportListing(String name,
                   String city,
                   String country,
                   String code,
                   double lat,
                   double lon)
    {
        // Remove unnecessary "".
        this.name = name.replace("\"", "");
        // Remove unnecessary "".
        this.city = city.replace("\"", "");
        // Remove unnecessary "".
        this.country = country.replace("\"", "");

        // Absent airport codes will appear as \N in the airports data file.
        // When such a code is passed to this constructor, it is replaced with
        // an empty String which is easier to deal with.
        if (code.contains("\\N"))
        {
            this.code = "";
        }
        // In cases where an airport code is present, remove unnecessary "".
        else
        {
            this.code = code.replace("\"", "");
        }

        this.lat = lat;
        this.lon = lon;

        // The timezone instance variable will be given 78.9 here to indicate
        // that it has not been assigned a proper timezone yet.
        timezone = 78.9;
    }


    /**
     * A method which takes the geographical coordinates of this AirportListing
     * and the supplied date timestamp to make a request to the Google TimeZone API
     * and assign the returned DST timezone offset to the timezone instance variable.
     *
     * @param timestamp a date and time in the form of Epoch seconds timestamp
     * @param key       the Google authentication key to validate the request
     * @param context   the required context
     */
    void requestTimezone(long timestamp, String key, Activity context)
    {
        // An instance of RequestQueue to manage worker threads.
        RequestQueue requestQueue = Volley.newRequestQueue(context);

        // The URL String comprising the Google Time Zone API request,
        // being fed the location, the timestamp and the authentication key.
        final String url = "https://maps.googleapis.com/maps/api/timezone/json?location=" + getLocation() + "&timestamp=" + timestamp + "&key=" + key;

        // Request a String response after providing the request URL,
        // implementing a listener for the response event.
        StringRequest request = new StringRequest(
                Request.Method.POST,
                url,
                new Response.Listener<String>()
                {
                    /**
                     * Method triggered in the event of a response returned by the request.
                     * In this case, the returned data is used to generate a proper timezone
                     * for this airport.
                     *
                     * @param   response    the String response of the request containing
                     *                      timezone information of this airports location
                     *                      at the time given.
                     */
                    @Override
                    public void onResponse(String response)
                    {
                        try
                        {
                            // Assign the returned response String to a JSON object.
                            JSONObject jsonResponse = new JSONObject(response);

                            // Fetch the value associated with the rawOffset key in the JSON object
                            // and store it as a double. It is the UTC timezone offset in seconds.
                            double rawOffsetSeconds = Double.parseDouble(
                                    jsonResponse.getString("rawOffset")
                            );
                            // Fetch the value associated with the dstOffset key in the JSON object
                            // and store it as a double. It is the additional offset due to DST
                            // in seconds.
                            double dstOffsetSeconds = Double.parseDouble(
                                    jsonResponse.getString("dstOffset")
                            );

                            // Store the two added offsets together and convert it to hours.
                            double totalOffsetHours = (rawOffsetSeconds + dstOffsetSeconds) / 3600;

                            // Assign the resulting value to the timezone of this airport
                            // with setter.
                            setTimezone(totalOffsetHours);
                        }
                        // Handle any JSON related exceptions.
                        catch (JSONException exception)
                        {
                            exception.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    /**
                     * Method triggered by an error event as a result of the request.
                     * In such cases, assign the value 99.9 to this airports timezone
                     * as a means of indicating an error.
                     *
                     * @param   error the error
                     */
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        setTimezone(99.9);
                    }
                }
        );

        // Add the request to the RequestQueue.
        requestQueue.add(request);
    }


    /**
     * @param timezone  the required timezone of this airport
     */
    private void setTimezone(double timezone)
    {
        this.timezone = timezone;
    }


    /**
     * @return  the name of this airport
     */
    String getName()
    {
        return name;
    }


    /**
     * @return  the city of this airport
     */
    String getCity()
    {
        return city;
    }


    /**
     * @return  the country of this airport
     */
    String getCountry()
    {
        return country;
    }


    /**
     * @return  the airport code of this airport
     */
    String getCode()
    {
        return code;
    }


    /**
     * @return  the latitude and longitude of this airport, separated by a comma
     */
    private String getLocation()
    {
        return "" + lat + "," + lon;
    }


    /**
     * @return  the timezone of this airport
     */
    double getTimezone()
    {
        return timezone;
    }


    /**
     * Overriding toString method of this class.
     *
     * @return  the name of this airport if no airport code is present,
     *          the airport code followed by an em dash and the name otherwise
     */
    @NonNull
    @Override
    public String toString()
    {
        if (code.equals(""))
        {
            return name;
        }
        else
        {
            return code + " \u2014 " + name;
        }
    }
}
