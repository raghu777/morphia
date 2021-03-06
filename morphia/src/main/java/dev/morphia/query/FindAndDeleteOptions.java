package dev.morphia.query;

import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Collation;
import com.mongodb.client.model.FindOneAndDeleteOptions;
import dev.morphia.internal.SessionConfigurable;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.concurrent.TimeUnit;

/**
 * Defines options to use for find and delete operations
 */
public class FindAndDeleteOptions extends FindOneAndDeleteOptions implements SessionConfigurable<FindAndDeleteOptions> {
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;
    private ClientSession clientSession;

    /**
     * Applies the options to the collection
     *
     * @param collection the collection to update
     * @param <T>        the collection type
     * @return either the passed collection or the updated collection
     * @since 2.0
     */
    public <T> MongoCollection<T> apply(final MongoCollection<T> collection) {
        return writeConcern == null
               ? collection
               : collection.withWriteConcern(writeConcern);
    }

    @Override
    public FindAndDeleteOptions clientSession(final ClientSession clientSession) {
        this.clientSession = clientSession;
        return this;
    }

    @Override
    public ClientSession clientSession() {
        return clientSession;
    }

    @Override
    public FindAndDeleteOptions projection(final Bson projection) {
        super.projection(projection);
        return this;
    }

    @Override
    public FindAndDeleteOptions sort(final Bson sort) {
        super.sort(sort);
        return this;
    }

    @Override
    public FindAndDeleteOptions maxTime(final long maxTime, final TimeUnit timeUnit) {
        super.maxTime(maxTime, timeUnit);
        return this;
    }

    @Override
    public FindAndDeleteOptions collation(final Collation collation) {
        super.collation(collation);
        return this;
    }

    /**
     * @param sort the sort to apply
     * @return this
     */
    public FindAndDeleteOptions sort(final Document sort) {
        super.sort(sort);
        return this;
    }

    /**
     * @return the write concern to use
     */
    public WriteConcern writeConcern() {
        return writeConcern;
    }

    /**
     * Sets the write concern
     *
     * @param writeConcern the write concern
     * @return this
     */
    public FindAndDeleteOptions writeConcern(final WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }
}
