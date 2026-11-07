# Mengidata — Usimamizi wa Mapato, Malipo na Matumizi

Programu ya usimamizi wa fedha za biashara yenye UI moja ya React inayotumika
kwenye tovuti na ndani ya app ya Android. UI na data yake huhifadhiwa kwenye simu,
kwa hiyo app inafanya kazi bila intaneti; intaneti inahitajika tu kwa viwango vya
sarafu na kuangalia updates. Data kutoka kwenye toleo la zamani la Android huletwa
kiotomatiki mara ya kwanza app mpya inapofunguliwa.

| | |
|---|---|
| Faili la kusakinisha | `Mengidata.apk` |
| Toleo | 1.8.1 |
| Android | 7.0 (API 24) au zaidi |
| Intaneti | Inahitajika kwa updates na viwango vya sarafu tu |
| Lugha | Kiswahili na English |
| Signature | APK v2/v3, SHA-256 (angalia chini) |
| Muonekano | Mandhari ya Mfumo/Mwanga/Giza Premium, logo ya duara ya 3D, fonti maalum na animation fupi |

---

## 1. Jinsi ya kusakinisha kwenye simu

1. Hamishia `Mengidata.apk` kwenye simu — kwa USB, Bluetooth, au ujitumie
   mwenyewe kwenye WhatsApp/Telegram.
2. Fungua faili hilo kwenye simu (kwa mfano kwenye **Files** au **My Files**).
3. Android itaomba ruhusa ya **"Install unknown apps"** kwa programu
   unayotumia kufungua faili. Bonyeza **Settings → Allow from this source**.
4. Bonyeza **Install**, kisha **Open**. Kama **Play Protect** inaonyesha tahadhari kwa sababu programu haitoki Play Store, chagua **Install anyway** / **Sakinisha hata hivyo**.

> Kama toleo la zamani lina data muhimu, fungua **Mipangilio → Pakua nakala rudufu
> (.json)** kabla ya kusakinisha hili. Toleo hili la Android ya asili hutumia
> SQLite, kwa hiyo unaweza kuleta backup kupitia **Mipangilio → Rejesha nakala
> rudufu**. Ikiwa Android itasema saini ya APK haitolingani, hifadhi backup
> kisha ondoa toleo la zamani na usakinishe hili jipya; usiondoe app kabla ya
> kuthibitisha nakala rudufu.

### Kama "App not installed" inaonekana

- Hakikisha simu ina Android 7.0 au zaidi.
- Hakikisha kuna nafasi ya kutosha (angalau 50 MB).
- Ondoa toleo la zamani kwanza, kisha jaribu tena.
- Kama faili lilihamishwa kwa njia isiyokamilika, hamisha tena.

---

## 2. Matumizi

- **Dashibodi** — muhtasari wa mapato, matumizi, faida na grafu ya nguzo ya
  mapato dhidi ya matumizi kwa miezi sita.
- **Miamala** — ongeza, hariri, tafuta na chuja mapato au matumizi. Chagua
  matumizi ya kila siku, matumizi ya nyumbani, chakula, umeme, maji, kodi,
  usafiri, afya, elimu, mawasiliano au kundi lako maalum.
- **Ankara** — tengeneza ankara, rekodi kama mteja amelipa au anadaiwa, chuja
  wamelipa/wanaodaiwa na tazama jumla ya fedha zilizopokelewa na salio.
  Pakua PDF ya kibiashara yenye
  maelezo ya mteja, bidhaa, kodi, punguzo, jumla na salio; PDF hupanuka kwa
  kurasa zaidi ikiwa bidhaa ni nyingi.
- **Bili** — fuatilia bili zinazokuja, zilizolipwa na zilizochelewa; chagua
  kundi kama kodi ya pango, umeme, maji, chakula, malipo ya fundi, matengenezo,
  mishahara, usafiri, mafuta, manunuzi, afya, kodi na vibali, pamoja na aina
  nyingine za gharama.
- **Daftari la biashara** — hifadhi hadi kurasa 500 za madokezo ya biashara,
  hariri au futa ukurasa, na weka alama/bookmark kwenye kurasa muhimu. Madokezo
  yanabaki kwenye simu na yanajumuishwa kwenye nakala rudufu ya JSON.
- **Ripoti** — uchambuzi wa mwezi kwa mwezi, kundi kwa kundi; pakua CSV au
  chapisha ripoti.
