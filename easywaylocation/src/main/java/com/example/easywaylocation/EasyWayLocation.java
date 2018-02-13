package com.example.easywaylocation;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Utility class for easy access to the device location on Android
 */
public class EasyWayLocation implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {


    /*
    google api client request code for activty result
     */
    public static final int LOCATION_SETTING_REQUEST_CODE = 5;

    /**
     * {@link Listener} object
     */

    public Listener getmListener;
    /**
     * The internal name of the provider for the coarse location
     */
    private static final String PROVIDER_COARSE = LocationManager.NETWORK_PROVIDER;
    /**
     * The internal name of the provider for the fine location
     */
    private static final String PROVIDER_FINE = LocationManager.GPS_PROVIDER;
    /**
     * The internal name of the provider for the fine location in passive mode
     */
    private static final String PROVIDER_FINE_PASSIVE = LocationManager.PASSIVE_PROVIDER;
    /**
     * The default interval to receive new location updates after (in milliseconds)
     */
    private static final long INTERVAL_DEFAULT = 10 * 60 * 1000;
    /**
     * The factor for conversion from kilometers to meters
     */
    private static final float KILOMETER_TO_METER = 1000.0f;
    /**
     * The factor for conversion from latitude to kilometers
     */
    private static final float LATITUDE_TO_KILOMETER = 111.133f;
    /**
     * The factor for conversion from longitude to kilometers at zero degree in latitude
     */
    private static final float LONGITUDE_TO_KILOMETER_AT_ZERO_LATITUDE = 111.320f;
    /**
     * The PRNG that is used for location blurring
     */
    private static final Random mRandom = new Random();
    private static final double SQUARE_ROOT_TWO = Math.sqrt(2);
    /**
     * The last location that was internally cached when creating new instances in the same process
     */
    private static Location mCachedPosition;
    /**
     * The LocationManager instance used to query the device location
     */
    private final LocationManager mLocationManager;
    /**
     * Whether a fine location should be required or coarse location can be used
     */
    private final boolean mRequireFine;
    /**
     * Whether passive mode shall be used or not
     */
    private final boolean mPassive;
    /**
     * The internal after which new location updates are requested (in milliseconds) where longer intervals save battery
     */
    private final long mInterval;
    /**
     * Whether to require a new location (`true`) or accept old (last known) locations as well (`false`)
     */
    private final boolean mRequireNewLocation;
    boolean gps_enabled = false;
    boolean network_enabled = false;
    GoogleApiClient googleApiClient;
    private Boolean locationReturn = true;
    private Context context;
    /**
     * The blur radius (in meters) that will be used to blur the location for privacy reasons
     */
    private int mBlurRadius;
    /**
     * The LocationListener instance used internally to listen for location updates
     */
    private LocationListener mLocationListener;
    /**
     * The current location with latitude, longitude, speed and altitude
     */
    private Location mPosition;
    private Listener mListener;

    /**
     * Constructs a new instance with default granularity, mode and interval
     *
     * @param context the Context reference to get the system service from
     */
    public EasyWayLocation(final Context context) {
        this(context, false);
    }

    /**
     * Constructs a new instance with default mode and interval
     *
     * @param context     the Context reference to get the system service from
     * @param requireFine whether to require fine location or use coarse location
     */
    public EasyWayLocation(final Context context, final boolean requireFine) {
        this(context, requireFine, false);
    }

    /**
     * Constructs a new instance with default interval
     *
     * @param context     the Context reference to get the system service from
     * @param requireFine whether to require fine location or use coarse location
     * @param passive     whether to use passive mode (to save battery) or active mode
     */
    public EasyWayLocation(final Context context, final boolean requireFine, final boolean passive) {
        this(context, requireFine, passive, INTERVAL_DEFAULT);
    }

    /**
     * Constructs a new instance
     *
     * @param context     the Context reference to get the system service from
     * @param requireFine whether to require fine location or use coarse location
     * @param passive     whether to use passive mode (to save battery) or active mode
     * @param interval    the interval to request new location updates after (in milliseconds) where longer intervals save battery
     */
    public EasyWayLocation(final Context context, final boolean requireFine, final boolean passive, final long interval) {
        this(context, requireFine, passive, interval, false);
    }

