# PROJECTBRAIN — CRM Lite

> **Amaç:** Bu dosya projenin **güncel durumunun tek doğ­ru kaynağıdır** (single source of truth).
> Hem projeye sonradan dönen geliştirici, hem de sıfırdan bağlam kuran bir AI agent bu dosyayı
> okuyarak "nerede kaldık, neden böyle yapıldı, sırada ne var" sorularını cevaplayabilmelidir.
>
> **Son güncelleme:** 2026-07-09 (customer-service sertleştirildi: arama prefix+middleName, role.name,
> canonical `GET /api/customers`, nationalityId ACTIVE-only tekillik, gateway 503 fix, testler eklendi)
> **Bu dosyayı güncel tut:** Her anlamlı değişiklikten sonra ilgili bölümü ve "Sırada ne var" listesini güncelle.

---

## 1. Proje Özeti

CRM Lite — Spring Boot tabanlı bir **mikroservis monorepo**'su. Altyapı çekirdeği
(config server + service discovery + API gateway) kurulu ve çalışır durumda; `customer-service`
müşteri çekirdek CRUD'unu (arama/görüntüleme/oluşturma/güncelleme/soft-delete) implemente ediyor
ve Postgres ile çalışıyor; `auth-service` henüz iskelet halinde.

- **Dil / Runtime:** Java 25
- **Framework:** Spring Boot `4.1.0`, Spring Cloud `2025.1.2`
- **Build:** Maven (monorepo, kök parent POM ile)
- **Container:** Dockerfile'lar + `infra/docker-compose.yml` (Rancher Desktop **ve** Podman uyumlu)
- **Geliştirme ortamı:** Windows 11, IntelliJ IDEA. Makinede terminalde `mvn`/`docker` PATH'te
  kurulu DEĞİL — servisler IDE'den (Run) veya Podman/Rancher üzerinden çalıştırılıyor.

---

## 2. Mimari

```
                    ┌─────────────────────┐
   İstemci ───────► │   api-gateway :8080 │  (Spring Cloud Gateway - WebMVC)
                    └──────────┬──────────┘
                               │ lb:// (Eureka'dan servis bulur)
                               ▼
                    ┌─────────────────────┐        ┌──────────────────────┐
                    │  auth-service :8081 │        │ discovery-server:8761│
                    │  (JPA + JWT, İSKELET│◄──────►│  (Eureka Server)     │
                    │   Postgres gerekli) │  kayıt └──────────────────────┘
                    └─────────────────────┘

   Üç servis de açılışta ─────► ┌───────────────────────┐
   spring.config.import ile     │ config-server :8888   │
   kendi config'ini çeker       │ (native/classpath repo)│
                                 └───────────────────────┘
```

| Servis | Port | Rol | Durum |
|---|---|---|---|
| `config-server` | 8888 | Merkezi config (Spring Cloud Config Server, native/classpath) | ✅ Çalışıyor |
| `discovery-server` | 8761 | Eureka service registry | ✅ Çalışıyor |
| `api-gateway` | 8080 | API gateway (WebMVC), routing + security | ✅ Çalışıyor |
| `auth-service` | 8081 | Kimlik doğrulama / JWT | ⛔ İskelet — Postgres + kod eksik, ayağa kalkmıyor |
| `customer-service` | 8082 | Müşteri çekirdek CRUD (FR-CUST-01..05) | ✅ Çalışıyor — Postgres gerekli |

---

## 3. Depo Yapısı

```
crm-lite-project-dev/
├── pom.xml                        # KÖK PARENT POM (packaging: pom) — ortak versiyonları yönetir
├── .dockerignore                  # Docker build context'i küçük tutar (target, .git, .idea hariç)
├── PROJECTBRAIN.md                # BU DOSYA
├── backend/
│   ├── config-server/
│   │   ├── pom.xml                # parent = com.crm:crm-lite-project
│   │   ├── Dockerfile             # root context, maven base image
│   │   └── src/main/
│   │       ├── java/com/crm/configserver/ConfigServerApplication.java  (@EnableConfigServer)
│   │       └── resources/
│   │           ├── application.yml            # profile=native, search-locations=classpath:/config-repo/
│   │           └── config-repo/                # her servisin config'i burada, servis adına göre dosyalanmış
│   │               ├── discovery-server.yml
│   │               ├── api-gateway.yml
│   │               └── auth-service.yml
│   ├── discovery-server/
│   │   ├── pom.xml                # parent = com.crm:crm-lite-project
│   │   ├── Dockerfile             # root context, maven base image
│   │   └── src/main/
│   │       ├── java/com/crm/discovery/DiscoveryServerApplication.java  (@EnableEurekaServer)
│   │       └── resources/application.yml
│   ├── api-gateway/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/crm/gateway/
│   │       │   ├── ApiGatewayApplication.java
│   │       │   └── config/SecurityConfig.java   # permitAll (geçici)
│   │       └── resources/application.yml
│   ├── auth-service/
│   │   ├── pom.xml
│   │   ├── Dockerfile
│   │   └── src/main/
│   │       ├── java/com/crm/auth/
│   │       │   ├── AuthServiceApplication.java
│   │       │   ├── common/    (controller/dto/entity/repository/service — HEPSİ BOŞ İSKELET)
│   │       │   ├── login/     (controller/dto/entity/repository/service — HEPSİ BOŞ İSKELET)
│   │       │   ├── security/  (controller/dto/entity/repository/service — HEPSİ BOŞ İSKELET)
│   │       │   └── session/   (controller/dto/entity/repository/service — HEPSİ BOŞ İSKELET)
│   │       └── resources/
│   │           ├── application.yml                 # datasource BOŞ (doldurulmalı)
│   │           └── db/migration/.gitkeep           # Flyway migration YOK
│   └── customer-service/
│       ├── pom.xml                # parent = com.crm:crm-lite-project + annotationProcessorPaths(lombok) fix
│       ├── Dockerfile             # root context, maven base image
│       └── src/main/
│           ├── java/com/crm/customer/
│           │   ├── CustomerServiceApplication.java
│           │   ├── common/exception/    (BusinessException, ErrorResponse, GlobalExceptionHandler, MessageKeys)
│           │   └── customer/
│           │       ├── controller/CustomerController.java
│           │       ├── service/CustomerService.java + service/impl/CustomerServiceImpl.java
│           │       ├── rules/CustomerBusinessRules.java
│           │       ├── repository/ (5 repo + CustomerSpecifications)
│           │       ├── entity/ (Role, Party, Individual, PartyRole, Customer, Status, Gender)
│           │       ├── dto/request/ + dto/response/
│           │       └── mapper/CustomerMapper.java
│           └── resources/
│               ├── application.yml
│               └── db/migration/ (V1__create_customer_tables.sql, V2__seed_customer_data.sql)
└── infra/
    ├── docker-compose.yml         # config-server + discovery-server + api-gateway + postgres + customer-service
    └── postgres/init/01-create-databases.sql   # CREATE DATABASE customer_db
```

---

## 4. Servislerin Güncel Durumu (detay)

### 4.0 config-server ✅
- `@EnableConfigServer`, port 8888.
- Profil `native`, kaynak `classpath:/config-repo/` — yani config dosyaları **git-backed bir dış repo değil**,
  bu servisin kendi `src/main/resources/config-repo/` klasöründe duruyor ve jar'a gömülüyor.
