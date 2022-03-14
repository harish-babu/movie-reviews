package io.realworld.db;

import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.sqlobject.CreateSqlObject;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface TransactionalMovieRepository {

    @CreateSqlObject
    abstract MoviesRepository createRepository();

    @Transaction
    default void likeMovie (Long userId, Long articleId) {
        createRepository().likeMovie(userId, articleId);
        if (userId > 0) throw new TransactionException ("Just for kicks!");
        createRepository().incrementFavoritesCount(articleId);
    }
}
