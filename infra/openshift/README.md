# StockInteligence no OpenShift — guia completo

Este guia documenta a implantação real do backend (feature 001 + 002, com
mensageria Kafka) e do frontend (React + Vite) no **Red Hat Developer
Sandbox** — um cluster OpenShift de verdade, gratuito, sem cartão de
crédito. Escrito para quem nunca mexeu com OpenShift antes: explica o
conceito antes do comando.

**URLs públicas em produção:**
- Frontend: `https://frontend-enzoblousa-dev.apps.rm2.thpm.p1.openshiftapps.com`
- Backend (API): `https://backend-enzoblousa-dev.apps.rm2.thpm.p1.openshiftapps.com`
- notification-service (alertas): `https://notification-enzoblousa-dev.apps.rm2.thpm.p1.openshiftapps.com`

(confirme com `oc get route <nome> -o jsonpath='{.spec.host}'` — pode
mudar se o namespace for recriado.)

> Pra explorar o cluster no dia a dia (ver logs, entrar no banco, debugar
> um pod que não sobe, etc.), ver o guia de comandos `oc`:
> `comandos-oc.md`.

## Índice

1. [Conceitos básicos](#1-conceitos-básicos)
2. [Arquitetura implantada](#2-arquitetura-implantada)
3. [Pré-requisitos](#3-pré-requisitos)
4. [Passo a passo completo](#4-passo-a-passo-completo)
5. [Como verificar que está funcionando](#5-como-verificar-que-está-funcionando)
6. [Como atualizar depois de uma mudança de código](#6-como-atualizar-depois-de-uma-mudança-de-código)
7. [Problemas reais encontrados (troubleshooting)](#7-problemas-reais-encontrados-troubleshooting)
8. [Limitações conhecidas](#8-limitações-conhecidas)

---

## 1. Conceitos básicos

Se você já usou `docker-compose` neste projeto (o Dev Services do Quarkus
sobe Postgres e Kafka em containers automaticamente), a ideia é a mesma —
"rodar containers" — só que num **cluster** (várias máquinas gerenciadas
como uma só) em vez da sua máquina.

| Termo | O que é | Equivalente no `docker-compose` |
|---|---|---|
| **Project** (namespace) | Uma "pasta" isolada com todos os seus recursos | O seu `docker-compose.yml` |
| **Pod** | Uma instância rodando de um container | Um container rodando |
| **Deployment** | "Quero N cópias deste container sempre rodando" | Um serviço do Compose |
| **Service** | Endereço de rede interno fixo pros pods se acharem | A rede interna automática do Compose |
| **Route** | Expõe um Service pra internet com URL pública HTTPS (só existe no OpenShift, não em Kubernetes puro) | `ports: "8080:8080"`, mas virando uma URL de verdade |
| **Secret** | Guarda senha/credencial sem deixar em texto puro no manifest | Variáveis de ambiente do `%prod.*` |
| **PersistentVolumeClaim (PVC)** | Pedido de disco que sobrevive a reinício de container | `volumes:` do Compose |
| **BuildConfig** | "Receita" de como construir a imagem Docker dentro do próprio cluster | `docker build` |
| **ImageStream** | "Prateleira" interna onde a imagem construída fica guardada | O registry local do Docker Hub, mas dentro do cluster |
| **SCC (Security Context Constraints)** | Regra de segurança do OpenShift que controla com que usuário/permissões um container pode rodar | Não existe equivalente direto no Compose — ver seção 2 |
| **`oc`** | Comando de terminal pra falar com o cluster | `docker compose ...` |

## 2. Arquitetura implantada

```
                    ┌──────────────────────┐   ┌──────────────────────┐
  internet ────────▶│ Route "frontend"       │   │ Route "backend"        │◀──── internet
                    │ (HTTPS, edge TLS)      │   │ (HTTPS, edge TLS)      │
                    └──────────┬───────────┘   └──────────┬───────────┘
                               ▼                          ▼
                    ┌──────────────────────┐   ┌──────────────────────┐
                    │ Service "frontend"     │   │ Service "backend"      │
                    │      (8080)             │   │      (8080)             │
                    └──────────┬───────────┘   └──────────┬───────────┘
                               ▼                          ▼
                    ┌──────────────────────┐   ┌──────────────────────┐
                    │ Deployment "frontend"  │   │ Deployment "backend"   │
                    │ nginx (ubi9/nginx-124) │   │ Quarkus (ubi9/openjdk) │
                    │ servindo o build        │   │ construído pelo         │
                    │ estático do Vite         │──▶│ BuildConfig binary      │
                    │ (chama o backend via     │   │ build                    │
                    │  VITE_API_BASE_URL,      │   └───────┬─────────┬────┘
                    │  já embutido no build)   │           ▼         ▼
                    └──────────────────────┘  ┌───────────────┐ ┌───────────┐
                                                │Service         │ │Service    │
                                                │"postgresql"    │ │"kafka"     │
                                                │   (5432)        │ │  (9092)    │
                                                └───────┬───────┘ └─────┬─────┘
                                                        ▼               ▼
                                                ┌───────────────┐ ┌───────────┐
                                                │Deployment       │ │Deployment  │
                                                │postgresql        │ │kafka        │◀─┐
                                                │quay.io/sclorg/   │ │apache/kafka-│  │
                                                │postgresql-16-c9s │ │native (KRaft│  │
                                                │+ PVC (1Gi)        │ │nó único)     │  │consome
                                                └───────────────┘ │sem PVC (§8)  │  │(@Incoming)
                                                                    └───────────┘  │
                                                                                    │
                                          ┌─────────────────────┐   ┌──────────────┴──┐
  internet ──────────────────────────────▶│ Route "notification" │   │ Deployment        │
  (o frontend chama esta URL direto,       │ (HTTPS, edge TLS)     │   │ "notification"     │
   igual faz com a do backend)            └──────────┬──────────┘   │ Quarkus, sem banco │
                                                       ▼               │ (alertas em          │
                                          ┌─────────────────────┐   │  memória, ver §8)    │
                                          │ Service "notification"│──▶│ replicas: 1 (ver     │
                                          │      (8080)             │   │  infra/openshift/    │
                                          └─────────────────────┘   │  19-notification-...) │
                                                                       └────────────────────┘
```

Nenhum dos cinco componentes usa um serviço "gerenciado" (tipo Amazon RDS
ou um CDN) — são todos containers simples rodando dentro do seu próprio
namespace, porque o Developer Sandbox não permite instalar operadores nem
provisionar serviços gerenciados (você não é administrador do cluster). O
frontend fala com o backend **e** com o notification-service **direto
pelas URLs públicas das Routes**, não por um Service interno — o
navegador de quem acessa o site é quem faz essas chamadas, então precisam
ser endereços alcançáveis de fora do cluster (por isso
`VITE_API_BASE_URL`/`VITE_ALERTAS_API_BASE_URL` apontam pras Routes, não
pros Services). Já o `notification-service` fala com o Kafka **por dentro
do cluster** (`kafka:9092`, o Service interno) — ele não precisa de URL
pública pra isso, só a Route serve pra expor `GET /alertas` ao navegador.

### Por que essas imagens específicas, e não as "óbvias"

- **Postgres não é a imagem oficial do Docker Hub.** Por padrão, o
  OpenShift roda todo container sob uma regra de segurança chamada SCC
  `restricted`: o cluster escolhe um número de usuário (UID) aleatório pro
  container, em vez de deixar ele rodar como `root` ou com um UID fixo.
  Isso quebra várias imagens genéricas — inclusive o Postgres oficial, que
  espera rodar com um usuário fixo. A imagem `quay.io/sclorg/postgresql-16-c9s`
  (mantida pela própria Red Hat, open source, sem custo, sem precisar de
  login) foi desenhada especificamente pra funcionar com qualquer UID que o
  cluster atribuir.
- **Kafka é a mesma imagem que você já usa localmente** (`apache/kafka-native`,
  a que o Dev Services do Quarkus sobe sozinho em dev) — ela já roda como
  usuário não-root por padrão, então funciona sem ajuste nenhum aqui.
- **O `Dockerfile.jvm` do backend não foi alterado.** Ele já usa uma imagem
  de base da Red Hat (`ubi9/openjdk-21-runtime`) e já roda como usuário
  `185` (não-root) — foi feito pra funcionar em OpenShift desde o início,
  sem precisar de nenhuma mudança pra este deploy.
- **Frontend: `registry.access.redhat.com/ubi9/nginx-124`**, não uma
  imagem `nginx` genérica do Docker Hub — pela mesma razão do Postgres
  (compatibilidade com UID arbitrário). O `frontend/Dockerfile` é
  multi-stage: um estágio Node.js (`ubi9/nodejs-20`) só compila o React
  (`npm run build`), e a imagem final leva só os arquivos estáticos
  resultantes + nginx, sem sobrar nenhuma ferramenta de build.

## 3. Pré-requisitos

- Conta gratuita no [Red Hat Developer Sandbox](https://sandbox.redhat.com) (só e-mail, sem cartão).
- CLI `oc` instalado — no Windows: `winget install --id RedHat.OpenShift-Client -e`.
- Login feito: `oc login --token=... --server=https://api.rm2.thpm.p1.openshiftapps.com:6443`
  (o comando exato aparece no console web, menu do seu usuário no canto
  superior direito → "Copy login command").
- `./mvnw clean package` funcionando localmente no `backend/` (nenhuma
  ferramenta OpenShift-específica precisa estar instalada além do `oc`).
- `npm ci` / `npm run build` funcionando localmente no `frontend/` (Node.js
  instalado). **Feche qualquer `npm run dev` que esteja rodando antes de
  reinstalar dependências** — ver §7, é um problema real que já aconteceu.

## 4. Passo a passo completo

```powershell
# 0. Confirmar contexto antes de aplicar qualquer coisa
oc whoami
oc project enzoblousa-dev
oc get storageclass   # confere qual classe de armazenamento é a padrão do cluster

# 1. Criar o Secret do Postgres — senha gerada na hora, NUNCA em arquivo/Git
$pgPassword = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 24 | % {[char]$_})
$pgAdminPassword = -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 24 | % {[char]$_})
oc create secret generic postgresql-credentials `
  --from-literal=POSTGRESQL_USER=stockinteligence `
  --from-literal=POSTGRESQL_PASSWORD=$pgPassword `
  --from-literal=POSTGRESQL_DATABASE=stockinteligence `
  --from-literal=POSTGRESQL_ADMIN_PASSWORD=$pgAdminPassword

# 2. Subir o Postgres
oc apply -f infra/openshift/02-postgresql-pvc.yaml
oc apply -f infra/openshift/03-postgresql-deployment.yaml
oc apply -f infra/openshift/04-postgresql-service.yaml
oc rollout status deployment/postgresql --timeout=180s
oc exec deploy/postgresql -- pg_isready -U stockinteligence -d stockinteligence

# 3. Subir o Kafka
oc apply -f infra/openshift/05-kafka-deployment.yaml
oc apply -f infra/openshift/06-kafka-service.yaml
oc rollout status deployment/kafka --timeout=180s

# 4. Validar o Kafka isoladamente (ver §7 — a imagem "native" não tem
#    kafka-topics.sh embutido, por isso usamos um pod cliente à parte)
oc run kafka-client --rm -i --restart=Never --image=docker.io/apache/kafka:4.2.0 `
  --command -- /opt/kafka/bin/kafka-topics.sh --create --topic teste-validacao --bootstrap-server kafka:9092
oc run kafka-client-list --rm -i --restart=Never --image=docker.io/apache/kafka:4.2.0 `
  --command -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092

# 5. Build do backend (local -> dentro do cluster)
cd backend
./mvnw clean package -DskipTests
cd ..
oc apply -f infra/openshift/07-backend-imagestream.yaml
oc apply -f infra/openshift/08-backend-buildconfig.yaml
oc start-build backend --from-dir=backend --follow

# 6. Subir o backend
oc apply -f infra/openshift/09-backend-deployment.yaml
oc apply -f infra/openshift/10-backend-service.yaml
oc apply -f infra/openshift/11-backend-route.yaml
oc rollout status deployment/backend --timeout=180s

# 7. Pegar a URL pública do backend e testar
$backendHost = oc get route backend -o jsonpath='{.spec.host}'
curl.exe -s "https://$backendHost/q/health/ready"

# 8. Build e deploy do frontend — repare que frontend/.env.production já
#    precisa conter a URL real do backend (passo 7) ANTES deste build, já
#    que o Vite embute esse valor no JavaScript estático (não é lido em
#    runtime como uma env var de um app Java).
cd frontend
npm ci
npm run build
cd ..
oc apply -f infra/openshift/12-frontend-imagestream.yaml
oc apply -f infra/openshift/13-frontend-buildconfig.yaml
oc start-build frontend --from-dir=frontend --follow
oc apply -f infra/openshift/14-frontend-deployment.yaml
oc apply -f infra/openshift/15-frontend-service.yaml
oc apply -f infra/openshift/16-frontend-route.yaml
oc rollout status deployment/frontend --timeout=180s

# 9. Se a URL do frontend (passo 8) ficou diferente do que já estava em
#    application.properties (quarkus.http.cors.origins) e em
#    frontend/.env.production, atualize os dois arquivos e repita o
#    passo 7 (rebuild do backend) e o passo 8 (rebuild do frontend).
$frontendHost = oc get route frontend -o jsonpath='{.spec.host}'
curl.exe -s -o NUL -w "status: %{http_code}`n" "https://$frontendHost/"

# 10. Build e deploy do notification-service (consome o mesmo Kafka já
#     implantado, sem infra nova de mensageria)
cd notification-service
./mvnw clean package -DskipTests
cd ..
oc apply -f infra/openshift/17-notification-imagestream.yaml
oc apply -f infra/openshift/18-notification-buildconfig.yaml
oc start-build notification --from-dir=notification-service --follow
oc apply -f infra/openshift/19-notification-deployment.yaml
oc apply -f infra/openshift/20-notification-service.yaml
oc apply -f infra/openshift/21-notification-route.yaml
oc rollout status deployment/notification --timeout=180s

# 11. Validar isoladamente (repare que /alertas pode já vir com itens —
#     o consumidor usa auto.offset.reset=earliest, então consome qualquer
#     alerta que já estava no tópico antes dele existir)
$notificationHost = oc get route notification -o jsonpath='{.spec.host}'
curl.exe -s "https://$notificationHost/q/health/ready"
curl.exe -s "https://$notificationHost/alertas"

# 12. Se o host do passo 11 ficou diferente do previsto em
#     frontend/.env.production (VITE_ALERTAS_API_BASE_URL), atualize e
#     rebuilde o frontend (mesmos comandos do passo 8).
```

## 5. Como verificar que está funcionando

```powershell
$backendHost = oc get route backend -o jsonpath='{.spec.host}'
$base = "https://$backendHost"

# Saúde do banco e da mensageria
curl.exe -s "$base/q/health/ready"
curl.exe -s "$base/q/health/live"

# Fluxo de negócio completo: cadastro -> saldo -> alerta de estoque baixo
$produto = Invoke-RestMethod -Method Post -Uri "$base/api/produtos" -ContentType "application/json" `
  -Body '{"sku":"TESTE-001","nome":"Produto Teste","categoria":"BEBIDAS","unidadeMedida":"UN","precoCusto":4.50,"precoVenda":7.90}'
$id = $produto.id

Invoke-RestMethod -Method Post -Uri "$base/api/produtos/$id/saldo-estoque" -ContentType "application/json" `
  -Body '{"quantidadeInicial": 10, "quantidadeMinima": 5}'

Invoke-RestMethod -Method Post -Uri "$base/api/produtos/$id/saldo-estoque/saidas" -ContentType "application/json" `
  -Body '{"quantidade": 6}'
# Resposta esperada: "abaixoDoMinimo": true — confirma que o alerta cruzou
# o limiar e foi publicado no Kafka de verdade, dentro do cluster.
```

Pra conferir nos logs que a mensagem realmente foi publicada:

```powershell
oc logs deploy/backend --tail=50 | Select-String "kafka" -CaseSensitive:$false
```

Pra conferir o frontend (SPA + CORS):

```powershell
$frontendHost = oc get route frontend -o jsonpath='{.spec.host}'
$backendHost = oc get route backend -o jsonpath='{.spec.host}'

# index.html na raiz
curl.exe -s -o NUL -w "status: %{http_code}`n" "https://$frontendHost/"

# rota interna do React Router — deve cair no index.html (200), não 404,
# graças ao try_files configurado em frontend/nginx-default-cfg/spa-fallback.conf
curl.exe -s -o NUL -w "status: %{http_code}`n" "https://$frontendHost/produtos/novo"

# CORS de verdade: confirma que o backend responde com o header
# access-control-allow-origin batendo com a origem do frontend (não só um
# 200 genérico — sem esse header, um navegador de verdade bloquearia)
curl.exe -s -D - -o NUL -X OPTIONS "https://$backendHost/api/produtos" `
  -H "Origin: https://$frontendHost" -H "Access-Control-Request-Method: GET" |
  Select-String "access-control-allow-origin"
```

Pra conferir o pipeline de mensageria completo (backend publica →
notification-service consome → frontend exibe), pelo terminal:

```powershell
$notificationHost = oc get route notification -o jsonpath='{.spec.host}'

# Dispara uma saída que cruza o limiar (mesmo fluxo acima) e, alguns
# segundos depois, confirma que o alerta chegou no notification-service:
curl.exe -s "https://$notificationHost/alertas"
```

Ou visualmente: abra `https://<frontend>/produtos`, clique em **"Estoque"**
na linha de um produto, registre uma saída que deixe a quantidade igual ou
abaixo da mínima, depois abra **"Alertas"** (link no topo de "Produtos") —
o alerta aparece na tabela, mais recente primeiro, tipicamente em menos de
1 segundo depois de registrado.



## 6. Como atualizar depois de uma mudança de código

Não existe deploy automático a partir do Git nesta configuração (ver
decisão do Binary Build, §7) — cada atualização precisa repetir o build
manualmente.

**Backend:**
```powershell
cd backend
./mvnw clean package -DskipTests
cd ..
oc start-build backend --from-dir=backend --follow
oc rollout restart deployment/backend
oc rollout status deployment/backend --timeout=180s
```

**Frontend:**
```powershell
cd frontend
npm run build   # lê frontend/.env.production automaticamente
cd ..
oc start-build frontend --from-dir=frontend --follow
oc rollout restart deployment/frontend
oc rollout status deployment/frontend --timeout=180s
```

**notification-service:**
```powershell
cd notification-service
./mvnw clean package -DskipTests
cd ..
oc start-build notification --from-dir=notification-service --follow
oc rollout restart deployment/notification
oc rollout status deployment/notification --timeout=180s
```

Se a mudança envolveu a URL de um serviço contra o outro (ex.: recriou uma
Route e o host mudou), atualize `frontend/.env.production` **e** o CORS do
serviço correspondente (`quarkus.http.cors.origins`, tanto em
`backend/src/main/resources/application.properties` quanto em
`notification-service/src/main/resources/application.properties`), e
refaça os builds afetados — sempre o(s) backend(s) primeiro, frontend por
último (senão o frontend novo aponta pra uma URL que o backend ainda não
libera no CORS).

## 7. Problemas reais encontrados (troubleshooting)

Esses são os imprevistos genuínos que apareceram executando este guia —
não hipóteses, coisas que realmente aconteceram:

- **`mvn clean` falhou com "Failed to delete backend-dev.jar"** — um
  processo `quarkus:dev` esquecido rodando (de uma sessão de teste
  anterior) estava com o arquivo aberto/travado. Solução: encontrar e
  encerrar o processo Java antes de rebuildar:
  ```powershell
  Get-Process java | Select-Object Id, StartTime
  # identifique o processo certo (verifique a linha de comando antes de
  # encerrar) e rode: Stop-Process -Id <PID> -Force
  ```
- **Binary build falhando por não achar o `Dockerfile.jvm`** — o
  `backend/.dockerignore` exclui **tudo** por padrão (`*`) e só
  reinclui os artefatos de build (`target/quarkus-app/*` etc.). Isso
  também excluía o próprio `Dockerfile.jvm` do pacote enviado pelo
  `oc start-build --from-dir`. Corrigido adicionando
  `!src/main/docker/Dockerfile.jvm` ao final do `.dockerignore` (não afeta
  `docker build` local, que sempre lê o Dockerfile direto do disco, fora
  do contexto).
- **A imagem `apache/kafka-native` não tem `kafka-topics.sh`/`bin/*.sh`
  embutidos** — é uma imagem *native-image* (GraalVM) minimalista, só o
  binário compilado do broker (`/opt/kafka/kafka.Kafka`), sem os scripts
  Java tradicionais. Para testar produce/consume, usamos um **pod cliente
  temporário e descartável** com a imagem completa (`apache/kafka:4.2.0`,
  com JVM e todos os scripts), apontando pro `Service` do Kafka real —
  sem alterar o que roda em produção.
- **`npm ci` falhou com `EPERM: operation not permitted, unlink ...`
  num arquivo dentro de `node_modules`** — havia um `npm run dev` rodando
  em outro terminal (mantendo o processo do Vite vivo, com arquivos
  binários de dependências nativas abertos). O `npm ci` apaga
  `node_modules` antes de reinstalar, e não consegue apagar arquivo em
  uso. Solução: encerrar qualquer `npm run dev` antes de rodar `npm ci`
  (`Get-Process node` pra identificar o processo, conferir a linha de
  comando antes de encerrar, `Stop-Process -Id <PID> -Force`).
- **Frontend implantado mas em `CrashLoopBackOff`, com o log mostrando só
  uma mensagem de "uso" do S2I** (algo como "This is a S2I rhel base
  image. To use it in OpenShift, run: `oc new-app nginx:1.24~...`") —
  a imagem `ubi9/nginx-124` é pensada pra ser usada via o fluxo
  `s2i build` do OpenShift, que sobrescreve o comando padrão do container
  pelo script real de start (`/usr/libexec/s2i/run`). Como fizemos um
  `Dockerfile` comum (com `COPY`, sem passar pelo `s2i build`), a imagem
  ficou com o comportamento padrão de "imagem builder standalone", que só
  imprime instruções de uso e sai — nunca inicia o nginx de verdade.
  Corrigido adicionando `CMD ["/usr/libexec/s2i/run"]` explicitamente no
  final do `frontend/Dockerfile`.
- **`ObjectMapperDeserializer<T>` (lado consumidor Kafka) não tem
  construtor sem argumentos** — ao contrário do `ObjectMapperSerializer`
  usado no lado produtor (backend), que tem. Corrigido criando uma
  subclasse (`EstoqueBaixoAtingidoDeserializer`) chamando
  `super(EstoqueBaixoAtingidoRecebido.class)` — confirmado por
  decompilação do bytecode de `quarkus-kafka-client`, não por suposição.
  Detalhe menos óbvio: isso exige `quarkus-rest-jackson` no classpath do
  `notification-service` mesmo sem nenhum endpoint de escrita — é essa
  extensão que registra o `ObjectMapper` gerenciado por CDI com
  `JavaTimeModule`; sem ela, a desserialização de `OffsetDateTime` falha.
- **Teste do consumidor Kafka falhava com "Expected size: 1 but was: 0"
  mesmo com o conector `smallrye-in-memory` corretamente configurado** —
  o processamento de um `@Incoming` passa pelo pipeline reativo (Mutiny) e
  não é síncrono em relação ao `.send()` do teste: o assert rodava antes
  do consumidor processar a mensagem. Corrigido com `Awaitility`
  (`await().atMost(...).until(...)`) esperando o `AlertaStore` deixar de
  estar vazio, em vez de assertar logo após o `send()` — mesma abordagem
  recomendada pela documentação oficial do Quarkus para este cenário.

## 8. Limitações conhecidas

Não são bugs — são características do ambiente gratuito, documentadas de
propósito pra não virarem surpresa:

- **Pods são reciclados automaticamente após ~12h consecutivas rodando**
  (política do Developer Sandbox) — o `Deployment` sobe um pod novo
  sozinho, é esperado. O Postgres tem `PersistentVolumeClaim`, então os
  dados sobrevivem; o Kafka **não** tem PVC, então o tópico
  `estoque.baixo-atingido` é perdido e recriado automaticamente no
  primeiro publish seguinte (mesmo comportamento já validado localmente).
- **A conta do Developer Sandbox expira em ~30 dias** e precisa ser
  reativada (gratuito, um clique) — se a URL parar de responder depois de
  um tempo, esse é o motivo mais provável.
- **Sem CI/CD automático**: cada mudança de código exige rodar o build
  manualmente (§6) — não há gatilho automático a partir de `git push`.
  Automatizar isso via GitHub Actions é uma evolução natural, não
  implementada aqui.
- **Alertas do `notification-service` ficam em memória, não em banco** —
  decisão deliberada (ver `notification-service/.../alerta/AlertaStore.java`):
  somem quando o pod reciclar (mesma limitação já aceita pro Kafka acima).
  O dado real — saldo de estoque — continua seguro no Postgres do backend;
  alertas são notificação, não registro de negócio.
- **`notification-service` não pode ter mais de 1 réplica** enquanto os
  alertas forem armazenados em memória (`AlertaStore` é local ao
  processo) — `GET /alertas` responderia de forma inconsistente
  dependendo de qual pod atendeu a requisição. Documentado explicitamente
  em `infra/openshift/19-notification-deployment.yaml`.
