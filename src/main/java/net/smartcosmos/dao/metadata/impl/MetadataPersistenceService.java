package net.smartcosmos.dao.metadata.impl;

import lombok.extern.slf4j.Slf4j;
import net.smartcosmos.dao.metadata.MetadataDao;
import net.smartcosmos.dao.metadata.domain.MetadataEntity;
import net.smartcosmos.dao.metadata.repository.MetadataRepository;
import net.smartcosmos.dao.metadata.util.SearchSpecifications;
import net.smartcosmos.dto.metadata.MetadataCreate;
import net.smartcosmos.dto.metadata.MetadataResponse;
import net.smartcosmos.util.UuidUtil;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import javax.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetadataPersistenceService implements MetadataDao {

    public static final int NO_LIMIT_ON_QUERY_RESPONSE_SIZE = 0;

    private final MetadataRepository metadataRepository;
    private final ConversionService conversionService;
    private final SearchSpecifications<MetadataEntity> searchSpecifications = new SearchSpecifications<>();

    @Autowired
    public MetadataPersistenceService(MetadataRepository metadataRepository,
                                      ConversionService conversionService) {
        this.metadataRepository = metadataRepository;
        this.conversionService = conversionService;
    }

    @Override
    public Optional<MetadataResponse> create(String tenantId, MetadataCreate createMetadata)
        throws ConstraintViolationException {

        UUID accountId = UuidUtil.getUuidFromAccountUrn(tenantId);

        List<MetadataEntity> responseList = new ArrayList<>();
        // Using MetadataEntity[] here, spring conversionService does not accept List<MetadataEntity> template
        MetadataEntity[] entities = conversionService.convert(createMetadata, MetadataEntity[].class);

        for (MetadataEntity entity : entities) {
            entity.setTenantId(accountId);
            entity = persist(entity);
            responseList.add(entity);
        }
        MetadataResponse response = conversionService.convert(responseList.toArray(), MetadataResponse.class);
        if (response != null) {
            return Optional.of(response);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MetadataResponse> update(
        String tenantId,
        String ownerType,
        String ownerId,
        String keyName,
        Object value)
        throws ConstraintViolationException {

        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public List<MetadataResponse> delete(String tenantUrn, String ownerType, String ownerUrn, String keyName) {

        UUID accountId = UuidUtil.getUuidFromAccountUrn(tenantUrn);
        List<MetadataEntity> deleteList = new ArrayList<>();

        try {
            UUID ownerId = UuidUtil.getUuidFromUrn(ownerUrn);
            deleteList = metadataRepository.deleteByTenantIdAndOwnerTypeAndOwnerIdAndKey(
                accountId,
                ownerType,
                ownerId,
                keyName);
        } catch (IllegalArgumentException e) {
            // empty list will be returned anyway
            log.warn("Illegal URN submitted: %s by tenant %s", ownerUrn, tenantUrn);
        }

        return deleteList.stream()
            .map(o -> conversionService.convert(o, MetadataResponse.class))
            .collect(Collectors.toList());
    }

    @Override
    public List<MetadataResponse> deleteAllByOwner(String tenantUrn, String ownerType, String ownerUrn) {
        UUID accountId = UuidUtil.getUuidFromAccountUrn(tenantUrn);
        List<MetadataEntity> deleteList = new ArrayList<>();

        try {
            UUID ownerId = UuidUtil.getUuidFromUrn(ownerUrn);
            deleteList = metadataRepository.deleteByTenantIdAndOwnerTypeAndOwnerId(
                accountId,
                ownerType,
                ownerId);
        } catch (IllegalArgumentException e) {
            // empty list will be returned anyway
            log.warn("Illegal URN submitted: %s by tenant %s", ownerUrn, tenantUrn);
        }

        return deleteList.stream()
            .map(o -> conversionService.convert(o, MetadataResponse.class))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Object> findByKeyName(String tenantUrn, String ownerType, String ownerUrn, String key) {

        Optional<MetadataEntity> entity = Optional.empty();
        try {
            UUID tenantId = UuidUtil.getUuidFromAccountUrn(tenantUrn);
            UUID uuid = UuidUtil.getUuidFromUrn(ownerUrn);

            entity = metadataRepository.findByTenantIdAndOwnerTypeAndOwnerIdAndKey(
                tenantId,
                ownerType,
                uuid,
                key);
        } catch (IllegalArgumentException e) {
            // empty Optional will be returned anyway
            log.warn("Illegal URN submitted: %s by account %s", ownerUrn, tenantUrn);
        }

        if (entity.isPresent())
        {
            final MetadataResponse response = conversionService.convert(entity.get(), MetadataResponse.class);
            return Optional.ofNullable(response);
        }
        return Optional.empty();
    }

    @Override
    public Optional<MetadataResponse> findByOwner(
        String tenantUrn,
        String ownerType,
        String ownerUrn,
        Collection<String> keyNames) {

        UUID tenantId = UuidUtil.getUuidFromAccountUrn(tenantUrn);
        UUID uuid = UuidUtil.getUuidFromUrn(ownerUrn);

        if (keyNames.isEmpty()) {
            try {
                List<MetadataEntity> responseList = metadataRepository.findByTenantIdAndOwnerTypeAndOwnerId(
                    tenantId,
                    ownerType,
                    uuid
                );

                MetadataResponse response = conversionService.convert(responseList.toArray(), MetadataResponse.class);
                if (response != null) {
                    return Optional.of(response);
                }
            } catch (IllegalArgumentException e) {
                // empty Optional will be returned anyway
                log.warn("Illegal URN submitted: %s by account %s", ownerUrn, tenantUrn);
            }
        } else {
            try {
                List<MetadataEntity> responseList = new ArrayList<>();
                for (String keyName: keyNames) {
                    Optional<MetadataEntity> entity = metadataRepository.findByTenantIdAndOwnerTypeAndOwnerIdAndKey(
                        tenantId,
                        ownerType,
                        uuid,
                        keyName);
                    if (entity.isPresent()) {
                        responseList.add(entity.get());
                    }
                }
                MetadataResponse response = conversionService.convert(responseList.toArray(), MetadataResponse.class);
                if (response != null) {
                    return Optional.of(response);
                }
            } catch (IllegalArgumentException e) {
                // empty Optional will be returned anyway
                log.warn("Illegal URN submitted: %s by account %s", ownerUrn, tenantUrn);
            }
        }
        return Optional.empty();
    }


    /**
     * Saves an metadata entity in an {@link MetadataRepository}.
     *
     * @param entity the metadata entity to persist
     * @return the persisted metadata entity
     * @throws ConstraintViolationException if the transaction fails due to violated constraints
     * @throws TransactionException if the transaction fails because of something else
     */
    @SuppressWarnings("Duplicates")
    private MetadataEntity persist(MetadataEntity entity) throws ConstraintViolationException, TransactionException {
        try {
            return metadataRepository.save(entity);
        } catch (TransactionException e) {
            // we expect constraint violations to be the root cause for exceptions here,
            // so we throw this particular exception back to the caller
            if (ExceptionUtils.getRootCause(e) instanceof ConstraintViolationException) {
                throw (ConstraintViolationException) ExceptionUtils.getRootCause(e);
            } else {
                throw e;
            }
        }
    }
}
