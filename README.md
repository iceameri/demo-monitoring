                                                                                                                # Demo Monitoring Application
Spring Boot 모니터링 예제입니다.

어느 프로젝트에서든 실무 적용 가능하도록 지향합니다.

로컬 프로젝트에서는 Spring Boot(인텔리제이) + Prometheus(Docker) + Grafana(Docker) 를 실행하는 것으로 계획했습니다.

장애의 기준을 정하고 우선순위를 설계를 목표로 삼았습니다.

`서버 다운 -> 500 에러 -> 레이턴시 -> 외부 서비스 500 에러 -> 리소스 고갈`
을 기본을 했고 추가적으로 AWS와 같은 클라우드 서비스를 이용하기 때문에 네트워크 장애를 추가했습니다.

최근 운영에서 클라우드 서비스 장애가 빈번하게 발생되어 추가하였는데 네트워크 장애의 판단에 대한 기준을 파악할 수 있게 되었습니다.

실행 순서
1. Spring Boot 실행 (옵션 없음)

2. docker 실행\
`/monitoring` 디렉토리에서 `docker-compose up -d` 

3. 접속 및 모니터링 설정
- Prometheus\
http://localhost:9090 -> Status -> Target Health -> State `UP` 확인

- Grafana 대시보드 설정\
http://localhost:3000\
ID/PW: admin/admin\
Dashboards -> import dashboard -> ID: 20727 입력 -> Load

- Alertmanager\
http://localhost:9093

알람 테스트 시나리오
1. Spring Boot 중지
2. /error-test API 호출

## 전체 아키텍쳐
```
Spring Boot (Micrometer)
├─ HTTP / JVM / Custom Metrics
└─ /actuator/prometheus
↓
Prometheus
├─ scrape
├─ alert rules (시나리오 기반)
↓
Alertmanager
├─ service / severity / slo routing
└─ Slack / On-call
```