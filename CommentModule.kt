package com.sarang.torang.di.comment_di

import com.sarang.torang.BuildConfig
import com.sarang.torang.api.ApiComment
import com.sarang.torang.api.ApiCommentLike
import com.sarang.torang.data.comments.Comment
import com.sarang.torang.data.comments.TagUser
import com.sarang.torang.data.comments.User
import com.sarang.torang.data.dao.LoggedInUserDao
import com.sarang.torang.repository.CommentRepository
import com.sarang.torang.session.SessionClientService
import com.sarang.torang.session.SessionService
import com.sarang.torang.usecase.comments.AddCommentLikeUseCase
import com.sarang.torang.usecase.comments.DeleteCommentLikeUseCase
import com.sarang.torang.usecase.comments.DeleteCommentUseCase
import com.sarang.torang.usecase.comments.GetCommentsUseCase
import com.sarang.torang.usecase.comments.GetUserUseCase
import com.sarang.torang.usecase.comments.SendCommentUseCase
import com.sarang.torang.usecase.comments.SendReplyUseCase
import com.sarang.torang.util.DateConverter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class CommentModule {
    @Provides
    fun providesGetCommentsUseCase(
        commentRepository: CommentRepository
    ): GetCommentsUseCase {
        return object : GetCommentsUseCase {
            override suspend fun invoke(reviewId: Int): List<Comment> {
                return commentRepository.getComment(reviewId = reviewId).list.map {
                    Comment(
                        name = it.user.userName,
                        comment = it.comment,
                        date = DateConverter.formattedDate(it.create_date),
                        profileImageUrl = BuildConfig.PROFILE_IMAGE_SERVER_URL + it.user.profilePicUrl,
                        userId = it.user.userId,
                        commentsId = it.comment_id.toLong(),
                        commentLikeCount = it.comment_like_count,
                        commentLikeId = it.comment_like_id,
                        tagUser = if (it.tagUser != null) TagUser(
                            it.tagUser!!.userId,
                            it.tagUser!!.userName
                        ) else null,
                        subCommentCount = it.sub_comment_count,
                        parentCommentId = it.parentCommentId.toLong()
                    )
                }
            }
        }
    }

    @Provides
    fun providesGetUserUseCase(
        loggedInUserDao: LoggedInUserDao
    ): GetUserUseCase {
        return object : GetUserUseCase {
            override suspend fun invoke(): User {
                val user = loggedInUserDao.getLoggedInUser1()
                if (user != null) {
                    return User(
                        BuildConfig.PROFILE_IMAGE_SERVER_URL + user.profilePicUrl,
                        userId = user.userId,
                        userName = user.userName
                    )
                } else {
                    throw Exception("로그인을 해주세요.")
                }
            }
        }
    }

    @Provides
    fun providesSendCommentUseCase(
        apiComment: ApiComment,
        sessionService: SessionService
    ): SendCommentUseCase {
        return object : SendCommentUseCase {
            override suspend fun invoke(reviewId: Int, comment: String, tagUserId: Int?): Comment {
                val auth = sessionService.getToken()
                if (auth != null) {
                    auth.let {
                        val it = apiComment.addComment(
                            auth = auth,
                            review_id = reviewId,
                            comment = comment
                        )
                        return Comment(
                            name = it.user.userName,
                            comment = it.comment,
                            date = "",
                            profileImageUrl = BuildConfig.PROFILE_IMAGE_SERVER_URL + it.user.profilePicUrl,
                            userId = it.user.userId,
                            commentsId = it.comment_id.toLong(),
                            commentLikeCount = 0
                        )
                    }
                } else {
                    throw Exception("로그인을 해주세요.")
                }
            }
        }
    }

    @Provides
    fun providesSendReplyUseCase(
        apiComment: ApiComment,
        sessionService: SessionService
    ): SendReplyUseCase {
        return object : SendReplyUseCase {
            override suspend fun invoke(
                reviewId: Int,
                parentCommentId: Long,
                comment: String,
                tagUserId: Int?
            ): Comment {
                val auth = sessionService.getToken()
                if (auth != null) {
                    auth.let {
                        val it = apiComment.addComment(
                            auth = auth,
                            review_id = reviewId,
                            comment = comment,
                            parentCommentId = parentCommentId.toInt(),
                            tagUserId = tagUserId
                        )
                        return Comment(
                            name = it.user.userName,
                            comment = it.comment,
                            date = "",
                            profileImageUrl = BuildConfig.PROFILE_IMAGE_SERVER_URL + it.user.profilePicUrl,
                            userId = it.user.userId,
                            commentsId = it.comment_id.toLong(),
                            commentLikeCount = 0,
                            parentCommentId = it.parentCommentId.toLong(),
                        )
                    }
                } else {
                    throw Exception("로그인을 해주세요.")
                }
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
}