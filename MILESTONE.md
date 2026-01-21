Spring boot + Prometheus + Grafana 

Spring boot 는 로컬에서 실행
Prometheus 와 Grafana 는 Docker-compose 구성

핵심 포인트
- Spring Boot는 메트릭만 노출
- Prometheus가 주기적으로 수집
- Grafana는 시각화만 담당
- 운영에서는 Docker / Kubernetes로 거의 동일하게 확장 가능

핵심 지표
- TPS
- 평균/최대 응답시간
- HTTP 4xx / 5xx
- JVM Heap / GC
- Thread 수

운영 확장 시 구조 변화

|단계|변경점|
|---|-----|
|서버 분리|Prometheus 서버 독립|
|멀티 인스턴스|instance 라벨 사용|
|알람|Alertmanager 추가|
|보안|actuator IP 제한|
|K8s|ServiceMonitor 사용|

네트워크 장애 판단 로직

| 패턴                   | 판단      |
| -------------------- | ------- |
| Up=0                 | 앱 다운    |
| Up=1, RPS≈0, Error=0 | 🔥 네트워크 |
| Up=1, RPS↓, Error↑   | 앱/의존성   |
| 특정 URI만 0            | 상위 서비스  |
| Healthcheck만 0       | LB 문제   |

*RPS = Request Per Second

Grafana 대시보드에 추가할 패널

| 패널                  | 이유           |
| ------------------- | ------------ |
| RPS                 | 트래픽 단절 확인    |
| Error Rate          | 네트워크 vs 앱 구분 |
| Up                  | 프로세스 상태      |
| Health Endpoint RPS | LB 이슈        |
| External API RPS    | 외부 네트워크      |