- Her istemci servis kendi adına göre bir dosya çeker (`discovery-server.yml`, `api-gateway.yml`, `auth-service.yml`).
  `application.yml` adlı bir dosya varsa (şu an yok) tüm servislere ortak uygulanır.
- **Trade-off:** classpath kaynaklı olduğu için bir config değişikliği config-server'ın rebuild/restart
  edilmesini gerektirir; git-backed'deki gibi canlı `/actuator/refresh` akışı yok. Dev aşaması için kabul edilen ödün.
- Diğer üç servis `spring.config.import: "optional:configserver:http://localhost:8888"` ile buna bağlanıyor;
  `optional:` öneki sayesinde config-server ayakta değilse servis yine de (config-server'dan önceki) yerel
  varsayılanlarıyla açılmaya çalışır — sert bir bağımlılık değil, ama pratikte config-server'ın **ilk** açılması gerekiyor.

### 4.1 discovery-server ✅
- `@EnableEurekaServer`, port 8761.
- `application.yml`: kendini register etmiyor (`register-with-eureka: false`, `fetch-registry: false`) — bu ayar artık
  yerel dosyada değil, `config-server`'ın `config-repo/discovery-server.yml` dosyasında.
- IDE'den çalışıyor, dashboard `http://localhost:8761` açılıyor.
- **Not:** Eureka "self-preservation" uyarısı (EMERGENCY banner) tek client varken normaldir, zararsız.

### 4.2 api-gateway ✅
- `spring-cloud-starter-gateway-server-webmvc` → **WebMVC (servlet) stack** (WebFlux DEĞİL). Bu ayrım kritik (bkz. §6).
- Eureka client olarak kayıt oluyor.
- **Security:** `config/SecurityConfig.java` → servlet `SecurityFilterChain`, şu an `anyRequest().permitAll()` + `csrf().disable()`. Geçici; JWT gelince sıkılaştırılacak.
- **Route:** `spring.cloud.gateway.server.webmvc.routes` altında `/api/auth/**` → `lb://auth-service`.
- **Timeout:** `spring.http.client.connect-timeout: 2s`, `read-timeout: 5s` (WebMVC'nin doğru property'leri).
- **Eureka instance:** `prefer-ip-address: true`, `instance-id: ${spring.application.name}:${random.value}`.
- **Actuator:** `health, info, mappings` açık.
- **Not:** Route/timeout/eureka/actuator ayarlarının tamamı artık yerel `application.yml`'de değil,
  `config-server`'ın `config-repo/api-gateway.yml` dosyasında. Yerel dosyada sadece `spring.application.name`
  ve `spring.config.import` kaldı.
- **Doğrulanmış davranış:**
  - `GET /actuator/health` → `{"status":"UP"}` (401 yok, security düzgün).
  - `GET /actuator/mappings` → route `predicate: /api/auth/**` + `ProxyExchangeHandlerFunction` görünüyor (route yüklü).
  - `GET /api/auth/login` → **503** (route eşleşiyor, load-balancer auth-service'i arıyor ama ayakta değil — BEKLENEN doğru sonuç).
- **`GatewayExceptionHandler` (yeni, `com.crm.gateway.exception`):** downstream servis Eureka'da hiç kayıtlı
  değilse, `LoadBalancerFilterFunctions` bir `HttpServerErrorException` fırlatıyor ve gerçek status'u
  (örn. 503, mesajı "Unable to find instance for X") kendi içinde taşıyor — ama hiçbir şey bunu geri okumadığı
  için düzeltilmeden önce **generic 500**'e düşüyordu (gerçekte gözlemlendi: `mvn spring-boot:run` ile
  auth-service kapalıyken `GET /api/auth/login` **500** dönüyordu, PROJECTBRAIN'deki eski "503" notu yanlıştı).
  Yeni `@RestControllerAdvice`: `HttpStatusCodeException`'ı yakalayıp gömülü status'u (503 için
  `MSG-SERVICE-UNAVAILABLE`) response'a yansıtıyor; `ResourceAccessException` (instance bulundu ama TCP
  bağlantısı başarısız) için de 503 dönüyor; kalan her şey için generic 500 + `MSG-INTERNAL-ERROR`.
  Response şekli: `{timestamp, status, error, messageKey, message, path}` — customer-service'in
  `ErrorResponse`'una benzer ama gateway'e özel ayrı bir `GatewayErrorResponse` record'u.

### 4.3 auth-service ⛔
- **Ayağa KALKMIYOR.** Başlatınca hata:
  `Failed to configure a DataSource: 'url' attribute is not specified...` / `Failed to determine a suitable driver class`.
- Sebep: `spring.datasource.url/username/password` **boş** — artık yerel `application.yml`'de değil,
  `config-server`'ın `config-repo/auth-service.yml` dosyasında boş duruyor; ve `db/migration` altında **hiç Flyway migration yok**.
- Java dosyalarının **tamamı boş iskelet** (paket + boş class). Hiç iş mantığı yazılmamış.
- `pom.xml`: web, data-jpa, security, validation, actuator, flyway (+ postgresql), lombok, jjwt (api/impl/jackson), config-client bağımlılıkları hazır.
- **Not:** Postgres kurulduğunda datasource/flyway değerleri doğrudan `config-repo/auth-service.yml`'e yazılmalı
  (önce yerel dosyaya geçici değer koyup sonra config-server'a taşımak yerine — bkz. §5.8 gerekçesi).

### 4.4 customer-service ✅
- Port 8082. Müşteri çekirdek CRUD'u: FR-CUST-01 (arama), FR-CUST-02 (detay), FR-CUST-03 (**sadece çekirdek**
  oluşturma — adres/iletişim hariç), FR-CUST-04 (güncelleme), FR-CUST-05 (soft delete).
- **Katman mimarisi:** Controller → Service → BusinessRules → Repository. `common/exception` altında
  `BusinessException` + `ErrorResponse` + `GlobalExceptionHandler` (+ `MessageKeys` sabitleri).
- **Veri modeli:** `roles` / `parties` / `individuals` / `party_roles` / `customers` — Party-Role deseni
  (Party ↔ Individual 1-1, PartyRole Party+Role'e bağlar, Customer bir PartyRole'e bağlanır). Flyway ile
  kurulu, `hibernate.ddl-auto: validate` (asla create/update değil).
- **Endpoint'ler:** canonical arama `GET /api/customers`; `GET /api/customers/search` geriye dönük uyumluluk
  için **legacy alias** olarak tutuluyor (aynı controller metoduna delege ediyor, davranış birebir aynı).
  Detay/oluşturma/güncelleme/silme aynı kaldı (bkz. docs/customer-service.md).
- **Arama (FR-CUST-01):** `CustomerSpecifications` ile JPA Criteria — sadece `ACTIVE` müşteriler.
  `firstName`/`lastName` **prefix** araması yapar (`lower(x) LIKE lower(:x) || '%'`), `contains` değil —
  `firstName=li` "Ali"/"Velihan" döndürmez. `firstName` ayrıca `middleName`'in prefix'ini de eşler
  (`firstName=Can` → "Ali Can Kaya", middleName="Can" üzerinden). firstName+lastName tek bir "isim kriteri"
  olarak AND'lenir, bu kriter nationalityId ve customerId ile OR'lanır. Sayfalama varsayılan 20,
  `firstName ASC, lastName ASC` sıralı (nested path: `partyRole.party.individual.firstName`).
- **Türkçe karakter desteği:** `VR-NAME` regex'i Türkçe harfleri (ÇĞİÖŞÜçğıöşü) kabul ediyor ve API bunları
  doğru işliyor — "Malformed request body" hatası görülürse bu neredeyse her zaman bir **shell/terminal
  encoding sorunudur** (Windows + Git Bash + native curl.exe, argv üzerinden Türkçe karakter geçerken kod
  sayfası dönüşümü bozuyor), sunucu kodunda bir hata değil. `GlobalExceptionHandler` artık kök nedeni
  (gerçek Jackson istisnasını) loglara tam yazıyor; doğru curl kullanımı (`--data-binary @-` ile stdin)
  docs/customer-service.md'de detaylı anlatılıyor. Ayrıca geçersiz bir enum değeri (örn. `gender: "Unknown"`)
  artık generic "Malformed request body" yerine temiz bir 400 + `validationErrors: {"gender": "..."}` döner.
- **Role gösterimi:** Arama/detay yanıtlarındaki `role` alanı `role.name` kullanıyor ("Customer"),
  `role.code` ("CUSTOMER") değil — dahili lookup'lar (`roleRepository.findByCode`) hâlâ code kullanıyor.
- **accountNumber/gsmNumber/orderNumber** henüz customer-service'e ait değil — verilirse **501** +
  `MSG-FEATURE-NOT-IMPLEMENTED` döner (sessizce yanlış sonuç vermek yerine). account/contact/order-service
  geldiğinde entegre edilecek (TODO yorumları kodda mevcut).
- **nationalityId tekilliği — düzeltildi (ACTIVE-only):** `individuals.nationality_id` üzerindeki global DB
  `UNIQUE` kısıtı **kaldırıldı** (V1 migration doğrudan değiştirildi, henüz merge/push edilmediği için kabul
  edilebilir — bkz. §5.14). Artık tekillik **sadece uygulama katmanında, ACTIVE müşteriler arasında**
  kontrol ediliyor (`checkNationalityIdIsUniqueForCreate/ForUpdate`), DB seviyesinde hiçbir kısıt yok.
  Sonuç: soft-delete edilmiş bir müşterinin nationalityId'si artık **yeniden kullanılabiliyor** (eskiden
  global UNIQUE'e takılıp ham bir DB hatasına düşüyordu). Ek güvence: `DataIntegrityViolationException`
  artık `GlobalExceptionHandler`'da yakalanıp temiz bir 409 + `MSG-CUST-DUP-NATID`'e çevriliyor (uygulama
  kontrolünü aşan bir yarış durumu için defense-in-depth).
  **⚠️ Bu migration değişikliğini çeken geliştiriciler `customer_db`'yi sıfırlamalı** (bkz. §5.14, §7.2).
- **Soft delete (FR-CUST-05):** customer + partyRole + party durumu tek transaction'da `PASSIVE`'e çekiliyor.
  `checkCustomerHasNoActiveProducts` şu an TODO/no-op (product-service yok), ileride account/product-service'e
  entegre edilecek.
- **Mesaj anahtarları:** Verilen listedeki 9 key kullanılıyor + iki ek key (`MSG-VALIDATION-ERROR`,
  `MSG-INTERNAL-ERROR`) framework seviyesi hatalar (Bean Validation, malformed JSON, beklenmeyen hata) için
  eklendi — orijinal listede yoktu, bilinçli bir ekleme.
- **Gateway route'u:** `config-repo/api-gateway.yml`'e `/api/customers/**` → `lb://customer-service` eklendi.
- **Testler eklendi** (`src/test/java`): `CustomerBusinessRulesTest` (saf Mockito birim testleri),
  `CustomerSpecificationsTest` (gerçek local Postgres'e karşı `@DataJpaTest`, Testcontainers kurulu değil —
  config-server'a ihtiyaç duymadan sadece Postgres'i kullanacak şekilde `src/test/resources/application.yml`
  ile izole edildi), `GlobalExceptionHandlerTest`. Spring Boot 4'te `@DataJpaTest`/`@AutoConfigureTestDatabase`/
  `TestEntityManager` `spring-boot-test-autoconfigure`'dan ayrı modüllere taşındı
  (`spring-boot-data-jpa-test`, `spring-boot-jdbc-test`, `spring-boot-jpa-test`) — bkz. §5.11'deki Flyway
  örneğiyle aynı desen; `customer-service/pom.xml`'e `spring-boot-data-jpa-test` test-scope bağımlılığı eklendi.
- **⚠️ Proje çapında önemli düzeltme:** Bu servisi yazarken **Lombok'un hiç çalışmadığı** ortaya çıktı —
  JDK 25 + bu Maven Compiler Plugin sürümü, `annotationProcessorPaths` açıkça tanımlanmadıkça artık
  `-classpath`'teki processor'leri (Lombok dahil) otomatik keşfetmiyor. `customer-service/pom.xml`'e bu
  yapılandırma eklenerek düzeltildi (bkz. §5.9). **`auth-service`'in pom'unda da Lombok bağımlılığı var ama
  henüz hiç kullanılmıyor (tüm sınıflar boş) — o servise gerçek Lombok anotasyonlu kod yazıldığı an aynı
  hatayla karşılaşılacak, aynı düzeltme oraya da taşınmalı.**

---

## 5. Alınan Kararlar ve Gerekçeleri

Bu bölüm "neden böyle yapıldı" sorusunun cevabıdır. Değiştirmeden önce gerekçeyi oku.

### 5.1 Monorepo + kök parent POM
- Kökte `com.crm:crm-lite-project` (packaging `pom`) oluşturuldu; `java.version`, `spring-cloud.version`
  ve `spring-cloud-dependencies` importu **tek yerden** yönetiliyor.
- Dört servisin `<parent>`'ı bu köke bağlı (`relativePath ../../pom.xml`). Tekrarlanan versiyon/dependencyManagement
  blokları child POM'lardan silindi. Servise özel property'ler (örn. auth-service `jjwt.version`) child'da kaldı.
- **Yeni servis eklerken:** kök `pom.xml` `<modules>`'a ekle + yeni servisin parent'ını köke bağla.

### 5.2 Docker build context = repo KÖKÜ (önemli!)
- Parent POM köke taşınınca, her servisin POM'u `../../pom.xml`'e bakıyor. Docker build context'i
  sadece servis klasörü olursa parent POM context dışında kalır → build patlar.
- **Çözüm (seçilen):** build context repo köküdür. Dockerfile'larda `COPY pom.xml` + `COPY backend/<servis>/...`
  şeklinde gerçek klasör hiyerarşisi korunur; `mvn -f backend/<servis>/pom.xml ...` ile build edilir.
- **docker-compose'da** her servis: `build.context: ..` (infra'nın bir üstü = repo kökü) + `dockerfile: backend/<servis>/Dockerfile`.
- **Build komutu (elle):** repo kökünden `docker build -f backend/<servis>/Dockerfile -t <ad> .`
- Alternatif "parent POM'u .m2'ye install et" yaklaşımı reddedildi (ekstra base-image adımı + güncel tutma riski).