- **Muonekano wa premium** — chagua Mfumo (fuata mandhari ya simu), Mwanga, au
  Giza Premium yenye kijani cha kina na lafudhi za lime. Mandhari ya Mfumo
  hufuata hali ya Android moja kwa moja; rangi za status bar, navigation bar,
  paneli, fomu, vichujio na kurasa hubadilika pamoja na mandhari. UI hutumia
  fonti za Plus Jakarta Sans na Playfair Display, na animation fupi huheshimu
  mipangilio ya kupunguza mwendo pamoja na font scale ya Android.
- **Kikokotoo** — fungua kutoka kwenye sehemu za kiasi/bei au Mipangilio; hesabu
  za kawaida na matokeo hujazwa kwenye sehemu uliyochagua.
- **Kubadilisha sarafu** — pata viwango vya hivi karibuni mtandaoni kwa TZS,
  USD, KES, UGX, EUR na GBP. Viwango hutolewa na ExchangeRate-API na vinaweza
  kusasishwa kwa vipindi; si bei ya moja kwa moja ya soko.
- **Updates za app** — washa ukaguzi wa kiotomatiki kwenye Mipangilio au angalia
  mwenyewe. Update itapakuliwa baada ya kuidhinishwa na Android itaomba ruhusa
  ya kusakinisha. Release ya umma yenye APK lazima iwepo GitHub kwanza.
- **Mipangilio** — taarifa za biashara, VAT, maelezo ya malipo, nakala rudufu.
- **Kufuli la PIN** — weka au badili PIN ya tarakimu 6–12 kwenye Mipangilio.
  App hujifunga ikifichwa, huzuia picha za skrini, na huongeza muda wa kusubiri
  baada ya majaribio mengi yasiyo sahihi. PIN inalindwa kwa ufunguo wa Android
  Keystore; faili la hifadhidata ya fedha lenyewe halijasimbwa kwa njia fiche.
- **Lugha** — tumia kitufe cha EN/SW kwenye sehemu ya juu kubadili Kiingereza na Kiswahili.
- **Vikumbusho vya bili** — washa kwenye Mipangilio; ruhusu arifa ili upokee
  ukumbusho saa 9:00 asubuhi siku ya malipo na kila siku bili ikiwa haijalipwa.
- **Kushiriki ankara** — fungua ankara, chagua **Shiriki ankara**, hifadhi PDF
  kwenye folda unayochagua, kisha chagua programu ya kuituma.

Vitufe vya kawaida:

