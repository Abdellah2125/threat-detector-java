# Threat Detector — إصدار JavaFX

إعادة كتابة **مستقلة بالكامل** بلغة Java وJavaFX لأداة كشف تهديدات مبنية أصلاً بتقنية Rust/Tauri. المشروع منفصل عن المستودع الأصلي ويُستخدم للمقارنة والتطوير التعليمي.

ينفّذ المشروع متطلبات Sprint 01 حرفياً:

- واجهة **JavaFX** أصلية.
- قواعد كشف **محلية** بلغة Java.
- **تعدد خيوط حقيقي** بمسبح ثابت من 4 عمال.
- إدخال مفرد وإدخال دفعي عبر **CSV**.
- حالة مستقلة **Not Verified** لا تُحوَّل أبداً إلى "آمن".

---

## التشغيل السريع

### الطريقة ١ — الأسهل: ملف `run.bat` (Windows)

1. افتح مجلد المشروع.
2. انقر مرتين على `run.bat`.

الملف يفعل الآتي تلقائياً:

- ينتقل إلى مجلد المشروع.
- يتحقق من وجود الـ jar؛ وإن لم يكن موجوداً يبنيه عبر Maven.
- يشغّل التطبيق.

### الطريقة ٢ — من سطر الأوامر

```bat
cd /d C:\Users\mhmdb\Desktop\threat-detector-java
java -jar target\threat-detector-java-0.1.0.jar
```

### الطريقة ٣ — بناء كامل من الصفر

```bat
cd /d C:\Users\mhmdb\Desktop\threat-detector-java
mvn clean test
mvn package -DskipTests
java -jar target\threat-detector-java-0.1.0.jar
```

أو دفعة واحدة:

```bat
mvn clean package -DskipTests
```

### بعد الاستنساخ من GitHub

```bat
git clone <رابط-المستودع>
cd <اسم-المجلد>
mvn clean package -DskipTests
java -jar target\threat-detector-java-0.1.0.jar
```

> على Linux/macOS استخدم `java -jar target/threat-detector-java-0.1.0.jar` و`mvn` بالطريقة نفسها.

---

## المتطلبات

| المتطلب | الإصدار المُختبر |
|---|---|
| JDK | 17 أو أحدث (Temurin 17) |
| Maven | 3.9 أو أحدث |
| JavaFX | مُضمَّن داخل الـ jar، لا حاجة لتثبيته منفرداً |
| الشبكة | غير مطلوبة وقت التشغيل؛ Maven ينزّل التبعيات مرة واحدة أثناء البناء فقط |

---

## استخدام الواجهة

الواجهة الآن على شكل dashboard فاتح مستوحى من النموذج المرجعي: شريط تنقّل علوي، بطاقة تعريف، ثم عمود فحص رئيسي وعمود جانبي للملخّص والسجل.

| المساحة | ما تفعله |
|---|---|
| **Dashboard** | يفتح فحص مفرد. اختر `URL` أو `Message`، أو اضغط إحدى البطاقات الثلاث `Phishing URL` / `Nested domain` / `Clean URL` لتعبئة الحقل، ثم `Analyze`. |
| **History** | يفتح فحص CSV. اختر `Choose file` وافتح `samples\inputs.csv`، ثم `Analyze`. |
| **Settings** | يعرض سلم الدرجات، قواعد الروابط والرسائل، وعقد التزامن والحدود. |
| **Hardware** | يعرض مشهد `ThreadCore` ثلاثي الأبعاد وحالة العمال أثناء الفحص. |
| **Risk Levels** | اختصار لصفحة `Settings`. |
| **Threat Summary** | ملخّص حي لعدد الفحوص ونتائج الخطورة العالية والمتوسطة. |
| **Analysis Results** | آخر خمس نتائج مع مستوى الخطورة والحالة والتفاصيل والوقت. |

مفتاحا `Single Input` و`CSV Batch` أعلى بطاقة الفحص يبدّلان بين الوضعين مباشرة.

الهوية البصرية الجديدة: خلفية `#F4F7F5`، بطاقات بيضاء بحواف `#1E7D3C` وظلال خفيفة، نص `#10161C`، وألوان مخاطر `#C2263B` / `#B45309` / `#127A48`. اللقطة الحالية في `preview.png`.

### ملف CSV المقبول

```csv
type,target
url,https://example.com
message,Your code is 123456
```

