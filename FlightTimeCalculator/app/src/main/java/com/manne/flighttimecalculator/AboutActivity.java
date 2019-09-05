package com.manne.flighttimecalculator;

import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.cardview.widget.CardView;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * The about Activity, which will contain non-functional
 * information relating to the app in its UI.
 */
public class AboutActivity extends AppCompatActivity
{
    // The button to toggle the additional about navigation menu.
    private MenuItem hideShowBtn;

    // The additional about navigation bar.
    private LinearLayout aboutNavBar;

    // A boolean of whether or not the navigation bar is toggled on.
    private boolean hasNavBar = true;


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
        setContentView(R.layout.activity_about);

        // Get the app toolbar by id.
        Toolbar toolbar = findViewById(R.id.toolbar);
        // Set the title of the toolbar.
        toolbar.setTitle("About");
        // Set the text style of the toolbar, which currently only
        // sets the title's text size.
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitleTheme);
        // Set the text color of the title in the toolbar.
        toolbar.setTitleTextColor(getResources().getColor(R.color.black1));
        // Set this toolbar as the ActionBar.
        setSupportActionBar(toolbar);

        // Get the ActionBar.
        ActionBar actionBar = getSupportActionBar();

        // If the ActionBar exists, enable the home button,
        // which upon click opens the MainActivity.
        if (actionBar != null)
        {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Getting the about navigation bar by id.
        aboutNavBar = findViewById(R.id.aboutNavBar);

        // Getting by id the TextView to display the Terms of Use intro in bold.
        TextView termsIntroBold = findViewById(R.id.termsIntroBold);
        // Get the String containing links.
        String termsLinkText = getResources().getString(R.string.terms_intro_bold);
        // Set its HTML as text.
        termsIntroBold.setText(Html.fromHtml(termsLinkText));
        // Activate its links.
        termsIntroBold.setMovementMethod(LinkMovementMethod.getInstance());

        // Getting by id the TextView to display the Terms of Use contact info.
        TextView termsContact = findViewById(R.id.termsContact);
        // Get the String containing email links.
        String termsEmailText = getResources().getString(R.string.terms_contact);
        // Set its HTML as text.
        termsContact.setText(Html.fromHtml(termsEmailText));
        // Activate its links.
        termsContact.setMovementMethod(LinkMovementMethod.getInstance());

        // Getting by id the TextView to display the Privacy Policy intro in bold.
        TextView privacyIntroBold = findViewById(R.id.privacyIntroBold);
        // Get the String containing links.
        String privacyLinkText = getResources().getString(R.string.privacy_intro_bold);
        // Set its HTML as text.
        privacyIntroBold.setText(Html.fromHtml(privacyLinkText));
        // Activate its links.
        privacyIntroBold.setMovementMethod(LinkMovementMethod.getInstance());

        // Getting by id the TextView to display the Privacy Policy contact info.
        TextView privacyContact = findViewById(R.id.privacyContact);
        // Get the String containing email links.
        String privacyEmailText = getResources().getString(R.string.privacy_contact);
        // Set its HTML as text.
        privacyContact.setText(Html.fromHtml(privacyEmailText));
        // Activate its links.
        privacyContact.setMovementMethod(LinkMovementMethod.getInstance());

        // Getting by id the TextView to display source code info.
        TextView sourceCode = findViewById(R.id.sourceCode);
        // Get the String containing links.
        String sourceCodeLinkText = getResources().getString(R.string.source_code);
        // Set its HTML as text.
        sourceCode.setText(Html.fromHtml(sourceCodeLinkText));
        // Activate its links.
        sourceCode.setMovementMethod(LinkMovementMethod.getInstance());

        // Getting the scroll view of this Activity by id.
        final ScrollView aboutScrollView = findViewById(R.id.aboutScrollView);
        // Getting the How It Works CardView by id.
        final CardView howItWorksCard = findViewById(R.id.howItWorksCard);
        // Getting the Terms of Use CardView by id.
        final CardView termsCard = findViewById(R.id.termsCard);
        // Getting the Privacy Policy CardView by id.
        final CardView privacyCard = findViewById(R.id.privacyCard);
        // Getting the Source Code CardView by id.
        final CardView sourceCodeCard = findViewById(R.id.sourceCodeCard);

        // Getting the How It Works navigation Button by id.
        Button howItWorksBtn = findViewById(R.id.howItWorksBtn);
        // Set an onClickListener to the How It Works Button which upon click
        // scrolls to its corresponding CardView.
        howItWorksBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                aboutScrollView.smoothScrollTo(0, howItWorksCard.getTop() - toPixels(10));
            }
        });

        // Getting the Terms of Use navigation Button by id.
        Button termsBtn = findViewById(R.id.termsBtn);
        // Set an onClickListener to the Terms of Use Button which upon click
        // scrolls to its corresponding CardView.
        termsBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                aboutScrollView.smoothScrollTo(0, termsCard.getTop() - toPixels(9));
            }
        });

        // Getting the Privacy Policy navigation Button by id.
        Button privacyBtn = findViewById(R.id.privacyBtn);
        // Set an onClickListener to the Privacy Policy Button which upon click
        // scrolls to its corresponding CardView.
        privacyBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                aboutScrollView.smoothScrollTo(0, privacyCard.getTop() - toPixels(9));
            }
        });

        // Getting the Source Code navigation Button by id.
        Button sourceCodeBtn = findViewById(R.id.sourceCodeBtn);
        // Set an onClickListener to the Source Code Button which upon click
        // scrolls to its corresponding CardView.
        sourceCodeBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                aboutScrollView.smoothScrollTo(0, sourceCodeCard.getTop() - toPixels(9));
            }
        });

        // If the user's android version is at least Marshmallow,
        // set the How It Works content to avoid word breaks.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            TextView howItWorksContent = findViewById(R.id.howItWorks);
            howItWorksContent.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
        }
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
        inflater.inflate(R.menu.about_menu, menu);

        // Get the navigation bar toggle MenuItem.
        hideShowBtn = menu.findItem(R.id.toggleNavBar);

        return true;
    }


    /**
     * A method which is called whenever an item in the options menu
     * is selected. This menu will have two items, <- (home button)
     * and ^/⌄ (navigation bar toggle button). Selecting either
     * will be handled here in separate cases.
     *
     * @param item  the menu item selected
     * @return      whether menu processing may proceed
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                openMainActivity();
                return true;
            case R.id.toggleNavBar:
                toggleNavBar(hasNavBar);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * A method which will override the transition animation when MainActivity is re-opened.
     */
    @Override
    public void finish()
    {
        super.finish();

        // Allow for a smooth transition to the new Activity.
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }


    /**
     * A method to launch MainActivity.
     */
    private void openMainActivity()
    {
        // Create the required intent, i.e. a description of MainActivity.
        Intent intent = new Intent(this, MainActivity.class);

        // Start the intent.
        startActivity(intent);

        // Call this Activity's finish() method.
        finish();
    }


    /**
     * A method to toggle the about navigation bar on and off.
     *
     * @param isVisible the boolean determining whether or not the navigation is visible
     */
    private void toggleNavBar(boolean isVisible)
    {
        // If the navigation bar was already visible, switch toggle button icon,
        // hide the navigation bar and set hasNavBar to false.
        if (isVisible)
        {
            hideShowBtn.setIcon(getResources().getDrawable(R.drawable.ic_expand_more));
            aboutNavBar.setVisibility(View.GONE);
            hasNavBar = false;
        }
        // Otherwise, switch toggle button icon, show the navigation bar and set
        // hasNavBar to true.
        else
        {
            hideShowBtn.setIcon(getResources().getDrawable(R.drawable.ic_expand_less));
            aboutNavBar.setVisibility(View.VISIBLE);
            hasNavBar = true;
        }

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
}
