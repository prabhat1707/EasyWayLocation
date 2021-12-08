package com.example.prabhat.locationsample

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, DirectionUtil.DirectionCallBack {

    companion object{
        val WAY_POINT_TAG = "way_point_tag"
        val ARC_POINT_TAG = "arc_point_tag"
    }

    override fun pathFindFinish(polyLineDetails: HashMap<String, PolyLineDataBean>) {
       for (i in polyLineDetails.keys){
           Log.v("sample", polyLineDetails[i]?.time)
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.423669, -122.090168), 16F))
        wayPoints.add(LatLng(37.423669, -122.090168))
        wayPoints.add(LatLng(37.420930, -122.085362))
                val directionUtil = DirectionUtil.Builder()
                .setDirectionKey("xyz")
                        .setOrigin(LatLng(37.421481, -122.092156))
                        .setWayPoints(wayPoints)
                        .setGoogleMap(mMap)
                        .setPolyLinePrimaryColor(R.color.black)
                        .setPolyLineWidth(5)
                        .setPathAnimation(true)
                        .setCallback(this)
                        .setPolylineTag(WAY_POINT_TAG)
                        .setDestination(LatLng(37.421519, -122.086809))
                        .build()

        directionUtil.drawPath()
        directionUtil.drawArcDirection(LatLng(37.421481, -122.092156),LatLng(37.421519, -122.086809),0.5,ARC_POINT_TAG)
        Handler().postDelayed({ directionUtil.clearPolyline(WAY_POINT_TAG) }, 3000)

        Handler().postDelayed({ directionUtil.clearPolyline(ARC_POINT_TAG) }, 6000)
    }




}
