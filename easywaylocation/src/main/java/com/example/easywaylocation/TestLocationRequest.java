package com.example.easywaylocation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.Build;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;

/**
 * Created by prabhat on 2/10/18.
 */

public class TestLocationRequest implements RequestCallback.LocationRequestCallback {

    public static final int CURRENT_LOCATION_ONE_TIME = 0;
    public static final int CURRENT_LOCATION_UPDATES = 1;
    public static final int LAST_KNOWN_LOCATION = 2;
    public static final int SMART_LOCATION = 3;
    private static final String TAG = TestLocationRequest.class.getSimpleName();
    private final WeakReference<Activity> weakActivity;
    private FusedLocationProviderClient mLocationClient;
    private Context mContext;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    // Flag to track if continuous location updates have been requested at any point within the
    // object's life. False by default and set to true by any method that requests continuous updates.
    private boolean mHasReceivedLocationUpdates;
    // Flag to track if location updates are currently being received
    private boolean mIsReceivingUpdates;
    private AddressHelper mAddressHelper;


    public TestLocationRequest(Activity activity) {
        // assign the activity to the weak reference
        this.weakActivity = new WeakReference<>(activity);


        // Hold a reference to the Application Context single object
        this.mContext = activity.getApplicationContext();

        // Instantiate the location client
        this.mLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        // Set the request state flags to false by default
        mHasReceivedLocationUpdates = false;
        mIsReceivingUpdates = false;

        // Set up the default LocationRequest parameters (these can be changed at run-time with a
        // call to setLocationRequestParams)
        this.mLocationRequest = new LocationRequest();
        mAddressHelper = new AddressHelper(mContext, Locale.getDefault());
        setLocationRequestParams(30000, 10000, LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        // Sets up the LocationRequest with an update interval of 30 seconds, a fastest
        // update interval cap of 10 seconds and using balanced power accuracy priority.
    }

    /**
     * Retrieves and returns the device's cached last known location via an instance of
     * RequestCallback.com.example.easywaylocation.RequestCallback.LocationRequestCallback, which must be implemented by the caller.
     * <p>
     * Location can be null in certain circumstances, for example on a new or recently
     * factory-reset device or if location services are turned off in device settings.
     *
     * @param callback An interface which must be implemented by the caller in order to
     *                 receive the results of the location request.
     */
    @SuppressLint("MissingPermission")
    public void getLastKnownLocation(final RequestCallback.LocationRequestCallback callback) {
        // Re-acquire a strong reference to the calling activity and verify that it still exists and is active
        final Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing()) {
            // Activity is no longer valid, don't do anything
            return;
        }

        // Request the last known location from the location client
        mLocationClient.getLastLocation()
                .addOnSuccessListener(activity, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if (location != null) {
                            // Call back to the main thread with the location result
                            callback.onLocationResult(location);
                        } else {
                            // Call back to the main thread to advise of a null result
                            callback.onFailedRequest(mContext.getString(R.string.deviceLocationUtil_request_returned_null));
                        }

                    }
                });

    }


    /**
     * Returns the device's current location via an instance of RequestCallback.com.example.easywaylocation.RequestCallback.LocationRequestCallback,
     * which must be implemented by the caller.
     * <p>
     * Turns on location updates, retrieves the current device location then turns
     * location updates off again. This can be used if last location returns null
     * but location services are turned on, or if a more recent or accurate location
     * is needed than the one stored in the device's cache.
     *
     * @param callback An interface which must be implemented by the caller in order to
     *                 receive the results of the location request.
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocationOneTime(final RequestCallback.LocationRequestCallback callback) {
        if (mIsReceivingUpdates) {
            callback.onFailedRequest(mContext.getString(R.string.deviceLocationUtil_requests_currently_active));
            return;
        }

        // Set up the RequestCallback.LocationRequestCallback for the request
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                // Update the request state flags
                mHasReceivedLocationUpdates = true;
                mIsReceivingUpdates = true;

                if (locationResult != null) {
                    callback.onLocationResult(locationResult.getLastLocation());
                } else {
                    callback.onFailedRequest(mContext.getString(R.string.deviceLocationUtil_request_returned_null));
                }

                // Stop location updates on result (even if null)
                stopLocationUpdates();

            }
        };

        // Start the request
        mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

    }

    /**
     * Starts a location update request with the parameters specified by mLocationRequest and
     * returns the location result via an instance of RequestCallback.com.example.easywaylocation.RequestCallback.LocationRequestCallback, which must be
     * implemented by the caller.
     * <p>
     * This is inherently power-intensive so care should be taken to balance the frequency
     * of requested updates with the need for accuracy.
     * <p>
     * Location updates should be disabled using the stopLocationUpdates() method when no
     * longer needed, such as when the user closes or otherwise navigates away from the app.
     *
     * @param callback An interface which must be implemented by the caller in order to
     *                 receive the results of the location request.
     * @see TestLocationRequest#stopLocationUpdates()
     */
    @SuppressLint("MissingPermission")
    public void getCurrentLocationUpdates(final RequestCallback.LocationRequestCallback callback) {
        if (mIsReceivingUpdates) {
            callback.onFailedRequest(mContext.getString(R.string.deviceLocationUtil_requests_currently_active));
            return;
        }

        // Set up the LocationCallback for the request
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                // Update the request state flags
                mHasReceivedLocationUpdates = true;
                mIsReceivingUpdates = true;

                if (locationResult != null) {
                    callback.onLocationResult(locationResult.getLastLocation());
                } else {
                    callback.onFailedRequest(mContext.getString(R.string.deviceLocationUtil_request_returned_null));
                }

            }

        };

        // Start the request
        mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

    }

    /**
     * Returns the best available location via an instance of RequestCallback.com.example.easywaylocation.RequestCallback.LocationRequestCallback, which
     * must be implemented by the caller.
     * <p>
     * Checks if last known location is available. If unavailable (null) then a single
     * location update is requested instead.
     * <p>
     * If current location is also unavailable (due to a disabled service for example)
     * then an onFailedRequest callback is executed to be handled by the caller.
     * <p>
     * This method should be preferred over the getLastKnownLocation() and
     * getCurrentLocationOneTime() methods in most use cases.
     *
     * @param callback An interface which must be implemented by the caller in order to
     *                 receive the results of the location request.
     */
    @SuppressLint("MissingPermission")
    public void getSmartLocation(final RequestCallback.LocationRequestCallback callback) {
        // Re-acquire a strong reference to the calling activity and verify that it still exists and is active
        Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing()) {
            // Activity is no longer valid, don't do anything
            return;
        }

        // Request the last known location from the location client
        mLocationClient.getLastLocation()
                .addOnSuccessListener(activity, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if (location != null) {

                            // Call back to the main thread with the location result
                            Log.i(TAG, "getSmartLocation(): " +
                                    mContext.getString(R.string.deviceLocationUtil_location_provided_by_lastLocation));
                            callback.onLocationResult(location);

                        } else {
                            // Location result is null so request location updates to attempt to get
                            // the device's current location.

                            // Set up the LocationCallback for the request
                            mLocationCallback = new LocationCallback() {
                                @Override
                                public void onLocationResult(LocationResult locationResult) {

                                    // Update the request state flags
                                    mHasReceivedLocationUpdates = true;
                                    mIsReceivingUpdates = true;

                                    if (locationResult != null) {
                                        callback.onLocationResult(locationResult.getLastLocation());
                                        Log.i(TAG, "getSmartLocation(): " +
                                                mContext.getString(R.string.deviceLocationUtil_location_provided_by_locationUpdates));
                                        // Stop location updates now that we have a location result
                                        stopLocationUpdates();
                                    } else {
                                        callback.onFailedRequest(mContext.getString(R.string.deviceLocationUtil_request_returned_null));
                                        // Stop location updates on null result
                                        stopLocationUpdates();
                                    }

                                }

                            };

                            // Start the request
                            mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);

                        }

                    }
                });

    }

    public boolean permissionIsGranted() {

        int permissionState = ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.ACCESS_FINE_LOCATION);

        return permissionState == PackageManager.PERMISSION_GRANTED;
    }


    /**
     * Explicitly requests permission to access the device's fine location.
     * <p>
     * Determines if additional rationale should be provided to the user, displays it if
     * so then initiates a permission request via a call to startPermissionRequest().
     *
     * @param requestCode A package-defined int constant to identify the request.
     *                    It is returned to the onRequestPermissionsResult callback
     *                    which must be implemented by the caller.
     */
    public void requestPermission(final int requestCode) {
        // Re-acquire a strong reference to the calling activity and verify that it still exists and is active
        Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing()) {
            // Activity is no longer valid, don't do anything
            return;
        }

        // Check device SDK version as run-time permissions were only introduced in SDK ver.23
        if (Build.VERSION.SDK_INT >= 23) {

            // Determine if additional rationale for the permission request should be displayed to the user
            boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.ACCESS_FINE_LOCATION);

            if (shouldProvideRationale) {

                // Provide additional rationale to the user. This would happen if the user denied the request
                // previously but didn't tick the "Don't ask again" checkbox.
                RationaleDialogProvider dialog = new RationaleDialogProvider(activity, requestCode);
                dialog.displayDialog(new RequestCallback.PermissionRequestCallback() {
                    @Override
                    public void onRationaleDialogOkPressed(int requestCode) {
                        startPermissionRequest(requestCode);
                    }
                });

            } else {

                // Request permission. It's possible this can be auto-answered if the device policy sets
                // the permission in a given state or the user denied the request previously and ticked
                // the "Don't ask again" checkbox.
                startPermissionRequest(requestCode);
            }

        }
        // Fail gracefully if SDK version < 23
    }


    /**
     * Called by requestPermission() to initiate a permission request
     *
     * @param requestCode The request code passed in by requestPermission()
     * @see TestLocationRequest#requestPermission(int)
     */
    private void startPermissionRequest(int requestCode) {
        // Re-acquire a strong reference to the calling activity and verify that it still exists and is active
        Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing()) {
            // Activity is no longer valid, don't do anything
            return;
        }

        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                requestCode);
        // requestCode is an int constant. The onRequestPermissionsResult callback
        // gets the result of the request. The calling Activity should implement
        // ActivityCompat.OnRequestPermissionsResultCallback then override the
        // onRequestPermissionsResult() method to handle the result.
    }


    /**
     * Checks if all required location settings are satisfied
     * <p>
     * If the settings are not satisfied a dialog requesting the user enable the required
     * settings will be displayed. The result of the request can be checked in
     * onActivityResult() in the calling Activity if necessary.
     *
     * @param requestCode A package-defined int constant to identify the request.
     *                    It is returned to the onActivityResult callback which
     *                    must be implemented by the caller.
     */
    public void checkDeviceSettings(final int requestCode) {
        // Re-acquire a strong reference to the calling activity and verify that it still exists and is active
        final Activity activity = weakActivity.get();
        if (activity == null || activity.isFinishing()) {
            // Activity is no longer valid, don't do anything
            return;
        }

        // Create a settings request builder and pass it the LocationRequest
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        // Create a settings client
        SettingsClient client = LocationServices.getSettingsClient(mContext);

        // Create a Task from the client
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        // Query the result of the Task to determine if the required location settings are satisfied
        task.addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // Location settings are satisfied, no need for further action
                Log.i(TAG, mContext.getString(R.string.deviceLocationUtil_location_settings_satisfied));
            }
        });
        task.addOnFailureListener(activity, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, display a dialog to the user
                // requesting the settings to be enabled
                Log.e(TAG, mContext.getString(R.string.deviceLocationUtil_location_settings_not_satisfied));
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    // Show the dialog
                    resolvable.startResolutionForResult(activity, requestCode);
                } catch (IntentSender.SendIntentException sendException) {
                    // Ignore the error.
                }

            }
        });

    }


    /**
     * Determines if location updates are currently active or not
     *
     * @return True if receiving location updates, false if not.
     */
    public boolean isReceivingLocationUpdates() {
        return mIsReceivingUpdates;
    }


    /**
     * Determines if continuous location updates have been initiated at any
     * point within the object's life.
     *
     * @return True if so, false if not.
     */
    public boolean hasEverReceivedLocationUpdates() {
        return mHasReceivedLocationUpdates;
    }


    /**
     * Stops location updates from being received.
     * <p>
     * This should be called in the calling Activity's onPause() method so
     * that location updates don't continue in the background when the user
     * navigates away from the app (unless such functionality is explicitly
     * required).
     */
    public void stopLocationUpdates() {

        if (mLocationCallback != null && mIsReceivingUpdates) {
            mLocationClient.removeLocationUpdates(mLocationCallback);
            mIsReceivingUpdates = false;
            Log.i(TAG, mContext.getString(R.string.deviceLocationUtil_location_updates_removed));
        }

    }


    /**
     * Resumes location updates if they have previously been set up
     * <p>
     * This should be called in the calling Activity's onResume() method if
     * you want your app to continue to receive location updates when it resumes
     *
     * @see TestLocationRequest#stopLocationUpdates()
     */
    @SuppressLint("MissingPermission")
    public void resumeLocationUpdates() {

        if (mLocationCallback != null && !mIsReceivingUpdates) {
            mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            mIsReceivingUpdates = true;
            Log.i(TAG, mContext.getString(R.string.deviceLocationUtil_location_updates_resumed));
        }

    }


    /**
     * Provides a way for a calling Activity to set or change the default parameters of
     * the LocationRequest object to suit its needs.
     *
     * @param interval        Set the desired interval for active location updates, in milliseconds.
     * @param fastestInterval Explicitly set the fastest interval for location updates, in
     *                        milliseconds. This controls the fastest rate at which your
     *                        application will receive location updates.
     * @param priority        Set the priority of the request. Use with a priority constant such as
     *                        LocationRequest.PRIORITY_HIGH_ACCURACY. No other values are accepted.
     */
    public void setLocationRequestParams(long interval, long fastestInterval, int priority) {

        if (mLocationRequest == null) {
            mLocationRequest = new LocationRequest();
        }

        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastestInterval);
        mLocationRequest.setPriority(priority);
    }


    @Override
    public void onLocationResult(Location location) {

    }

    @Override
    public void onFailedRequest(String result) {

    }



    public void getAddressList(Location loca,RequestCallback.AddressRequestCallback addressRequestCallback) {
        mAddressHelper.getAddressList(loca, addressRequestCallback);
    }

    public void getAddressByElement(int elementCode, Location location) {
        mAddressHelper.getAddressElement(elementCode, location);
    }


}
