package com.example.swingcoach.activity

import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.GRAVITY_EARTH
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.swingcoach.R
import com.example.swingcoach.feedback.model.FeedbackListItem
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class WaitingActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var preferences: SharedPreferences

    private lateinit var sensorManager: SensorManager

    private lateinit var accText: TextView
    private lateinit var accCompsText: TextView

    private var feedbackAfterEachSwing: Boolean = true
    private val feedbacks: ArrayDeque<Int> = ArrayDeque()
    private var enableDebug: Boolean = false
    private lateinit var buttonStop: Button

    private lateinit var graph: GraphView
    private lateinit var viewport: Viewport
    private var pointsStored: Int = 1
    private var measuringSwing: Boolean = false
    private var measuringTime: Int = 0
    private var maxZDeviation: Float = 0F
    private var stasisTime: Int = 0
    private var gravityTolerance: Float = 4.0F
    private var stasisThreshold: Int = 100
    private var swingMinLength: Int = 50
    private var swingMaxLength: Int = 300
    private var swingMinArcHeight: Double = 30.0
    private var graphScrollSpeed: Int = 400

    private lateinit var series: LineGraphSeries<DataPoint>
    private var bufferCapacity: Int = 1000 // how many points to store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)
        supportActionBar?.hide()

        // Change status bar color
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.default_background)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        feedbackAfterEachSwing = preferences.getString("feedback_frequency", "each") == "each"
        enableDebug = preferences.getBoolean("debug", false)
        gravityTolerance = preferences.getString("gravity_tolerance", "4.0")!!.toFloat()
        stasisThreshold = preferences.getString("stasis_threshold", "100")!!.toInt()
        swingMinLength = preferences.getString("swing_min_length", "50")!!.toInt()
        swingMaxLength = preferences.getString("swing_max_length", "300")!!.toInt()
        swingMinArcHeight = preferences.getString("swing_min_arc_height", "30.0")!!.toDouble()
        graphScrollSpeed = preferences.getString("graph_scroll_speed", "400")!!.toInt()

        series = LineGraphSeries()

        accText = findViewById(R.id.debugAcceleration)
        accCompsText = findViewById(R.id.debugAccComps)
        graph = findViewById(R.id.debugGraph)

        if (enableDebug) {
            viewport = graph.viewport
            viewport.isScrollable = true
            viewport.isXAxisBoundsManual = true
            graph.addSeries(series)

            accText.visibility = View.VISIBLE
            accCompsText.visibility = View.VISIBLE
            graph.visibility = View.VISIBLE
        } else {
            accText.visibility = View.INVISIBLE
            accCompsText.visibility = View.INVISIBLE
            graph.visibility = View.INVISIBLE
        }

        buttonStop = findViewById(R.id.buttonStop)
        if (feedbackAfterEachSwing) {
            buttonStop.visibility = View.INVISIBLE
        } else {
            buttonStop.visibility = View.VISIBLE
            buttonStop.setOnClickListener {
                goToFeedback()
            }
        }

        setupSensor()
    }

    private fun setupSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_GAME
                // NB: can change to SENSOR_DELAY_FASTEST, but probably not needed (and needs a permission)
            )
        }
    }

    private fun reCalcMaxZDeviation(z: Float) {
        val dev = abs(GRAVITY_EARTH - z)
        if (dev > maxZDeviation) {
            maxZDeviation = dev
        }
    }

    private fun hasSpike(): Boolean {
        // compute some buffer stats
        val iterator = series.getValues(series.lowestValueX, series.highestValueX)
        var sum = 0.0
        iterator.forEach {
            sum += it.y
        }
        val len = series.highestValueX - series.lowestValueX
        val mean = sum / len
        sum = 0.0
        iterator.forEach {
            sum += (it.y - mean).pow(2)
        }
        val stdDev = sqrt(sum / len)

        // compute (absolute) z-score for each point in buffer
        val zScores = mutableListOf<Double>()
        var maxZScore = 0.0
        iterator.forEach {
            val zScore = abs((it.y - mean) / stdDev)
            zScores.add(zScore)
            if (zScore > maxZScore) {
                maxZScore = zScore
            }
        }
        return maxZScore >= 3
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x: Float = event.values[0]
            val y: Float = event.values[1]
            val z: Float = event.values[2]
            val a: Float = sqrt(x * x + y * y + z * z)

            // Store data (cyclical buffer)
            if (pointsStored > bufferCapacity) {
                pointsStored = 1
                series.resetData(arrayOf(DataPoint(pointsStored.toDouble(), a.toDouble())))
            }
            pointsStored++
            series.appendData(
                DataPoint(pointsStored.toDouble(), a.toDouble()),
                true,
                pointsStored
            )

            // Analyze the data (swing detection)
            if (a < GRAVITY_EARTH - gravityTolerance || a > GRAVITY_EARTH + gravityTolerance) {
                // As soon as we exceed tolerances, start measuring the swing
                if (!measuringSwing) {
                    measuringSwing = true
                    measuringTime = 0
                }
                measuringTime++
                reCalcMaxZDeviation(z)
                // reset stasis when outside tolerance (we're still measuring)
                stasisTime = 0
            } else if (measuringSwing) {
                measuringTime++
                reCalcMaxZDeviation(z)
                // Within tolerance, but we were measuring, start counting stasis (within tolerance behavior)
                // if stasis lasts long enough, we are stopping the measurement
                stasisTime++
                if (stasisTime >= stasisThreshold) {
                    val feedbackId = when {
                        // Longer swing
                        measuringTime < swingMinLength -> 4
                        // Follow through
                        hasSpike() -> 3
                        // Bigger arc
                        maxZDeviation < swingMinArcHeight -> 2 // TODO should be: not enough downward acceleration
                        // Faster
                        measuringTime >= swingMaxLength -> 1
                        // Perfect by default
                        else -> 0
                    }

                    measuringSwing = false
                    measuringTime = 0
                    feedbacks.add(feedbackId)
                    if (feedbackAfterEachSwing) {
                        // Just generate the 1 feedback for this swing only
                        goToFeedback()
                    }
                }
            } else {
                // data within tolerance interval = keep monitoring, because no swing
                stasisTime++
            }

            // Update view/graph
            if (enableDebug) {
                accText.text = getString(R.string.debugAcceleration, a)
                accCompsText.text = getString(R.string.debugAccComps, x, y, z)

                viewport.setMaxX(pointsStored.toDouble())
                viewport.setMinX((pointsStored - graphScrollSpeed).toDouble())
            }
        }
    }

    private fun goToFeedback() {
        // Feedbacks
        val swingFeedbacks = listOf(
            Triple(true, R.string.feedbackPerfect, R.drawable.feedback_icon_perfect),
            Triple(false, R.string.feedbackFaster, R.drawable.feedback_icon_faster),
            Triple(false, R.string.feedbackBiggerArc, R.drawable.feedback_icon_bigger_arc),
            Triple(false, R.string.feedbackFollowThrough, R.drawable.feedback_icon_follow_through),
            Triple(false, R.string.feedbackLongerSwing, R.drawable.feedback_icon_longer_swing),
        )
        // Swing strokes
        val strokes = listOf(
            R.string.swingForehand,
            R.string.swingForehandVolley,
            R.string.swingBackhand,
            R.string.swingBackhandVolley,
            R.string.swingOverheadSmash
        )

        // Construct the feedback intent
        val feedbackMulti = Intent(
            this,
            if (feedbackAfterEachSwing) FeedbackActivity::class.java else FeedbackMultiActivity::class.java
        )
        feedbacks.fold(0) { index, feedbackId ->
            val feedbackItem = FeedbackListItem(
                swingFeedbacks[feedbackId].third,
                swingFeedbacks[feedbackId].second,
                strokes[strokes.indices.random()],
                swingFeedbacks[feedbackId].first
            )
            feedbackMulti.putExtra("feedback_item_${index}", feedbackItem)
            index + 1
        }
        startActivity(feedbackMulti)
        finish() // need to destroy this activity to stop sensing
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        return
    }

    override fun onBackPressed() {
        val back = Intent(this, MainActivity::class.java)
        startActivity(back)
        finish()
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }
}