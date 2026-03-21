# План реализации K8s деплоя

Детальный пошаговый план по развертыванию F1 GOAT Determiner в Kubernetes.
Основан на стратегии из `k8s-deployment-strategy.md`.

## Соглашения

| Что                 | Значение                                         |
|---------------------|--------------------------------------------------|
| Домен (prod)        | `f1goatdeterminer.mooo.com` (freedns.afraid.org) |
| Домен (test)        | `test.f1goatdeterminer.mooo.com`                 |
| Docker Hub username | `remsely`                                        |
| Образ web (prod)    | `remsely/f1-goat-determiner-web`                 |
| Образ web (test)    | `remsely/f1-goat-determiner-web-test`            |
| Образ analytics-api (prod) | `remsely/f1-goat-determiner-analytics-api` |
| Образ analytics-api (test) | `remsely/f1-goat-determiner-analytics-api-test` |
| Образ data-sync-svc (prod) | `remsely/f1-goat-determiner-data-sync-svc` |
| Образ data-sync-svc (test) | `remsely/f1-goat-determiner-data-sync-svc-test` |
| JDK                 | 21 (eclipse-temurin)                             |
| PostgreSQL          | 18.2-alpine                                      |
| Кластер             | k3s, 2 ноды (server + agent)                     |

---

## MR 1: Подготовка Docker-образов

**Ветка**: `k8s/docker-images`
**Цель**: все сервисы готовы к контейнеризации, CI собирает и пушит образы.

### 1.1. Объединить frontend + nginx в один образ

**Что**: переписать `nginx/Dockerfile` — multi-stage build, где stage 1 собирает React,
stage 2 кладёт результат в nginx.

- [ ] Переписать `nginx/Dockerfile`:
    - Stage 1 (`builder`): `node:20-alpine`, установить bun, `bun install --frozen-lockfile`, `bun run build`
    - Stage 2: `nginx:alpine`, скопировать `nginx/nginx.conf` и `dist/` из builder
    - Build context — **корень репозитория** (`context: ./`, `file: ./nginx/Dockerfile`)
- [ ] Создать `nginx/Dockerfile.dockerignore`:
    - Исключить всё кроме `frontend/` и `nginx/`
    - Внутри `frontend/` — исключить `node_modules`, `dist`, `.idea`, `.vscode`

### 1.2. Создать Dockerfile для data-sync-svc

**Что**: multi-stage Dockerfile для Spring Boot (JDK 21).

- [ ] Создать `data-sync-svc/Dockerfile`:
    - Stage 1 (`builder`): `eclipse-temurin:21-jdk-alpine`, `./gradlew :app:bootJar --no-daemon`
    - Stage 2: `eclipse-temurin:21-jre-alpine`, копируем JAR, `ENTRYPOINT ["java", "-jar", "app.jar"]`
    - Expose 8080
- [ ] Создать `data-sync-svc/.dockerignore`:
    - Исключить `.gradle/`, `*/build/`, `.idea/`, `*.iml`

### 1.3. Переименовать Docker-образы

**Что**: унифицировать названия образов: `f1-goat-determiner-{service}`.

Старое → Новое:

- `f1-goat-analytics-api` → `f1-goat-determiner-analytics-api`
- `f1-goat-frontend` → удаляется (вместо него `f1-goat-determiner-web`)
- `f1-goat-nginx` → удаляется (вместо него `f1-goat-determiner-web`)
- `f1-goat-data-sync-svc` → `f1-goat-determiner-data-sync-svc`

Обновить:

- [ ] `docker-compose.prod.yaml` — image names и container names
- [ ] `.github/workflows/main-ci.yml` — image tags в build jobs

### 1.4. Обновить CI (`main-ci.yml`)

- [ ] Добавить job `web-build`:
    - `needs: [frontend-build-check, nginx-lint]`
    - `context: ./`, `file: ./nginx/Dockerfile`
    - Tags: `f1-goat-determiner-web:latest`, `f1-goat-determiner-web:${{ github.sha }}`
- [ ] Убрать job `frontend-build` (push Docker image) — frontend теперь внутри web
- [ ] Убрать job `nginx-build` — заменён на `web-build`
- [ ] Добавить фильтр `web` в detect-changes:
    - Объединить паттерны `frontend/` + `nginx/`
    - Lint-фильтры `frontend` и `nginx` — оставить для lint/test jobs
- [ ] Включить job `data-sync-build` (убрать `if: false`)
    - Tags: `f1-goat-determiner-data-sync-svc:latest` / `${{ github.sha }}`
