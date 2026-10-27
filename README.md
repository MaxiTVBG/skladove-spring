# skladove-spring

Информационна система за отдаване на складове под наем: собственици, складови агенти, договори, справки и известия.

## Екип
- Светлин Дечков 23621908
- Михаил Димитров 23621932

## Технологии
Java 21, Spring Boot 3, Spring Data JPA (Hibernate), PostgreSQL, Spring Security, Thymeleaf, Log4j2, Swagger (springdoc), JUnit 5.

## Стартиране
1. Инсталирайте PostgreSQL и създайте база `skladove`.
2. Ако потребителят и паролата не са `postgres`/`postgres`, задайте ги като променливи на средата `DB_USER` и `DB_PASSWORD`.
3. Стартирайте:
   ```bash
   mvn spring-boot:run
   ```
4. Отворете http://localhost:8080 и влезте с `admin` / `admin123`.

API документация: http://localhost:8080/swagger-ui.html

## Тестове
```bash
mvn test
```
Тестовете ползват H2 база в паметта и не изискват PostgreSQL.

## Структура
| Пакет | Модул |
|---|---|
| `user` | Потребители и роли (администратор, собственик, складов агент), рейтинг |
| `warehouse` | Складови помещения |
| `rental` | Договори за наем |
| `report` | Справки по период |
| `notification` | Известия |
| `ai` | AI функционалност |
| `config` | Сигурност, начални данни |

## Работа в екип
- `main` съдържа само работещ код.
- Всяка задача се прави в отделен клон, например `feature/warehouses`, и се влива с Pull Request.
- Commit-и всяка седмица, с описание какво е направено.

План за разработка: [docs/PLAN.md](docs/PLAN.md)
