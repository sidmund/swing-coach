package com.example.swingcoach.feedback.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.swingcoach.R
import com.example.swingcoach.feedback.model.FeedbackListItem

/**
 * Adapter for the [RecyclerView] in [FeedbackMultiActivity].
 * Displays [FeedbackListItem] data object.
 */
class ItemAdapter(
    private val context: Context,
    private val dataset: List<FeedbackListItem>
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    /*
    Provide a reference to the views for each data item.
    Complex data items may need more than one view per item,
    and you provide access to all the views for a data item in a view holder.
     */
    class ItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        //        val layout: ConstraintLayout = view.findViewById(R.id.layout_list_item)
        val imgIcon: ImageView = view.findViewById(R.id.item_image)
        val textFeedback: TextView = view.findViewById(R.id.item_text_feedback)
        val textSwing: TextView = view.findViewById(R.id.item_text_swing)
    }

    /**
     * Create new views (invoked by the layout manager).
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        // Create a new view
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)

        return ItemViewHolder(adapterLayout)
    }

    /**
     * Replace the contents of a view (invoked by the layout manager).
     */
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]

        holder.imgIcon.setImageResource(item.iconResourceId)
        holder.textFeedback.text = context.resources.getString(item.stringResourceIdFeedbackText)
        holder.textFeedback.setBackgroundColor(
            context.resources.getColor(
                if (item.isPerfect) R.color.feedback_perfect else R.color.feedback_improve
            )
        )
        holder.textSwing.text = context.resources.getString(item.stringResourceIdSwingType)
    }

    /**
     * Return the size of the dataset (invoked by the layout manager).
     */
    override fun getItemCount() = dataset.size

}