- [ ] Переименовать тег `analytics-api` → `f1-goat-determiner-analytics-api`

### 1.5. Обновить docker-compose.prod.yaml

- [ ] Убрать сервис `frontend` и volume `frontend-static`
- [ ] Сервис `nginx` → `web`, image: `f1-goat-determiner-web`
- [ ] Обновить `analytics-api` image name
- [ ] Сохранить `docker-compose.yaml` (dev) без изменений — hot reload нужен

### 1.6. Проверка

- [ ] `docker build -f nginx/Dockerfile -t f1-goat-determiner-web:dev .` — успешно
- [ ] `docker build -t f1-goat-determiner-data-sync-svc:dev ./data-sync-svc` — успешно
- [ ] `docker compose -f docker-compose.prod.yaml up` — всё поднимается, сайт работает
- [ ] CI pipeline проходит на ветке

---

## MR 2: Kubernetes-манифесты (Kustomize base + overlays)

**Ветка**: `k8s/manifests`
**Цель**: полный набор манифестов, проверенных в minikube.

**Предусловие**: установить minikube + kubectl локально.

### 2.1. Структура k8s/

```
k8s/
├── base/
│   ├── kustomization.yaml
│   ├── db-config.yaml              # ConfigMap
│   ├── analytics-api/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── web/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── ingress.yaml
│   ├── postgres/
│   │   ├── statefulset.yaml        # включает volumeClaimTemplates
│   │   └── service.yaml            # Headless Service (clusterIP: None)
│   └── data-sync-svc/
│       └── deployment.yaml
├── overlays/
│   ├── prod/
│   │   ├── kustomization.yaml
│   │   └── ingress-patch.yaml
│   └── test/
│       ├── kustomization.yaml
│       └── ingress-patch.yaml
└── cert-manager/
    ├── cluster-issuer-staging.yaml
    └── cluster-issuer-prod.yaml
```

### 2.2. base/ — общие манифесты

- [ ] `kustomization.yaml` — оглавление ресурсов
- [ ] `db-config.yaml` — ConfigMap с `POSTGRES_DB: f1_goat_determiner`
- Namespace'ы создаются отдельно через `kubectl create namespace` (не через base)

### 2.3. PostgreSQL (StatefulSet)

- [ ] `postgres/statefulset.yaml`:
    - `postgres:18.2-alpine`, 1 реплика
    - Env из ConfigMap (`POSTGRES_DB`) и Secret (`POSTGRES_USER`, `POSTGRES_PASSWORD`)
    - `POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=C"`
    - `volumeClaimTemplates`: 2Gi, `ReadWriteOnce`
    - `readinessProbe` (exec): `["/bin/sh", "-c", "pg_isready -U \"$POSTGRES_USER\" -d \"$POSTGRES_DB\""]`
    - Resources: requests 256Mi/100m, limits 512Mi/500m
- [ ] `postgres/service.yaml`: Headless Service (`clusterIP: None`), порт 5432

### 2.4. analytics-api (Deployment + Service)

- [ ] `analytics-api/deployment.yaml`:
    - Image: `remsely/f1-goat-determiner-analytics-api:latest`
    - Env: `PYTHONUNBUFFERED=1`, `DB_HOST=postgres`, `DB_PORT=5432`,
      `DB_NAME` из ConfigMap, `DB_USER`/`DB_PASSWORD` из Secret
    - `readinessProbe`/`livenessProbe`: HTTP GET `/` на порт 8000
    - Resources: requests 128Mi/100m, limits 512Mi/500m
- [ ] `analytics-api/service.yaml`: ClusterIP, порт 8000

### 2.5. web (Deployment + Service + Ingress)

- [ ] `web/deployment.yaml`:
    - Image: `remsely/f1-goat-determiner-web:latest`
    - `readinessProbe`: HTTP GET `/` на порт 80
    - Resources: requests 64Mi/50m, limits 128Mi/200m
- [ ] `web/service.yaml`: ClusterIP, порт 80
- [ ] `web/ingress.yaml`:
    - Аннотация `cert-manager.io/cluster-issuer: letsencrypt-prod`
    - Host: `f1goatdeterminer.mooo.com` (overlay переопределит)
    - TLS secretName: `f1-goat-tls`

### 2.6. data-sync-svc (Deployment)