### 5.3 Dockerfile base image = `maven:3.9-eclipse-temurin-25`
- Başlangıçta `eclipse-temurin:25-jdk` + `mvnw` (Maven wrapper) kullanılıyordu.
- **Sorun:** Bu projedeki wrapper "jar'sız" tip — `mvnw` çalışınca wrapper jar'ını internetten indirmeye çalışır,
  ama `eclipse-temurin` imajında `curl`/`wget` yok → `ClassNotFoundException: MavenWrapperMain` hatası.
- **Çözüm:** İçinde Maven hazır gelen `maven:3.9-eclipse-temurin-25` base image; doğrudan `mvn` çağrılıyor,
  `.mvn/`+`mvnw` kopyalamaya gerek yok. Üç servis de artık aynı desende.

### 5.4 Gateway security: dependency kalır, geçici permitAll
- `spring-boot-starter-security` dependency yerinde; şimdilik her isteğe izin veren `SecurityFilterChain`.
- Gerekçe: JWT doğrulama mekanizması (auth-service) henüz yok. permitAll ile 401 duvarı kalkıp routing test edilebiliyor.
- İleride bu sınıfın içi JWT doğrulama filtresine dönüştürülecek (dependency zaten yerinde, geçiş kolay).

### 5.5 Gateway route/timeout/instance ayarları
- Route prefix'i **WebMVC**'ye özel: `spring.cloud.gateway.server.webmvc.routes` (bkz. §6).
- Timeout **WebMVC**'de `spring.http.client.*` ile (WebFlux'un `spring.cloud.gateway.httpclient.*`'ı ÇALIŞMAZ).
- `lb://` şeması için `spring-cloud-starter-loadbalancer` açıkça pom'a eklendi (eureka-client transitive getirse de niyet belgelensin diye).
- Eureka instance ayarları container uyumu için (IP tabanlı kayıt + benzersiz id).

