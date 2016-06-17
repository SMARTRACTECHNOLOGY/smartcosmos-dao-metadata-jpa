package net.smartcosmos.dao.metadata.impl;

import net.smartcosmos.dao.metadata.MetadataPersistenceConfig;
import net.smartcosmos.dao.metadata.MetadataPersistenceTestApplication;
import net.smartcosmos.dao.metadata.domain.MetadataEntity;
import net.smartcosmos.dao.metadata.repository.MetadataRepository;
import net.smartcosmos.dto.metadata.MetadataQuery;
import net.smartcosmos.dto.metadata.MetadataQueryMatchResponse;
import net.smartcosmos.dto.metadata.MetadataResponse;
import net.smartcosmos.dto.metadata.MetadataUpsert;
import net.smartcosmos.security.user.SmartCosmosUser;
import net.smartcosmos.util.UuidUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

@SuppressWarnings("Duplicates")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { MetadataPersistenceTestApplication.class, MetadataPersistenceConfig.class })
@ActiveProfiles("test")
@WebAppConfiguration
@IntegrationTest({ "spring.cloud.config.enabled=false", "eureka.client.enabled:false" })
public class MetadataPersistenceServiceTest {

    private final UUID accountId = UUID.randomUUID();
    private final String accountUrn = UuidUtil.getAccountUrnFromUuid(accountId);

    private static final String ENTITY_REFERENCE_TYPE = "Object";
    private static final String KEY_A = "keyName-A";
    private static final String KEY_B = "keyName-B";
    private static final String DATA_TYPE_STRING = "StringType";
    private static final String DATA_TYPE_BOOLEAN = "BooleanType";
    private static final String RAW_VALUE_STRING_A = "ABC";
    private static final String RAW_VALUE_STRING_B = "DEF";
    private static final String RAW_VALUE_BOOLEAN_A = "true";
    private static final String RAW_VALUE_BOOLEAN_B = "false";

    private static final UUID REFERENCE_ID_ONE = UuidUtil.getNewUuid();
    private static final String KEY_ONE = KEY_A;
    private static final String DATA_TYPE_ONE = DATA_TYPE_STRING;
    private static final String RAW_VALUE_ONE = RAW_VALUE_STRING_A;

    private static final UUID REFERENCE_ID_TWO = UuidUtil.getNewUuid();
    private static final String KEY_TWO = KEY_A;
    private static final String DATA_TYPE_TWO = DATA_TYPE_BOOLEAN;
    private static final String RAW_VALUE_TWO = RAW_VALUE_BOOLEAN_A;

    private static final UUID REFERENCE_ID_THREE = UuidUtil.getNewUuid();
    private static final String KEY_THREE = KEY_A;
    private static final String DATA_TYPE_THREE = DATA_TYPE_STRING;
    private static final String RAW_VALUE_THREE = RAW_VALUE_STRING_B;

    private static final UUID REFERENCE_ID_FOUR = UuidUtil.getNewUuid();
    private static final String KEY_FOUR = KEY_B;
    private static final String DATA_TYPE_FOUR = DATA_TYPE_BOOLEAN;
    private static final String RAW_VALUE_FOUR = RAW_VALUE_BOOLEAN_A;

    private static final UUID REFERENCE_ID_FIVE = UuidUtil.getNewUuid();
    private static final String KEY_FIVE = KEY_B;
    private static final String DATA_TYPE_FIVE = DATA_TYPE_STRING;
    private static final String RAW_VALUE_FIVE = RAW_VALUE_STRING_A;

    private static final UUID REFERENCE_ID_SIX = UuidUtil.getNewUuid();
    private static final String KEY_SIX = KEY_B;
    private static final String DATA_TYPE_SIX = DATA_TYPE_BOOLEAN;
    private static final String RAW_VALUE_SIX = RAW_VALUE_BOOLEAN_B;

    @Autowired
    MetadataPersistenceService metadataPersistenceService;

    @Autowired
    MetadataRepository metadataRepository;

