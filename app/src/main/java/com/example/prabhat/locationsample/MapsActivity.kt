package com.example.prabhat.locationsample

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, DirectionUtil.DirectionCallBack {

    override fun pathFindFinish(polyLineDetails: HashMap<String, PolyLineDataBean>) {
       for (i in polyLineDetails.keys){
           Log.v("sample",polyLineDetails[i]?.time)
       }
    }

    private lateinit var mMap: GoogleMap
    private var wayPoints:ArrayList<LatLng> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.423669, -122.090168),16F))
        wayPoints.add(LatLng(37.423669, -122.090168))
        wayPoints.add(LatLng(37.420930, -122.085362))
                val directionUtil = DirectionUtil.Builder()
                .setDirectionKey("xyz")
                        .setOrigin(LatLng(37.421481, -122.092156))
                        .setWayPoints(wayPoints)
                        .setGoogleMap(mMap)
                        .setPathAnimation(false)
                        .setPolyLinePrimaryColor(Color.WHITE)
                        .setPolyLineWidth(8)
                        .setCallback(this)
                        .setDestination(LatLng(37.421519, -122.086809))
                        .build()

        directionUtil.drawPath()
    }
}
