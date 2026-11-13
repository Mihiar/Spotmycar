# SpotMyCar : document de contexte du projet

> Document vivant. À mettre à jour à chaque décision. Dernière mise à jour : 13 novembre 2026.

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
| Direction visuelle | « Spotmy Car » (Claude Design) : terracotta chaleureux, thèmes clair (fond `#F8F4EE`) et sombre (fond `#1B1714`), accent `#E8794F`, police Outfit (+ JetBrains Mono pour les libellés), boutons en pilule ; guidage « 4c Ligne droite » | Appliquée le 13 novembre 2026 ; remplace « SpotMyCar Prototype v2 » (clair bleu) du même jour |
| Identifiant de l'app | `app.spotmycar.android` | Définitif une fois publié sur Google Play |
| Android minimum | Android 8.0 (API 26) ; cible Android 16 (API 36) | API 36 est exigée par Google Play pour toute nouvelle app |

## Principes du produit

1. **Le GPS absent est le cas normal.** Étage, zone et photo sont le fonctionnement
   principal, pas un plan B.
2. **Zéro effort.** L'enregistrement automatique par Bluetooth est ce qui distingue
   l'app : les concurrents échouent parce qu'ils demandent un geste au moment où
   l'on est le moins motivé.
3. **Automatique par défaut.** Après « Park my car », aucune saisie : le téléphone écoute
   ses capteurs. Le mode de secours ne s'affiche que si la fiabilité est trop basse, et ne
   demande qu'un seul geste (confirmer l'étage ou prendre une photo).
4. **La fiabilité est toujours visible** : un pourcentage à l'enregistrement, une pastille
   « Direction fiable » ou « Direction estimée · ± N m » pendant le guidage.
5. **Règle du pic et de la fin.** L'écran « Trouvée ! » clôt le parcours sur une note heureuse.
6. **Deux gestes sur l'accueil** : « Park my car » et « Find my car ». Celle qui sert est mise
   en avant (terracotta sans voiture enregistrée, couleur encre avec).
7. **Terracotta chaleureux, clair et sombre** (suit le réglage du téléphone), cartes très arrondies, gros chiffres.

## Ce que fait la v1

- **Lancement** : la spirale, le nom, la phrase d'accroche (une seconde et demie).
- **Introduction** (une fois) : trois pages — la place s'enregistre seule ; niveau, zone, place ; on vous ramène.
- **Accueil** (onglet) : les deux grandes cartes « Park my car » et « Find my car ». Avec une voiture
  enregistrée : ses repères, sa fiabilité, la photo, « Modifier la place », partage, et les trois dernières places.
- **Park** : « Détection automatique » (GPS, Bluetooth de la voiture, baromètre : l'état réel de chaque
  signal) → « Voiture localisée automatiquement » avec le pourcentage de fiabilité, ou « Position
  incertaine » (mode de secours) → « Confirmez l'étage » (niveau, zone, numéro, photo, repère ; tout est
  facultatif) → « Votre voiture est enregistrée ! » avec les sources utilisées.
- **Find** : « Recherche de votre voiture » (rappel des repères et de la fiabilité) → guidage « Ligne droite » :
  des pas en ligne droite vers une voiture vue de dessus (un pas n'apparaît que là où l'on est déjà passé,
  la ligne tourne vers la voiture), mètres restants et temps à pied (1,2 m/s), pastille de fiabilité,
  changement d'étage estimé par le baromètre, itinéraire Google Maps, photo → arrivée : la voiture pulse,
  ses phares clignotent, le téléphone vibre → « Trouvée ! ».
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
  location/Barometer.kt        baromètre : pression près de la voiture, écart de niveaux au retour
  location/Reliability.kt      score de fiabilité (0 à 100) et seuil du mode de secours
  ui/theme/Theme.kt            palettes claire / sombre et polices de la direction « Spotmy Car » (terracotta)
  ui/Common.kt, Widgets.kt     briques visuelles : boutons, cartes, spirale, lignes de signal, fiabilité
  ui/HomeScreen.kt             accueil : cartes « Park my car » et « Find my car »
  ui/ParkFlowScreens.kt        lancement, détection auto, voiture localisée, mode de secours, enregistrée
  ui/FindScreens.kt            recherche, « Trouvée ! »
  ui/GuideScreen.kt            guidage « Ligne droite » : pas et voiture dessinés, fiabilité, changement d'étage
  ui/…                         Introduction, repères (SaveScreen), Historique, Alertes, Réglages,
                               photo et partage (Overlays.kt)
app/src/main/res/font/         Outfit et JetBrains Mono (licences : licenses/Outfit-OFL.txt, licenses/JetBrainsMono-OFL.txt)
                               (les fichiers Plus Jakarta Sans ne servent plus et peuvent être supprimés)
app/src/main/res/values*/strings.xml   textes EN / FR / AR (générés par tools/gen_strings.py)
docs/                          page web + politique de confidentialité (GitHub Pages)
.github/workflows/build.yml    compilation automatique et publication de l'APK
```

## État

- [x] Code de l'app écrit (v1 complète)
- [x] Page web et politique de confidentialité rédigées
- [x] Compilation automatique préparée
- [ ] **Première compilation** (le code n'a pas encore été compilé : à faire sur GitHub)
- [x] Nouvelle interface : « SpotMyCar Prototype v2 » appliqué (13 nov. 2026) : thème clair bleu, parcours Park / Find,
      baromètre et score de fiabilité. **Pas encore compilé ni testé sur téléphone.**
- [x] Nouvelle direction : « Spotmy Car » appliquée (13 nov. 2026) : terracotta clair + sombre, Outfit, guidage
      « 4c Ligne droite ». **Pas encore compilé ni testé sur téléphone.** Minuteur de stationnement (écrans 1a–1c)
      pas encore fait.
- [ ] Test sur le Xiaomi : parcours manuel, parking sans GPS, Bluetooth automatique, alertes, boussole,
      baromètre (changement d'étage) et mode de secours
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
- **Textes arabes** : à relire par un arabophone avant publication (y compris les nouveaux textes du 13 novembre).
- **Baromètre** : tous les téléphones n'en ont pas, et la pression change avec la météo. L'écart de
  niveaux n'est donc affiché que si la voiture a été garée il y a moins de deux heures, et il est
  présenté comme une estimation. La valeur « 0,36 hPa par niveau » (environ 3 m) est à vérifier sur le terrain.
- **Fiabilité** : le pourcentage est un score maison (GPS, Bluetooth, baromètre, repères confirmés), pas
  une mesure physique. Les seuils sont dans `location/Reliability.kt` et devront être ajustés après les tests.
- **Non repris du prototype v2** : le signal Wi-Fi (il faudrait scanner les réseaux, donc une autorisation
  de plus, et aucune carte des Wi-Fi de parking n'existe), le scan de QR code ou de pilier, et les photos
  d'illustration (remplacées par des dessins). Les pas du guidage sont une animation qui indique la
  direction ; ils ne comptent pas les vrais pas.
- **Page web** (`docs/`) : encore aux couleurs de l'ancien style sombre.

## Pour modifier un texte de l'app

Modifier la table dans `tools/gen_strings.py`, puis lancer :

```
python3 tools/gen_strings.py app/src/main/res
```
