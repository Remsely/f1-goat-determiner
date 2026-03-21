# F1 GOAT Determiner — Стратегия деплоя в Kubernetes

## Содержание

1. Обзор проекта и целевая архитектура
2. Ключевые концепции Kubernetes
3. Фаза 0 — Подготовка Docker-образов
4. Фаза 1 — Локальная отладка (minikube)
5. Фаза 2 — Kubernetes-манифесты (Kustomize)
6. Фаза 3 — PostgreSQL в кластере
7. Фаза 4 — Секреты и конфигурация
8. Фаза 5 — Поднятие боевого кластера (k3s на VPS)
9. Фаза 6 — Домен и HTTPS (cert-manager)
10. Фаза 7 — CI/CD для Kubernetes
11. Фаза 8 — Два окружения (prod / test)
12. Фаза 9 — data-sync-svc как постоянный сервис
13. Чек-лист готовности
14. Приложение A — Терминология: кластер, нода, control plane

---

## 1. Обзор проекта и целевая архитектура

### Текущие сервисы

| Сервис            | Стек                             | Роль                                             | Docker-образ                                            |
|-------------------|----------------------------------|--------------------------------------------------|---------------------------------------------------------|
| **analytics-api** | Python FastAPI, psycopg2, pandas | REST API — тир-листы, кластеризация              | `remsely/f1-goat-analytics-api`                         |
| **frontend**      | React, Vite, bun                 | SPA-интерфейс                                    | `remsely/f1-goat-frontend` (init-контейнер со статикой) |
| **nginx**         | Nginx Alpine                     | Reverse proxy + раздача статики                  | `remsely/f1-goat-nginx`                                 |
| **PostgreSQL**    | PostgreSQL 18.2 Alpine           | Хранение F1-данных                               | Стандартный образ `postgres:18.2-alpine`                |
| **data-sync-svc** | Kotlin Spring Boot               | Синхронизация данных из Jolpica API → PostgreSQL | `remsely/f1-goat-data-sync-svc` (нужен Dockerfile)      |

### Текущий Docker Compose стек

```
nginx (:80)
├── / → static files (shared volume от frontend init-контейнера)
└── /api/ → proxy_pass → analytics-api (:8000) → PostgreSQL (:5432)
```

docker-compose.prod.yaml уже использует env-переменные для credentials
(`POSTGRES_PASSWORD` обязателен, остальное с дефолтами).

### Целевая архитектура в Kubernetes

```
Internet
   │
   ▼
Traefik Ingress Controller (встроен в k3s, слушает 80/443)
   │
   ├─ f1goat.example.com ─────► Service: web ──► Pod: web (nginx + static)
   │                                                 │
   │                                                 └─ proxy_pass ──► Service: analytics-api
   │                                                                       │
   │                                                                       ▼
   │                                                                  Pod(s): analytics-api
   │                                                                       │
   │                                                                       ▼
   │                                                              Service: postgres
   │                                                                       │
   │                                                                       ▼
   │                                                              Pod: postgres-0 (StatefulSet)
   │                                                                       ▲
   │                                                                       │
   │                                                              Pod: data-sync-svc (Deployment, 1 replica)
   │                                                              ShedLock cron → Jolpica API → PostgreSQL
   │
   └─ test.f1goat.example.com ─► (аналогично, namespace: test)
```

### Что мы получим в итоге

