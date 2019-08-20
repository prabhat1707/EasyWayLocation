# Android - EasyWayLocation
This library contain all utils related to google location. like, getting lat or long, Address and Location Setting dialog, many more...

# Images:
![alt Setting IMages1](https://goo.gl/rT7bi7)
![alt Setting IMages2](https://goo.gl/pL2gr4)
![alt Setting IMages3](https://goo.gl/kjrCCW)


# Prerequisites
- Android 16
# Installing
## Step 1:- Add it in your root build.gradle at the end of repositories:
````
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
  
````
## Step 1:- Add the dependency:
````

		dependencies {
	        implementation 'com.github.prabhat1707:EasyWayLocation:1.2'
		}
	
  
````

# Usage

###### If the device is running Android 6.0 or higher, and your app's target SDK is 29 or highe then first check permission of location then call it.

## Add the required permissions
For fine location (GPS location), add the following permission in your AndroidManifest.xml:
````
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

````

For coarse location (network location), add the following permission in your AndroidManifest.xml:
````
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

````

# Retrieve the location from the device
````
public class MainActivity extends AppCompatActivity implements Listener {
    EasyWayLocation easyWayLocation;
    private TextView location, latLong, diff;
    private Double lati, longi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //--
        easyWayLocation = new EasyWayLocation(this, false,this);
    }

    @Override
    public void locationOn() {
        Toast.makeText(this, "Location ON", Toast.LENGTH_SHORT).show();    
    }

   @Override
    public void currentLocation(Location location) {
        StringBuilder data = new StringBuilder();
        data.append(location.getLatitude());
        data.append(" , ");
        data.append(location.getLongitude());
        latLong.setText(data);
        getLocationDetail.getAddress(location.getLatitude(), location.getLongitude(), "xyz");
    }
    
    @Override
    public void locationCancelled() {
         Toast.makeText(this, "Location Cancelled", Toast.LENGTH_SHORT).show();
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
        easyWayLocation.startLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        easyWayLocation.endUpdates();

    }
}

````

# Location Notifier

````
@Override
    public void locationOn() {
        Toast.makeText(this, "Location ON", Toast.LENGTH_SHORT).show();
        
    }

    @Override
    public void  currentLocation(Location location){
       // give lat and long at every interval 
    }

    @Override
    public void locationCancelled() {
        // location not on
    }
    
````
# Constructor options

## Points to Remember
- if you want only last lcation then pass it true and if false then it give you location update as per default location request.
- if you don't pass then it take default location request or you can pass your's one also(see constructor 2nd).

````
Context context = this;
boolean requireFineGranularity = false;
new EasyWayLocation(this, requireLastLocation = false,listner = this);

or

request = new LocationRequest();
request.setInterval(10000);
request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

new EasyWayLocation(this,locationRequest = request ,  requireLastLocation = false,listner = this);


````

## Calculate distance between two points

````

double startLatitude = 59.95;
double startLongitude = 30.3;
double endLatitude = 44.84;
double endLongitude = -0.58;
location.calculateDistance(startLatitude, startLongitude, endLatitude, endLongitude);

// or

Point startPoint = new EasyWayLocation.Point(59.95, 30.3);
Point endPoint = new EasyWayLocation.Point(44.84, -0.58);
location.calculateDistance(startPoint, endPoint);

````

## Update Get Address Detail's of location.
- if you want address from current location then you need to pass key and context.
- why I want key here if android already provide Geocoder because in some cases or in some devices geo coder not work well and                                                          	throws Exception, so in that case i use google geocode api for fetcth address.
- For this you need to implement Callback ,  LocationData.AddressCallBack

````
GetLocationDetail getLocationDetail = new GetLocationDetail(callback = this, context = this);

getLocationDetail.getAddress(location.getLatitude(), location.getLongitude(), key = "xyz");

````

#### Bugs, Feature requests

Found a bug? Something that's missing? Feedback is an important part of improving the project, so please
<a href="https://github.com/prabhat1707/EasyWayLocation/issues">open an issue</a>

# License

````
Copyright (c) delight.im <prabhat.rai1707@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable la
w or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
````
