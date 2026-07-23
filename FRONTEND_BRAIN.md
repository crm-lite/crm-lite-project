# FRONTEND_BRAIN — CRM Lite

> **Amaç:** Bu dosya frontend'in **güncel durumunun tek doğru kaynağıdır**
> (single source of truth). `PROJECTBRAIN.md`'nin frontend kardeşidir; backend
> tarafı için o dosya geçerlidir. Hem projeye sonradan dönen geliştirici hem de
> sıfırdan bağlam kuran bir AI agent bu dosyayı okuyarak "nerede kaldık, neden
> böyle yapıldı, sırada ne var" sorularını cevaplayabilmelidir.
>
> **Son güncelleme:** 2026-07-23 (**karar-kayıt sistemi kuruldu:** FE-ADR-001..013
> yazıldı, `docs/frontend/scope-and-conflicts.md` açıldı, sürümler npm
> registry / Node.js release index / Docker Hub üzerinden doğrulanarak pinlendi.
> **Henüz hiç kod yazılmadı — `frontend/` klasörü mevcut değil.**)
>
> **Bu dosyayı güncel tut:** Her anlamlı değişiklikten sonra ilgili bölümü ve
> "Sırada Ne Var" listesini güncelle.

---

## 1. Proje Özeti

CRM Lite'ın Angular tabanlı web arayüzü. Backend'in `api-gateway` BFF'i
(`http://localhost:8080`) üzerinden konuşur; kendi kimlik doğrulaması **yoktur**
— oturum Keycloak + gateway tarafından yönetilir.

**Mevcut durum:** 🚧 **Kod yazılmadı.** Bu aşamada yalnız karar kaydı ve
tasarım referansı mevcut. Bir sonraki adım iskelet kurulumu (§7).

- **Framework:** Angular **22.0.8** (standalone components, NgModule YOK)
- **Dil:** TypeScript **6.0.3** (strict mode, tüm katılık bayrakları açık)
- **Runtime:** Node.js **22.23.1** (LTS "Jod")
- **Stil:** Tailwind CSS **4.3.3** + Etiya EDS Lite token'ları
- **State:** Angular Signals + ince servis katmanı (NgRx YOK)
- **Bileşen kütüphanesi:** **YOK** — EDS bileşenleri kendimiz yazıyoruz
- **Konum:** `frontend/` (bu repo; ayrı repo DEĞİL)
- **Kapsam:** Customer Search + Create Customer + Customer Info
  (demografik/adres/iletişim). Satış/hesap/ürün ekranları kapsam dışı.

