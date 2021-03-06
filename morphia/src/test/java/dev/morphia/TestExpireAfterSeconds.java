package dev.morphia;


import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.Indexes;
import dev.morphia.mapping.Mapper;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class TestExpireAfterSeconds extends TestBase {

    @Test
    public void testClassAnnotation() {
        getMapper().map(ClassAnnotation.class);
        getDs().ensureIndexes();

        getDs().save(new ClassAnnotation());

        final List<Document> indexes = getIndexInfo(ClassAnnotation.class);

        Assert.assertNotNull(indexes);
        Assert.assertEquals(2, indexes.size());
        Document index = null;
        for (final Document candidateIndex : indexes) {
            if (candidateIndex.containsKey("expireAfterSeconds")) {
                index = candidateIndex;
            }
        }
        Assert.assertNotNull(index);
        Assert.assertTrue(index.containsKey("expireAfterSeconds"));
        Assert.assertEquals(5, ((Number) index.get("expireAfterSeconds")).intValue());
    }

    @Test
    public void testIndexedField() {
        getMapper().map(HasExpiryField.class);
        getDs().ensureIndexes();

        getDs().save(new HasExpiryField());

        final List<Document> indexes = getIndexInfo(HasExpiryField.class);

        Assert.assertNotNull(indexes);
        Assert.assertEquals(2, indexes.size());
        Document index = null;
        for (final Document candidateIndex : indexes) {
            if (candidateIndex.containsKey("expireAfterSeconds")) {
                index = candidateIndex;
            }
        }
        Assert.assertNotNull(index);
        Assert.assertEquals(5, ((Number) index.get("expireAfterSeconds")).intValue());
    }

    @Entity
    public static class HasExpiryField {
        @Id
        private ObjectId id;
        @Indexed(options = @IndexOptions(expireAfterSeconds = 5))
        private final Date offerExpiresAt = new Date();
    }

    @Entity
    @Indexes(@Index(fields = @Field("offerExpiresAt"), options = @IndexOptions(expireAfterSeconds = 5)))
    public static class ClassAnnotation {
        @Id
        private ObjectId id;
        private final Date offerExpiresAt = new Date();
    }
}
