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

Alertmanager 전체 구조
```
Spring Boot
└─ /actuator/prometheus
↑
│ scrape
│
Prometheus
└─ alert rules
↓
Alertmanager
└─ Slack / Email / Webhook
```
✔️ Prometheus는 판단만
✔️ Alertmanager가 알림 책임
