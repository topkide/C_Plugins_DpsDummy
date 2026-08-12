# DpsDummy

DPS 측정용 무적 갑옷 거치대 플러그인 (Paper 26.2)

## 기능

- `/dps` — 현재 위치에 무적 DPS 측정기(갑옷 거치대) 소환. 플레이어를 마주보게 소환되며 서버 재시작 후에도 유지.
- `/undps` — 반경 5블록(콘피그 조정 가능) 내 측정기 제거. 장착돼 있던 장비는 바닥에 드롭.
- 측정기를 때리면(근접/화살 모두) 공격자에게 액션바로 실시간 DPS 표시.
  - 롤링 윈도우 방식: 최근 5초간 넣은 데미지 합 ÷ 5
  - 여러 명이 동시에 때려도 각자 독립 측정
  - 5초간 무타격 시 측정 종료 + 최고 DPS 표시
- 갑옷/플레이어 머리를 들고 우클릭하면 장착 가능, 빈손 우클릭으로 회수.
  - 방어구 장착 시 바닐라 방어 공식(방어 포인트 + 강도 + 보호 인챈트 EPF)을 직접 계산해
    **감소된 실효 데미지**로 DPS가 기록됨 (액션바에 🛡 표시)
  - 방어구 세팅별 실효 DPS 시뮬레이터로 활용 가능

## 빌드

```bash
./gradlew build
```

- JDK 25 필요 (없으면 foojay resolver가 자동 다운로드)
- 산출물: `build/libs/DpsDummy-<버전>.jar`

## 릴리스 (GitHub Actions)

`v*` 태그를 푸시하면 GitHub Actions가 자동으로 빌드 후 jar를 GitHub Release에 업로드합니다.

```bash
git tag v1.0.1 && git push origin v1.0.1
```

## 콘피그

```yaml
window-seconds: 5    # DPS 롤링 윈도우 (초)
remove-radius: 5.0   # /undps 제거 반경 (블록)
```
