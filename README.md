# 6. Key-Value Database

6장 안정해시에 대한 구현 과제입니다. [Notion 정리: 6장 키-값 저장소](https://puffy-daisy-806.notion.site/6-080d1a2de78c4ab2901c90f533420d4c?pvs=4)  
담당자: 박상엽(park-sy)  

|Week|Date|Desc|
|------|---|---|
|1주차|24.06.17~|내용 정리 및 설계|
|2주차|24.06.24~|키-값 저장소 기본 기능(get,put) / 정족수 합의 프로토콜(replication, read&write) / docker를 통한 클러스터 구현|
|3주차|24.07.01~|데이터 버저닝(vertor clock) 구현 / 가십 프로토콜 구현|
|4주차|24.07.08~|머클 트리 구현 / 일시적,영구적 장애처리|


### 상세 설계  
```bash.
└── com
    └── zeromh
        └── kvdb
            └── server
                ├── ServerApplication.java
                ├── application
                │   ├── KeyUseCase.java
                │   ├── ServerUseCase.java
                │   └── impl
                │       ├── KeyService.java
                │       └── ServerService.java
                ├── config
                │   ├── ConsistentHashConfig.java
                │   ├── QuorumProperty.java
                │   ├── ServerConfig.java
                │   └── ServerProperty.java
                ├── domain
                │   └── DataObject.java
                ├── infrastructure
                │   ├── network
                │   │   ├── NetworkPort.java
                │   │   └── impl
                │   │       └── RestNetwork.java
                │   └── store
                │       ├── StorePort.java
                │       └── impl
                │           └── MongoRepository.java
                └── interfaces
                    ├── KeyController.java
                    └── ServerController.java

```



- 저장소는 안정 해시 위의 가상 노드로 배치되어 요청에 대한 get/put 연산 두가지를 수행한다.
- 요청은 load balancer(Nginx)를 통해 전달되며, 어느 물리 서버에나 전달될 수 있다.
- 전달 받은 요청에 대한 key가 어느 load balancer가 요청한 서버에 책임이 없을 경우, 책임 서버를 확인한 후 요청을 해당 서버에 전달하여 요청이 이루어진다.
- 정족수 합의 프로토콜을 통해 다중화 및 일관성을 지원한다.
- 가십 프로토콜을 통해 장애를 감지하며, 일시적/영구적 장애를 처리한다.
- 반-엔트로피 프로토콜(머클트리)를 활용하여 사본들을 동기화한다.


### 주요 기능
#### 다중화 지원
- 데이터 저장 시, 미리 설정한 N개의 데이터가 서버에 복제되며, W개의 복제 서버로부터 저장 확인을 받은 후 응답한다.
- 데이터 조회 시, 미리 설정한 N개의 복제 서버에 해당 데이터를 요청하여, R개의 복제 서버로부터 값을 확인한 후 응답한다.
#### 일관성 지원
#### 가십 프로토콜을 통한 서버 상태 확인
#### 머클 트리를 이용한 


### 사용방법
#### 사전 준비
서버로 사용되는 mongodb와 redis의 사용을 위해서 먼저 docker의 설치가 필요하다.  
- [docker 다운로드](https://www.docker.com/products/docker-desktop/)
- shell에 명령어 입력 : docker pull mongo, docker pull mongo


#### 서버 구동 방법
