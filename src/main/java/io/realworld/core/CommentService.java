package io.realworld.core;

import io.realworld.api.request.NewComment;
import io.realworld.api.response.MovieReview;
import io.realworld.api.response.Comment;
import io.realworld.db.ReviewRepository;
import io.realworld.db.CommentRepository;
import io.realworld.db.UserRepository;
import io.realworld.exceptions.ApplicationException;
import io.realworld.exceptions.ErrorCode;

import java.util.List;
import java.util.Objects;

import static io.realworld.exceptions.ErrorCode.NOT_FOUND;

public class CommentService {
    private final CommentRepository commentRepository;
    private final ReviewRepository articleRepository;
    private final UserRepository userRepository;

    public CommentService(final CommentRepository commentRepository,
                          final ReviewRepository articleRepository,
                          final UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
    }

    public Comment saveComment(final String username, final String slug, final NewComment newComment) {
        final Long articleId = articleRepository.findReviewIdBySlug(slug);
        if (articleId == null) {
            throw new ApplicationException(NOT_FOUND, "Could not find article [" + slug + "]");
        }
        final Long userId = userRepository.findUserIdByUsername(username);
        final long newCommentId = commentRepository.saveComment(userId, articleId, newComment.getBody());

        return commentRepository.findCommentById(newCommentId);
    }

    public void deleteComment(final String username, final String slug, final long commentId) {
        final Comment comment = findComment(commentId);
        final MovieReview movieReview = articleRepository.findReview(slug);

        final boolean isCommentAuthor = Objects.equals(comment.getAuthor().getUsername(), username);
        final boolean isArticleAuthor = movieReview != null && Objects.equals(movieReview.getAuthor().getUsername(), username);
        final boolean isAuthorizedToDeleteComment = isCommentAuthor || isArticleAuthor;
        if (!isAuthorizedToDeleteComment) {
            throw new ApplicationException(ErrorCode.FORBIDDEN, "Not allowed to delete comment with id [" + commentId + "]");
        }
        commentRepository.deleteComment(slug, commentId);
    }

    public List<Comment> findArticleComments(final String slug) {
        return commentRepository.findArticleComments(slug);
    }

    private Comment findComment(final long commentId) {
        final Comment comment = commentRepository.findCommentById(commentId);

        if (comment == null) {
            throw new ApplicationException(NOT_FOUND, "Comment with id [" + commentId + "] not found");
        }
        return comment;
    }
}
