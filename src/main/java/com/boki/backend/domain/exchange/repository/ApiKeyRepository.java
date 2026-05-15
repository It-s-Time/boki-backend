package com.boki.backend.domain.exchange.repository;

import com.boki.backend.domain.exchange.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByMemberId(Long memberId);

    List<ApiKey> findAllByMemberIdIn(List<Long> memberIds);
}
