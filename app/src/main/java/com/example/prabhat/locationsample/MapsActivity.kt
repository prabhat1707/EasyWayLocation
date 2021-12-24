package com.example.prabhat.locationsample

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, DirectionUtil.DirectionCallBack {

    lateinit var polyLineDetails:HashMap<String, PolyLineDataBean>
    lateinit var directionUtil: DirectionUtil

    companion object{
        val WAY_POINT_TAG = "way_point_tag"
        val ARC_POINT_TAG = "arc_point_tag"
        val waypoint1 = LatLng(37.423669, -122.090168)
        val waypoint2 = LatLng(37.420930, -122.085362)
        val origin = LatLng(37.421481, -122.092156)
        val destination = LatLng(37.421519, -122.086809)

        val markerOptionsOrigin = MarkerOptions()
        val markerOptionsDestination = MarkerOptions()

    }

    override fun pathFindFinish(
        polyLineDetails: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        this.polyLineDetails = polyLineDetails
        mMap.clear()
        initAllMarker(polyLineDetailsArray)
        mMap.addMarker(markerOptionsOrigin)
        directionUtil.drawPath(WAY_POINT_TAG)
    }

    private fun initAllMarker(polyLineDetails: ArrayList<PolyLineDataBean>) {
        markerOptionsOrigin.icon(BitmapDescriptorFactory.fromBitmap(getIcon(R.drawable.car_icon)));
        markerOptionsOrigin.rotation(SphericalUtil.computeHeading(origin, waypoint1).toFloat())

        for (data in polyLineDetails){
            data.position?.let {
                val markerOptionswayPoint = MarkerOptions()
                markerOptionswayPoint.position(data.position!!)
                markerOptionswayPoint.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin));
                markerOptionswayPoint.title(data.toJson().toString())
                mMap.addMarker(markerOptionswayPoint)

            }

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
        val markerInfoWindowAdapter = CustomInfoWindowForGoogleMap(this)
        googleMap.setInfoWindowAdapter(markerInfoWindowAdapter)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.423669, -122.090168), 16F))
        wayPoints.add(waypoint1)
        wayPoints.add(waypoint2)
        directionUtil = DirectionUtil.Builder()
            .setDirectionKey("xyz")
            .setOrigin(origin)
            .setWayPoints(wayPoints)
            .setGoogleMap(mMap)
            .setPathAnimation(true)
            .setPolyLineWidth(5)
            .setCallback(this)
            .setDestination(destination)
            .build()

        val bean = PolyLineDataBean().also {
            it.placeSummary = "Origin"
        }
        val bean2 = PolyLineDataBean().also {
            it.placeSummary = "destination"
        }
        markerOptionsOrigin.position(origin)
        markerOptionsOrigin.icon(BitmapDescriptorFactory.fromBitmap(getIcon(R.drawable.map_pin)));
        markerOptionsOrigin.title(bean.toJson().toString())

        val data = directionUtil.getShortestPathDetails(origin, destination)

        markerOptionsDestination.position(destination)
        markerOptionsDestination.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin));
        markerOptionsDestination.title(data.toJson().toString())

        directionUtil.drawArcDirection(origin, destination, 0.5, ARC_POINT_TAG)
        mMap.addMarker(markerOptionsOrigin).tag = "origin"
        mMap.addMarker(markerOptionsDestination)

        button2.setOnClickListener {
            directionUtil.clearPolyline(ARC_POINT_TAG)
            directionUtil.initPath()
            Handler().postDelayed({
                try {
                    directionUtil.clearPolyline(WAY_POINT_TAG)
                    directionUtil.serOrigin(LatLng(37.422404, -122.091699),wayPoints)
                    directionUtil.setPathAnimation(false)
                    directionUtil.initPath()
                }catch (erro:Exception){

                }
            },5000)
            button2.text = "Complete Ride"
        }
    }


    fun getIcon(id:Int):Bitmap{
        val height = 100
        val width = 100
        val bitmapdraw: BitmapDrawable = resources.getDrawable(id) as BitmapDrawable
        val b: Bitmap = bitmapdraw.getBitmap()
        return Bitmap.createScaledBitmap(b, width, height, false)
    }



}
