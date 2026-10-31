# Диаграми на системата „Складове“ (Mermaid)

Диаграмите са написани на [Mermaid](https://mermaid.js.org) и се показват директно в GitHub
и в IntelliJ (Markdown preview). Всяка е и в отделен `.mmd` файл, а `.png` картинките са за документацията.

| Файл | Какво показва | Изискване от заданието |
|---|---|---|
| `use-case.mmd` | Кой потребител какво може да прави | 2.d.ii UML – Use Case |
| `er-chen.mmd` | Концептуален модел на базата | 2.d.iii ER диаграма (модел на Чен) |
| `class-diagram.mmd` | JPA entity класовете и връзките им | 2.d.ii UML – Class Diagram |
| `sequence-rental.mmd` | Как минава заявката за отдаване под наем | 2.d.ii UML – Sequence diagram |
| `er-relational.mmd` | Таблиците в PostgreSQL с ключовете | 2.e.i Релационна схема |

Картинките се генерират с Mermaid CLI: `npx -p @mermaid-js/mermaid-cli mmdc -i er-chen.mmd -o er-chen.png -s 2 -b white`

## 1. Use Case диаграма
Цветът на линията показва кой актьор използва случая: червено = администратор, синьо = собственик,
зелено = складов агент. «extend» = по желание, «include» = винаги.

```mermaid
---
title: Use Case диаграма — система „Складове“
---
flowchart LR
  %% Mermaid няма отделен тип за Use Case, затова се рисува с flowchart:
  %% актьорите са отвън, случаите на употреба (овали) са в рамката на системата.
  %% Цветът на линията показва кой актьор използва случая.

  ADMIN["👤 Администратор"]
  OWNER["👤 Собственик"]

  subgraph SYS["Информационна система „Складове“ (всички случаи изискват вход)"]
    subgraph G1["Потребители и профили"]
      uc1([Създаване на собственик])
      uc2([Създаване на складов агент])
      uc3([Рейтинговане на складов агент])
      uc4([Поддържане на профил<br/>хонорар и др.])
    end
    subgraph G2["Складове и наеми"]
      uc5([Добавяне / редакция на склад])
      uc6([Избор на складови агенти])
      uc7([Отдаване под наем<br/>формуляр за наем])
    end
    subgraph G3["Справки по произволен период"]
      uc8([Складове на собственик])
      uc9([Складов агент: договори, рейтинг])
      uc10([Налични складове])
      uc11([История на наемателите])
    end
    subgraph G4["Изкуствен интелект"]
      uc12([AI препоръка: цена / склад])
    end
    subgraph G5["Известия"]
      uc13([Преглед на известия])
      uc14([Проверка за изтичащи договори])
    end
  end

  AGENT["👤 Складов агент"]
  SCHED["⏱ Система (по график)"]

  %% Администратор (линии 0–1)
  ADMIN --- uc1
  ADMIN --- uc2
  %% Собственик (линии 2–9)
  OWNER --- uc3
  OWNER --- uc4
  OWNER --- uc5
  OWNER --- uc6
  OWNER --- uc8
  OWNER --- uc9
  OWNER --- uc12
  OWNER --- uc13
  %% Складов агент (линии 10–15)
  uc4 --- AGENT
  uc7 --- AGENT
  uc10 --- AGENT
  uc11 --- AGENT
  uc12 --- AGENT
  uc13 --- AGENT
  %% Система (линия 16)
  uc14 --- SCHED
  %% extend / include (линии 17–18)
  uc6 -. "«extend»" .-> uc5
  uc14 -. "«include»" .-> uc13

  linkStyle 0,1 stroke:#C00000,stroke-width:2.5px
  linkStyle 2,3,4,5,6,7,8,9 stroke:#2F5597,stroke-width:2.5px
  linkStyle 10,11,12,13,14,15 stroke:#548235,stroke-width:2.5px
  linkStyle 16 stroke:#7F7F7F,stroke-width:2.5px

  classDef admin fill:#FDE9E9,stroke:#C00000,stroke-width:2px,color:#C00000,font-weight:bold
  classDef owner fill:#DEEBF7,stroke:#2F5597,stroke-width:2px,color:#2F5597,font-weight:bold
  classDef agent fill:#E2F0D9,stroke:#548235,stroke-width:2px,color:#548235,font-weight:bold
  classDef sched fill:#EEEEEE,stroke:#7F7F7F,stroke-width:2px,color:#404040,font-weight:bold
  classDef uc fill:#FFFFFF,stroke:#7F7F7F,color:#000
  class ADMIN admin
  class OWNER owner
  class AGENT agent
  class SCHED sched
  class uc1,uc2,uc3,uc4,uc5,uc6,uc7,uc8,uc9,uc10,uc11,uc12,uc13,uc14 uc
  style SYS fill:#FFFFFF,stroke:#1F3864,stroke-width:2px
  style G1 fill:#FBE5D6,stroke:#F4B183
  style G2 fill:#DEEBF7,stroke:#9DC3E6
  style G3 fill:#E2F0D9,stroke:#A9D18E
  style G4 fill:#EADCF4,stroke:#C5A3E0
  style G5 fill:#FFF2CC,stroke:#FFD966
```

## 2. ER диаграма (модел на Чен)
Правоъгълник = обект, ромб = връзка, овал = атрибут, подчертан атрибут = първичен ключ,
1 / N / M = кардиналност.

```mermaid
---
title: ER диаграма (модел на Чен) — система „Складове“
---
flowchart TB
  %% Правоъгълник = обект, ромб = връзка, овал = атрибут, подчертан = първичен ключ

  %% ---------- Обекти ----------
  USER[ПОТРЕБИТЕЛ]
  WH[СКЛАД]
  RC[ДОГОВОР ЗА НАЕМ]
  RT[РЕЙТИНГ]
  NT[ИЗВЕСТИЕ]

  %% ---------- Връзки ----------
  owns{притежава}
  serves{обслужва}
  signs{сключва}
  covers{отнася се за}
  rated{получава оценка}
  gives{дава оценка}
  receives{получава}
  about{относно}

  USER ---|1| owns ---|N| WH
  USER ---|M| serves ---|N| WH
  USER ---|1| signs ---|N| RC
  WH ---|1| covers ---|N| RC
  USER ---|1| rated ---|N| RT
  USER ---|1| gives ---|N| RT
  USER ---|1| receives ---|N| NT
  RC ---|1| about ---|N| NT

  %% ---------- Атрибути ----------
  u1([<u>id</u>]) --- USER
  u2([username]) --- USER
  u3([парола]) --- USER
  u4([име]) --- USER
  u5([имейл]) --- USER
  u6([телефон]) --- USER
  u7([роля]) --- USER
  u8([хонорар]) --- USER

  WH --- w1([<u>id</u>])
  WH --- w2([адрес])
  WH --- w3([размери])
  WH --- w4([климат])
  WH --- w5([вид стока])
  WH --- w6([категория])
  WH --- w7([цена / месец])
  WH --- w8([статус])

  RC --- c1([<u>id</u>])
  RC --- c2([наемател])
  RC --- c3([от дата])
  RC --- c4([до дата])
  RC --- c5([цена])
  RC --- c6([статус])

  RT --- r1([<u>id</u>])
  RT --- r2([оценка 1–5])
  RT --- r3([коментар])
  RT --- r4([дата])

  NT --- n1([<u>id</u>])
  NT --- n2([тип])
  NT --- n3([съобщение])
  NT --- n4([прочетено])
  NT --- n5([дата])

  %% ---------- Стилове ----------
  classDef entity fill:#2F5597,stroke:#1F3864,stroke-width:2px,color:#fff,font-weight:bold
  classDef rel fill:#FFE699,stroke:#BF9000,stroke-width:1.5px,color:#000
  classDef attr fill:#F2F2F2,stroke:#7F7F7F,color:#000
  classDef key fill:#FFF2CC,stroke:#BF9000,stroke-width:2px,color:#000,font-weight:bold
  class USER,WH,RC,RT,NT entity
  class owns,serves,signs,covers,rated,gives,receives,about rel
  class u2,u3,u4,u5,u6,u7,u8,w2,w3,w4,w5,w6,w7,w8,c2,c3,c4,c5,c6,r2,r3,r4,n2,n3,n4,n5 attr
  class u1,w1,c1,r1,n1 key
```

## 3. Class диаграма
Синьо = модули на Човек A (потребители, рейтинг, известия), зелено = модули на Човек B (складове, наеми),
жълто = enum. `-` = private поле, `+` = public метод.

```mermaid
---
title: Class диаграма — домейн модел (JPA entity-та)
---
classDiagram
  direction TB

  class User {
    <<entity>>
    -Long id
    -String username
    -String passwordHash
    -String fullName
    -String email
    -String phone
    -Role role
    -BigDecimal fee
    +averageRating() Double
  }
  class Rating {
    <<entity>>
    -Long id
    -int score
    -String comment
    -LocalDateTime createdAt
    -User agent
    -User ratedBy
  }
  class Notification {
    <<entity>>
    -Long id
    -NotificationType type
    -String message
    -boolean read
    -LocalDateTime createdAt
    -User recipient
    -RentalContract contract
  }
  class Warehouse {
    <<entity>>
    -Long id
    -String address
    -Double length
    -Double width
    -Double height
    -ClimateCondition climate
    -String goodsType
    -WarehouseCategory category
    -BigDecimal monthlyPrice
    -WarehouseStatus status
    -User owner
    -Set~User~ agents
    +area() Double
  }
  class RentalContract {
    <<entity>>
    -Long id
    -String tenantName
    -String tenantPhone
    -String tenantEmail
    -LocalDate startDate
    -LocalDate endDate
    -BigDecimal price
    -ContractStatus status
    -Warehouse warehouse
    -User agent
    +isExpiringWithin(int days) boolean
  }

  class Role {
    <<enumeration>>
    ADMIN
    OWNER
    AGENT
  }
  class WarehouseStatus {
    <<enumeration>>
    AVAILABLE
    RENTED
    INACTIVE
  }
  class ClimateCondition {
    <<enumeration>>
    DRY
    HEATED
    REFRIGERATED
    FROZEN
  }
  class WarehouseCategory {
    <<enumeration>>
    A
    B
    C
  }
  class ContractStatus {
    <<enumeration>>
    PENDING
    ACTIVE
    EXPIRED
    CANCELLED
  }
  class NotificationType {
    <<enumeration>>
    NEW_REQUEST
    WAREHOUSE_RENTED
    CONTRACT_EXPIRING
  }

  User "1" -- "0..*" Warehouse : притежава
  User "0..*" -- "0..*" Warehouse : обслужва (агенти)
  Warehouse "1" -- "0..*" RentalContract : договори
  User "1" -- "0..*" RentalContract : сключва (агент)
  User "1" -- "0..*" Rating : оценка за агент
  User "1" -- "0..*" Rating : дадена от собственик
  User "1" -- "0..*" Notification : получава
  RentalContract "0..1" -- "0..*" Notification : относно

  User ..> Role
  Warehouse ..> WarehouseStatus
  Warehouse ..> ClimateCondition
  Warehouse ..> WarehouseCategory
  RentalContract ..> ContractStatus
  Notification ..> NotificationType

  %% синьо = модули на Човек A, зелено = модули на Човек B, жълто = enum
  style User fill:#DEEBF7,stroke:#2F5597,stroke-width:2px
  style Rating fill:#DEEBF7,stroke:#2F5597,stroke-width:2px
  style Notification fill:#DEEBF7,stroke:#2F5597,stroke-width:2px
  style Warehouse fill:#E2F0D9,stroke:#548235,stroke-width:2px
  style RentalContract fill:#E2F0D9,stroke:#548235,stroke-width:2px
  style Role fill:#FFF2CC,stroke:#BF9000
  style WarehouseStatus fill:#FFF2CC,stroke:#BF9000
  style ClimateCondition fill:#FFF2CC,stroke:#BF9000
  style WarehouseCategory fill:#FFF2CC,stroke:#BF9000
  style ContractStatus fill:#FFF2CC,stroke:#BF9000
  style NotificationType fill:#FFF2CC,stroke:#BF9000
```

## 4. Sequence диаграма: отдаване на склад под наем
Показва многослойната архитектура: Controller → Service → Repository → база данни.

```mermaid
---
title: Sequence диаграма — отдаване на склад под наем
---
sequenceDiagram
  autonumber
  actor A as Складов агент
  participant UI as Браузър<br/>(Thymeleaf форма)
  participant SEC as Spring Security
  participant C as RentalController
  participant S as RentalService
  participant WR as WarehouseRepository
  participant RR as RentalContractRepository
  participant N as NotificationService
  participant DB as PostgreSQL (Neon)

  A->>UI: Попълва формуляр за наем<br/>(наемател, период, цена)
  UI->>SEC: POST /rentals
  SEC->>SEC: Проверка: влязъл ли е и има ли роля AGENT?
  alt няма права
    SEC-->>UI: 403 Forbidden
  else има права
    SEC->>C: create(form, текущ агент)
    C->>C: @Valid — проверка на полетата
    C->>S: rent(warehouseId, данни, агент)
    S->>WR: findById(warehouseId)
    WR->>DB: SELECT * FROM warehouses WHERE id = ?
    DB-->>WR: склад
    WR-->>S: Warehouse

    alt складът е зает или агентът не е назначен за него
      S-->>C: изключение (бизнес правило)
      C-->>UI: формата отново със съобщение за грешка
    else складът е свободен
      S->>RR: save(new RentalContract(...))
      RR->>DB: INSERT INTO rental_contracts ...
      S->>S: warehouse.status = RENTED
      S->>WR: save(warehouse)
      WR->>DB: UPDATE warehouses SET status = 'RENTED'
      S->>N: notifyOwner(WAREHOUSE_RENTED)
      N->>DB: INSERT INTO notifications ...
      S->>S: log.info("Склад X отдаден ...")  [Log4j]
      S-->>C: RentalContract
      C-->>UI: redirect към договора
      UI-->>A: Потвърждение: „Складът е отдаден“
    end
  end
  Note over S,DB: Всичко в rent() е в една @Transactional транзакция:<br/>при грешка нито договорът, нито статусът се записват.
```

## 5. Релационна схема
`PK` = първичен ключ, `FK` = външен ключ, `UK` = уникална стойност.
Връзката M:N между склад и агент става чрез свързващата таблица `warehouse_agents`.

```mermaid
---
title: Релационна схема (таблици в PostgreSQL)
---
erDiagram
  users ||--o{ warehouses : "притежава (owner_id)"
  users ||--o{ warehouse_agents : "агент"
  warehouses ||--o{ warehouse_agents : "склад"
  warehouses ||--o{ rental_contracts : "договори"
  users ||--o{ rental_contracts : "сключва (agent_id)"
  users ||--o{ ratings : "оценен (agent_id)"
  users ||--o{ ratings : "оценява (rated_by_id)"
  users ||--o{ notifications : "получава"
  rental_contracts |o--o{ notifications : "относно"

  users {
    bigint id PK
    varchar username UK "уникално"
    varchar password_hash "BCrypt"
    varchar full_name
    varchar email
    varchar phone
    varchar role "ADMIN / OWNER / AGENT"
    numeric fee "хонорар (само агент)"
  }
  warehouses {
    bigint id PK
    varchar address
    double length
    double width
    double height
    varchar climate "DRY / HEATED / ..."
    varchar goods_type "вид стока"
    varchar category "A / B / C"
    numeric monthly_price
    varchar status "AVAILABLE / RENTED / INACTIVE"
    bigint owner_id FK
  }
  warehouse_agents {
    bigint warehouse_id PK,FK
    bigint agent_id PK,FK
  }
  rental_contracts {
    bigint id PK
    varchar tenant_name
    varchar tenant_phone
    varchar tenant_email
    date start_date
    date end_date
    numeric price
    varchar status "PENDING / ACTIVE / EXPIRED / CANCELLED"
    bigint warehouse_id FK
    bigint agent_id FK
  }
  ratings {
    bigint id PK
    int score "1–5"
    varchar comment
    timestamp created_at
    bigint agent_id FK
    bigint rated_by_id FK
  }
  notifications {
    bigint id PK
    varchar type "NEW_REQUEST / WAREHOUSE_RENTED / CONTRACT_EXPIRING"
    varchar message
    boolean is_read
    timestamp created_at
    bigint recipient_id FK
    bigint contract_id FK "може да е NULL"
  }
```

## Проектни решения

- **Една таблица за всички потребители** с колона `role` (ADMIN, OWNER, AGENT). Входът и правата се правят
  на едно място (Spring Security), а хонорарът (`fee`) се ползва само при агентите.
- **Наемателят не е потребител на системата.** Той не влиза в нея, затова данните му се пазят в договора.
- **Заявката за наем е договор със статус `PENDING`.** При одобрение става `ACTIVE`, след крайната дата `EXPIRED`.
- **Агентите на склад са M:N,** защото собственикът избира „един или списък“ агенти, а един агент работи с много складове.
- **Известието за изтичащ договор** се създава автоматично по график (`@Scheduled`), затова в Use Case има актьор „Система“.
