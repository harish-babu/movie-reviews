package io.realworld.core;

import com.google.common.collect.Sets;
import io.realworld.api.request.NewMovieReview;
import io.realworld.api.request.UpdatedArticle;
import io.realworld.api.response.MovieReview;
import io.realworld.api.response.MovieReviewList;
import io.realworld.db.ReviewRepository;
import io.realworld.db.CommentRepository;
import io.realworld.db.TagRepository;
import io.realworld.db.UserRepository;
import io.realworld.db.mapper.ArticleTagsMapper;
import io.realworld.exceptions.ApplicationException;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.*;

import static io.realworld.exceptions.ErrorCode.FORBIDDEN;
import static io.realworld.exceptions.ErrorCode.NOT_FOUND;
import static java.util.stream.Collectors.*;

public class ReviewsService {
    private final ReviewRepository articleRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final TagRepository tagRepository;

    public ReviewsService(final ReviewRepository articleRepository,
                          final UserRepository userRepository,
                          final CommentRepository commentRepository, final TagRepository tagRepository) {
        this.articleRepository = articleRepository;
        this.userRepository = userRepository;
        this.commentRepository = commentRepository;
        this.tagRepository = tagRepository;
    }

    public MovieReview findBySlug(final String username, final String slug) {
        final MovieReview movieReview = articleRepository.findReview(slug);
        fillAdditionalData(List.of(movieReview), username);
        return movieReview;
    }

    @Transaction
    public MovieReview createReview(final String username, final String id, final NewMovieReview newMovieReview) {
        final Long authorId = userRepository.findUserIdByUsername(username);
        final Long articleId = articleRepository.saveArticle(authorId,
                generateSlug(newMovieReview.getTitle()),
                Long.parseLong(id),
                newMovieReview.getTitle(),
                newMovieReview.getDescription(),
                newMovieReview.getBody());

        if (newMovieReview.getTagList() != null) {
            updateArticleTags(articleId, newMovieReview.getTagList());
        }

        return findFullDetailsArticle(username, articleId);
    }

    @Transaction
    public MovieReview updateReview(final String username, final MovieReview old, final UpdatedArticle update) {
        final Long articleId = old.getId();

        if (!Objects.equals(old.getAuthor().getUsername(), username)) {
            throw new ApplicationException(FORBIDDEN, "User is not allowed to update article [" + old.getSlug() + "]");
        }

        articleRepository.updateReview(articleId,
                update.getTitle() != null && !Objects.equals(update.getTitle(), old.getTitle()) ? generateSlug(update.getTitle()) : old.getSlug(),
                update.getTitle() != null ? update.getTitle() : old.getTitle(),
                update.getDescription() != null ? update.getDescription() : old.getDescription(),
                update.getBody() != null ? update.getBody() : old.getBody());


        if (update.getTagList() != null) {
            tagRepository.deleteArticleTags(articleId);
            updateArticleTags(articleId, update.getTagList());
        }

        return findFullDetailsArticle(username, articleId);
    }

    @Transaction
    public void deleteReview(final String slug) {
        tagRepository.deleteArticleTags(slug);
        commentRepository.deleteArticleComments(slug);
        articleRepository.deleteReview(slug);
    }

    @Transaction
    public MovieReview addArticleToFavourites(final String username, final String slug) {
        final Long userId = userRepository.findUserIdByUsername(username);
        final Long articleId = findArticleId(slug);

        articleRepository.addToFavorites(userId, articleId);
        articleRepository.incrementFavoritesCount(articleId);
        return findFullDetailsArticle(username, articleId);
    }

    @Transaction
    public MovieReview removeArticleFromFavourites(final String username, final String slug) {
        final Long userId = userRepository.findUserIdByUsername(username);
        final Long articleId = findArticleId(slug);

        articleRepository.removeFromFavorites(userId, articleId);
        articleRepository.decrementFavoritesCount(articleId);
        return findFullDetailsArticle(username, articleId);
    }

    public MovieReviewList findFeed(final String username, final int offset, final int limit) {
        final int count = articleRepository.countFeedSize(username);
        final List<MovieReview> movieReviews = articleRepository.findReviewsOfAuthor(username, offset, limit);
        fillAdditionalData(movieReviews, username);
        return articleList(movieReviews, count);
    }

