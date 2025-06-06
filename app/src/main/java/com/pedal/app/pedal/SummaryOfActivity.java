package com.pedal.app.pedal;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class SummaryOfActivity extends BaseActivity {

    TextView avgSpeedtext, distanceText, timeText, caloriesText, profileName, profileMobNO, dateTimeSummary;
    String timeElapsed, lastActive, distance, avgSpeed, startTimeString, calories;
    Button registerButton;
    GoogleMap googleMap;

    DatabaseHandler databaseHandler = new DatabaseHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary_of);
        initializeMap();

        profileName = (TextView) findViewById(R.id.profileName);
        profileMobNO = (TextView) findViewById(R.id.mobileNo);

        commonDrawerConfigCall();
        addDrawerItems();
        setupDrawer();

        getSupportActionBar().setTitle(Html.fromHtml(getString(R.string.summaryActivity)));

        setUserDataOfCycling();

        if (sessionManagement.isLoggedIn()) {

            String[] profileDetails;
            profileDetails = sessionManagement.getProfileDetail();
            profileName.setText(profileDetails[0]);
            profileMobNO.setText("+" + profileDetails[1]);
        }


        avgSpeedtext = (TextView) findViewById(R.id.speed);
        distanceText = (TextView) findViewById(R.id.distance);
        timeText = (TextView) findViewById(R.id.time);
        caloriesText = (TextView) findViewById(R.id.calories);
        dateTimeSummary = (TextView) findViewById(R.id.timeText);

        timeText.setText(timeElapsed);
        distanceText.setText(distance);
        avgSpeedtext.setText(avgSpeed);
        caloriesText.setText(calories);
        setTimeOnSummaryPage();


    }

    public void setUserDataOfCycling() {
        String userDataOfCycling[] = getUserCyclingData();
        this.startTimeString = userDataOfCycling[0];
        this.timeElapsed = userDataOfCycling[1];
        this.lastActive = userDataOfCycling[2];
        this.distance = userDataOfCycling[3];
        this.avgSpeed = userDataOfCycling[4];
        this.calories = userDataOfCycling[5];
    }

    public void setTimeOnSummaryPage() {
        SimpleDateFormat parseDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // "mmm" expects a three digit minute and will not correctly parse
        // values like "10:05". Use the two digit minute pattern.
        SimpleDateFormat parseTime = new SimpleDateFormat("mm:ss");

        SimpleDateFormat displayTime = new SimpleDateFormat("h:mm a");
        SimpleDateFormat displayDate = new SimpleDateFormat("MMMM dd,yyyy");
        try {
            Date startDate = parseDate.parse(startTimeString);
            String startDateString = displayDate.format(startDate);

            String startTime = displayTime.format(startDate);
            Date endDate = parseDate.parse(lastActive);
            String endTimeString = displayTime.format(endDate);
            dateTimeSummary.setText(startDateString + " - " + startTime + " to " + endTimeString);

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void initializeMap() {

        if (googleMap == null) {
            final MapFragment mapFragment = ((MapFragment) getFragmentManager().findFragmentById(R.id.map));
             mapFragment.getMapAsync(new OnMapReadyCallback() {
                 @Override
                 public void onMapReady(GoogleMap map) {
                     googleMap = map;
                     drawPathOnMap();
                     UiSettings uiSettings = googleMap.getUiSettings();
                     uiSettings.setMapToolbarEnabled(false);
                     googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                         @Override
                         public void onMapClick(LatLng latLng) {
                             mapFragment.getView().setClickable(false);
                         }
                     });
                 }
             });


        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeMap();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.summary_page_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.tick:
                if (sessionManagement.isLoggedIn()) {
                    saveUserDataIntoTableLoggedIn();
                    Intent intentForStart = new Intent(getApplicationContext(), History.class);
                    startActivity(intentForStart);
                    finish();
                } else {
                    alertIfNotLoggedIn();
                }
                return true;

            case R.id.cancel:
                alertBeforeCancelSummary();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void alertBeforeCancelSummary() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(SummaryOfActivity.this, R.style.AppCompatAlertDialogStyle);
        dialog.setTitle("Discard Trip");
        dialog.setMessage("This will discard your trip, are you sure?");
        dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                Intent intentForCancel = new Intent(getApplicationContext(), StartActivity.class);
                intentForCancel.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intentForCancel);

            }
        });
        dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {

            }
        });
        dialog.show();
    }

    public void alertIfNotLoggedIn() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(SummaryOfActivity.this, R.style.AppCompatAlertDialogStyle);
        dialog.setTitle("Login");
        dialog.setMessage("You don't seem to be logged in. In order to save your trip to server please login.");
        dialog.setPositiveButton("Register", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                saveUserDataIntoTableNotLoggedIn();
                Intent intentForRegister = new Intent(getApplicationContext(), Registration.class);
                startActivity(intentForRegister);
                finish();
            }
        });
        dialog.setNegativeButton("Login", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                saveUserDataIntoTableNotLoggedIn();
                Intent intentForLogin = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intentForLogin);
                finish();
            }
        });
        dialog.show();
    }

    protected String[] getUserCyclingData() {
        String getUserCyclingData[] = getIntent().getStringArrayExtra("userDataOfCycling");
        return getUserCyclingData;
    }

    public long saveUserDataIntoTableLoggedIn() {
        long rowId = databaseHandler.insertValuesInTableLoggedIn(new UserData(timeElapsed, distance, avgSpeed, lastActive, sessionManagement.getUserId(), startTimeString, calories));
        return rowId;

    }

    public long saveUserDataIntoTableNotLoggedIn() {
        long rowId = databaseHandler.insertValuesInTableNotLoggedIn(new UserData(timeElapsed, distance, avgSpeed, lastActive, startTimeString, calories));
        return rowId;

    }

    protected void drawPathOnMap()
    {
        Bundle bundleForPoints = getIntent().getExtras();
        ArrayList<LatLng> points = (ArrayList<LatLng>) bundleForPoints.getSerializable("Points");

        if (points.size() != 0) {

            for (int i = 0; i < points.size() - 1; i++) {

                Polyline line = googleMap.addPolyline(new PolylineOptions()
                        .add(points.get(i), points.get(i + 1))
                        .width(6)
                        .color(Color.rgb(31, 144, 255)));
            }
            MarkerOptions startMark = new MarkerOptions().position(points.get(0));
            MarkerOptions endMark = new MarkerOptions().position(points.get(points.size() - 1));

            startMark.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_green));
            endMark.icon(BitmapDescriptorFactory.fromResource(R.drawable.location_red));

            googleMap.addMarker(startMark);
            googleMap.addMarker(endMark);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(points.size() - 1), 16));
        }
    }

    @Override
    public void onBackPressed() {
        alertBeforeCancelSummary();
    }
}