- **Pakua PDF** — chagua folda ya kuhifadhi kwenye kidirisha cha Android,
  kwa mfano **Download/Mengidata/**.
- **Hifadhi / rejesha faili** — Android hufungua kichagua faili cha kawaida.
  Chagua mahali kwenye simu au hifadhi ya nje; programu haiombi ruhusa pana ya
  kusoma faili zote za simu.
- **Chapisha** — hufungua dirisha la kuchapisha la Android; unaweza pia
  kuchagua **Save as PDF**.
- **Kitufe cha "back"** — kinafunga modal, kisha kinarudi Dashibodi; ukibonyeza
  tena kwenye Dashibodi, programu inafungwa.

---

## 3. Data na nakala rudufu

Data huhifadhiwa kwenye hifadhidata ya SQLite ya ndani ya app. Haipitii
seva yoyote.

- **Nakala rudufu:** Mipangilio → **Pakua Backup (.json)**. Chagua mahali pa
  kuhifadhi kupitia kidirisha cha Android.
- **Kurejesha:** Mipangilio → **Chagua faili la JSON**.
- **CSV:** Mipangilio au Ripoti → **Pakua CSV**.

> Kuondoa programu (uninstall) au kufuta data ya programu kutaondoa data yote.
> Pakua backup mara kwa mara na uihifadhi mahali pengine (kwa mfano ujitumie
> kwenye WhatsApp au uweke kwenye Drive).

> Ukisahau PIN, hakuna njia ya kuirejesha. Hifadhi nakala rudufu ya fedha,
> ondoa app kisha usakinishe tena na urejeshe backup. Kufuta data au kuondoa
> app bila backup kutapoteza taarifa zako.

---

## 4. Faragha

- Hakuna akaunti, hakuna matangazo, hakuna analytics.
- Data ya miamala hubaki kwenye simu. Intaneti hutumika tu kuangalia toleo la
  app na kupata kiwango cha sarafu; hakuna data ya miamala inayotumwa.
- APK ya update hukaguliwa kwa package name, toleo na cheti cha saini kabla ya
  kuanzisha usakinishaji. Android huomba uthibitisho wa mtumiaji.
- `android:allowBackup="false"` — Android haitakili data yako kwenye cloud.

---

## 5. Kujenga APK mwenyewe

Mahitaji: Android Studio au JDK 17+ pamoja na Android SDK (platform 35).

### Kujenga kupitia Android Studio

Fungua folda ya `android-studio/` kwenye Android Studio. Project hutumia
`native/java/` na `android/res/` moja kwa moja, na APK ya release hupatikana
kwenye `android-studio/app/build/outputs/apk/release/Mengidata.apk`.
Unaweza pia kuijenga kwa `.\gradlew.bat assembleRelease` ukiwa ndani ya folda
ya `android-studio`.

Kwa release iliyosainiwa, weka `MENGI_SIGNING_STORE_PASSWORD` kwenye
environment ya Android Studio kabla ya kuifungua. `MENGI_SIGNING_KEY_PASSWORD`
na `MENGI_SIGNING_KEY_ALIAS` ni za hiari; zisipowekwa hutumia nenosiri lilelile
na alias ya kawaida ya mradi. Usihifadhi nenosiri kwenye faili za project.

### Kujenga APK kupitia script

Script hii bado inajenga APK ileile bila kutumia Gradle:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-apk.ps1
```

Matokeo: `Mengidata.apk` kwenye folda kuu.

Maelezo:

- App ya Android imeandikwa kwa Java ya asili; skrini zake ziko
  `native/java/tz/mengidata/app/NativePages.java`.
- UI ya wavuti hutumia fonti ya mfumo ya kifaa; mchoro wa dashibodi ni vector
  ya Android inayobaki wazi bila kuongeza picha nzito za kujaza APK.
- Data huhifadhiwa kwa SQLite kupitia `native/java/tz/mengidata/app/Db.java`.
- `src/` ina toleo la wavuti la mradi; halijumuishwi wala kuendeshwa ndani ya APK.
- `scripts/build-apk.ps1` inakusanya rasilimali kwa `aapt2`, Java kwa `javac`,
  dex kwa `d8`, kisha kusaini kwa `apksigner`.
- Keystore ya kudumu: `android/keystore/mengi-data.keystore`
  (alias ya kawaida `mengidata`). **Ihifadhi** — ukibadilisha keystore,
  utalazimika kuondoa programu kabla ya kusakinisha toleo jipya. Nenosiri
  lisihifadhiwe kwenye faili za mradi; tumia `MENGI_SIGNING_STORE_PASSWORD`
  na, ikiwa tofauti, `MENGI_SIGNING_KEY_PASSWORD`.
- Nembo ya SVG: `public/mengi-data-icon.svg`; icons za Android na splash:
  `scripts/generate-icons.py` (inahitaji Pillow).

---

## 6. Utatuzi wa matatizo

| Tatizo | Suluhisho |
|---|---|
| Programu inafunguka na kisha inafunga | Fungua tena. Ikiendelea, sasisha Android au sakinisha APK upya; hifadhi nakala rudufu kabla ya kufuta data ya app. |
| Skrini tupu | Funga kabisa (recents → swipe) kisha fungua tena. Ikiendelea, sakinisha APK upya. |
| PDF haipatikani | Angalia folda uliyochagua kwenye kidirisha cha kuhifadhi. |
| PDF ina "?" badala ya herufi | PDF huhifadhi herufi za Kiswahili za kawaida; alama maalum sana zinaweza kubadilishwa. |
| Tarehe za ankara si sahihi | Rekebisha tarehe/muda wa simu. |
| "App not installed" | Ondoa toleo la zamani, angalia nafasi, na hakikisha Android 7+. |
| Data imepotea baada ya kusasisha | Kusasisha hakuondoi data; kama ulifuta data ya programu, rejesha kutoka backup. |

---

## 7. Muundo wa mradi

```
src/                     Toleo la wavuti la React (halitumiki na APK ya asili)
native/java/             App ya Android ya asili, skrini na hifadhidata ya SQLite
android/                 Manifest, icons na rasilimali za Android
scripts/build-apk.ps1    Kujenga APK bila Gradle
scripts/generate-icons.py
Mengidata.apk            Faili la kusakinisha
```

---

## 8. SHA-256 ya APK

APK iliyo kwenye folda kuu ni toleo 1.8.1 (`versionCode 9`), imesainiwa kwa
APK Signature Scheme v2/v3, na ukubwa wake ni takriban 0.44 MB.

```
0B9095B14D68D84BC2763C416E865472063873B2192671DEC6B0885A55A2C8F7
```

Unaweza kuithibitisha kwenye simu kwa programu za checksum, au kwenye kompyuta:

```powershell
Get-FileHash .\Mengidata.apk -Algorithm SHA256
```
