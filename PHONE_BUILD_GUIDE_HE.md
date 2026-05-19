# בניית APK ישירות מהטלפון (בלי מחשב)

אם אין לך מחשב בכלל, הדרך הכי אמינה היא GitHub Actions (בענן):

1. פתח ריפו ב-GitHub דרך הדפדפן בטלפון.
2. העלה את הקבצים של הפרויקט לריפו.
3. בכרטיסיית **Actions** בחר workflow בשם **Build APK**.
4. לחץ **Run workflow**.
5. המתן לסיום (סטטוס ירוק).
6. היכנס להרצה -> **Artifacts** -> הורד `vinst-share-debug-apk`.
7. בטלפון חלץ ZIP ותקבל `app-debug.apk`.
8. התקן את ה-APK.

## אם לא מופיע Actions
- אשר את GitHub Actions בריפו.
- ודא שהקובץ נמצא בנתיב: `.github/workflows/build-apk.yml`.

## התקנה בטלפון
- פתח קובץ APK דרך Files.
- אשר `Install unknown apps` לאפליקציית הקבצים/דפדפן.
- התקן.

