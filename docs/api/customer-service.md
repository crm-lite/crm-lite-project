# customer-service

Müşteri çekirdek CRUD servisi (FR-CUST-01..05). Port `8082`. Diğer servislerle aynı desen: config-server'dan
`spring.config.import` ile config çeker, Eureka'ya kayıt olur, gateway üzerinden `/api/customers/**` ile
dışarı açılır.

## Kapsam ve bilinen sınırlamalar

- **FR-CUST-03 sadece çekirdek oluşturma**: `POST /api/customers` yalnızca PARTY/INDIVIDUAL/PARTY_ROLE/CUSTOMER
  kayıtlarını oluşturur. Adres (`ADDR`) ve iletişim (`CNTC_MEDIUM`) bu serviste **yok** — address-service ve
  contact-service kurulunca orkestrasyon eklenecek (kodda `TODO` yorumlarıyla işaretli).
- **accountNumber / gsmNumber / orderNumber araması henüz desteklenmiyor**: arama endpoint'lerine bu
  parametrelerden biri verilirse, sessizce yanlış/eksik sonuç dönmek yerine **501 Not Implemented** +
  `MSG-FEATURE-NOT-IMPLEMENTED` döner. account/contact/order-service kurulunca gerçek entegrasyona çevrilecek.
- **Ürün kontrolü olmadan silme**: `DELETE /api/customers/{id}` öncesi çağrılan
  `checkCustomerHasNoActiveProducts` şu an her zaman geçiyor (product-service yok, TODO/no-op).
- **auth/JWT gateway güvenliği geçici olarak açık**: `api-gateway`'in `SecurityConfig`'i şu an `permitAll` —
  auth-service implement edilene kadar hiçbir endpoint (customer-service dahil) kimlik doğrulaması istemiyor.

## Endpoint'ler

| Metod | Path | Açıklama |
|---|---|---|
| GET | `/api/customers` | **Tercih edilen** arama endpoint'i (bkz. aşağıdaki query param'lar) |
| GET | `/api/customers/search` | Arama — geriye dönük uyumluluk için tutulan **legacy alias**, yeni parametrelerle birebir aynı davranır |
| GET | `/api/customers/{customerId}` | Detay görüntüleme |
| POST | `/api/customers` | Müşteri çekirdeği oluşturma |
| PUT | `/api/customers/{customerId}` | Müşteri çekirdeği güncelleme |
| DELETE | `/api/customers/{customerId}` | Soft delete (204 No Content) |

> **Not:** Bir servis-endpoint dokümanında `GET /customers`, `POST /customers` gibi kök path'ler geçebilir;
> gateway'in public path'i her zaman `/api/customers/**` olduğu için buradaki tüm örnekler bunu kullanır.

### Arama query param'ları
`firstName`, `lastName`, `nationalityId`, `customerId`, `accountNumber`, `gsmNumber`, `orderNumber`,
`page` (varsayılan 0), `size` (varsayılan 20, sıralama `firstName ASC, lastName ASC`).
En az bir tanesi (`firstName`/`lastName`/`nationalityId`/`customerId`) verilmeli, aksi halde 400 +
`MSG-SEARCH-CRITERIA-REQUIRED`.

**Arama davranışı (önemli):**
- `firstName`/`lastName` **prefix** (baştan eşleşme) araması yapar, `contains` değil:
  `lower(firstName) LIKE lower(:firstName) || '%'`. Yani `firstName=li` ne "Ali" ne "Velihan" döner;
  `firstName=Al` ise "Ali" ile başlayan kayıtları döner.
- `firstName` ayrıca **middleName**'in de prefix'ini eşler — `firstName=Can` "Ali Can Kaya"yı bulur
  (middleName="Can"), `firstName=Ali` da aynı kaydı firstName üzerinden bulur.
- `firstName` ve `lastName` birlikte verilirse tek bir isim kriteri olarak **AND**'lenir; bu kriter
  `nationalityId`/`customerId` ile **OR**'lanır (bkz. `CustomerSpecifications`).
- Sadece `ACTIVE` müşteriler döner (soft-delete edilmiş `PASSIVE` müşteriler asla görünmez).

## Türkçe karakter desteği (ÇĞİÖŞÜçğıöşü)

Türkçe isimler (`Gözek`, `Öztürk`, `Şahin`, `Çağla`, `İrem`, ...) API tarafında tam destekleniyor —
`VR-NAME` regex'i (`^[A-Za-zÇĞİÖŞÜçğıöşü]+( [A-Za-zÇĞİÖŞÜçğıöşü]+)*$`) bu karakterleri kabul ediyor,
JSON gövdesi UTF-8 olarak doğru okunuyor ve doğru geri dönüyor.