- Приложение доступно по домену с HTTPS (Let's Encrypt)
- Два окружения: `prod` (2 реплики API) и `test` (1 реплика)
- Автодеплой на prod при мердже в main
- Ручной деплой на test через environment с required reviewers
- PostgreSQL с persistent storage (данные переживают перезапуск Pod'а)
- data-sync-svc непрерывно синхронизирует данные из Jolpica API
- Секреты в Kubernetes Secrets (пароль БД, kubeconfig)
- Два VPS (server + agent) — реплики API разъезжаются по нодам

---

## 2. Ключевые концепции Kubernetes

### Pod

Pod — минимальная единица деплоя в Kubernetes. Это обёртка вокруг одного
или нескольких контейнеров, которые разделяют между собой сеть (обращаются
друг к другу через `localhost`) и хранилище.

Ты никогда не создаёшь Pod напрямую — им управляют контроллеры
(Deployment, StatefulSet). Если Pod упал, контроллер пересоздаёт его.

Аналогия с Docker: Pod ≈ `docker run`, но "одноразовый". При пересоздании
получает новый IP-адрес.

### Deployment

Deployment описывает **желаемое состояние**: "Я хочу 2 реплики
analytics-api из образа X". Kubernetes непрерывно сверяет реальность
с желаемым состоянием и приводит их в соответствие:

- Pod упал → создаётся новый
- Обновили образ → rolling update (старые Pod'ы заменяются новыми по одному)
- Увеличили replicas → запускаются дополнительные Pod'ы

Внутри Deployment есть `template` — шаблон Pod'а, из которого штампуются
реплики. Все реплики одного Deployment'а идентичны и взаимозаменяемы.

### Service

Pod'ы "смертны" — у каждого динамический IP. Если analytics-api
пересоздался, nginx не может знать его новый адрес. Service решает
эту проблему — это стабильный DNS-адрес + балансировщик для группы Pod'ов.

Service находит "свои" Pod'ы по **labels** (метки). Если у Pod'а label
`app: analytics-api`, а у Service selector `app: analytics-api` — трафик
будет маршрутизироваться к этим Pod'ам.

Типы Service:

- **ClusterIP** (по умолчанию) — доступен только внутри кластера.
  Пример: analytics-api не нужен снаружи, к нему обращается только nginx
  через внутренний DNS: `analytics-api.prod.svc.cluster.local` (или просто
  `analytics-api` внутри того же namespace).
- **NodePort** — открывает порт (30000-32767) на каждой ноде кластера.
  Редко используется напрямую.
- **LoadBalancer** — создаёт внешний балансировщик. Актуально в облаках
  (AWS ELB, GCP LB). На голом VPS не работает без дополнительных инструментов.

### Ingress

Ingress — декларативные правила маршрутизации HTTP/HTTPS-трафика извне
кластера к Service'ам. По сути, это конфиг reverse proxy на уровне кластера.

Пример: "Запросы на `f1goat.example.com` направляй в Service `web`
на порт 80".

Ingress сам по себе ничего не делает — нужен **Ingress Controller**:
программа, которая читает Ingress-ресурсы и настраивает реальный
reverse proxy. В k3s из коробки идёт **Traefik**.

### Namespace

Namespace — логическая изоляция внутри кластера. Объекты в разных
namespace'ах по умолчанию не видят друг друга. Но можно обращаться
кросс-namespace через полный DNS:
`service-name.namespace.svc.cluster.local`.

Мы создадим два namespace: `prod` и `test`. Каждый будет содержать
полный набор сервисов с отдельной базой данных.

### ConfigMap и Secret

**ConfigMap** хранит нечувствительную конфигурацию в формате ключ-значение.
Примеры: `PYTHONUNBUFFERED=1`, `DB_HOST=postgres`, `DB_PORT=5432`.
Можно монтировать как env-переменные или как файлы.

**Secret** хранит чувствительные данные (пароли, токены, ключи).
Хранятся в etcd (внутренней БД Kubernetes) в формате base64.
Важно: base64 — это **не шифрование**, а кодирование. Для настоящего
шифрования нужна дополнительная настройка (encryption at rest).
Для pet-проекта base64 достаточно — главное, что секреты отделены
от остальных конфигов и не попадают в git.

### StatefulSet

Похож на Deployment, но для **stateful**-нагрузок (базы данных,
очереди). Отличия от Deployment:

- Каждый Pod получает стабильное имя: `postgres-0`, `postgres-1`
  (не случайный суффикс как в Deployment).
- Каждому Pod'у привязан свой PersistentVolume с данными.
- При пересоздании Pod получает **тот же** volume — данные не теряются.
- Pod'ы создаются и удаляются последовательно (0, затем 1, затем 2...),
  а не параллельно.

### PersistentVolume (PV) и PersistentVolumeClaim (PVC)

**PVC** (Persistent Volume Claim) — запрос на хранилище: "Мне нужен
диск на 5GB". Это как заказ в магазине.

**PV** (Persistent Volume) — само хранилище, которое удовлетворяет
PVC. Это реальный диск на сервере.

В k3s PV создаётся автоматически через встроенный **local-path provisioner**:
когда Pod запрашивает PVC, k3s выделяет директорию на локальном диске ноды
и монтирует её в Pod. Данные живут в `/var/lib/rancher/k3s/storage/`.

### Kustomize

Инструмент для управления вариациями Kubernetes-манифестов **без шаблонизации**.
Встроен в kubectl (не требует установки). Альтернатива Helm.

Идея: есть `base/` (общие манифесты) и `overlays/` (патчи для окружений).
Overlay может переопределить количество реплик, образ, домен — не копируя
весь манифест, а описывая только **разницу** с base.

```
kubectl apply -k overlays/prod    # применить prod-overlay
kubectl apply -k overlays/test    # применить test-overlay
```

---

## 3. Фаза 0 — Подготовка Docker-образов

### Зачем менять

Сейчас 3 отдельных образа: `analytics-api`, `frontend`, `nginx`.
При этом `frontend` — init-контейнер, который копирует статику
в shared volume для nginx. В Docker Compose это работает через named volume.
В Kubernetes shared volumes между Deployment'ами — антипаттерн:
нужно синхронизировать жизненный цикл двух Deployment'ов.

### Решение: объединить frontend + nginx в один образ

Dockerfile живёт в `nginx/Dockerfile`, но build context — **корень
репозитория** (чтобы видеть и `frontend/`, и `nginx/`). Multi-stage
build: stage 1 собирает React, stage 2 кладёт результат в nginx.

#### nginx/Dockerfile (переписать)

```dockerfile
# === Stage 1: Build frontend ===
FROM node:20-alpine AS builder
WORKDIR /app
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm install -g bun
COPY frontend/package.json frontend/bun.lock ./
RUN bun install --frozen-lockfile
COPY frontend/ .
RUN bun run build

# === Stage 2: Nginx with static ===
FROM nginx:alpine
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

#### nginx/Dockerfile.dockerignore (создать)

Поскольку build context — корень, Docker по умолчанию отправит всё
дерево в daemon. Buildx поддерживает Dockerfile-specific `.dockerignore`
(файл рядом с Dockerfile с суффиксом `.dockerignore`):

```
# Всё, кроме frontend/ и nginx/
*
!frontend/
!nginx/

# Внутри frontend/ — лишнее
frontend/node_modules
frontend/dist
frontend/.idea
frontend/.vscode

# Внутри nginx/ — лишнее
nginx/*.md
```

Это сокращает build context с ~сотен мегабайт до ~единиц мегабайт.

#### nginx.conf — без изменений

Текущий конфиг уже использует `proxy_pass http://analytics-api:8000` —
это совпадает с именем Service в Kubernetes. Менять ничего не нужно. ✅

### Изменения в CI (main-ci.yml)

#### Что убираем

- Job `frontend-build` (push Docker-образа `f1-goat-frontend`) — больше не нужен.
  Frontend собирается внутри nginx-образа.
- Job `nginx-build` — заменяется на `web-build` с другим контекстом и тегами.
- Docker-образ `f1-goat-frontend` на DockerHub — перестаёт обновляться.
- Docker-образ `f1-goat-nginx` — переименовывается в `f1-goat-web`.

#### Что остаётся без изменений

- `frontend-lint`, `frontend-test`, `frontend-build-check` — CI-проверки
  исходников, не связаны с Docker-образом. Они нужны для PR-проверок.
- `nginx-lint` (gixy) — проверяет nginx.conf, остаётся.

#### Новый job: web-build

```yaml
  web-build:
      name: Web - Build & Push
      needs: [ frontend-build-check, nginx-lint ]    # зависит от обоих
      runs-on: ubuntu-latest
      steps:
          -   uses: actions/checkout@v4
          -   uses: docker/setup-buildx-action@v3
          -   uses: docker/login-action@v3
              with:
                  username: ${{ secrets.DOCKERHUB_USERNAME }}
                  password: ${{ secrets.DOCKERHUB_TOKEN }}
          -   uses: docker/build-push-action@v5
              with:
                  context: ./                       # ← корень репозитория
                  file: ./nginx/Dockerfile          # ← Dockerfile внутри nginx/
                  push: true
                  tags: |
                      ${{ env.DOCKERHUB_USERNAME }}/f1-goat-web:latest
                      ${{ env.DOCKERHUB_USERNAME }}/f1-goat-web:${{ github.sha }}
                  cache-from: type=gha
                  cache-to: type=gha,mode=max
```

#### detect-changes: объединённый фильтр

Вместо отдельных `frontend` и `nginx` фильтров для Docker-сборки
нужен один `web`:

```yaml
web:
    - 'frontend/src/**'
    - 'frontend/package.json'
    - 'frontend/bun.lock'
    - 'frontend/vite.config.ts'
    - 'frontend/tsconfig*.json'
    - 'nginx/**'
    - '.github/workflows/main-ci.yml'
```

Фильтры `frontend` и `nginx` для lint/test job'ов — остаются.

### Изменения в docker-compose.prod.yaml

Убираем `frontend` сервис и shared volume, `nginx` → `web`:

```yaml
services:
    postgres:
    # ... без изменений ...

    analytics-api:
    # ... без изменений ...

    web: # ← было nginx + frontend
        image: ${DOCKERHUB_USERNAME:-remsely}/f1-goat-web:latest
        container_name: f1-goat-web
        ports:
            - "80:80"
        depends_on:
            - analytics-api
        restart: unless-stopped
        networks:
            - f1-network

# frontend-static volume больше не нужен
volumes:
    postgres_data:

networks:
    f1-network:
        name: f1-goat-network
```

### docker-compose.yaml (dev) — без изменений

Для локальной разработки оставляем текущую схему: отдельные frontend
и nginx контейнеры с shared volume. Это сохраняет hot reload через
Vite (`bun run dev`). Объединённый образ используется только в prod/k8s.

### Итого: образы для Kubernetes

| Образ                           | Содержимое                          | Dockerfile                                 |
|---------------------------------|-------------------------------------|--------------------------------------------|
| `remsely/f1-goat-web`           | nginx + React static + proxy config | `nginx/Dockerfile` (context: root)         |
| `remsely/f1-goat-analytics-api` | FastAPI + psycopg2                  | `analytics-api/Dockerfile` (без изменений) |
| `remsely/f1-goat-data-sync-svc` | Spring Boot + Flyway                | `data-sync-svc/Dockerfile` (создать)       |
| `postgres:18.2-alpine`          | Стандартный образ PostgreSQL        | —                                          |

### Задачи

- [ ] Переписать `nginx/Dockerfile` (multi-stage: frontend build + nginx)
- [ ] Создать `nginx/Dockerfile.dockerignore` (исключить всё кроме frontend/ и nginx/)
- [ ] Обновить CI (`main-ci.yml`):
    - [ ] Добавить job `web-build` (context: `./`, file: `./nginx/Dockerfile`)
    - [ ] Убрать job `frontend-build` (push Docker image)
    - [ ] Убрать job `nginx-build`
    - [ ] Добавить фильтр `web` в detect-changes
- [ ] Обновить `docker-compose.prod.yaml` (убрать frontend, nginx → web)
- [ ] Собрать образ локально и проверить: `docker build -f nginx/Dockerfile -t f1-goat-web:dev .`
- [ ] Создать `data-sync-svc/Dockerfile` (JDK multi-stage build)

---

## 4. Фаза 1 — Локальная отладка (minikube)

### Зачем

Писать манифесты и сразу проверять на VPS — медленно и больно.
Minikube создаёт одноноднодный Kubernetes-кластер на твоей машине.
Можно быстро итерировать: написал манифест → `kubectl apply` →
посмотрел результат → поправил.

### Установка (Windows)

```powershell
# Minikube (требует Docker Desktop или Hyper-V)
winget install minikube

# kubectl (CLI для Kubernetes)
winget install Kubernetes.kubectl

# Проверка
minikube version
kubectl version --client
```

### Запуск кластера

```powershell
minikube start --driver=docker --memory=4096 --cpus=2

# Проверить, что нода Ready
kubectl get nodes
```

### Ключевые команды для отладки

```bash
# Посмотреть все ресурсы в namespace
kubectl get all -n prod

# Логи конкретного Pod'а
kubectl logs <pod-name> -n prod

# Логи с follow (как tail -f)
kubectl logs -f <pod-name> -n prod

# Зайти внутрь контейнера
kubectl exec -it <pod-name> -n prod -- sh

# Описание ресурса (полезно для диагностики ошибок)
kubectl describe pod <pod-name> -n prod

# События в namespace (показывает причины ошибок)
kubectl get events -n prod --sort-by='.lastTimestamp'

# Пробросить порт на локальную машину (для тестирования)
kubectl port-forward service/web 8080:80 -n prod
# Теперь открывай http://localhost:8080
```

### Доступ к локальным образам в minikube

Minikube по умолчанию тянет образы из Docker Hub. Чтобы использовать
локально собранные образы (не пушить на Hub при каждом изменении):

```bash
# Вариант 1: использовать Docker daemon minikube
eval $(minikube docker-env)   # Linux/Mac
minikube docker-env | Invoke-Expression   # PowerShell

# Теперь docker build будет собирать образы ВНУТРИ minikube
docker build -f nginx/Dockerfile -t remsely/f1-goat-web:dev .
docker build -t remsely/f1-goat-analytics-api:dev ./analytics-api

# В манифестах ставим imagePullPolicy: Never
```

```bash
# Вариант 2: загрузить готовый образ в minikube
minikube image load remsely/f1-goat-web:dev
```

### Задачи

- [ ] Установить minikube и kubectl
- [ ] Запустить кластер: `minikube start`
- [ ] Убедиться, что `kubectl get nodes` показывает Ready
- [ ] Собрать образы локально и загрузить в minikube

---

## 5. Фаза 2 — Kubernetes-манифесты (Kustomize)

### Структура файлов в репозитории

```
k8s/
├── base/                          # Общие манифесты (без привязки к окружению)
│   ├── kustomization.yaml
│   ├── namespace.yaml
│   │
│   ├── analytics-api/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   │
│   ├── web/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── ingress.yaml
│   │
│   ├── postgres/
│   │   ├── statefulset.yaml
│   │   ├── service.yaml
│   │   └── pvc.yaml
│   │
│   └── data-sync-svc/
│       └── deployment.yaml
│
├── overlays/
│   ├── prod/
│   │   ├── kustomization.yaml     # replicas: 2, prod-домен, prod-секреты
│   │   └── ingress-patch.yaml
│   │
│   └── test/
│       ├── kustomization.yaml     # replicas: 1, test-домен, test-секреты
│       └── ingress-patch.yaml
│
└── cert-manager/
    └── cluster-issuer.yaml        # Let's Encrypt issuer
```

### base/kustomization.yaml

Kustomize использует этот файл как "оглавление" — он указывает, какие
ресурсы включить:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

resources:
    - namespace.yaml
    - postgres/statefulset.yaml
    - postgres/service.yaml
    - postgres/pvc.yaml
    - analytics-api/deployment.yaml
    - analytics-api/service.yaml
    - web/deployment.yaml
    - web/service.yaml
    - web/ingress.yaml
    - data-sync-svc/deployment.yaml
```

### base/namespace.yaml

```yaml
apiVersion: v1
kind: Namespace
metadata:
    name: f1-goat    # overlay переопределит на prod / test
```

### base/analytics-api/deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
    name: analytics-api
    labels:
        app: analytics-api
spec:
    replicas: 1                    # overlay переопределит для prod
    selector:
        matchLabels:
            app: analytics-api         # по этим labels Deployment находит свои Pod'ы
    template: # <-- шаблон Pod'а
        metadata:
            labels:
                app: analytics-api       # labels Pod'а — должны совпадать с selector
        spec:
            containers:
                -   name: analytics-api
                    image: remsely/f1-goat-analytics-api:latest
                    ports:
                        -   containerPort: 8000
                    env:
                        -   name: PYTHONUNBUFFERED
                            value: "1"
                        -   name: DB_HOST
                            value: postgres     # DNS-имя Service PostgreSQL
                        -   name: DB_PORT
                            value: "5432"
                        -   name: DB_NAME
                            valueFrom:
                                configMapKeyRef:
                                    name: db-config
                                    key: POSTGRES_DB
                        -   name: DB_USER
                            valueFrom:
                                secretKeyRef:
                                    name: db-credentials
                                    key: POSTGRES_USER
                        -   name: DB_PASSWORD
                            valueFrom:
                                secretKeyRef:
                                    name: db-credentials
                                    key: POSTGRES_PASSWORD
                    resources:
                        requests: # минимум, который Pod гарантированно получит
                            memory: "128Mi"
                            cpu: "100m"         # 100 миллиядер = 0.1 CPU
                        limits: # максимум, при превышении — OOM Kill
                            memory: "512Mi"
                            cpu: "500m"
                    readinessProbe: # K8s не пошлёт трафик, пока проба не пройдёт
                        httpGet:
                            path: /
                            port: 8000
                        initialDelaySeconds: 5
                        periodSeconds: 10
                    livenessProbe: # если проба падает — Pod перезапустится
                        httpGet:
                            path: /
                            port: 8000
                        initialDelaySeconds: 10
                        periodSeconds: 30
```

### base/analytics-api/service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
    name: analytics-api
spec:
    type: ClusterIP               # доступен только внутри кластера
    selector:
        app: analytics-api           # ← направляет трафик к Pod'ам с этим label
    ports:
        -   port: 8000                 # порт Service (по нему обращаются другие сервисы)
            targetPort: 8000           # порт контейнера
```

### base/web/deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
    name: web
    labels:
        app: web
spec:
    replicas: 1
    selector:
        matchLabels:
            app: web
    template:
        metadata:
            labels:
                app: web
        spec:
            containers:
                -   name: web
                    image: remsely/f1-goat-web:latest
                    ports:
                        -   containerPort: 80
                    resources:
                        requests:
                            memory: "64Mi"
                            cpu: "50m"
                        limits:
                            memory: "128Mi"
                            cpu: "200m"
                    readinessProbe:
                        httpGet:
                            path: /
                            port: 80
                        initialDelaySeconds: 3
                        periodSeconds: 10
```

### base/web/service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
    name: web
spec:
    type: ClusterIP
    selector:
        app: web
    ports:
        -   port: 80
            targetPort: 80
```

### base/web/ingress.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
    name: f1-goat-ingress
    annotations:
        cert-manager.io/cluster-issuer: letsencrypt-prod
spec:
    tls:
        -   hosts:
                - f1goat.example.com         # overlay переопределит
            secretName: f1-goat-tls
    rules:
        -   host: f1goat.example.com       # overlay переопределит
            http:
                paths:
                    -   path: /
                        pathType: Prefix
                        backend:
                            service:
                                name: web
                                port:
                                    number: 80
```

### Важно: nginx.conf для Kubernetes

В текущем nginx.conf `proxy_pass` смотрит на `http://backend:8000`.
В Kubernetes DNS-имя сервиса будет `analytics-api` (имя из Service).
Нужно обновить:

```nginx
location /api/ {
    rewrite ^/api/(.*) /$1 break;
    proxy_pass http://analytics-api:8000;    # ← изменить hostname
}
```

### Задачи

- [ ] Создать структуру `k8s/base/`
- [ ] Написать Deployment + Service для analytics-api
- [ ] Написать Deployment + Service + Ingress для web
- [ ] Обновить `nginx.conf`: `backend` → `analytics-api`
- [ ] Проверить в minikube: `kubectl apply -k k8s/base/`
- [ ] Port-forward и проверить работу: `kubectl port-forward svc/web 8080:80`

---

## 6. Фаза 3 — PostgreSQL в кластере

### Почему StatefulSet, а не Deployment

Deployment создаёт "одноразовые" Pod'ы с рандомными именами. Если Pod
пересоздался, его PersistentVolume может не переподключиться корректно.
StatefulSet гарантирует:

1. Стабильные имена (`postgres-0`, не `postgres-7f8b4a2c`)
2. Стабильную привязку к PersistentVolume
3. Упорядоченное создание/удаление

### base/postgres/statefulset.yaml

```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
    name: postgres
    labels:
        app: postgres
spec:
    serviceName: postgres           # связь с Headless Service
    replicas: 1                     # одна реплика — для pet-project достаточно
    selector:
        matchLabels:
            app: postgres
    template:
        metadata:
            labels:
                app: postgres
        spec:
            containers:
                -   name: postgres
                    image: postgres:18.2-alpine
                    ports:
                        -   containerPort: 5432
                    env:
                        -   name: POSTGRES_DB
                            valueFrom:
                                configMapKeyRef:
                                    name: db-config
                                    key: POSTGRES_DB
                        -   name: POSTGRES_USER
                            valueFrom:
                                secretKeyRef:
                                    name: db-credentials
                                    key: POSTGRES_USER
                        -   name: POSTGRES_PASSWORD
                            valueFrom:
                                secretKeyRef:
                                    name: db-credentials
                                    key: POSTGRES_PASSWORD
                        -   name: POSTGRES_INITDB_ARGS
                            value: "--encoding=UTF8 --locale=C"
                    volumeMounts:
                        -   name: postgres-data
                            mountPath: /var/lib/postgresql/data
                    resources:
                        requests:
                            memory: "256Mi"
                            cpu: "100m"
                        limits:
                            memory: "512Mi"
                            cpu: "500m"
                    readinessProbe:
                        exec:
                            command:
                                - pg_isready
                                - -U
                                - f1user                    # будет переопределено через env
                                - -d
                                - f1_goat_determiner
                        initialDelaySeconds: 5
                        periodSeconds: 10
    volumeClaimTemplates: # ← StatefulSet автоматически создаёт PVC
        -   metadata:
                name: postgres-data
            spec:
                accessModes: [ "ReadWriteOnce" ]
                resources:
                    requests:
                        storage: 2Gi            # 2GB — для F1-данных более чем достаточно
```

### base/postgres/service.yaml

```yaml
apiVersion: v1
kind: Service
metadata:
    name: postgres
spec:
    type: ClusterIP
    selector:
        app: postgres
    ports:
        -   port: 5432
            targetPort: 5432
```

### Миграции (Flyway)

Миграции запускает `data-sync-svc` при старте (Spring Boot + Flyway).
На этапе первого деплоя база будет пустой. Порядок наполнения:

1. PostgreSQL Pod стартует → пустая база с пользователем
2. Запуск data-sync-svc (Job или вручную) → Flyway накатывает миграции
    + синхронизация данных из Jolpica API
3. analytics-api начинает отвечать с данными

Подробнее — в Фазе 9.

### Задачи

- [ ] Написать StatefulSet для PostgreSQL
- [ ] Написать Service для PostgreSQL
- [ ] Проверить в minikube: Pod стартует, `pg_isready` проходит
- [ ] Подключиться к БД через `kubectl exec` и проверить:
  `kubectl exec -it postgres-0 -- psql -U f1user -d f1_goat_determiner`

---

## 7. Фаза 4 — Секреты и конфигурация

### Что куда

| Что                  | Где                    | Почему                               |
|----------------------|------------------------|--------------------------------------|
| `POSTGRES_DB`        | ConfigMap              | Не секрет, нужен нескольким сервисам |
| `POSTGRES_USER`      | Secret                 | Credential, не должен быть в git     |
| `POSTGRES_PASSWORD`  | Secret                 | Credential, не должен быть в git     |
| `PYTHONUNBUFFERED`   | Прямо в Deployment env | Не меняется между окружениями        |
| `DB_HOST`, `DB_PORT` | Прямо в Deployment env | Фиксированные значения               |
| SSH-ключ для CI      | GitHub Actions Secrets | Нужен только в CI                    |
| Kubeconfig           | GitHub Actions Secrets | Нужен только в CI                    |
| DockerHub token      | GitHub Actions Secrets | Уже настроен                         |

### ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
    name: db-config
data:
    POSTGRES_DB: f1_goat_determiner
```

### Secret (НЕ коммитим в git!)

Создаётся императивно через kubectl:

```bash
kubectl create secret generic db-credentials \
  -n prod \
  --from-literal=POSTGRES_USER=f1user \
  --from-literal=POSTGRES_PASSWORD=<strong-random-password>
```

Для разных namespace — разные секреты (разные пароли на prod и test).

### Почему НЕ Vault

HashiCorp Vault решает проблемы, которых у нас нет:

- Ротация секретов по расписанию → у нас один пароль БД, меняем вручную
- Динамические credentials → у нас статичный пользователь
- Аудит доступа к секретам → один разработчик
- Multi-team RBAC → команды нет

Kubernetes Secrets + GitHub Actions Secrets покрывают все потребности
pet-проекта. Vault можно добавить позже как отдельное учебное упражнение.

### Задачи

- [ ] Создать ConfigMap для `db-config`
- [ ] Создать Secret `db-credentials` через `kubectl create secret`
- [ ] Проверить, что Pod'ы подхватывают переменные:
  `kubectl exec <pod> -- env | grep DB_`
- [ ] НЕ коммитить Secret в git (добавить `k8s/**/secret*.yaml` в .gitignore)

---

## 8. Фаза 5 — Поднятие боевого кластера (k3s на VPS)

### Важно: у нас ОДИН кластер

Два VPS — это **не** два кластера и не два "ДЦ". Это две ноды
**одного** кластера с единым control plane. Server-нода и agent-нода
равноправны в плане запуска Pod'ов, отличие только в том, что
на server-ноде дополнительно живёт control plane (API Server, etcd,
Scheduler). Подробнее о терминологии — в Приложении A.

DNS будет указывать на IP server-ноды. Это единственная точка входа.
Если server-нода упадёт — приложение недоступно, несмотря на живую
agent-ноду. Для pet-проекта это нормально. Для отказоустойчивости
можно добавить Hetzner Load Balancer (~5€/мес) — он будет распределять
трафик на обе ноды и автоматически исключать упавшую.

### Выбор хостинга

| Провайдер    | Минимальный VPS           | Цена (2 VPS) | Плюсы                                   |
|--------------|---------------------------|--------------|-----------------------------------------|
| **Hetzner**  | CX22 (2 vCPU, 4GB RAM)    | ~€10/мес     | Дёшево, отличная сеть, дата-центры в EU |
| **TimeWeb**  | Cloud-1 (2 vCPU, 2GB RAM) | ~600₽/мес    | Российский хостинг, рублёвая оплата     |
| **Selectel** | 1 vCPU / 2GB RAM          | ~800₽/мес    | Российский, хороший uptime              |

Рекомендация: **Hetzner CX22** (или аналог у TimeWeb). Для k3s на ноде
достаточно 2GB RAM, но 4GB комфортнее (PostgreSQL + приложение + сам k3s).

### Подготовка серверов

На обоих VPS (Ubuntu 22.04 / 24.04):

```bash
# Обновить пакеты
apt update && apt upgrade -y

# Открыть порты на firewall (если используется ufw)
ufw allow 6443/tcp    # Kubernetes API (только на server)
ufw allow 80/tcp      # HTTP
ufw allow 443/tcp     # HTTPS
ufw allow 10250/tcp   # kubelet
ufw allow 51820/udp   # WireGuard (связь между нодами в k3s)
```

### Установка k3s — Server нода

```bash
# На первом VPS (server)
curl -sfL https://get.k3s.io | sh -

# Проверить, что нода Ready
kubectl get nodes

# Забрать join-token для второй ноды
cat /var/lib/rancher/k3s/server/node-token
```

### Установка k3s — Agent нода

```bash
# На втором VPS (agent)
curl -sfL https://get.k3s.io | \
  K3S_URL=https://<SERVER_IP>:6443 \
  K3S_TOKEN=<TOKEN_FROM_SERVER> \
  sh -
```

### Настройка kubeconfig на своей машине

```bash
# На server-ноде
cat /etc/rancher/k3s/k3s.yaml

# Скопировать содержимое на свою машину в ~/.kube/config
# Заменить 127.0.0.1 на публичный IP server-ноды
```

Проверка с локальной машины:

```bash
kubectl get nodes
# NAME           STATUS   ROLES                  AGE   VERSION
# server-node    Ready    control-plane,master   5m    v1.31.x+k3s1
# agent-node     Ready    <none>                 2m    v1.31.x+k3s1
```

### Задачи

- [ ] Арендовать 2 VPS
- [ ] Установить k3s (server + agent)
- [ ] Проверить `kubectl get nodes` — обе ноды Ready
- [ ] Скопировать kubeconfig на локальную машину
- [ ] Сохранить kubeconfig в GitHub Actions Secrets (base64-encoded)

---

## 9. Фаза 6 — Домен и HTTPS (cert-manager)

### Домен

Покупаешь домен у любого регистратора. Настраиваешь DNS A-записи:

```
f1goat.example.com       → <SERVER_IP>
test.f1goat.example.com  → <SERVER_IP>
```

Traefik в k3s слушает 80 и 443 на всех нодах. Ingress Controller
посмотрит на Host header входящего запроса и направит в нужный Service.

### cert-manager

cert-manager — это Kubernetes-оператор, который автоматически выпускает
и продлевает TLS-сертификаты. Работает с Let's Encrypt.

Установка:

```bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.17.2/cert-manager.yaml

# Подождать, пока все Pod'ы в namespace cert-manager станут Ready
kubectl get pods -n cert-manager --watch
```

### ClusterIssuer

ClusterIssuer — глобальный ресурс, описывающий *как* получать сертификаты:

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
    name: letsencrypt-prod
spec:
    acme:
        server: https://acme-v02.api.letsencrypt.org/directory
        email: your-email@example.com       # ← для уведомлений о сертификатах
        privateKeySecretRef:
            name: letsencrypt-prod-key
        solvers:
            -   http01:
                    ingress:
                        class: traefik              # Ingress Controller в k3s
```

Когда ты создаёшь Ingress с аннотацией
`cert-manager.io/cluster-issuer: letsencrypt-prod`, cert-manager
автоматически:

1. Создаёт временный Ingress для ACME HTTP-01 challenge
2. Let's Encrypt проверяет, что ты контролируешь домен
3. Сертификат сохраняется в Kubernetes Secret (`f1-goat-tls`)
4. Traefik подхватывает сертификат и начинает обслуживать HTTPS
5. За 30 дней до истечения cert-manager продлит сертификат

### Совет: сначала используй staging

Let's Encrypt имеет лимит на выпуск сертификатов (50 в неделю на домен).
При отладке используй staging-issuer, чтобы не упереться в лимит:

```yaml
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
    name: letsencrypt-staging
spec:
    acme:
        server: https://acme-staging-v02.api.letsencrypt.org/directory
        # ... остальное идентично
```

Когда всё заработает — переключи Ingress на `letsencrypt-prod`.

### Задачи

- [ ] Купить домен
- [ ] Настроить DNS A-записи
- [ ] Установить cert-manager
- [ ] Создать ClusterIssuer (сначала staging)
- [ ] Проверить, что сертификат выпустился:
  `kubectl get certificate -n prod`
- [ ] Переключить на letsencrypt-prod

---

## 10. Фаза 7 — CI/CD для Kubernetes

### Текущий CI (main-ci.yml)

Уже делает: detect-changes → lint → test → build & push Docker images.
Нужно добавить: **шаг деплоя** после успешной сборки образов.

### Стратегия деплоя

```
push to main
    │
    ▼
detect-changes → lint → test → build & push images
                                      │
                                      ▼
                              deploy to prod (kubectl)
```

### GitHub Actions: деплой

```yaml
  deploy-prod:
      name: Deploy to Production
      needs: [ analytics-api-build, web-build, data-sync-build ]
      if: |
          always() &&
          !contains(needs.*.result, 'failure') &&
          !contains(needs.*.result, 'cancelled')
      runs-on: ubuntu-latest
      steps:
          -   uses: actions/checkout@v4

          -   name: Set up kubectl
              uses: azure/setup-kubectl@v4

          -   name: Configure kubeconfig
              run: |
                  mkdir -p ~/.kube
                  echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > ~/.kube/config

          -   name: Update images
              run: |
                  cd k8s/overlays/prod
                  kustomize edit set image \
                    remsely/f1-goat-analytics-api=remsely/f1-goat-analytics-api:${{ github.sha }} \
                    remsely/f1-goat-web=remsely/f1-goat-web:${{ github.sha }} \
                    remsely/f1-goat-data-sync-svc=remsely/f1-goat-data-sync-svc:${{ github.sha }}
                  kubectl apply -k .

          -   name: Wait for rollout
              run: |
                  kubectl rollout status deployment/analytics-api -n prod --timeout=120s
                  kubectl rollout status deployment/web -n prod --timeout=120s
                  kubectl rollout status deployment/data-sync-svc -n prod --timeout=120s
```

### Деплой на тест (ручной approve в PR, как `when: manual` в GitLab)

В GitHub Actions нет прямого аналога GitLab `when: manual` на уровне
отдельного job'а. Но есть механизм **environments с required reviewers**,
который даёт похожий UX.

Как это работает:

1. В настройках репозитория (Settings → Environments) создаёшь
   environment `test` и ставишь галку "Required reviewers" (указываешь себя).
2. В workflow job'е указываешь `environment: test`.
3. Когда pipeline доходит до этого job'а — он **встаёт на паузу**
   и показывает кнопку "Review deployments" в UI GitHub.
4. Ты нажимаешь Approve — job запускается. Или не нажимаешь — и он
   просто висит, не блокируя остальной пайплайн.

```yaml
  deploy-test:
      name: Deploy to Test
      needs: [ analytics-api-build, web-build, data-sync-build ]
      if: |
          always() &&
          !contains(needs.*.result, 'failure') &&
          !contains(needs.*.result, 'cancelled')
      runs-on: ubuntu-latest
      environment: test                    # ← ждёт ручного approve
      steps:
          -   uses: actions/checkout@v4

          -   name: Set up kubectl
              uses: azure/setup-kubectl@v4

          -   name: Configure kubeconfig
              run: |
                  mkdir -p ~/.kube
                  echo "${{ secrets.KUBE_CONFIG }}" | base64 -d > ~/.kube/config

          -   name: Deploy to test
              run: |
                  cd k8s/overlays/test
                  kustomize edit set image \
                    remsely/f1-goat-analytics-api=remsely/f1-goat-analytics-api:${{ github.sha }} \
                    remsely/f1-goat-web=remsely/f1-goat-web:${{ github.sha }} \
                    remsely/f1-goat-data-sync-svc=remsely/f1-goat-data-sync-svc:${{ github.sha }}
                  kubectl apply -k .

          -   name: Wait for rollout
              run: |
                  kubectl rollout status deployment/analytics-api -n test --timeout=120s
                  kubectl rollout status deployment/web -n test --timeout=120s
                  kubectl rollout status deployment/data-sync-svc -n test --timeout=120s
```

В UI GitHub PR это выглядит как жёлтая плашка "Review deployments"
прямо в разделе Checks. Нажал Approve → деплой на тест. Не нажал →
ничего не происходит, PR можно мерджить без деплоя на тест.

Дополнительно можно создать `workflow_dispatch` workflow для деплоя
на тест вне контекста PR (например, деплой конкретного тега).

### Какие секреты добавить в GitHub

| Secret               | Значение            |
|----------------------|---------------------|
| `KUBE_CONFIG`        | `cat ~/.kube/config | base64` — kubeconfig server-ноды |
| `DOCKERHUB_USERNAME` | Уже есть            |
| `DOCKERHUB_TOKEN`    | Уже есть            |

### Задачи

- [ ] Добавить `KUBE_CONFIG` в GitHub Actions Secrets
- [ ] Добавить deploy step в `main-ci.yml`
- [ ] Создать `deploy-test.yml` с `workflow_dispatch`
- [ ] Проверить: пуш в main → образы собираются → деплой в prod
- [ ] Проверить: ручной запуск → деплой в test

---

## 11. Фаза 8 — Два окружения (prod / test)

### Overlays: как это работает

`overlays/prod/kustomization.yaml` не копирует весь base, а описывает
только отличия:

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: prod                    # все ресурсы попадут в namespace prod

resources:
    - ../../base

patches:
    -   path: ingress-patch.yaml

replicas:
    -   name: analytics-api
        count: 2                       # 2 реплики в проде

images:
    -   name: remsely/f1-goat-analytics-api
        newTag: latest                 # CI заменит на конкретный SHA
    -   name: remsely/f1-goat-web
        newTag: latest
    -   name: remsely/f1-goat-data-sync-svc
        newTag: latest
```

### overlays/prod/ingress-patch.yaml

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
    name: f1-goat-ingress
spec:
    tls:
        -   hosts:
                - f1goat.example.com
            secretName: f1-goat-tls-prod
    rules:
        -   host: f1goat.example.com
            http:
                paths:
                    -   path: /
                        pathType: Prefix
                        backend:
                            service:
                                name: web
                                port:
                                    number: 80
```

### overlays/test/kustomization.yaml

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: test

resources:
    - ../../base

patches:
    -   path: ingress-patch.yaml

replicas:
    -   name: analytics-api
        count: 1                       # 1 реплика на тесте

images:
    -   name: remsely/f1-goat-analytics-api
        newTag: latest
    -   name: remsely/f1-goat-web
        newTag: latest
    -   name: remsely/f1-goat-data-sync-svc
        newTag: latest
```

### Создание namespace и секретов

```bash
# Namespace'ы
kubectl create namespace prod
kubectl create namespace test

# Секреты — отдельно для каждого namespace
kubectl create secret generic db-credentials \
  -n prod \
  --from-literal=POSTGRES_USER=f1user \
  --from-literal=POSTGRES_PASSWORD=<prod-password>

kubectl create secret generic db-credentials \
  -n test \
  --from-literal=POSTGRES_USER=f1user \
  --from-literal=POSTGRES_PASSWORD=<test-password>

# ConfigMap — тоже в каждый namespace
kubectl apply -f k8s/base/db-config.yaml -n prod
kubectl apply -f k8s/base/db-config.yaml -n test
```

### Деплой

```bash
kubectl apply -k k8s/overlays/prod
kubectl apply -k k8s/overlays/test
```

### Задачи

- [ ] Создать overlay для prod
- [ ] Создать overlay для test
- [ ] Создать namespace'ы
- [ ] Создать секреты и ConfigMap в каждом namespace
- [ ] Проверить: `kubectl get all -n prod` и `kubectl get all -n test`

---

## 12. Фаза 9 — data-sync-svc как постоянный сервис

### Роль в кластере

data-sync-svc — постоянно работающий сервис. Spring Boot + ShedLock
управляет расписанием синхронизации внутри приложения. При старте
Flyway накатывает миграции, затем ShedLock запускает cron-задачи
по расписанию.

Это обычный **Deployment** (не Job и не CronJob), потому что:

- Сервис должен жить постоянно (не "запустился и умер")
- ShedLock гарантирует, что только один инстанс выполняет sync-задачу
- Flyway-миграции применяются при каждом старте (идемпотентно)

### Подготовка: Dockerfile для data-sync-svc

Сейчас Dockerfile отсутствует, а сборка в CI отключена (`if: false`).
Нужно создать Dockerfile:

```dockerfile
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :app:bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Kubernetes-манифесты

#### base/data-sync-svc/deployment.yaml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
    name: data-sync-svc
    labels:
        app: data-sync-svc
spec:
    replicas: 1                       # Только 1 реплика — ShedLock не нуждается в нескольких
    selector:
        matchLabels:
            app: data-sync-svc
    template:
        metadata:
            labels:
                app: data-sync-svc
        spec:
            containers:
                -   name: data-sync-svc
                    image: remsely/f1-goat-data-sync-svc:latest
                    env:
                        -   name: SPRING_DATASOURCE_URL
                            value: jdbc:postgresql://postgres:5432/f1_goat_determiner
                        -   name: SPRING_DATASOURCE_USERNAME
                            valueFrom:
                                secretKeyRef:
                                    name: db-credentials
                                    key: POSTGRES_USER
                        -   name: SPRING_DATASOURCE_PASSWORD
                            valueFrom:
                                secretKeyRef:
                                    name: db-credentials
                                    key: POSTGRES_PASSWORD
                    resources:
                        requests:
                            memory: "256Mi"
                            cpu: "100m"
                        limits:
                            memory: "512Mi"
                            cpu: "500m"
                    readinessProbe:
                        httpGet:
                            path: /actuator/health
                            port: 8080
                        initialDelaySeconds: 15
                        periodSeconds: 10
                    livenessProbe:
                        httpGet:
                            path: /actuator/health
                            port: 8080
                        initialDelaySeconds: 30
                        periodSeconds: 30
```

data-sync-svc **не нуждается в Service** — к нему никто не обращается
извне. Он сам ходит в PostgreSQL и Jolpica API. Если позже
понадобится Actuator endpoint — можно добавить ClusterIP Service.

### Порядок первого запуска

1. PostgreSQL Pod стартует → пустая база
2. data-sync-svc Pod стартует → Flyway накатывает миграции → создаются таблицы
3. ShedLock запускает первую синхронизацию → данные из Jolpica API → PostgreSQL
4. analytics-api начинает отвечать с данными

Первая полная синхронизация может занять несколько минут (зависит от
rate limits Jolpica API). analytics-api до этого момента будет отвечать
пустыми результатами — это нормально.

### Альтернатива для ускорения первого деплоя: pg_dump

Если data-sync-svc уже наполнил локальную базу, можно ускорить
первый деплой через дамп:

```bash
# Локально: сделать дамп
pg_dump -h localhost -p 5433 -U f1user f1_goat_determiner > dump.sql

# Скопировать дамп в Pod
kubectl cp dump.sql prod/postgres-0:/tmp/dump.sql

# Восстановить
kubectl exec -it postgres-0 -n prod -- \
  psql -U f1user -d f1_goat_determiner -f /tmp/dump.sql
```

После этого data-sync-svc при старте увидит, что миграции уже
применены, и перейдёт в режим инкрементальной синхронизации.

### Обновление CI

Включить сборку data-sync-svc в `main-ci.yml` (убрать `if: false`):

```yaml
  data-sync-build:
      name: Data Sync - Build & Push
      needs: data-sync-verify
      runs-on: ubuntu-latest
      steps:
          -   uses: actions/checkout@v4
          -   uses: docker/setup-buildx-action@v3
          -   uses: docker/login-action@v3
              with:
                  username: ${{ secrets.DOCKERHUB_USERNAME }}
                  password: ${{ secrets.DOCKERHUB_TOKEN }}
          -   uses: docker/build-push-action@v5
              with:
                  context: ./data-sync-svc
                  push: true
                  tags: |
                      ${{ env.DOCKERHUB_USERNAME }}/f1-goat-data-sync-svc:latest
                      ${{ env.DOCKERHUB_USERNAME }}/f1-goat-data-sync-svc:${{ github.sha }}
                  cache-from: type=gha
                  cache-to: type=gha,mode=max
```

### Добавить в base/kustomization.yaml

```yaml
resources:
    # ... existing resources ...
    - data-sync-svc/deployment.yaml
```

### Задачи

- [ ] Создать `data-sync-svc/Dockerfile`
- [ ] Включить сборку в CI (убрать `if: false` в `main-ci.yml`)
- [ ] Написать Deployment-манифест для data-sync-svc
- [ ] Добавить в base/kustomization.yaml
- [ ] Проверить в minikube: Pod стартует, Flyway накатывает миграции
- [ ] Проверить: analytics-api отдаёт данные после синхронизации

---

## 13. Чек-лист готовности

### Инфраструктура

- [ ] 2 VPS арендованы и настроены
- [ ] k3s установлен (server + agent), обе ноды Ready
- [ ] Домен куплен, DNS настроен
- [ ] cert-manager установлен, ClusterIssuer создан
- [ ] kubeconfig сохранён в GitHub Actions Secrets

### Образы

- [ ] `f1-goat-web` (объединённый nginx + frontend) собирается и пушится
- [ ] `f1-goat-analytics-api` собирается и пушится
- [ ] `f1-goat-data-sync-svc` собирается и пушится
- [ ] CI обновлён под новую структуру образов

### Kubernetes-манифесты

- [ ] base/ манифесты написаны и протестированы в minikube
- [ ] overlays/prod и overlays/test настроены
- [ ] PostgreSQL StatefulSet с PVC работает
- [ ] data-sync-svc Deployment работает (Flyway + синхронизация)
- [ ] Ingress маршрутизирует трафик корректно

### Секреты и данные

- [ ] db-credentials Secret создан в обоих namespace'ах
- [ ] db-config ConfigMap создан в обоих namespace'ах
- [ ] База данных наполнена (data-sync-svc синхронизация или pg_dump)

### CI/CD

- [ ] Push в main → автодеплой в prod
- [ ] Environment `test` создан с required reviewers
- [ ] Ручной approve в PR → деплой в test
- [ ] Rollout status проверяется в CI

### Проверка

- [ ] `https://f1goat.example.com` открывается, показывает тир-лист
- [ ] `https://test.f1goat.example.com` работает отдельно
- [ ] API отвечает: `https://f1goat.example.com/api/`
- [ ] Сертификат Let's Encrypt валиден (не staging)
- [ ] 2 реплики analytics-api в prod на разных нодах:
  `kubectl get pods -n prod -o wide`
- [ ] data-sync-svc Pod работает, синхронизация проходит:
  `kubectl logs deployment/data-sync-svc -n prod`

---

## 14. Приложение A — Терминология: кластер, нода, control plane

### Нода

Нода — это одна машина (физическая или виртуальная), на которой
установлен kubelet (Kubernetes-агент). Нода умеет запускать Pod'ы.
Сама по себе нода — это просто сервер с Linux. Без подключения
к control plane она не знает, какие Pod'ы запускать.

Наши два VPS — это две ноды.

### Кластер

Кластер — это **группа нод**, объединённых одним control plane.
Control plane + все подключённые к нему ноды = один кластер.

У нас **один кластер из двух нод**. Не два кластера, не два "ДЦ".
Одна система с единым управлением. Все Pod'ы видят друг друга
по внутренней сети, все управляются одним API Server'ом.

### Control plane

Control plane — "мозг" кластера. Состоит из:

- **API Server** — единственная точка входа для управления. Все
  команды `kubectl`, все запросы от kubelet'ов идут сюда. Все
  компоненты общаются друг с другом только через API Server.
- **etcd** — distributed key-value store, где хранится всё состояние
  кластера: какие Deployment'ы существуют, сколько реплик, какие
  Pod'ы на каких нодах, содержимое ConfigMap'ов и Secret'ов.
- **Scheduler** — когда нужно создать новый Pod, scheduler решает,
  на какую ноду его разместить. Учитывает доступные ресурсы (CPU,
  RAM), affinity-правила, taints/tolerations.
- **Controller Manager** — набор контроллеров, которые непрерывно
  сверяют реальное состояние с желаемым. Пример: ReplicaSet controller
  видит "нужно 2 Pod'а analytics-api, а есть 1" → просит scheduler
  создать ещё один. Deployment controller видит "образ обновился" →
  запускает rolling update.

В k3s все компоненты control plane упакованы в один бинарник и
работают на server-ноде. Agent-нода не имеет control plane.

### Server-нода vs Agent-нода

|                      | Server-нода      | Agent-нода |
|----------------------|------------------|------------|
| Control plane        | ✅ Да             | ❌ Нет      |
| Запускает Pod'ы      | ✅ Да             | ✅ Да       |
| kubelet + kube-proxy | ✅ Да             | ✅ Да       |
| Traefik (DaemonSet)  | ✅ Да             | ✅ Да       |
| Принимает `kubectl`  | ✅ Да (порт 6443) | ❌ Нет      |

Обе ноды равноправны в плане запуска рабочих Pod'ов. Server-нода
просто несёт на себе дополнительную нагрузку в виде control plane.

### Аналогия с ya-a / ya-b

ya-a и ya-b у Яндекса — это **зоны доступности** (availability zones).
Каждая зона — физически отдельный дата-центр (отдельное питание,
охлаждение, сеть). Внутри каждой зоны — десятки-сотни серверов,
свой кластер (или часть мульти-зонного кластера), своя реплика данных.
Над зонами стоит глобальный балансировщик.

Наша схема — это **не** аналог ya-a / ya-b. У нас один кластер
из двух машин в одном дата-центре. Для настоящей зональной
отказоустойчивости нужен совсем другой масштаб:

- 3+ server-ноды (etcd требует кворум для HA control plane)
- ноды в разных физических дата-центрах
- репликация данных (PostgreSQL streaming replication)
- внешний балансировщик перед кластером

Это enterprise-уровень, для pet-проекта нерелевантно. Наши две ноды
дают опыт работы с мульти-нодным кластером, scheduling Pod'ов
по разным машинам и overlay network — а не настоящую отказоустойчивость.
