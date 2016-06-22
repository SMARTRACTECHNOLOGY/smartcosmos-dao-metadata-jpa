package net.smartcosmos.dao.metadata.impl;

import net.smartcosmos.dao.metadata.MetadataPersistenceConfig;
import net.smartcosmos.dao.metadata.MetadataPersistenceTestApplication;
import net.smartcosmos.dao.metadata.domain.MetadataEntity;
import net.smartcosmos.dao.metadata.repository.MetadataRepository;
import net.smartcosmos.dao.metadata.util.UuidUtil;
import net.smartcosmos.dto.metadata.MetadataResponse;
import net.smartcosmos.security.user.SmartCosmosUser;
import org.json.JSONArray;
import org.json.JSONObject;
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

import static org.junit.Assert.*;

@SuppressWarnings("Duplicates")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { MetadataPersistenceTestApplication.class, MetadataPersistenceConfig.class })
@ActiveProfiles("test")
@WebAppConfiguration
@IntegrationTest({ "spring.cloud.config.enabled=false", "eureka.client.enabled:false" })
public class MetadataPersistenceServiceTest {

    private final UUID tenantId = UUID.randomUUID();
    private final String tenantUrn = UuidUtil.getTenantUrnFromUuid(tenantId);

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
            .thenReturn(new SmartCosmosUser(tenantUrn, "urn:userUrn", "username",
                "password", Arrays.asList(new SimpleGrantedAuthority("USER"))));
        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @After
    public void tearDown() throws Exception {
        metadataRepository.deleteAll();
    }

    // region Create

    @Test
    public void testCreate() throws Exception {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        final Boolean bool = true;
        final JSONObject jsonObject = new JSONObject("{\"x\":1,\"y\":2}");
        final JSONArray jsonArray = new JSONArray("[\"x\",\"y\"]");
        final Number number = 123;
        final String text = "Text";

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("someBool", bool);
        keyValues.put("someJsonObject", jsonObject);
        keyValues.put("someJsonArray", jsonArray);
        keyValues.put("someNumber", number);
        keyValues.put("someNull", null);
        keyValues.put("someString", text);

        Optional<MetadataResponse> response = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);

        assertTrue(response.isPresent());
        assertEquals(ownerType, response.get().getOwnerType());
        assertEquals(ownerUrn, response.get().getOwnerUrn());

        assertEquals(6, response.get().getMetadata().size());

        assertEquals(bool, response.get().getMetadata().get("someBool"));
        assertEquals(jsonObject.toString(), response.get().getMetadata().get("someJsonObject").toString());
        assertEquals(jsonArray.toString(), response.get().getMetadata().get("someJsonArray").toString());
        assertEquals(number, response.get().getMetadata().get("someNumber"));
        assertEquals(JSONObject.NULL, response.get().getMetadata().get("someNull"));
        assertEquals(text, response.get().getMetadata().get("someString"));

        List<MetadataEntity> entityList = metadataRepository.findByTenantIdAndOwnerTypeAndOwnerId(
            tenantId,
            ownerType,
            UuidUtil.getUuidFromUrn(ownerUrn));

        assertFalse(entityList.isEmpty());

