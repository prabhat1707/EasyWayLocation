package com.example.prabhat.locationsample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.easywaylocation.EasyWayLocation;
import com.example.easywaylocation.Listener;
import com.example.easywaylocation.draw_path.DirectionUtil;

import static com.example.easywaylocation.EasyWayLocation.LOCATION_SETTING_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements Listener {
    //EasyWayLocation easyWayLocation;
    private TextView location, latLong, diff;
    private Double lati, longi;
    //private TestLocationRequest testLocationRequest;
    private EasyWayLocation easyWayLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        location = findViewById(R.id.location);
        latLong = findViewById(R.id.latlong);
        diff = findViewById(R.id.diff);
//        DirectionUtil directionUtil = new DirectionUtil.Builder()
//                .setDirectionKey("AIzaSyDUCCidq_7tBb0s1LRLhhvFyNqd0BeQBuI")
//
        // testLocationRequest = new TestLocationRequest(this);
        easyWayLocation = new EasyWayLocation(this, false,this);
        if (permissionIsGranted()) {
            doLocationWork();
        } else {
            // Permission not granted, ask for it
            //testLocationRequest.requestPermission(121);
        }
    }

    public boolean permissionIsGranted() {

        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void doLocationWork() {
        easyWayLocation.startLocation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOCATION_SETTING_REQUEST_CODE) {
            easyWayLocation.onActivityResult(resultCode);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        easyWayLocation.startLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        easyWayLocation.endUpdates();

    }

    @Override
    public void locationOn() {
    }

    @Override
    public void currentLocation(Location location) {
        Log.v("location_test","------------>"+location.getLatitude());
    }

    @Override
    public void locationCancelled() {

    }
}
