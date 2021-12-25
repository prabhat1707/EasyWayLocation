package com.example.easywaylocation.draw_path

import com.google.android.gms.maps.model.LatLng
import org.json.JSONObject

class PolyLineDataBean(){
    var time:String = "0.0" // in second
    var timeFromPrevPoint:String = "0.0" // in second
    var distance:String = "0.0" // in meter
    var distanceFromPrevPoint:String = "0.0" // in meter
    var placeSummary:String = ""
    var position:LatLng? = null

    fun toJson(): JSONObject {
        return JSONObject().put("placeSummary",placeSummary)
            .put("time",time)
            .put("distance",distance)
            .put("distanceFromPrevPoint",distanceFromPrevPoint)
            .put("timeFromPrevPoint",timeFromPrevPoint)
    }


}