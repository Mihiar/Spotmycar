# SpotMyCar : document de contexte du projet

> Document vivant. À mettre à jour à chaque décision. Dernière mise à jour : 12 novembre 2026.

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
| Direction visuelle | Prototype 3a « sombre technique » (Claude Design) : graphite `#131519`, ambre `#F5A524`, polices Barlow et Barlow Condensed | Choisie le 12 novembre 2026 ; lisible au fond d'un parking mal éclairé |
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
5. **Actions principales empilées verticalement** ; seul le petit bouton « partager » se place à côté de « Modifier la place ».
6. **Animation « pin drop ».** L'épingle tombe et une onde part quand on se gare ;
   les ondes reviennent et l'épingle saute quand on retrouve la voiture.
7. **Thème toujours sombre**, ambre pour ce qui compte (niveau, bouton principal), gris doux pour le reste. Gros chiffres, contrastes forts.

## Ce que fait la v1

- **Introduction** (une fois) : trois pages — la place s'enregistre seule ; niveau, zone, place ; on vous ramène.
- **Ma voiture** (onglet) : sans place, « Enregistrer ma place » (l'épingle tombe, la place est notée
  tout de suite, la position GPS arrive ensuite). Avec une place : niveau en très grand, zone, place,
  photo de repère, dernier signal GPS, mode d'enregistrement, repère, « Me guider vers la voiture »,
  « Modifier la place », partage, et les trois dernières places.
- **Nouvelle place** : niveau (3 à -5) et zone (A à H) en un appui, numéro de place, photo du panneau,
  repère. Tout est facultatif ; « Annuler » juste après avoir noté une place l'efface.
- **Retour à la voiture** : flèche-boussole et distance en direct quand il y a du GPS ; niveaux du
  parking, étapes (niveau, zone, place, repère) ; itinéraire Google Maps ; « Je suis à ma voiture ».
- **Historique** (onglet) : places du mois, durée moyenne, liste des places précédentes (sans photo, 50 au plus).
- **Alertes** (onglet) : parking qui ferme (rappel 15 min avant), durée maximale, stationnement gratuit,
  et l'enregistrement automatique par coupure Bluetooth. Alarmes programmées sur le téléphone.
- **Partage** : message prêt (lien de carte, niveau, zone, place, repère) à copier, ou à envoyer par
  WhatsApp, SMS ou une autre app (avec la photo).
- **Réglages** : mise en route de l'automatique en étapes à cocher, aide Xiaomi / Oppo / Huawei,
  politique de confidentialité, revoir l'introduction.

## Plan des fichiers

```
app/src/main/java/app/spotmycar/android/
  MainActivity.kt              l'unique écran Android
  data/ParkingSpot.kt          ce qu'est une « place » (niveau, zone, place, repère, photo, position)
  data/ParkingStore.kt         la place en cours
  data/HistoryStore.kt         l'historique des places précédentes
  data/AppSettings.kt          réglages (automatique, introduction, alertes)
  location/LocationFetcher.kt  obtenir la position sans bloquer
  location/Guidance.kt         position en direct et boussole pour le guidage
  location/PlaceNamer.kt       nom du lieu (« Carrefour Anfa »), si Internet le permet
  bluetooth/CarBluetoothReceiver.kt   LE cœur : déconnexion Bluetooth -> place notée
  bluetooth/BluetoothHelper.kt appareils associés, état de l'automatique
  alerts/AlertScheduler.kt     programme les alertes (fermeture, durée, gratuité)
  alerts/AlertReceiver.kt      affiche l'alerte ; reprogramme après redémarrage
  notify/Notifier.kt           notifications
  util/Photos.kt, Actions.kt, SpotText.kt   photos ; cartes, partage ; petites phrases
  ui/theme/Theme.kt            couleurs et polices de la direction « sombre technique »
  ui/…                         Introduction, Ma voiture, Nouvelle place, Retour, Historique,
                               Alertes, Réglages, photo et partage (Overlays.kt)
app/src/main/res/font/         Barlow et Barlow Condensed (licence : licenses/Barlow-OFL.txt)
app/src/main/res/values*/strings.xml   textes EN / FR / AR (générés par tools/gen_strings.py)
docs/                          page web + politique de confidentialité (GitHub Pages)
.github/workflows/build.yml    compilation automatique et publication de l'APK
```

## État

- [x] Code de l'app écrit (v1 complète)
- [x] Page web et politique de confidentialité rédigées
- [x] Compilation automatique préparée
- [ ] **Première compilation** (le code n'a pas encore été compilé : à faire sur GitHub)
- [x] Nouvelle interface : prototype 3a « sombre technique » appliqué (12 nov. 2026)
- [ ] Test sur le Xiaomi : parcours manuel, parking sans GPS, Bluetooth automatique, alertes, boussole
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
