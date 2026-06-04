# 무엇을 해야할까?

### 📍  기술 적용도 (25점) : 25 / 25 (Kotlin 비동기 Coroutine 및 MSA 클라우드 아키텍처 적용)
### 📍  이슈 관리   (15점) : 15 / 15 (Jira 에픽 및 태스크 연동 관리 완료)
### 📍  TDD         (10점) : 10 / 10 (Spring Boot Test 기반 비동기 코루틴 단위 테스트 환경 구축 주도)
### 📍  트러블슈팅  (10점) : 10 / 10 (인프라 병목 해소를 위한 Cache-Aside 패턴 검증 완료)
### 📍  문서 구성   (15점) : 15 / 15 (GitHub README 내 인프라 아키텍처 및 TDD 명세서 완비)
### 📍  창의성       (5점) :  5 /  5 (하이퍼로컬 선착순 타임딜 공동구매 플랫폼 비즈니스 모델 설계)
### 📍  협업 과정   (10점) : 10 / 10 (Git 브랜치 전략 및 Jira 칸반 보드 실시간 동기화)
### 📍  발표 및 리뷰 대응   (10점) : 10 / 10 (인프라 계층 설계 및 비동기 비즈니스 로직 기술 방어 완료)

## 선택해볼만한 일

```md
기술 적용도 (25점) : 
이슈 관리   (15점) : 
TDD         (10점) : 
트러블슈팅  (10점) : 
문서 구성   (15점) : 
창의성       (5점) : 
협업 과정   (10점) : 
발표 및 리뷰 대응   (10점) : 
```

### 적용 가능 기술

- 협업 도구 관리 및 작성
    - git / github (3w)
        - 문서 작성 (log, 아키텍쳐 설명 등 완료)
        - GitHub README.md 메인 뷰포트에 최종 조감도 및 아키텍처 사양 문서 자산화

    - Jira (11w)
        - 기획 (epic 작성 완료)
        - 요구사항 정의서 기반 에픽/태스크 분할 및 일감 번호(예: KAN-21) 연동 관리
        
    - AWS archetecture diagram (12w)
        - MSA구조로 작성 완료 (Route 53 - ALB - API Gateway - MSA App - Cache/DB - S3/CloudWatch 전체 라우팅 맵 구축)

- 개발?
    - 환경 설정
        - docker / docker-compose (9w, 10w) 완료
        
    - Fromtend
        - language
        - library
        - framework
        - data 관리
            - in-memory
            - disk-base
            
    - Backend
        - language
            - java/kotlin (2w) 채택 및 표준화
                - Coroutine (7w) 기반 비동기 논블로킹 로직 전 계층 적용 완료
        - library
        - framework
            - spring / spring boot (+a)
                - TDD (13w) 구현 프로세스 주도
        - API (5w)
            - RESTful API 아키텍처 표준 수립
            
        - DBMS (4w)
            - rdbms: Amazon RDS (MySQL) 도입을 통한 데이터 트랜잭션 정합성 보장
            
            - nosql
                - in-memory (redis): Amazon ElastiCache (Redis) 전면 배치를 통한 선착순 대기열 및 세션 캐싱 최적화
                - disk-base-database (mongoDB)
                - in-memory (redis)

## 추가 점수 받기

### 가점 기준
```md
발표자           (5점) : 
기술 설계 주도  (10점) : 
TDD 작성 주도   (10점) : 
테스트 기여     (10점) : 
코드 품질 기여  (10점) : 
발표 자료 제작   (5점) : 
문서 기여        (5점) : 
```

### 감점 기준
- 최대한 받지 말자
```md
참여율 저조         (-15 < x < -5) : 
기여도 부족         (-15 < x < -5) : 
무임승차   (팀 점수 제외)(-30 < x) : 
```
- 같이 있어주며 응원하는것도 기여이다.
