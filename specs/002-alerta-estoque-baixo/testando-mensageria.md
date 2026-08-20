# Como ver e testar a mensageria (Kafka) — feature 002

**Complementa:** `spec.md`, `plan.md` (seção "Configuração de mensageria")

Este guia é prático: mostra como verificar, em três camadas cada vez mais
"reais", que o alerta de estoque baixo (`EstoqueBaixoAtingido`) está sendo
publicado corretamente no tópico Kafka `estoque.baixo-atingido`.

| Camada | O que prova | Precisa de Docker rodando? |
|---|---|---|
| 1. Testes automatizados | A regra de negócio (evento só publica após commit) | Só via Dev Services, automático |
| 2. End-to-end via HTTP | O fluxo completo funciona de ponta a ponta | Sim (Dev Services) |
| 3. Mensagem crua no tópico | O JSON publicado é válido e tem o formato certo | Sim (Dev Services) |

A camada 1 não prova que o JSON publicado no Kafka é válido — o conector de
teste (`smallrye-in-memory`) intercepta o objeto Java diretamente, sem
serializar. Por isso a camada 3 existe: é a única que olha os bytes reais
que iriam para o Kafka em produção.

---

## Camada 1 — Testes automatizados

```bash
cd backend
./mvnw test -Dtest=EstoqueBaixoAtingidoKafkaPublisherTest
./mvnw test -Dtest=RegistrarSaidaEstoqueCommandHandlerTest
```

- **`EstoqueBaixoAtingidoKafkaPublisherTest`** — prova o requisito central da
  US-5 (FR-009): dispara o evento dentro de uma transação que **comita** →
  1 mensagem no sink; dispara dentro de uma transação que sofre
  **rollback** → 0 mensagens. Usa `InMemoryConnector` (conector
  `smallrye-in-memory`, ativado só em `%test` — ver `application.properties`).
- **`RegistrarSaidaEstoqueCommandHandlerTest`** — prova, com
  `Event<EstoqueBaixoAtingido>` mockado, que o `.fire()` só é chamado quando
  o agregado de fato sinaliza o evento (saldo cruza o limiar).

Rápidos, sem depender de um broker Kafka real.

---

## Camada 2 — Teste manual end-to-end via HTTP

Sobe a aplicação; o Dev Services do Quarkus provisiona Postgres **e**
Kafka/Redpanda automaticamente (nenhuma configuração manual):

```bash
cd backend
./mvnw quarkus:dev
```

Espere a linha `Listening on: http://localhost:8080` no log. Em outro
terminal (Bash/Git Bash):

```bash
# 1. Cadastrar um produto
PRODUTO=$(curl -s -X POST http://localhost:8080/api/produtos \
  -H "Content-Type: application/json" \
  -d '{"sku":"TESTE-001","nome":"Produto Teste","categoria":"BEBIDAS","unidadeMedida":"UN","precoCusto":4.50,"precoVenda":7.90}')
ID=$(echo "$PRODUTO" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "produto id: $ID"

# 2. Definir saldo inicial (10, mínimo 5 — ainda acima do mínimo)
curl -s -X POST "http://localhost:8080/api/produtos/$ID/saldo-estoque" \
  -H "Content-Type: application/json" \
  -d '{"quantidadeInicial": 10, "quantidadeMinima": 5}'

# 3. Saída que CRUZA o limiar (10 -> 4): deve disparar o alerta
curl -s -X POST "http://localhost:8080/api/produtos/$ID/saldo-estoque/saidas" \
  -H "Content-Type: application/json" -d '{"quantidade": 6}'
```

Equivalente em **PowerShell**:

```powershell
$produto = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/produtos" `
  -ContentType "application/json" `
  -Body '{"sku":"TESTE-001","nome":"Produto Teste","categoria":"BEBIDAS","unidadeMedida":"UN","precoCusto":4.50,"precoVenda":7.90}'
$id = $produto.id

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/produtos/$id/saldo-estoque" `
  -ContentType "application/json" -Body '{"quantidadeInicial": 10, "quantidadeMinima": 5}'

Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/produtos/$id/saldo-estoque/saidas" `
  -ContentType "application/json" -Body '{"quantidade": 6}'
