# 03주차 정리

## 학습 주제

### Git 기본 사용법

Git을 활용한 소스코드 버전 관리 및 협업 프로세스를 학습하였다.

---

### Git 명령어

- git init
- git clone
- git commit
- git push
- git pull
- git branch
- git checkout
- git merge
- git rebase

Git 저장소 생성부터 협업, 브랜치 관리 및 병합 전략까지 학습하였다.

---

### Git Flow

대규모 프로젝트에서 사용하는 브랜치 전략을 학습하였다.

브랜치 종류

- Master
- Develop
- Feature
- Release
- Hotfix

---

### GitHub Flow

GitHub 기반 협업 프로젝트에서 사용하는 단순 브랜치 전략을 학습하였다.

브랜치 종류

- Main
- Feature Branch
- Pull Request

---

### Pull Request(PR)

코드를 메인 브랜치에 병합하기 전에 코드 리뷰 및 검토를 수행하는 협업 프로세스를 학습하였다.

---

# 핵심 개념

## git init

현재 디렉토리를 Git 저장소로 초기화한다.

```bash
git init
```

---

## git clone

원격 저장소를 로컬 환경으로 복제한다.

```bash
git clone <repository-url>
```

---

## git commit

변경사항을 로컬 저장소에 저장한다.

```bash
git commit -m "회원가입 기능 추가"
```

---

## git push

로컬 저장소의 변경사항을 원격 저장소에 업로드한다.

```bash
git push origin feature/daoul
```

---

## git pull

원격 저장소의 최신 변경사항을 가져온다.

```bash
git pull origin main
```

---

## git branch

새로운 작업 공간(브랜치)을 생성한다.

```bash
git branch feature/daoul
```

---

## git checkout

작업할 브랜치로 이동한다.

```bash
git checkout feature/daoul
```

---

## git merge

브랜치를 병합한다.

```bash
git merge feature/daoul
```

---

## git rebase

커밋 히스토리를 정리하면서 브랜치를 재배치한다.

```bash
git rebase develop
```

### 주의사항

이미 원격 저장소에 Push된 커밋은 Rebase를 사용하지 않는 것이 일반적이다.

---

# Git Flow

## 목적

대규모 프로젝트에서 기능 개발, 배포, 긴급 수정 작업을 체계적으로 분리하기 위한 브랜치 전략이다.

---

## 브랜치 구조

### Master

운영 환경(Production)에 배포되는 안정적인 코드

---

### Develop

다음 버전을 준비하는 통합 개발 브랜치

---

### Feature

새로운 기능 개발

예시

```text
feature/user-service
feature/catalog-service
feature/timedeal-service
```

---

### Release

배포 전 최종 검증

---

### Hotfix

운영 환경 긴급 장애 수정

---

# GitHub Flow

## 목적

단순하고 빠른 개발 사이클을 위한 브랜치 전략이다.

---

## 브랜치 구조

### Main

항상 배포 가능한 상태 유지

---

### Feature Branch

기능 단위 개발

예시

```text
feature/daoul
```

---

### Pull Request

코드 리뷰 후 Main 브랜치 병합

---

# Git Flow vs GitHub Flow

| 항목 | Git Flow | GitHub Flow |
|--------|--------|--------|
| 복잡도 | 높음 | 낮음 |
| 프로젝트 규모 | 대규모 | 중소규모 |
| 릴리즈 관리 | 명확함 | 단순함 |
| 브랜치 수 | 많음 | 적음 |
| 개발 속도 | 상대적으로 느림 | 빠름 |

---

# 프로젝트 적용

## 프로젝트 주제

### 우리동네 기반 타임딜 공동구매 플랫폼

본 프로젝트는 여러 명의 팀원이 동시에 작업하는 협업 프로젝트이므로 Git 기반 브랜치 전략이 필수적이다.

---

## 브랜치 전략 적용

현재 프로젝트에서는 GitHub Flow 방식에 가까운 구조를 사용한다.

예시

```text
main

├── feature/daoul
├── feature/aws
├── feature/container
├── feature/jira
└── feature/tdd
```

