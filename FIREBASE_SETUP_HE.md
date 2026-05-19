# הגדרת Firebase App Distribution שלב-אחר-שלב (מהטלפון)

המטרה: כל Push/Run Workflow יבנה APK ויפיץ אותו אוטומטית להתקנה דרך Firebase.

## 1) ודא שיש לך אפליקציה רשומה ב-Firebase
1. כנס ל-Firebase Console.
2. בחר פרויקט.
3. אם אין Android app:
   - לחץ Add app (Android)
   - הזן Package name של האפליקציה שלך
4. שמור את ה-App ID (פורמט כמו: `1:1234567890:android:abcdef123456`).

> להפצה דרך CI לא חובה לשלב כרגע SDK בתוך האפליקציה. `google-services.json` לא נדרש ל-Distribution בלבד.

## 2) צור קבוצת בודקים ב-Firebase
1. Firebase Console -> App Distribution.
2. כנס ללשונית Testers & Groups.
3. לחץ Create group.
4. שם קבוצה: `internal-testers`.
5. הוסף את המייל שלך כמבקר (Tester).
6. שמור.

אם בחרת שם אחר לקבוצה - עדכן בקובץ workflow את `groups:`.

## 3) צור Service Account JSON
1. Firebase Console -> Project settings -> Service accounts.
2. לחץ "Generate new private key".
3. ירד לך קובץ JSON.
4. פתח את הקובץ והעתק את כל התוכן (כולל הסוגריים).

## 4) הוסף Secrets ב-GitHub (חובה)
ב-GitHub Repo:
1. Settings -> Secrets and variables -> Actions.
2. New repository secret:
   - Name: `FIREBASE_APP_ID`
   - Value: App ID מהשלב הראשון.
3. New repository secret:
   - Name: `FIREBASE_SERVICE_ACCOUNT_JSON`
   - Value: כל תוכן ה-JSON.

## 5) הרץ Workflow
1. Actions -> "Build & Distribute to Firebase App Distribution".
2. לחץ Run workflow.
3. המתן לסיום ירוק.

## 6) התקנה מהטלפון
1. פתח Firebase App Distribution (מייל הזמנה או האפליקציה של Firebase testers).
2. בחר את ה-Release האחרון.
3. Install.

## תקלות נפוצות
- "Missing secret": אחד ה-secrets לא הוגדר נכון.
- "Tester not in group": המייל שלך לא בקבוצה `internal-testers`.
- "Invalid app id": `FIREBASE_APP_ID` שגוי או שייך לפרויקט אחר.
