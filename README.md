# 6. Key-Value Database

6장 안정해시에 대한 구현 과제입니다. [Notion 정리: 6장 키-값 저장소](https://puffy-daisy-806.notion.site/6-080d1a2de78c4ab2901c90f533420d4c?pvs=4)  
담당자: 박상엽(park-sy)  

|Week|Date|Desc|
|------|---|---|
|1주차|24.06.17~|내용 정리 및 설계|
|2주차|24.06.24~|키-값 저장소 기본 기능(get,put) / 정족수 합의 프로토콜(replication, read&write) / docker를 통한 클러스터 구현|
|3주차|24.07.01~|데이터 버저닝(vertor clock) 구현 / 가십 프로토콜 구현|
|4주차|24.07.08~|일시적,영구적 장애처리|
|4주차|24.07.15~|머클 트리 구현 및 문서화|


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

![image](https://github.com/user-attachments/assets/f66226be-ab1f-42df-8201-884b140a60cc)


- 저장소는 안정 해시 위의 가상 노드로 배치되어 요청에 대한 get/put 연산 두가지를 수행한다.
- 요청은 load balancer(Nginx)를 통해 전달되며, 어느 물리 서버에나 전달될 수 있다.
- 전달 받은 요청에 대한 key가 어느 load balancer가 요청한 서버에 책임이 없을 경우, 책임 서버를 확인한 후 요청을 해당 서버에 전달하여 요청이 이루어진다.
- 정족수 합의 프로토콜을 통해 다중화 및 일관성을 지원한다.
- 가십 프로토콜을 통해 장애를 감지하며, 일시적/영구적 장애를 처리한다.
- 반-엔트로피 프로토콜(머클트리)를 활용하여 사본들의 정합성을 검증한다.


### 주요 기능
#### 다중화 지원
<img src = "https://github.com/user-attachments/assets/c8b7c638-c805-476c-a92c-f8e7436c78e9" width="60%" height="60%">

- 안정해시를 이용하여 데이터를 서버에 저장하며, 해시링 상에서 만나는 최초 N개에 서버에 데이터를 저장하여 다중화한다.

#### 일관성 지원
<img src = "https://github.com/user-attachments/assets/35632c1a-3cfe-4f95-81a4-fd0c04daa52f" width="60%" height="60%">

- 정족수 합의 프로토콜을 이용하여 최종 일관성을 지원한다.
- 데이터 저장 시, 미리 설정한 N개의 데이터가 서버에 복제되며, W개의 복제 서버로부터 저장 확인을 받은 후 응답한다.
- 데이터 조회 시, 미리 설정한 N개의 복제 서버에 해당 데이터를 요청하여, R개의 복제 서버로부터 값을 확인한 후 응답한다.  
<img src = "https://github.com/user-attachments/assets/24474359-059c-4142-bf81-ac52be13407c" width="60%" height="60%">

- 데이터의 충돌을 대비하여, Vector Clock을 이용하여 데이터의 버전을 관리하고 충돌을 해결한다.

#### 가십 프로토콜을 통한 서버 상태 확인 지원
<img src = "https://github.com/user-attachments/assets/4cd6538c-d702-49ff-85c7-9be540fa848d" width="60%" height="60%">

- 5초마다 heartbeat와 timestamp를 업데이트하고 임의로 상태 정보를 전송한다.
- 서버가 heatheat가 미리 지정한 시간보다 오랫동안 업데이트 되지 않았으면, 해당 서버의 상태를 일시 장애 상태로 변경한다.
- 서버가 heatheat가 미리 지정한 시간보다 오랫동안 업데이트 되지 않았고, 일시 장애 상태라면, 서버를 영구 장애 상태에 있다고 보고 서버 정보를 제거한다.
- 일시 장애에서 회복한 서버가 업데이트 된 상태를 보내면, 전파받은 서버는 일시 장애 상태를 정상 상태로 변경한다.

#### 일시 장애 시 임시 위탁 기능을 통한 데이터 관리
<img src = "https://github.com/user-attachments/assets/2f61bc39-e913-4076-a4bb-daa1019b7acc" width="60%" height="60%">

- Put 연산을 처리하는 N개의 서버 중 일시 장애 서버가 있다면, 위탁 서버 정보를 가져온 뒤에, 해당 서버에 데이터를 임시로 저장한다.
- 일시 장애에서 회복한 서버는 서버 전송 후, 자신의 상태가 일시 장애 상태에 있었다는 것을 확인하면 모든 서버에 위탁한 데이터를 가져온다.

#### 머클 트리를 이용한 데이터 정합성 확인
<img src = "https://github.com/user-attachments/assets/f46e436a-6191-45a1-aea1-7bf38a7ef1fd" width="60%" height="60%">
- 데이터 저장 시, 머클 트리를 생성하고, 해시 값을 관리한다.
- 일시 장애 복구 후 임시 위탁한 데이터를 가져오면 다시 전체 머클 트리의 해시값을 계산하고, 모든 서버와 머클 트리의 값을 비교한다.
- 머클 트리의 값이 다르다면, 데이터가 일치하지 않는 key를 가져온다.


### 사용방법
#### 사전 준비
서버로 사용되는 mongodb와 redis의 사용을 위해서 먼저 docker의 설치가 필요하다.  
- [docker 다운로드](https://www.docker.com/products/docker-desktop/)
- shell에 명령어 입력 : docker pull mongo, docker pull mongo


#### 서버 구동 방법
