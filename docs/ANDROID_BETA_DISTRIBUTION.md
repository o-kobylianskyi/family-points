# Публікація Android BETA поза Google Play

- Публічний маршрут: `https://family-point.com/download`.
- Завантаження: `https://family-point.com/downloads/familypoints-beta.apk`.
- APK зберігається **окремо від образу Web** в `/opt/familypoints-beta/downloads/`, змонтованому read-only у Nginx-контейнер. APK не включати в Git.
- До першої публікації сторінка показує «APK ще не опубліковано»; не створювати фальшивих посилань.
- Перший реліз APK потребує власного постійного release keystore та секретів `FP_ANDROID_KEYSTORE_FILE`, `FP_ANDROID_KEYSTORE_PASSWORD`, `FP_ANDROID_KEY_ALIAS`, `FP_ANDROID_KEY_PASSWORD`. Ключ і паролі не комітити й не надсилати в чат. Обов'язково мати захищену резервну копію: без того самого ключа наступне оновлення поверх установленого APK неможливе.
- Збірка на Windows в корені проєкту: `powershell -ExecutionPolicy Bypass -File scripts/android/build-beta-release.ps1 -VersionCode 1 -VersionName "0.1.0-beta.4" -ApiUrl "https://family-point.com/"`. Команда працює лише якщо секрети для підпису задані локально.
- Перед публікацією перевірити `apksigner verify --verbose --print-certs app-release.apk`, SHA256 та робочий HTTPS API. Кожний наступний APK повинен мати **більший versionCode**, незмінні `applicationId` і підпис.
- Завантажити через захищений SSH/SCP у тимчасовий файл в `downloads/`, перевірити SHA256 на обох машинах і атомарно перейменувати на `familypoints-beta.apk`. Не виставляти пароль чи підписуючий ключ на сервері — лише готовий APK.
- При оновленнях сервера не запускати `docker compose down -v`; bind-mount `downloads/` не залежить від перебудови Web.
- Сторінка /download не має показувати активну кнопку до появи правильного APK. Автоматичне оновлення Android *ще не реалізоване*: на цьому етапі користувач вручну завантажує нову версію поверх старої.