- [ ] `data-sync-svc/deployment.yaml`:
    - Image: `remsely/f1-goat-determiner-data-sync-svc:latest`
    - 1 реплика (ShedLock — не нужно масштабирование)
    - Env: `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/$(DB_NAME)`,
      `SPRING_DATASOURCE_USERNAME`/`SPRING_DATASOURCE_PASSWORD` из Secret
    - `readinessProbe`/`livenessProbe`: TCP socket на порт 8080 (Actuator не подключен)
    - Resources: requests 256Mi/100m, limits 512Mi/500m
    - **Без Service** — к нему никто не обращается извне

### 2.7. Overlays

- [ ] `overlays/prod/kustomization.yaml`:
    - `namespace: prod`
    - `resources: [../../base]`
    - `replicas: analytics-api -> 2`
    - `images:` — все три образа с `newTag: latest` (CI заменит на SHA)
    - `patches: [ingress-patch.yaml]`
- [ ] `overlays/prod/ingress-patch.yaml`:
    - Host: `f1goatdeterminer.mooo.com`
    - TLS secretName: `f1-goat-tls-prod`
- [ ] `overlays/test/kustomization.yaml`:
    - `namespace: test`
    - `replicas: analytics-api -> 1`
- [ ] `overlays/test/ingress-patch.yaml`:
    - Host: `test.f1goatdeterminer.mooo.com`
    - TLS secretName: `f1-goat-tls-test`

### 2.8. .gitignore

- [ ] Добавить `k8s/**/secret*.yaml` — секреты не коммитим

### 2.9. Проверка в minikube

- [ ] `minikube start --driver=docker --memory=4096 --cpus=2`
- [ ] Собрать образы и загрузить в minikube (`minikube image load`)
- [ ] Создать namespace + Secret + ConfigMap вручную
- [ ] `kubectl apply -k k8s/base/` — все ресурсы поднимаются
- [ ] `kubectl port-forward svc/web 8080:80` — сайт открывается
- [ ] analytics-api отвечает на `/api/` запросы
- [ ] PostgreSQL принимает соединения (`kubectl exec -it postgres-0 -- psql ...`)
- [ ] data-sync-svc стартует, Flyway накатывает миграции, синхронизация идёт

---

## MR 3: CI/CD — деплой в Kubernetes

**Ветка**: `k8s/ci-cd-deploy`
**Цель**: автодеплой на prod при мердже в main, ручной деплой на test.

**Предусловие**:

- 2 VPS арендованы, k3s установлен (server + agent), обе ноды Ready
- kubeconfig скопирован на локальную машину
- Namespace-ы `prod` и `test` созданы
- Secret-ы `db-credentials` и ConfigMap-ы `db-config` созданы в обоих namespace-ах

### 3.1. Тестовые образы — отдельные репозитории на Docker Hub

Тестовые образы публикуются в отдельные Docker Hub репозитории с суффиксом `-test`,
чтобы не засорять историю продовых образов:

| Prod                                       | Test                                            |
|--------------------------------------------|-------------------------------------------------|
| `remsely/f1-goat-determiner-web`           | `remsely/f1-goat-determiner-web-test`           |
| `remsely/f1-goat-determiner-analytics-api` | `remsely/f1-goat-determiner-analytics-api-test` |
| `remsely/f1-goat-determiner-data-sync-svc` | `remsely/f1-goat-determiner-data-sync-svc-test` |

Test overlay (`k8s/overlays/test/kustomization.yaml`) должен ссылаться на `*-test` образы.

### 3.2. deploy-prod job в main-ci.yml

- [ ] Добавить job `deploy-prod`:
    - `needs: [analytics-api-build, web-build, data-sync-build]`
    - Условие: все build-jobs прошли (или были skipped — detect-changes)
    - `azure/setup-kubectl@v4`
    - `imranismail/setup-kustomize@v2` — установка kustomize CLI
    - Kubeconfig из `secrets.KUBE_CONFIG` (base64-encoded)
    - `kustomize edit set image` — подставить `${{ github.sha }}` теги для всех образов
    - `kubectl apply -k k8s/overlays/prod`
    - `kubectl rollout status` для analytics-api, web, data-sync-svc (timeout 120s)

### 3.3. deploy-test job

- [ ] Добавить job `deploy-test`:
    - `environment: test` — ждёт ручного approve (required reviewers)
    - Шаги:
        1. **Собрать и запушить тестовые образы** — аналогично prod build jobs, но
           теги: `*-test:latest` и `*-test:${{ github.sha }}`
        2. **Деплой** — `kustomize edit set image` с тестовыми образами,
           `kubectl apply -k k8s/overlays/test`
        3. **Проверка** — `kubectl rollout status` с namespace `test` (timeout 120s)

