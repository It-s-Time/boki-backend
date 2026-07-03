package com.boki.backend.domain.exchange.repository;

import com.boki.backend.domain.exchange.entity.ApiKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByMemberId(Long memberId);

    List<ApiKey> findAllByMemberIdIn(List<Long> memberIds);

    @Query("""
            select apiKey
            from ApiKey apiKey
            where (apiKey.nextTradeSyncAt is null or apiKey.nextTradeSyncAt <= :now)
              and (apiKey.tradeSyncStartedAt is null or apiKey.tradeSyncStartedAt <= :staleStartedBefore)
            order by apiKey.memberApiKeyId asc
            """)
    List<ApiKey> findDueTradeSyncTargets(
            @Param("now") LocalDateTime now,
            @Param("staleStartedBefore") LocalDateTime staleStartedBefore,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query("""
            update ApiKey apiKey
            set apiKey.tradeSyncStartedAt = :startedAt
            where apiKey.memberApiKeyId = :memberApiKeyId
              and (apiKey.tradeSyncStartedAt is null or apiKey.tradeSyncStartedAt <= :staleStartedBefore)
            """)
    int claimTradeSync(
            @Param("memberApiKeyId") Long memberApiKeyId,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("staleStartedBefore") LocalDateTime staleStartedBefore
    );
}
