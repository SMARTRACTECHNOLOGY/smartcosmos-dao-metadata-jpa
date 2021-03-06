package net.smartcosmos.dao.metadata.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import net.smartcosmos.dao.metadata.domain.MetadataOwnerEntity;

public interface MetadataOwnerRepository extends JpaRepository<MetadataOwnerEntity, UUID>, MetadataOwnerRepositoryCustom {

    Optional<MetadataOwnerEntity> findByTenantIdAndTypeAndId(UUID tenantId, String type, UUID id);

    @Transactional
    List<MetadataOwnerEntity> deleteByTenantIdAndTypeAndId(UUID tenantId, String type, UUID id);
}