### 3.4. GitHub Secrets

Убедиться, что настроены:

| Secret               | Описание                                                |
|----------------------|---------------------------------------------------------|
| `KUBE_CONFIG`        | `cat ~/.kube/config \| base64` — kubeconfig server-ноды |
| `DOCKERHUB_USERNAME` | Уже есть                                                |
| `DOCKERHUB_TOKEN`    | Уже есть                                                |

### 3.5. GitHub Environment

- [ ] Settings -> Environments -> создать `test` с Required reviewers (указать себя)

### 3.6. Проверка

- [ ] Push в main -> образы собираются -> deploy-prod запускается -> Pod-ы обновляются
- [ ] В PR -> deploy-test ждёт approve -> собирает `*-test` образы -> деплой на test
- [ ] `kubectl get pods -n prod -o wide` — analytics-api на разных нодах
- [ ] `kubectl get pods -n test` — всё работает, образы с суффиксом `-test`

---

## MR 4: HTTPS с cert-manager

**Ветка**: `k8s/https`
**Цель**: приложение доступно по HTTPS с Let's Encrypt сертификатом.

**Предусловие**:

- Домены зарегистрированы на freedns.afraid.org
- DNS A-записи настроены на IP server-ноды
- cert-manager установлен в кластер

### 4.1. ClusterIssuer

- [ ] `k8s/cert-manager/cluster-issuer-staging.yaml`:
    - ACME staging endpoint (для отладки, без rate limits)
    - HTTP-01 solver с `class: traefik`
- [ ] `k8s/cert-manager/cluster-issuer-prod.yaml`:
    - ACME production endpoint
    - Email для уведомлений

### 4.2. Проверка со staging

- [ ] Применить staging issuer
- [ ] Обновить Ingress аннотацию на `letsencrypt-staging`
- [ ] `kubectl get certificate -n prod` — сертификат выпустился
- [ ] Сайт открывается по HTTPS (сертификат невалиден — staging, это ок)

### 4.3. Переключение на prod

- [ ] Переключить Ingress аннотацию на `letsencrypt-prod`
- [ ] `kubectl get certificate -n prod` — валидный сертификат
- [ ] `https://f1goatdeterminer.mooo.com` — работает
- [ ] `https://test.f1goatdeterminer.mooo.com` — работает

---

## Порядок выполнения

```
MR 1 (Docker-образы)
  |
  v
[Установить minikube + kubectl]
  |
  v
MR 2 (K8s-манифесты) <- проверка в minikube
  |
  v
[Арендовать 2 VPS, установить k3s, создать секреты/namespace-ы]
  |
  v
MR 3 (CI/CD деплой) <- первый деплой на VPS
  |
  v
[Зарегистрировать домены, настроить DNS, установить cert-manager]
  |
  v
MR 4 (HTTPS) <- приложение доступно по домену
```

---

## Финальный чек-лист

### Инфраструктура

- [ ] 2 VPS арендованы, k3s установлен, обе ноды Ready
- [ ] Домены зарегистрированы на freedns.afraid.org, DNS настроен
- [ ] cert-manager установлен, ClusterIssuer создан
- [ ] kubeconfig в GitHub Actions Secrets

### Образы

- [ ] `f1-goat-determiner-web` — собирается и пушится в CI
- [ ] `f1-goat-determiner-analytics-api` — собирается и пушится в CI
- [ ] `f1-goat-determiner-data-sync-svc` — собирается и пушится в CI

### Kubernetes

- [ ] base/ манифесты проверены в minikube
- [ ] overlays/prod (2 реплики API) и overlays/test (1 реплика) настроены
- [ ] PostgreSQL StatefulSet с PVC работает
- [ ] data-sync-svc Deployment — Flyway + синхронизация
- [ ] Ingress маршрутизирует трафик

### CI/CD

- [ ] Push в main -> автодеплой в prod
- [ ] Environment `test` с required reviewers -> ручной деплой

### Проверка

- [ ] `https://f1goatdeterminer.mooo.com` — тир-лист отображается
- [ ] `https://test.f1goatdeterminer.mooo.com` — работает отдельно
- [ ] `https://f1goatdeterminer.mooo.com/api/` — API отвечает
- [ ] Let's Encrypt сертификат валиден
- [ ] 2 реплики analytics-api в prod на разных нодах
- [ ] data-sync-svc синхронизирует данные
