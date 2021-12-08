package com.example.easywaylocation.draw_path

import android.graphics.Color
import android.util.Log
import androidx.annotation.ColorRes
import androidx.annotation.IntegerRes
import com.example.easywaylocation.Logger
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class DirectionUtil private constructor(builder: Builder) {
    private var directionCallBack: DirectionCallBack?
    private val allPathPoints:ArrayList<LatLng> = ArrayList()
    private var mMap: GoogleMap?
    private var directionKey: String?
    val TAG = "Draw path -->"
    private var wayPoints = ArrayList<LatLng>()
    private var origin: LatLng?
    private var destination: LatLng?
    private var polyLineWidth = 10
    private var mTag:String
    private var pathAnimation:Boolean
    private var polyLinePrimaryColor:Int = 0
    private var polyLineSecondaryColor = 0
    private var isEnd = false
    private var pathCompletionTime = 2500
    private var pathColorFillAnimationTime = 1800
    private var polyLineDetails:HashMap<String,PolyLineDataBean> = HashMap()
    private var polyLineDataBean = PolyLineDataBean()

    private var primaryLineCompletionTime = 2000

    private var animationDelay = 200
    private var polylineMap:HashMap<String,MapAnimator> = HashMap()

    init {
        this.mTag = builder.mTag
        this.directionCallBack = builder.directionCallBack
        this.destination = builder.destination
        this.directionKey = builder.key
        this.origin = builder.origin
        this.polyLineWidth = builder.polyLineWidth
        builder.polyLinePrimaryColor?.let {
            this.polyLinePrimaryColor =it
        }?:kotlin.run {
            this.polyLinePrimaryColor = Color.BLACK
        }
        builder.polyLineSecondaryColor?.let {
            this.polyLineSecondaryColor =it
        }?:kotlin.run {
            this.polyLineSecondaryColor = Color.LTGRAY
        }
        this.wayPoints = builder.wayPoints
        this.pathAnimation = builder.pathAnimation
        this.mMap = builder.mMap
        this.pathCompletionTime = builder.pathCompletionTime
        this.pathColorFillAnimationTime = builder.pathColorFillAnimationTime
        this.primaryLineCompletionTime = builder.primaryLineCompletionTime
        this.animationDelay = builder.animationDelay
    }

    @Throws(Exception::class)
    fun drawPath() {
        if (directionKey.isNullOrBlank()) {
            throw Exception("Direction directionKey is not valid")
        }
        if (allPathPoints.isNotEmpty()){
            allPathPoints.clear()
        }

        origin?.let { org ->
            destination?.let { des ->
                wayPoints.add(0, org)
                wayPoints.add(wayPoints.size, des)
                GlobalScope.launch(Dispatchers.Main + mainException) {
                    for (i in 1 until wayPoints.size) {
                        if (i == wayPoints.size - 1) {
                            isEnd = true
                        }
                        val url = getUrl(wayPoints[i - 1], wayPoints[i])
                        val data = async(Dispatchers.IO + downloadDataFromUrlException) { downloadUrl(url) }
                        Logger.LogDebug(TAG,data.await())
                        val parseData = async(Dispatchers.IO + parseDataFromUrlException) { doParsingWork(data.await()) }
                        drawData(parseData.await(),i)
                    }
                }

            } ?: kotlin.run {
                throw Exception("Make sure destination not null")
            }

        } ?: kotlin.run {
            throw Exception("Make sure Origin not null")
        }


    }

    val downloadDataFromUrlException = CoroutineExceptionHandler { _, exception ->
        exception.message?.let {
            Logger.LogDebug(TAG, "${it}")
        }
    }

    val parseDataFromUrlException = CoroutineExceptionHandler { _, exception ->
        exception.message?.let {
            Logger.LogDebug(TAG, "${it}")
        }
    }

    val mainException = CoroutineExceptionHandler { _, exception ->
        exception.message?.let {
            Logger.LogDebug(TAG, "${it} Please check your internet Connection.")
        }
    }

    private fun drawData(result: List<List<HashMap<String, String>>>, pathIncrementer: Int) {
        var points: ArrayList<LatLng>
        val lineOptions = PolylineOptions()

        // Traversing through all the routes
        for (i in result.indices) {
            points = ArrayList()
            //lineOptions = PolylineOptions()

            // Fetching i-th route
            val path = result[i]

            // Fetching all the points in i-th route
            for (j in path.indices) {
                val point = path[j]

                val lat = java.lang.Double.parseDouble(point["lat"]!!)
                val lng = java.lang.Double.parseDouble(point["lng"]!!)
                val position = LatLng(lat, lng)

                points.add(position)
                allPathPoints.add(position)
            }

            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points)
            lineOptions.jointType(JointType.ROUND)
            lineOptions.width(polyLineWidth.toFloat())
            lineOptions.clickable(true)
            Logger.LogInfo(TAG,"onPostExecute lineoptions decoded")

        }
        // Drawing polyline in the Google Map for the i-th route
        if (!pathAnimation){
            val polyLine = mMap?.addPolyline(lineOptions)
//            polyLine?.tag = getTag()
        }
        polyLineDetails["path$pathIncrementer"] = polyLineDataBean
        if (isEnd) {
            directionCallBack?.pathFindFinish(polyLineDetails)
            isEnd = false
            if (pathAnimation){
                mMap?.let {
                    val mapAnimator = MapAnimator()
                    mapAnimator.setColorFillCompletion(pathColorFillAnimationTime)
                    mapAnimator.setDelayTime(animationDelay)
                    mapAnimator.setPrimaryLineColor(polyLinePrimaryColor)
                    mapAnimator.setSecondaryLineColor(polyLineSecondaryColor)
                    mapAnimator.setCompletionTime(pathCompletionTime)
                    mapAnimator.setPrimaryLineCompletion(primaryLineCompletionTime)
                    mapAnimator.animateRoute(it, allPathPoints,polyLineDataBean)
                    polylineMap.put(mTag,mapAnimator)
                }
            }
        }
    }

    private fun doParsingWork(jsonData: String): List<List<HashMap<String, String>>> {
        val jObject = JSONObject(jsonData)
        Logger.LogInfo(TAG,jsonData)
        val parser = DataParser()
        Logger.LogInfo(TAG,parser.toString())
        // Starts parsing data
        val routes: List<List<HashMap<String, String>>> = parser.parse(jObject)
        polyLineDataBean = parser.polyLineDataBean
        Logger.LogInfo(TAG,"Executing routes--->")
        Logger.LogDebug(TAG, routes.toString())
        return routes
    }

    /**
     *
     * A method to download json data frpublicom url
     */

    private fun downloadUrl(strUrl: String): String {
        val data: String
        var iStream: InputStream? = null
        val urlConnection: HttpURLConnection?
        val url = URL(strUrl)

        // Creating an http connection to communicate with url
        urlConnection = url.openConnection() as HttpURLConnection

        // Connecting to url
        urlConnection.connect()

        // Reading data from url
        iStream = urlConnection.inputStream

        val br = BufferedReader(InputStreamReader(iStream))

        val sb = StringBuffer()
        val strings = br.readLines()
        for( i in strings){
            sb.append(i)
        }
        data = sb.toString()
        br.close()
        iStream?.close()
        urlConnection.disconnect()
        return data
    }

    private fun getUrl(origin: LatLng, dest: LatLng): String {

        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude

        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude


        // Sensor enabled
        val sensor = "sensor=false"

        //directionKey
        val key = "key=" + directionKey!!

        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$sensor&$key"

        // Output format
        val output = "json"

        // Building the url to the web service


        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters"
    }

     fun drawArcDirection(origin: LatLng, destination: LatLng, radius: Double,tag:String) {
        val allPathPoints:ArrayList<LatLng> = ArrayList()
        val d: Double = SphericalUtil.computeDistanceBetween(origin, destination)
        val h: Double = SphericalUtil.computeHeading(origin, destination)

        //Midpoint position
        val p: LatLng = SphericalUtil.computeOffset(origin, d * 0.5, h)

        val x = (1 - radius * radius) * d * 0.5 / (2 * radius)
        val r = (1 + radius * radius) * d * 0.5 / (2 * radius)
        val c: LatLng = SphericalUtil.computeOffset(p, x, h + 90.0)

        //Calculate heading between circle center and two points
        val h1: Double = SphericalUtil.computeHeading(c, origin)
        val h2: Double = SphericalUtil.computeHeading(c, destination)

        //Calculate positions of points on circle border and add them to polyline options
        val numpoints = 100
        val step = (h2 - h1) / numpoints
        for (i in 0 until numpoints) {
            val pi: LatLng = SphericalUtil.computeOffset(c, r, h1 + i * step)
            allPathPoints.add(pi)
        }

        mMap?.let {
            val mapAnimator = MapAnimator()
            mapAnimator.setColorFillCompletion(pathColorFillAnimationTime)
            mapAnimator.setDelayTime(animationDelay)
            mapAnimator.setPrimaryLineColor(polyLinePrimaryColor)
            mapAnimator.setSecondaryLineColor(polyLineSecondaryColor)
            mapAnimator.setCompletionTime(pathCompletionTime)
            mapAnimator.setPrimaryLineCompletion(primaryLineCompletionTime)
            mapAnimator.animateRoute(it, allPathPoints,polyLineDataBean)
            polylineMap.put(tag,mapAnimator)
        }

    }

    fun clearPolyline(mTag: String){
        if (!polylineMap.containsKey(mTag)){
            throw java.lang.Exception("No Polyline Tag Found")
        }
        polylineMap.get(mTag)?.getBck()?.let {
            polylineMap.get(mTag)?.getFor()?.remove()
            it.remove()
        }?:run{
            throw java.lang.Exception("Please initiate polyline before calling this.")
        }

    }


    interface DirectionCallBack {
        fun pathFindFinish(polyLineDetails: HashMap<String, PolyLineDataBean>)
    }

    class Builder {
        var directionCallBack: DirectionCallBack? = null
            private set
        var mMap: GoogleMap? = null
            private set
        var mTag: String = ""
            private set
        var key: String? = null
            private set
        var wayPoints = ArrayList<LatLng>()
            private set
        var origin: LatLng? = null
            private set
        var destination: LatLng? = null
            private set
        var polyLineWidth = 13
            private set
        var polyLineSecondaryColor:Int? = null
            private set
        var polyLinePrimaryColor:Int? = null
            private set
        var pathAnimation = false
            private set
        var pathCompletionTime = 2500
            private set

        var pathColorFillAnimationTime = 1800
            private set

        var primaryLineCompletionTime = 2000
            private set

        var animationDelay = 200
            private set

        fun setCallback(directionCallBack: DirectionCallBack) = apply { this.directionCallBack = directionCallBack }

        fun setWayPoints(wayPoints: ArrayList<LatLng>): Builder {
            this.wayPoints = wayPoints
            return this
        }

        fun setOrigin(origin: LatLng): Builder {
            this.origin = origin
            return this
        }

        fun setDestination(destination: LatLng): Builder {
            this.destination = destination
            return this
        }

        fun setDirectionKey(key: String): Builder {
            this.key = key
            return this
        }

        fun setGoogleMap(map: GoogleMap): Builder {
            this.mMap = map
            return this
        }

        fun setPolylineTag(mTag: String): Builder {
            this.mTag = mTag
            return this
        }

        fun setPolyLineWidth(polyLineWidth: Int): Builder {
            this.polyLineWidth = polyLineWidth
            return this
        }

        fun setPolyLinePrimaryColor(@ColorRes color: Int): Builder {
            this.polyLinePrimaryColor = color
            return this
        }

        fun setPolyLineSecondaryColor(@ColorRes color: Int): Builder {
            this.polyLineSecondaryColor = color
            return this
        }

        fun setPathAnimation(boolean:Boolean) = apply { this.pathAnimation = boolean }

        fun setCompletionTime(@IntegerRes time: Int) = apply { this.pathCompletionTime = time }

        fun setColorFillCompletion(@IntegerRes time: Int) = apply { this.pathColorFillAnimationTime = time }

        fun setPrimaryLineCompletion(@IntegerRes time: Int) = apply { this.primaryLineCompletionTime = time }

        fun setDelayTime(@IntegerRes time: Int) = apply { this.animationDelay = time }

        fun build(): DirectionUtil {
            return DirectionUtil(this)
        }
    }



}
