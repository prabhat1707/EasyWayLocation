package com.example.easywaylocation.draw_path;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DirectionUtil {
    private DirectionCallBack directionCallBack;
    private GoogleMap mMap;
    private String directionKey;
    private ArrayList<LatLng> wayPoints = new ArrayList<>();
    private LatLng origin;
    private LatLng destination;
    private int polyLineWidth =  13;
    private int polyLineColor =  Color.BLUE;
    private boolean isEnd = false;

    private DirectionUtil(Builder builder) {
        this.directionCallBack = builder.directionCallBack;
        this.destination = builder.destination;
        this.origin = builder.origin;
        this.polyLineWidth = builder.polyLineWidth;
        this.polyLineColor = builder.polyLineColor;
        this.wayPoints = builder.wayPoints;
        this.mMap = builder.mMap;
    }

    private DirectionUtil(DirectionCallBack directionCallBack, GoogleMap googleMap, String directionKey, ArrayList<LatLng> wayPoints,
                          LatLng origin, LatLng destination, int polyLineWidth, int polyLineColor){

    }

    public void drawPath() throws Exception {

        if (directionKey.isEmpty()){
            throw new Exception("Direction directionKey is not valid");
        }

        if (origin == null && destination == null){
            throw new Exception("Make sure Origin and destination not null");
        }
        wayPoints.add(0,origin);
        wayPoints.add(wayPoints.size()-1,destination);
        for(int i=1;i<wayPoints.size();i++){
            if (i == wayPoints.size()-1){
                isEnd = true;
            }
            String url = getUrl(wayPoints.get(i-1),wayPoints.get(i));
            new FetchUrl().execute(url);
        }
    }
    /**
     *
     * A method to download json data frpublicom url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            Log.d("downloadUrl", data);
            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;


        // Sensor enabled
        String sensor = "sensor=false";

        //directionKey
        String key = "key="+directionKey;

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }


    public interface DirectionCallBack {
        void pathFindFinish();
    }

    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask", jsonData[0]);
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask", "Executing routes");
                Log.d("ParserTask", routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask", e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(polyLineWidth);
                lineOptions.color(polyLineColor);
                if (isEnd){
                    directionCallBack.pathFindFinish();
                    isEnd = false;
                }
                Log.d("onPostExecute", "onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
            } else {
                Log.d("onPostExecute", "without Polylines drawn");
            }
        }
    }

    public static class Builder{
        private DirectionCallBack directionCallBack;
        private GoogleMap mMap;
        private String key;
        private ArrayList<LatLng> wayPoints = new ArrayList<>();
        private LatLng origin;
        private LatLng destination;
        private int polyLineWidth =  13;
        private int polyLineColor =  Color.BLUE;

        public Builder setCallback(final DirectionCallBack directionCallBack){
            this.directionCallBack = directionCallBack;
            return this;
        }

        public Builder setWayPoints(final ArrayList<LatLng> wayPoints){
            this.wayPoints = wayPoints;
            return this;
        }

        public Builder setOrigin(final LatLng origin){
            this.origin = origin;
            return this;
        }

        public Builder setDestination(final LatLng destination){
            this.destination = destination;
            return this;
        }

        public Builder setDirectionKey(final String key){
            this.key = key;
            return this;
        }

        public Builder setGoogleMap(final GoogleMap map){
            this.mMap = map;
            return this;
        }

        public Builder setPolyLineWidth(int polyLineWidth){
            this.polyLineWidth = polyLineWidth;
            return this;
        }

        public Builder setPolyLineColor(int color){
            this.polyLineColor = color;
            return this;
        }

        public DirectionUtil build(){
            return new DirectionUtil(this);
        }
    }

}
