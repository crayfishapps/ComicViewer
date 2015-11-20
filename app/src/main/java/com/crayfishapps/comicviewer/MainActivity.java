package com.crayfishapps.comicviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    private static final String SELECTED_DATE = "selected_date";

    private Calendar actualDate;    // holds the selected day
    private Calendar minDate;       // the day of the first Dilbert strip
    private Calendar maxDate;       // today

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup action bar to show logo and title bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);

        // initiate calendars
        Calendar now = Calendar.getInstance();
        if (savedInstanceState != null) {
            actualDate = (Calendar) savedInstanceState.getSerializable(SELECTED_DATE);
        } else {
            actualDate = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        }
        minDate = new GregorianCalendar(1989, Calendar.APRIL, 16);
        maxDate = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));

        // load today's strip at startup
        loadImage(actualDate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Move to the previous day
            case R.id.action_previous:
                if (!actualDate.equals(minDate)) {
                    actualDate.add(Calendar.DAY_OF_MONTH, -1);
                }
                loadImage(actualDate);
                return true;

            // Select a day from the calendar
            case R.id.action_select:
                Calendar now = Calendar.getInstance();
                DatePickerDialog dpd = DatePickerDialog.newInstance(
                        MainActivity.this,
                        actualDate.get(Calendar.YEAR),
                        actualDate.get(Calendar.MONTH),
                        actualDate.get(Calendar.DAY_OF_MONTH)
                );
                dpd.setMinDate(minDate);
                dpd.setMaxDate(maxDate);
                dpd.show(getFragmentManager(), "Datepickerdialog");
                return true;

            // Move to the next day
            case R.id.action_next:
                if (!actualDate.equals(maxDate)) {
                    actualDate.add(Calendar.DAY_OF_MONTH, 1);
                }
                loadImage(actualDate);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(SELECTED_DATE, actualDate);
    }

    @Override
    public void onResume() {
        super.onResume();
        DatePickerDialog dpd = (DatePickerDialog) getFragmentManager().findFragmentByTag("Datepickerdialog");
        if(dpd != null) {
            dpd.setOnDateSetListener(this);
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int month, int dayOfMonth) {
        actualDate = new GregorianCalendar(year, month, dayOfMonth);
        loadImage(actualDate);
    }

    private class ParseURL extends AsyncTask<String, Void, Bitmap> {

        Bitmap bitmap = null;

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                // locate the image resource
                Document document = Jsoup.connect(strings[0]).userAgent("Mozilla").get();
                String urlImage = document.select("meta[property=\"og:image\"]").get(0).attr("content");

                // load the image
                URL url = new URL(urlImage);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream inputstream = new BufferedInputStream(urlConnection.getInputStream());
                bitmap = BitmapFactory.decodeStream(inputstream);
            } catch (Exception e) {
                e.getMessage();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView imageview = (ImageView) findViewById(R.id.imageViewStrip);
            ScrollView scrollview = (ScrollView) findViewById(R.id.scrollView);
            HorizontalScrollView horizontalscrollview = (HorizontalScrollView) findViewById(R.id.horizontalScrollView);
            scrollview.scrollTo(0, 0);
            horizontalscrollview.scrollTo(0, 0);

            // adjust the size of the bitmap to screen size and orientation
            int scaling = (1000 * scrollview.getHeight()) / (bitmap.getHeight());
            if (scaling < 1500) {
                scaling = 1500;
            }
            if (scaling > 2000) {
                scaling = 2000;
            }
            Bitmap bitmapScaled = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * scaling / 1000, bitmap.getHeight() * scaling / 1000, true);
            imageview.setImageBitmap(bitmapScaled); ;
        }
    }

    private void loadImage(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        // compose URL
        String date = "dilbert.com/strip/" + year + "-" + String.format("%02d", (month+1)) + "-" + String.format("%02d", dayOfMonth);
        new ParseURL().execute("http://" + date);
    }

}