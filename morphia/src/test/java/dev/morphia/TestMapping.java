/*
  Copyright (C) 2010 Olafur Gauti Gudmundsson
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
  obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
  and limitations under the License.
 */


package dev.morphia;


import dev.morphia.TestInheritanceMappings.MapLike;
import dev.morphia.annotations.AlsoLoad;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.NotSaved;
import dev.morphia.annotations.Reference;
import dev.morphia.annotations.experimental.Constructor;
import dev.morphia.annotations.experimental.Name;
import dev.morphia.mapping.MappedClass;
import dev.morphia.mapping.MappedField;
import dev.morphia.mapping.Mapper;
import dev.morphia.mapping.MapperOptions;
import dev.morphia.mapping.MappingException;
import dev.morphia.mapping.NamingStrategy;
import dev.morphia.mapping.experimental.MorphiaReference;
import dev.morphia.mapping.lazy.proxy.ReferenceException;
import dev.morphia.mapping.validation.ConstraintViolationException;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.testmodel.Address;
import dev.morphia.testmodel.Hotel;
import dev.morphia.testmodel.PhoneNumber;
import dev.morphia.testmodel.RecursiveChild;
import dev.morphia.testmodel.RecursiveParent;
import dev.morphia.testmodel.TravelAgency;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import static dev.morphia.query.experimental.filters.Filters.eq;
import static dev.morphia.query.experimental.filters.Filters.exists;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@SuppressWarnings({"unchecked", "unchecked"})
public class TestMapping extends TestBase {

    @Test(expected = ConstraintViolationException.class)
    public void badConstructorMapping() {
        getDs().getMapper().map(BadConstructorBased.class);
    }

    @Test
    public void collectionNaming() {
        MapperOptions options = MapperOptions.builder()
                                             .collectionNaming(NamingStrategy.lowerCase())
                                             .build();
        Datastore datastore = Morphia.createDatastore(TEST_DB_NAME, options);
        List<MappedClass> map = datastore.getMapper().map(ContainsMapWithEmbeddedInterface.class, ContainsIntegerList.class);

        Assert.assertEquals("containsmapwithembeddedinterface", map.get(0).getCollectionName());
        Assert.assertEquals("cil", map.get(1).getCollectionName());

        options = MapperOptions.builder()
                               .collectionNaming(NamingStrategy.kebabCase())
                               .build();
        datastore = Morphia.createDatastore(TEST_DB_NAME, options);
        map = datastore.getMapper().map(ContainsMapWithEmbeddedInterface.class, ContainsIntegerList.class);

        Assert.assertEquals("contains-map-with-embedded-interface", map.get(0).getCollectionName());
        Assert.assertEquals("cil", map.get(1).getCollectionName());
    }

    @Test
    public void constructors() {
        getDs().getMapper().map(ConstructorBased.class);

        ContainsFinalField value = new ContainsFinalField();
        ConstructorBased instance = new ConstructorBased(new ObjectId(), "test instance", MorphiaReference.wrap(value));

        getDs().save(List.of(value, instance));

        ConstructorBased first = getDs().find(ConstructorBased.class).first();
        assertNotNull(first);
        assertEquals(instance, first);
    }

    @Test
    public void fieldNaming() {
        MapperOptions options = MapperOptions.builder()
                                             .fieldNaming(NamingStrategy.snakeCase())
                                             .build();
        Datastore datastore1 = Morphia.createDatastore(TEST_DB_NAME, options);
        List<MappedClass> map = datastore1.getMapper().map(ContainsMapWithEmbeddedInterface.class, ContainsIntegerList.class);

        List<MappedField> fields = map.get(0).getFields();
        validateField(fields, "_id", "id");
        validateField(fields, "embedded_values","embeddedValues");

        fields = map.get(1).getFields();
        validateField(fields, "_id", "id");
        validateField(fields, "int_list","intList");

        options = MapperOptions.builder()
                               .fieldNaming(NamingStrategy.kebabCase())
                               .build();
        final Datastore datastore2 = Morphia.createDatastore(TEST_DB_NAME, options);
        map = datastore2.getMapper().map(ContainsMapWithEmbeddedInterface.class, ContainsIntegerList.class);

        fields = map.get(0).getFields();
        validateField(fields, "_id", "id");
        validateField(fields, "embedded-values","embeddedValues");

        fields = map.get(1).getFields();
        validateField(fields, "_id", "id");
        validateField(fields, "int-list","intList");

    }

