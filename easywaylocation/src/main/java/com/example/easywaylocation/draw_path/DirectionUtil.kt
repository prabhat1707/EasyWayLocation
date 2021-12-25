package com.example.easywaylocation.draw_path

import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.annotation.IntegerRes
import com.example.easywaylocation.Logger
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
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
    private val TAG = "Draw path -->"
    private var wayPoints = ArrayList<LatLng>()
    private var origin: LatLng?
    private var destination: LatLng?
    private var polyLineWidth = 10
    private var pathAnimation:Boolean
    private var polyLinePrimaryColor:Int = 0
    private var polyLineSecondaryColor = 0
    private var isEnd = false
    private var pathCompletionTime = 2500
    private var pathColorFillAnimationTime = 1800
    private var polyLineDetails:HashMap<String,PolyLineDataBean> = HashMap()
    private var polyLineDetailsArray:ArrayList<PolyLineDataBean> = ArrayList()
    private var polyLineDataBean = PolyLineDataBean()
    private var arrayOfPoints: ArrayList<ArrayList<LatLng>> = ArrayList()
    private var primaryLineCompletionTime = 2000

    private var animationDelay = 200
    private var polylineMap:HashMap<String,ArrayList<PolylineBean>> = HashMap()

    init {
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

    fun setPathAnimation(boolean:Boolean) = apply { this.pathAnimation = boolean }

    fun setCompletionTime(@IntegerRes time: Int) = apply { this.pathCompletionTime = time }

    fun setColorFillCompletion(@IntegerRes time: Int) = apply { this.pathColorFillAnimationTime = time }

    fun setPrimaryLineCompletion(@IntegerRes time: Int) = apply { this.primaryLineCompletionTime = time }

    fun setDelayTime(@IntegerRes time: Int) = apply { this.animationDelay = time }

    fun setPolyLineWidth(polyLineWidth: Int) {
        this.polyLineWidth = polyLineWidth
    }

    fun setPolyLinePrimaryColor(@ColorRes color: Int) {
        this.polyLinePrimaryColor = color
    }

    fun getPolylineMap():HashMap<String,ArrayList<PolylineBean>>{
        return polylineMap
    }

    fun setPolyLineSecondaryColor(@ColorRes color: Int) {
        this.polyLineSecondaryColor = color
    }

    @Throws(Exception::class)
    fun initPath() {
        if (directionKey.isNullOrBlank()) {
            throw Exception("Direction directionKey is not valid")
        }
        if (allPathPoints.isNotEmpty()){
            allPathPoints.clear()
            polyLineDetailsArray.clear()
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
                        val data = downlaodDataFromUrl(wayPoints[i - 1],wayPoints[i],i)
                        Logger.LogDebug(TAG,data.await())
                        drawData(parseDownloadedData(data.await(),i).await(),i)
                    }
                }

            } ?: kotlin.run {
                throw Exception("Make sure destination not null")
            }

        } ?: kotlin.run {
            throw Exception("Make sure Origin not null")
        }
    }

    private fun downlaodDataFromUrl(o:LatLng,d:LatLng,i:Int):Deferred<String> = runBlocking {
        val url = getUrl(o, d)
        Logger.LogInfo("$TAG url $i", url)
        async(Dispatchers.IO + downloadDataFromUrlException) { downloadUrl(url) }
    }

    private fun parseDownloadedData(data:String,i:Int):Deferred<List<List<HashMap<String, String>>> > = runBlocking {
        async(Dispatchers.IO + parseDataFromUrlException) { doParsingWork(data,wayPoints[i]) }
    }

    fun getShortestPathDetails(origin:LatLng, destination: LatLng):PolyLineDataBean = runBlocking{
        val data = downlaodDataFromUrl(origin,destination,0)
        val jObject =  JSONObject(data.await())
        Logger.LogInfo(TAG,jObject.toString())
        val parser = DataParser()
        parser.parse(jObject)
        parser.polyLineDataBean.apply {
            this.position = destination
            this.distanceFromPrevPoint = this.distance
            this.timeFromPrevPoint = this.time
        }
    }

    private val downloadDataFromUrlException = CoroutineExceptionHandler { _, exception ->
        exception.message?.let {
            Logger.LogDebug(TAG, "${it}")
        }
    }

    private val parseDataFromUrlException = CoroutineExceptionHandler { _, exception ->
        exception.message?.let {
            Logger.LogDebug(TAG, "${it}")
        }
    }

    private val mainException = CoroutineExceptionHandler { _, exception ->
        exception.message?.let {
            Logger.LogDebug(TAG, "${it} Please check your internet Connection.")
        }
    }

    fun serOrigin(latLng: LatLng, wayPoint: ArrayList<LatLng>){
        this.origin = latLng
        this.wayPoints.clear()
        this.wayPoints = ArrayList(wayPoint)
    }

    private fun drawData(result: List<List<HashMap<String, String>>>, pathIncrementer: Int) {
        arrayOfPoints.clear();
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
                arrayOfPoints.add(allPathPoints)

            }
        }
        polyLineDetails["path$pathIncrementer"] = polyLineDataBean
        polyLineDetailsArray.add(polyLineDataBean)
        if (isEnd) {
            updatePathDetails(polyLineDetails)
            directionCallBack?.pathFindFinish(polyLineDetails,polyLineDetailsArray)
            isEnd = false
        }
    }

    private fun updatePathDetails(polyLineDetails: java.util.HashMap<String, PolyLineDataBean>) {
        val size = polyLineDetails.size
        if (size == 1) return
        var pathIncrementer = 1;
        while (pathIncrementer <= size){
            polyLineDetails["path${pathIncrementer}"]?.timeFromPrevPoint = ((polyLineDetails["path${pathIncrementer-1}"]?.timeFromPrevPoint?.toDouble() ?: 0.0) + (polyLineDetails["path${pathIncrementer}"]?.time?.toDouble()
                ?: 0.0)).toString()
            polyLineDetails["path${pathIncrementer}"]?.distanceFromPrevPoint = ((polyLineDetails["path${pathIncrementer-1}"]?.distanceFromPrevPoint?.toDouble() ?: 0.0) + (polyLineDetails["path${pathIncrementer}"]?.distance?.toDouble()
                ?: 0.0)).toString()
            pathIncrementer++;
        }
    }

    fun drawPath(mTag:String){
        val polylineArray = ArrayList<PolylineBean>()
        if (!pathAnimation){
            for(array in arrayOfPoints){
                val lineOptions = PolylineOptions()
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(array)
                lineOptions.jointType(JointType.ROUND)
                lineOptions.width(polyLineWidth.toFloat())
                lineOptions.clickable(true)
                polylineArray.add(PolylineBean(mMap?.addPolyline(lineOptions),null))
                Logger.LogInfo(TAG,"onPostExecute lineoptions decoded")
            }
            polylineMap.put(mTag,polylineArray)

        }else{
            mMap?.let {
                val mapAnimator = MapAnimator()
                mapAnimator.setColorFillCompletion(pathColorFillAnimationTime)
                mapAnimator.setDelayTime(animationDelay)
                mapAnimator.setPrimaryLineColor(polyLinePrimaryColor)
                mapAnimator.setSecondaryLineColor(polyLineSecondaryColor)
                mapAnimator.setCompletionTime(pathCompletionTime)
                mapAnimator.setPrimaryLineCompletion(primaryLineCompletionTime)
                mapAnimator.animateRoute(it, allPathPoints,polyLineDataBean)
                polylineMap.put(mTag,mapAnimator.getPolyline())
            }
        }

    }

    private fun doParsingWork(jsonData: String, latLng: LatLng): List<List<HashMap<String, String>>> {
        val jObject = JSONObject(jsonData)
        Logger.LogInfo(TAG,jsonData)
        val parser = DataParser()
        Logger.LogInfo(TAG,parser.toString())
        // Starts parsing data
        val routes: List<List<HashMap<String, String>>> = parser.parse(jObject)
        polyLineDataBean = parser.polyLineDataBean.apply {
            this.position = latLng
        }
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
         GlobalScope.launch(Dispatchers.IO) {
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
             withContext(Dispatchers.Main){
                 val polylineArray = ArrayList<PolylineBean>()
                 if (!pathAnimation){
                     val lineOptions = PolylineOptions()
                     lineOptions.addAll(allPathPoints)
                     lineOptions.jointType(JointType.ROUND)
                     lineOptions.width(polyLineWidth.toFloat())
                     lineOptions.clickable(true)
                     val polyLine = mMap?.addPolyline(lineOptions)
                     polylineArray.add(PolylineBean(polyLine,null))
                     polylineMap.put(tag,polylineArray)
                     this.cancel()
                     return@withContext
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
                     polylineMap.put(tag,mapAnimator.getPolyline())
                 }
             }
         }

    }

    fun clearPolyline(mTag: String){
        if (!polylineMap.containsKey(mTag)){
            throw java.lang.Exception("No Polyline Tag Found")
        }
        polylineMap.get(mTag)?.let {
            for (polyline in it){
                polyline.foreground?.remove()
                if (pathAnimation){
                    polyline.backPolyline?.remove()
                }
            }
        }?:run{
            throw java.lang.Exception("Please initiate polyline before calling this.")
        }

    }

    interface DirectionCallBack {
        fun pathFindFinish(
            polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
            polyLineDetailsArray: ArrayList<PolyLineDataBean>
        )
    }

    class Builder {
        var directionCallBack: DirectionCallBack? = null
            private set
        var mMap: GoogleMap? = null
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
            this.wayPoints = ArrayList(wayPoints)
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

        fun setDebuggable():Builder{
            Logger.isDebuggable = true
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