    public MovieReviewList findReviews(final String username,
                                       final Long movieId,
                                       final String author,
                                       final String tag,
                                       final String favorite,
                                       final Integer offset,
                                       final int limit) {
        final Long favoriteBy = favorite != null ? userRepository.findUserIdByUsername(favorite) : null;
        final int count = articleRepository.countReviews(movieId, author, tag, favoriteBy);
        final List<MovieReview> movieReviews = articleRepository.findReviews(movieId, author, tag, favoriteBy, offset, limit);
        fillAdditionalData(movieReviews, username);
        return articleList(movieReviews, count);
    }


    private MovieReview findFullDetailsArticle(final String username, final Long articleId) {
        final MovieReview movieReview = articleRepository.findReviewById(articleId);
        fillAdditionalData(List.of(movieReview), username);
        return movieReview;
    }

    private void fillAdditionalData(final List<MovieReview> movieReviewList, final String username) {
        final Set<Long> articleIds = movieReviewList.stream().map(MovieReview::getId).collect(toSet());
        final Set<String> authors = movieReviewList.stream().map(e -> e.getAuthor().getUsername()).collect(toSet());
        fillTags(movieReviewList, articleIds);
        fillFavoritesFlags(movieReviewList, username, articleIds);
        fillFollowing(movieReviewList, username, authors);
    }

    private void fillFollowing(final List<MovieReview> movieReviewList, final String username, final Set<String> authors) {
        if (username != null && authors != null && !authors.isEmpty()) {
            final Set<String> usernames = userRepository.findFollowedAuthorUsernames(authors, username);
            for (final MovieReview movieReview : movieReviewList) {
                movieReview.getAuthor().setFollowing(usernames.contains(movieReview.getAuthor().getUsername()));
            }
        }
    }

    private void fillFavoritesFlags(final List<MovieReview> movieReviewList, final String username, final Set<Long> articleIds) {
        if (username != null && articleIds != null && !articleIds.isEmpty()) {
            final Set<Long> favoriteArticleIds = articleRepository.findFavoriteReviews(username, articleIds);
            for (final MovieReview movieReview : movieReviewList) {
                movieReview.setFavorited(favoriteArticleIds.contains(movieReview.getId()));
            }
        }
    }

    private void fillTags(final List<MovieReview> movieReviewList, final Set<Long> articleIds) {
        final Map<Long, Set<String>> articleTags = findArticlesTags(articleIds);
        for (final MovieReview movieReview : movieReviewList) {
            movieReview.setTagList(articleTags.get(movieReview.getId()));
        }
    }

    private MovieReview findArticle(final String slug) {
        final MovieReview movieReview = articleRepository.findReview(slug);
        if (movieReview == null) {
            throw new ApplicationException(NOT_FOUND, "Article [" + slug + "] not found");
        }
        return movieReview;
    }

    private Long findArticleId(final String slug) {
        final Long articleId = articleRepository.findReviewIdBySlug(slug);
        if (articleId == null) {
            throw new ApplicationException(NOT_FOUND, "Article [" + slug + "] not found");
        }
        return articleId;
    }

    private void updateArticleTags(final long articleId, final Set<String> tags) {
        final Set<String> existingTags = tagRepository.findTags(tags);
        final Set<String> missingTags = Sets.difference(tags, existingTags);
        tagRepository.saveTags(missingTags);
        tagRepository.addTagsToArticle(Collections.nCopies(tags.size(), articleId), tags);
    }

    private Map<Long, Set<String>> findArticlesTags(final Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Collections.emptyMap();
        }

        final List<ArticleTagsMapper.ArticleIdTag> articleTags = tagRepository.findArticlesTags(articleIds);
        return articleTags
                .stream()
                .collect(groupingBy(
                        ArticleTagsMapper.ArticleIdTag::getArticleId,
                        mapping(ArticleTagsMapper.ArticleIdTag::getTag, toSet())
                ));
    }

    private MovieReviewList articleList(final List<MovieReview> movieReviews, final int count) {
        final MovieReviewList movieReviewList = new MovieReviewList();
        movieReviewList.setArticles(movieReviews);
        movieReviewList.setReviewsCount(count);
        return movieReviewList;
    }

    private String generateSlug(final String title) {
        final String slug = title.strip()
                .replaceAll("[^\\p{IsAlphabetic}^\\p{IsDigit}]+", "-")
                .toLowerCase();

        return isSlugUnique(slug) ? slug : UUID.randomUUID().toString();
    }

    private boolean isSlugUnique(final String slug) {
        return articleRepository.findReviewIdBySlug(slug) == null;
    }
}
