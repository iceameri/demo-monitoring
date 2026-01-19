# Demo Monitoring Application
Spring Boot 모니터링 예제입니다.

어느 프로젝트에서든 실무 적용 가능하도록 지향합니다.

로컬 프로젝트에서는 Spring Boot(인텔리제이) + Prometheus(Docker) + Grafana(Docker) 를 실행하는 것으로 계획했습니다. 

실행 순서
1. Spring Boot 실행 (옵션 없음)

2. docker 실행\
`/monitoring` 디렉토리에서 `docker-compose up -d` 

3. 접속 및 모니터링 설정

- Prometheus
http://localhost:9090 -> Status -> Target Health -> State `UP` 확인

- Grafana 대시보드 설정 
http://localhost:3000
admin/admin
Dashboards -> import dashboard -> ID: 20727 입력 -> Load


