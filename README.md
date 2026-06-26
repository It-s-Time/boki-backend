# 보키(BOKI) Backend



## 서비스 소개

<table>
  <tr>
    <td><img src="https://avatars.githubusercontent.com/u/269872241?s=400&u=2e9abc9e50df8e3849a87cfc5dc139dd216fa6a4&v=4" width="550"/></td>
    <td>
      <b>BOKI</b>는 코인 투자자가 매 거래 경험을 놓치지 않고 자신의 실력으로 바꿀 수 있도록 돕는 <b>초간편 코인매매 복기 도우미</b>입니다.<br/><br/>
      단순한 기록을 넘어 더 나은 투자를 위한 AI의 맞춤형 조언과 개선 방향을 제공하며, 투자자가 매매 직후의 생생한 감정을 아주 짧고 쉽게 기록하게 함으로써 자신의 투자 원칙을 되새기게 하고, 이를 바탕으로 AI와 함께 다음 전략을 고민하며 실수를 줄여나가는 과정을 돕습니다.
    </td>
  </tr>
</table>

### 핵심 기능

| 기능 | 설명 |
|---|---|
| 기록 자동화 | 업비트 API 연동으로 체결내역을 자동 수집, 기록 노동 없이 복기에만 집중 |
| 매매원칙 관리 | 3가지 추전 원칙 프리셋 또는 직접 작성한 커스텀 원칙으로 나만의 매매원칙 세트 구성 및 관리 |
| 원칙 기반 복기 | 원칙별 1~5점 준수 체크와 메모/사진 첨부로 매매 직후 짧은 시간 내에 완료하는 초간편 회고 서비스 |
| AI 분석 리포트 | 준수율 기반으로 S-F 등급과 잘한 점/아쉬운 점 피드백, 맞춤 원칙을 AI가 자동으로 제안 |
| 통계 인사이트 | 잘 지킨/못 지킨 원칙 랭킹과 등급 분포를 시각화하여 데이터 기반으로 투자 습관을 교정 |

---

## Contributors

| **강서현** | **유용선** |
|:---:|:---:|
| [<img src="https://avatars.githubusercontent.com/u/180945392?v=4" width="150"><br/>seohyunk09](https://github.com/seohyunk09) | [<img src="https://avatars.githubusercontent.com/u/165531847?v=4" width="150"><br/>Yongseon-Yoo](https://github.com/Yongseon-Yoo) |
| Backend | Backend |

---

## 아키텍처 구조

### 도메인 아키텍처 (DDD)

```
src/main/java/com/boki/backend
├── domain
│   ├── ai          # AI 분석 리포트
│   ├── auth        # 소셜 로그인 / 인증
│   ├── member      # 회원
│   ├── ruleset     # 매매원칙 세트
│   ├── trade       # 거래 내역
│   └── review      # 복기
└── global
    ├── auth        # JWT / 인증 공통
    ├── config      # 설정
    ├── entity      # 공통 엔티티 (BaseEntity)
    ├── exception   # 공통 예외 처리
    └── response    # 공통 응답 포맷
```

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.14 |
| Build | Gradle |
| Database | MySQL |
| ORM | Spring Data JPA (Hibernate) |
| Infra | AWS EC2, AWS S3 |
| CI/CD | GitHub Actions, Docker, GHCR |
| API 문서 | Swagger (SpringDoc OpenAPI) |
| AI 코드 리뷰 | CodeRabbit |

---



### 커밋

| 태그 | 설명 |
|---|---|
| feat | 기능 추가 |
| refactor | 기능 변경 없이 코드 변경 |
| fix |  버그 수정 |
| docs | 문서 수정 |
| chore | 수정, 변경 이외의 작업 |
| remove | 코드 및 파일 삭제 |