각 팀원은 자신의 브랜치에서 작업을 수행한다.

---

## 협업 방식

### 1단계

기능 브랜치 생성

```bash
git checkout -b feature/daoul
```

---

### 2단계

개발 진행

예시

- User Service 구현
- GraphQL Resolver 작성
- DTO 설계

---

### 3단계

Commit

```bash
git commit -m "User Service 회원가입 기능 구현"
```

---

### 4단계

Push

```bash
git push origin feature/daoul
```

---

### 5단계

코드 리뷰 및 병합

Pull Request 생성 후 Main 브랜치에 병합

---

# User Service 적용

## 담당자

최다울

---

## 담당 서비스

```text
User Service

- 회원가입
- 로그인
- 사용자 인증
- GraphQL Query
```

---

## 브랜치 관리

User Service 관련 코드는 별도 Feature Branch에서 개발한다.

```text
feature/daoul
```

이를 통해 다른 팀원의 작업과 충돌을 최소화한다.

---

## 실제 개발 흐름

```text
UserRegisterDto 작성

↓

UserService 작성

↓

UserQueryResolver 작성

↓

Commit

↓

Push

↓

Pull Request

↓

Main 병합
```

---

## 협업 시 주의사항

### Main 브랜치 직접 수정 금지

모든 기능 개발은 Feature Branch에서 진행한다.

---

### 충돌 최소화

자신이 담당한 서비스 범위만 수정한다.

예시

```text
최다울
→ User Service

김민상
→ AWS Diagram

남궁연제
→ Container
```

---

### Pull 전 최신 코드 확인

```bash
git pull origin main
```

최신 코드를 반영한 후 작업한다.

---

# README 반영 여부

## 반영 완료

- Git 활용 방식
- Feature Branch 전략
- GitHub 협업 방식
- 역할 분담
- Jira 기반 협업 구조

---

## 추가 반영 가능

### 브랜치 전략

```markdown
본 프로젝트는 GitHub Flow 기반 협업 방식을 적용하였다.

각 팀원은 Feature Branch에서 독립적으로 개발을 진행하고 Pull Request를 통해 Main 브랜치에 병합하였다.
```

---

# 발표 예상 질문

## Q1. Git을 사용한 이유는 무엇인가요?

### 답변

소스코드 버전 관리와 협업을 효율적으로 수행하기 위해 사용하였습니다.

---

## Q2. 브랜치를 왜 사용하나요?

### 답변

기능별로 독립적인 개발 공간을 제공하여 충돌을 최소화하고 안정적인 협업 환경을 구축하기 위해 사용합니다.

---

## Q3. Git Flow와 GitHub Flow의 차이는 무엇인가요?

### 답변

Git Flow는 Release, Hotfix 등 다양한 브랜치를 활용하여 대규모 프로젝트에 적합하고, GitHub Flow는 Main과 Feature Branch 중심의 단순한 구조로 빠른 개발에 적합합니다.

---

## Q4. 현재 프로젝트는 어떤 전략을 사용했나요?

### 답변

Feature Branch를 활용한 GitHub Flow 방식에 가깝게 협업을 진행하였습니다.

---

## Q5. User Service 개발 시 충돌은 어떻게 방지했나요?

### 답변

feature/daoul 브랜치에서 독립적으로 개발하고, 담당 범위(User Service) 외 파일 수정은 최소화하여 충돌을 방지하였습니다.

---

# 실무 관점

실제 기업에서는 단순히 코드를 작성하는 것보다 브랜치 전략을 통한 협업 관리가 매우 중요하다.

특히 여러 명의 개발자가 동시에 작업하는 환경에서는 Feature Branch와 Pull Request 기반 개발 방식이 사실상 표준으로 사용된다.

---

# 한 줄 요약

3주차에서는 Git의 핵심 명령어와 브랜치 전략(Git Flow, GitHub Flow)을 학습하였으며, 이를 실제 프로젝트의 User Service 개발 및 팀 협업 과정에 적용하였다.