    /**
     * Constructs a new instance
     *
     * @param context            the Context reference to get the system service from
     * @param requireFine        whether to require fine location or use coarse location
     * @param passive            whether to use passive mode (to save battery) or active mode
     * @param interval           the interval to request new location updates after (in milliseconds) where longer intervals save battery
     * @param requireNewLocation whether to require a new location (`true`) or accept old (last known) locations as well (`false`)
     */
    public EasyWayLocation(final Context context, final boolean requireFine, final boolean passive, final long interval, final boolean requireNewLocation) {
        mLocationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        mRequireFine = requireFine;
        this.context = context;
        mPassive = passive;
        mInterval = interval;
        mRequireNewLocation = requireNewLocation;

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        googleApiClient.connect();

        if (!mRequireNewLocation) {
            mPosition = getCachedPosition();
            cachePosition();
        }
    }

    /**
     * For any radius `n`, calculate a random offset in the range `[-n, n]`
     *
     * @param radius the radius
     * @return the random offset
     */
    private static int calculateRandomOffset(final int radius) {
        return mRandom.nextInt((radius + 1) * 2) - radius;
    }

    /**
     * Opens the device's settings screen where location access can be enabled
     *
     * @param context the Context reference to start the Intent from
     */
    public static void openSettings(final Context context) {
        context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    }

    /**
     * Converts a difference in latitude to a difference in kilometers (rough estimation)
     *
     * @param latitude the latitude (difference)
     * @return the kilometers (difference)
     */
    public static double latitudeToKilometer(double latitude) {
        return latitude * LATITUDE_TO_KILOMETER;
    }

    /**
     * Converts a difference in kilometers to a difference in latitude (rough estimation)
     *
     * @param kilometer the kilometers (difference)
     * @return the latitude (difference)
     */
    public static double kilometerToLatitude(double kilometer) {
        return kilometer / latitudeToKilometer(1.0f);
    }

    /**
     * Converts a difference in latitude to a difference in meters (rough estimation)
     *
     * @param latitude the latitude (difference)
     * @return the meters (difference)
     */
    public static double latitudeToMeter(double latitude) {
        return latitudeToKilometer(latitude) * KILOMETER_TO_METER;
    }

    /**
     * Converts a difference in meters to a difference in latitude (rough estimation)
     *
     * @param meter the meters (difference)
     * @return the latitude (difference)
     */
    public static double meterToLatitude(double meter) {
        return meter / latitudeToMeter(1.0f);
    }

    /**
     * Converts a difference in longitude to a difference in kilometers (rough estimation)
     *
     * @param longitude the longitude (difference)
     * @param latitude  the latitude (absolute)
     * @return the kilometers (difference)
     */
    public static double longitudeToKilometer(double longitude, double latitude) {
        return longitude * LONGITUDE_TO_KILOMETER_AT_ZERO_LATITUDE * Math.cos(Math.toRadians(latitude));
    }

    /**
     * Converts a difference in kilometers to a difference in longitude (rough estimation)
     *
     * @param kilometer the kilometers (difference)
     * @param latitude  the latitude (absolute)
     * @return the longitude (difference)
     */
    public static double kilometerToLongitude(double kilometer, double latitude) {
        return kilometer / longitudeToKilometer(1.0f, latitude);
    }

    /**
     * Converts a difference in longitude to a difference in meters (rough estimation)
     *
     * @param longitude the longitude (difference)
     * @param latitude  the latitude (absolute)
     * @return the meters (difference)
     */
    public static double longitudeToMeter(double longitude, double latitude) {
        return longitudeToKilometer(longitude, latitude) * KILOMETER_TO_METER;
    }

    /**
     * Converts a difference in meters to a difference in longitude (rough estimation)
     *
     * @param meter    the meters (difference)
     * @param latitude the latitude (absolute)
     * @return the longitude (difference)
     */
    public static double meterToLongitude(double meter, double latitude) {
        return meter / longitudeToMeter(1.0f, latitude);
    }

    /**
     * Calculates the difference from the start position to the end position (in meters)
     *
     * @param start the start position
     * @param end   the end position
     * @return the distance in meters
     */
    public static double calculateDistance(Point start, Point end) {
        return calculateDistance(start.latitude, start.longitude, end.latitude, end.longitude);
    }

    /**
     * Calculates the difference from the start position to the end position (in meters)
     *
     * @param startLatitude  the latitude of the start position
     * @param startLongitude the longitude of the start position
     * @param endLatitude    the latitude of the end position
     * @param endLongitude   the longitude of the end position
     * @return the distance in meters
     */
    public static double calculateDistance(double startLatitude, double startLongitude, double endLatitude, double endLongitude) {
        float[] results = new float[3];
        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
        return results[0];
    }

