package com.sarang.torang.di.comment_di

import com.sarang.torang.BuildConfig
import com.sarang.torang.api.ApiComment
import com.sarang.torang.api.ApiCommentLike
import com.sarang.torang.compose.comments.IsLoginFlowForCommentUseCase
import com.sarang.torang.core.database.dao.LoggedInUserDao
import com.sarang.torang.data.comments.Comment
import com.sarang.torang.data.comments.User
import com.sarang.torang.data.remote.response.RemoteComment
import com.sarang.torang.di.repository.from
import com.sarang.torang.repository.LoginRepository
import com.sarang.torang.repository.comment.CommentRepository
import com.sarang.torang.session.SessionClientService
import com.sarang.torang.usecase.comments.AddCommentLikeUseCase
import com.sarang.torang.usecase.comments.DeleteCommentLikeUseCase
import com.sarang.torang.usecase.comments.DeleteCommentUseCase
import com.sarang.torang.usecase.comments.GetCommentsUseCase
import com.sarang.torang.usecase.comments.GetUserUseCase
import com.sarang.torang.usecase.comments.LoadCommentsUseCase
import com.sarang.torang.usecase.comments.LoadMoreUseCase
import com.sarang.torang.usecase.comments.SendCommentUseCase
import com.sarang.torang.usecase.comments.SendReplyUseCase
import com.sarang.torang.util.DateConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class CommentModule {
    @Provides
    fun providesGetCommentsUseCase(
        commentRepository: CommentRepository
    ): GetCommentsUseCase {
        return object : GetCommentsUseCase {
            override suspend fun invoke(reviewId: Int): Flow<List<Comment>> {
                return commentRepository.getCommentsFlow(reviewId).map {
                    it.map { com.sarang.torang.data.comments.Comment(
                        commentsId = it.commentId.toLong(),
                        userId = it.userId,
                        profileImageUrl = it.profilePicUrl,
                        date = it.createDate,
                        comment = it.comment,
                        name = it.userName,
                        commentLikeCount = it.commentLikeCount
                    ) }
                }
            }
        }
    }

    @Provides
    fun providesLoadCommentsUseCase(
        commentRepository: CommentRepository
    ): LoadCommentsUseCase {
        return object : LoadCommentsUseCase {
            override suspend fun invoke(reviewId: Int) {
                commentRepository.clear()
                commentRepository.getCommentsWithOneReply(reviewId)
            }
        }
    }

    @Provides
    fun providesGetUserUseCase(
        loggedInUserDao: LoggedInUserDao
    ): GetUserUseCase {
        return object : GetUserUseCase {
            override suspend fun invoke(): Flow<User?> {
                return loggedInUserDao.getLoggedInUserFlow().map {
                    if (it != null) {
                        User(
                            BuildConfig.PROFILE_IMAGE_SERVER_URL + it.profilePicUrl,
                            userId = it.userId,
                            userName = it.userName
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }

    @Provides
    fun providesSendCommentUseCase(
        commentRepository: CommentRepository,
    ): SendCommentUseCase {
        return object : SendCommentUseCase {
            override suspend fun invoke(
                reviewId: Int,
                comment: String,
                tagUserId: Int?,
                onLocalUpdated: () -> Unit
            ) {
                commentRepository.addComment(reviewId, comment, onLocalUpdated)
            }
        }
    }

    @Provides
    fun providesSendReplyUseCase(
        commentRepository: CommentRepository
    ): SendReplyUseCase {
        return object : SendReplyUseCase {
            override suspend fun invoke(
                reviewId: Int,
                parentCommentId: Long,
                comment: String,
                tagUserId: Int?,
                onLocalUpdated: () -> Unit
            ) {
                commentRepository.addReply(
                    reviewId = reviewId,
                    comment = comment,
                    parentCommentId = parentCommentId.toInt(),
                    onLocalUpdated = onLocalUpdated
                )
            }
        }
    }

    @Provides
    fun provideDeleteCommentUseCase(apiComment: ApiComment): DeleteCommentUseCase {
        return object : DeleteCommentUseCase {
            override suspend fun delete(commentId: Long) {
                apiComment.deleteComment(commentId.toInt())
            }
        }
    }

    @Provides
    fun providesAddCommentLikeUseCase(
        apiCommentLike: ApiCommentLike,
        sessionClientService: SessionClientService
    ): AddCommentLikeUseCase {
        return object : AddCommentLikeUseCase {
            override suspend fun invoke(commentId: Long): Int {
                val result =
                    apiCommentLike.addCommentLike(
                        sessionClientService.getToken() ?: "",
                        commentId.toInt()
                    )
                return result.commentLikeId
            }
        }
    }

    @Provides
    fun providesDeleteCommentLikeUseCase(
        apiCommentLike: ApiCommentLike,
        sessionClientService: SessionClientService
    ): DeleteCommentLikeUseCase {
        return object : DeleteCommentLikeUseCase {
            override suspend fun invoke(commentId: Long): Boolean {
                return apiCommentLike.deleteCommentLike(
                    sessionClientService.getToken() ?: "",
                    commentId.toInt()
                )
            }
        }
    }

    @Provides
    fun providesLoadMoreUseCase(
        commentRepository: CommentRepository
    ): LoadMoreUseCase {
        return object : LoadMoreUseCase {
            override suspend fun invoke(commentId: Int) {
                return commentRepository.loadMoreReply(commentId)
            }
        }
    }

    fun RemoteComment.toComment(): Comment {
        return Comment(
            name = this.user.userName,
            comment = this.comment,
            date = "",
            profileImageUrl = BuildConfig.PROFILE_IMAGE_SERVER_URL + this.user.profilePicUrl,
            userId = this.user.userId,
            commentsId = this.comment_id.toLong(),
            commentLikeCount = 0,
            parentCommentId = this.parent_comment_id.toLong(),
        )
    }

    private fun Comment.toComment(): Comment {
        return Comment(
            name = this.name,
            comment = comment,
            date = DateConverter.formattedDate(this.date),
            profileImageUrl = BuildConfig.PROFILE_IMAGE_SERVER_URL + this.profileImageUrl,
            userId = userId,
            commentsId = this.commentsId,
            commentLikeCount = commentLikeCount,
            commentLikeId = commentLikeId,
            /*tagUser = if (it.tagUser != null) TagUser(
                it.tagUser!!.userId,
                it.tagUser!!.userName
            ) else null,*/
            subCommentCount = subCommentCount,
            parentCommentId = parentCommentId?.toLong(),
            isUploading = isUploading
        )
    }

    fun List<Comment>.toComments(): List<Comment> {
        return this.map {
            it.toComment()
        }
    }

    @Singleton
    @Provides
    fun provideIsLoginFlowUseCase(
        loginRepository: LoginRepository,
    ): IsLoginFlowForCommentUseCase {
        return object : IsLoginFlowForCommentUseCase {
            override val isLogin: Flow<Boolean> get() = loginRepository.isLogin

        }
    }
}