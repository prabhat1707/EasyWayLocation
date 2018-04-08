package com.example.prabhat.locationsample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.example.easywaylocation.EasyWayLocation;
import com.example.easywaylocation.Listener;

import static com.example.easywaylocation.EasyWayLocation.LOCATION_SETTING_REQUEST_CODE;

public class MainActivity extends AppCompatActivity implements Listener {
    EasyWayLocation easyWayLocation;
    private TextView location, latLong, diff;
    private Double lati, longi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        location = findViewById(R.id.location);
        latLong = findViewById(R.id.latlong);
        diff = findViewById(R.id.diff);
        easyWayLocation = new EasyWayLocation(this);
        easyWayLocation.setListener(this);
    }

    @Override
    public void locationOn() {
        Toast.makeText(this, "Location ON", Toast.LENGTH_SHORT).show();
        easyWayLocation.beginUpdates();
        diff.setText(String.valueOf(EasyWayLocation.calculateDistance(28.626686, 77.026409, 28.626799, 77.033619)));
    }

    @Override
    public void onPositionChanged() {
        lati = easyWayLocation.getLatitude();
        longi = easyWayLocation.getLongitude();
        location.setText(EasyWayLocation.getAddress(this, lati, longi, false, true));
        latLong.setText(String.valueOf(lati) + "  " + String.valueOf(longi));
        Toast.makeText(this, String.valueOf(easyWayLocation.getLongitude()) + "," + String.valueOf(easyWayLocation.getLatitude()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void locationCancelled() {
        easyWayLocation.showAlertDialog(getString(R.string.loc_title), getString(R.string.loc_mess), null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case LOCATION_SETTING_REQUEST_CODE:
                easyWayLocation.onActivityResult(resultCode);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // make the device update its location
        easyWayLocation.beginUpdates();


    }

    @Override
    protected void onPause() {
        // stop location updates (saves battery)
        easyWayLocation.endUpdates();


        super.onPause();
    }
}
