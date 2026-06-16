package com.server.realsync.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.realsync.entity.CatalogTemplate;

@Repository
public interface CatalogTemplateRepository extends JpaRepository<CatalogTemplate, Integer> {

    List<CatalogTemplate> findByAccountIdOrderByCreatedAtDesc(Integer accountId);

    Optional<CatalogTemplate> findByIdAndAccountId(Integer id, Integer accountId);

    long countByAccountIdAndStatus(Integer accountId, String status);

    List<CatalogTemplate> findByModuleCodeAndAccountIdOrderByCreatedAtDesc(String moduleCode, Integer accountId);

    List<CatalogTemplate> findByModuleCodeAndCategoryAndAccountIdOrderByCreatedAtDesc(String moduleCode, String category, Integer accountId);

    @org.springframework.data.jpa.repository.Query("SELECT t FROM CatalogTemplate t WHERE t.accountId = :accountId " +
            "AND (:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(t.content) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:templateType IS NULL OR LOWER(t.templateType) = LOWER(:templateType)) " +
            "AND (:status IS NULL OR LOWER(t.status) = LOWER(:status)) " +
            "AND (:channel IS NULL OR LOWER(t.channels) LIKE LOWER(CONCAT('%', :channel, '%'))) " +
            "ORDER BY t.createdAt DESC, t.id DESC")
    org.springframework.data.domain.Page<CatalogTemplate> searchAndFilterTemplates(
            @org.springframework.data.repository.query.Param("accountId") Integer accountId,
            @org.springframework.data.repository.query.Param("search") String search,
            @org.springframework.data.repository.query.Param("templateType") String templateType,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("channel") String channel,
            org.springframework.data.domain.Pageable pageable);

    List<CatalogTemplate> findByContentAndAccountId(String content, Integer accountId);
}