### 5.6 CORS ertelendi
- Henüz frontend yok → CORS eklense de test edilemez. Frontend gelince gerçek origin ile eklenecek.

### 5.7 docker-compose vendor-neutral (Rancher + Podman)
- `version:` satırı yok (modern Compose Spec; ikisi de destekler).
- Container ağında servisler birbirini **servis adıyla** bulur; bu yüzden gateway'in Eureka adresi compose'da
  env ile `http://discovery-server:8761/eureka` olarak ezilir (application.yml'deki `localhost` sadece IDE içindir).
- **Healthcheck artık VAR** (bkz. §5.10) — önceki not ("healthcheck yok, sadece depends_on sıralaması yeterli")
  yanlış çıktı: gerçek bir çalıştırmada config-server'ın diğer üç servisten milisaniyeler sonra hazır olması,
  hepsinin `localhost` varsayılanlarına düşüp kilitlenmesine yol açtı (§5.10'da detaylı anlatılıyor).

### 5.8 config-server: native profile + classpath (git-backed DEĞİL)
- İhtiyaç: her servisin `application.yml`'inde tekrarlanan/dağınık ayarları (eureka adresi, route'lar, gelecekte
  DB bağlantısı) tek yerden yönetmek; auth-service'in datasource'unu doldururken bunu **iki kere** yazmamak
  (önce yerele, sonra config-server'a taşımak yerine baştan config-server'da tanımlamak — bkz. §9.1'de tercih edilen plan).
- **Seçim:** `spring.profiles.active: native` + `spring.cloud.config.server.native.search-locations: classpath:/config-repo/`.
  Yani config dosyaları **ayrı/dış bir git deposu değil**, `config-server` modülünün kendi
  `src/main/resources/config-repo/` klasöründe duruyor ve servisin jar'ına gömülüyor.
- **Alternatif reddedildi:** git-backed config-repo (ayrı repo veya `file:` ile harici bir git klasörü) — bu proje
  tek geliştiricili ve erken/dev aşamada olduğu için ekstra bir git deposu yönetmenin getirisi yok; ileride
  gerçek ortam/sır yönetimi gerektiğinde git-backed'e geçiş yolu açık bırakıldı.
- **Trade-off (bilinçli kabul edildi):** classpath kaynaklı olduğu için config değişikliği config-server'ın
  rebuild + restart edilmesini gerektiriyor; git-backed'in sağladığı canlı `/actuator/refresh` yok.
- **Client tarafı:** üç istemci servise de (`discovery-server`, `api-gateway`, `auth-service`) `spring-cloud-starter-config`
  bağımlılığı eklendi (bu olmadan `spring.config.import: configserver:...` sessizce hiçbir şey yapmaz).
  Her birinin yerel `application.yml`'i artık sadece `spring.application.name` + `spring.config.import` içeriyor;
  gerçek ayarlar `config-server/src/main/resources/config-repo/<servis-adı>.yml` dosyalarında.
- **`optional:` öneki:** `spring.config.import: "optional:configserver:http://localhost:8888"` — config-server
  ayakta değilse servis yine de başlamayı dener (sert bağımlılık değil), ama pratikte doğru ayarları almak için
  config-server'ın **ilk sırada** ayakta olması gerekiyor (bkz. §7 başlatma sırası).
- **Başlatma sırası değişti:** artık `config-server → discovery-server → api-gateway`. Docker Compose'da
  `depends_on` bu sırayı yansıtacak şekilde güncellendi; container ağında istemciler config-server'a
  `http://config-server:8888` ile ulaşıyor (env override, aynı `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` deseninde).

### 5.10 Docker Compose healthcheck — `depends_on` tek başına yetmiyordu (gerçek arıza, teorik değil)
- **Sorun (gerçekte yaşandı):** `depends_on` (healthcheck'siz haliyle) sadece container'ın **başlatılmasını**
  garanti ediyor, içindeki Spring Boot uygulamasının portu **dinlemeye başlamasını** değil. `customer-service`
  entegrasyonunu Docker Compose'da test ederken, `config-server` diğer üç servisten (discovery-server,
  api-gateway, customer-service) birkaç saniye geç ayağa kalktığı için, üçü de `spring.config.import`
  ile config-server'a **tek seferlik** (retry'sız — `spring.cloud.config.retry.*`/`fail-fast` tanımlı değil)
  bağlanmayı denedi, "Connection refused" aldı ve **process'in tüm ömrü boyunca** Spring Cloud Netflix'in
  hard-coded varsayılanına (`eureka.client.service-url.defaultZone=http://localhost:8761/eureka`) düştü —
  bizim `discovery-server:8761` env override'ımızı hiç görmeden. Sonuç: `discovery-server` kendi kendine
  `localhost:8761`'e register olmaya çalışıyor, `api-gateway`/`customer-service` de aynı şekilde — sürekli
  "Cannot execute request on any known server" hatasıyla sonsuz döngü.
- **Neden `docker compose restart <servis>` geçici olarak işe yaradı:** config-server o ana kadar zaten
  tam ayağa kalkmış oluyor; yeniden başlatılan servis bu sefer config-server hazırken deniyor ve doğru
  ayarları alıyor. Ama bu kalıcı bir çözüm değil, her `up` sonrası manuel restart gerektirirdi.
- **Kalıcı çözüm (uygulandı):** `config-server`, `discovery-server`, `postgres`'e Docker Compose `healthcheck`
  eklendi; bunlara bağımlı servislerin `depends_on`'u `condition: service_healthy` ile **gerçekten hazır
  olmayı bekleyecek** şekilde güncellendi.
  - JVM servisleri (config-server, discovery-server, api-gateway, customer-service) için healthcheck:
    `bash -c '</dev/tcp/127.0.0.1/<port>'` — runtime image'lar (`eclipse-temurin:*-jre`, alpine değil) `curl`/
    `wget` içermiyor, ama Debian tabanlı oldukları için `bash` var; `/dev/tcp` bash built-in'i ekstra paket
    kurmadan basit bir TCP-port-açık-mı kontrolü sağlıyor.
  - `postgres` için: image'da zaten hazır gelen `pg_isready` komutu kullanıldı, ekstra bir şey gerekmedi.
- **Alternatif reddedildi:** `spring.cloud.config.fail-fast: true` + retry ayarları (uygulama tarafında
  config-server'a karşı retry) — bu da işe yarardı ama her istemci servise ayrı ayrı config eklemek
  gerekirdi; Compose seviyesinde tek yerden çözmek (healthcheck) daha az tekrar gerektiriyor.

### 5.11 Spring Boot 4: Flyway'in kendi autoconfigürasyon modülü var, `flyway-core` yeterli değil
- **Sorun (gerçekte yaşandı):** `flyway-core` + `flyway-database-postgresql` pom'da olmasına, config'in
  `spring.flyway.enabled: true` demesine ve Postgres bağlantısının sorunsuz çalışmasına rağmen, Flyway
  migration'ları **hiç çalışmadı** — loglarda "Flyway" kelimesi bile geçmiyordu, Hibernate doğrudan boş bir
  şemaya karşı `ddl-auto: validate` yapıp "missing table [customers]" hatasıyla çöktü.
- **Teşhis:** `--debug` ile alınan Spring Boot condition-evaluation raporunda `FlywayAutoConfiguration`
  hiç görünmüyordu (ne eşleşenler ne eşleşmeyenler arasında) — yani sınıf Spring Boot tarafından hiç
  taranmıyordu. Sebep: Spring Boot 4, autoconfigürasyonu (§ örn. `HibernateJpaConfiguration`'ın
  `org.springframework.boot.hibernate.autoconfigure`'a taşınması gibi) küçük, özellik bazlı modüllere böldü;
  Flyway'in autoconfigürasyonu da artık `org.springframework.boot:spring-boot-flyway` adlı **ayrı** bir
  modülde — `flyway-core` sadece Flyway'in çekirdek motorunu getiriyor, Spring entegrasyonunu değil.
- **Çözüm:** `customer-service/pom.xml`'e açıkça `org.springframework.boot:spring-boot-flyway` bağımlılığı
  eklendi (versiyon yok, Spring Boot parent BOM'undan miras alınıyor, 4.1.0).
- **Etkilenen diğer servisler:** `auth-service`'in `pom.xml`'inde de `flyway-core`/`flyway-database-postgresql`
  var ama `spring-boot-flyway` **yok** — auth-service'in Postgres/migration'ları kurulduğunda aynı sessiz
  hatayla karşılaşılacak, aynı düzeltme oraya da taşınmalı (bkz. §9.2).

### 5.12 `SELECT DISTINCT` + `ORDER BY` join kolonuna göre → Postgres hatası (arama endpoint'i)
- **Sorun (gerçekte yaşandı):** `GET /api/customers/search?firstName=ali` **500** dönüyordu.
  `GlobalExceptionHandler`'a eklenen loglama sayesinde (bkz. §5.13) gerçek sebep görüldü:
  `ERROR: for SELECT DISTINCT, ORDER BY expressions must appear in select list`.
- **Sebep:** `CustomerSpecifications.search()` içinde `query.distinct(true)` vardı ("mükerrer müşteri
  dönmesin" gereksinimi için, önlem amaçlı eklenmişti); ama `CustomerController`'daki `Sort`,
  `partyRole.party.individual.firstName/lastName` (join'lenmiş `individuals` tablosunun kolonları) üzerinden
  sıralıyor. Postgres, `SELECT DISTINCT` ile birlikte `SELECT` listesinde olmayan bir kolona göre
  `ORDER BY` yapılmasına izin vermiyor.
- **Çözüm:** `distinct(true)` kaldırıldı. Zaten gereksizdi: `Customer → PartyRole → Party → Individual → Role`
  zincirindeki tüm ilişkiler `@OneToOne`/`@ManyToOne` (1-1) — koleksiyon join'i olmadığı için bu sorgu hiçbir
  zaman fiziksel olarak mükerrer satır üretemez, `DISTINCT` başından beri no-op'tu.
- **Ders:** `DISTINCT` + join'lenmiş bir tabloya göre `ORDER BY` kombinasyonu genel bir SQL tuzağı —
  ileride yeni bir arama/listeleme endpoint'i yazılırken tekrar hatırlanmalı.

### 5.13 GlobalExceptionHandler'ın genel `catch-all`'ı hatayı hiçbir yere loglamıyordu
- **Sorun:** `@ExceptionHandler(Exception.class)` metodu sadece generic bir `ErrorResponse` dönüyordu,
  `logger.error(...)` çağrısı yoktu — yani beklenmeyen bir hata olduğunda ne response'ta ne konsol
  loglarında **hiçbir zaman** gerçek stack trace görünmüyordu. §5.12'deki hatayı bulmak bu yüzden
  zorlaştı (ilk denemede loglar tamamen sessizdi).
- **Çözüm:** `handleUnexpected` metoduna `log.error("Unexpected error handling {} {}", method, uri, ex)`
  eklendi — artık her beklenmeyen hata, tam stack trace'iyle konsola basılıyor.

### 5.9 Lombok: `annotationProcessorPaths` açıkça tanımlanmalı (JDK 25 tuzağı)
- **Sorun:** `customer-service` yazılırken derleme, Lombok'un ürettiği **hiçbir** getter/setter/builder'ı
  bulamadı (`cannot find symbol`, `variable X not initialized in the default constructor` gibi hatalar).
  Standalone `javac -processorpath lombok.jar` ile aynı kod sorunsuz derlendi — yani Lombok kütüphanesinin
  kendisi JDK 25 ile uyumlu, ama **Maven Compiler Plugin, `annotationProcessorPaths` açıkça verilmediğinde
  artık `-classpath`'teki processor'leri otomatik keşfetmiyor** (önceki javac sürümlerinin varsayılan
  davranışının aksine).
- **Çözüm (seçilen):** `customer-service/pom.xml`'e `maven-compiler-plugin` için açık
  `annotationProcessorPaths` (Lombok, `${lombok.version}` — Spring Boot parent'tan miras alınıyor) eklendi.
- **Etkilenen diğer servisler:** `auth-service`'in `pom.xml`'inde de Lombok bağımlılığı var ama tüm sınıfları
  boş olduğu için bu hata hiç tetiklenmedi. **auth-service'e gerçek kod yazılırken bu düzeltme oraya da
  taşınmalı** — aksi halde aynı "cannot find symbol" hatalarıyla karşılaşılır. `discovery-server` ve
  `api-gateway` Lombok kullanmıyor, etkilenmiyor.
- **Neden root POM'a değil, sadece customer-service'e eklendi:** Görev kapsamı customer-service ile sınırlı
  tutuldu ("mevcut çalışan servisleri gereksiz yere değiştirme" ilkesi); ama bu aslında **proje çapında bir
  yapılandırma boşluğu** — kök `pom.xml`'in `<pluginManagement>`'ına taşınması, her yeni Lombok kullanan
  servisin bunu tekrar tekrar eklemek zorunda kalmaması için mantıklı bir sonraki adım (§9'a eklendi).

### 5.14 nationalityId: global DB UNIQUE kaldırıldı, ACTIVE-only kural sadece uygulama katmanında
- **Sorun:** `individuals.nationality_id` DB'de global `UNIQUE`'ti ama iş kuralı sadece "ACTIVE müşteriler
  arasında tekil" istiyordu. Bu iki kural örtüşmüyordu: bir müşteri soft-delete (PASSIVE) edildikten sonra
  aynı nationalityId ile yeni bir aktif müşteri oluşturulmaya çalışılırsa, uygulama katmanı bunu engellemiyordu
  (sadece ACTIVE'lere bakıyordu) ama DB'deki eski PASSIVE satır global UNIQUE'e hâlâ takılıyor, ham bir
  `DataIntegrityViolationException` → genel 500'e düşüyordu.
- **Seçenekler:** (A) DB'deki UNIQUE'i kaldır, tekilliği tamamen uygulama katmanında bırak. (B) Cross-table
  partial unique index (customers/party_roles/parties'teki status'e bakan) ekle.
- **Seçilen: (A).** Status alanı `individuals` tablosunda değil `customers`/`party_roles`/`parties`'te
  olduğu için, (B) bu şemada join gerektiren bir partial index anlamına gelir — Postgres'te doğrudan
  desteklenmez (partial index'in WHERE koşulu sadece kendi tablosundaki kolonlara bakabilir), bu yüzden
  (B) ya denormalize edilmiş bir "is_active" kolonu individuals'a eklemeyi ya da trigger tabanlı bir kısıtı
  gerektirirdi — bu aşama için gereksiz karmaşıklık. (A) daha basit ve zaten var olan uygulama kontrolüyle
  (`checkNationalityIdIsUniqueForCreate/ForUpdate`) tam örtüşüyor.
- **Uygulama:** `V1__create_customer_tables.sql`'deki `UNIQUE` kaldırıldı (yeni bir V3 migration yerine V1
  doğrudan değiştirildi — bu servis henüz merge/push edilmemişken, local/dev aşamasında kabul edilebilir bir
  kısayol; bkz. docs/customer-service.md'deki "developers must reset customer_db" notu).
  `Individual` entity'sindeki `@Column(unique = true)` da kaldırıldı.
- **Ek güvence:** `GlobalExceptionHandler`'a bir `DataIntegrityViolationException` handler'ı eklendi (409 +
  `MSG-CUST-DUP-NATID`) — uygulama kontrolünü aşan bir yarış durumu (iki eşzamanlı istek) hâlâ DB'de
  `roles.code`/`individuals.party_id`/`customers.party_role_id` gibi kalan başka UNIQUE kısıtlara takılırsa,
  bu da genel 500 yerine temiz bir yanıt döner.
- **Doğrulama:** Aynı nationalityId ile oluştur → soft-delete → aynı nationalityId ile tekrar oluştur akışı
  gerçek bir Postgres'e karşı elle test edildi, artık 201 dönüyor (öncesinde global UNIQUE'e takılırdı).

### 5.15 Türkçe karakter "Malformed request body" hatası: sunucu değil, shell/argv encoding sorunu
- **İlk şüphe (yanlış çıktı):** `Gender.java`'nın `com.fasterxml.jackson.annotation` importu kullanması ve
  `JacksonConfig`'in Jackson 3 (`tools.jackson.*`) API'sini kullanması karışık görünüyordu. **Doğrulandı ki
  bu bir sorun değil** — Jackson 3, `jackson-annotations` modülünü (dolayısıyla `@JsonValue`/`@JsonCreator`)
  bilinçli olarak `com.fasterxml.jackson.annotation` paketinde bıraktı; `tools.jackson.databind`'in Jackson 3
  ObjectMapper'ı bu anotasyonları sorunsuz tanıyor.
- **Gerçek kök neden (canlı sunucuya karşı doğrulandı):** Turkçe karakterli bir `curl -d '...'` isteği,
  hem gateway üzerinden hem doğrudan customer-service'e karşı **her zaman** "Malformed request body" +
  400 dönüyordu — ama tamamen ASCII bir istek sorunsuz çalışıyordu. Kök neden loglaması eklenip
  (`GlobalExceptionHandler.handleUnreadableBody` artık `log.warn(..., ex)` yapıyor) sunucu yeniden
  başlatıldığında gerçek istisna görüldü: `tools.jackson.core.exc.StreamReadException: Invalid UTF-8 middle
  byte 0x7a`. `curl -v --trace-ascii` ile tel üzerindeki baytlar incelendiğinde, "ö" (UTF-8'de 2 bayt,
  `0xC3 0xB6`) **tek, geçersiz bir bayta** dönüşmüş olarak gönderildiği görüldü — yani JSON, curl'e ulaşmadan
  ÖNCE bozulmuştu.
- **Neden:** Bu ortamdaki `curl` (`/mingw64/bin/curl`, `x86_64-w64-mingw32` derlemesi) native bir Win32
  programı. Git Bash, bu tür bir programı çağırırken komut satırı argümanlarını kendi iç UTF-8 temsilinden
  Win32 CRT'nin beklediği "multi-byte" temsile (aktif ANSI kod sayfası — `chcp` ile görülür, bu ortamda 437)
  çevirmek zorunda; `LANG`/`LC_ALL` da boştu. Bu çeviri UTF-8 çok baytlı bir diziyi doğru taşıyamıyor.
  **`--data-binary @-` ile heredoc/stdin kullanmak** bu sorunu tamamen atlıyor (veri argv üzerinden değil,
  bash'in kendi UTF-8 farkında stdin akışından geçiyor) — canlı sunucuya karşı doğrulandı, "Gözek" sorunsuz
  round-trip etti. Postman gibi GUI araçları da argv'den geçmediği için hiç etkilenmiyor.
- **Yapılan kod değişikliği:** Sadece `GlobalExceptionHandler`'a kök nedeni loglayan bir satır eklendi;
  ayrıca (ilgisiz ama aynı handler'da bulunan) geçersiz enum değerleri (`gender: "Unknown"` gibi) artık genel
  "Malformed request body" yerine temiz bir 400 + `validationErrors` döner. `JacksonConfig`/`Gender`'da
  hiçbir değişiklik gerekmedi — ikisi de zaten doğruydu.
- **Ders:** "Malformed request body" + Türkçe karakter kombinasyonu görülünce önce sunucu kodundan şüphelenmek
  yanlış bir refleksti; gerçek kanıt (raw byte inceleme + kök neden loglaması) olmadan kod "düzeltmeye"
  çalışmak, çalışan kodu bozma riski taşırdı.

---

## 6. WebMVC vs WebFlux Tuzağı (DİKKAT)

Bu gateway **WebMVC** (servlet) stack'idir. İnternetteki Spring Cloud Gateway örneklerinin çoğu **WebFlux** (reactive)
içindir ve **birebir kopyalamak hatalıdır**. Doğrulanmış farklar:

| Konu | WebFlux (YANLIŞ, bizde çalışmaz) | WebMVC (DOĞRU, bizim kullandığımız) |
|---|---|---|
| Route prefix | `spring.cloud.gateway.routes` | `spring.cloud.gateway.server.webmvc.routes` |
| Eski MVC prefix | — | `spring.cloud.gateway.mvc.routes` **deprecated**, kullanma |
| Timeout | `spring.cloud.gateway.httpclient.*` | `spring.http.client.connect-timeout` / `read-timeout` |
| Security bean | `SecurityWebFilterChain` + `ServerHttpSecurity` | `SecurityFilterChain` + `HttpSecurity` (servlet) |
| Route listeleme | `/actuator/gateway/routes` (var) | **YOK** — route'lar `RouterFunction`, `/actuator/mappings`'te görünür |

---

## 7. Nasıl Çalıştırılır

### 7.1 IDE (mevcut ana yöntem)
Sıra önemli (config-server artık ilk sırada; customer-service Postgres gerektirir):
1. **config-server** → `ConfigServerApplication` Run. Doğrula: `http://localhost:8888/discovery-server/default`
   (Config Server'ın REST API'si, `discovery-server.yml` içeriğini JSON olarak döner).
2. **discovery-server** → `DiscoveryServerApplication` Run. Doğrula: `http://localhost:8761`.
3. **api-gateway** → `ApiGatewayApplication` Run. Doğrula: Eureka'da `API-GATEWAY` görünür.
4. **Postgres** → IDE'den değil, Podman/Rancher ile ayağa kaldır (bkz. §7.2'nin postgres kısmı) — `customer-service`
   için `localhost:5432/customer_db` gerekli (`crmlite`/`crmlite`).
5. **customer-service** → `CustomerServiceApplication` Run. Doğrula: Eureka'da `CUSTOMER-SERVICE` görünür,
   `GET http://localhost:8082/actuator/health` → `UP`, Flyway loglarında `V1`/`V2` migration'ları uygulandığı görünür.
6. auth-service → **şu an çalıştırılamaz** (Postgres/kod eksik).

> IDE'de Maven projeleri görünmüyorsa: kök `pom.xml`'i "Add as Maven Project" / "Load Maven Project" ile ekle;
> beş modülü otomatik tanır.

### 7.1b Terminal / Maven (aynı sıra, ayrı terminallerde)
```bash
mvn clean install -DskipTests   # tek seferlik build

# Terminal 1
mvn -pl backend/config-server spring-boot:run
# Terminal 2
mvn -pl backend/discovery-server spring-boot:run
# Terminal 3
mvn -pl backend/api-gateway spring-boot:run

# Postgres (repo kökünden):
docker compose -f infra/docker-compose.yml up -d postgres
# Podman: podman compose -f infra/docker-compose.yml up -d postgres

# Terminal 4 (Postgres ayaktayken)
mvn -pl backend/customer-service spring-boot:run
```
Detaylı curl doğrulama örnekleri için bkz. docs/customer-service.md.

### 7.2 Docker / Podman (infra/docker-compose.yml)
Repo kökünden:
```bash
# Rancher Desktop (dockerd):
docker compose -f infra/docker-compose.yml up --build

# Podman:
podman compose -f infra/docker-compose.yml up --build
# veya: podman-compose -f infra/docker-compose.yml up --build
```
İlk build birkaç dakika sürer (image + bağımlılık indirir + Postgres init script'i `customer_db`'yi oluşturur).
Durdurma: aynı komut `down` ile. **Postgres verisini de silmek için:** `down -v` (volume'u da kaldırır).
Not: compose artık `config-server`, `discovery-server`, `api-gateway`, `postgres`, `customer-service` içerir
(`auth-service` henüz yok — Postgres bağımlılığı DB kurulana kadar eklenmeyecek).

**⚠️ Flyway/schema değişikliği sonrası (örn. bu PR'daki `nationality_id` UNIQUE kaldırma, bkz. §5.14):**
V1 migration'ı doğrudan değiştiren bir güncelleme çektiyseniz, daha önce kurulmuş bir `customer_db` Flyway'in
checksum doğrulamasını geçemez. Volume'u sıfırlayın:
```bash
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d postgres
# Podman eşleniği:
podman compose -f infra/docker-compose.yml down -v
podman compose -f infra/docker-compose.yml up -d postgres
```
ardından customer-service'i yeniden başlatın (Flyway V1/V2'yi temiz şemaya uygular).

---

## 8. Doğrulama (Smoke Test)

config-server + discovery-server + api-gateway + postgres + customer-service ayaktayken:
- `GET http://localhost:8888/discovery-server/default` → `discovery-server.yml` içeriği JSON olarak döner (config-server çalışıyor).
- `GET http://localhost:8888/customer-service/default` → `customer-service.yml` içeriği döner.
- `GET http://localhost:8761` → Eureka dashboard, `API-GATEWAY` ve `CUSTOMER-SERVICE` kayıtlı.
- `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`.
- `GET http://localhost:8080/api/auth/login` → **503** temiz JSON (`messageKey: MSG-SERVICE-UNAVAILABLE`) —
  auth-service ayakta değilken beklenen; route çalışıyor ve `GatewayExceptionHandler` (§4.2) doğru status'u
  yansıtıyor demek. (Fix öncesi bu generic 500'dü — bkz. §4.2.)
- `GET http://localhost:8080/api/customers?firstName=Ali` → gateway üzerinden customer-service'e ulaşır,
  seed verideki iki "Ali" kaydını döner (canonical endpoint; `GET .../api/customers/search?firstName=Ali`
  aynı sonucu veren legacy alias'tır — bkz. docs/customer-service.md için tam örnek istekler).

---

## 9. Sırada Ne Var (Roadmap / Öncelik)

### 9.1 Bir sonraki büyük adım — customer-service çekirdeği KURULDU ✅
`customer-service` artık ayakta (§4.4): FR-CUST-01..05 çekirdek implementasyonu, Postgres + Flyway ile.
Kalan büyük engeller: **auth-service'in DB kurulumu** (değişmedi) ve **address-service/contact-service**
(customer-service'in FR-CUST-03'ü tam anlamıyla tamamlaması için gerekli, henüz yok).

### 9.2 auth-service'i tamamlama (blokör işler)
- [ ] PostgreSQL zaten compose'da mevcut (`customer-service` için eklendi) — auth-service aynı Postgres
  container'ında `auth_db` adında ikinci bir database olarak eklenebilir (`infra/postgres/init/`'e yeni satır).
- [ ] `config-repo/auth-service.yml`'deki datasource'u doldur (url/username/password).
- [ ] Flyway migration(lar)ı yaz (`src/main/resources/db/migration/V1__init.sql` ...). Şu an klasör boş.
- [ ] Boş iskelet sınıfları implement et (login/security/session/common: entity, repository, service, controller, dto).
- [ ] JWT üretimi/doğrulaması (jjwt bağımlılıkları hazır).
- [ ] auth-service'i `infra/docker-compose.yml`'e ekle (root-context Dockerfile'ı zaten hazır).
- [ ] **Lombok anotasyonu kullanacaksa** `pom.xml`'e customer-service'teki `annotationProcessorPaths`
  düzeltmesini kopyala (bkz. §5.9) — aksi halde "cannot find symbol" hatalarıyla karşılaşılır.
- [ ] **`pom.xml`'e `org.springframework.boot:spring-boot-flyway` bağımlılığını ekle** (bkz. §5.11) —
  `flyway-core`/`flyway-database-postgresql` zaten pom'da ama bu tek başına yetmiyor; olmadan migration'lar
  sessizce hiç çalışmaz, Hibernate boş şemaya karşı "missing table" hatasıyla çöker.

### 9.3 customer-service'i tamamlama (bir sonraki iterasyon)
- [ ] **address-service + contact-service** kurulunca FR-CUST-03'ü tam orkestrasyona genişlet (şu an sadece
  PARTY/INDIVIDUAL/PARTY_ROLE/CUSTOMER oluşturuyor, ADDR/CNTC_MEDIUM hariç — kodda TODO yorumları mevcut).
- [ ] **account-service/contact-service/order-service** kurulunca `checkNoUnsupportedCrossServiceSearchCriterion`'ı
  kaldırıp accountNumber/gsmNumber/orderNumber aramasını gerçek entegrasyona çevir (şu an 501 dönüyor).
- [ ] **product-service/account-service** kurulunca `checkCustomerHasNoActiveProducts`'ı gerçek bir çağrıya çevir
  (şu an TODO/no-op).
- [x] ~~`nationality_id` global UNIQUE kısıtının "sadece ACTIVE arasında unique" iş kuralıyla tam örtüşmemesi~~
  — **çözüldü** (bkz. §4.4, §5.14): global UNIQUE kaldırıldı, tekillik tamamen uygulama katmanında.
- [x] ~~Test yok~~ — **eklendi**: `CustomerBusinessRulesTest`, `CustomerSpecificationsTest` (gerçek Postgres'e
  karşı `@DataJpaTest`), `GlobalExceptionHandlerTest` (bkz. §4.4). Testcontainers hâlâ kurulu değil — arama
  testi bunun yerine zaten var olan dev Postgres'i kullanıyor.
- [ ] Arama performansı: prefix (`LIKE 'term%'`) artık `lower()` fonksiyonel index'ten faydalanabiliyor;
  yine de veri hacmi büyürse `pg_trgm` trigram index değerlendirilebilir (şu an gerekli değil).

### 9.4 config-server sertleştirme (ileride)
- [ ] Şu an classpath/native — sırlar (DB şifresi, JWT secret) düz metin olarak jar'a gömülüyor. Gerçek ortam
  öncesi ya Jasypt/Spring Cloud Config encrypt-decrypt endpoint'i ya da git-backed + harici secret yönetimine geçiş değerlendirilmeli.
- [ ] `/actuator/refresh` / Spring Cloud Bus ile canlı config reload (şu an yok — değişiklik = rebuild+restart).

### 9.5 Gateway sıkılaştırma (auth-service hazır olunca)
- [ ] `SecurityConfig`'i permitAll'dan JWT doğrulamaya çevir.
- [ ] CORS ekle (frontend geldiğinde, gerçek origin ile).
- [ ] (İsteğe bağlı) resilience4j circuit breaker — WebMVC blocking model için downstream koruması.

### 9.6 Container/altyapı borçları
- [x] ~~Compose healthcheck'leri~~ — **eklendi** (bkz. §5.10): config-server/discovery-server/postgres için
  healthcheck + bağımlı servislerde `depends_on: condition: service_healthy`.
- [ ] Eureka self-preservation'ı dev için kapatmak (`eureka.server.enable-self-preservation: false`) — opsiyonel.
- [ ] discovery-server, gateway ve customer-service için graceful shutdown, gerekiyorsa auth için de.
- [ ] **Lombok `annotationProcessorPaths` düzeltmesi kök `pom.xml`'in `<pluginManagement>`'ına taşınmalı**
  (bkz. §5.9) — şu an sadece customer-service'te, her yeni Lombok kullanan serviste tekrar eklenmesi gerekiyor.

---

## 10. Bilinen Teknik Borç / Notlar

- **auth-service tamamen iskelet** — çalıştırmayı deneme, Postgres+kod olmadan çökecektir (beklenen).
- **Gateway security şu an açık (permitAll)** — hiçbir kimlik doğrulama yapmıyor; production'a gitmeden JWT'ye çevrilmeli.
- **Makinede `mvn` artık PATH'te** (önceki not güncel değildi) — `docker` ise hâlâ PATH'te değil, Podman/Rancher kullanılıyor.
- **config-server native/classpath, sır yönetimi yok** — `config-repo` dosyaları düz metin olarak jar'a gömülüyor;
  gerçek bir DB şifresi/JWT secret girildiğinde bu düz metin git'e de gidecek demektir (bkz. §9.4).
- **Compose 5 servis** — auth-service henüz eklenmedi (Postgres artık var, ama auth-service'in kendi kodu/migration'ı eksik).
- **customer-service: accountNumber/gsmNumber/orderNumber araması kasıtlı olarak 501 dönüyor** — account/contact/
  order-service entegrasyonu tamamlanana kadar (sessizce yanlış sonuç vermek yerine).
- **customer-service: adres/iletişim orkestrasyonu yok** — `POST /api/customers` sadece PARTY/INDIVIDUAL/
  PARTY_ROLE/CUSTOMER oluşturuyor; address-service/contact-service kurulunca genişletilecek (bkz. §9.3).
- **customer-service: `checkCustomerHasNoActiveProducts` TODO/no-op** — product/account-service kurulunca
  gerçek bir çağrıya çevrilecek (bkz. §4.4, §9.3).
- **Testcontainers hâlâ kurulu değil** — `CustomerSpecificationsTest` gerçek local Postgres'e karşı çalışıyor
  (bkz. §4.4); bu yeterli ama CI'da Postgres'in ayrıca ayakta olmasını gerektiriyor.
- **Lombok, `annotationProcessorPaths` açıkça tanımlanmadan çalışmıyor** (JDK 25 + bu Maven Compiler Plugin
  sürümü) — şu an sadece `customer-service/pom.xml`'de düzeltilmiş durumda (bkz. §5.9, §9.6).

---

## 11. AI Agent İçin Hızlı Başlangıç Notları

- Bu dosyayı ve `§5` (kararlar) + `§6` (WebMVC tuzağı) bölümlerini **kod önermeden önce** oku.
- Gateway ile ilgili herhangi bir şey yaparken WebFlux örneği kopyalama — §6 tablosuna uy.
- Docker/Compose ile ilgili değişiklikte **root build context** kuralını (§5.2) koru.
- Yeni servis = kök POM `<modules>` + parent bağlama + root-context Dockerfile.
- Emin olmadığın bir Spring Cloud property'sini **resmi WebMVC dokümanından doğrula**, tahmin etme.
- Değişiklik yaptıkça bu dosyanın ilgili bölümünü ve §9 listesini güncelle.