**Eğer Türkçe karakterli bir istek "Malformed request body" / `MSG-VALIDATION-ERROR` ile 400 dönerse,
bu servis kodunda bir hata DEĞİL, neredeyse her zaman bir shell/terminal encoding sorunudur:**
Windows + Git Bash üzerinde `curl`, komut satırı argümanlarını (`-d '...'` ile) native bir Win32
programına (mingw-w64 derlemesi) geçirirken, aktif kod sayfası (`chcp`) ve `LANG`/`LC_ALL` ayarlı
değilse, UTF-8 çok baytlı karakterler (`ö` = `0xC3 0xB6`) tek, geçersiz bir bayta dönüşebilir —
curl bu bozuk baytı olduğu gibi gönderir, sunucu da (haklı olarak) geçersiz UTF-8'i reddeder.
`GlobalExceptionHandler` artık kök nedeni (gerçek Jackson `StreamReadException`'ı) sunucu loglarına
tam olarak yazıyor; log'da `Invalid UTF-8 middle byte ...` görürseniz bu senaryodur.

**Çözüm — gövdeyi argüman değil, stdin üzerinden gönderin:**

```bash
curl -X POST "http://localhost:8080/api/customers" \
  -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @- <<'JSON'
{
  "firstName": "Velihan",
  "lastName": "Gözek",
  "birthDate": "1992-03-15",
  "gender": "Male",
  "nationalityId": "10000000004"
}
JSON
```

Heredoc/stdin (`--data-binary @-`), bash'in dahili UTF-8 işleyişini kullanır ve native bir programın
argv dönüşümünden geçmez, bu yüzden Türkçe karakterler bozulmadan iletilir. **Postman kullanıyorsanız**
bu sorun hiç yaşanmaz — Postman gövdeyi doğrudan UTF-8 olarak gönderir, `-d`'nin argv sorunu sadece
komut satırından native curl çağırırken ortaya çıkar.

Postman/GUI body örneği (aynı JSON, tek satır):
```json
{"firstName":"Velihan","lastName":"Gözek","birthDate":"1992-03-15","gender":"Male","nationalityId":"10000000004"}
```

## Gender alanı

API'de `gender` alanı büyük/küçük harf duyarsız kabul edilir (`"male"`, `"MALE"`, `"Male"` hepsi geçerli)
ama yanıt her zaman `"Male"`/`"Female"` olarak döner (bkz. `Gender` enum, `@JsonCreator`/`@JsonValue`).
Geçersiz bir değer (`"gender": "Unknown"` gibi) temiz bir 400 döner:
```json
{
  "message": "Request validation failed",
  "messageKey": "MSG-VALIDATION-ERROR",
  "validationErrors": {"gender": "Invalid gender value: Unknown"}
}
```

## Role gösterimi

Arama ve detay yanıtlarındaki `role` alanı `role.name` kullanır ("Customer"), `role.code` ("CUSTOMER")
değil — dahili lookup'lar için `role.code` hâlâ kullanılabilir ama API'ye hiç sızmıyor.

## nationalityId tekilliği (ACTIVE-only)

`nationalityId` sadece **ACTIVE müşteriler arasında** tekildir; bu kural tamamen uygulama katmanında
(`CustomerBusinessRules.checkNationalityIdIsUniqueForCreate/ForUpdate`) uygulanır. `individuals.nationality_id`
kolonunda artık **global bir DB UNIQUE kısıtı yok** (bilinçli tercih — status alanı individuals'ta değil
customers/party_roles/parties'te olduğu için, cross-table bir partial unique index olmadan DB seviyesinde
"sadece ACTIVE'ler arasında unique" ifade edilemez). Sonuç: bir müşteri soft-delete edildikten sonra aynı
nationalityId ile **yeni bir aktif müşteri oluşturulabilir** — eski (PASSIVE) kayıt DB'de durmaya devam
eder ama artık hiçbir constraint'e takılmaz.

- Aktif bir müşteriyle çakışan `nationalityId` → temiz **409** + `MSG-CUST-DUP-NATID`.
- Uygulama katmanındaki kontrolü aşan (yarış durumu gibi) beklenmedik bir DB constraint çakışması olursa,
  `GlobalExceptionHandler`'daki `DataIntegrityViolationException` handler'ı bunu genel bir 500 yerine
  yine temiz bir **409** + `MSG-CUST-DUP-NATID`'e çevirir (defense in depth).

**⚠️ Önemli — DB migration'ı değişti (V1):** `individuals.nationality_id` kolonundaki `UNIQUE` kısıtı
`V1__create_customer_tables.sql`'den kaldırıldı. Bu servis henüz merge/push edilmediği ve local/dev
aşamasında olduğu için V1 doğrudan değiştirildi (yeni bir V3 migration yerine). **Bu değişikliği çekip
daha önce customer_db'yi kurmuş bir geliştirici, Flyway'in checksum doğrulamasını geçmek için Postgres
volume'unu sıfırlamalı:**

```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d postgres
# ardından customer-service'i yeniden başlat (Flyway V1/V2'yi temiz şemaya uygular)

# Podman eşleniği:
podman compose -f infra/docker-compose.yml down -v
podman compose -f infra/docker-compose.yml up -d postgres
```

Compose kullanmıyorsanız (postgres'e doğrudan bağlıysanız), aynı etkiyi customer_db şemasını
sıfırlayarak da alabilirsiniz: `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` (customer_db'ye
bağlıyken çalıştırın), sonra customer-service'i yeniden başlatın.

## Nasıl Çalıştırılır

### Maven ile (IntelliJ/terminal)

Build (test'ler dahil değil, hızlı):
```bash
mvn clean install -DskipTests
```

Çalıştırma sırası önemli — repo kökünden, her biri **ayrı bir terminalde**:

```bash
# Terminal 1
mvn -pl backend/config-server spring-boot:run

# Terminal 2
mvn -pl backend/discovery-server spring-boot:run

# Terminal 3
mvn -pl backend/api-gateway spring-boot:run

# Postgres (Rancher/Docker):
docker compose -f infra/docker-compose.yml up -d postgres
# Podman:
podman compose -f infra/docker-compose.yml up -d postgres
# veya: podman-compose -f infra/docker-compose.yml up -d postgres

# Terminal 4 (postgres ayaktayken)
mvn -pl backend/customer-service spring-boot:run
```

IntelliJ'den çalıştırıyorsanız aynı sırayı koruyun (1→2→3→Postgres→4); `CustomerServiceApplication`'ı
Postgres ayakta değilken çalıştırırsanız Flyway/Hikari datasource hatasıyla açılış başarısız olur.

**Flyway/schema değişikliği sonrası temiz başlangıç gerekiyorsa** (bkz. yukarıdaki nationalityId notu):
```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d postgres
# Podman eşleniği:
podman compose -f infra/docker-compose.yml down -v
podman compose -f infra/docker-compose.yml up -d postgres
```
ardından customer-service'i yeniden başlatın.

### Docker / Podman Compose ile (tüm stack)

```bash
docker compose -f infra/docker-compose.yml up --build
# Podman:
podman compose -f infra/docker-compose.yml up --build
# veya: podman-compose -f infra/docker-compose.yml up --build
```

## Doğrulama / Test Örnekleri

Aşağıdaki sıra ile test edilmesi önerilir (her adım bir öncekinin ayakta olduğunu varsayar).

**A) Health check'ler**
```bash
curl http://localhost:8888/actuator/health
curl http://localhost:8761/actuator/health
curl http://localhost:8080/actuator/health
curl http://localhost:8082/actuator/health
```

**B) Gateway route smoke test**
```bash
curl "http://localhost:8080/api/customers?firstName=Ali"
```

**C) Prefix arama testleri**
```bash
curl "http://localhost:8080/api/customers?firstName=Ali"   # Ali, Ali Can Kaya
curl "http://localhost:8080/api/customers?firstName=Al"    # aynı ikisi (prefix)
curl "http://localhost:8080/api/customers?firstName=li"    # BOŞ - Ali/Velihan dönmemeli
```

