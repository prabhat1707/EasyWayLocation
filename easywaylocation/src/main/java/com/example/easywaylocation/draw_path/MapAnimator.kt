package com.example.easywaylocation.draw_path

import android.animation.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.IntegerRes
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import java.lang.Exception


internal class MapAnimator {

    private var backgroundPolyline: Polyline? = null

    private var foregroundPolyline: Polyline? = null

    private var optionsForeground: PolylineOptions? = null

    private var firstRunAnimSet: AnimatorSet? = null

    private var secondLoopRunAnimSet: AnimatorSet? = null

    private var backgroundColor: Int? = null
    private var foregroundColor: Int? = null
    private var PERCENT_COMPLETION = 2500
    private var COLOR_FILL_ANIMATION = 1800
    private var FOREGROUND_TIME = 2000
    private var DELAY_TIME = 200


    private var polyLineDetails: HashMap<String, PolyLineDataBean> = HashMap()


    fun setCompletionTime(@IntegerRes time: Int) {
        PERCENT_COMPLETION = time
    }

    fun setColorFillCompletion(@IntegerRes time: Int) {
        COLOR_FILL_ANIMATION = time
    }

    fun setPrimaryLineCompletion(@IntegerRes time: Int) {
        FOREGROUND_TIME = time
    }

    fun setDelayTime(@IntegerRes time: Int) {
        DELAY_TIME = time
    }


    fun setPrimaryLineColor(color: Int) {
        foregroundColor = color
    }

    fun setSecondaryLineColor(color: Int) {
        backgroundColor = color
    }

    fun animateRoute(googleMap: GoogleMap, routes: List<LatLng>, polyLineDataBean: PolyLineDataBean) {
        if (firstRunAnimSet == null) {
            firstRunAnimSet = AnimatorSet()
        } else {
            firstRunAnimSet!!.removeAllListeners()
            firstRunAnimSet!!.end()
            firstRunAnimSet!!.cancel()

            firstRunAnimSet = AnimatorSet()
        }
        if (secondLoopRunAnimSet == null) {
            secondLoopRunAnimSet = AnimatorSet()
        } else {
            secondLoopRunAnimSet!!.removeAllListeners()
            secondLoopRunAnimSet!!.end()
            secondLoopRunAnimSet!!.cancel()

            secondLoopRunAnimSet = AnimatorSet()
        }
        //Reset the polylines
        if (foregroundPolyline != null) foregroundPolyline!!.remove()
        if (backgroundPolyline != null) backgroundPolyline!!.remove()


        val optionsBackground = PolylineOptions().add(routes[0]).color(backgroundColor!!).width(8f)?.geodesic(false)
        backgroundPolyline = googleMap.addPolyline(optionsBackground)


        optionsForeground = PolylineOptions().add(routes[0]).color(foregroundColor!!).width(8f)?.geodesic(false)
        foregroundPolyline = googleMap.addPolyline(optionsForeground)
//        foregroundPolyline?.tag = getTag()
//        polyLineDetails[foregroundPolyline?.tag as String] = polyLineDataBean


        val percentageCompletion = ValueAnimator.ofInt(0, 100)
        percentageCompletion.duration = PERCENT_COMPLETION.toLong()
        percentageCompletion.interpolator = DecelerateInterpolator()
        percentageCompletion.addUpdateListener { animation ->
            val foregroundPoints = backgroundPolyline!!.points

            val percentageValue = animation.animatedValue as Int
            val pointcount = foregroundPoints.size
            val countTobeRemoved = (pointcount * (percentageValue / 100.0f)).toInt()
            val subListTobeRemoved = foregroundPoints.subList(0, countTobeRemoved)
            subListTobeRemoved.clear()

            foregroundPolyline!!.points = foregroundPoints
        }
        percentageCompletion.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                foregroundPolyline!!.color = backgroundColor!!
                foregroundPolyline!!.points = backgroundPolyline!!.points
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })


        val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), backgroundColor, foregroundColor)
        colorAnimation.interpolator = AccelerateInterpolator()
        colorAnimation.duration = COLOR_FILL_ANIMATION.toLong() // milliseconds

        colorAnimation.addUpdateListener { animator -> foregroundPolyline!!.color = animator.animatedValue as Int }

        val foregroundRouteAnimator = ObjectAnimator.ofObject(this, "routeIncreaseForward", RouteEvaluator(), *routes.toTypedArray())
        foregroundRouteAnimator.interpolator = AccelerateDecelerateInterpolator()
        foregroundRouteAnimator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                backgroundPolyline!!.points = foregroundPolyline!!.points
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })
        foregroundRouteAnimator.duration = FOREGROUND_TIME.toLong()
        //        foregroundRouteAnimator.start();

        firstRunAnimSet!!.playSequentially(foregroundRouteAnimator,
                percentageCompletion)
        firstRunAnimSet!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                secondLoopRunAnimSet!!.start()
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })

        secondLoopRunAnimSet!!.playSequentially(colorAnimation,
                percentageCompletion)
        secondLoopRunAnimSet!!.startDelay = DELAY_TIME.toLong()

        secondLoopRunAnimSet!!.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {

            }

            override fun onAnimationEnd(animation: Animator) {
                secondLoopRunAnimSet!!.start()
            }

            override fun onAnimationCancel(animation: Animator) {

            }

            override fun onAnimationRepeat(animation: Animator) {

            }
        })

        firstRunAnimSet!!.start()
    }

    /**
     * This will be invoked by the ObjectAnimator multiple times. Mostly every 16ms.
     */
    fun setRouteIncreaseForward(endLatLng: LatLng) {
        val foregroundPoints = foregroundPolyline!!.points
        foregroundPoints.add(endLatLng)
        foregroundPolyline!!.points = foregroundPoints
    }

    fun getFor():Polyline{
         foregroundPolyline?.let {
             return it;
         }?:run{
             throw Exception("Please initiate polyline before calling this.")
         }
    }

    fun getBck():Polyline{
        backgroundPolyline?.let {
            return it;
        }?:run{
            throw Exception("Please initiate polyline before calling this.")
        }
    }
}

