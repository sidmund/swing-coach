package com.example.swingcoach.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.swingcoach.R
import com.example.swingcoach.displayNotification
import com.example.swingcoach.feedback.model.FeedbackListItem
import java.lang.Thread.sleep
import java.util.*

class FeedbackActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var preferences: SharedPreferences

    private lateinit var textFeedback: TextView
    private lateinit var iconFeedback: ImageView
    private lateinit var textSwingType: TextView

    private lateinit var feedbackText: String

    private var doNotification: Boolean = true
    private var notificationId: Int = 0

    private var doSpoken: Boolean = false
    private var tts: TextToSpeech? = null

    private var doVibration: Boolean = false

    private var thread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        supportActionBar?.hide()

        // get feedback preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val hideDelay: Long = preferences.getString("feedback_hide_delay", "4")!!.toInt() * 1000L
        doNotification = preferences.getBoolean("doNotificationFeedback", true)
        doSpoken = preferences.getBoolean("doSpokenFeedback", false)
        if (doSpoken) {
            // create TTS object (implicitly calls onInit)
            tts = TextToSpeech(this, this)
        }
        doVibration = preferences.getBoolean("doVibrationFeedback", false)

        textFeedback = findViewById(R.id.textFeedback)
        iconFeedback = findViewById(R.id.feedbackIcon)
        textSwingType = findViewById(R.id.textSwingType)

        // get feedback from intent
        val feedback = intent.getParcelableExtra("feedback_item_0") // should be only 1 here
            ?: FeedbackListItem(
                R.drawable.feedback_icon_none,
                R.string.feedbackNone,
                R.string.swingUnknown
            )

        // set properties based on feedback
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(
            this,
            if (feedback.isPerfect) R.color.feedback_perfect else R.color.feedback_improve
        )
        val layout: ConstraintLayout = findViewById(R.id.layoutFeedback)
        layout.setBackgroundColor(
            resources.getColor(
                if (feedback.isPerfect) R.color.feedback_perfect else R.color.feedback_improve
            )
        )
        feedbackText = getString(feedback.stringResourceIdFeedbackText)
        textFeedback.text = feedbackText
        iconFeedback.setImageResource(feedback.iconResourceId)
        textSwingType.text = getString(feedback.stringResourceIdSwingType)

        // give feedback (spoken happens separately, if enabled and after TTS initialization)
        if (doNotification) {
            displayNotification(
                this,
                getString(R.string.notificationTitle),
                feedbackText,
                notificationId++
            )
        }
        if (doVibration) {
            vibrate()
        }

        layout.setOnClickListener {
            if (thread != null) {
                thread!!.interrupt()
            } else {
                goBackToWaitingActivity()
            }
        }

        // Timeout to automatically go back to waiting activity
        thread = Thread {
            try {
                sleep(hideDelay)
                goBackToWaitingActivity()
            } catch (e: InterruptedException) {
                goBackToWaitingActivity()
            }
        }
        thread!!.start()
    }

    private fun vibrate() {
        // TODO different intensity based on good/bad swing
        // TODO need to test on actual phone
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

    /**
     * Initialize the TTS (only happens if spoken feedback is enabled).
     * If successful, speak the feedback.
     */
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported!")
            } else {
                tts!!.speak(feedbackText, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }
    }

    private fun goBackToWaitingActivity() {
        val back = Intent(this, WaitingActivity::class.java)
        startActivity(back)
        finish()
    }

    override fun onBackPressed() {
        if (thread != null) {
            thread!!.interrupt()
        } else {
            goBackToWaitingActivity()
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
        super.onDestroy()
    }
}