- الترتيب: `type,target` (غير حساس لحالة الحروف).
- الأنواع المقبولة: `url` أو `message` فقط.
- الحد الأقصى: **5 MB** و**5000 صف بيانات**.
- الصف غير الصالح يبقى `Not Verified / INVALID_CSV_ROW` ولا يُسقط الملف.

---

## آلية الكشف

### الخيار المستخدم: Option A — قواعد محلية

لا يوجد اتصال بالإنترنت. يقوم `RuleSet` بتحليل الرابط أو الرسالة عبر قواعد حتمية، ثم `Scoring` يحوّل المجموع إلى درجة ومستوى خطورة.

| المستوى | الدرجة |
|---|---|
| `HIGH` | 60 – 100 |
| `MEDIUM` | 25 – 59 |
| `LOW` | 0 – 24 |
| `NOT VERIFIED` | تعذّر التحقق — حالة مستقلة وليست "منخفضة" |

عدد القواعد: **10 قواعد للروابط + 7 قواعد للرسائل = 17 قاعدة**.

### الخيار الثاني: Option B — API خارجي

`ExternalApiEvaluationService` يُحاكي فقط أعطال API المطلوبة في التكليف: فقدان المفتاح، HTTP 429، timeout، واستجابة غير صالحة. لا يوجد طلب HTTP حقيقي في النسخة المُسلَّمة.

### طبقة AI الاختيارية

`AiExplanationService` بنية قابلة للتوصيل فقط؛ تُعيد `Optional.empty()` عندما لا تتوفر، ولا تغيّر الدرجة. لم يُرفق نموذج مدرب حقيقي.

---

## البنية

```
src/main/java/com/threatdetector/
├── Launcher.java                  نقطة دخول الـ fat-jar
├── model/                         سجلات بيانات وأنماط وأحداث غير قابلة للتعديل
├── detection/                     17 قاعدة + RuleSet + Scoring + RuleCatalog
├── intake/                        قارئ CSV محدود ومُتحقَّق منه
├── runtime/                       ScanCoordinator + ScanEngine + الخدمات
├── ui/                            قشرة JavaFX، اللوحات، التقرير، مشهد ThreadCore ثلاثي الأبعاد
│   ├── TopNav.java                شريط التنقل العلوي وحالة الفحص
│   ├── SummaryCard.java           بطاقة Threat Summary
│   ├── RecentHistoryCard.java     جدول Analysis Results
│   └── Mode.java                  الأوضاع: Dashboard / Batch / Method / Core

src/main/resources/
├── app.css                        الهوية البصرية ولوحة الألوان
└── fonts/                         Chakra Petch وJetBrains Mono بترخيص OFL

docs/SPRINT1_MAPPING.md            مصفوفة مطابقة المتطلبات
samples/inputs.csv                 عيّنة جاهزة للفحص الدفعي
threat-detector-report-ar.pdf      تقرير شامل باللغة العربية
report.html                        مصدر التقرير القابل للتعديل
run.bat                            ملف التشغيل السريع على Windows
```

اتجاه التبعية:

```
ui → runtime → detection → model
intake → model
```

الواجهة لا تحسب أي درجة؛ تستدعي `ScanCoordinator` فقط وتستقبل سجلات غير قابلة للتعديل.

---

## الاختبارات

```bat
mvn test
```

النتيجة المتوقعة:

```
Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
```

توزيع الاختبارات: `RuleSetTest 24`، `CsvLoaderTest 8`، `ScanEngineTest 8`، `ScoringTest 5`، `ServiceTest 7`.

---

## ملاحظات

- تشغيل الـ jar قد يُظهر تحذير `Unsupported JavaFX configuration: classes were loaded from 'unnamed module'`؛ هو تحذير غير مؤثر.
- المشروع لا يرسل أي بيانات خارج الجهاز في الوضع الحالي.
- لقطة شاشة للحالة الحالية موجودة في `preview.png`.
- التفاصيل الكاملة في [docs/SPRINT1_MAPPING.md](docs/SPRINT1_MAPPING.md) و[التقرير PDF](threat-detector-report-ar.pdf).

---

## English quick start

```bat
mvn clean package -DskipTests
java -jar target\threat-detector-java-0.1.0.jar
```

Batch sample: `samples\inputs.csv` → **Bulk scan → Choose file → Run batch**.
