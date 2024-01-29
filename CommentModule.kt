package com.sarang.torang.di.comment_di

import com.sarang.torang.BuildConfig
import com.sarang.torang.api.ApiComment
import com.sarang.torang.data.comments.Comment
import com.sarang.torang.data.comments.User
import com.sarang.torang.data.dao.LoggedInUserDao
import com.sarang.torang.repository.CommentRepository
import com.sarang.torang.session.SessionService
import com.sarang.torang.usecase.comments.DeleteCommentUseCase
import com.sarang.torang.usecase.comments.GetCommentsUseCase
import com.sarang.torang.usecase.comments.GetUserUseCase
import com.sarang.torang.usecase.comments.SendCommentUseCase
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
                        date = it.create_date,
                        likeCount = 0,
                        profileImageUrl = BuildConfig.PROFILE_IMAGE_SERVER_URL + it.user.profilePicUrl,
                        userId = it.user.userId,
                        commentsId = it.comment_id
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
                        userId = user.userId
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
            override suspend fun invoke(reviewId: Int, comment: String): Comment {
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
                            likeCount = 0,
                            profileImageUrl = it.user.profilePicUrl,
                            userId = it.user.userId,
                            commentsId = it.comment_id
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
            override suspend fun delete(commentId: Int) {
                apiComment.deleteComment(commentId)
            }
        }
    }
}