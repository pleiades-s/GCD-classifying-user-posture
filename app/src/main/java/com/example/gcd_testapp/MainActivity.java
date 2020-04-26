package com.example.gcd_testapp;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ListView;
import android.app.PendingIntent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Context mContext;
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    //Define an ActivityRecognitionClient//

    private ActivityRecognitionClient mActivityRecognitionClient;
    private ActivitiesAdapter mAdapter;

    // Gyro sensor
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private  SensorEventListener gyroscopeEventListener;

    // Accelerometor sensor
    private Sensor accelorSensor;
    private SensorEventListener accelorEventListener;

    private boolean IsHolding = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        accelorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


        if (gyroscopeSensor == null){
            Toast.makeText(this, "The device has no Gyroscope", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (accelorSensor == null){
            Toast.makeText(this, "The device has no Accelerometer", Toast.LENGTH_SHORT).show();
            finish();
        }

        gyroscopeEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {

                String log = String.valueOf(sensorEvent.values[0]) + " " + String.valueOf(sensorEvent.values[1])  + " " + String.valueOf(sensorEvent.values[2]);
                Log.d("gyro", log);
                // On table
                if (sensorEvent.values[0] < 0.005f && sensorEvent.values[0] > -0.005f){
                    getWindow().getDecorView().setBackgroundColor(Color.GREEN);
                    IsHolding = false;
                }
                else{
                    IsHolding = true;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        accelorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {

                String log = String.valueOf(sensorEvent.values[0]) + " " + String.valueOf(sensorEvent.values[1])  + " " + String.valueOf(sensorEvent.values[2]);
                Log.d("acceler", log);

                if (IsHolding) {
                    // Right
                    if (sensorEvent.values[0] < -2.5f) {
                        getWindow().getDecorView().setBackgroundColor(Color.CYAN);
                    }

                    // Left
                    else if (sensorEvent.values[0] > 2.5f) {
                        getWindow().getDecorView().setBackgroundColor(Color.BLUE);
                    } else {
                        // Sit or Stand
                        if (sensorEvent.values[2] > 2.5f) {
                            getWindow().getDecorView().setBackgroundColor(Color.RED);
                        }
                        // Back
                        else if (sensorEvent.values[2] < -2.5f) {
                            getWindow().getDecorView().setBackgroundColor(Color.YELLOW);
                        }
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        mContext = this;

        //Retrieve the ListView where we’ll display our activity data//
        ListView detectedActivitiesListView = (ListView) findViewById(R.id.activities_listview);

        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        DETECTED_ACTIVITY, ""));

//Bind the adapter to the ListView//
        mAdapter = new ActivitiesAdapter(this, detectedActivities);
        detectedActivitiesListView.setAdapter(mAdapter);
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();

        sensorManager.registerListener(gyroscopeEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(accelorEventListener, accelorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }
    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();

        sensorManager.unregisterListener(gyroscopeEventListener);
        sensorManager.unregisterListener(accelorEventListener);
    }
    public void requestUpdatesHandler(View view) {
    //Set the activity detection interval. I’m using 3 seconds//
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                3000,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });
    }
    //Get a PendingIntent//
    private PendingIntent getActivityDetectionPendingIntent() {
//Send the activity data to our DetectedActivitiesIntentService class//
        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }
    //Process the list of activities//
    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(DETECTED_ACTIVITY, ""));

        mAdapter.updateActivities(detectedActivities);
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }
}