**D) middleName araması**
```bash
curl "http://localhost:8080/api/customers?firstName=Can"   # Ali Can Kaya (middleName=Can üzerinden)
```

**E) lastName araması**
```bash
curl "http://localhost:8080/api/customers?lastName=Ka"     # Kaya ile başlayanlar
```

**F) nationalityId araması**
```bash
curl "http://localhost:8080/api/customers?nationalityId=10000000003"
```

**G) Geçersiz (sayısal olmayan) nationalityId → 400**
```bash
curl "http://localhost:8080/api/customers?nationalityId=abc"
```

**H) Detay**
```bash
curl "http://localhost:8080/api/customers/1"
```

**I) Türkçe karakterli oluşturma (UTF-8 güvenli, Git Bash/Linux/macOS)**
```bash
curl -X POST "http://localhost:8080/api/customers" \
  -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @- <<'JSON'
{
  "firstName": "Velihan",
  "lastName": "Gözek",
  "birthDate": "1992-03-15",
  "gender": "Male",
  "nationalityId": "10000000004"
}
JSON
```
Postman/GUI body (tek satır, aynı içerik):
```json
{"firstName":"Velihan","lastName":"Gözek","birthDate":"1992-03-15","gender":"Male","nationalityId":"10000000004"}
```

**J) Aynı nationalityId ile tekrar oluşturma → 409**
```bash
curl -X POST "http://localhost:8080/api/customers" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Ayla","lastName":"Oz","birthDate":"1992-03-15","gender":"Female","nationalityId":"10000000004"}'
```
Beklenen: `409` + `MSG-CUST-DUP-NATID`.

