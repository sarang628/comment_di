package com.sarang.torang.di.comment_di

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.sarang.torang.compose.comments.ReplyComment
import com.sarang.torang.data.comments.Comment
import com.sarang.torang.data.comments.User
import com.sarang.torang.data.comments.testComment
import com.sarang.torang.data.comments.testSubComment
import com.sarang.torang.uistate.CommentsUiState
import com.sarang.torang.uistate.isLogin
import com.sarang.torang.uistate.isUploading
import com.sarang.torang.viewmodels.CommentViewModel
import com.sryang.torang.compose.bottomsheet.bottomsheetscaffold.TorangCommentBottomSheetScaffold


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    viewModel: CommentViewModel = hiltViewModel(),
    reviewId: Int? = null,
    onDismissRequest: () -> Unit,
    show: Boolean = false,
    onHidden: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
    image: @Composable (Modifier, String, Dp?, Dp?, ContentScale?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val replySingleEvent by viewModel.replySingleEvent.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = show) {
        if (show)
            viewModel.loadComment(reviewId)
    }

    LaunchedEffect(key1 = uiState.snackBarMessage, block = {
        uiState.snackBarMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearErrorMessage()
        }
    })

    TorangCommentBottomSheetScaffold(
        show = show,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        input = {
            InputCommentForSticky(
                uiState = uiState,
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
                    onCommentChange = { viewModel.onCommentChange(it) },
                    onScrollTop = { viewModel.onPosition() },
                    sendComment = { viewModel.sendComment() },
                    onFavorite = { viewModel.onFavorite(it) },
                    onReply = { viewModel.onReply(it) },
                    onClearReply = { viewModel.onClearReply() },
                    onViewMore = { viewModel.onViewMore(it) },
                    image = image
                )
            }
        },
        content = content,
        onHidden = {
            onHidden?.invoke()
            onDismissRequest.invoke()
            viewModel.onClear()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun PreviewCommentBottomSheet() {
    TorangCommentBottomSheetScaffold(input = { PreviewInputCommentSticky() }, sheetContent = {
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
    sendComment: () -> Unit,
    onCommentChange: (String) -> Unit,
    onFavorite: ((Long) -> Unit)? = null,
    onReply: ((Comment) -> Unit)? = null,
    onClearReply: (() -> Unit)? = null,
    onViewMore: ((Long) -> Unit)? = null,
    image: @Composable (Modifier, String, Dp?, Dp?, ContentScale?) -> Unit
) {
    ConstraintLayout(
        modifier = modifier
            .padding(bottom = if (uiState.reply != null) 100.dp else 50.dp)
            .fillMaxSize(),
        constraintSet = commentsBottomSheetConstraintSet()
    ) {
        Text(
            modifier = Modifier.layoutId("title"),
            text = "Comments",
            fontWeight = FontWeight.Bold
        )
        //CommentHelp(Modifier.layoutId("commentHelp"))

        if (uiState.list.isEmpty()) {
            EmptyComment(Modifier.layoutId("itemCommentList"))
        } else {
            Comments(
                modifier = Modifier
                    .layoutId("itemCommentList")
                    .heightIn(min = 350.dp)
                    .fillMaxWidth(),
                list = uiState.list,
                movePosition = uiState.movePosition,
                onPosition = onScrollTop,
                onDelete = onDelete,
                onUndo = onUndo,
                onFavorite = onFavorite,
                onReply = onReply,
                myId = uiState.writer?.userId,
                onViewMore = onViewMore,
                image = image
            )
        }
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

@Composable
fun InputCommentForSticky(
    uiState: CommentsUiState,
    sendComment: () -> Unit,
    onCommentChange: (String) -> Unit,
    onClearReply: (() -> Unit)?,
    requestFocus: Boolean = false,
    image: @Composable (Modifier, String, Dp?, Dp?, ContentScale?) -> Unit
) {
    Column {
        if (uiState.reply != null)
            ReplyComment(
                profileImageUrl = uiState.reply!!.profileImageUrl,
                uiState.reply!!.name,
                onClearReply,
                image = image
            )

        if (uiState.isLogin)
            HorizontalDivider(
                modifier = Modifier.layoutId("divide"),
                color = Color.LightGray
            )

        if (uiState.isLogin)
            InputComment(
                modifier = Modifier.layoutId("inputComment"),
                profileImageUrl = uiState.writer?.profileUrl ?: "",
                onSend = { sendComment() },
                name = uiState.writer?.userName ?: "",
                input = uiState.comment,
                onValueChange = { onCommentChange(it) },
                replyName = uiState.reply?.name,
                isUploading = uiState.isUploading,
                requestFocus = requestFocus,
                image = image
            )
    }
}

@Preview
@Composable
fun PreviewCommentBody() {
    CommentBottomSheetBody(/*Preview*/
        onScrollTop = {},
        onCommentChange = {},
        onDelete = {},
        onUndo = {},
        sendComment = {},
        image = { _, _, _, _, _ -> },
        uiState = CommentsUiState().copy(
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
    )
}

fun provideCommentBottomDialogSheet(
    show: Boolean,
    onHidden: (() -> Unit)? = null,
): @Composable (reviewId: Int?) -> Unit =
    { reviewId ->
        CommentBottomSheet(
            reviewId = reviewId,
            onDismissRequest = { },
            show = show,
            onHidden = onHidden,
            content = { }
        )
    }

@Preview
@Composable
fun PreviewInputCommentSticky() {
    InputCommentForSticky(uiState = CommentsUiState().copy(
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
    ), sendComment = { /*TODO*/ },
        onClearReply = {}, onCommentChange = {}, image = { _, _, _, _, _ -> })
}