**Doküman dili konvansiyonu** (bilinçli, backend'i taklit ediyor):
`FRONTEND_BRAIN.md` ve `docs/frontend/*.md` **Türkçe** (PROJECTBRAIN ile aynı);
`docs/frontend/adr/FE-ADR-*.md` **İngilizce** (`docs/architecture/adr/` ile aynı,
çünkü sürekli ADR-001..012'ye referans veriyorlar ve birlikte okunuyorlar).

---

## 2. Teknoloji Seçimleri (kesin sürümlerle)

> Aşağıdaki her numara **2026-07-23'te** npm registry, Node.js release index ve
> Docker Hub'dan **okundu** — hafızadan yazılmadı. Doğrulama komutları
> FE-ADR-002 §Verification bölümünde.

| Bileşen | **Pinlenen sürüm** | Not |
|---|---|---|
| `@angular/core` ve kardeşleri | **22.0.8** | Doğrulama tarihindeki en güncel Angular |
| `@angular/cli`, `@angular/build`, `@angular/compiler-cli` | **22.0.7** | Tooling framework'ten bağımsız sürümleniyor |
| **TypeScript** | **6.0.3** | ⚠️ **En güncel TS DEĞİL.** `@angular/compiler-cli@22.0.8` → `peerDependencies: { typescript: ">=6.0 <6.1" }`. En güncel TS **7.0.2** ve bu Angular ile **uyumsuz** |
| **Node.js** | **22.23.1** | Angular `engines`: `^22.22.3 \|\| ^24.15.0 \|\| >=26.0.0` → **karşılanıyor**. "Jod" LTS, **maintenance**; EOL **2027-04-30** (FE-ADR-002) |
| `rxjs` | **7.8.2** | Angular peer: `^6.5.3 \|\| ^7.4.0` |
| `tailwindcss` | **4.3.3** | |
| `zone.js` | **YOK** | **Zoneless** çalışılıyor. `@angular/core@22.0.8` → `peerDependenciesMeta: { zone.js: { optional: true } }` (FE-ADR-006 §7) |
| Build image | **`node:22.23.1-alpine`** | Pinlenen Node ile birebir |
| Runtime image | **`nginx:1.30.4-alpine`** | nginx **stable** hattı (çift minor; 1.31.x mainline) |

**Pinleme politikası:** `package.json`'da **kesin sürüm** yazılır (`^` yok, `~`
yok, `latest` asla), `package-lock.json` commit'lenir. Backend'in
`<spring-cloud.version>2025.1.2</spring-cloud.version>` disiplininin aynısı.

> 🔴 **Ortam uyarısı:** Geliştirme makinesinde şu an **Node v23.11.1** kurulu.
> Node 23 tek numaralı (non-LTS) bir hat ve Angular 22'nin **hiçbir** kabul
> aralığını karşılamıyor. Scaffold öncesi **22.23.1** kurulmalı, yoksa `ng`
> çalışmaz. (`scope-and-conflicts.md` §4.3)

### Bilinçli olarak KULLANILMAYANLAR

| Reddedilen | Neden | Karar |
|---|---|---|
| Angular Material / PrimeNG | Kendi görsel dillerini dayatıyorlar; EDS zaten tam tanımlı — sürekli override savaşı olur | FE-ADR-011 §c |
| Headless bileşen kütüphanesi | Çözdüğü problem (combobox/autocomplete/multi-select overlay'leri) mock'ta **yok**; tek `Select` paneli + `DatePicker` için orantısız | FE-ADR-011 §c |
| NgRx / Redux türevleri | Uygulama state'inin ezici çoğunluğu istek-kapsamlı; paylaşılan mutable state 3 kalem. Boilerplate karşılığında hiçbir şey alınmıyor | FE-ADR-006 §4 |
| `@angular/localize` (derleme-zamanı i18n) | Dil başına ayrı bundle → dil değişimi tam sayfa yeniden yükleme → **AC-LANG-01-02 ihlali** | FE-ADR-012 §a |
| Harici i18n kütüphanesi | Katalog küçük ve tam bilinen; signal'lar zaten gereken reaktiviteyi veriyor | FE-ADR-012 §a |
| Template-driven forms | Wizard'ın adımlar-arası/alanlar-arası kuralları ve `FormArray` ile pratik değil; tipli form yok | FE-ADR-007 §1 |
| CORS | Auth tasarımıyla aktif olarak çatışıyor (SameSite=Lax + `X-XSRF-TOKEN` yalnız same-origin) | FE-ADR-004 §4 |
| Ayrı frontend repo'su | Kontrat kayması; atomik revert imkânsız | FE-ADR-001 §3 |
| Hazır tablo bileşeni | Gereken davranış gösterim + zebra + tek link kolonu; sıralama sunucuda sabit | FE-ADR-011 §e |

---

## 3. Klasör Yapısı Planı

> Henüz **oluşturulmadı**. Bu, FE-ADR-003'ün hedef yapısıdır.

```
crm-lite-project/
├── PROJECTBRAIN.md              # backend'in beyni
├── FRONTEND_BRAIN.md            # BU DOSYA
├── backend/                     # DOKUNULMAZ
├── infra/
│   └── docker-compose.yml       # yalnız YENİ servis eklenir (FE-ADR-010 §4)
├── docs/frontend/
│   ├── mock-ui-analysis.md      # ⭐ TASARIM REFERANSI (§6)
│   ├── scope-and-conflicts.md   # kapsam + çelişki kaydı
│   └── adr/
│       └── FE-ADR-001..013.md
└── frontend/                    # ← henüz YOK
    ├── Dockerfile               # multi-stage: node → nginx
    ├── nginx.conf               # SPA fallback + /api,/oauth2,/login,/logout proxy
    ├── proxy.conf.json          # ng serve → :8080
    ├── .nvmrc                   # 22.23.1
    ├── package.json             # kesin sürümler
    └── src/app/
        ├── core/                # singleton'lar; features'a ASLA bakmaz
        │   ├── auth/            # session servisi, interceptor'lar, guard'lar
        │   ├── http/            # hata sınıflandırma interceptor'ı (FE-ADR-008)
        │   ├── i18n/
        │   │   ├── i18n.service.ts
        │   │   └── catalog/
        │   │       ├── messages.ts   # MSG-* (analist + proje-yazımı, işaretli)
        │   │       └── labels.ts     # LBL-* (analist, 21 anahtar)
        │   └── lookup/          # cities/districts önbelleği
        ├── shared/              # core'a ve features'a ASLA bakmaz
        │   ├── ui/              # EDS bileşenleri (7 adet — FE-ADR-011 §d)
        │   │   ├── eds-icon/ eds-button/ eds-icon-button/
        │   │   ├── eds-form-field/ eds-text-input/ eds-select/ eds-date-picker/
        │   │   └── (PasswordInput YOK — FE-ADR-005 §P5)
        │   └── patterns/        # DS'de olmayanlar: modal, toast, tabs,
        │                        # stepper, pagination, status-badge, card
        └── features/
            └── customer/
                ├── search/      # Customer Search  (+ i18n.ts)
                ├── create/      # 3 adımlı wizard  (+ i18n.ts)
                ├── detail/      # 4 sekme → 3 sekme (+ i18n.ts)
                ├── address/     # ALT MODÜL — create ve detail ortak kullanır
                ├── contact/     # ALT MODÜL
                ├── data/        # HTTP istemcisi
                └── model/       # backend kontrat tipleri
```

**İçe aktarma yönü tek yönlüdür:** `features → core | shared`, `core → shared`.
`shared` hiçbir şeye bakmaz. Feature'lar birbirine bakmaz.

**`address/` ve `contact/` neden alt modül?** Backend **ADR-001** adres ve
iletişimi ayrı servis yapmayı bilinçle reddetti — bunlar customer agregatının
iç modülleri. Frontend, backend'in reddettiği sınırı icat etmez. (FE-ADR-003 §3)

---

## 4. Backend Bağlantı Noktaları (özet)

Tam kontrat: `docs/api/customer-service.md`, `docs/api/authentication.md`,
`docs/frontend/mock-ui-analysis.md` §5A.4.

| Konu | Kural |
|---|---|
| **Taban URL** | **YOK.** Tüm çağrılar göreli path (`/api/...`). `environment.ts`'te API host'u bulunmaz (FE-ADR-004 §2) |
| **Proxy'lenen prefix'ler** | `/api`, `/oauth2`, `/login`, `/logout` — hem `ng serve` hem nginx |
| **Oturum** | Tek kaynak `GET /api/session/me`. Token tarayıcıya **hiç gelmiyor** |
| **CSRF** | Angular `HttpClient` varsayılanı yeterli (`XSRF-TOKEN` → `X-XSRF-TOKEN`) |
| **Login** | `/oauth2/authorization/keycloak`'a **tam sayfa yönlendirme** |
| **Logout** | `POST /logout` + CSRF header, tam sayfa navigasyon |
| **Liste** | `GET /api/customers?page=0&size=20`; **`sort` parametresi YOK** (sıralama sunucuda sabit) |
| **Sayfa boyutu** | Varsayılan **20** |
| **Tarih** | Taşımada daima ISO `YYYY-MM-DD`; gösterimde `dd.MM.yyyy` |
| **Hata** | `messageKey` kullanılır; backend'in `message` alanı **kullanıcıya asla gösterilmez** |
| **Detay ≠ liste değil** | Detay endpoint'inde adres/iletişim **yok** → Customer Info 3 ayrı çağrı yapar |

---

## 5. Sırada Ne Var (Roadmap / Öncelik)

### 5.1 Bloke edenler
- [ ] **Node 22.23.1 kurulumu** — makinede v23.11.1 var, Angular 22 ile
      uyumsuz. **Tek kalan bloke edici** (`scope-and-conflicts.md` §4.3)
- [x] ~~**OAuth redirect origin**~~ ✅ **ÇÖZÜLDÜ (23.07.2026)** — gateway'e
      `forward-headers-strategy: framework`; realm'e `:4200` redirect URI +
      webOrigin + post-logout URI; `keycloak-init` her `up`'ta yeniden
      uyguluyor; nginx `$host`'u geçiriyor. Çalışan stack'te doğrulandı —
      PKCE `S256` ve `directAccessGrants=false` korunuyor
      (FE-ADR-004 §Addendum)
- [x] ~~**Frontend host portu**~~ ✅ **4200** (FE-ADR-010 §5)

> ⚠️ **Gateway'in yeni config'i alması için `config-server` ve `api-gateway`
> yeniden build + deploy edilmeli** — `forward-headers-strategy` config-repo'da,
> yani config-server'ın classpath'ine gömülü. Keycloak tarafında `up` yeterli
> (`keycloak-init` her seferinde uyguluyor, mevcut `keycloak_db` dahil).

### 5.2 İskelet
- [ ] `ng new` ile `frontend/` iskeleti (standalone, strict, routing)
- [ ] Kesin sürüm pinleme + `.nvmrc` + `package-lock.json`
- [ ] Tailwind kurulumu + **EDS token'larının tema olarak aktarılması**
      (`mock-ui-analysis.md` §2 tabloları — değer uydurulmaz)
- [ ] `proxy.conf.json` + `core/` iskeleti (session, i18n, hata interceptor'ı)
- [ ] `Page` zarfının gerçek JSON şeklinin teyidi (§4.4)

### 5.3 Bileşen katmanı
- [ ] `shared/ui/` — 7 EDS bileşeni (Icon, FormField, Button, TextInput,
      IconButton, Select, DatePicker)
- [ ] `shared/patterns/` — modal, confirm dialog, toast, tabs, stepper,
      pagination, status badge, card, empty state
- [ ] Her bileşende erişilebilirlik (klavye, odak, ARIA) — FE-ADR-011 §g

### 5.4 Ekranlar (kapsam içi)
- [ ] **Customer Search** — golden path
- [ ] **Create Customer** — 3 adımlı wizard + adres dialog'u
- [ ] **Customer Info** — 3 sekme (hesap sekmesi YOK)

### 5.5 Container
- [ ] `frontend/Dockerfile` (multi-stage) + `nginx.conf`
- [ ] `infra/docker-compose.yml`'e **yalnız yeni servis bloğu** ekle
- [ ] PROJECTBRAIN §10'daki "Compose 8 servis" notunu güncelle

### 5.6 Karar bekleyenler (bloke etmiyor)
- [ ] Per page seçenek listesi (§2.4) · Sidenav 11px etiket (§2.18) ·
      Header rol metninin kaynağı (§2.20) · Sidenav öğelerinin işlevi (§2.21)
- [ ] zone.js vs zoneless (§4.5) · CI wiring (§4.6) ·
      import-boundary lint (§4.7) · `data-testid` lint (§4.8) ·
      katalog bütünlük testi (§4.9)

---

## 6. Tasarım Referansı

> ⭐ **`docs/frontend/mock-ui-analysis.md` — CRM Lite frontend tasarımının TEK
> REFERANS KAYNAĞIDIR.**

Analistlerden gelen mock UI bundle'ından
(`docs/source/mock-ui/Guncel_Etiya_CRM_Lite_Full_App.html`) çıkarılıp
**2026-07-23'te doğrulanmıştır** (bundle açıldı, 7 ekran + token CSS'leri +
EDS bileşen bundle'ı incelendi).

**Bağlayıcı kurallar:**

- Renk, boşluk, tipografi, yarıçap, elevation, motion ve layout değerleri
  **oradaki tablolardan** alınır. Değer **uydurulmaz**, mock HTML'inden yeniden
  yorumlanmaz.
- Mock HTML'inin **satır içi CSS fallback'lerinin bir kısmı gerçek token
  değerleriyle uyuşmuyor** (`var(--eds-space-10, 48px)` ama token 40px;
  `--eds-type-title-size` diye bir token hiç yok). Daima **token dosyası
  değeri** esastır — §0'daki düzeltme tablosu.
- Ekran düzeni, tablo kolonları, buton yerleşimi, doğrulama mesajları ve boş
  durum metinleri §6'daki ekran detaylarından alınır.
- Bir değer yanlışsa **önce `mock-ui-analysis.md` düzeltilir**, sonra kod yazılır.
- **Mock görsel referanstır, veri sözleşmesi değildir.** Alan adı, tip, format
  veya arama semantiği çakışmasında **backend kontratı kazanır** (§5A,
  FE-ADR-013 §e).
- Turuncu zemin üzerine **asla beyaz metin** yazılmaz
  (`--eds-color-text-on-brand` = `#242441`).
- Bileşenler yalnız **semantic** token (`--eds-color-*`) tüketir; primitive
  paletler (`--eds-orange-*`, `--eds-ink-*`) doğrudan kullanılmaz.
- **z-index literal'i yasak** — yalnız `--eds-z-*` token'ları (§2.15).
- **`data-testid` her etkileşimli elemanda zorunludur**; mock'ta olmaması
  gerekçe değildir (§8, FE-ADR-009).
- **Kullanıcıya görünen hiçbir metin şablona gömülmez** — tamamı i18n
  kataloğundan gelir (§7A, FE-ADR-012).

---

## 7. Karar Kaydı (FE-ADR'ler)

Bağlayıcı frontend kararları `docs/frontend/adr/` altındadır. Bunlarla çelişen
her metin geçersizdir.

| ADR | Konu |
|---|---|
| **FE-ADR-001** | Frontend bu repo'da, `frontend/` altında; ayrı repo değil, Maven modülü değil |
| **FE-ADR-002** | Standalone components, TS strict, kesin sürüm pinleme |
| **FE-ADR-003** | Feature-bazlı mimari + `core/` + `shared/`; adres/iletişim alt modül |
| **FE-ADR-004** | Same-origin + göreli path; CORS bilinçli reddedildi |
| **FE-ADR-005** | BFF cookie-session; **Angular'da login formu YAZILMAZ** (yasaklar bölümü) |
| **FE-ADR-006** | Signals + ince servis katmanı; NgRx reddedildi |
| **FE-ADR-007** | Reactive Forms; istemci validasyonu yalnız UX, otorite backend |
| **FE-ADR-008** | `messageKey` kontrat; backend `message` alanı asla basılmaz |
| **FE-ADR-009** | `data-testid` konvansiyonu |
| **FE-ADR-010** | Multi-stage container; compose'a **yalnız ekleme** |
| **FE-ADR-011** | Tailwind + gerçek EDS token'ları; bileşen kütüphanesi yok |
| **FE-ADR-012** | Runtime i18n; iki katalog, tek çatı |
| **FE-ADR-013** | Kapsam yönetimi: mock backend'den geniş |

**Kapsam ve çelişki kaydı:** `docs/frontend/scope-and-conflicts.md` — backend'in
`document-delta.md` disiplininin karşılığı. Her satır bir durum etiketi taşır
(karar bekliyor / analiste soruldu / karara bağlandı).

---

## 8. AI Agent İçin Kurallar

> Kod önermeden **önce** bu bölümü ve §6'yı oku.

### 8.1 Önce oku
1. `FRONTEND_BRAIN.md` (bu dosya)
2. `docs/frontend/mock-ui-analysis.md` — **her tasarım değeri buradan**
3. `docs/frontend/adr/FE-ADR-*.md` — bağlayıcı kararlar
4. `docs/frontend/scope-and-conflicts.md` — neyin kapsam dışı olduğu
5. Backend kontratı: `docs/api/authentication.md`,
   `docs/api/customer-service.md`, `docs/architecture/adr/ADR-005..011`

### 8.2 Mutlak yasaklar
- ❌ **Login formu yazma.** Kullanıcı adı/şifre alanı, login route'u, login
  component'i YOK. Kimlik girişi yalnız Keycloak sayfasında (FE-ADR-005 §P1).
- ❌ **Token saklama/okuma/parse etme.** `localStorage`/`sessionStorage`'a token
  yazma, JWT decode etme, `Authorization: Bearer` header'ı set etme — hepsi
  yasak. Tarayıcıda token zaten yok (FE-ADR-005 §P3).
- ❌ **ROPC / Direct Grant** — testte, script'te, Postman'da bile (FE-ADR-005 §P2).
- ❌ **Keyfi hex renk veya keyfi piksel değeri.** Yalnız tanımlı token'lar.
  `#3B82F6`, `p-[13px]`, `w-[137px]` yasak (FE-ADR-011 §f).
- ❌ **z-index sayısal literal'i.** Yalnız `--eds-z-*` (mock-ui-analysis §2.15).
- ❌ **Şablona gömülü kullanıcı metni.** Her metin çeviri anahtarından
  (FE-ADR-012 §b).
- ❌ **`data-testid`siz etkileşimli eleman** (FE-ADR-009 §1).
- ❌ **Backend'in `message` alanını kullanıcıya gösterme** (FE-ADR-008 §2).
- ❌ **Backend'i olmayan işlev için sahte veri/placeholder** (FE-ADR-013 §a).
- ❌ **`infra/docker-compose.yml`'de mevcut satırları değiştirme.** Yalnız yeni
  servis bloğu eklenir (FE-ADR-010 §4).
- ❌ **`backend/` altında değişiklik.** Frontend görevlerinde backend'e dokunma;
  bulgu varsa `scope-and-conflicts.md` §5'e yaz.
- ❌ **Otomatik commit/push** (CLAUDE.md).

### 8.3 Zorunlular
- ✅ Tasarım değeri gerekince **mock HTML'ini yeniden yorumlama** —
  `mock-ui-analysis.md` tablolarından al.
- ✅ Mock ile backend çakışırsa **backend kazanır**; çakışmayı
  `scope-and-conflicts.md`'ye yaz.
- ✅ Yeni bir karar/çelişki çıkarsa **önce kayıt, sonra kod**.
- ✅ Standalone component yaz; NgModule üretme.
- ✅ Reactive Forms kullan; template-driven üretme.
- ✅ State için signal; `BehaviorSubject` ile state tutma.
- ✅ Göreli path kullan; base URL sabiti oluşturma.
- ✅ Her `shared/ui/` bileşeninde klavye + odak + ARIA (FE-ADR-011 §g).
- ✅ Sürüm eklerken **kesin numara** yaz; `^`/`~`/`latest` kullanma.

### 8.4 Emin olmadığında
Bir bilgiye dosyalardan ulaşamıyorsan **uydurma** — *"dosyalarda yok,
netleştirilmeli"* diye işaretle ve `scope-and-conflicts.md`'ye satır ekle.
Bu projede bilinmeyen bir şeyi tahminle doldurmak, boş bırakmaktan daha
maliyetlidir.

---

## 9. Bilinen Teknik Borç / Notlar

- **Hiç kod yok.** `frontend/` klasörü mevcut değil; §5.2 ilk adım.
- **3 bloke edici karar** var (§5.1) — özellikle OAuth redirect origin'i
  container aşamasını bloke ediyor.
- **`Page` zarfının JSON alan adları doğrulanmadı** — `Page<CustomerDetailResponse>`
  dönüyor ama serileştirme modu ayarlanmamış; çalışan instance'ta teyit gerekiyor.
- **Erişilebilirlik bizim yükümüz** — bileşen kütüphanesi kullanmama kararının
  gerçek bedeli bu (FE-ADR-011 §g). Her bileşende gözetilecek.
- **Validasyon kuralları bilinçli olarak çift yazılıyor** (istemci + sunucu).
  Backend kataloğu değişirse frontend'in takip etmesi gerekir.
- **Backend'e bildirilmiş 5 bulgu** var (`scope-and-conflicts.md` §5) — biri
  gerçek bir arama hatası (`İ` ile başlayan isimler bulunamıyor).
- **Angular 6 ayda bir major çıkarıyor** — sürüm yükseltme planlı bir iş olarak
  ele alınmalı, pasif sürüklenme olarak değil (FE-ADR-002).
