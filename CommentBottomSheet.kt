package com.sarang.torang.di.comment_di

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import com.sarang.torang.compose.comments.Comments
import com.sarang.torang.compose.comments.EmptyComment
import com.sarang.torang.compose.comments.InputComment
import com.sarang.torang.compose.comments.PreviewInputComment
import com.sarang.torang.data.comments.Comment
import com.sarang.torang.data.comments.User
import com.sarang.torang.data.comments.testComment
import com.sarang.torang.data.comments.testSubComment
import com.sarang.torang.uistate.Comments
import com.sarang.torang.uistate.CommentsUiState
import com.sarang.torang.uistate.isLogin
import com.sarang.torang.viewmodels.CommentViewModel
import com.sarang.torang.compose.bottomsheet.bottomsheetscaffold.TorangCommentBottomSheetScaffold

private val TAG = "__CommentBottomSheet"

@OptIn(ExperimentalMaterial3Api::class)
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = reviewId) {
        if (reviewId != null)
            viewModel.loadComment(reviewId)
    }

    TorangCommentBottomSheetScaffold(
        show = reviewId != null,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        input = {
            if (uiState is CommentsUiState.Success)
                InputComment(
                    uiState = uiState.comments,
                    sendComment = { viewModel.sendComment() },
                    onCommentChange = { viewModel.onCommentChange(it) },
                    onClearReply = { viewModel.onClearReply() },
                    requestFocus = !show || replySingleEvent != null,
                    image = image
                )
        },
        sheetPeekHeight = 400.dp,
        inputHiddenOffset = 200.dp,
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
            //viewModel.onClear()
            viewModel.onHidden()
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PreviewCommentBottomSheet() {
    TorangCommentBottomSheetScaffold(input = { PreviewInputComment() }, sheetContent = {
        PreviewCommentBody()
    }, sheetPeekHeight = 400.dp, inputHiddenOffset = 150.dp) {

    }
}

@Composable
fun CommentBottomSheetBody(
    modifier: Modifier = Modifier,
    uiState: CommentsUiState,
    onScrollTop: () -> Unit,
    onDelete: (Long) -> Unit,
    onUndo: (Long) -> Unit,
    onFavorite: (Long) -> Unit,
    onReply: (Comment) -> Unit,
    onViewMore: (Long) -> Unit,
    onName: (Int) -> Unit,
    onImage: (Int) -> Unit,
    image: @Composable (Modifier, String, Dp?, Dp?, ContentScale?) -> Unit,
) {

    when (uiState) {
        is CommentsUiState.Success -> {
            ConstraintLayout(
                modifier = modifier
                    .padding(bottom = if (uiState.comments.reply != null) 100.dp else 50.dp)
                    .fillMaxSize(),
                constraintSet = commentsBottomSheetConstraintSet()
            ) {
                Text(
                    modifier = Modifier.layoutId("title"),
                    text = "Comments",
                    fontWeight = FontWeight.Bold
                )
                //CommentHelp(Modifier.layoutId("commentHelp"))

                if (uiState.comments.list.isEmpty()) {
                    EmptyComment(Modifier.layoutId("itemCommentList"))
                } else {
                    Comments(
                        modifier = Modifier
                            .layoutId("itemCommentList")
                            .heightIn(min = 350.dp)
                            .fillMaxWidth(),
                        list = uiState.comments.list,
                        movePosition = uiState.comments.movePosition,
                        onPosition = onScrollTop,
                        onDelete = onDelete,
                        onUndo = onUndo,
                        onFavorite = onFavorite,
                        onReply = onReply,
                        myId = uiState.comments.writer?.userId,
                        onViewMore = onViewMore,
                        image = image,
                        onName = onName,
                        onImage = onImage,
                        isLogin = uiState.comments.isLogin
                    )
                }
            }
        }

        CommentsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }

        CommentsUiState.Error -> {}
    }
}

fun commentsBottomSheetConstraintSet(): ConstraintSet {
    return ConstraintSet {
        val title = createRefFor("title")
        val commentHelp = createRefFor("commentHelp")
        val itemCommentList = createRefFor("itemCommentList")
        val inputComment = createRefFor("inputComment")
        val divide = createRefFor("divide")
        val reply = createRefFor("reply")

        constrain(title) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(parent.top)
        }

        constrain(commentHelp) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(title.bottom)
        }

        constrain(itemCommentList) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            top.linkTo(commentHelp.bottom)
            bottom.linkTo(inputComment.top)
            height = Dimension.fillToConstraints
        }

        constrain(inputComment) {
            start.linkTo(parent.start)
            end.linkTo(parent.end)
            bottom.linkTo(parent.bottom)
        }

        constrain(divide) {
            start.linkTo(parent.start, 8.dp)
            end.linkTo(parent.end, 8.dp)
            bottom.linkTo(inputComment.top)
        }

        constrain(reply) {
            bottom.linkTo(divide.top)
        }
    }
}

@Preview
@Composable
fun PreviewCommentBody() {
    CommentBottomSheetBody(/*Preview*/
        onScrollTop = {},
        onDelete = {},
        onUndo = {},
        image = { _, _, _, _, _ -> },
        uiState = CommentsUiState.Success(
            Comments().copy(
                list = arrayListOf(
                    testComment(0),
                    testComment(1),
                    testComment(2),
                    testSubComment(9),
                    testSubComment(10),
                    testSubComment(11),
                    testComment(3),
                    testComment(4),
                    testComment(5),
                    testComment(6),
                    testComment(7),
                    testComment(8),
                ),
                writer = User("", 10, ""),
                reply = testComment()
            )
        ),
        onReply = {},
        onFavorite = {},
        onViewMore = {},
        onName = {},
        onImage = {}

    )
}