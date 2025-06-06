package com.pedal.app.pedal;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class History extends BaseActivity {

    int minutes, seconds;
    ListView listItem;
    public static final String fetch_from_server = "http://188.166.232.9/Cycling%20app/Services/fetchUserData.php";
    public static final String url_server = "http://188.166.232.9/Cycling%20app/Services/syncToServer.php";
    private View pDialog;
    ArrayList<HashMap<String, String>> listOfHistory;
    TextView profileName, profileMobNO, noDataMessage;

    DatabaseHandler databaseHandler = new DatabaseHandler(this);

    JsonParser jsonParser = new JsonParser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        pDialog = findViewById(R.id.history_fetch_progress);

        commonDrawerConfigCall();
        addDrawerItems();
        setupDrawer();
        getSupportActionBar().setTitle(Html.fromHtml(getString(R.string.historyActiivty)));

        profileName = (TextView) findViewById(R.id.profileName);
        profileMobNO = (TextView) findViewById(R.id.mobileNo);
        noDataMessage=(TextView)findViewById(R.id.noDataMessage);

        listItem = (ListView) findViewById(R.id.historyItem);

        if (sessionManagement.isLoggedIn()) {

            String[] profileDetails;
            profileDetails = sessionManagement.getProfileDetail();
            profileName.setText(profileDetails[0]);
            profileMobNO.setText("+" + profileDetails[1]);

            if (!databaseHandler.isAllDbSyncedOfNotLoggedUser()) {
                databaseHandler.fetchNotSyncedDataFromNotLoggedInTableAndCopyDataToLoggedInTable(sessionManagement.getUserId());
            }

                if(!isNetworkAvailable(getApplicationContext())) {
                    getUserCyclingDataWhenNotLoggedInFromLocalDb();
                    alertNetworkNotAvailable();
                }
                else
                    new syncToSever().execute();


        } else {
            getUserCyclingDataWhenNotLoggedInFromLocalDb();
        }

    }

    public void History() {

        int size = listOfHistory.size(), j = 0;
        if(size==0)
        {
            noDataMessage.setVisibility(View.VISIBLE);
        }
        else {
            String totalTime = "", totalTimeText = "", totalDistance = "";
            // "mmm:ss" expects a three digit minute which is incorrect for
            // values like "12:34". Use two digit minute format instead.
            SimpleDateFormat timeFormat = new SimpleDateFormat("mm:ss");

            ObjectHistoryItem[] item = new ObjectHistoryItem[size];
            for (int i = size - 1; i >= 0; i--) {
                totalTime = listOfHistory.get(i).get("totalTime");

                totalDistance = listOfHistory.get(i).get("distance");
                double totaldistance = Double.parseDouble(totalDistance);
                totaldistance = round(totaldistance, 2);

                try {
                    Date time = timeFormat.parse(totalTime);
                    Calendar calendarTime = Calendar.getInstance();
                    calendarTime.setTime(time);
                    int hours = calendarTime.get(calendarTime.HOUR);
                    minutes = calendarTime.get(calendarTime.MINUTE) + hours * 60;
                    seconds = calendarTime.get(calendarTime.SECOND);
                    totalTimeText = Integer.toString(minutes) + "m" + Integer.toString(seconds) + "s";

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                item[j] = new ObjectHistoryItem(R.drawable.cycling_red, getString(R.string.cycling), listOfHistory.get(i).get("date"), listOfHistory.get(i).get("startTime") + "-" + listOfHistory.get(i).get("endTime"), Double.toString(totaldistance) + getString(R.string.kms), totalTimeText, getString(R.string.alone));
                j++;
            }

            CustomAdapterForHistory customAdapterForHistory = new CustomAdapterForHistory(this, R.layout.history_item, item);
            listItem.setAdapter(customAdapterForHistory);
        }

    }

    protected void getUserCyclingDataWhenNotLoggedInFromLocalDb() {
        ArrayList<HashMap<String, String>> allData = databaseHandler.fetchAllDataFromNotLoggedInTable();

        int size=allData.size();
        if(size==0)
        {
            noDataMessage.setVisibility(View.VISIBLE);
        }
        else {
            String startTimeString, endTimeString, timeElapsedString, distanceString, startDateString;
            int j = 0;

            SimpleDateFormat parseDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // The cycling time is stored as minutes and seconds (e.g. "05:23").
            // Using "mmm" would require three digits for minutes and fails to
            // parse standard two digit values. Use "mm" instead.
            SimpleDateFormat parseTime = new SimpleDateFormat("mm:ss");

            SimpleDateFormat displayTime = new SimpleDateFormat("hh:mm a");
            SimpleDateFormat displayDate = new SimpleDateFormat("dd MMM'' yyyy");

            Calendar calendarDate = Calendar.getInstance();

            ObjectHistoryItem[] item = new ObjectHistoryItem[size];
            for (int i = size - 1; i >= 0; i--) {
                HashMap<String, String> allDataRow = allData.get(i);
                startDateString = allDataRow.get("startTime");
                startTimeString = allDataRow.get("startTime");
                timeElapsedString = allDataRow.get("timeelapsed");
                endTimeString = allDataRow.get("time");
                distanceString = allDataRow.get("distance");

                distanceString = distanceString + getString(R.string.kms);

                try {
                    Date startDate = parseDate.parse(startDateString);
                    startDateString = displayDate.format(startDate);

                    startTimeString = displayTime.format(startDate);

                    Date endDate = parseDate.parse(endTimeString);
                    endTimeString = displayTime.format(endDate);

                    Date time = parseTime.parse(timeElapsedString);
                    calendarDate.setTime(time);
                    int hours = calendarDate.get(calendarDate.HOUR);
                    minutes = calendarDate.get(calendarDate.MINUTE) + hours * 60;
                    seconds = calendarDate.get(calendarDate.SECOND);
                    timeElapsedString = Integer.toString(minutes) + "m" + Integer.toString(seconds) + "s";

                } catch (ParseException e) {
                    e.printStackTrace();
                }

                item[j] = new ObjectHistoryItem(R.drawable.cycling_red, getString(R.string.cycling), startDateString, startTimeString + "-" + endTimeString, distanceString, timeElapsedString, "Alone");
                j = j + 1;
            }

            CustomAdapterForHistory customAdapterForHistory = new CustomAdapterForHistory(this, R.layout.history_item, item);
            listItem.setAdapter(customAdapterForHistory);
        }

    }


    class syncToSever extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog.setVisibility(View.VISIBLE);
        }

        protected String doInBackground(String... args) {
            //Check if all local db sync to server
            if (!databaseHandler.isAllDbSynced()) {
                //check if there is active network connection
                if (hasActiveInternetConnection(getApplicationContext())) {
                    // Building Parameters
                    List<NameValuePair> params = new ArrayList<NameValuePair>();
                    params.add(new BasicNameValuePair("userStat", databaseHandler.composeJson()));


                    // getting JSON Array
                    jsonParser.makeHttpRequest(url_server,
                            "POST", params);

                    JSONArray json = jsonParser.jsonForMultipleObj();

                    try {
                        for (int i = 0; i < json.length(); i++) {
                            JSONObject obj = (JSONObject) json.get(i);
                            String status = obj.getString("status");
                            String date = obj.getString("timestamp");
                            if (status.equals("yes")) {
                                databaseHandler.updateDbSyncStatus(date);
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            new FetchHistory().execute();
        }
    }


    class FetchHistory extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        protected String doInBackground(String... args) {

            //check if there is active network connection
            if (hasActiveInternetConnection(getApplicationContext())) {
                // Building Parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("userId", Integer.toString(sessionManagement.getUserId())));


                // getting JSON Array
                jsonParser.makeHttpRequest(fetch_from_server,
                        "POST", params);
                listOfHistory = new ArrayList<HashMap<String, String>>();

                JSONArray json = jsonParser.jsonForMultipleObj();
                if (json != null) {

                    try {

                        for (int i = 0; i < json.length(); i++) {
                            JSONObject obj = (JSONObject) json.get(i);

                            HashMap<String, String> map = new HashMap<String, String>();

                            map.put("date", obj.getString("date"));
                            map.put("startTime", obj.getString("start"));
                            map.put("endTime", obj.getString("end"));
                            map.put("distance", obj.getString("distance"));
                            map.put("totalTime", obj.getString("timeElapsed"));
                            listOfHistory.add(map);

                           // Log.d("date", obj.getString("date"));

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            pDialog.setVisibility(View.GONE);
            History();
        }
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public void comingSoon(View view) {
        Toast.makeText(getApplicationContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
    }

    public void alertNetworkNotAvailable() {
        android.support.v7.app.AlertDialog.Builder dialog = new android.support.v7.app.AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle);
        dialog.setCancelable(false);
        dialog.setTitle("Network Error");
        dialog.setMessage("To save your data to online server, make sure you are connected to internet");
        dialog.setPositiveButton("Wifi Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                Intent myIntent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                startActivity(myIntent);
            }
        });
        dialog.setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {

            }
        });
        dialog.show();

    }

}