        assertFalse(entityList.isEmpty());
        assertEquals(6, entityList.size());
    }

    @Test
    public void testCreateFailOnDuplicateKey() {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("duplicateCreate", true);

        Optional<MetadataResponse> response1 = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);
        assertTrue(response1.isPresent());

        Optional<MetadataResponse> response2 = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);
        assertFalse(response2.isPresent());
    }

    @Test
    public void testCreateFailOnEmptyMetadataMap() {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();

        Optional<MetadataResponse> response = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);
        assertFalse(response.isPresent());
    }

    @Test
    public void testCreateFailOnNullMetadataMap() {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = null;

        Optional<MetadataResponse> response = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);
        assertFalse(response.isPresent());
    }

    // endregion */

    // region Upsert

    @Test
    public void testUpsert() throws Exception {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        JSONObject o = new JSONObject("{\"x\":1,\"y\":2}");

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("upsertBool", true);
        keyValues.put("upsertJson", o);
        keyValues.put("upsertNumber", 123);
        keyValues.put("upsertNull", null);
        keyValues.put("upsertString", "Text");

        Optional<MetadataResponse> response = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);

        assertTrue(response.isPresent());
        assertEquals(ownerType, response.get().getOwnerType());
        assertEquals(ownerUrn, response.get().getOwnerUrn());
        assertEquals(5, response.get().getMetadata().size());
        assertTrue(Boolean.parseBoolean(response.get().getMetadata().get("upsertBool").toString()));
        assertEquals("Text", response.get().getMetadata().get("upsertString").toString());

        JSONObject output = (JSONObject) response.get().getMetadata().get("upsertJson");
        assertEquals(1, output.getInt("x"));
        assertEquals(2, output.getInt("y"));

        List<MetadataEntity> entityList = metadataRepository.findByTenantIdAndOwnerTypeAndOwnerId(
            tenantId,
            ownerType,
            UuidUtil.getUuidFromUrn(ownerUrn));

        assertFalse(entityList.isEmpty());

        assertFalse(entityList.isEmpty());
        assertEquals(5, entityList.size());
    }

    @Test
    public void testUpsertWorksOnDuplicateKey() {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("duplicateUpsert", true);

        Optional<MetadataResponse> response1 = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);
        assertTrue(response1.isPresent());

        Optional<MetadataResponse> response2 = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);
        assertTrue(response2.isPresent());
    }

    @Test
    public void testUpsertFailOnEmptyMetadataMap() {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();

        Optional<MetadataResponse> response = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);
        assertFalse(response.isPresent());
    }

    @Test
    public void testUpsertFailOnNullMetadataMap() {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = null;

        Optional<MetadataResponse> response = metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);
        assertFalse(response.isPresent());
    }

    // endregion */

    // region Update

    @Test
    public void testUpdate() throws Exception {

        final String keyName = "updateMe";
        final Boolean value = true;
        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put(keyName, value);
        metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);

        Optional<Object> o = metadataPersistenceService.findByKey(tenantUrn, ownerType, ownerUrn, keyName);

        assertTrue(o.isPresent());
        assertEquals(true, o.get());

        Optional<MetadataResponse> response = metadataPersistenceService.update(tenantUrn, ownerType, ownerUrn, keyName, false);

        assertTrue(response.isPresent());
        assertEquals(ownerType, response.get().getOwnerType());
        assertEquals(ownerUrn, response.get().getOwnerUrn());
        assertEquals(1, response.get().getMetadata().size());
        assertFalse(Boolean.parseBoolean(response.get().getMetadata().get("updateMe").toString()));

        o = metadataPersistenceService.findByKey(tenantUrn, ownerType, ownerUrn, keyName);

        assertTrue(o.isPresent());
        assertEquals(false, o.get());
    }

    // endregion */

    // region Delete

    @Test
    public void testDelete() {

        final String keyName = "deleteMe";
        final Boolean value = true;
        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put(keyName, value);

        metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);

        List<MetadataResponse> deleteList = metadataPersistenceService.delete(tenantUrn, ownerType, ownerUrn, keyName);

        assertFalse(deleteList.isEmpty());
        assertEquals(1, deleteList.size());
        assertEquals(ownerType, deleteList.get(0).getOwnerType());
        assertEquals(ownerUrn, deleteList.get(0).getOwnerUrn());
        assertEquals(1, deleteList.get(0).getMetadata().size());
        assertTrue(Boolean.parseBoolean(deleteList.get(0).getMetadata().get(keyName).toString()));
    }

    @Test
    public void testDeleteNonexistent() {

        final String keyName = "this-does-not-exist";
        final String ownerType = "Object";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        List<MetadataResponse> deleteList = metadataPersistenceService
            .delete(tenantUrn, ownerType, ownerUrn, keyName);

        assertTrue(deleteList.isEmpty());
    }

    // endregion */

    // region DeleteAllByOwner

    @Test
    public void testDeleteAllByOwner() {

        final Boolean value = true;
        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put("testDeleteAll1", value);
        keyValues.put("testDeleteAll2", value);

        metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);

        List<MetadataResponse> deleteList = metadataPersistenceService
            .deleteAllByOwner(tenantUrn, ownerType, ownerUrn);

        assertFalse(deleteList.isEmpty());
        assertEquals(2, deleteList.size());
        assertEquals(ownerType, deleteList.get(0).getOwnerType());
        assertEquals(ownerUrn, deleteList.get(0).getOwnerUrn());
        assertEquals(1, deleteList.get(0).getMetadata().size());
    }

    @Test
    public void testDeleteAllByOwnerNonexistent() {

        final String keyName = "these-does-not-exist";
        final String ownerType = "Object";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        List<MetadataResponse> deleteList = metadataPersistenceService
            .deleteAllByOwner(tenantUrn, ownerType, ownerUrn);

        assertTrue(deleteList.isEmpty());
    }

    // endregion */

    // region Find by Key

    @Test
    public void testFindByKey() {

        final String keyName = "findMe";
        final Boolean value = true;
        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put(keyName, value);

        metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);

        Optional<Object> response = metadataPersistenceService.findByKey(tenantUrn, ownerType, ownerUrn, keyName);

        assertTrue(response.isPresent());
        assertEquals(true, response.get());
    }

    @Test
    public void testFindByKeyNonexistent() {

        final String keyName = "this-does-not-exist";
        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Optional<Object> response = metadataPersistenceService.findByKey(tenantUrn, ownerType, ownerUrn, keyName);

        assertFalse(response.isPresent());
    }

    // endregion */

    // region Find by Owner

    @Test
    public void testFindByOwner() {

        final Boolean value = true;
        final String key1 = "testFindByKey1";
        final String key2 = "testFindByKey2";
        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put(key1, value);
        keyValues.put(key2, value);

        metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);

        Collection<String> keySet = new ArrayList<>();
        keySet.add(key1);
        keySet.add(key2);

        Optional<MetadataResponse> response = metadataPersistenceService
                .findByOwner(tenantUrn, ownerType, ownerUrn, keySet);

        assertTrue(response.isPresent());

        assertEquals(ownerType, response.get().getOwnerType());
        assertEquals(ownerUrn, response.get().getOwnerUrn());
        assertEquals(keySet.size(), response.get().getMetadata().size());
        assertTrue(Boolean.parseBoolean(response.get().getMetadata().get(key1).toString()));
        assertTrue(Boolean.parseBoolean(response.get().getMetadata().get(key2).toString()));
    }

    @Test
    public void testFindByOwnerWithoutKeys() {

        final Boolean value = true;
        final String key1 = "testFindWithoutKey1";
        final String key2 = "testFindWithoutKey2";
        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Map<String, Object> keyValues = new HashMap<>();
        keyValues.put(key1, value);
        keyValues.put(key2, value);

        metadataPersistenceService.create(tenantUrn, ownerType, ownerUrn, keyValues);

        Collection<String> keySet = new ArrayList<>();

        Optional<MetadataResponse> response = metadataPersistenceService
            .findByOwner(tenantUrn, ownerType, ownerUrn, keySet);

        assertTrue(response.isPresent());

        assertEquals(ownerType, response.get().getOwnerType());
        assertEquals(ownerUrn, response.get().getOwnerUrn());
        assertEquals(2, response.get().getMetadata().size());
        assertTrue(Boolean.parseBoolean(response.get().getMetadata().get(key1).toString()));
        assertTrue(Boolean.parseBoolean(response.get().getMetadata().get(key2).toString()));
    }

    @Test
    public void testFindByOwnerNonexistent() {

        final String ownerType = "Thing";
        final String ownerUrn = UuidUtil.getThingUrnFromUuid(UUID.randomUUID());

        Collection<String> keySet = new ArrayList<>();

        Optional<MetadataResponse> response = metadataPersistenceService
                .findByOwner(tenantUrn, ownerType, ownerUrn, keySet);

        assertFalse(response.isPresent());
    }

    // endregion */
}
