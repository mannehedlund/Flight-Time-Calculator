package com.manne.flighttimecalculator;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.temporal.ChronoUnit;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The main Activity, containing most of the UI.
 * From this Activity, the user can enter any number of
 * flight details and calculate the total flight time duration.
 * The calculation logic is put in a nested class extending AsyncTask,
 * which runs the calculation in a worker thread and displays
 * (from the UI thread) the result in an AlertDialog.
 */
public class MainActivity extends AppCompatActivity
{
    // A scrollable View..
    private ScrollView scrollView;

    // The layout containing all content.
    private LinearLayout mainLayout;

    // A Button which adds a flight upon click.
    private Button addFlightBtn;
    // A Button which adds a flight upon click.
    private Button deleteFlightBtn;
    // A Button which calculates flight time upon click.
    private Button calculateBtn;

    // A List of type AirportListing to contain the details of all the airports.
    private List<AirportListing> airports;
    // A list of type FlightManager to contain the details of all the flights
    // inputted by the user.
    private List<FlightManager> flights;

    // A Map which maps every airport's toString() result to its
    // AirportListing instance.
    final private Map<String, AirportListing> airportFinder = new HashMap<>();

    // A boolean to determine whether or not the departure date
    // input should be automatically selected. This will be true,
    // unless the departure airport input has been pre-filled.
    private boolean shouldAutoSelectTempDepDate;

    // A popup AlertDialog to contain the View with calculation results.
    private AlertDialog calculationDialog;
    // The View which will display the calculation results and associated content.
    private View calculationView;
    // A progressbar to display progress in the calculation popup dialog.
    private ProgressBar progressBar;


    /**
     * A method which initialises the Activity,
     *
     * @param savedInstanceState    the previous instance state which is
     *                              null unless Activity is being re-initialised
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Set the content view, defining the UI.
        setContentView(R.layout.activity_main);

        // Get the app toolbar by id.
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Set the text color of the title in the toolbar.
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitleTheme);
        // Set this toolbar as the ActionBar.
        toolbar.setTitleTextColor(getResources().getColor(R.color.black1));
        // Set the text style of the toolbar, which currently only
        // sets the title's text size.
        setSupportActionBar(toolbar);

        // Get the ActionBar.
        ActionBar actionBar = getSupportActionBar();

        // If the ActionBar exists, set and display the app logo in it.
        if (actionBar!= null)
        {
            actionBar.setLogo(R.mipmap.ic_logo);
            actionBar.setDisplayUseLogoEnabled(true);
        }

        // Getting scrollView by id.
        scrollView = findViewById(R.id.scrollView);
        // Getting mainLayout by id.
        mainLayout = findViewById(R.id.mainLayout);
        // Getting addFlightBtn by id.
        addFlightBtn = findViewById(R.id.addFlightBtn);
        // Getting deleteFlightBtn by id.
        deleteFlightBtn = findViewById(R.id.deleteFlightBtn);
        // Getting calculateBtn by id.
        calculateBtn = findViewById(R.id.calculateBtn);

        // Set an onFocusChangeListener to mainLayout to improve UX. If focused,
        // the keyboard will be hidden after a short delay.
        mainLayout.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View view, boolean isFocused)
            {
                if (isFocused)
                {
                    mainLayout.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            hideSoftKeyboard();
                        }
                    }, 100);
                }
            }
        });

        // Assign an ArrayList to the flights List.
        flights = new ArrayList<>();

        // Fill the airports List with an AirportListing for every
        // airport in the airports data file.
        fillAirportList();

        // The custom Adapter to deal with the filtering and
        // display of the airports List.
        final AirportListAdapter adapter = new AirportListAdapter(this, airports);

        // Adds the first flight, i.e. an empty collection of flight input fields,
        // with the adapter passed as a parameter in order to allow airport
        // suggestions in the airport inputs.
        addFlight(adapter);

        // Set an onClickListener to addFlightBtn which adds another flight upon click.
        addFlightBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                addFlight(adapter);
            }
        });

        // Disable deleteFlightBtn initially, since there should be
        // a minimum of one flight.
        deleteFlightBtn.setEnabled(false);

        // Set an onClickListener to deleteFlightBtn which removes
        // the last flight upon click.
        deleteFlightBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (flights.size() > 1)
                {
                    deleteFlight();
                }
            }
        });

        // Set an onClickListener to calculateBtn which starts a custom
        // AsyncTask to calculate the flight time with use of a worker thread.
        calculateBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new  CalculationTask(MainActivity.this).execute();
            }
        });
    }


    /**
     * A method which sets a custom options menu, i.e. the app's ActionBar layout.
     *
     * @param menu  the current options menu
     * @return      whether menu should be displayed
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Get the menu inflater.
        MenuInflater inflater = getMenuInflater();

        // Inflate own menu resource.
        inflater.inflate(R.menu.main_menu, menu);

        return true;
    }


    /**
     * A method which is called whenever an item in the options menu
     * is selected. This menu will have two items, Clear and About.
     * Selecting either will be handled here in separate cases.
     *
     * @param item  the menu item selected
     * @return      whether menu processing may proceed
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.clear:
                clearAllInputs();
                return true;
            case R.id.about:
                openAboutActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * A method to launch the AboutActivity.
     */
    private void openAboutActivity()
    {
        // Create the required intent, i.e. a description of AboutActivity.
        Intent intent = new Intent(this, AboutActivity.class);

        // Start the intent.
        startActivity(intent);

        // Allow for a smooth transition to the new Activity.
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }


