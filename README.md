# 6. Key-Value Database

6장 키-값 저장소에 대한 구현 과제입니다.  
[Notion 정리: 6장 키-값 저장소](https://puffy-daisy-806.notion.site/6-080d1a2de78c4ab2901c90f533420d4c?pvs=4)  
담당자: 박상엽(park-sy)  

|Week|Date|Desc|
|------|---|---|
|1주차|24.06.17~|내용 정리 및 설계|
|2주차|24.06.24~|키-값 저장소 기본 기능(get,put) / 정족수 합의 프로토콜(replication, read&write) / docker를 통한 클러스터 구현|
|3주차|24.07.01~|데이터 버저닝(vertor clock) 구현 / 가십 프로토콜 구현|
|4주차|24.07.08~|일시적,영구적 장애처리|
|4주차|24.07.15~|머클 트리 구현 및 문서화|

### 개요
해당 프로젝트에서는 키-값 저장소는 BASE를 기반으로하여 관리자 없이 서로 상호작용하며 동작하는 DB를 의미한다.
PUT, GET에 대한 간단한 연산과 데이터 다중화, 일관성, 상호 감시, 데이터 정합성 확인 등을 지원한다.

### 상세 설계  
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
- shell에 명령어 입력하여 필요한 application을 다운로드한다.(grafana와 influxdb는 선택사항)
```bash.
$ docker pull mongo
$ docker pull nginx
$ docker pull grafana
$ docker pull influxdb/influxdb
```

#### 서버 구동 방법
- 프로젝트 .jar로 빌드
```bash.
$ ./gradlew build
```

- docker-compose 실행
```bash.
$ docker-compose -f docker-compose-local.yml up
```
<img src = "https://github.com/user-attachments/assets/9fc2a343-6098-428f-a04e-462ef3644de2" width="60%" height="60%">

#### GET 요청 
- GET: /key/{key}
- request url example: /key/6

#### PUT 요청 
- POST: /key
- request url example: /key
- request body:
``` json.
{
    "key": "key",
    "value": "value"
}
```


### 결과
Grafana와 InfluxDB를 통해 각 기능에 대한 모니터링을 구축하였으며, 이를 통해서 결과를 확인하였다.
#### Test Set
- 클러스터는 6개의 서버로 구성되었으며 각 서버는 4개의 가상노드를 생성하여 해시링에 배치한다.
- 정족수 합의 프로토콜에서 사본의 개수(N) 3, 쓰기 연산 정족수(W) 2, 읽기 연산 정족수(R) 2로 설정하여 최종 일관성을 지원한다.
- 

#### 다중화 지원
![key-put-high](https://github.com/user-attachments/assets/4019d091-9961-4e5a-997c-6a9d78456a42)  
PUT 요청 시 3개의 서버에서 데이터가 저장되는 모습을 확인할 수 있다.

#### 일관성 지원
![quorum-put-high](https://github.com/user-attachments/assets/4369258c-07ad-4f57-bbbd-e8b32486a91b)  
(N=3, W=2)이므로 PUT 요청이 3개의 서버로 전달되는 것을 확인할 수 있고, 요청 처리를 담당하는 서버는 2개의 요청에 대한 응답을 받아 처리하는 모습을 확인할 수 있다.

![quorum-get-high](https://github.com/user-attachments/assets/a0e68f2b-bd67-45f7-9e3a-ef60f1785d7a)
(N=3, R=2)이므로 GET 요청이 3개의 서버로 전달되는 것을 확인할 수 있고, 요청 처리를 담당하는 서버는 2개의 요청에 대한 응답을 받아 처리하는 모습을 확인할 수 있다.

#### 가십 프로토콜을 통한 서버 상태 확인 지원
![gossip-middle](https://github.com/user-attachments/assets/42855ba1-9d71-4f31-85a1-c3e3bc02bb7c)  
Membership이 지속적인 업데이트가 되는 것을 확인할 수 있으며, 일시 장애 서버를 제외한 다른 서버들의 상태에 일시 장애 상태가 표시된다.

#### 일시 장애 시 임시 위탁 기능을 통한 데이터 관리
![handoff-temporary-high](https://github.com/user-attachments/assets/191da1f2-36fb-49a4-a35d-afe3ff4cb270)


![handoff-failover-high](https://github.com/user-attachments/assets/d3f6c982-3c06-4d2a-b3d9-c8b1cced18c3)


#### 머클 트리를 이용한 데이터 정합성 확인
