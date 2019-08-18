package com.example.easywaylocation;

import android.location.Address;
import android.location.Location;

import java.util.List;

/**
 * Created by prabhat on 2/10/18.
 */

public class RequestCallback {

    public interface LocationRequestCallback {
        void onLocationResult(Location location);

        void onFailedRequest(String result);
    }

    public interface PermissionRequestCallback {
        void onRationaleDialogOkPressed(int requestCode);
    }

    public interface AddressRequestCallback {
        void onAddressSuccessfulResult(List<Address> addressList);

        void onAddressFailedResult(String result);
    }


}
