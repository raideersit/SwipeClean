package com.swipeclean.app.data.local

import androidx.room.TypeConverter
import com.swipeclean.app.domain.model.ReviewDecision

/** Conversores de tipos no primitivos para Room. */
class Converters {

    @TypeConverter
    fun fromReviewDecision(value: ReviewDecision): String = value.name

    @TypeConverter
    fun toReviewDecision(value: String): ReviewDecision = ReviewDecision.valueOf(value)
}