**K) Güncelleme**
```bash
curl -X PUT "http://localhost:8080/api/customers/1" \
  -H "Content-Type: application/json; charset=utf-8" \
  --data-binary @- <<'JSON'
{
  "firstName": "Ali",
  "middleName": "Can",
  "lastName": "Kaya",
  "birthDate": "1990-05-10",
  "gender": "Male",
  "nationalityId": "10000000001"
}
JSON
```

**L) Soft delete**
```bash
curl -X DELETE -i "http://localhost:8080/api/customers/1"
```
Beklenen: `204 No Content`.

**M) Silinen müşterinin görünmediğini doğrula**
```bash
curl "http://localhost:8080/api/customers?customerId=1"
```
Beklenen: boş sonuç (müşteri artık `PASSIVE`, arama sadece `ACTIVE` döner).

**N) Desteklenmeyen cross-service arama → 501**
```bash
curl "http://localhost:8080/api/customers?gsmNumber=05321112233"
```
Beklenen: `501` + `MSG-FEATURE-NOT-IMPLEMENTED`.

## Hata Yanıtı Formatı

```json
{
  "timestamp": "2026-07-09T16:45:20.260Z",
  "status": 409,
  "error": "Conflict",
  "messageKey": "MSG-CUST-DUP-NATID",
  "message": "nationalityId is already used by an active customer: 10000000004",
  "path": "/api/customers",
  "validationErrors": null
}
```

`GlobalExceptionHandler` şu durumları tutarlı bu şekle çevirir: `BusinessException`,
`MethodArgumentNotValidException`, `ConstraintViolationException`, `MethodArgumentTypeMismatchException`,
`HttpMessageNotReadableException` (malformed JSON / geçersiz enum — kök neden her zaman sunucu loglarına
yazılır, stack trace asla response'a sızmaz), `DataIntegrityViolationException` (409'a çevrilir) ve
son bir `Exception` catch-all'ı (500, "Unexpected error", detaylar sadece logda).

## Seed Veri (V2 migration)

| customerId | firstName | middleName | lastName | nationalityId |
|---|---|---|---|---|
| 1 | Ali | - | Yilmaz | 10000000001 |
| 2 | Ayse | - | Demir | 10000000002 |
| 3 | Ali | Can | Kaya | 10000000003 |

"Ali" (customer 1) ve "Ali Can Kaya" (customer 3) bilinçli olarak eklendi: `firstName=Ali` ikisini de
döner (customer 3 firstName üzerinden), `firstName=Can` sadece customer 3'ü döner (middleName üzerinden).

## Testler

`src/test/java` altında:
- `CustomerBusinessRulesTest` — iş kuralları için saf birim testleri (Mockito ile repository mock'lanır,
  DB gerektirmez).
- `CustomerSpecificationsTest` — gerçek local Postgres'e karşı çalışan bir `@DataJpaTest` (Testcontainers
  bu projede henüz kurulu değil; en pratik yol zaten çalışan dev Postgres'i kullanmaktı). Sadece Postgres'in
  ayakta olmasını gerektirir, config-server'a ihtiyaç duymaz (`src/test/resources/application.yml` bunu
  devre dışı bırakır). Her test kendi `@Transactional` rollback'iyle temizlenir, seed veriye dokunmaz.
- `GlobalExceptionHandlerTest` — her exception handler'ın doğru status/messageKey/mesaj eşlemesi yaptığını
  ve hiçbir zaman stack trace/iç detay sızdırmadığını doğrular.

Çalıştırma (Postgres ayaktayken):
```bash
mvn -pl backend/customer-service test
```
