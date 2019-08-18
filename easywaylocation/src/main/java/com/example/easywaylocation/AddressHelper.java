package com.example.easywaylocation;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by prabhat on 2/10/18.
 */

public class AddressHelper {
    private static final String TAG = AddressHelper.class.getSimpleName();

    public static final int ADMIN_AREA = 0;
    public static final int CITY_NAME = 1;
    public static final int COUNTRY_CODE = 2;
    public static final int COUNTRY_NAME = 3;
    public static final int FEATURE_NAME = 4;
    public static final int FULL_ADDRESS = 5;
    public static final int PHONE_NUMBER = 6;
    public static final int POST_CODE = 7;
    public static final int PREMISES = 8;
    public static final int STREET_NAME = 9;
    public static final int SUB_ADMIN_AREA = 10;
    public static final int SUB_THOROUGHFARE = 11;

    private Context mContext;
    private Geocoder mGeocoder;

    public AddressHelper(Context context, Locale locale) {

        this.mContext = context;
        this.mGeocoder = new Geocoder(mContext, locale);

    }

    public void getAddressList(Location location, RequestCallback.AddressRequestCallback callback) {

        List<Address> addressList = null;

        try {

            addressList = mGeocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(),
                    1); // We only want one address to be returned.

        } catch (IOException e) {
            // Catch network or other IO problems
            callback.onAddressFailedResult(mContext.getString(R.string.deviceLocationUtil_geocoder_not_available));
            return;
        } catch (IllegalArgumentException e) {
            // Catch invalid latitude or longitude values
            callback.onAddressFailedResult(mContext.getString(R.string.deviceLocationUtil_geocoder_invalid_latLong));
            return;
        }

        // Handle case where no address is found
        if (addressList == null || addressList.size() == 0) {
            callback.onAddressFailedResult(mContext.getString(R.string.deviceLocationUtil_geocoder_address_not_found));
        } else {
            // Return the address list
            callback.onAddressSuccessfulResult(addressList);
        }

    }

    /**
     * Returns a String containing the requested address element or null if not found
     *
     * @param elementCode A package-defined int constant representing the specific
     *                    address element to return.
     * @param location    A Location object containing a latitude and longitude.
     * @return String containing the requested address element if found, a reason for
     * failure if necessary or null if address element doesn't exist.
     */
    public String getAddressElement(int elementCode, Location location) {

        List<Address> addressList;
        Address address;
        String elementString = null;

        try {

            addressList = mGeocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(),
                    1); // We only want one address to be returned.

        } catch (IOException e) {
            // Catch network or other IO problems
            return mContext.getString(R.string.deviceLocationUtil_geocoder_not_available);
        } catch (IllegalArgumentException e) {
            // Catch invalid latitude or longitude values
            return mContext.getString(R.string.deviceLocationUtil_geocoder_invalid_latLong);
        }

        // Handle case where no address is found
        if (addressList == null || addressList.size() == 0) {
            return mContext.getString(R.string.deviceLocationUtil_geocoder_address_not_found);
        } else {
            // Create the Address object from the address list
            address = addressList.get(0);
        }

        // Get the specific address element requested by the caller
        switch (elementCode) {

            case ADMIN_AREA:
                elementString = address.getAdminArea();
                break;
            case CITY_NAME:
                elementString = address.getLocality();
                break;
            case COUNTRY_CODE:
                elementString = address.getCountryCode();
                break;
            case COUNTRY_NAME:
                elementString = address.getCountryName();
                break;
            case FEATURE_NAME:
                elementString = address.getFeatureName();
                break;
            case FULL_ADDRESS:
                elementString = address.toString();
                break;
            case PHONE_NUMBER:
                elementString = address.getPhone();
                break;
            case POST_CODE:
                elementString = address.getPostalCode();
                break;
            case PREMISES:
                elementString = address.getPremises();
                break;
            case STREET_NAME:
                elementString = address.getThoroughfare();
                break;
            case SUB_ADMIN_AREA:
                elementString = address.getSubAdminArea();
                break;
            case SUB_THOROUGHFARE:
                elementString = address.getSubThoroughfare();
                break;
            default:
                elementString = mContext.getString(R.string.deviceLocationUtil_geocoder_invalid_element);
                break;
        }

        return elementString;
    }

}
