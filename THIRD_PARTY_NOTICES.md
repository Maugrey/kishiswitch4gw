# Third-party notices / Composants tiers

Kishi Switch's PolyForm Noncommercial license applies to the project's own code.
It does not replace the licenses of the third-party components below.
License texts and attributions are also bundled in the APK and can be read
offline in **Licences et attribution** (licenses and attribution).

La licence PolyForm Noncommercial de Kishi Switch s'applique au code propre du
projet. Elle ne remplace pas les licences des composants tiers ci-dessous.
Les textes de licence et les attributions sont aussi intégrés à l'APK et
consultables dans « Licences et attribution » sans connexion réseau.

## App libraries / Bibliothèques de l'application

### Shizuku API 13.1.5

Modules : `dev.rikka.shizuku:api`, `provider`, `shared` et `aidl`.

Modules: `dev.rikka.shizuku:api`, `provider`, `shared` and `aidl`.

Copyright (c) 2021 RikkaW.

License / Licence: MIT — [full text / texte intégral](licenses/Shizuku-API-MIT.txt).
Sources : https://github.com/RikkaApps/Shizuku-API

The Shizuku app itself is installed separately and is not bundled in this APK.
Its project is licensed under Apache 2.0 and retains its own identity and icons.

L'application Shizuku elle-même est installée séparément et n'est pas embarquée
dans cet APK. Son projet est sous Apache 2.0 et conserve son identité et ses icônes.

### Kotlin standard library 2.2.21

Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.

Main license / Licence principale : [Apache 2.0](licenses/Apache-2.0.txt).
Sources : https://github.com/JetBrains/kotlin/tree/v2.2.21/libraries/stdlib

The standard library also includes the following portions, according to Kotlin's
license inventory and this version's sources:

La bibliothèque standard comporte également les portions suivantes, selon
l'inventaire de licences de Kotlin et les sources de cette version :

- GWT-derived collections / Collections dérivées de GWT : Copyright 2007 Google Inc. — [Kotlin-gwt_license.txt](licenses/Kotlin-gwt_license.txt).
- Guava-derived unsigned arithmetic / Calculs non signés dérivés de Guava : Copyright 2011 The Guava Authors — [Kotlin-guava_license.txt](licenses/Kotlin-guava_license.txt).
- Boost-derived math functions / Fonctions mathématiques dérivées de Boost : Copyright Eric Ford & Hubert Holin 2001. — [Kotlin-boost_LICENSE.txt](licenses/Kotlin-boost_LICENSE.txt).
- ThreeTenBP-derived time handling / Gestion du temps dérivée de ThreeTenBP : Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos — [Kotlin-threetenbp_license.txt](licenses/Kotlin-threetenbp_license.txt).

Reference / Référence : https://github.com/JetBrains/kotlin/blob/v2.2.21/license/README.md

### AndroidX Annotation 1.3.0

Copyright (C) 2013 The Android Open Source Project
Copyright (C) 2014 The Android Open Source Project
Copyright (C) 2015 The Android Open Source Project
Copyright (C) 2016 The Android Open Source Project
Copyright (C) 2017 The Android Open Source Project
Copyright 2018 The Android Open Source Project
Copyright 2019 The Android Open Source Project
Copyright (C) 2020 The Android Open Source Project
Copyright (C) 2021 The Android Open Source Project
Copyright 2021 The Android Open Source Project

License / Licence : [Apache 2.0](licenses/Apache-2.0.txt).
Version sources / Sources de la version : https://dl.google.com/dl/android/maven2/androidx/annotation/annotation/1.3.0/annotation-1.3.0-sources.jar

### JetBrains Annotations 13.0

Copyright 2000-2009 JetBrains s.r.o.
Copyright 2000-2012 JetBrains s.r.o.
Copyright 2000-2013 JetBrains s.r.o.
Copyright 2006 Sascha Weinreuter

License / Licence : [Apache 2.0](licenses/Apache-2.0.txt).
Version sources / Sources de la version : https://repo1.maven.org/maven2/org/jetbrains/annotations/13.0/annotations-13.0-sources.jar

## Repository tools / Outils du dépôt

Gradle Wrapper 8.13 (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`)
is distributed under [Apache 2.0](licenses/Apache-2.0.txt). Its scripts retain
their original notices, including `Copyright 2015 the original author or authors`.

Le Gradle Wrapper 8.13 (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`)
est distribué sous [Apache 2.0](licenses/Apache-2.0.txt). Les scripts conservent
leurs mentions originales, dont `Copyright 2015 the original author or authors`.
Project / Projet : https://github.com/gradle/gradle/tree/v8.13.0

Build tools and test libraries are downloaded by Gradle under their respective
licenses. JUnit and AndroidX Test are not bundled in the release APK.

Les outils de compilation et les bibliothèques de test sont téléchargés par
Gradle sous leurs licences respectives. JUnit et AndroidX Test ne sont pas
intégrés à l'APK de livraison.