    /**
     * A method to configure the logic of an airport AutoCompleteTextView input.
     *
     * @param requiredAirportInput  the required AutoCompleteTextView input to configure
     * @param adapter               the adapter which will provide the AutoCompleteTextView
     *                              input with custom auto-complete behaviour
     */
    private void configureAirportInputLogic(AutoCompleteTextView requiredAirportInput,
                                            AirportListAdapter adapter)
    {
        // Make the airport input final to allow access within inner class.
        final AutoCompleteTextView airportInput = requiredAirportInput;

        // Make the auto-complete suggestions appear after only
        // 1 character has been entered into the input.
        airportInput.setThreshold(1);

        // Set the adapter of the input to the custom one provided.
        airportInput.setAdapter(adapter);

        // Set an onClickListener to select its entire inputted text upon click.
        // This will allow users to more easily correct their mistakes.
        airportInput.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                airportInput.setSelection(0, airportInput.getText().length());
            }
        });

        // Set an onFocusChangeListener to the airport input to improve UX. If focused,
        // the keyboard will be shown after a short delay.
        airportInput.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View view, boolean isFocused)
            {
                if (isFocused)
                {
                    airportInput.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            showSoftKeyboard();
                        }
                    }, 100);
                }
            }
        });
    }


    /**
     * A method to configure the logic of a date EditText input.
     *
     * @param requiredDateInput the required EditText input to configure
     * @param isDeparture       whether or not this input is for a departure date
     */
    private void configureDateInputLogic(EditText requiredDateInput,
                                         final boolean isDeparture)
    {
        // Make the date input final to allow access within inner classes.
        final EditText dateInput = requiredDateInput;

        // Store the index of this flight for later. Note that this
        // flight has not yet been added to the flights List.
        final int flightIndex = flights.size();

        // Set an onClickListener for whenever the date input is clicked.
        dateInput.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Remove focus from any airport input if necessary.
                mainLayout.requestFocus();

                // Assign the current date to the default date,
                // i.e. the preselected date.
                LocalDate defaultDate = LocalDate.now();

                // If date input is already filled with a date,
                // make that date the default date.
                if (!dateInput.getText().toString().equals(""))
                {
                    defaultDate = parseDate(dateInput.getText().toString());
                }
                else
                {
                    // Check the date input is for a departure flight.
                    if (isDeparture)
                    {
                        // And if there are more than one flight, check if the arrival date
                        // input for the previous flight is filled with a date. If so,
                        // then set that date as the default date.
                        if (flights.size() > 1)
                        {
                            FlightManager previousFlight = flights.get(flightIndex - 1);

                            if (!previousFlight.getArrDateInput().getText().toString().equals(""))
                            {
                                defaultDate = parseDate(
                                        previousFlight.getArrDateInput().getText().toString()
                                );
                            }
                        }
                    }
                    // Otherwise, the date input is for an arrival flight.
                    // Check if the preceding departure date is filled with a date.
                    // If so, then set that date as the default date.
                    else
                    {
                        FlightManager currentFlight = flights.get(flightIndex);

                        if (!currentFlight.getDepDateInput().getText().toString().equals(""))
                        {
                            defaultDate = parseDate(
                                    currentFlight.getDepDateInput().getText().toString()
                            );
                        }
                    }
                }

                // A DatePickerDialog for user to select a date with. It will
                // take the default date and use that as the preselected date.
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        MainActivity.this,
                        new DatePickerDialog.OnDateSetListener()
                        {
                            /**
                             * This will be triggered upon clicking Ok with a selected date.
                             *
                             * @param datePicker    the picker associated with this Dialog
                             * @param year          the year selected
                             * @param month         the month selected
                             * @param day           the day selected
                             */
                            @Override
                            public void onDateSet(DatePicker datePicker,
                                                  int year,
                                                  int month,
                                                  int day)
                            {
                                // Increment the month parameter, to fix it being
                                // initially indexed starting from 0.
                                month++;

                                // Construct a date from the date parameters given.
                                LocalDate date = LocalDate.of(year, month, day);

                                // Format that date into a custom String.
                                String formattedDate = date.format(
                                        DateTimeFormatter.ofPattern("dd-MMM-yyyy")
                                );

                                // Place that String in the date input.
                                dateInput.setText(formattedDate);
                            }
                        },
                        defaultDate.getYear(),
                        defaultDate.getMonthValue() - 1,
                        defaultDate.getDayOfMonth()
                );

                // The minimum date allowed for selection by the date picker.
                // This will be initialised as the current date.
                LocalDate minDate = LocalDate.now();

                // Check the date input is for a departure flight.
                if (isDeparture)
                {
                    // And if there are more than one flight, check if the arrival date
                    // input for the previous flight is filled with a date. If so,
                    // then set that date as the min date.
                    if (flights.size() > 1)
                    {
                        FlightManager previousFlight = flights.get(flightIndex - 1);

                        if (!previousFlight.getArrDateInput().getText().toString().equals(""))
                        {
                            minDate = parseDate(
                                    previousFlight.getArrDateInput().getText().toString()
                            );
                        }
                    }
                }
                // Otherwise, the date input is for an arrival flight.
                // Check if the preceding departure date is filled with a date.
                // If so, then set that date as the min date.
                else
                {
                    FlightManager currentFlight = flights.get(flightIndex);

                    if (!currentFlight.getDepDateInput().getText().toString().equals(""))
                    {
                        minDate = parseDate(
                                currentFlight.getDepDateInput().getText().toString()
                        );
                    }
                }

                // Set minDate as the minimum date for the picker
                // associated with the DatePickerDialog. This will
                // prevent calculations of flights in the past.
                datePickerDialog.getDatePicker().setMinDate(
                        minDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                );

                // Show the DatePickerDialog.
                datePickerDialog.show();
            }
        });

        // Place focus on mainLayout after configuring date input.
        mainLayout.requestFocus();
    }


    /**
     * A method to configure the logic of a time EditText input.
     *
     * @param requiredTimeInput the required EditText input to configure
     */
    private void configureTimeInputLogic(EditText requiredTimeInput)
    {
        // Make the time input final to allow access within inner classes.
        final EditText timeInput = requiredTimeInput;

        // Set an onClickListener for whenever the time input is clicked.
        timeInput.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                // Remove focus from any airport input if necessary.
                mainLayout.requestFocus();

                // A TimePickerDialog for user to select a time with. It will use
                // 00:00 as the preselected time and be configured in a 24h standard.
                TimePickerDialog timePickerDialog = new TimePickerDialog(
                        MainActivity.this,
                        new TimePickerDialog.OnTimeSetListener()
                        {
                            /**
                             * This will be triggered upon clicking Ok with a selected time.
                             *
                             * @param timePicker    the picker associated with this Dialog
                             * @param hour          the hour selected
                             * @param minute        the minute selected
                             */
                            public void onTimeSet(TimePicker timePicker,
                                                  int hour,
                                                  int minute)
                            {
                                // Place an appropriately formatted String of the
                                // time parameters given into the time input.
                                timeInput.setText(String.format(
                                        Locale.getDefault(),
                                        "%02d:%02d", hour, minute
                                ));
                            }
                        },
                        0,
                        0,
                        true
                );

                // Show the TimePickerDialog.
                timePickerDialog.show();
            }
        });

        // Place focus on mainLayout after configuring time input.
        mainLayout.requestFocus();
    }


    /**
     * A method to configure the layout of an airport AutoCompleteTextView input.
     *
     * @param airportInput  the required AutoCompleteTextView input to configure
     * @param isDeparture   whether or not this input is for a departure airport
     */
    private void configureAirportInputStyle(AutoCompleteTextView airportInput,
                                            boolean isDeparture)
    {
        // Create appropriate layout parameters.
        LinearLayout.LayoutParams airportLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Set the margins of those layout parameters.
        airportLayoutParams.setMargins(
                toPixels(10),
                0,
                toPixels(10),
                0
        );

        // Assign those layout parameters to the input View.
        airportInput.setLayoutParams(airportLayoutParams);

        // Set the width of the airport suggestions dropdown list.
        airportInput.setDropDownWidth(LinearLayout.LayoutParams.WRAP_CONTENT);

        // Select entire inputted text upon focus. This will allow
        // users to more easily correct their mistakes.
        airportInput.setSelectAllOnFocus(true);

        // Set the padding of the View.
        airportInput.setPadding(
                0,
                0,
                toPixels(10),
                0
        );

        // Making text compatible with multiple lines and disabling spell checking.
        airportInput.setRawInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        // Center the text vertically.
        airportInput.setGravity(Gravity.CENTER_VERTICAL);

        // Set the text color.
        airportInput.setTextColor(getResources().getColor(R.color.black2));

        // Set the text size.
        airportInput.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);

        // Set the background drawable of the View.
        airportInput.setBackgroundResource(R.drawable.bg_input);

        // If the input is for a departure airport, set the drawable
        // icon of the input to reflect that.
        if (isDeparture)
        {
            airportInput.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_dep_airport_input_1,
                    0,
                    0,
                    0
            );
        }
        // Otherwise, set the drawable icon of the input to
        // reflect that it is for an arrival airport.
        else
        {
            airportInput.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ic_arr_airport_input_1,
                    0,
                    0,
                    0
            );
        }

        // Set the padding of the drawable icon.
        airportInput.setCompoundDrawablePadding(toPixels(10));

        // Make the empty text display a hint.
        airportInput.setHint("Enter airport");
    }


    /**
     * A method to configure the layout of a date EditText input.
     *
     * @param dateInput the required EditText input to configure
     */
    private void configureDateInputStyle(EditText dateInput)
    {
        // Create appropriate layout parameters.
        RelativeLayout.LayoutParams dateLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        // Set the margins of those layout parameters.
        dateLayoutParams.setMargins(
                toPixels(10),
                0,
                toPixels(130),
                0
        );

        // Align the input View to the left within its parent RelativeLayout.
        dateLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        // Assign those layout parameters to the input View.
        dateInput.setLayoutParams(dateLayoutParams);

        // Center the text vertically.
        dateInput.setGravity(Gravity.CENTER_VERTICAL);

        // Set the input to not focusable, as a separate Dialog will
        // let user set the text of the View.
        dateInput.setFocusable(false);

        // Set the input to not focusable.
        dateInput.setFocusableInTouchMode(false);

        // Set the input to clickable, as this is how its Dialog will be summoned.
        dateInput.setClickable(true);

        // Set the padding of the View.
        dateInput.setPadding(
                0,
                0,
                0,
                0
        );

        // Set the text color.
        dateInput.setTextColor(getResources().getColor(R.color.black2));

        // Set the text size.
        dateInput.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);

        // Set the text Ems, i.e. a uniform width of the View
        // independent of different digit widths.
        dateInput.setEms(10);

        // Set its input type as null.
        dateInput.setInputType(InputType.TYPE_NULL);

        // Set the background drawable of the View.
        dateInput.setBackgroundResource(R.drawable.bg_input);

        // Set the drawable icon of the input to reflect that it is for a date.
        dateInput.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_date_input,
                0,
                0,
                0
        );

        // Set the padding of the drawable icon.
        dateInput.setCompoundDrawablePadding(toPixels(10));

        // Make the empty text display a hint.
        dateInput.setHint("Enter date");
    }


    /**
     * A method to configure the layout of a time EditText input.
     *
     * @param timeInput the required EditText input to configure
     */
    private void configureTimeInputStyle(EditText timeInput)
    {
        // Create appropriate layout parameters.
        RelativeLayout.LayoutParams timeLayoutParams = new RelativeLayout.LayoutParams(
                toPixels(110),
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        // Set the margins of those layout parameters.
        timeLayoutParams.setMargins(
                0,
                0,
                toPixels(10),
                0
        );

        // Align the input View to the right within its parent RelativeLayout.
        timeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

        // Assign those layout parameters to the input View.
        timeInput.setLayoutParams(timeLayoutParams);

        // Set the input to not focusable, as a separate Dialog will
        // let user set the text of the View.
        timeInput.setFocusable(false);

        // Set the input to not focusable.
        timeInput.setFocusableInTouchMode(false);

        // Set the input to clickable, as this is how its Dialog will be summoned.
        timeInput.setClickable(true);

        // Set the padding of the View.
        timeInput.setPadding(
                0,
                0,
                0,
                0
        );

        // Set the text color.
        timeInput.setTextColor(getResources().getColor(R.color.black2));

        // Set the text size.
        timeInput.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);

        // Set the text Ems, i.e. a uniform width of the View
        // independent of different digit widths.
        timeInput.setEms(10);

        // Center the text.
        timeInput.setGravity(Gravity.CENTER);

        // Set its input type as null.
        timeInput.setInputType(InputType.TYPE_NULL);

        // Set the background drawable of the View.
        timeInput.setBackgroundResource(R.drawable.bg_input);

        // Set the drawable icon of the input to reflect that it is for a time.
        timeInput.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_time_input,
                0,
                0,
                0
        );

        // Make the empty text display a hint.
        timeInput.setHint("- - : - -");
    }


    /**
     * A method to configure the layout containing some airport input.
     *
     * @param airportLayout the required LinearLayout to configure
     */
    private void configureAirportLayout(LinearLayout airportLayout)
    {
        // Create appropriate layout parameters.
        LinearLayout.LayoutParams airportLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Assign those layout parameters to the layout.
        airportLayout.setLayoutParams(airportLayoutParams);

        // Set the orientation of the layout to be horizontal.
        airportLayout.setOrientation(LinearLayout.HORIZONTAL);

        // Set the padding of the layout.
        airportLayout.setPadding(
                0,
                0,
                0,
                toPixels(10)
        );
    }


    /**
     * A method to configure the layout containing some date input and time input.
     *
     * @param dateTimeLayout    the required RelativeLayout to configure
     */
    private void configureDateTimeLayout(RelativeLayout dateTimeLayout)
    {
        // Create appropriate layout parameters.
        RelativeLayout.LayoutParams depDateTimeLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        // Assign those layout parameters to the layout.
        dateTimeLayout.setLayoutParams(depDateTimeLayoutParams);

        // Set the padding of the layout.
        dateTimeLayout.setPadding(
                0,
                0,
                0,
                toPixels(10)
        );
    }


    /**
     * A method to configure the layout of a heading TextView.
     *
     * @param heading   the required TextView to configure
     */
    private void configureHeadingStyle(TextView heading)
    {
        // Create appropriate layout parameters.
        LinearLayout.LayoutParams headingLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        // Assign those layout parameters to the input View.
        heading.setLayoutParams(headingLayoutParams);

        // Set the padding of the View.
        heading.setPadding(
                0,
                0,
                0,
                toPixels(5)
        );

        // Set the text color.
        heading.setTextColor(getResources().getColor(R.color.black1));

        // Set the text size.
        heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

        // Center the text.
        heading.setGravity(Gravity.CENTER);
    }


    /**
     * A method to configure the layout of a CardView containing
     * all Views relating to a flight.
     *
     * @param contentCard   the required CardView to configure
     */
    private void configureCardStyle(CardView contentCard)
    {
        // Create appropriate layout parameters.
        CardView.LayoutParams contentCardLayoutParams = new CardView.LayoutParams(
                CardView.LayoutParams.MATCH_PARENT,
                CardView.LayoutParams.WRAP_CONTENT
        );

        // Set the margins of those layout parameters.
        contentCardLayoutParams.setMargins(
                0,
                0,
                0,
                toPixels(5)
        );

        // Assign those layout parameters to the CardView.
        contentCard.setLayoutParams(contentCardLayoutParams);

        // Make the CardView use additional padding to create a shadow effect.
        contentCard.setUseCompatPadding(true);

        // Set the radius of the CardView corners.
        contentCard.setRadius(toPixels(5));

        // Make sure its content doesn't overlap with the corners.
        contentCard.setPreventCornerOverlap(true);

        // Set the padding of the CardView's content.
        contentCard.setContentPadding(
                toPixels(10),
                toPixels(10),
                toPixels(10),
                toPixels(10)
        );

        // Set the background color of the CardView.
        contentCard.setCardBackgroundColor(getResources().getColor(R.color.grey3));
    }


    /**
     * A method which adds a custom TextWatcher onto every input field supplied.
     *
     * @param flightIndex       the index at which the required flight
     *                          can be found in the flights List
     * @param depAirportInput   the required departure airport AutoCompleteTextView
     * @param arrAirportInput   the required arrival airport AutoCompleteTextView
     * @param depDateInput      the required departure date EditText
     * @param arrDateInput      the required arrival date EditText
     * @param depTimeInput      the required departure time EditText
     * @param arrTimeInput      the required arrival time EditText
     */
    private void addInputTextWatchers(final int flightIndex,
                                      final AutoCompleteTextView depAirportInput,
                                      final AutoCompleteTextView arrAirportInput,
                                      final EditText depDateInput,
                                      final EditText arrDateInput,
                                      final EditText depTimeInput,
                                      final EditText arrTimeInput)
    {
        // Create a TextWatcher with method implementations
        // appropriate for the departure airport input.
        // The onTextChanged(...) method is called when changes
        // to the text have been made. The afterTextChanged(...)
        // method is called when the text is editable.
        TextWatcher depAirportTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i,
                                          int i1,
                                          int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence,
                                      int i,
                                      int i1,
                                      int i2)
            {
                // Run the following when prepared.
                depAirportInput.post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        // If the text in the input is more than one line,
                        // an icon of greater height will be needed.
                        if (depAirportInput.getLineCount() > 1)
                        {
                            // Check if the number of lines is exactly 2.
                            if (depAirportInput.getLineCount() == 2)
                            {
                                // If the input text matches an AirportListing in the airportFinder Map,
                                // then set the input icon to accommodate 2 lines and be green to indicate
                                // that the inputted text is valid.
                                if (airportFinder.containsKey(depAirportInput.getText().toString()))
                                {
                                    depAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                            R.drawable.ic_dep_airport_input_2_check,
                                            0,
                                            0,
                                            0
                                    );
                                }
                                // Otherwise, set the input icon to accommodate 2 lines and be white
                                // to indicate that the inputted text is not valid.
                                else
                                {
                                    depAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                            R.drawable.ic_dep_airport_input_2,
                                            0,
                                            0,
                                            0
                                    );
                                }
                            }
                            // If the number of lines is >1 and != 2, we can assume it is 3 at this point.
                            else
                            {
                                // If the input text matches an AirportListing in the airportFinder Map,
                                // then set the input icon to accommodate 3 lines and be green to indicate
                                // that the inputted text is valid.
                                if (airportFinder.containsKey(depAirportInput.getText().toString()))
                                {
                                    depAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                            R.drawable.ic_dep_airport_input_3_check,
                                            0,
                                            0,
                                            0
                                    );
                                }
                                // Otherwise, set the input icon to accommodate 3 lines and be white
                                // to indicate that the inputted text is not valid.
                                else
                                {
                                    depAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                            R.drawable.ic_dep_airport_input_3,
                                            0,
                                            0,
                                            0
                                    );
                                }
                            }
                        }
                        // If the number of lines is not >1, it is either 0 or 1.
                        else
                        {
                            // If the input text matches an AirportListing in the airportFinder Map,
                            // then set the input icon to accommodate 1 line and be green to indicate
                            // that the inputted text is valid.
                            if (airportFinder.containsKey(depAirportInput.getText().toString()))
                            {
                                depAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                        R.drawable.ic_dep_airport_input_1_check,
                                        0,
                                        0,
                                        0
                                );
                            }
                            // Otherwise, set the input icon to accommodate 1 line and be white
                            // to indicate that the inputted text is not valid.
                            else
                            {
                                depAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                        R.drawable.ic_dep_airport_input_1,
                                        0,
                                        0,
                                        0
                                );
                            }
                        }
                    }
                });
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // If the input text matches an AirportListing in the airportFinder Map,
                // then request mainLayout to be focused and click the departure date input,
                // if not already filled with a date.
                if (airportFinder.containsKey(depAirportInput.getText().toString()))
                {
                    mainLayout.requestFocus();

                    if (!flights.get(flightIndex).hasDepDateInput())
                    {
                        if (shouldAutoSelectTempDepDate)
                        {
                            depDateInput.performClick();
                        }
                    }
                }

                // If all input fields have been filled, enable the calculate Button.
                if (isAllFilled())
                {
                    calculateBtn.setEnabled(true);
                }
                // Otherwise, disable the calculate Button.
                else
                {
                    calculateBtn.setEnabled(false);
                }
            }
        };

        // Create a TextWatcher with method implementations
        // appropriate for the arrival airport input.
        // The onTextChanged(...) method is called when changes
        // to the text have been made. The afterTextChanged(...)
        // method is called when the text is editable.
        TextWatcher arrAirportTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i,
                                          int i1,
                                          int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence,
                                      int i,
                                      int i1,
                                      int i2)
            {
                // If the text in the input is more than one line,
                // an icon of greater height will be needed.
                if (arrAirportInput.getLineCount() > 1)
                {
                    // Check if the number of lines is exactly 2.
                    if (arrAirportInput.getLineCount() == 2)
                    {
                        // If the input text matches an AirportListing in the airportFinder Map,
                        // then set the input icon to accommodate 2 lines and be green to indicate
                        // that the inputted text is valid.
                        if (airportFinder.containsKey(arrAirportInput.getText().toString()))
                        {
                            arrAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_arr_airport_input_2_check,
                                    0,
                                    0,
                                    0
                            );
                        }
                        // Otherwise, set the input icon to accommodate 2 lines and be white
                        // to indicate that the inputted text is not valid.
                        else
                        {
                            arrAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_arr_airport_input_2,
                                    0,
                                    0,
                                    0
                            );
                        }
                    }
                    // If the number of lines is >1 and != 2, we can assume it is 3 at this point.
                    else
                    {
                        // If the input text matches an AirportListing in the airportFinder Map,
                        // then set the input icon to accommodate 3 lines and be green to indicate
                        // that the inputted text is valid.
                        if (airportFinder.containsKey(arrAirportInput.getText().toString()))
                        {
                            arrAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_arr_airport_input_3_check,
                                    0,
                                    0,
                                    0
                            );
                        }
                        // Otherwise, set the input icon to accommodate 3 lines and be white
                        // to indicate that the inputted text is not valid.
                        else
                        {
                            arrAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                    R.drawable.ic_arr_airport_input_3,
                                    0,
                                    0,
                                    0
                            );
                        }
                    }
                }
                // If the number of lines is not >1, it is either 0 or 1.
                else
                {
                    // If the input text matches an AirportListing in the airportFinder Map,
                    // then set the input icon to accommodate 1 line and be green to indicate
                    // that the inputted text is valid.
                    if (airportFinder.containsKey(arrAirportInput.getText().toString()))
                    {
                        arrAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_arr_airport_input_1_check,
                                0,
                                0,
                                0
                        );
                    }
                    // Otherwise, set the input icon to accommodate 1 line and be white
                    // to indicate that the inputted text is not valid.
                    else
                    {
                        arrAirportInput.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.ic_arr_airport_input_1,
                                0,
                                0,
                                0
                        );
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // Check if the input text matches an AirportListing in the airportFinder Map.
                if (airportFinder.containsKey(arrAirportInput.getText().toString()))
                {
                    // If there exists a succeeding flight and if its associated departure
                    // airport input is not already filled, then set that departure
                    // airport input to match the text of this arrival airport input.
                    if (flightIndex + 1 < flights.size())
                    {
                        if (!flights.get(flightIndex + 1).hasDepAirportInput())
                        {
                            flights.get(flightIndex + 1).getDepAirportInput().setText(
                                    arrAirportInput.getText().toString()
                            );
                        }
                    }

                    // Request mainLayout to be focus.
                    mainLayout.requestFocus();

                    // If the arrival date input is not already filled with a date,
                    // then click it.
                    if (!flights.get(flightIndex).hasArrDateInput())
                    {
                        arrDateInput.performClick();
                    }
                }

                // If all input fields have been filled, enable the calculate Button.
                if (isAllFilled())
                {
                    calculateBtn.setEnabled(true);
                }
                // Otherwise, disable the calculate Button.
                else
                {
                    calculateBtn.setEnabled(false);
                }
            }
        };

        // Create a TextWatcher with method implementations
        // appropriate for the departure date input.
        // The onTextChanged(...) method is called when changes
        // to the text have been made. The afterTextChanged(...)
        // method is called when the text is editable.
        TextWatcher depDateTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i,
                                          int i1,
                                          int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence,
                                      int i,
                                      int i1,
                                      int i2)
            {
                // Check if the departure date input is filled with a date.
                if (flights.get(flightIndex).hasDepDateInput())
                {
                    // Center the text.
                    depDateInput.setGravity(Gravity.CENTER);

                    // Remove the padding of the input icon.
                    depDateInput.setCompoundDrawablePadding(0);

                    // Set the input icon to be green to indicate validity.
                    depDateInput.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_date_input_check,
                            0,
                            0,
                            0
                    );
                }
                // Otherwise, the input field is empty.
                else
                {
                    // Align the text to the left.
                    depDateInput.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

                    // Add padding to the input icon, as text is aligned to the left.
                    depDateInput.setCompoundDrawablePadding(toPixels(10));

                    // Set the input icon to be white to indicate invalidity.
                    depDateInput.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_date_input,
                            0,
                            0,
                            0
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // If the departure date input is filled with a date and the
                // departure time input is not filled then click it.
                if (flights.get(flightIndex).hasDepDateInput() &&
                        !flights.get(flightIndex).hasDepTimeInput())
                {
                    depTimeInput.performClick();
                }

                // If all input fields have been filled, enable the calculate Button.
                if (isAllFilled())
                {
                    calculateBtn.setEnabled(true);
                }
                // Otherwise, disable the calculate Button.
                else
                {
                    calculateBtn.setEnabled(false);
                }
            }
        };

        // Create a TextWatcher with method implementations
        // appropriate for the arrival date input.
        // The onTextChanged(...) method is called when changes
        // to the text have been made. The afterTextChanged(...)
        // method is called when the text is editable.
        TextWatcher arrDateTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i,
                                          int i1,
                                          int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence,
                                      int i,
                                      int i1,
                                      int i2)
            {
                // Check if the arrival date input is filled with a date.
                if (flights.get(flightIndex).hasArrDateInput())
                {
                    // Center the text.
                    arrDateInput.setGravity(Gravity.CENTER);

                    // Remove the padding of the input icon.
                    arrDateInput.setCompoundDrawablePadding(0);

                    // Set the input icon to be green to indicate validity.
                    arrDateInput.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_date_input_check,
                            0,
                            0,
                            0
                    );
                }
                // Otherwise, the input field is empty.
                else
                {
                    // Align the text to the left.
                    arrDateInput.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);

                    // Add padding to the input icon, as text is aligned to the left.
                    arrDateInput.setCompoundDrawablePadding(toPixels(10));

                    // Set the input icon to be white to indicate invalidity.
                    arrDateInput.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_date_input,
                            0,
                            0,
                            0
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // If the arrival date input is filled with a date and the
                // arrival time input is not filled then click it.
                if (flights.get(flightIndex).hasArrDateInput() &&
                        !flights.get(flightIndex).hasArrTimeInput())
                {
                    arrTimeInput.performClick();
                }

                // If all input fields have been filled, enable the calculate Button.
                if (isAllFilled())
                {
                    calculateBtn.setEnabled(true);
                }
                // Otherwise, disable the calculate Button.
                else
                {
                    calculateBtn.setEnabled(false);
                }
            }
        };

        // Create a TextWatcher with method implementations
        // appropriate for the departure time input.
        // The onTextChanged(...) method is called when changes
        // to the text have been made. The afterTextChanged(...)
        // method is called when the text is editable.
        TextWatcher depTimeTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i,
                                          int i1,
                                          int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence,
                                      int i,
                                      int i1,
                                      int i2)
            {
                // Check if the departure time input is filled with a time.
                if (flights.get(flightIndex).hasDepTimeInput())
                {
                    // Set the input icon to be green to indicate validity.
                    depTimeInput.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_time_input_check,
                            0,
                            0,
                            0
                    );
                }
                // Otherwise, the input field is empty.
                else
                {
                    // Set the input icon to be white to indicate invalidity.
                    depTimeInput.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_time_input,
                            0,
                            0,
                            0
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // If the departure time input is filled with a time and the
                // arrival airport input is not filled then click it.
                if (flights.get(flightIndex).hasDepTimeInput() &&
                        !flights.get(flightIndex).hasArrAirportInput())
                {
                    arrAirportInput.requestFocus();
                }

                // If all input fields have been filled, enable the calculate Button.
                if (isAllFilled())
                {
                    calculateBtn.setEnabled(true);
                }
                // Otherwise, disable the calculate Button.
                else
                {
                    calculateBtn.setEnabled(false);
                }
            }
        };

        // Create a TextWatcher with method implementations
        // appropriate for the arrival time input.
        // The onTextChanged(...) method is called when changes
        // to the text have been made. The afterTextChanged(...)
        // method is called when the text is editable.
        TextWatcher arrTimeTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence,
                                          int i,
                                          int i1,
                                          int i2)
            {
            }

            @Override
            public void onTextChanged(CharSequence charSequence,
                                      int i,
                                      int i1,
                                      int i2)
            {
                // Check if the arrival time input is filled with a time.
                if (flights.get(flightIndex).hasArrTimeInput())
                {
                    // Set the input icon to be green to indicate validity.
                    arrTimeInput.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_time_input_check,
                            0,
                            0,
                            0
                    );
                }
                // Otherwise, the input field is empty.
                else
                {
                    // Set the input icon to be white to indicate invalidity.
                    arrTimeInput.setCompoundDrawablesWithIntrinsicBounds(
                            R.drawable.ic_time_input,
                            0,
                            0,
                            0
                    );
                }
            }

            @Override
            public void afterTextChanged(Editable editable)
            {
                // Check if there exists a succeeding flight.
                if (flightIndex + 1 < flights.size())
                {
                    // If the arrival time input is filled with a time and the
                    // departure airport input succeeding flight associated with
                    // the succeeding flight is not yet filled, request to focus it.
                    if (flights.get(flightIndex).hasArrTimeInput() &&
                            !flights.get(flightIndex + 1).hasDepAirportInput())
                    {
                        flights.get(flightIndex + 1).getDepAirportInput().requestFocus();
                    }
                }

                // If all input fields have been filled, enable the calculate Button.
                if (isAllFilled())
                {
                    calculateBtn.setEnabled(true);
                }
                // Otherwise, disable the calculate Button.
                else
                {
                    calculateBtn.setEnabled(false);
                }
            }
        };

        // Add all created TextWatchers to their intended inputs.
        depAirportInput.addTextChangedListener(depAirportTextWatcher);
        arrAirportInput.addTextChangedListener(arrAirportTextWatcher);
        depDateInput.addTextChangedListener(depDateTextWatcher);
        arrDateInput.addTextChangedListener(arrDateTextWatcher);
        depTimeInput.addTextChangedListener(depTimeTextWatcher);
        arrTimeInput.addTextChangedListener(arrTimeTextWatcher);
    }


    /**
     * A method which adds a set of inputs in the UI for another flight and
     * adds those inputs as a FlightManager to the flights List.
     *
     * @param adapter   the adapter which will provide the airport inputs
     *                  with custom auto-complete behaviour
     */
    private void addFlight(AirportListAdapter adapter)
    {
        // An index to allow the added flight to be accessed from the flights List.
        final int flightIndex = flights.size();

        // A LinearLayout to contain the departure airport input.
        LinearLayout depAirportLayout = new LinearLayout(this);
        // Configure the layout of the LinearLayout.
        configureAirportLayout(depAirportLayout);

        // A RelativeLayout to contain the departure date input and departure time input.
        RelativeLayout depDateTimeLayout = new RelativeLayout(this);
        // Configure the layout of the RelativeLayout.
        configureDateTimeLayout(depDateTimeLayout);

        // A LinearLayout to contain the arrival airport input.
        LinearLayout arrAirportLayout = new LinearLayout(this);
        // Configure the layout of the LinearLayout.
        configureAirportLayout(arrAirportLayout);

        // A RelativeLayout to contain the arrival date input and arrival time input.
        RelativeLayout arrDateTimeLayout = new RelativeLayout(this);
        // Configure the layout of the LinearLayout.
        configureDateTimeLayout(arrDateTimeLayout);

        // The heading for all departure inputs.
        TextView depHeading = new TextView(this);
        // Configure the layout of the heading.
        configureHeadingStyle(depHeading);
        // Set the text of the heading.
        depHeading.setText(R.string.departure);

        // The heading for all arrival inputs.
        TextView arrHeading = new TextView(this);
        // Configure the layout of the heading.
        configureHeadingStyle(arrHeading);
        // Set the text of the heading.
        arrHeading.setText(R.string.arrival);

        // The departure airport input.
        AutoCompleteTextView depAirportInput = new AutoCompleteTextView(this);
        // Configure the logic of the departure airport input.
        configureAirportInputLogic(depAirportInput, adapter);
        // Configure the layout of the departure airport input.
        configureAirportInputStyle(depAirportInput, true);

        // The arrival airport input.
        AutoCompleteTextView arrAirportInput = new AutoCompleteTextView(this);
        // Configure the logic of the arrival airport input.
        configureAirportInputLogic(arrAirportInput, adapter);
        // Configure the layout of the arrival airport input.
        configureAirportInputStyle(arrAirportInput, false);

        // The departure date input.
        EditText depDateInput = new EditText(this);
        // Configure the logic of the departure date input.
        configureDateInputLogic(depDateInput, true);
        // Configure the layout of the departure date input.
        configureDateInputStyle(depDateInput);

        // The arrival date input.
        EditText arrDateInput = new EditText(this);
        // Configure the logic of the arrival date input.
        configureDateInputLogic(arrDateInput, false);
        // Configure the layout of the arrival date input.
        configureDateInputStyle(arrDateInput);

        // The departure time input.
        EditText depTimeInput = new EditText(this);
        // Configure the logic of the departure time input.
        configureTimeInputLogic(depTimeInput);
        // Configure the layout of the departure time input.
        configureTimeInputStyle(depTimeInput);

        // The arrival time input.
        EditText arrTimeInput = new EditText(this);
        // Configure the logic of the arrival time input.
        configureTimeInputLogic(arrTimeInput);
        // Configure the layout of the arrival time input.
        configureTimeInputStyle(arrTimeInput);

        // Add all the configured inputs as a FlightManager into the flights List.
        flights.add(new FlightManager(
                depAirportInput,
                arrAirportInput,
                depDateInput,
                arrDateInput,
                depTimeInput,
                arrTimeInput
        ));

        // Reset shouldAutoSelectTempDepDate to true.
        shouldAutoSelectTempDepDate = true;

        // Add customised TextWatchers too all inputs,
        // to react appropriately to inputted text changes.
        addInputTextWatchers(
                flightIndex,
                depAirportInput,
                arrAirportInput,
                depDateInput,
                arrDateInput,
                depTimeInput,
                arrTimeInput
        );

        // Add the departure airport input to the departure airport layout.
        depAirportLayout.addView(depAirportInput);
        // Add the departure date input to the departure date time layout.
        depDateTimeLayout.addView(depDateInput);
        // Add the departure time input to the departure date time layout.
        depDateTimeLayout.addView(depTimeInput);
        // Add the arrival airport input to the arrival airport layout.
        arrAirportLayout.addView(arrAirportInput);
        // Add the arrival date input to the arrival date time layout.
        arrDateTimeLayout.addView(arrDateInput);
        // Add the arrival time input to the arrival date time layout.
        arrDateTimeLayout.addView(arrTimeInput);

        // Create a LinearLayout for all content associated with this flight.
        LinearLayout flightContent = new LinearLayout(this);
        // Create appropriate layout parameters.
        LinearLayout.LayoutParams flightContentLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        // Assign those layout parameters to the LinearLayout.
        flightContent.setLayoutParams(flightContentLayoutParams);
        // Set the orientation of the layout to be vertical.
        flightContent.setOrientation(LinearLayout.VERTICAL);

        // Create a CardView to display the flight content LinearLayout
        // in an appealing card form.
        CardView contentCard = new CardView(this);
        // Configure the layout of the CardView.
        configureCardStyle(contentCard);

        // The index at which the CardView can be appended onto mainLayout.
        int appendPosition;

        // If there are more than 2 elements already in mainLayout,
        // then flight content has already been added to the UI.
        if (mainLayout.getChildCount() > 2)
        {
            // Make the append position the size of the flights List - 1.
            appendPosition = (flights.size() - 1);

            // Since more than one flight has been added to the UI, enable
            // the delete flight Button.
            deleteFlightBtn.setEnabled(true);

            // Get the inputted text from the previous arrival airport input.
            String prevArrAirportString
                    = flights.get(flights.size() - 2).getArrAirportInput().getText().toString();

            // If that inputted text is not the empty String, set that as the
            // text of the departure airport input of this flight. Also make
            // sure that the departure date input will not be automatically selected.
            if (!prevArrAirportString.equals(""))
            {
                shouldAutoSelectTempDepDate = false;
                depAirportInput.setText(prevArrAirportString);
            }
        }
        // Otherwise, mainLayout can be assumed to have no flight content, so
        // make the append position 0 to place the first collection of
        // flight content at the top of mainLayout.
        else
        {
            appendPosition = 0;
        }

        // Add the arrival date time layout to the top of flightContent.
        flightContent.addView(arrDateTimeLayout, 0);
        // Add the arrival airport layout to the top of flightContent.
        flightContent.addView(arrAirportLayout, 0);
        // Add the arrival heading to the top of flightContent.
        flightContent.addView(arrHeading, 0);
        // Add the departure date time layout to the top of flightContent.
        flightContent.addView(depDateTimeLayout, 0);
        // Add the departure airport layout to the top of flightContent.
        flightContent.addView(depAirportLayout, 0);
        // Add the departure heading to the top of flightContent.
        flightContent.addView(depHeading, 0);

        // Add the flight content LinearLayout to the CardView.
        contentCard.addView(flightContent);

        // Add the CardView to mainLayout at the appropriate index.
        mainLayout.addView(contentCard, appendPosition);

        // If all input fields have been filled, enable the calculate Button.
        if (isAllFilled())
        {
            calculateBtn.setEnabled(true);
        }
        // Otherwise, disable the calculate Button.
        else
        {
            calculateBtn.setEnabled(false);
        }

        // Scroll to the bottom of the app after a short delay,
        // to allow time for UI to be added.
        scrollView.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        },100);
    }


    /**
     * A method which removes the set of inputs associated with the last flight.
     * In other words, the bottom-most flight in the UI will be removed, as well as
     * the last instance of FlightManager in the flights List.
     */
    private void deleteFlight()
    {
        // Removes the bottom-most flight from mainLayout.
        mainLayout.removeViewAt(flights.size() - 1);

        // Removes the flight from the flights List.
        flights.remove(flights.size() - 1);

        // If there now remains less than 2 flights in the UI, disable
        // the delete flight Button.
        if (flights.size() < 2)
        {
            deleteFlightBtn.setEnabled(false);
        }


        // If all input fields have been filled, enable the calculate Button.
        if (isAllFilled())
        {
            calculateBtn.setEnabled(true);
        }
        // Otherwise, disable the calculate Button.
        else
        {
            calculateBtn.setEnabled(false);
        }
    }


    /**
     * A method which removes all additional flights from the UI,
     * thereby resetting the UI to its initial state.
     */
    private void clearAllInputs()
    {
        // While there are more than flight one flight, delete a flight.
        while (flights.size() > 1)
        {
            deleteFlight();
        }

        // Set the text of all inputs in the remaining flight to the empty String.
        flights.get(0).getDepAirportInput().setText("");
        flights.get(0).getArrAirportInput().setText("");
        flights.get(0).getDepDateInput().setText("");
        flights.get(0).getArrDateInput().setText("");
        flights.get(0).getDepTimeInput().setText("");
        flights.get(0).getArrTimeInput().setText("");

        // Notify the user with a Toast that all selections have been cleared.
        Toast.makeText(this, "Selections cleared", Toast.LENGTH_SHORT).show();

        // Request focus to mainLayout.
        mainLayout.requestFocus();
    }




    /**
     * A method which forces the keyboard the be shown.
     */
    private void showSoftKeyboard()
    {
        InputMethodManager inputMethodManager
                = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null)
        {
            inputMethodManager.showSoftInput(
                    getCurrentFocus(), InputMethodManager.SHOW_FORCED
            );
        }
    }


    /**
     * A method which forces the keyboard the be hidden.
     */
    private void hideSoftKeyboard()
    {
        InputMethodManager inputMethodManager
                = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null &&
                getCurrentFocus() != null)
        {
            inputMethodManager.hideSoftInputFromWindow(
                    getCurrentFocus().getWindowToken(),
                    0
            );
        }
    }


    /**
     * A method which parses a date String of custom format into a LocalDate object.
     *
     * @param dateString    the String describing the required date
     * @return              the LocalDate representing the same date
     */
    private LocalDate parseDate(String dateString)
    {
        // Create a custom formatter.
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d-MMM-yyyy");

        // Use the custom formatter to parse the given String into a LocalDate.
        return LocalDate.parse(dateString, formatter);
    }


    /**
     * @return  whether or not every FlightManager in the flights List
     *          has all their UI input Views filled.
     */
    private boolean isAllFilled()
    {
        // For each FlightManager in the flights List,
        // check if its filled, returning false if it's not.
        for (FlightManager flight : flights)
        {
            if (!flight.isFilled())
            {
                return false;
            }
        }

        // If for loop hasn't returned false, then return true.
        return true;
    }


    /**
     * A method which converts the given density-independent pixels into actual pixels.
     *
     * @param dp    the required int representing the dp to be converted
     * @return      the equivalent value in the unit of pixels
     */
    private int toPixels(int dp)
    {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }


    /**
     * A method which populates the airports List with an AirportListing for
     * every airport in the airports data file. The data file is already sorted
     * alphabetically, so using the sort method would be redundant.
     */
    private void fillAirportList()
    {
        // Initialising the airports List.
        airports = new ArrayList<>();

        // An InputStream to hold the input file.
        InputStream input = null;

        try
        {
            // Get the data file.
            input = getAssets().open("airports-data.txt");

            // A BufferedReader to read the data.
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(input));

            // A String to hold the current line read from the data.
            String currentLine;

            // Read every line of the data, one line at a time. Each line will
            // contain the details of one airport.
            while ((currentLine = inputReader.readLine()) != null)
            {
                // Split the line with the delimiter being a comma not followed by a space.
                // The separated Strings are placed in String array.
                String[] details = currentLine.split(",(?! )");

                // Compose an AirportListing out of the airport detail Strings.
                AirportListing currentAirport = new AirportListing(
                        details[0],
                        details[1],
                        details[2],
                        details[3],
                        Double.parseDouble(details[4]),
                        Double.parseDouble(details[5])
                );

                // Add the AirportListing to the airports List.
                airports.add(currentAirport);

                // Map the AirportListing's toString() result to the AirportListing
                // itself in the airportFinder Map.
                airportFinder.put(currentAirport.toString(), currentAirport);
            }
        }
        // Handle any IOException.
        catch (IOException exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            // Close the input.
            try
            {
                if (input != null)
                {
                    input.close();
                }
            }
            // Handle any IOException.
            catch (IOException exception)
            {
                exception.printStackTrace();
            }
        }
    }


    /**
     * A class to represent a custom AsyncTask meant to calculate the total flight time,
     * utilising a worker thread for most of the calculation logic (including multiple HTTP GET
     * requests) while updating the UI before, concurrently and afterwards in the UI thread.
     */
    private static class CalculationTask extends AsyncTask<Void, Integer, String>
    {
        // A weak reference to MainActivity.
        final private WeakReference<MainActivity> activityReference;

        // The number of times, per flight, the progress bar will be updated before completion.
        final int progressSteps = 10;


        /**
         * A constructor.
         *
         * @param context   the context, in this case MainActivity.
         */
        CalculationTask(MainActivity context)
        {
            // Initialise the weak reference to MainActivity.
            activityReference = new WeakReference<>(context);
        }


        /**
         * A method which is invoked in the UI thread before
         * the worker thread starts the calculation.
         */
        @SuppressLint("InflateParams")
        @Override
        protected void onPreExecute()
        {
            // The activity from which to make UI changes,
            // i.e. this instance of MainActivity.
            final MainActivity activity = activityReference.get();

            // If the activity is null or is in the process of finishing,
            // then this CalculationTask should not be running, so
            // simply return in that case.
            if (activity == null || activity.isFinishing())
            {
                return;
            }

            // Create a builder for the AlertDialog with a custom theme.
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                    activity,
                    R.style.DialogTheme
            );

            // Inflate the calculation popup layout and assign it to calculationView.
            activity.calculationView = activity.getLayoutInflater().inflate(
                    R.layout.calculation_popup,
                    null
            );

            // Getting the progress bar by id.
            activity.progressBar = activity.calculationView.findViewById(R.id.progressBar);

            // Set the maximum number of iterations of the progress bar.
            activity.progressBar.setMax(activity.flights.size() * progressSteps);

            // Set the progress bar progress to 0 to reflect its initial state.
            activity.progressBar.setProgress(0);

            // For each FlightManager in the flights List, make the
            // flight details available from a worker thread.
            for (FlightManager flight : activity.flights)
            {
                flight.updateFlightDetails();
            }

            // Set calculationView as the View for the dialog builder.
            dialogBuilder.setView(activity.calculationView);

            // Store the associated AlertDialog in calculationDialog.
            activity.calculationDialog = dialogBuilder.create();

            // The window of the AlertDialog.
            final Window window = activity.calculationDialog.getWindow();

            // Check if the window is not null.
            if (window != null)
            {
                // Set the layout of the window.
                window.setLayout(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

                // Dim the all content behind the AlertDialog.
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                // Set the dim amount.
                window.setDimAmount(0.6f);

                // Make the background of the window transparent.
                window.setBackgroundDrawableResource(android.R.color.transparent);
            }

            // Getting the AlertDialog close Button by id.
            Button closeDialogBtn = activity.calculationView.findViewById(R.id.closeDialogBtn);

            // Set an onClickListener to the close Button which closes the AlertDialog.
            closeDialogBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.calculationDialog.dismiss();
                }
            });

            // Show the AlertDialog.
            activity.calculationDialog.show();
        }


        /**
         * A method which is invoked in a worker thread immediately after onPreExecute()
         * has finished executing. It computes the total flight time by a series of calculations
         * and HTTP GET requests.
         *
         * @param voids means no parameter
         * @return      a String with the calculation results
         */
        @Override
        protected String doInBackground(Void... voids)
        {
            // The activity from which to access its instance variables,
            // i.e. this instance of MainActivity.
            final MainActivity activity = activityReference.get();

            // If the activity is null or is in the process of finishing,
            // then this CalculationTask should not be running, so
            // simply return any String in that case.
            if (activity == null || activity.isFinishing())
            {
                return "error";
            }

            // The total flight time in hours, initially 0.
            double totalFlightTime = 0;
            // The total layover time in hours, initially 0.
            double totalLayoverTime = 0;

            // A temporary LocalDate variable to hold the arrival date and time.
            // It will be used to calculate the layover time between two flights.
            LocalDateTime tempArrDateTime = null;

            try
            {
                // A count of the current flight being processed, initially 0.
                int flightCount = 0;

                // For each FlightManager in the flights List.
                for (FlightManager flight : activity.flights)
                {
                    // Increment the flight count by 1.
                    flightCount++;

                    // Fetch the flight details of this flight as a String array.
                    String[] flightDetails = flight.getFlightDetails();

                    // Store the flight details as String variables.
                    String depAirportString = flightDetails[0];
                    String arrAirportString = flightDetails[1];
                    String depDateString = flightDetails[2];
                    String arrDateString = flightDetails[3];
                    String depTimeString = flightDetails[4];
                    String arrTimeString = flightDetails[5];

                    // Use the airportFinder Map to get the AirportListing
                    // corresponding to the departure airport String.
                    AirportListing depAirport = activity.airportFinder.get(depAirportString);
                    // Use the airportFinder Map to get the AirportListing
                    // corresponding to the arrival airport String.
                    AirportListing arrAirport = activity.airportFinder.get(arrAirportString);

                    // Convert the departure date String into a LocalDate object.
                    LocalDate depDate = activity.parseDate(depDateString);
                    // Convert the arrival date String into a LocalDate object.
                    LocalDate arrDate = activity.parseDate(arrDateString);

                    // Convert the departure time String into a LocalTime object.
                    LocalTime depTime = LocalTime.parse(depTimeString);
                    // Convert the arrival time String into a LocalTime object.
                    LocalTime arrTime = LocalTime.parse(arrTimeString);

                    // Create a LocalDateTime object out of the departure LocalDate and LocalTime.
                    LocalDateTime depDateTime = LocalDateTime.of(depDate, depTime);
                    // Create a LocalDateTime object out of the arrival LocalDate and LocalTime.
                    LocalDateTime arrDateTime = LocalDateTime.of(arrDate, arrTime);

                    // The departure time represented as the decimal number of hours since midnight.
                    double depTimeValue = depTime.getHour() + ((double) depTime.getMinute() / 60);
                    // The arrival time represented as the decimal number of hours since midnight.
                    double arrTimeValue = arrTime.getHour() + ((double) arrTime.getMinute() / 60);

                    // The departure LocalDateTime in epoch seconds to be used as a timestamp.
                    long depTimestamp = depDateTime.toEpochSecond(ZoneOffset.UTC);
                    // The arrival LocalDateTime in epoch seconds to be used as a timestamp.
                    long arrTimestamp = arrDateTime.toEpochSecond(ZoneOffset.UTC);

                    // Check if the departure airport and arrival airport are not null.
                    if (depAirport != null && arrAirport != null)
                    {
                        // Using the departure timestamp, calls an AirportListing method
                        // which makes a request to the Google Time Zone API and sets
                        // the returned timezone offset as the departure airport's timezone.
                        depAirport.requestTimezone(
                                depTimestamp,
                                activity.getString(R.string.GOOGLE_API_KEY),
                                activity
                        );

                        // Using the arrival timestamp, calls an AirportListing method
                        // which makes a request to the Google Time Zone API and sets
                        // the returned timezone offset as the arrival airport's timezone.
                        arrAirport.requestTimezone(
                                arrTimestamp,
                                activity.getString(R.string.GOOGLE_API_KEY),
                                activity
                        );

                        // A count of the number of ms delayed
                        // by the following while loop.
                        int sleepCounter = 0;

                        // While the timezone of either the departure or arrival airport is 78.9
                        // (indicating an unassigned timezone), wait 2 ms, increment the counter,
                        // and throw an exception if loop has waited 5 s.
                        while (depAirport.getTimezone() == 78.9 ||
                                arrAirport.getTimezone() == 78.9)
                        {
                            Thread.sleep(2);
                            sleepCounter += 2;

                            if (sleepCounter > 5000) {
                                throw new InterruptedException("Awaiting timezone for too long.");
                            }
                        }

                        // If the timezone of either the departure or arrival airport is 99.9
                        // (indicating a failed HTTP request), abort the calculation by
                        // returning "error".
                        if (depAirport.getTimezone() == 99.9 ||
                                arrAirport.getTimezone() == 99.9)
                        {
                            return "error";
                        }

                        // Subtract the departure timezone offset from the decimal number
                        // of hours since midnight until departure. This will yield the
                        // value as if it was in GMT +0.0.
                        depTimeValue -= depAirport.getTimezone();
                        // Subtract the arrival timezone offset from the decimal number
                        // of hours since midnight until arrival. This will yield the
                        // value as if it was in GMT +0.0.
                        arrTimeValue -= arrAirport.getTimezone();
                    }

                    // Get the number of days passed between the departure and arrival date.
                    double daysPassed = (double) ChronoUnit.DAYS.between(depDate, arrDate);

                    // Subtract the number of hours since midnight until departure from
                    // the number of hours since midnight until arrival. Adding 24 for each
                    // day passed will produce the duration of the flight. This will
                    // be added onto the total flight time variable.
                    totalFlightTime += arrTimeValue - depTimeValue + 24 * daysPassed;

                    // If the tempArrDateTime variable has already been assigned a LocalDateTime,
                    // get the number of minutes between the arrival date of the previous flight
                    // and the departure date of the current flight, convert it to the decimal
                    // number of hours and add it to the total layover time variable.
                    if (tempArrDateTime != null)
                    {
                        double currentLayoverMinutes = (double) (ChronoUnit.MINUTES.between(
                                tempArrDateTime,
                                depDateTime
                        ));

                        totalLayoverTime += (currentLayoverMinutes / 60);
                    }

                    // Assign the current arrival LocalDateTime to the tempArrDateTime, to
                    // allow it to be accessed when processing the next flight.
                    tempArrDateTime = arrDateTime;

                    // For each iteration of the progress steps, publish this progress
                    // to the UI thread for the progress bar to be updated. Wait 10 ms
                    // after each update, to increase the smoothness of the progress bar's
                    // visual animation.
                    for (int progressIndex = (flightCount - 1) * progressSteps;
                         progressIndex <= flightCount * progressSteps;
                         progressIndex++)
                    {
                        publishProgress(progressIndex);
                        Thread.sleep(10);
                    }
                }
            }
            // Handle any InterruptedException caused by Thread.sleep().
            catch (InterruptedException exception)
            {
                Thread.currentThread().interrupt();
                exception.printStackTrace();
            }

            // Return the total flight time (in hours) and the total layover time (in hours),
            // separated by a semi-colon.
            return totalFlightTime + ";" + totalLayoverTime;
        }


        /**
         * A method which is invoked on the UI thread whenever publishProgress(..)
         * is called in the worker thread.
         *
         * @param progress  the progress to be set in the progress bar
         */
        @Override
        protected void onProgressUpdate(Integer... progress)
        {
            // The activity from which to make UI changes,
            // i.e. this instance of MainActivity.
            final MainActivity activity = activityReference.get();

            // If the activity is null or is in the process of finishing,
            // then this CalculationTask should not be running, so
            // simply return in that case.
            if (activity == null || activity.isFinishing())
            {
                return;
            }

            // If the user's android version is at least Nougat,
            // set the progress of the progress bar with an animation.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                activity.progressBar.setProgress(progress[0], true);
            }
            // Otherwise, set the progress of the progress bar without an animation.
            else
            {
                activity.progressBar.setProgress(progress[0]);
            }
        }


        /**
         * A method which is invoked in the UI thread upon the completion
         * of the background calculation task, with its result being passed
         * as a String parameter.
         *
         * @param result    a String consisting of the total flight hours and
         *                  the total layover hours, separated by a semi-colon
         */
        @Override
        protected void onPostExecute(String result)
        {
            // The activity from which to make UI changes,
            // i.e. this instance of MainActivity.
            final MainActivity activity = activityReference.get();

            // If the activity is null or is in the process of finishing,
            // then this CalculationTask should not be running, so
            // simply return in that case.
            if (activity == null || activity.isFinishing())
            {
                return;
            }

            // If the returned String is "error", close the AlertDialog and
            // use a Toast to notify the user of a network failure.
            if (result.equals("error"))
            {
                activity.calculationDialog.dismiss();

                Toast networkErrorToast = Toast.makeText(
                        activity,
                        "Network failure.",
                        Toast.LENGTH_SHORT
                );
                networkErrorToast.show();
            }
            // Otherwise, the result may be displayed.
            else
            {
                // The index at which the semi-colon is located within the result String.
                int splitIndex = result.indexOf(";");
                // Get the total flight hours String from the result.
                String flightTimeValueString = result.substring(0, splitIndex);
                // Get the total layover hours String from the result.
                String layoverTimeValueString = result.substring(splitIndex + 1);

                // The index at which the decimal point is located within
                // the total flight hours String.
                splitIndex = flightTimeValueString.indexOf(".");
                // The flight hours extracted from the total flight hours String.
                int flightHours = Integer.parseInt(flightTimeValueString.substring(0, splitIndex));
                // The flight minutes extracted from decimal remainder of
                // the total flight hours String before being converted to minutes.
                int flightMinutes = (int) Math.round(
                        Double.parseDouble(flightTimeValueString.substring(splitIndex)) * 60
                );

                // The index at which the decimal point is located within
                // the total layover hours String.
                splitIndex = layoverTimeValueString.indexOf(".");
                // The layover hours extracted from the total layover hours String.
                int layoverHours = Integer.parseInt(layoverTimeValueString.substring(0, splitIndex));
                // The flight minutes extracted from decimal remainder of
                // the total layover hours String before being converted to minutes.
                int layoverMinutes = (int) Math.round(
                        Double.parseDouble(layoverTimeValueString.substring(splitIndex)) * 60
                );

                // The String to display the total flight time.
                String flightTimeString = "" + flightHours + "h " + flightMinutes + "min";
                // The String to display the total layover time.
                String layoverTimeString = "" + layoverHours + "h " + layoverMinutes + "min";
                // The String to display the total trip time, i.e. flight time + layover time.
                String tripTimeString = "" + (flightHours + layoverHours) + "h " + (flightMinutes + layoverMinutes) + "min";

                // If the resulting flight time is less than 0, close the AlertDialog,
                // and use a Toast to notify the user of the negative result.
                if (Double.parseDouble(flightTimeValueString) < 0)
                {
                    activity.calculationDialog.dismiss();

                    Toast inputErrorToast = Toast.makeText(
                            activity,
                            "Negative result. Please check parameters.",
                            Toast.LENGTH_LONG
                    );
                    inputErrorToast.show();
                }

                // Getting the flight time text display by id.
                TextView flightTimeOutput = activity.calculationView.findViewById(R.id.flightTime);
                // Getting the layover time text display by id.
                TextView layoverTimeOutput = activity.calculationView.findViewById(R.id.layoverTime);
                // Getting the trip time text display by id.
                TextView tripTimeOutput = activity.calculationView.findViewById(R.id.tripTime);

                // Set the flight time String as the text of the flight time TextView.
                flightTimeOutput.setText(flightTimeString);
                // Set the layover time String as the text of the layover time TextView.
                layoverTimeOutput.setText(layoverTimeString);
                // Set the trip time String as the text of the trip time TextView.
                tripTimeOutput.setText(tripTimeString);

                // Make the progress bar invisible, as the calculation is completed.
                activity.progressBar.setVisibility(View.INVISIBLE);
            }
        }
    }
}