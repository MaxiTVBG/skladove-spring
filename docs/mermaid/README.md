# Диаграми на системата „Складове“ (Mermaid)

Диаграмите са написани на [Mermaid](https://mermaid.js.org) и се показват директно в GitHub
и в IntelliJ (Markdown preview). Всяка е и в отделен `.mmd` файл, а `.png` картинките са за документацията.
Диаграмите отразяват кода в `src/main/java/bg/skladove` (entity-та, enum-и, `RentalService`).

| Файл | Какво показва | Изискване от заданието |
|---|---|---|
| `use-case.mmd` | Кой потребител какво може да прави | 2.d.ii UML – Use Case |
| `er-chen.mmd` | Концептуален модел на базата | 2.d.iii ER диаграма (модел на Чен) |
| `class-diagram.mmd` | JPA entity класовете и връзките им | 2.d.ii UML – Class Diagram |
| `sequence-rental.mmd` | Заявка за наем и одобрение от собственика | 2.d.ii UML – Sequence diagram |
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
      uc7([Заявка за наем<br/>формуляр: наемател, период, цена])
      uc15([Одобряване / отхвърляне<br/>на заявка за наем])
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
  %% Собственик (линии 2–10)
  OWNER --- uc3
  OWNER --- uc4
  OWNER --- uc5
  OWNER --- uc6
  OWNER --- uc15
  OWNER --- uc8
  OWNER --- uc9
  OWNER --- uc12
  OWNER --- uc13
  %% Складов агент (линии 11–16)
  uc4 --- AGENT
  uc7 --- AGENT
  uc10 --- AGENT
  uc11 --- AGENT
  uc12 --- AGENT
  uc13 --- AGENT
  %% Система (линия 17)
  uc14 --- SCHED
  %% extend / include (линии 18–19)
  uc6 -. "«extend»" .-> uc5
  uc14 -. "«include»" .-> uc13

  linkStyle 0,1 stroke:#C00000,stroke-width:2.5px
  linkStyle 2,3,4,5,6,7,8,9,10 stroke:#2F5597,stroke-width:2.5px
  linkStyle 11,12,13,14,15,16 stroke:#548235,stroke-width:2.5px
  linkStyle 17 stroke:#7F7F7F,stroke-width:2.5px

  classDef admin fill:#FDE9E9,stroke:#C00000,stroke-width:2px,color:#C00000,font-weight:bold
  classDef owner fill:#DEEBF7,stroke:#2F5597,stroke-width:2px,color:#2F5597,font-weight:bold
  classDef agent fill:#E2F0D9,stroke:#548235,stroke-width:2px,color:#548235,font-weight:bold
  classDef sched fill:#EEEEEE,stroke:#7F7F7F,stroke-width:2px,color:#404040,font-weight:bold
  classDef uc fill:#FFFFFF,stroke:#7F7F7F,color:#000
  class ADMIN admin
  class OWNER owner
  class AGENT agent
  class SCHED sched
  class uc1,uc2,uc3,uc4,uc5,uc6,uc7,uc8,uc9,uc10,uc11,uc12,uc13,uc14,uc15 uc
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
  NT[ИЗВЕСТИЕ]

  %% ---------- Връзки ----------
  owns{притежава}
  serves{обслужва}
  creates{създава заявка}
  decides{одобрява /<br/>отхвърля}
  covers{отнася се за}
  receives{получава}

  USER ---|1| owns ---|N| WH
  USER ---|M| serves ---|N| WH
  USER ---|1| creates ---|N| RC
  USER ---|1| decides ---|N| RC
  WH ---|1| covers ---|N| RC
  USER ---|1| receives ---|N| NT

  %% ---------- Атрибути ----------
  u1([<u>id</u>]) --- USER
  u2([username]) --- USER
  u3([парола]) --- USER
  u4([име]) --- USER
  u5([имейл]) --- USER
  u6([телефон]) --- USER
  u7([роля]) --- USER
  u8([хонорар]) --- USER
  u9([рейтинг]) --- USER

  WH --- w1([<u>id</u>])
  WH --- w2([име])
  WH --- w3([адрес])
  WH --- w4([площ])
  WH --- w5([височина])
  WH --- w6([климатични условия])
  WH --- w7([вид стока])
  WH --- w8([категория])
  WH --- w9([цена / месец])
  WH --- w10([статус])

  RC --- c1([<u>id</u>])
  RC --- c2([наемател<br/>име, телефон, имейл])
  RC --- c3([от дата])
  RC --- c4([до дата])
  RC --- c5([цена])
  RC --- c6([статус])
  RC --- c7([дата на заявка])
  RC --- c8([дата на решение])
  RC --- c9([бележка])

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
  class USER,WH,RC,NT entity
  class owns,serves,creates,decides,covers,receives rel
  class u2,u3,u4,u5,u6,u7,u8,u9,w2,w3,w4,w5,w6,w7,w8,w9,w10,c2,c3,c4,c5,c6,c7,c8,c9,n2,n3,n4,n5 attr
  class u1,w1,c1,n1 key
```

## 3. Class диаграма
Синьо = модули на Човек A (потребители, известия), зелено = модули на Човек B (складове, наеми),
жълто = enum. `-` = private поле, `#` = protected метод.

```mermaid
---
title: Class диаграма — домейн модел (JPA entity-та)
---
classDiagram
  direction TB

  class AppUser {
    <<entity>>
    -Long id
    -String username
    -String password
    -String fullName
    -String email
    -String phone
    -Role role
    -BigDecimal fee
    -Double rating
  }
  class Notification {
    <<entity>>
    -Long id
    -AppUser recipient
    -Type type
    -String message
    -boolean read
    -LocalDateTime createdAt
  }
  class Warehouse {
    <<entity>>
    -Long id
    -String name
    -String address
    -Double area
    -Double height
    -String climateConditions
    -String goodsType
    -String category
    -BigDecimal monthlyPrice
    -WarehouseStatus status
    -AppUser owner
    -Set~AppUser~ agents
  }
  class RentalContract {
    <<entity>>
    -Long id
    -Warehouse warehouse
    -AppUser agent
    -AppUser decidedBy
    -String tenantName
    -String tenantPhone
    -String tenantEmail
    -LocalDate startDate
    -LocalDate endDate
    -BigDecimal price
    -RentalStatus status
    -LocalDate createdAt
    -LocalDateTime decidedAt
    -String decisionNote
    #onCreate() void
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
    MAINTENANCE
    INACTIVE
  }
  class RentalStatus {
    <<enumeration>>
    PENDING_OWNER_APPROVAL
    ACTIVE
    REJECTED
    COMPLETED
    CANCELLED
  }
  class Type {
    <<enumeration>>
    NEW_RENTAL_REQUEST
    WAREHOUSE_RENTED
    CONTRACT_EXPIRING
  }

  AppUser "1" -- "0..*" Warehouse : притежава (owner)
  AppUser "0..*" -- "0..*" Warehouse : обслужва (agents)
  Warehouse "1" -- "0..*" RentalContract : договори
  AppUser "1" -- "0..*" RentalContract : създава заявка (agent)
  AppUser "0..1" -- "0..*" RentalContract : одобрява / отхвърля (decidedBy)
  AppUser "1" -- "0..*" Notification : получава (recipient)

  AppUser ..> Role
  Warehouse ..> WarehouseStatus
  RentalContract ..> RentalStatus
  Notification ..> Type : вложен enum Notification.Type

  %% синьо = модули на Човек A, зелено = модули на Човек B, жълто = enum
  style AppUser fill:#DEEBF7,stroke:#2F5597,stroke-width:2px
  style Notification fill:#DEEBF7,stroke:#2F5597,stroke-width:2px
  style Warehouse fill:#E2F0D9,stroke:#548235,stroke-width:2px
  style RentalContract fill:#E2F0D9,stroke:#548235,stroke-width:2px
  style Role fill:#FFF2CC,stroke:#BF9000
  style WarehouseStatus fill:#FFF2CC,stroke:#BF9000
  style RentalStatus fill:#FFF2CC,stroke:#BF9000
  style Type fill:#FFF2CC,stroke:#BF9000
```

## 4. Sequence диаграма: заявка за наем и одобрение
Показва многослойната архитектура (Controller → Service → Repository → база данни)
и двете стъпки на наема: агентът създава заявка, собственикът я одобрява или отхвърля.

```mermaid
---
title: Sequence диаграма — заявка за наем и одобрение от собственика
---
sequenceDiagram
  autonumber
  actor AG as Складов агент
  actor OW as Собственик
  participant SEC as Spring Security
  participant C as RentalController
  participant S as RentalService
  participant AV as WarehouseAvailabilityService
  participant R as Repositories
  participant DB as PostgreSQL (Neon)

  rect rgb(226, 240, 217)
  Note over AG,DB: Част 1 — агентът създава заявка за наем
  AG->>SEC: POST /agent/rentals (наемател, период, цена)
  SEC->>SEC: Влязъл ли е и има ли роля AGENT?
  SEC->>C: заявката е разрешена
  C->>S: createRequest(warehouseId, agentId, данни)
  S->>R: findById(склад), findById(агент)
  R->>DB: SELECT ...
  S->>S: Проверки: ролята е AGENT, агентът е назначен<br/>към склада, периодът и цената са валидни
  S->>AV: isAvailable(склад, от, до)
  AV->>R: findByWarehouseIdAndStatus(склад, ACTIVE)
  R->>DB: SELECT ... FROM rental_contract
  AV-->>S: true / false (има ли застъпване на периоди)
  alt складът е зает за периода или проверка не минава
    S-->>C: изключение със съобщение
    C-->>AG: формата отново, с грешката
  else всичко е наред
    S->>R: save(договор със статус PENDING_OWNER_APPROVAL)
    R->>DB: INSERT INTO rental_contract ...
    C-->>AG: „Заявката е изпратена за одобрение“
  end
  end

  rect rgb(222, 235, 247)
  Note over OW,DB: Част 2 — собственикът одобрява или отхвърля
  OW->>SEC: POST /owner/rentals/{id}/approve или /reject
  SEC->>SEC: Влязъл ли е и има ли роля OWNER?
  SEC->>C: заявката е разрешена
  alt одобрява
    C->>S: approveRequest(contractId, ownerId)
    S->>S: Проверки: той е собственикът на склада,<br/>заявката още чака решение
    S->>AV: isAvailable(...) — повторна проверка
    S->>R: save(договор: ACTIVE, decidedBy, decidedAt)
    S->>AV: synchronizeStatus(склад)
    AV->>AV: Има ли активен договор днес? → RENTED, иначе AVAILABLE
    S->>R: save(склад)
    R->>DB: UPDATE rental_contract, UPDATE warehouse
    C-->>OW: „Заявката е одобрена“
  else отхвърля
    C->>S: rejectRequest(contractId, ownerId, бележка)
    S->>R: save(договор: REJECTED, бележка)
    R->>DB: UPDATE rental_contract
    C-->>OW: „Заявката е отхвърлена“
  end
  end

  Note over S,DB: Всеки метод на RentalService е @Transactional:<br/>при грешка нищо не се записва наполовина.
```

## 5. Релационна схема
`PK` = първичен ключ, `FK` = външен ключ, `UK` = уникална стойност.
Връзката M:N между склад и агент става чрез свързващата таблица `warehouse_agent`.

```mermaid
---
title: Релационна схема (таблици в PostgreSQL)
---
erDiagram
  app_user ||--o{ warehouse : "притежава (owner_id)"
  app_user ||--o{ warehouse_agent : "агент"
  warehouse ||--o{ warehouse_agent : "склад"
  warehouse ||--o{ rental_contract : "договори"
  app_user ||--o{ rental_contract : "създава (agent_id)"
  app_user |o--o{ rental_contract : "решава (decided_by_id)"
  app_user ||--o{ notification : "получава"

  app_user {
    bigint id PK
    varchar username UK "уникално"
    varchar password "BCrypt хеш"
    varchar full_name
    varchar email
    varchar phone
    varchar role "ADMIN / OWNER / AGENT"
    numeric fee "хонорар (само агент)"
    float rating "среден рейтинг (само агент)"
  }
  warehouse {
    bigint id PK
    varchar name
    varchar address
    float area "кв. м"
    float height "м"
    varchar climate_conditions
    varchar goods_type "вид стока"
    varchar category
    numeric monthly_price
    varchar status "AVAILABLE / RENTED / MAINTENANCE / INACTIVE"
    bigint owner_id FK
  }
  warehouse_agent {
    bigint warehouse_id PK,FK
    bigint agent_id PK,FK
  }
  rental_contract {
    bigint id PK
    bigint warehouse_id FK
    bigint agent_id FK
    bigint decided_by_id FK "собственик, NULL докато чака"
    varchar tenant_name
    varchar tenant_phone
    varchar tenant_email
    date start_date
    date end_date
    numeric price
    varchar status "PENDING_OWNER_APPROVAL / ACTIVE / REJECTED / COMPLETED / CANCELLED"
    date created_at
    timestamp decided_at
    varchar decision_note
  }
  notification {
    bigint id PK
    bigint recipient_id FK
    varchar type "NEW_RENTAL_REQUEST / WAREHOUSE_RENTED / CONTRACT_EXPIRING"
    varchar message
    boolean read
    timestamp created_at
  }
```

## Проектни решения

- **Една таблица за всички потребители** (`app_user`) с колона `role` (ADMIN, OWNER, AGENT).
  Входът и правата се правят на едно място (Spring Security). Хонорарът (`fee`) и средният рейтинг
  (`rating`) се ползват само при агентите.
- **Наемателят не е потребител на системата.** Той не влиза в нея, затова данните му се пазят в договора.
- **Наемът минава през одобрение.** Агентът създава заявка (`PENDING_OWNER_APPROVAL`), собственикът
  я одобрява (`ACTIVE`) или отхвърля (`REJECTED`, с бележка). Кой и кога е решил се пази в
  `decided_by_id` и `decided_at`.
- **Свободен ли е складът се проверява по периоди**, а не само по статус: заявка се приема, само ако
  периодът ѝ не се застъпва с активен договор (`WarehouseAvailabilityService.isAvailable`).
  Статусът на склада (`RENTED` / `AVAILABLE`) се пресмята от активните договори (`synchronizeStatus`).
- **Агентите на склад са M:N,** защото собственикът избира „един или списък“ агенти, а един агент работи с много складове.
- **Известието за изтичащ договор** ще се създава автоматично по график (`@Scheduled`), затова в Use Case има актьор „Система“.