    private void validateField(final List<MappedField> fields, final String mapped, final String java) {
        Assert.assertNotNull(mapped, fields.stream().filter(f -> f.getMappedFieldName().equals(mapped)
                                                           && f.getJavaFieldName().equals(java)));
    }

    @Test
    public void testAlsoLoad() {
        getMapper().map(ContainsIntegerListNew.class, ContainsIntegerList.class);
        final ContainsIntegerList cil = new ContainsIntegerList();
        cil.intList.add(1);
        getDs().save(cil);
        final ContainsIntegerList cilLoaded = getDs().find(ContainsIntegerList.class)
                                                     .filter("_id", cil.id)
                                                     .first();
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(cilLoaded.intList.size(), cil.intList.size());
        assertEquals(cilLoaded.intList.get(0), cil.intList.get(0));

        final ContainsIntegerListNew cilNew = getDs().find(ContainsIntegerListNew.class).filter("_id", cil.id).first();
        assertNotNull(cilNew);
        assertNotNull(cilNew.integers);
        assertEquals(1, cilNew.integers.size());
        assertEquals(1, (int) cil.intList.get(0));
    }

    @Test
    public void testBadMappings() {
        try {
            getMapper().map(MissingId.class);
            fail("Validation: Missing @Id field not caught");
        } catch (MappingException e) {
            // good
        }

        try {
            getMapper().map(IdOnEmbedded.class);
            fail("Validation: @Id field on @Embedded not caught");
        } catch (MappingException e) {
            // good
        }

        try {
            getMapper().map(MissingIdStill.class);
            fail("Validation: Missing @Id field not not caught");
        } catch (MappingException e) {
            // good
        }

        try {
            getMapper().map(MissingIdRenamed.class);
            fail("Validation: Missing @Id field not not caught");
        } catch (MappingException e) {
            // good
        }

        try {
            getMapper().map(NonStaticInnerClass.class);
            fail("Validation: Non-static inner class allowed");
        } catch (MappingException e) {
            // good
        }
    }

    @Test
    public void testBaseEntityValidity() {
        getMapper().map(UsesBaseEntity.class);
    }

    @Test
    public void testBasicMapping() {
        Mapper mapper = getDs().getMapper();
        mapper.map(List.of(Hotel.class, TravelAgency.class));

        final Hotel borg = new Hotel();
        borg.setName("Hotel Borg");
        borg.setStars(4);
        borg.setTakesCreditCards(true);
        borg.setStartDate(new Date());
        borg.setType(Hotel.Type.LEISURE);
        borg.getTags().add("Swimming pool");
        borg.getTags().add("Room service");
        borg.setTemp("A temporary transient value");
        borg.getPhoneNumbers().add(new PhoneNumber(354, 5152233, PhoneNumber.Type.PHONE));
        borg.getPhoneNumbers().add(new PhoneNumber(354, 5152244, PhoneNumber.Type.FAX));

        final Address address = new Address();
        address.setStreet("Posthusstraeti 11");
        address.setPostCode("101");
        borg.setAddress(address);

        getDs().save(borg);

        Query<Hotel> query = getDs().find(Hotel.class)
                                    .filter("_id", borg.getId());
        Hotel borgLoaded = query.first();

        assertEquals(borg.getName(), borgLoaded.getName());
        assertEquals(borg.getStars(), borgLoaded.getStars());
        assertEquals(borg.getStartDate(), borgLoaded.getStartDate());
        assertEquals(borg.getType(), borgLoaded.getType());
        assertEquals(borg.getAddress().getStreet(), borgLoaded.getAddress().getStreet());
        assertEquals(borg.getTags().size(), borgLoaded.getTags().size());
        assertEquals(borg.getTags(), borgLoaded.getTags());
        assertEquals(borg.getPhoneNumbers().size(), borgLoaded.getPhoneNumbers().size());
        assertEquals(borg.getPhoneNumbers().get(1), borgLoaded.getPhoneNumbers().get(1));
        assertNull(borgLoaded.getTemp());
        assertTrue(borgLoaded.getPhoneNumbers() instanceof Vector);
        assertNotNull(borgLoaded.getId());

        final TravelAgency agency = new TravelAgency();
        agency.setName("Lastminute.com");
        agency.getHotels().add(borgLoaded);

        getDs().save(agency);

        final TravelAgency agencyLoaded = getDs()
                                              .find(TravelAgency.class)
                                              .filter("_id", agency.getId())
                                              .first();

        assertEquals(agency.getName(), agencyLoaded.getName());
        assertEquals(1, agency.getHotels().size());
        assertEquals(agency.getHotels().get(0).getName(), borg.getName());

        // try clearing values
        borgLoaded.setAddress(null);
        borgLoaded.getPhoneNumbers().clear();
        borgLoaded.setName(null);

        getDs().save(borgLoaded);

        borgLoaded = query.first();

        assertNull(borgLoaded.getAddress());
        assertEquals(0, borgLoaded.getPhoneNumbers().size());
        assertNull(borgLoaded.getName());
    }

