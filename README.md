# aps-weather

API REST em Spring Boot que disponibiliza os dados climáticos do projeto `APIClima-usoBasico`
(antes uma aplicação de console sem Spring). As previsões são coletadas do Open-Meteo (ou de uma
fonte simulada) e persistidas em SQLite.

## Requisitos

- Java 25
- Maven 3.9+ (ou use `./mvnw`)

## Como executar

```bash
mvn spring-boot:run
```

A aplicação sobe em `http://localhost:8080`, cria/atualiza o banco `estacao.db` e cadastra as
cinco cidades iniciais: São Paulo, Rio de Janeiro, Brasília, Salvador e Porto Alegre.

Para rodar sem rede, usando a fonte simulada:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.weather.source=simulada --app.weather.simulation.scenario=TEMPESTADE"
```

## Configuração (`application.properties`)

| Propriedade | Padrão | Descrição |
| --- | --- | --- |
| `app.weather.source` | `openmeteo` | `openmeteo` ou `simulada` |
| `app.weather.simulation.scenario` | `DIA_CALMO` | `DIA_CALMO`, `ONDA_DE_CALOR`, `FRENTE_FRIA`, `TEMPESTADE` |
| `app.weather.collection.enabled` | `true` | Liga/desliga a coleta agendada |
| `app.weather.collection.interval-ms` | `900000` | Intervalo entre coletas (15 min, mesma janela do `current` do Open-Meteo) |
| `app.weather.collection.initial-delay-ms` | `5000` | Atraso da primeira coleta |
| `app.weather.collection.pular-repetidos` | `true` | Ignora previsões já gravadas para o mesmo horário |
| `app.weather.openmeteo.base-url` | `https://api.open-meteo.com/v1/forecast` | URL base da API Open-Meteo |
| `app.weather.openmeteo.timeout-ms` | `10000` | Timeout de cada requisição ao Open-Meteo |
| `app.weather.openmeteo.retry-backoff-ms` | `1000` | Espera base entre tentativas (dobra a cada retry) |
| `app.weather.timezone.default` | `America/Sao_Paulo` | Fuso usado quando a localização não tem timezone |
| `app.weather.forecast.ttl-ms` | `900000` | Validade do snapshot de previsões horárias/diárias |
| `spring.datasource.url` | `jdbc:sqlite:estacao.db` | Arquivo do banco |

## Endpoints

| Método | Rota | Descrição |
| --- | --- | --- |
| GET | `/api/cities` | Lista as cidades cadastradas |
| GET | `/api/cities/{id}/weather` | Última previsão gravada da cidade pelo id |
| GET | `/api/cities/{id}/history` | Histórico paginado pelo id (`?page=0&size=50`, teto 200) |
| GET | `/api/cities/{id}/hourly` | Temperaturas hora a hora pelo id |
| GET | `/api/cities/{id}/daily` | Mínimas e máximas por id |
| GET | `/api/weather/{city}` | Última previsão gravada da cidade |
| GET | `/api/weather/{city}/history` | Histórico paginado da cidade (`?page=0&size=50`, teto 200) |
| GET | `/api/weather/{city}/hourly` | Temperaturas previstas hora a hora para o dia atual |
| GET | `/api/weather/{city}/daily` | Mínimas e máximas para os próximos 7 dias, incluindo hoje |
| POST | `/api/collect` | Coleta todas as cidades e relata gravações e falhas |
| POST | `/api/collect/{city}` | Coleta e grava previsões de uma cidade |
| POST | `/api/cities/{id}/collect` | Coleta e grava previsões de uma cidade pelo id |

O nome da cidade é resolvido sem diferenciar maiúsculas nem acentos: `são paulo`, `SÃO PAULO` e
`Brasilia` encontram as cidades cadastradas, e cidades com a mesma grafia normalizada são
tratadas como uma só.

O `id` de cada cidade vem de `GET /api/cities`. As rotas `/api/cities/{id}/...` têm a mesma
resposta das rotas por nome e evitam codificar acentos e espaços na URL — indicadas para o
front-end.

### Exemplos

```bash
curl http://localhost:8080/api/cities

curl "http://localhost:8080/api/weather/S%C3%A3o%20Paulo"

curl "http://localhost:8080/api/weather/S%C3%A3o%20Paulo/history?page=0&size=20"

curl "http://localhost:8080/api/weather/S%C3%A3o%20Paulo/hourly"

curl "http://localhost:8080/api/weather/S%C3%A3o%20Paulo/daily"

curl "http://localhost:8080/api/cities/1/hourly"

curl -X POST http://localhost:8080/api/collect

curl -X POST "http://localhost:8080/api/collect/Rio%20de%20Janeiro"
```

Resposta de uma previsão:

