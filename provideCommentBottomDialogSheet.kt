package com.sarang.torang.di.comment_di

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sarang.torang.RootNavController
import com.sarang.torang.di.image.provideTorangAsyncImage

private val tag : String = "__CommentBottomDialogSheetData"
data class CommentBottomDialogSheetData(
    val reviewId    : Int?                                  = 0,
    val onHidden    : () -> Unit                            = {},
    val content     : @Composable (PaddingValues) -> Unit   = { Log.i(tag, "content does not set") }
)

fun provideCommentBottomDialogSheet(
    rootNavController: RootNavController,
): @Composable (commentBottomDialogSheetData : CommentBottomDialogSheetData) -> Unit =
    {
        var currentReviewId: Int? by remember { mutableStateOf(null) }
        var show by remember { mutableStateOf(false) }

        if (currentReviewId != it.reviewId) {
            currentReviewId = it.reviewId;
            show = true
        }

        CommentBottomSheet(
            reviewId            = it.reviewId,
            onDismissRequest    = it.onHidden,
            onHidden            = { it.onHidden.invoke(); show = false },
            show                = show,
            content             = it.content,
            image               = provideTorangAsyncImage(),
            onImage             = { rootNavController.profile(it) },
            onName              = { rootNavController.profile(it) }
        )
    }