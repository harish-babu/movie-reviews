package io.realworld.api.response;

import java.util.List;

public class MovieReviewList {
    private List<MovieReview> movieReviews;
    private int reviewsCount;

    public List<MovieReview> getArticles() {
        return movieReviews;
    }

    public void setArticles(final List<MovieReview> movieReviews) {
        this.movieReviews = movieReviews;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(final int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }
}