    /**
     * Attaches or detaches a listener that informs about certain events
     *
     * @param listener the `EasyWayLocation.Listener` instance to attach or `null` to detach
     */
    public void setListener(final Listener listener) {
        mListener = listener;
    }

    public boolean locationEnabled() {
        try {
            gps_enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        return !(!gps_enabled && !network_enabled);


    }

    /**
     * Whether the device has location access enabled in the settings
     *
     * @return whether location access is enabled or not
     */
    public boolean hasLocationEnabled() {
        return hasLocationEnabled(getProviderName());
    }

    private boolean hasLocationEnabled(final String providerName) {
        try {
            return mLocationManager.isProviderEnabled(providerName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Starts updating the location and requesting new updates after the defined interval
     */
    @SuppressLint("MissingPermission")
    public void beginUpdates() {
        if (mLocationListener != null) {
            endUpdates();
        }

        if (!mRequireNewLocation) {
            mPosition = getCachedPosition();
        }

        mLocationListener = createLocationListener();
        mLocationManager.requestLocationUpdates(getProviderName(), mInterval, 25, mLocationListener);
    }

    /**
     * Stops the location updates when they aren't needed anymore so that battery can be saved
     */
    @SuppressLint("MissingPermission")
    public void endUpdates() {
        if (mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
            mLocationListener = null;
        }
    }

    /**
     * Blurs the specified location with the defined blur radius or returns an unchanged location if no blur radius is set
     *
     * @param originalLocation the original location received from the device
     * @return the blurred location
     */
    private Location blurWithRadius(final Location originalLocation) {
        if (mBlurRadius <= 0) {
            return originalLocation;
        } else {
            Location newLocation = new Location(originalLocation);

            double blurMeterLong = calculateRandomOffset(mBlurRadius) / SQUARE_ROOT_TWO;
            double blurMeterLat = calculateRandomOffset(mBlurRadius) / SQUARE_ROOT_TWO;

            newLocation.setLongitude(newLocation.getLongitude() + meterToLongitude(blurMeterLong, newLocation.getLatitude()));
            newLocation.setLatitude(newLocation.getLatitude() + meterToLatitude(blurMeterLat));

            return newLocation;
        }
    }

    /**
     * Returns the current position as a Point instance
     *
     * @return the current location (if any) or `null`
     */
    public Point getPosition() {
        if (mPosition == null) {
            return null;
        } else {
            Location position = blurWithRadius(mPosition);
            return new Point(position.getLatitude(), position.getLongitude());
        }
    }

    /**
     * Returns the latitude of the current location
     *
     * @return the current latitude (if any) or `0`
     */
    public double getLatitude() {
        if (mPosition == null) {
            return 0.0f;
        } else {
            Location position = blurWithRadius(mPosition);
            return position.getLatitude();
        }
    }

    /**
     * Returns the longitude of the current location
     *
     * @return the current longitude (if any) or `0`
     */
    public double getLongitude() {
        if (mPosition == null) {
            return 0.0f;
        } else {
            Location position = blurWithRadius(mPosition);
            return position.getLongitude();
        }
    }

    /**
     * Returns the current speed
     *
     * @return the current speed (if detected) or `0`
     */
    public float getSpeed() {
        if (mPosition == null) {
            return 0.0f;
        } else {
            return mPosition.getSpeed();
        }
    }

    /**
     * Returns the current altitude
     *
     * @return the current altitude (if detected) or `0`
     */
    public double getAltitude() {
        if (mPosition == null) {
            return 0.0f;
        } else {
            return mPosition.getAltitude();
        }
    }

    /**
     * Sets the blur radius (in meters) to use for privacy reasons
     *
     * @param blurRadius the blur radius (in meters)
     */
    public void setBlurRadius(final int blurRadius) {
        mBlurRadius = blurRadius;
    }

    /**
     * Creates a new LocationListener instance used internally to listen for location updates
     *
     * @return the new LocationListener instance
     */
    private LocationListener createLocationListener() {
        return new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                mPosition = location;
                cachePosition();

                if (mListener != null) {
                    mListener.onPositionChanged();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }

        };
    }

    /**
     * Returns the name of the location provider that matches the specified settings
     *
     * @return the provider's name
     */
    private String getProviderName() {
        return getProviderName(mRequireFine);
    }

    /**
     * Returns the name of the location provider that matches the specified settings and depends on the given granularity
     *
     * @param requireFine whether to require fine location or use coarse location
     * @return the provider's name
     */
    private String getProviderName(final boolean requireFine) {
        // if fine location (GPS) is required
        if (requireFine) {
            // we just have to decide between active and passive mode

            if (mPassive) {
                return PROVIDER_FINE_PASSIVE;
            } else {
                return PROVIDER_FINE;
            }
        }
        // if both fine location (GPS) and coarse location (network) are acceptable
        else {
            // if we can use coarse location (network)
            if (hasLocationEnabled(PROVIDER_COARSE)) {
                // if we wanted passive mode
                if (mPassive) {
                    // throw an exception because this is not possible
                    throw new RuntimeException("There is no passive provider for the coarse location");
                }
                // if we wanted active mode
                else {
                    // use coarse location (network)
                    return PROVIDER_COARSE;
                }
            }
            // if coarse location (network) is not available
            else {
                // if we can use fine location (GPS)
                if (hasLocationEnabled(PROVIDER_FINE) || hasLocationEnabled(PROVIDER_FINE_PASSIVE)) {
                    // we have to use fine location (GPS) because coarse location (network) was not available
                    return getProviderName(true);
                }
                // no location is available so return the provider with the minimum permission level
                else {
                    return PROVIDER_COARSE;
                }
            }
        }
    }

    /**
     * Returns the last position from the cache
     *
     * @return the cached position
     */
    @SuppressLint("MissingPermission")
    private Location getCachedPosition() {
        if (mCachedPosition != null) {
            return mCachedPosition;
        } else {
            try {
                return mLocationManager.getLastKnownLocation(getProviderName());
            } catch (Exception e) {
                return null;
            }
        }
    }

    /**
     * Caches the current position
     */
    private void cachePosition() {
        if (mPosition != null) {
            mCachedPosition = mPosition;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(context, "failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        LocationRequest mLocationRequest = new LocationRequest();
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result1) {
                Status status = result1.getStatus();
                final LocationSettingsStates states = result1.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        mListener.locationOn();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {

                            status.startResolutionForResult((Activity) context, LOCATION_SETTING_REQUEST_CODE);

                        } catch (IntentSender.SendIntentException e) {

                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        locationReturn = false;
                        break;
                }

            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    public Boolean checkLocation() {
        return locationReturn;
    }



    /**
     * Wrapper for two coordinates (latitude and longitude)
     */
    public static class Point implements Parcelable {

        public static final Creator<Point> CREATOR = new Creator<Point>() {

            @Override
            public Point createFromParcel(Parcel in) {
                return new Point(in);
            }

            @Override
            public Point[] newArray(int size) {
                return new Point[size];
            }

        };
        /**
         * The latitude of the point
         */
        public final double latitude;
        /**
         * The longitude of the point
         */
        public final double longitude;

        /**
         * Constructs a new point from the given coordinates
         *
         * @param lat the latitude
         * @param lon the longitude
         */
        public Point(double lat, double lon) {
            latitude = lat;
            longitude = lon;
        }

        private Point(Parcel in) {
            latitude = in.readDouble();
            longitude = in.readDouble();
        }

        @Override
        public String toString() {
            return "(" + latitude + ", " + longitude + ")";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeDouble(latitude);
            out.writeDouble(longitude);
        }

    }

    public void onActivityResult(int result) {
        if (result == Activity.RESULT_OK) {
            mListener.locationOn();
        } else if (result == Activity.RESULT_CANCELED) {
            mListener.locationCancelled();
        }
    }

    public void showAlertDialog(String title, String message, Drawable drawable){
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        if (drawable != null)
        {
            alertDialog.setIcon(drawable);
        }
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    public static String getAddress(Context context, Double latitude, Double longitude, boolean country, boolean fullAddress) {
        String add = "";
        Geocoder geoCoder = new Geocoder(((Activity) context).getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = geoCoder.getFromLocation(latitude, longitude, 1);

            if (addresses.size() > 0) {
                if (country) {
                    add = addresses.get(0).getCountryName();
                } else if (fullAddress) {
                    add = addresses.get(0).getFeatureName() + "," + addresses.get(0).getSubLocality() + "," + addresses.get(0).getSubAdminArea() + "," + addresses.get(0).getPostalCode() + "," + addresses.get(0).getCountryName();
                } else {
                    add = addresses.get(0).getLocality();
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return add.replaceAll(",null", "");
    }

}
