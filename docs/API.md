# API документация (задача №16)

Приложението има REST API само за четене. То е описано по стандарта **OpenAPI 3** и се генерира автоматично от кода със **springdoc-openapi**.

| Адрес | Какво е |
|---|---|
| `/swagger-ui.html` | Swagger UI – интерактивна документация |
| `/v3/api-docs` | OpenAPI спецификация в JSON |

В документацията влизат само REST адресите (`/api/**`, `/owner/api/**`, `/agent/api/**`), а не HTML страниците. Филтърът е зададен в `application.yml` (`springdoc.paths-to-match`).

## Как се използва

1. Влезте в приложението през `/login` като собственик или складов агент.
2. В същия браузър отворете `/swagger-ui.html`.
3. Изберете endpoint, натиснете **Try it out**, а после **Execute**.

Заявките се изпращат с бисквитката на сесията (`JSESSIONID`), затова се изпълняват с правата на влезлия потребител.

| Ситуация | Отговор |
|---|---|
| без вход | **401** |
| с друга роля | **403** |
| невалидни данни (напр. обърнат период) | **400** с `{"error": "..."}` |

## Endpoint-и

### Собственик (роля OWNER)

| Метод и адрес | Описание |
|---|---|
| `GET /owner/api/warehouses` | моите складове с назначените агенти |
| `GET /owner/api/warehouses/{id}` | един мой склад (403, ако е чужд) |
| `GET /owner/api/rentals` | заявките и договорите за моите складове |
| `GET /owner/api/reports/warehouses?from=&to=` | справка за моите складове за период (№14) |
| `GET /owner/api/ai/price-recommendation?area=&height=&category=&climate=&warehouseId=` | AI препоръка за цена (№15) |
| `GET /owner/api/ai/price-model/evaluation` | проверка на модела за цена (leave-one-out) |

### Складов агент (роля AGENT)

| Метод и адрес | Описание |
|---|---|
| `GET /agent/api/warehouses` | складовете, към които съм назначен |
| `GET /agent/api/rentals` | моите заявки и договори |
| `GET /agent/api/report?from=&to=` | приключилите ми договори за период и рейтингът ми |
| `GET /agent/api/ai/recommend?area=&climate=&goodsType=&category=&budget=&from=&to=` | AI препоръка на склад (№15) |

### Справки (собственици и агенти)

| Метод и адрес | Описание |
|---|---|
| `GET /api/reports/available?from=&to=&category=&search=` | налични складове за период (№13) |

Датите са във формат `ГГГГ-ММ-ДД`, например `2026-12-01`.

## Реализация

- `bg.skladove.api.OpenApiConfig` задава заглавието, описанието и схемата за удостоверяване със сесия.
- Контролерите `OwnerApiController`, `AgentApiController` и `ReportApiController` използват същите услуги като HTML страниците. Логиката не е дублирана.
- Отговорите са DTO-та (`bg.skladove.api.dto`), а не entity-тата. Така в JSON не излизат LAZY връзки, пароли и данни на собственика.
- Анотациите `@Tag`, `@Operation`, `@Parameter`, `@ApiResponse` и `@Schema` добавят описанията и примерите, които се виждат в Swagger UI.
- `ApiExceptionHandler` превръща грешките от валидацията в 400 с JSON.
- `SecurityConfig` връща 401 за REST адресите вместо пренасочване към `/login`.

## Тестове

`ApiIntegrationTest` стартира цялото приложение с H2 база и проверява:
- че `/v3/api-docs` съдържа REST адресите, но не и HTML страниците;
- че Swagger UI е достъпен;
- 401 без вход и 403 при чужда роля;
- данните, които връщат endpoint-ите за собственик, агент, справки и AI;
- 400 с JSON при невалиден период.
