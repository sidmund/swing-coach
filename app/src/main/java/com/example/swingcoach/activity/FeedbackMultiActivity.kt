package com.example.swingcoach.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.swingcoach.R
import com.example.swingcoach.feedback.model.FeedbackListItem
import com.example.swingcoach.feedback.adapter.ItemAdapter

class FeedbackMultiActivity : AppCompatActivity() {

    private lateinit var textSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_multi)

        // Change status bar color
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.default_background)

        var nrPerfect = 0
        val dataset = mutableListOf<FeedbackListItem>()
        var fid = 0
        while (true) {
            val feedback = intent.getParcelableExtra<FeedbackListItem>("feedback_item_${fid++}")
                ?: break
            dataset.add(feedback)
            if (feedback.isPerfect) {
                nrPerfect++
            }
        }

        textSummary = findViewById(R.id.text_summary)
        textSummary.text =
            getString(R.string.feedbackSummary, nrPerfect, dataset.size, dataset.size - nrPerfect)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = ItemAdapter(this, dataset)
        recyclerView.setHasFixedSize(true) // improve performance (if layout size won't change)
    }
}