    @Test
    public void testByteArrayMapping() {
        getMapper().map(ContainsByteArray.class);
        final ObjectId savedKey = getDs().save(new ContainsByteArray()).id;
        final ContainsByteArray loaded = getDs().find(ContainsByteArray.class)
                                                .filter("_id", savedKey)
                                                .first();
        assertEquals(new String((new ContainsByteArray()).bytes), new String(loaded.bytes));
        assertNotNull(loaded.id);
    }

    @Test
    public void testCollectionMapping() {
        getMapper().map(ContainsCollection.class);
        final ObjectId savedKey = getDs().save(new ContainsCollection()).id;
        final ContainsCollection loaded = getDs().find(ContainsCollection.class)
                                                 .filter("_id", savedKey)
                                                 .first();
        assertEquals(loaded.coll, (new ContainsCollection()).coll);
        assertNotNull(loaded.id);
    }

    @Test
    public void testEmbeddedArrayElementHasNoClassname() {
        getMapper().map(ContainsEmbeddedArray.class);
        final ContainsEmbeddedArray cea = new ContainsEmbeddedArray();
        cea.res = new RenamedEmbedded[]{new RenamedEmbedded()};

        final Document document = getMapper().toDocument(cea);
        List<Document> res = (List<Document>) document.get("res");
        assertFalse(res.get(0).containsKey(getMapper().getOptions().getDiscriminatorKey()));
    }

    @Test
    public void testEmbeddedDocument() {
        getMapper().map(ContainsDocument.class);
        getDs().save(new ContainsDocument());
        assertNotNull(getDs().find(ContainsDocument.class)
                             .execute(new FindOptions().limit(1))
                             .next());
    }

    @Test
    public void testEmbeddedEntity() {
        getMapper().map(ContainsEmbeddedEntity.class);
        getDs().save(new ContainsEmbeddedEntity());
        final ContainsEmbeddedEntity ceeLoaded = getDs().find(ContainsEmbeddedEntity.class)
                                                        .execute(new FindOptions().limit(1))
                                                        .next();
        assertNotNull(ceeLoaded);
        assertNotNull(ceeLoaded.id);
        assertNotNull(ceeLoaded.cil);
        assertNull(ceeLoaded.cil.id);

    }

    @Test
    public void testEmbeddedEntityDocumentHasNoClassname() {
        getMapper().map(ContainsEmbeddedEntity.class);
        final ContainsEmbeddedEntity cee = new ContainsEmbeddedEntity();
        cee.cil = new ContainsIntegerList();
        cee.cil.intList = Collections.singletonList(1);
        final Document document = getMapper().toDocument(cee);
        assertFalse(((Document) document.get("cil")).containsKey(getMapper().getOptions().getDiscriminatorKey()));
    }

