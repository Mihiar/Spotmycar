# SpotMyCar

Retrouvez votre voiture, même sans GPS.

SpotMyCar est une application Android qui note toute seule l'endroit où vous êtes
garé, dès que votre téléphone quitte le Bluetooth de la voiture. En parking
souterrain, où le GPS ne passe pas, le niveau, la zone, la place et une photo prennent le relais.

- Gratuit, sans compte, sans publicité
- Tout reste sur le téléphone : aucun serveur
- Français, anglais, arabe
- Android 8.0 ou plus récent

## Ouvrir le projet

1. Installer [Android Studio](https://developer.android.com/studio).
2. `File > Open`, puis choisir ce dossier.
3. Attendre la fin de la synchronisation Gradle, brancher un téléphone, appuyer sur ▶.

## Publier une version

Pousser une étiquette `v1.0.0` (par exemple) : GitHub compile l'APK signé et le publie
dans l'onglet **Releases**. Les secrets de signature sont décrits dans
`.github/workflows/build.yml`.

Le contexte complet du projet (décisions, état, points de vigilance) est dans
[PROJECT_SETUP.md](PROJECT_SETUP.md).
