package com.example.swingcoach.feedback.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize

@Parcelize
data class FeedbackListItem(
    @DrawableRes val iconResourceId: Int,
    @StringRes val stringResourceIdFeedbackText: Int,
    @StringRes val stringResourceIdSwingType: Int,
    val isPerfect: Boolean = false,
) : Parcelable