    @Before
    public void setUp() throws Exception {

        // Need to mock out user for conversion service.
        // Might be a good candidate for a test package util.
        Authentication authentication = Mockito.mock(Authentication.class);
        Mockito.when(authentication.getPrincipal())
            .thenReturn(new SmartCosmosUser(accountUrn, "urn:userUrn", "username",
                "password", Arrays.asList(new SimpleGrantedAuthority("USER"))));
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @After
    public void tearDown() throws Exception {
        metadataRepository.deleteAll();
    }

    // region Upsert

    @Test
    public void testCreate() throws Exception {

        final String key = "keyName";
        final String dataType = "BooleanType";
        final String rawValue = "true";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        MetadataUpsert create = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(rawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> createList = new ArrayList<>();
        createList.add(create);

        List<MetadataResponse> responseList = metadataPersistenceService.upsert(accountUrn, createList);

        assertFalse(responseList.isEmpty());
        assertEquals(1, responseList.size());
        assertEquals(referenceUrn, responseList.get(0).getReferenceUrn());
        assertEquals(entityReferenceType, responseList.get(0).getEntityReferenceType());
        assertEquals(key, responseList.get(0).getKey());
        assertEquals(dataType, responseList.get(0).getDataType());
        assertEquals(rawValue, responseList.get(0).getRawValue());

        List<MetadataEntity> entityList = metadataRepository.findByAccountIdAndReferenceId(accountId, UuidUtil.getUuidFromUrn(referenceUrn));

        assertFalse(entityList.isEmpty());

        assertFalse(entityList.isEmpty());
        assertEquals(1, entityList.size());
        assertEquals(referenceUrn, UuidUtil.getUrnFromUuid(entityList.get(0).getOwnerId()));
        assertEquals(entityReferenceType, entityList.get(0).getOwnerType());
        assertEquals(key, entityList.get(0).getKeyName());
        assertEquals(dataType, entityList.get(0).getDataType());
        assertEquals(rawValue, entityList.get(0).getValue());
    }

    @Test
    public void testUpdate() throws Exception {

        final String key = "updateKey";
        final String dataType = "BooleanType";
        final String initialRawValue = "true";
        final String updateRawValue = "false";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        MetadataUpsert create = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(initialRawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> createList = new ArrayList<>();
        createList.add(create);

        List<MetadataResponse> createResponseList = metadataPersistenceService.upsert(accountUrn, createList);

        assertFalse(createResponseList.isEmpty());
        assertEquals(1, createResponseList.size());
        assertEquals(referenceUrn, createResponseList.get(0).getReferenceUrn());
        assertEquals(entityReferenceType, createResponseList.get(0).getEntityReferenceType());
        assertEquals(key, createResponseList.get(0).getKey());
        assertEquals(dataType, createResponseList.get(0).getDataType());
        assertEquals(initialRawValue, createResponseList.get(0).getRawValue());

        MetadataUpsert update = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(updateRawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> updateList = new ArrayList<>();
        updateList.add(update);

        List<MetadataResponse> updateResponseList = metadataPersistenceService.upsert(accountUrn, updateList);

        assertFalse(updateResponseList.isEmpty());
        assertEquals(1, updateResponseList.size());
        assertEquals(referenceUrn, updateResponseList.get(0).getReferenceUrn());
        assertEquals(entityReferenceType, updateResponseList.get(0).getEntityReferenceType());
        assertEquals(key, updateResponseList.get(0).getKey());
        assertEquals(dataType, updateResponseList.get(0).getDataType());
        assertEquals(updateRawValue, updateResponseList.get(0).getRawValue());

        List<MetadataEntity> entityList = metadataRepository.findByAccountIdAndReferenceId(accountId, UuidUtil.getUuidFromUrn(referenceUrn));

        assertFalse(entityList.isEmpty());

        assertFalse(entityList.isEmpty());
        assertEquals(1, entityList.size());
        assertEquals(referenceUrn, UuidUtil.getUrnFromUuid(entityList.get(0).getOwnerId()));
        assertEquals(entityReferenceType, entityList.get(0).getOwnerType());
        assertEquals(key, entityList.get(0).getKeyName());
        assertEquals(dataType, entityList.get(0).getDataType());
        assertEquals(updateRawValue, entityList.get(0).getValue());
    }

    // endregion

    // region Delete

    @Test
    public void testDelete() {

        final String key = "deleteMe";
        final String dataType = "BooleanType";
        final String rawValue = "true";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        MetadataUpsert create = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(rawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> createList = new ArrayList<>();
        createList.add(create);
        metadataPersistenceService.upsert(accountUrn, createList);

        List<MetadataResponse> deleteList = metadataPersistenceService.delete(accountUrn, entityReferenceType, referenceUrn, key);

        assertFalse(deleteList.isEmpty());
        assertEquals(1, deleteList.size());
        assertEquals(referenceUrn, deleteList.get(0).getReferenceUrn());
        assertEquals(entityReferenceType, deleteList.get(0).getEntityReferenceType());
        assertEquals(key, deleteList.get(0).getKey());
        assertEquals(dataType, deleteList.get(0).getDataType());
        assertEquals(rawValue, deleteList.get(0).getRawValue());
    }

    @Test
    public void testDeleteNonexistent() {

        final String key = "this-does-not-exist";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        List<MetadataResponse> deleteList = metadataPersistenceService.delete(accountUrn, entityReferenceType, referenceUrn, key);

        assertTrue(deleteList.isEmpty());
    }

    // endregion

    // region Find by Key

    @Test
    public void testFindByKey() {

        final String key = "findMe";
        final String dataType = "BooleanType";
        final String rawValue = "true";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        MetadataUpsert create = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(rawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> createList = new ArrayList<>();
        createList.add(create);
        metadataPersistenceService.upsert(accountUrn, createList);

        Optional<MetadataResponse> response = metadataPersistenceService.findByKey(accountUrn, entityReferenceType, referenceUrn, key);

        assertTrue(response.isPresent());
        assertEquals(referenceUrn, response.get().getReferenceUrn());
        assertEquals(entityReferenceType, response.get().getEntityReferenceType());
        assertEquals(key, response.get().getKey());
        assertEquals(dataType, response.get().getDataType());
        assertEquals(rawValue, response.get().getRawValue());
    }

    @Test
    public void testFindByKeyNonexistent() {

        final String key = "this-does-not-exist";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        Optional<MetadataResponse> response = metadataPersistenceService.findByKey(accountUrn, entityReferenceType, referenceUrn, key);

        assertFalse(response.isPresent());
    }

    // endregion

    // region Query

    @Test
    public void testFindBySingleSearchCriteriaKey() {

        populateQueryData();

        final String key = "searchMe";
        final String dataType = "BooleanType";
        final String rawValue = "true";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        MetadataUpsert create = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(rawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> createList = new ArrayList<>();
        createList.add(create);
        metadataPersistenceService.upsert(accountUrn, createList);

        MetadataQuery query1 = MetadataQuery.builder()
            .key(key)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);

        Set<MetadataQueryMatchResponse> responseList = metadataPersistenceService.findBySearchCriteria(accountUrn,
            entityReferenceType,
            queryCollection,
            20);

        assertFalse(responseList.isEmpty());
        assertEquals(1, responseList.size());
    }

    @Test
    public void testFindBySingleSearchCriteriaRawValue() {

        populateQueryData();

        final String key = "searchMe";
        final String dataType = "IntegerType";
        final String rawValue = "42";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        MetadataUpsert create = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(rawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> createList = new ArrayList<>();
        createList.add(create);
        metadataPersistenceService.upsert(accountUrn, createList);

        MetadataQuery query1 = MetadataQuery.builder()
            .rawValue(rawValue)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);

        Set<MetadataQueryMatchResponse> responses = metadataPersistenceService.findBySearchCriteria(accountUrn,
            entityReferenceType,
            queryCollection,
            20);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(referenceUrn, responses.iterator().next().getUrn());
    }

    @Test
    public void testFindBySingleSearchCriteriaDataType() {

        populateQueryData();

        final String key = "searchMe";
        final String dataType = "DoubleType";
        final String rawValue = "23";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        MetadataUpsert create = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(rawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> createList = new ArrayList<>();
        createList.add(create);
        metadataPersistenceService.upsert(accountUrn, createList);

        MetadataQuery query1 = MetadataQuery.builder()
            .dataType(dataType)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);

        Set<MetadataQueryMatchResponse> responses = metadataPersistenceService.findBySearchCriteria(accountUrn,
            entityReferenceType,
            queryCollection,
            20);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());
        assertEquals(referenceUrn, responses.iterator().next().getUrn());
    }

    @Test
    public void testFindBySearchCriteriaNonexistent() {

        final String key = "does-not-exist";

        MetadataQuery query1 = MetadataQuery.builder()
            .key(key)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);

        Set<MetadataQueryMatchResponse> responses = metadataPersistenceService.findBySearchCriteria(accountUrn,
            "anyEntityReferenceType",
            queryCollection,
            20);

        assertTrue(responses.isEmpty());
    }

    @Test
    public void testFindByComplexSearchCriteriaDataTypeStringKeyA() {
        populateQueryData();

        MetadataQuery query1 = MetadataQuery.builder()
            .key(KEY_A)
            .dataType(DATA_TYPE_STRING)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);

        Set<MetadataQueryMatchResponse> responseList = metadataPersistenceService.findBySearchCriteria(accountUrn,
            ENTITY_REFERENCE_TYPE,
            queryCollection,
            20);

        assertFalse(responseList.isEmpty());
        assertEquals(2, responseList.size());

        List<UUID> uuidList = responseList.stream()
            .map(response -> UuidUtil.getUuidFromUrn(response.getUrn()))
            .collect(Collectors.toList());

        assertTrue(uuidList.contains(REFERENCE_ID_ONE));
        assertTrue(uuidList.contains(REFERENCE_ID_THREE));
    }

    @Test
    public void testFindByComplexSearchCriteriasDataTypeBooleanValueAAndKeyA() {
        populateQueryData();

        MetadataQuery query1 = MetadataQuery.builder()
            .dataType(DATA_TYPE_BOOLEAN)
            .rawValue(RAW_VALUE_BOOLEAN_A)
            .build();

        MetadataQuery query2 = MetadataQuery.builder()
            .key(KEY_A)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);
        queryCollection.add(query2);

        Set<MetadataQueryMatchResponse> responses = metadataPersistenceService.findBySearchCriteria(accountUrn,
            ENTITY_REFERENCE_TYPE,
            queryCollection,
            20);

        assertFalse(responses.isEmpty());
        assertEquals(1, responses.size());

        List<UUID> uuidList = responses.stream()
            .map(response -> UuidUtil.getUuidFromUrn(response.getUrn()))
            .collect(Collectors.toList());

        assertTrue(uuidList.contains(REFERENCE_ID_TWO));
    }

    @Test
    public void testFindByComplexSearchCriteriaNothingMatches() {
        populateQueryData();

        MetadataQuery query1 = MetadataQuery.builder()
            .dataType(DATA_TYPE_BOOLEAN)
            .rawValue(RAW_VALUE_BOOLEAN_A)
            .build();

        MetadataQuery query2 = MetadataQuery.builder()
            .key(KEY_A)
            .build();

        MetadataQuery query3 = MetadataQuery.builder()
            .dataType(DATA_TYPE_STRING)
            .key(KEY_B)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);
        queryCollection.add(query2);
        queryCollection.add(query3);

        Set<MetadataQueryMatchResponse> responses = metadataPersistenceService.findBySearchCriteria(accountUrn,
            ENTITY_REFERENCE_TYPE,
            queryCollection,
            20);

        assertTrue(responses.isEmpty());
    }

    // endregion

    // region Count

    @Test
    public void testCountBySingleSearchCriteriaKey() {

        final String key = "searchMe";
        final String dataType = "BooleanType";
        final String rawValue = "true";
        final String entityReferenceType = "Object";
        final String referenceUrn = "urn:uuid:" + UuidUtil.getNewUuidAsString();

        MetadataUpsert create = MetadataUpsert.builder()
            .referenceUrn(referenceUrn)
            .entityReferenceType(entityReferenceType)
            .rawValue(rawValue)
            .dataType(dataType)
            .key(key)
            .build();

        List<MetadataUpsert> createList = new ArrayList<>();
        createList.add(create);
        metadataPersistenceService.upsert(accountUrn, createList);

        MetadataQuery query1 = MetadataQuery.builder()
            .key(key)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);

        Long count = metadataPersistenceService.countBySearchCriteria(accountUrn,
            ENTITY_REFERENCE_TYPE,
            queryCollection);

        assertNotNull(count);
        assertTrue(1L == count);
    }

