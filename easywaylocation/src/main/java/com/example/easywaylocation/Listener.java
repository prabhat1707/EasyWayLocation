package com.example.easywaylocation;

/**
 * Created by prabhat on 11/2/18.
 */

import android.location.Location;

/**
 * Callback that can be implemented in order to listen for events
 */

public interface Listener {
    public String TAG = "Location_Sample_Logs";
    void locationOn();

    void currentLocation(Location location);

    void locationCancelled();
}


