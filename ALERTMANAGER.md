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
️️✔️ PromQL에서 service 기준 필터링
️️✔️ Alert labels는 Alertmanager routing 기준
️️✔️ 상단 route → 하위 routes 순서로 평가됨
️️✔️ 매칭되면 하위 route로 전달
️️✔️ 팀 + 심각도까지 분리
️️✔️ 알람 수신 즉시 누구 서비스인지 인지 가능

SLO 기반 알람

SLO / SLI / Error Budget 핵심 정리

|개념| 의미                   |
|---|----------------------|
|SLI| 측정 지표 (예: 성공 요청 비율)  |
|SLO| 목표 (예: 99.9%)        |
|Error Budget| 실패 허용치 (0.1%)        |

예제 서비스 SLO 정의

|항목| 값     |
|---|-------|
|기간| 30일   |
|SLO| 99.9% |
|Error Budget| 0.1%  |
|허용 실패율| 0.001 |

SLI 정의
- 성공 요청 = 전체 요청 - 5xx
```promql
# 전체 요청
sum(rate(http_server_requests_seconds_count{
  service="demo-monitoring-service"
}[5m]))
-
# 실패 요청 (5xx)
sum(rate(http_server_requests_seconds_count{
  service="demo-monitoring-service",
  status=~"5.."
}[5m]))
```

Error Ratio
- 실패 요청 (5xx) / 전체 요청
```promql
# 실패 요청 (5xx)
(
  sum(rate(http_server_requests_seconds_count{
    service="demo-monitoring-service",
    status=~"5.."
  }[5m]))
)
/
# 전체 요청
(
  sum(rate(http_server_requests_seconds_count{
    service="demo-monitoring-service"
  }[5m]))
)
```
Error Budget Burn Rate 개념
Burn Rate = 현재 에러율 / 허용 에러율

```promql
(
  sum(rate(http_server_requests_seconds_count{
    service="demo-monitoring-service",
    status=~"5.."
  }[5m]))
)
/
(
  sum(rate(http_server_requests_seconds_count{
    service="demo-monitoring-service"
  }[5m]))
)
/
0.001
```

알람 이중 구조

|알람| 의미              |
|---|-----------------|
|Fast Burn| 대규모 장애, 즉각 대응   |
|Slow Burn| 잠재 장애, 근무 시간 대응 |

운영 배포 흐름
```aiignore
배포 시작
  ↓
Silence 생성
  ↓
배포 수행
  ↓
헬스체크
  ↓
Silence 제거
```


# 실무에서 자주 터지는 5대 장애 시나리오

| # | 시나리오           | 실제 영향 |
| - |----------------| ----- |
| 1 | 서비스 전체 다운      | 즉시 장애 |
| 2 | 부분 실패 (5xx 증가) | 기능 장애 |
| 3 | 응답 지연          | 체감 장애 |
| 4 | 외부 의존성 장애      | 연쇄 장애 |
| 5 | 리소스 고갈         | 곧 장애  |

## 시나리오 1: 서비스 전체 다운
- 배포 실패
- 프로세스 크래시

알람 조건
```promql
- alert: DemoMonitoringServiceAllDown
  expr: sum(up{service="demo-monitoring-service"}) == 0
  for: 30s
  labels:
    severity: critical
    service: demo-monitoring-service
  annotations:
    summary: "[DemoMonitoring] 서비스 전체 다운"
    runbook: "재기동 또는 롤백"
```

대응
1. 배포 여부 확인
2. 최근 로그 확인
3. 롤백 or 재기동

*인스턴스 하나 죽음은 알람 아님

## 시나리오 2: 부분 실패
- 특정 API 버그
- DB 쿼리 실패

알람 조건
```promql
- alert: DemoMonitoringPartialFailure
  expr: |
    (
      sum(rate(http_server_requests_seconds_count{
        service="demo-monitoring-service",
        status=~"5.."
      }[5m]))
      /
      sum(rate(http_server_requests_seconds_count{
        service="demo-monitoring-service"
      }[5m]))
    ) > 0.05
  for: 2m
  labels:
    severity: warning
    service: demo-monitoring-service
  annotations:
    summary: "[DemoMmonitoring] 부분 장애 감지"
    runbook: "실패 API / DB 상태 확인"
```
대응
- 에러 나는 API 확인
- 최근 배포 확인
- 기능 제한 or 핫픽스

## 시나리오 3: 응답 지연
- DB 슬로우 쿼리
- Thread 고갈

알람 조건
```promql
- alert: DemoMonitoringHighLatency
  expr: |
    histogram_quantile(
      0.95,
      sum(rate(http_server_requests_seconds_bucket{
        service="demo-monitoring-service"
      }[5m])) by (le)
    ) > 1
  for: 3m
  labels:
    severity: warning
    service: demo-monitoring-service
  annotations:
    summary: "[DemoMonitoring] 응답 지연 증가"
    runbook: "Thread / DB 상태 점검"
```

