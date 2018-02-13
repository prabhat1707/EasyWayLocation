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
	        compile 'com.github.prabhat1707:EasyWayLocation:1.0'
	}
	
  
````

# Usage

###### If the device is running Android 6.0 or higher, and your app's target SDK is 23 or highe then first check permission of location then call it.

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
        easyWayLocation = new EasyWayLocation(this);
        easyWayLocation.setListener(this);
    }

    @Override
    public void locationOn() {
        Toast.makeText(this, "Location ON", Toast.LENGTH_SHORT).show();
        easyWayLocation.beginUpdates();
        lati = easyWayLocation.getLatitude();
        longi = easyWayLocation.getLongitude();
       
       //--
    }

    @Override
    public void onPositionChanged() {
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

````

# Location Notifier

````
@Override
    public void locationOn() {
        Toast.makeText(this, "Location ON", Toast.LENGTH_SHORT).show();
        
    }

    @Override
    public void onPositionChanged() {
       // give lat and long at every interval 
    }

    @Override
    public void locationCancelled() {
        // location not on
    }
    
````
# Constructor options

## Decide for the required granularity
- If you want to get the device's location with fine granularity (between 2m and 100m precision), GPS will be required. This consumes more battery but is most precise.
- If you want to get the device's location with coarse granularity only (precise to several hundred meters), the location will be retrieved from the network (Wi-Fi and cell towers). This saves battery but is less precise.

````
Context context = this;
boolean requireFineGranularity = false;
new EasyWayLocation(context, requireFineGranularity);

or

Context context = this;
boolean requireFineGranularity = false;
boolean passiveMode = false;
long updateIntervalInMilliseconds = 10 * 60 * 1000;
new EasyWayLocation(context, requireFineGranularity, passiveMode, updateIntervalInMilliseconds);

or

Context context = this;
boolean requireFineGranularity = false;
boolean passiveMode = false;
new EasyWayLocation(context, requireFineGranularity, passiveMode);

or

Context context = this;
boolean requireFineGranularity = false;
boolean passiveMode = false;
long updateIntervalInMilliseconds = 10 * 60 * 1000;
boolean requireNewLocation = false;
new EasyWayLocation(context, requireFineGranularity, passiveMode, updateIntervalInMilliseconds, requireNewLocation);

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

# License

````
Copyright (c) delight.im <prabhatrai@trenzlr.com>

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
