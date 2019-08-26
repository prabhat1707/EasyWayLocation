package com.example.easywaylocation;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class GetLocationDetail {
    private static final String BASE_URL = "https://maps.googleapis.com/maps/";
    private static Retrofit retrofit;
    private LocationData.AddressCallBack addressCallBack;
    private Context context;

    public GetLocationDetail(LocationData.AddressCallBack addressCallBack, Context context) {
        this.addressCallBack = addressCallBack;
        this.context = context;
    }

    private static Retrofit getRetrofitInstance() {
        if (retrofit == null) {
            retrofit = new retrofit2.Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public void getAddress(Double latitude, Double longitude, String key) {
        try {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {

                String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                String city = addresses.get(0).getLocality();
                String state = addresses.get(0).getAdminArea();
                String country = addresses.get(0).getCountryName();
                String postalCode = addresses.get(0).getPostalCode();
                String knownName = addresses.get(0).getFeatureName(); // Only if available else return NULL
                LocationData locationData = new LocationData();
                locationData.setCity(city);
                locationData.setFull_address(address);
                locationData.setPincode(postalCode);
                locationData.setCountry(country);
                addressCallBack.locationData(locationData);

            }
        } catch (IOException e) {
            e.printStackTrace();
            getAddressFromApi(latitude, longitude, key);
        }
    }

    private void getAddressFromApi(Double latitude, Double longitude, String key) {
        StringBuilder tempBuilder = new StringBuilder();
        tempBuilder.append(latitude);
        tempBuilder.append(",");
        tempBuilder.append(longitude);
        DataService dataService = getRetrofitInstance().create(DataService.class);
        Call<String> stringCall = dataService.getData(tempBuilder.toString(), true, key);
        if (stringCall.isExecuted()) {
            stringCall.cancel();
        }
        stringCall.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                try {
                    JSONObject jsonObject = new JSONObject(response.body());
                    JSONArray Results = jsonObject.getJSONArray("results");
                    JSONObject zero = Results.getJSONObject(0);
                    JSONArray address_components = zero.getJSONArray("address_components");
                    LocationData locationData = new LocationData();
                    locationData.setFull_address(zero.getString("formatted_address"));
                    for (int i = 0; i < address_components.length(); i++) {
                        JSONObject zero2 = address_components.getJSONObject(i);
                        String long_name = zero2.getString("long_name");
                        JSONArray mtypes = zero2.getJSONArray("types");
                        String Type = mtypes.getString(0);
                        if (TextUtils.isEmpty(long_name) == false || !long_name.equals(null) || long_name.length() > 0 || long_name != "") {
                            if (Type.equalsIgnoreCase("street_number")) {
                                //Address1 = long_name + " ";
                            } else if (Type.equalsIgnoreCase("route")) {
                                //Address1 = Address1 + long_name;
                            } else if (Type.equalsIgnoreCase("sublocality")) {
                                // Address2 = long_name;
                            } else if (Type.equalsIgnoreCase("locality")) {
                                // Address2 = Address2 + long_name + ", ";
                                locationData.setCity(long_name);
                            } else if (Type.equalsIgnoreCase("administrative_area_level_2")) {
                                // County = long_name;

                            } else if (Type.equalsIgnoreCase("administrative_area_level_1")) {
                                // State = long_name;
                            } else if (Type.equalsIgnoreCase("country")) {
                                locationData.setCountry(long_name);
                            } else if (Type.equalsIgnoreCase("postal_code")) {
                                locationData.setPincode(long_name);
                            }
                        }
                    }
                    addressCallBack.locationData(locationData);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Log.v("response", t.toString());
            }
        });
    }


    private interface DataService {
        @GET("api/geocode/json")
        Call<String> getData(@Query("latlng") String latLong, @Query("sensor") boolean sensor, @Query("key") String key);
    }
}