```json
{
  "id": 1,
  "city": "São Paulo",
  "forecastAt": "2026-10-30T18:00:00Z",
  "temperature": 19.0,
  "apparentTemperature": 16.1,
  "humidity": 94,
  "precipitation": 11.7,
  "weatherCode": 95,
  "condition": "TEMPESTADE",
  "conditionDescription": "Tempestade",
  "windSpeed": 52.3,
  "windDirection": 190.7,
  "pressure": 994.5,
  "cloudCover": 98.9,
  "updatedAt": "2026-10-30T18:00:01.234Z"
}
```

Resposta do `POST /api/collect`:

```json
{
  "gravadas": [ { "id": 1, "city": "São Paulo", "...": "..." } ],
  "falhas": [ { "cidade": "Rio de Janeiro", "motivo": "API respondeu com status 500: ..." } ]
}
```

O status reflete o resultado: `200` quando tudo foi gravado, `207 Multi-Status` quando houve
gravação e falha (a coleta segue cidade a cidade) e `502` quando todas as tentativas falharam.
Uma cidade que falha não impede as demais.

Resposta do histórico paginado:

```json
{
  "content": [ { "id": 10, "city": "São Paulo", "...": "..." } ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

Os itens vêm ordenados por `forecastAt` decrescente (empate desfeito pelo `id` decrescente).

Resposta de `/hourly` (24 temperaturas do dia atual no fuso da cidade):

```json
{
  "date": "2026-11-02",
  "timezone": "America/Sao_Paulo",
  "temperatures": [
    { "time": "00:00", "temperature": 19.5 },
    { "time": "01:00", "temperature": 19.2 }
  ]
}
```

Resposta de `/daily` (7 dias a partir de hoje, no fuso da cidade):

```json
{
  "timezone": "America/Sao_Paulo",
  "forecast": [
    { "date": "2026-11-02", "minTemperature": 18.0, "maxTemperature": 27.0 },
    { "date": "2026-11-03", "minTemperature": 19.0, "maxTemperature": 30.0 }
  ]
}
```

As previsões horárias e diárias usam leitura *read-through* sobre o SQLite: a primeira
requisição de cada cidade consulta a Open-Meteo, grava o snapshot e responde; as seguintes
reaproveitam o snapshot por até `app.weather.forecast.ttl-ms` (15 minutos). Não há coleta
agendada — a renovação é sob demanda. Se a fonte falhar e houver snapshot anterior, ele é
servido mesmo vencido; sem snapshot, o erro da fonte é propagado.

## Documentação da API

As rotas são documentadas com Spring REST Docs a partir dos próprios testes HTTP
(`ApiDocumentationTests`). O build gera a página em `target/generated-docs/index.html`:

```bash
./mvnw verify
open target/generated-docs/index.html
```

O fonte fica em `src/docs/asciidoc/index.adoc` e os snippets em
`target/generated-snippets/`. Como os exemplos vêm dos testes, a documentação acompanha
o contrato real da API.

## Observabilidade

O Actuator expõe apenas endpoints de leitura (`management.endpoints.web.exposure.include`):

| Rota | Descrição |
| --- | --- |
| `GET /actuator/health` | Estado da aplicação e do banco SQLite (detalhes ocultos) |
| `GET /actuator/metrics` | Lista as métricas disponíveis |
| `GET /actuator/info` | Informações da aplicação |

Métricas da coleta (Micrometer):

| Métrica | Tipo | Tags | Descrição |
| --- | --- | --- | --- |
| `aps.weather.collect` | timer | `tipo=lote` ou `tipo=cidade` | Duração da coleta |
| `aps.weather.collect.city` | contador | `cidade`, `resultado=sucesso` ou `falha` | Resultado por cidade |

```bash
curl http://localhost:8080/actuator/health

curl "http://localhost:8080/actuator/metrics/aps.weather.collect.city?tag=resultado:sucesso"
```

## Docker

```bash
docker build -t aps-weather .

docker run --rm -p 8080:8080 -v aps-data:/data aps-weather

curl http://localhost:8080/api/cities
```

A imagem usa build multi-estágio (Maven + Temurin 25 e JRE 25) e roda como usuário sem
privilégios. O banco fica em `/data/estacao.db`, montado como volume — sem ele, os dados somem
quando o container é removido. `.scratch/`, `docs/` e `target/` não entram na imagem.

## Integração contínua

`.github/workflows/ci.yml` roda `mvn -B verify` em Java 25 (Temurin) a cada push e pull request,
com cache das dependências Maven.

## Erros

Os erros seguem o formato `application/problem+json` (RFC 7807):

| Status | Situação |
| --- | --- |
| 400 | Parâmetro inválido (cidade vazia/longa, `page`/`size` inválidos) |
| 404 | Cidade desconhecida ou sem coleta registrada |
| 422 | Dados inválidos recebidos da fonte |
| 502 | Falha ao consultar a fonte de clima |
| 503 | Erro de banco ou coleta interrompida |

## Testes

```bash
mvn test
```

Os testes usam a fonte simulada, um banco SQLite temporário e não disparam a coleta agendada.
