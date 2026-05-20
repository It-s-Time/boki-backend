package com.boki.backend.domain.exchange.entity;

import com.boki.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "api_key",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_api_key_member_id", columnNames = "member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey extends BaseEntity {

    @Id
    @Column(name = "member_api_key_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberApiKeyId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "access_key", nullable = false, length = 255)
    private String accessKey;

    @Column(name = "secret_key", nullable = false, length = 1000)
    private String secretKey;

    @Builder
    private ApiKey(Long memberId, String accessKey, String secretKey) {
        this.memberId = memberId;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public void update(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }
}
