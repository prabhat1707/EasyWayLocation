package com.example.prabhat.locationsample

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, DirectionUtil.DirectionCallBack,
    Listener {

    private var isMarkerRotating: Boolean = false
    lateinit var polyLineDetails:HashMap<String, PolyLineDataBean>
    lateinit var directionUtil: DirectionUtil
    lateinit var easyWayLocation: EasyWayLocation
    var movingCabMarker: Marker? = null
    lateinit var driverCurrentLocation: Location
    var markerList = ArrayList<Marker>()
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
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        this.polyLineDetails = polyLineDetailsMap
       for (marker in markerList){
           marker.remove()
       }
//        animateCamera(LatLng(driverCurrentLocation.latitude,driverCurrentLocation.longitude))
        animateMarker(LatLng(driverCurrentLocation.latitude,driverCurrentLocation.longitude), movingCabMarker, driverCurrentLocation.bearing)
        initAllMarker(polyLineDetailsArray)
        directionUtil.drawPath(WAY_POINT_TAG)
    }

    private fun initAllMarker(polyLineDetails: ArrayList<PolyLineDataBean>) {
        markerList.clear()
//        markerOptionsOrigin.rotation(SphericalUtil.computeHeading(origin, waypoint1).toFloat())
        for (data in polyLineDetails){
            data.position?.let {
                val markerOptionswayPoint = MarkerOptions()
                markerOptionswayPoint.position(data.position!!)
                markerOptionswayPoint.icon(BitmapDescriptorFactory.fromResource(R.drawable.map_pin));
                markerOptionswayPoint.title(data.toJson().toString())
                markerList.add(mMap.addMarker(markerOptionswayPoint))

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

        easyWayLocation = EasyWayLocation(this, false, true, this)
        easyWayLocation.endUpdates()
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
        markerList.add(mMap.addMarker(markerOptionsOrigin))
        markerList.add(mMap.addMarker(markerOptionsDestination))

        end_update.setOnClickListener {
            easyWayLocation.endUpdates()
        }

        button2.setOnClickListener {
            if (permissionIsGranted()) {
                button2.visibility = View.GONE
                end_update.visibility = View.VISIBLE
                directionUtil.clearPolyline(ARC_POINT_TAG)
                easyWayLocation.startLocation()
            } else {
                // Permission not granted, ask for it
                //testLocationRequest.requestPermission(121);
            }
            button2.text = "Complete Ride"
        }
    }

    fun permissionIsGranted(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    fun getIcon(id:Int):Bitmap{
        val height = 100
        val width = 100
        val bitmapdraw: BitmapDrawable = resources.getDrawable(id) as BitmapDrawable
        val b: Bitmap = bitmapdraw.getBitmap()
        return Bitmap.createScaledBitmap(b, width, height, false)
    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location?) {
        try {
            try {
                directionUtil.clearPolyline(WAY_POINT_TAG)
            }catch (e:java.lang.Exception){
                e.printStackTrace()
            }
            location?.let {
                driverCurrentLocation = it
                checkPoint(driverCurrentLocation,wayPoints);
                if (movingCabMarker == null){
                    markerOptionsOrigin
                        .icon(BitmapDescriptorFactory.fromBitmap(getIcon(R.drawable.car_icon)))
                        .anchor(0.5f, 0.5f)
                        .rotation(driverCurrentLocation.bearing)
                        .flat(true)
                        .title("Driver")
                    movingCabMarker = mMap.addMarker(markerOptionsOrigin)
                }
                directionUtil.serOrigin(
                    LatLng(driverCurrentLocation.latitude,driverCurrentLocation.longitude),wayPoints)
                directionUtil.initPath()
            }
        }catch (e:Exception){
            e.printStackTrace()
        }
    }


    fun animateCamera(location: LatLng) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, mMap.cameraPosition.zoom))
    }

    private fun checkPoint(location: Location, wayPoints: ArrayList<LatLng>) {
        val results = FloatArray(1)
        for (points in wayPoints){
            Location.distanceBetween(
                location.latitude,
                location.longitude,
                points.latitude,
                points.longitude,
                results
            )
            val distanceInMeters = results[0]
            if(distanceInMeters <= 50.0){
                wayPoints.remove(points)
            }
            return
        }
    }

    override fun locationCancelled() {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EasyWayLocation.LOCATION_SETTING_REQUEST_CODE) {
            easyWayLocation.onActivityResult(resultCode)
        }
    }

    private fun rotateMarker(marker: Marker, toRotation: Float) {
        if (!isMarkerRotating) {
            val handler = Handler()
            val start: Long = SystemClock.uptimeMillis()
            val startRotation = marker.rotation
            val duration: Long = 1000
            val interpolator: Interpolator = LinearInterpolator()
            handler.post(object : Runnable {
                override fun run() {
                    isMarkerRotating = true
                    val elapsed: Long = SystemClock.uptimeMillis() - start
                    val t: Float = interpolator.getInterpolation(elapsed.toFloat() / duration)
                    val rot = t * toRotation + (1 - t) * startRotation
                    marker.rotation = if (-rot > 180) rot / 2 else rot
                    if (t < 1.0) {
                        // Post again 16ms later.
                        handler.postDelayed(this, 16)
                    } else {
                        isMarkerRotating = false
                    }
                }
            })
        }
    }

    private fun animateMarker(destination: LatLng, marker: Marker?, bearing: Float) {
        if (marker != null) {
            Log.d("check_in_device_9", "----------->" + "markerAnimate" + destination.latitude.toString())

            val startPosition = marker.position

            val startRotation = marker.rotation

            val latLngInterpolator = LatLngInterpolator.LinearFixed()
            val valueAnimator = ValueAnimator.ofFloat(0F, 1F)
            valueAnimator.duration = 1000 // duration 1 second
            valueAnimator.interpolator = LinearInterpolator()
            valueAnimator.addUpdateListener {
                try {
                    val v = it.animatedFraction
                    val newPosition = latLngInterpolator.interpolate(v, startPosition, destination)
                    marker.position = newPosition
                    marker.setAnchor(0.5f, 0.5f)
                    marker.rotation = computeRotation(v, startRotation, bearing)
                    marker.isFlat = true
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Log.d("check_in_device_9", "----------->" + "exsception" + ex.message.toString())

                }
            }

            valueAnimator.start()
        }
    }

    fun computeRotation(fraction: Float, start: Float, end: Float): Float {
        val normalizeEnd = end - start // rotate start to 0
        val normalizedEndAbs = (normalizeEnd + 360) % 360
        val direction: Float
        if (normalizedEndAbs > 180) {
            direction = -1F
        } else {
            direction = 1F
        }
//        val direction: Float = (normalizedEndAbs > 180) ?-1 : 1; // -1 = anticlockwise, 1 = clockwise
        val rotation: Float
        if (direction > 0) {
            rotation = normalizedEndAbs
        } else {
            rotation = normalizedEndAbs - 360
        }

        val result = fraction * rotation + start
        return (result + 360) % 360
    }

    interface LatLngInterpolator {
        fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng

        class LinearFixed : LatLngInterpolator {
            override fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
                val lat = (b.latitude - a.latitude) * fraction + a.latitude
                var lngDelta = b.longitude - a.longitude
                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360
                }
                val lng = lngDelta * fraction + a.longitude
                return LatLng(lat, lng)
            }
        }
    }

}
