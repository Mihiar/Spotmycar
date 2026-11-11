# SpotMyCar : document de contexte du projet

> Document vivant. À mettre à jour à chaque décision. Dernière mise à jour : 11 novembre 2026.

## En une phrase

Une app Android qui note toute seule où la voiture est garée, y compris en parking
souterrain où le GPS ne passe pas, et qui aide à la retrouver.

## Décisions verrouillées

| Sujet | Décision | Pourquoi |
|---|---|---|
| Plateforme | Android d'abord, iOS plus tard | Trouver le public Android avant d'investir dans iOS |
| Langage | Kotlin natif (pas Flutter) | Décision prise après le pivot ; ne plus y revenir avant le lancement |
| Interface | Jetpack Compose (Material 3) | Moins de fichiers, standard actuel d'Android |
| Stockage | Sur le téléphone uniquement (SharedPreferences) | Pas de compte, pas de serveur, confidentialité totale |
| Marché | Maroc d'abord, puis international | Français, anglais et arabe dès la v1 |
| Distribution | Lien direct (APK) + Google Play en parallèle | Google impose ~3 semaines à un compte neuf ; le lien direct est disponible tout de suite |
| Hébergement | GitHub (code, page web, fichier à télécharger) | Gratuit, et l'app n'a aucun serveur à faire tourner |
| Identifiant de l'app | `app.spotmycar.android` | Définitif une fois publié sur Google Play |
| Android minimum | Android 8.0 (API 26) ; cible Android 16 (API 36) | API 36 est exigée par Google Play pour toute nouvelle app |

## Principes du produit

1. **Le GPS absent est le cas normal.** Étage, zone et photo sont le fonctionnement
   principal, pas un plan B.
2. **Zéro effort.** L'enregistrement automatique par Bluetooth est ce qui distingue
   l'app : les concurrents échouent parce qu'ils demandent un geste au moment où
   l'on est le moins motivé.
3. **Loi de Hick.** Un seul geste possible sur l'écran d'accueil.
4. **Règle du pic et de la fin.** L'écran « retrouver » est le moment qui compte :
   le repère en très grand, l'itinéraire à un appui.
5. **Boutons empilés verticalement**, jamais côte à côte.
6. **Animation « pin drop ».** L'épingle tombe et une onde part quand on se gare ;
   les ondes reviennent et l'épingle saute quand on retrouve la voiture.
7. **Couleurs pour tous les âges** : bleu panneau de parking, ambre, vert.

## Ce que fait la v1

- **Garer ici** : un appui. La place est notée tout de suite ; la position GPS arrive
  ensuite si elle existe (sinon : dernière position connue, souvent l'entrée du parking).
- **Détails facultatifs** : photo, étage/zone (boutons rapides de -3 à 3), note.
- **Ma voiture** : photo, étage en très grand, « garée il y a… », itinéraire à pied
  (Google Maps), partage avec un proche, « J'ai retrouvé ma voiture ».
- **Enregistrement automatique** : quand le téléphone se déconnecte du Bluetooth de la
  voiture choisie, la place est notée et une notification propose d'ajouter l'étage.
- **Réglages** : mise en route de l'automatique en étapes à cocher, aide pour Xiaomi /
  Oppo / Huawei (démarrage automatique, batterie), politique de confidentialité.

## Plan des fichiers

```
app/src/main/java/app/spotmycar/android/
  MainActivity.kt              l'unique écran Android
  data/ParkingSpot.kt          ce qu'est une « place »
  data/ParkingStore.kt         la mémoire de l'app (place en cours)
  data/AppSettings.kt          réglages de l'automatique
  location/LocationFetcher.kt  obtenir la position sans bloquer
  bluetooth/CarBluetoothReceiver.kt   LE cœur : déconnexion Bluetooth -> place notée
  bluetooth/BluetoothHelper.kt appareils associés, état de l'automatique
  notify/Notifier.kt           notification « Place enregistrée »
  util/Photos.kt, Actions.kt   photos ; cartes, partage, réglages du téléphone
  ui/…                         les 4 écrans (Garer, Détails, Ma voiture, Réglages)
app/src/main/res/values*/strings.xml   textes EN / FR / AR (générés par tools/gen_strings.py)
docs/                          page web + politique de confidentialité (GitHub Pages)
.github/workflows/build.yml    compilation automatique et publication de l'APK
```

## État

- [x] Code de l'app écrit (v1 complète)
- [x] Page web et politique de confidentialité rédigées
- [x] Compilation automatique préparée
- [ ] **Première compilation** (le code n'a pas encore été compilé : à faire sur GitHub)
- [ ] Test sur le Xiaomi : parcours manuel, parking sans GPS, Bluetooth automatique
- [ ] Clé de signature créée et mise à l'abri
- [ ] Version 1.0.0 publiée en lien direct
- [ ] Compte Google Play Console ouvert et vérifié
- [ ] Test fermé : 12 testeurs pendant 14 jours
- [ ] Déclaration « position en arrière-plan » (formulaire + courte vidéo)
- [ ] Mise en production sur Google Play

## Points de vigilance

- **Clé de signature** : la même clé doit signer le lien direct ET la version Google
  Play (option « utiliser ma propre clé » à la première mise en ligne), sinon les
  personnes ayant installé le lien direct ne pourront pas passer à la version Play.
- **Position en arrière-plan** : Google Play l'examine à part (formulaire, vidéo,
  explication dans l'app, déjà en place). C'est le point le plus susceptible de retarder
  la validation.
- **Xiaomi / MIUI** : sans « démarrage automatique » et « pas de restriction de
  batterie », le téléphone peut ne pas réveiller l'app à la déconnexion Bluetooth.
- **Vérification des développeurs Android** : en 2027, Google étendra au monde entier
  l'obligation d'être un développeur vérifié, même pour un APK en lien direct.
- **Textes arabes** : à relire par un arabophone avant publication.

## Pour modifier un texte de l'app

Modifier la table dans `tools/gen_strings.py`, puis lancer :

```
python3 tools/gen_strings.py app/src/main/res
```