```

**O que conferir:**
- A resposta do passo 3 deve trazer `"abaixoDoMinimo": true` — confirma que
  a saída cruzou o limiar.
- No log do `quarkus:dev`, deve aparecer o producer Kafka conectado
  (`SRMSG18258: Kafka producer ...`) e nenhuma exceção depois da saída — a
  publicação acontece silenciosamente em background, via
  `EstoqueBaixoAtingidoKafkaPublisher` (`@Observes(during = AFTER_SUCCESS)`).
- Repetir o passo 3 com uma quantidade pequena (ex. `{"quantidade": 1}`)
  **não** deve gerar um novo alerta — o saldo já está abaixo do mínimo (regra
  "só na transição", FR-008). Só volta a disparar depois de uma entrada que
  leve o saldo de volta para acima do mínimo.

---

## Camada 3 — Inspecionar a mensagem crua no tópico

Isso prova que o `ObjectMapperSerializer` está de fato gerando JSON válido
na saída — a única coisa que a Camada 1 não cobre.

### Passo 0 — descobrir o endereço do broker

Com o `quarkus:dev` rodando, procure no log a linha do producer:

```
... Kafka producer kafka-producer-estoque-baixo-atingido, connected to Kafka brokers 'localhost:NNNNN' ...
```

A porta (`NNNNN`) muda a cada subida — é o Dev Services expondo um
container Kafka/Redpanda efêmero via Testcontainers.

### Opção A (recomendada) — `kcat`

`kcat` é um cliente Kafka de linha de comando leve, sem precisar de
container extra. Instalar uma vez:

```powershell
# Chocolatey
choco install kcat

# ou Scoop
scoop install kcat
```

Consumir o tópico desde o início:

```bash
kcat -b localhost:NNNNN -t estoque.baixo-atingido -C -e -o beginning
```

(`-e` sai assim que não há mais mensagens; `-o beginning` lê desde o
começo do tópico.)

### Opção B — sem instalar nada (mini consumidor Java)

Se não puder instalar `kcat`, dá pra usar diretamente o `kafka-clients` que
já está no `~/.m2` local (mesma dependência transitiva do `quarkus-messaging-kafka`).
Salve como `PeekTopic.java`:

```java
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class PeekTopic {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, args[0]);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "peek-topic-" + System.currentTimeMillis());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(args[1]));
            long deadline = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(1000))) {
                    System.out.println("value (JSON bruto): " + r.value());
                }
            }
        }
    }
}
```

Compilar e rodar (ajuste as versões conforme o que estiver em `~/.m2`):

```bash
KAFKA_JAR=~/.m2/repository/org/apache/kafka/kafka-clients/4.2.1/kafka-clients-4.2.1.jar
SLF4J_JAR=~/.m2/repository/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar

javac -cp "$KAFKA_JAR;$SLF4J_JAR" PeekTopic.java
java -cp ".;$KAFKA_JAR;$SLF4J_JAR" PeekTopic localhost:NNNNN estoque.baixo-atingido
```

### Saída esperada (as duas opções)

```json
{"produtoId":"...","sku":"TESTE-001","quantidadeAtual":4.000,"quantidadeMinima":5.000,"ocorridoEm":"2026-08-20T14:34:27.527...-03:00"}
```

Confere: os 5 campos de `EstoqueBaixoAtingidoMensagem`
(`infrastructure/adapter/out/messaging/EstoqueBaixoAtingidoMensagem.java`),
`quantidadeAtual`/`quantidadeMinima` batendo com o que foi definido, e
`ocorridoEm` em ISO-8601 (Jackson serializa `OffsetDateTime` corretamente
porque `quarkus-rest-jackson` já registra o `JavaTimeModule` globalmente).

---

## Pegadinhas conhecidas (Windows / Git Bash)

- **`docker exec` no container `apache/kafka-native` não tem
  `kafka-console-consumer.sh`.** É uma imagem *native-image* (GraalVM),
  sem JVM nem os scripts Java tradicionais do Kafka — não dá pra usar as
  ferramentas CLI clássicas de dentro do container. Use a Opção A ou B
  acima, rodando fora do container.
- **Caminhos Unix em `docker exec`/`docker run` no Git Bash** às vezes são
  reescritos para caminhos Windows (`/opt/...` vira `C:/Program Files/...`).
  Se isso acontecer, prefixe o comando com `MSYS_NO_PATHCONV=1`.
- **Container avulso de outra imagem (ex.: `kcat` via `docker run`) não
  enxerga `localhost:NNNNN` do host por padrão** — a porta exposta pelo
  Testcontainers é só para acesso do host, não de outro container. Se for
  usar `kcat`/`kafkacat` via Docker em vez de instalado localmente, rode
  compartilhando a rede do container do broker
  (`docker run --network container:<id-do-kafka> ...`) e aponte para o IP
  interno do container (`docker inspect <id> --format '{{json .NetworkSettings.Networks}}'`),
  não para `localhost:NNNNN`.

---

## Referências

- `plan.md` › "Configuração de mensageria" — canal `estoque-baixo-atingido`,
  tópico `estoque.baixo-atingido`, overrides `%prod`/`%test`.
- `memory/tech-stack.md` › "Mensageria — publicação assíncrona" — por que a
  extensão foi introduzida e a limitação conhecida (não é Transactional
  Outbox completo).
- `EstoqueBaixoAtingidoKafkaPublisher.java` — o observador
  `@Observes(during = TransactionPhase.AFTER_SUCCESS)` que faz a publicação.