    @Test
    public void testEnumKeyedMap() {
        final ContainsEnum1KeyMap map = new ContainsEnum1KeyMap();
        map.values.put(Enum1.A, "I'm a");
        map.values.put(Enum1.B, "I'm b");
        map.embeddedValues.put(Enum1.A, "I'm a");
        map.embeddedValues.put(Enum1.B, "I'm b");

        getDs().save(map);

        final ContainsEnum1KeyMap mapLoaded = getDs().find(ContainsEnum1KeyMap.class).filter("_id", map.id).first();

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(Enum1.A));
        assertNotNull(mapLoaded.values.get(Enum1.B));
        assertEquals(2, mapLoaded.embeddedValues.size());
        assertNotNull(mapLoaded.embeddedValues.get(Enum1.A));
        assertNotNull(mapLoaded.embeddedValues.get(Enum1.B));
    }

    @Test
    public void testFinalField() {
        getMapper().map(ContainsFinalField.class);
        final ObjectId savedKey = getDs().save(new ContainsFinalField("blah")).id;
        final ContainsFinalField loaded = getDs().find(ContainsFinalField.class)
                                                 .filter("_id", savedKey)
                                                 .first();
        assertNotNull(loaded);
        assertNotNull(loaded.name);
        assertEquals("blah", loaded.name);
    }

    @Test
    public void testFinalFieldNotPersisted() {
        MapperOptions options = MapperOptions.builder(getMapper().getOptions())
                                             .ignoreFinals(true)
                                             .build();
        final Datastore datastore = Morphia.createDatastore(getMongoClient(), getDatabase().getName(), options);

        getMapper().map(ContainsFinalField.class);
        final ObjectId savedKey = datastore.save(new ContainsFinalField("blah")).id;
        final ContainsFinalField loaded = datastore.find(ContainsFinalField.class)
                                                   .filter("_id", savedKey)
                                                   .first();
        assertNotNull(loaded);
        assertNotNull(loaded.name);
        assertEquals("foo", loaded.name);
    }

    @Test
    public void testFinalIdField() {
        getMapper().map(HasFinalFieldId.class);
        final long savedKey = getDs().save(new HasFinalFieldId(12)).id;
        final HasFinalFieldId loaded = getDs().find(HasFinalFieldId.class)
                                              .filter("_id", savedKey)
                                              .first();
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertEquals(12, loaded.id);
    }

    @Test
    @Ignore("need to add this feature")
    @SuppressWarnings("unchecked")
    public void testGenericKeyedMap() {
        final ContainsXKeyMap<Integer> map = new ContainsXKeyMap<>();
        map.values.put(1, "I'm 1");
        map.values.put(2, "I'm 2");

        getDs().save(map);

        final ContainsXKeyMap<Integer> mapLoaded = getDs().find(ContainsXKeyMap.class).filter("_id", map.id).first();

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(1));
        assertNotNull(mapLoaded.values.get(2));
    }

    @Test
    public void testIdFieldWithUnderscore() {
        getMapper().map(StrangelyNamedIdField.class);
    }

    @Test
    public void testIntKeySetStringMap() {
        final ContainsIntKeySetStringMap map = new ContainsIntKeySetStringMap();
        map.values.put(1, Collections.singleton("I'm 1"));
        map.values.put(2, Collections.singleton("I'm 2"));

        getDs().save(map);

        final ContainsIntKeySetStringMap mapLoaded = getDs().find(ContainsIntKeySetStringMap.class)
                                                            .filter("_id", map.id)
                                                            .first();

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(1));
        assertNotNull(mapLoaded.values.get(2));
        assertEquals(1, mapLoaded.values.get(1).size());

        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.2").exists());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.2").doesNotExist().count());
        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.4").doesNotExist());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.4").exists().count());
    }

    @Test
    public void testIntKeyedMap() {
        final ContainsIntKeyMap map = new ContainsIntKeyMap();
        map.values.put(1, "I'm 1");
        map.values.put(2, "I'm 2");

        getDs().save(map);

        final ContainsIntKeyMap mapLoaded = getDs().find(ContainsIntKeyMap.class)
                                                   .filter(eq("_id", map.id))
                                                   .first();

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(1));
        assertNotNull(mapLoaded.values.get(2));

        assertNotNull(getDs().find(ContainsIntKeyMap.class)
                             .filter(exists("values.2")));
        assertEquals(0, getDs().find(ContainsIntKeyMap.class)
                               .filter(exists("values.2").not())
                               .count());
        assertNotNull(getDs().find(ContainsIntKeyMap.class)
                             .filter(exists("values.4").not()));
        assertEquals(0, getDs().find(ContainsIntKeyMap.class)
                               .filter(exists("values.4"))
                               .count());
    }

    @Test
    public void testIntLists() {
        ContainsIntegerList cil = new ContainsIntegerList();
        getDs().save(cil);
        ContainsIntegerList cilLoaded = getDs().find(ContainsIntegerList.class)
                                               .filter("_id", cil.id)
                                               .first();
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(cilLoaded.intList.size(), cil.intList.size());


        cil = new ContainsIntegerList();
        cil.intList = null;
        getDs().save(cil);
        cilLoaded = getDs().find(ContainsIntegerList.class)
                           .filter("_id", cil.id)
                           .first();
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(0, cilLoaded.intList.size());

        cil = new ContainsIntegerList();
        cil.intList.add(1);
        getDs().save(cil);
        cilLoaded = getDs().find(ContainsIntegerList.class)
                           .filter("_id", cil.id)
                           .first();
        assertNotNull(cilLoaded);
        assertNotNull(cilLoaded.intList);
        assertEquals(1, cilLoaded.intList.size());
        assertEquals(1, (int) cilLoaded.intList.get(0));
    }

    @Test
    public void testLongArrayMapping() {
        getMapper().map(ContainsLongAndStringArray.class);
        getDs().save(new ContainsLongAndStringArray());
        ContainsLongAndStringArray loaded = getDs().find(ContainsLongAndStringArray.class)
                                                   .execute(new FindOptions().limit(1))
                                                   .next();
        assertArrayEquals(loaded.longs, (new ContainsLongAndStringArray()).longs);
        assertArrayEquals(loaded.strings, (new ContainsLongAndStringArray()).strings);

        final ContainsLongAndStringArray array = new ContainsLongAndStringArray();
        array.strings = new String[]{"a", "B", "c"};
        array.longs = new Long[]{4L, 5L, 4L};
        getDs().save(array);
        loaded = getDs().find(ContainsLongAndStringArray.class)
                        .filter("_id", array.id)
                        .first();
        assertArrayEquals(loaded.longs, array.longs);
        assertArrayEquals(loaded.strings, array.strings);

        assertNotNull(loaded.id);
    }

    @Test
    public void testMapLike() {
        final ContainsMapLike ml = new ContainsMapLike();
        ml.m.put("first", "test");
        getDs().save(ml);
        final ContainsMapLike mlLoaded = getDs().find(ContainsMapLike.class)
                                                .execute(new FindOptions().limit(1))
                                                .next();
        assertNotNull(mlLoaded);
        assertNotNull(mlLoaded.m);
        assertNotNull(mlLoaded.m.containsKey("first"));
    }

    @Test
    public void testMapWithEmbeddedInterface() {
        final ContainsMapWithEmbeddedInterface aMap = new ContainsMapWithEmbeddedInterface();
        final Foo f1 = new Foo1();
        final Foo f2 = new Foo2();

        aMap.embeddedValues.put("first", f1);
        aMap.embeddedValues.put("second", f2);
        getDs().save(aMap);

        final ContainsMapWithEmbeddedInterface mapLoaded = getDs().find(ContainsMapWithEmbeddedInterface.class)
                                                                  .execute(new FindOptions().limit(1))
                                                                  .next();

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.embeddedValues.size());
        assertTrue(mapLoaded.embeddedValues.get("first") instanceof Foo1);
        assertTrue(mapLoaded.embeddedValues.get("second") instanceof Foo2);

    }

    @Test
    public void testObjectIdKeyedMap() {
        getMapper().map(ContainsObjectIdKeyMap.class);
        final ContainsObjectIdKeyMap map = new ContainsObjectIdKeyMap();
        final ObjectId o1 = new ObjectId("111111111111111111111111");
        final ObjectId o2 = new ObjectId("222222222222222222222222");
        map.values.put(o1, "I'm 1s");
        map.values.put(o2, "I'm 2s");

        getDs().save(map);

        final ContainsObjectIdKeyMap mapLoaded = getDs().find(ContainsObjectIdKeyMap.class).filter("_id", map.id).first();

        assertNotNull(mapLoaded);
        assertEquals(2, mapLoaded.values.size());
        assertNotNull(mapLoaded.values.get(o1));
        assertNotNull(mapLoaded.values.get(o2));

        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.111111111111111111111111").exists());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.111111111111111111111111").doesNotExist().count());
        assertNotNull(getDs().find(ContainsIntKeyMap.class).field("values.4").doesNotExist());
        assertEquals(0, getDs().find(ContainsIntKeyMap.class).field("values.4").exists().count());
    }

    @Test
    public void testPrimMap() {
        final ContainsPrimitiveMap primMap = new ContainsPrimitiveMap();
        primMap.embeddedValues.put("first", 1L);
        primMap.embeddedValues.put("second", 2L);
        primMap.values.put("first", 1L);
        primMap.values.put("second", 2L);
        getDs().save(primMap);

        final ContainsPrimitiveMap primMapLoaded = getDs().find(ContainsPrimitiveMap.class)
                                                          .filter("_id", primMap.id)
                                                          .first();

        assertNotNull(primMapLoaded);
        assertEquals(2, primMapLoaded.embeddedValues.size());
        assertEquals(2, primMapLoaded.values.size());
    }

    @Test
    public void testPrimMapWithNullValue() {
        final ContainsPrimitiveMap primMap = new ContainsPrimitiveMap();
        primMap.embeddedValues.put("first", null);
        primMap.embeddedValues.put("second", 2L);
        primMap.values.put("first", null);
        primMap.values.put("second", 2L);
        getDs().save(primMap);

        final ContainsPrimitiveMap primMapLoaded = getDs().find(ContainsPrimitiveMap.class)
                                                          .filter("_id", primMap.id)
                                                          .first();

        assertNotNull(primMapLoaded);
        assertEquals(2, primMapLoaded.embeddedValues.size());
        assertEquals(2, primMapLoaded.values.size());
    }

    @Test
    @Category(Reference.class)
    @Ignore("entity caching needs to be implemented")
    public void testRecursiveReference() {
        getMapper().map(RecursiveParent.class, RecursiveChild.class);

        final RecursiveParent parent = getDs().save(new RecursiveParent());
        final RecursiveChild child = getDs().save(new RecursiveChild());

        final RecursiveParent parentLoaded = getDs().find(RecursiveParent.class)
                                                    .filter("_id", parent.getId())
                                                    .first();
        final RecursiveChild childLoaded = getDs().find(RecursiveChild.class)
                                                  .filter("_id", child.getId())
                                                  .first();

        parentLoaded.setChild(childLoaded);
        childLoaded.setParent(parentLoaded);

        getDs().save(parentLoaded);
        getDs().save(childLoaded);

        final RecursiveParent finalParentLoaded = getDs().find(RecursiveParent.class)
                                                         .filter("_id", parent.getId())
                                                         .first();
        final RecursiveChild finalChildLoaded = getDs().find(RecursiveChild.class)
                                                       .filter("_id", child.getId())
                                                       .first();


        assertNotNull(finalParentLoaded.getChild());
        assertNotNull(finalChildLoaded.getParent());
    }

    @Test(expected = ReferenceException.class)
    public void testReferenceWithoutIdValue() {
        getMapper().map(RecursiveParent.class, RecursiveChild.class);
        final RecursiveParent parent = new RecursiveParent();
        final RecursiveChild child = new RecursiveChild();
        child.setId(null);
        parent.setChild(child);
        getDs().save(parent);
    }

    @Test
    public void testUUID() {
        getMapper().map(ContainsUUID.class);
        final ContainsUUID uuid = new ContainsUUID();
        final UUID before = uuid.uuid;
        getDs().save(uuid);
        final ContainsUUID loaded = getDs().find(ContainsUUID.class)
                                           .execute(new FindOptions().limit(1))
                                           .next();
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertNotNull(loaded.uuid);
        assertEquals(before, loaded.uuid);
    }

    @Test
    public void testUuidId() {
        getMapper().map(List.of(ContainsUuidId.class));
        final ContainsUuidId uuidId = new ContainsUuidId();
        final UUID before = uuidId.id;
        getDs().save(uuidId);
        final ContainsUuidId loaded = getDs().find(ContainsUuidId.class).filter("_id", before).first();
        assertNotNull(loaded);
        assertNotNull(loaded.id);
        assertEquals(before, loaded.id);
    }

    @Test
    public void testLoadOnly() {
        getDs().save(new Normal("value"));
        Normal n = getDs().find(Normal.class)
                          .execute(new FindOptions().limit(1))
                          .next();
        Assert.assertNotNull(n);
        Assert.assertNotNull(n.name);
        getDs().delete(n);
        getDs().save(new NormalWithNotSaved());
        n = getDs().find(Normal.class)
                   .execute(new FindOptions().limit(1))
                   .next();
        Assert.assertNotNull(n);
        Assert.assertNull(n.name);
        getDs().delete(n);
        getDs().save(new Normal("value21"));
        final NormalWithNotSaved notSaved = getDs().find(NormalWithNotSaved.class)
                                                   .execute(new FindOptions().limit(1))
                                                   .next();
        Assert.assertNotNull(notSaved);
        Assert.assertNotNull(notSaved.name);
        Assert.assertEquals("never", notSaved.name);
    }

    public enum Enum1 {
        A,
        B
    }

    @Embedded
    private interface Foo {
    }

    @Entity
    public static class BadConstructorBased {
        @Id
        private ObjectId id;
        private String name;

        @Constructor
        public BadConstructorBased(@Name("_id") final ObjectId id,
                                   @Name("named") final String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Entity
    public abstract static class BaseEntity {
        @Id
        private ObjectId id;

        public String getId() {
            return id.toString();
        }

        public void setId(final String id) {
            this.id = new ObjectId(id);
        }
    }

    @Entity
    public static class ConstructorBased {
        @Id
        private ObjectId id;
        private String name;
        private MorphiaReference<ContainsFinalField> reference;

        @Constructor
        public ConstructorBased(@Name("id") final ObjectId id,
                                @Name("name") final String name,
                                @Name("reference") final MorphiaReference<ContainsFinalField> reference) {
            this.id = id;
            this.name = name;
            this.reference = reference;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (reference != null ? reference.hashCode() : 0);
            return result;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ConstructorBased)) {
                return false;
            }

            final ConstructorBased that = (ConstructorBased) o;

            if (id != null ? !id.equals(that.id) : that.id != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }
            return reference != null ? reference.equals(that.reference) : that.reference == null;
        }
    }

    @Entity
    private static class ContainsByteArray {
        private final byte[] bytes = "Scott".getBytes();
        @Id
        private ObjectId id;
    }

    @Entity
    private static final class ContainsCollection {
        private final Collection<String> coll = new ArrayList<>();
        @Id
        private ObjectId id;

        private ContainsCollection() {
            coll.add("hi");
            coll.add("Scott");
        }
    }

    @Entity
    private static class ContainsDocument {
        @Id
        private ObjectId id;
        private Document document = new Document("field", "val");
    }

    @Entity
    private static class ContainsEmbeddedArray {
        @Id
        private ObjectId id = new ObjectId();
        private RenamedEmbedded[] res;
    }

    @Entity
    private static class ContainsEmbeddedEntity {
        @Id
        private final ObjectId id = new ObjectId();
        private ContainsIntegerList cil = new ContainsIntegerList();
    }

    @Entity
    private static class ContainsEnum1KeyMap {
        private final Map<Enum1, String> values = new HashMap<>();
        private final Map<Enum1, String> embeddedValues = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsFinalField {
        private final String name;
        @Id
        private ObjectId id;

        protected ContainsFinalField() {
            name = "foo";
        }

        ContainsFinalField(final String name) {
            this.name = name;
        }
    }

    @Entity
    private static class ContainsIntKeyMap {
        private final Map<Integer, String> values = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsIntKeySetStringMap {
        private final Map<Integer, Set<String>> values = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity(value = "cil", useDiscriminator = false)
    private static class ContainsIntegerList {
        @Id
        private ObjectId id;
        private List<Integer> intList = new ArrayList<>();
    }

    @Entity(value = "cil", useDiscriminator = false)
    private static class ContainsIntegerListNew {
        @AlsoLoad("intList")
        private final List<Integer> integers = new ArrayList<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsLongAndStringArray {
        @Id
        private ObjectId id;
        private Long[] longs = {0L, 1L, 2L};
        private String[] strings = {"Scott", "Rocks"};
    }

    @Entity
    private static class ContainsMapLike {
        private final MapLike m = new MapLike();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsMapWithEmbeddedInterface {
        private final Map<String, Foo> embeddedValues = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsObjectIdKeyMap {
        private final Map<ObjectId, String> values = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity
    private static class ContainsPrimitiveMap {
        private final Map<String, Long> embeddedValues = new HashMap<>();
        private final Map<String, Long> values = new HashMap<>();
        @Id
        private ObjectId id;
    }

    @Entity(useDiscriminator = false)
    private static class ContainsUUID {
        private final UUID uuid = UUID.randomUUID();
        @Id
        private ObjectId id;
    }

    @Entity(useDiscriminator = false)
    private static class ContainsUuidId {
        @Id
        private final UUID id = UUID.randomUUID();
    }

    @Entity
    private static class ContainsXKeyMap<T> {
        private final Map<T, String> values = new HashMap<>();
        @Id
        private ObjectId id;
    }

    private static class Foo1 implements Foo {
        private String s;
    }

    private static class Foo2 implements Foo {
        private int i;
    }

    @Entity
    private static class HasFinalFieldId {
        @Id
        private final long id;
        private String name = "some string";

        protected HasFinalFieldId() {
            id = -1;
        }

        HasFinalFieldId(final long id) {
            this.id = id;
        }
    }

    @Embedded
    private static class IdOnEmbedded {
        @Id
        private ObjectId id;
    }

    @Entity
    private static class MapSubclass extends LinkedHashMap<String, Object> {
        @Id
        private ObjectId id;
    }

    @Entity
    public static class MissingId {
        private String id;
    }

    @Entity("no-id")
    private static class MissingIdRenamed {
        private String id;
    }

    @Entity
    private static class MissingIdStill {
        private String id;
    }

    @Embedded(useDiscriminator = false)
    private static class RenamedEmbedded {
        private String name;
    }

    @Entity
    private static class StrangelyNamedIdField {
        //CHECKSTYLE:OFF
        @Id
        private ObjectId id_ = new ObjectId();
        //CHECKSTYLE:ON
    }

    @Entity
    private static class UsesBaseEntity extends BaseEntity {

    }

    @Entity
    private class NonStaticInnerClass {
        @Id
        private long id = 1;
    }

    @Entity(value = "Normal", useDiscriminator = false)
    static class Normal {
        @Id
        private ObjectId id = new ObjectId();
        private String name;

        Normal(final String name) {
            this.name = name;
        }

        protected Normal() {
        }
    }

    @Entity(value = "Normal", useDiscriminator = false)
    private static class NormalWithNotSaved {
        @NotSaved
        private final String name = "never";
        @Id
        private ObjectId id = new ObjectId();
    }
}
