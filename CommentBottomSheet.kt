package com.sarang.torang.di.comment_di

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sarang.torang.compose.bottomsheet.bottomsheetscaffold.InputInteractBottomSheetScaffold
import com.sarang.torang.compose.comments.InputComment
import com.sarang.torang.compose.comments.PreviewInputComment
import com.sarang.torang.uistate.CommentsUiState
import com.sarang.torang.viewmodels.CommentViewModel

@Composable
fun CommentBottomSheet(
    viewModel: CommentViewModel = hiltViewModel(),
    reviewId: Int? = null,
    onDismissRequest: () -> Unit,
    show: Boolean = false,
    onHidden: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
    onName: (Int) -> Unit,
    onImage: (Int) -> Unit,
    image: @Composable (Modifier, String, Dp?, Dp?, ContentScale?) -> Unit,
) {
    val uiState = viewModel.uiState
    val replySingleEvent by viewModel.replySingleEvent.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = reviewId) {
        if (reviewId != null)
            viewModel.loadComment(reviewId)
    }

    InputInteractBottomSheetScaffold( // torang bottom sheet library components
        show = show,
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
        input = {
            if (uiState is CommentsUiState.Success)
                InputComment(
                    comments = uiState.comments,
                    sendComment = { viewModel.sendComment() },
                    onCommentChange = { viewModel.onCommentChange(it) },
                    onClearReply = { viewModel.onClearReply() },
                    requestFocus = !show || replySingleEvent != null,
                    image = image
                )
        },
        sheetPeekHeightPercent = 55,
        criterionHeight = 50.dp,
        sheetContent = {
            Column {
                CommentBottomSheetBody(
                    modifier = Modifier,
                    uiState = uiState,
                    onUndo = { viewModel.onUndo(it) },
                    onDelete = { viewModel.onDelete(it) },
                    onScrollTop = { viewModel.onPosition() },
                    onFavorite = { viewModel.onFavorite(it) },
                    onReply = { viewModel.onReply(it) },
                    onViewMore = { viewModel.onViewMore(it) },
                    image = image,
                    onName = onName,
                    onImage = onImage
                )
            }
        },
        content = content,
        onHidden = {
            onHidden?.invoke()
            onDismissRequest.invoke()
            viewModel.onHidden()
        }
    )
}


@Preview
@Composable
fun PreviewCommentBottomSheet() {
    InputInteractBottomSheetScaffold(input = { PreviewInputComment() }, sheetContent = {
        PreviewCommentBottomSheetBody()
    }, sheetPeekHeightPercent = 65, criterionHeight = 150.dp) {

    }
}