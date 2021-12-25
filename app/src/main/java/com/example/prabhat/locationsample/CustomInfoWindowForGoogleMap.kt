package com.example.prabhat.locationsample

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject
import java.lang.Exception
import java.text.DecimalFormat

class CustomInfoWindowForGoogleMap(context: Context): GoogleMap.InfoWindowAdapter  {
    private val df: DecimalFormat = DecimalFormat("0.00")

    var mWindow = (context as Activity).layoutInflater.inflate(R.layout.marker_window, null)


    override fun getInfoWindow(p0: Marker?): View {
         render(p0, mWindow)
        return mWindow
    }

    override fun getInfoContents(p0: Marker?): View {
         render(p0, mWindow)
        return mWindow
    }

    private fun render(marker: Marker?, mWindow: View) {

        val title = mWindow.findViewById<TextView>(R.id.textView)
        val time = mWindow.findViewById<TextView>(R.id.textView2)
        val distance = mWindow.findViewById<TextView>(R.id.textView3)
        try {
            val json = JSONObject(marker?.title)
            title.text = json.getString("placeSummary")
            if (json.getString("time").isNotEmpty()){
                time.visibility = View.VISIBLE
                distance.visibility = View.VISIBLE
                time.text = df.format(json.getString("timeFromPrevPoint").toDouble()/60) + " sec"
                distance.text = df.format(json.getString("distanceFromPrevPoint").toDouble()/1609.344)+" mile"
            }else{
                time.visibility = View.GONE
                distance.visibility = View.GONE
            }
        }catch (e:Exception){
            e.printStackTrace()
        }

    }
}