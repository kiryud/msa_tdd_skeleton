# msa_tdd_skeleton

```
1조

main idea : 타임딜 공동구매 플랫폼
```

## 기획 문서

### Convention

- [git convention](./convention/git_convention.md)
- [jira convention](./convention/jira_convention.md)
- [pseudocode convention](./convention/pseudocode_convention.md)


## 시스템 아키텍쳐

> 타임딜 공동구매 플랫폼


### 구조 설계
micro service
```md
user : 최다올
creation : 추형우
intergration : 추형우
catalog : 김수연
timedeal : 김민상
order : 남궁연제
notification : 남궁연제
```

## 역할 분담 내용

### 팀장
- 정진석
	- 프로젝트 해석 및 기초 자료 문서화
	- 프로젝트 진행 관리
	- 역할 분리 및 할당
	- 최종 검수


### 팀원

|name|main task|micro service|idea|
|:---:|:---:|:---:|:---:|
|김수연|서비스 개발(설계, TDD)|catalog service|독서실 공간 예약|
|남궁연제|container 관리, 서비스 개발(설계, TDD)|order service / anounce-service|null|
|김민상|AWS 다이어그램 작성, 서비스 개발(설계, TDD)|TimeDeal Participation Service|출결, 과제 관리 플랫폼|
|최다울|서비스 개발(설계, TDD)|user service|타임딜 공동구매 플랫폼|
|윤혁주|Jira 관리|null|Book Bridge|
|장찬범|Jira 관리|null|PayMap|
|추형우|서비스 개발(설계, TDD)|deal creation / integration|WaitQ|
|이효원|발표 자료 제작, 프로젝트 문서화|null|medi care|


## 문제 상황 및 해결 내용

1. 작업에서 요구되는 사항에 대하여 팀원들에게 숙지시켜야했다.
	- 해당 프로젝트의 요구사항에 대한 해설안 작성
	- convention 작성을 통해 기본적인 틀 제작
2. 모두가 개별적으로 참여 가능한 사항(개발, TDD)도 있지만 그렇지 않은 경우도 있었다.
	- 개발, TDD 실습을 위해 마이크로 서비스를 구성하는 기본적인 틀 제작
		- 개인적으로 개발할 때 고려하는 순서대로 구조화
		- 해당 요소가 어떤 주차에서 학습했는지 기록
		- 각 영역에 대한 질문이 들어왔을 때 최대한 상세하게 답변
	- 그렇지 않은 영역에서 역할을 나누었다.
		- container ochestration
			- docker-compose.yml으로만 작성해보는 역할로 구성했다
			- nginx용 config 파일, dbms를 위한 init.sql 등과 세부적인 쉘 스크립트의 경우는 제외했다
		- 발표자료 제작 및 기록 문서화
		- Jira에 epic, task 등록
			- (원래 설계 이후 이걸 등록하면서 진행되어야하지만 개발 과정이 생략되어 문서 작성만 하는 흐름상 만들어지는 업무량이 너무 많아 각자 마이크로 서비스 구상 및 구현 후 그 자료를 jira 담당에게 보내 등록하고 할당하는 방식으로 진행하였다)
3. 준비할때는 팀장인 내가 혼자 준비할 수 있었지만 8명이 3가지 종류의 팀 (Jira, diagram, micro service)로 쪼개져 각자 토의하고 진행할 때 다른 주제에 관하여 설명해야했다
	- jira 팀
		- 해당 팀은 jira만 도맡아 하기로 했다
		- epic, task 등록을 쉽게 하는 목록에서의 생성방식을 알려주었다
		- 다른 업무 내역을 중간중간 공유받아 추가받을 수 있게 연결해주었다
	- diagram 팀
		- 프로젝트 설계 기반으로 aws 기능을 활용하되 docker-compose를 활용하고 계속 생성중인 micro service를 활용하도록 안내했다
	- micro service
		1. 어떤 시점에 어떤 이유로 서비스를 쪼갤지 논의가 생겼다
			- (교수님의 조언을 통해 내린 결론) 일반적으로 모놀리식으로 쓰다 "불편해서" 분리하겠지만 이 과제에선 실제 개발이 아닌 체험이니만큼 분리를 하지 않을 이유가 없다.
		2. 타임딜과 공구를 같은것으로 두고 논의해서 오류가 생겼다
			- 타임딜 : 수량 제한 (max), 시간 제한 (타임딜 제공 업체와의 협의에 따라 진행해야함 - 시간 지나도 물량 다 팔리기 전까지 혜택 제공하는 경우가 존재)
			- 공구 : 수량 제한 (min), 시간 제한 (주문을 위해선 엄격)