대응
1. Thread Dump
2. DB 커넥션 확인
3. 캐시 우회

## 시나리오 4: 외부 의존성 장애
- 결제 API 장애
- Redis 다운

알람 조건
```promql
- alert: PaymentDependencyDown
  expr: |
    sum(rate(http_client_requests_seconds_count{
      service="demo-monitoring-service",
      uri=~".*payment.*",
      status=~"5.."
    }[2m])) > 5
  for: 1m
  labels:
    severity: critical
    service: demo-monitoring-service
    dependency: payment
  annotations:
    summary: "[DemoMonitoring] 결제 시스템 장애"
    runbook: "Circuit Breaker / 장애 공지"
```

대응
1. Circuit Breaker ON
2. 장애 공지
3. 대체 수단 검토

## 시나리오 5: 리소스 고갈 (사전 경고)
- Memory Leak
- 트래픽 급증

알람 조건
```promql
- alert: DemoMonitoringHeapAlmostFull
  expr: |
    jvm_memory_used_bytes{area="heap"}
    /
    jvm_memory_max_bytes{area="heap"}
    > 0.85
  for: 5m
  labels:
    severity: warning
    service: demo-monitoring-service
  annotations:
    summary: "[DemoMonitoring] Heap 사용량 임계"
    runbook: "GC / 메모리 릭 점검"
```

대응
1. Heap Dump
2. 트래픽 제한
3. 스케일 아웃

알람에 반드시 들어가야 할 것 (Runbook)
```aiignore
annotations:
  runbook: "https://wiki.company.com/runbooks/demo-monitoring-down"

```


## 배포 시 Alertmanager Silence 자동화
배포 중에 발생하는 의미 없는 알람을 자동으로 억제하고
배포가 끝나면 자동으로 다시 알람 활성화
```aiignore
배포 시작
  → Alertmanager Silence 생성
    → 배포 중 알람 억제
배포 종료
  → Silence 자동 해제 (시간 만료)
```

기본 원칙 (중요)

1. Silence는 사람이 푸는 게 아니다

2. 시간 기반(auto-expire) 으로 생성

3. service / environment 기준으로 억제

4. 배포 실패해도 자동 복구

### #1 Bash 스크립트
실행 후 30분 뒤 자동 해제
vi create-silence.sh 
```sh
#!/bin/bash

ALERTMANAGER_URL="http://localhost:9093"
SERVICE_NAME="demo-monitoring-service"
DURATION_MINUTES=30

START_TIME=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
END_TIME=$(date -u -d "+${DURATION_MINUTES} minutes" +"%Y-%m-%dT%H:%M:%SZ")

curl -s -X POST "${ALERTMANAGER_URL}/api/v2/silences" \
  -H "Content-Type: application/json" \
  -d "{
    \"matchers\": [
      {
        \"name\": \"service\",
        \"value\": \"${SERVICE_NAME}\",
        \"isRegex\": false
      }
    ],
    \"startsAt\": \"${START_TIME}\",
    \"endsAt\": \"${END_TIME}\",
    \"createdBy\": \"deploy-bot\",
    \"comment\": \"deployment in progress\"
  }"
```

### #2 GitHub Actions
```yml
name: Deploy

jobs:
  deploy:
    runs-on: ubuntu-latest

    steps:
      - name: Create Alertmanager Silence
        run: |
          curl -X POST http://alertmanager:9093/api/v2/silences \
            -H "Content-Type: application/json" \
            -d '{
              "matchers": [
                { "name": "service", "value": "demo-monitoring-service", "isRegex": false }
              ],
              "startsAt": "'"$(date -u +"%Y-%m-%dT%H:%M:%SZ")"'",
              "endsAt": "'"$(date -u -d "+30 minutes" +"%Y-%m-%dT%H:%M:%SZ")"'",
              "createdBy": "github-actions",
              "comment": "auto silence for deployment"
            }'

      - name: Deploy Application
        run: ./deploy.sh
```

### #3 Jenkins Pipeline
```aiignore
stage('Create Alertmanager Silence') {
  steps {
    sh """
      curl -X POST http://alertmanager:9093/api/v2/silences \
      -H 'Content-Type: application/json' \
      -d '{
        "matchers": [
          { "name": "service", "value": "demo-monitoring-service", "isRegex": false }
        ],
        "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'",
        "endsAt": "'$(date -u -d "+30 minutes" +"%Y-%m-%dT%H:%M:%SZ")'",
        "createdBy": "jenkins",
        "comment": "deployment in progress"
      }'
    """
  }
}
```