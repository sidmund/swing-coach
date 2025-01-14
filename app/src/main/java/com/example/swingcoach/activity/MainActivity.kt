package com.example.swingcoach.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.swingcoach.CHANNEL_ID
import com.example.swingcoach.R
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Swing Coach is intended for tennis practice.
 * The app measures acceleration (of your arm) and provides audible feedback.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    private lateinit var switchNotification: SwitchMaterial
    private lateinit var switchSpoken: SwitchMaterial
    private lateinit var switchVibration: SwitchMaterial
    private lateinit var buttonAdvanced: Button
    private lateinit var buttonStart: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()
        createNotificationChannel()

        // Change status bar color
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.default_background)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        switchNotification = findViewById(R.id.switchNotification)
        switchNotification.setOnClickListener { savePreferences() }
        switchSpoken = findViewById(R.id.switchSpoken)
        switchSpoken.setOnClickListener { savePreferences() }
        switchVibration = findViewById(R.id.switchVibration)
        switchVibration.setOnClickListener { savePreferences() }

        buttonAdvanced = findViewById(R.id.buttonAdvanced)
        buttonAdvanced.setOnClickListener {
            savePreferences()

            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        buttonStart = findViewById(R.id.buttonStart)
        buttonStart.setOnClickListener {
            savePreferences()

            val intent = Intent(this, WaitingActivity::class.java)
            startActivity(intent)
        }

        loadPreferences()
    }

    private fun loadPreferences() {
        switchNotification.isChecked = preferences.getBoolean("doNotificationFeedback", true)
        switchSpoken.isChecked = preferences.getBoolean("doSpokenFeedback", false)
        switchVibration.isChecked = preferences.getBoolean("doVibrationFeedback", false)
    }

    private fun savePreferences() {
        val editor = preferences.edit()
        editor.putBoolean("doNotificationFeedback", switchNotification.isChecked)
        editor.putBoolean("doSpokenFeedback", switchSpoken.isChecked)
        editor.putBoolean("doVibrationFeedback", switchVibration.isChecked)
        editor.apply()
    }

    /**
     * Copied from https://developer.android.com/develop/ui/views/notifications/build-notification
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
