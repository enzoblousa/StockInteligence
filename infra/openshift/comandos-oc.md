# Guia de comandos `oc` — do zero ao dia a dia

Guia de referência do CLI do OpenShift (`oc`) para quem está começando.
Todo exemplo aqui usa os recursos reais deste projeto (`backend`,
`frontend`, `notification`, `kafka`, `postgresql`, namespace
`enzoblousa-dev`) — não são exemplos genéricos, são comandos que você
pode copiar e colar de verdade.

> Nota sobre o nome: o CLI se chama **`oc`** ("OpenShift Client"), não
> "OCI" — OCI é outra coisa (uma especificação de formato de container,
> sem relação direta com este comando). É fácil confundir os dois nomes.

## Índice

1. [O que é o `oc`, em uma frase](#1-o-que-é-o-oc-em-uma-frase)
2. [Autenticação e contexto](#2-autenticação-e-contexto)
3. [Enxergar o que existe](#3-enxergar-o-que-existe)
4. [Logs](#4-logs)
5. [Entrar dentro de um container](#5-entrar-dentro-de-um-container)
6. [Túnel pro seu computador (port-forward)](#6-túnel-pro-seu-computador-port-forward)
7. [Aplicar e atualizar manifests](#7-aplicar-e-atualizar-manifests)
8. [Builds (construir imagens dentro do cluster)](#8-builds-construir-imagens-dentro-do-cluster)
9. [Rollout — controlar versões de um Deployment](#9-rollout--controlar-versões-de-um-deployment)
10. [Criar recursos direto pela linha de comando](#10-criar-recursos-direto-pela-linha-de-comando)
11. [Debugging — "por que não está funcionando?"](#11-debugging--por-que-não-está-funcionando)
12. [Comandos que mudam ou apagam coisas — use com cuidado](#12-comandos-que-mudam-ou-apagam-coisas--use-com-cuidado)
13. [Referência rápida (tabela resumo)](#13-referência-rápida-tabela-resumo)

---

## 1. O que é o `oc`, em uma frase

É o programa de linha de comando que conversa com o cluster OpenShift —
o mesmo tipo de papel que o `docker`/`docker compose` tem localmente,
só que falando com um cluster remoto em vez da sua máquina. Quase todo
comando segue o padrão:

```
oc <verbo> <tipo-de-recurso> <nome> [opções]
```

Exemplo: `oc get pods` → verbo `get`, tipo `pods`, sem nome específico
(lista todos).

## 2. Autenticação e contexto

```powershell
oc login --token=... --server=https://api.rm2.thpm.p1.openshiftapps.com:6443
# Comando exato copiado do console web (menu do usuário → "Copy login command")

oc whoami                  # confirma quem você é
oc whoami --show-server    # confirma em qual cluster você está logado

oc projects                # lista os projetos (namespaces) que você tem acesso
oc project enzoblousa-dev  # muda o "diretório atual" pra este namespace
```

Depois de `oc project`, todo comando seguinte já assume esse namespace —
não precisa repetir `-n enzoblousa-dev` em cada linha (mas pode, se
quiser ser explícito ou estiver alternando entre namespaces).

## 3. Enxergar o que existe

```powershell
oc get pods              # lista os pods e o status de cada um
oc get pods -o wide       # igual acima, com mais colunas (IP interno, nó, etc.)
oc get all                # pods + Deployments + Services + Routes de uma vez
oc get routes              # só as URLs públicas
oc get deployments         # só os Deployments
oc get services             # só os Services (endereços internos)
oc get pvc                  # volumes persistentes (só o postgresql tem, ver §8 do README.md)
oc get secrets               # lista os Secrets (não mostra o conteúdo — ver §10)

oc get pods -w              # "watch": fica atualizando ao vivo. Ctrl+C pra sair.
```

`oc get routes -o custom-columns=NOME:.metadata.name,URL:.spec.host` —
formato customizado, útil pra ver só nome + URL de cada Route de uma vez.

## 4. Logs

```powershell
oc logs deploy/backend                # logs recentes (as últimas ~várias centenas de linhas)
oc logs deploy/backend --tail=50       # só as últimas 50 linhas
oc logs deploy/backend -f              # "follow" — acompanha ao vivo, tipo tail -f (Ctrl+C pra sair)
oc logs deploy/backend --previous      # logs do pod ANTERIOR — essencial se ele crashou e reiniciou
```

`deploy/backend` é um atalho pra "o pod atual gerenciado pelo Deployment
`backend`" — evita você ter que descobrir o nome exato do pod
(`backend-69997ffc45-dpkkv`, por exemplo) toda vez. Troque `backend` por
`kafka`, `postgresql`, `notification` ou `frontend` conforme o serviço.

## 5. Entrar dentro de um container

```powershell
# Rodar UM comando dentro do container e voltar (sem sessão interativa):
oc exec deploy/postgresql -- psql -U stockinteligence -d stockinteligence -c "SELECT * FROM produto;"

# Sessão interativa de verdade (fica "dentro" até você sair):
oc exec -it deploy/postgresql -- psql -U stockinteligence -d stockinteligence
oc exec -it deploy/backend -- /bin/bash
```

`oc rsh deploy/backend` faz basicamente a mesma coisa que
`oc exec -it deploy/backend -- /bin/sh` — é um atalho específico do
OpenShift (não existe no `kubectl` puro), mais curto de digitar.

**Rodar um pod novo e descartável**, sem usar nenhum Deployment existente
— útil pra ferramentas que você não tem instaladas em nenhum container já
implantado (como usamos pra testar o Kafka, já que a imagem `native` não
tem `kafka-topics.sh`):

```powershell
oc run kafka-client --rm -i --restart=Never --image=docker.io/apache/kafka:4.2.0 `
  --command -- /opt/kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092
```

`--rm` apaga o pod assim que ele termina; `--restart=Never` impede o
OpenShift de tentar "reviver" um pod que devia ser só temporário.

## 6. Túnel pro seu computador (port-forward)

```powershell
oc port-forward deploy/postgresql 5432:5432
```

Cria um túnel: `localhost:5432` na sua máquina passa a apontar pro
Postgres real do cluster, sem precisar expor uma `Route` pública pra
isso. Fica rodando naquele terminal até você apertar Ctrl+C — abra um
terminal separado pra usar o túnel enquanto ele está ativo (ex.: conectar
o DBeaver em `localhost:5432`).

## 7. Aplicar e atualizar manifests

```powershell
oc apply -f infra/openshift/09-backend-deployment.yaml   # cria ou atualiza a partir de um YAML
oc apply -f infra/openshift/                               # aplica TODOS os YAMLs de uma pasta de uma vez
```

`apply` é seguro de rodar de novo (idempotente) — se o recurso já existe
e está igual, não faz nada; se mudou, atualiza só a diferença.

## 8. Builds (construir imagens dentro do cluster)

```powershell
oc start-build backend --from-dir=backend --follow
# --from-dir envia o diretório local (Binary Build, ver README.md §7)
# --follow mostra o log do build ao vivo, em vez de só disparar e voltar

oc get builds                    # lista builds já rodados (backend-1, backend-2, notification-1...)
oc logs build/backend-2           # log de um build específico — útil pra investigar um build antigo que falhou
oc cancel-build backend-3         # cancela um build em andamento
```

## 9. Rollout — controlar versões de um Deployment

```powershell
oc rollout status deployment/backend       # espera o deploy atual terminar (usado depois de todo apply/build)
oc rollout restart deployment/backend       # recria os pods com a MESMA imagem (não builda de novo)
oc rollout history deployment/backend       # lista as versões (revisions) desse Deployment
oc rollout undo deployment/backend          # volta pra versão anterior — seu "Ctrl+Z" de deploy
```

`rollout restart` é o que já usamos depois de cada `oc start-build` —
sem ele, o Deployment continuaria rodando a imagem antiga mesmo depois de
uma imagem nova ter sido publicada no `ImageStream`.

## 10. Criar recursos direto pela linha de comando

Além de `oc apply -f arquivo.yaml`, alguns recursos você cria direto por
comando (mais rápido pra coisas simples/sensíveis, como Secrets — ver
README.md §4, passo 1, sobre por que a senha nunca vai num arquivo):

```powershell
oc create secret generic postgresql-credentials `
  --from-literal=POSTGRESQL_USER=stockinteligence `
  --from-literal=POSTGRESQL_PASSWORD=$pgPassword

# Ver as CHAVES de um Secret (sem mostrar o valor):
oc describe secret postgresql-credentials

# Decodificar um valor específico (o Secret guarda em base64, não é criptografia):
oc get secret postgresql-credentials -o jsonpath='{.data.POSTGRESQL_PASSWORD}' | base64 -d
```

## 11. Debugging — "por que não está funcionando?"

Ordem recomendada quando algo dá errado:

```powershell
oc get pods                                # 1. o pod existe? qual o status? (Running, CrashLoopBackOff, Pending...)
oc describe pod <nome-do-pod>               # 2. eventos detalhados — erro de imagem, falta de recurso, probe falhando etc.
oc logs deploy/<nome> --previous             # 3. se reiniciou, o log do que aconteceu ANTES de morrer
oc get events --sort-by='.lastTimestamp'     # 4. linha do tempo de tudo que aconteceu no namespace, mais recente por último
oc top pods                                   # 5. uso de CPU/memória agora (útil se suspeitar de falta de recurso)
```

`oc describe` é provavelmente o comando de debug mais usado no dia a dia
— ele mostra a seção **Events** no final, que costuma dizer exatamente o
que está errado (`ImagePullBackOff`, `readiness probe failed`, etc.).

## 12. Comandos que mudam ou apagam coisas — use com cuidado

Esses **não são readonly** — pense antes de rodar, principalmente em
produção:

```powershell
oc scale deployment/backend --replicas=0    # derruba todos os pods (mantém o Deployment, só zera réplicas)
oc scale deployment/backend --replicas=1    # volta ao normal

oc delete pod <nome>                          # apaga um pod específico (o Deployment sobe outro no lugar sozinho)
oc delete deployment/backend                  # apaga o Deployment inteiro (os pods somem e não voltam)
oc delete -f infra/openshift/09-backend-deployment.yaml   # mesma ideia, a partir do arquivo

oc edit deployment/backend                    # abre o YAML do recurso pra editar ao vivo, no editor padrão do terminal
```

**Cuidado especial com `notification`** (ver README.md §8): nunca rode
`oc scale deployment/notification --replicas=2` ou mais — o
`AlertaStore` é em memória, local a cada pod; mais de uma réplica faz
`GET /alertas` responder de forma inconsistente dependendo de qual pod
atender a requisição.

## 13. Referência rápida (tabela resumo)

| Quero... | Comando |
|---|---|
| Ver tudo que está rodando | `oc get all` |
| Ver só as URLs públicas | `oc get routes` |
| Ver logs recentes de um serviço | `oc logs deploy/<nome>` |
| Acompanhar logs ao vivo | `oc logs deploy/<nome> -f` |
| Rodar uma query no banco | `oc exec deploy/postgresql -- psql -U stockinteligence -d stockinteligence -c "..."` |
| Abrir um túnel pro banco | `oc port-forward deploy/postgresql 5432:5432` |
| Entrar num container | `oc exec -it deploy/<nome> -- /bin/bash` |
| Rebuildar depois de mudar código | `oc start-build <nome> --from-dir=<pasta> --follow` |
| Forçar os pods a pegar a imagem nova | `oc rollout restart deployment/<nome>` |
| Ver por que um pod não sobe | `oc describe pod <nome-do-pod>` |
| Ver o histórico de eventos do namespace | `oc get events --sort-by='.lastTimestamp'` |
| Desfazer o último deploy | `oc rollout undo deployment/<nome>` |