    @Test
    public void testCountNonexistent() {
        final String key = "does-not-exist";

        MetadataQuery query1 = MetadataQuery.builder()
            .key(key)
            .build();

        Collection<MetadataQuery> queryCollection = new ArrayList<>();
        queryCollection.add(query1);

        Long count = metadataPersistenceService.countBySearchCriteria(accountUrn, ENTITY_REFERENCE_TYPE, queryCollection);

        assertNotNull(count);
        assertTrue(0L == count);
    }

    // endregion

    // region Helper Methods

    private void populateQueryData() {

        MetadataEntity entity1 = MetadataEntity.builder()
            .accountId(accountId)
            .entityReferenceType(ENTITY_REFERENCE_TYPE)
            .referenceId(REFERENCE_ID_ONE)
            .key(KEY_ONE)
            .dataType(DATA_TYPE_ONE)
            .rawValue(RAW_VALUE_ONE)
            .build();

        MetadataEntity entity2 = MetadataEntity.builder()
            .accountId(accountId)
            .entityReferenceType(ENTITY_REFERENCE_TYPE)
            .referenceId(REFERENCE_ID_TWO)
            .key(KEY_TWO)
            .dataType(DATA_TYPE_TWO)
            .rawValue(RAW_VALUE_TWO)
            .build();

        MetadataEntity entity3 = MetadataEntity.builder()
            .accountId(accountId)
            .entityReferenceType(ENTITY_REFERENCE_TYPE)
            .referenceId(REFERENCE_ID_THREE)
            .key(KEY_THREE)
            .dataType(DATA_TYPE_THREE)
            .rawValue(RAW_VALUE_THREE)
            .build();

        MetadataEntity entity4 = MetadataEntity.builder()
            .accountId(accountId)
            .entityReferenceType(ENTITY_REFERENCE_TYPE)
            .referenceId(REFERENCE_ID_FOUR)
            .key(KEY_FOUR)
            .dataType(DATA_TYPE_FOUR)
            .rawValue(RAW_VALUE_FOUR)
            .build();

        MetadataEntity entity5 = MetadataEntity.builder()
            .accountId(accountId)
            .entityReferenceType(ENTITY_REFERENCE_TYPE)
            .referenceId(REFERENCE_ID_FIVE)
            .key(KEY_FIVE)
            .dataType(DATA_TYPE_FIVE)
            .rawValue(RAW_VALUE_FIVE)
            .build();

        MetadataEntity entity6 = MetadataEntity.builder()
            .accountId(accountId)
            .entityReferenceType(ENTITY_REFERENCE_TYPE)
            .referenceId(REFERENCE_ID_SIX)
            .key(KEY_SIX)
            .dataType(DATA_TYPE_SIX)
            .rawValue(RAW_VALUE_SIX)
            .build();

        metadataRepository.save(entity1);
        metadataRepository.save(entity2);
        metadataRepository.save(entity3);
        metadataRepository.save(entity4);
        metadataRepository.save(entity5);
        metadataRepository.save(entity6);
    }

    // endregion
}
