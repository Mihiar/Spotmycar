# SpotMyCar : ce qu'il reste à faire pour lancer

> Mis à jour le 16 novembre 2026. Les étapes sont dans l'ordre. Chacune dit qui la fait et combien de temps elle prend.

## Où on en est

Le code est complet et compile. Ce qui reste ne peut pas être fait par du code : il faut ton téléphone,
ta clé secrète, ton identité et ton moyen de paiement.

| # | Étape | Qui | Durée |
|---|---|---|---|
| 1 | Tester sur le Xiaomi | Toi | 1 à 2 h, dont un vrai parking souterrain |
| 2 | Créer la clé de signature | Toi (jamais Claude : elle doit rester secrète) | 15 min |
| 3 | Publier la v1.0.0 en lien direct | Toi (1 clic) ou Claude | 5 min |
| 4 | Mettre la page web en ligne | Toi | 2 min |
| 5 | Faire relire l'arabe | Un proche arabophone | 30 min |
| 6 | Ouvrir le compte Google Play | Toi | 30 min + délai de vérification |
| 7 | Test fermé : 12 testeurs, 14 jours | Toi + testeurs | 14 jours |
| 8 | Demander la mise en production | Toi | 1 h + délai d'examen |

Les étapes 1 à 4 suffisent pour que des gens téléchargent l'app. Les étapes 6 à 8 concernent seulement Google Play.

## 1. Tester sur le Xiaomi

Installer la version de test : sur le téléphone, ouvrir
<https://github.com/Mihiar/spotmycar/releases/tag/test>, télécharger `SpotMyCar-test.apk`, l'ouvrir.

Avant de tester l'automatique, deux réglages propres à Xiaomi (sinon le téléphone endort l'app) :

- Paramètres > Applications > SpotMyCar > **Démarrage automatique** : activé.
- Paramètres > Applications > SpotMyCar > Économie de batterie : **Aucune restriction**.

À cocher :

- [ ] Introduction, puis écrans d'autorisations : chaque refus et chaque accord se passent bien.
- [ ] Accueil : les deux boutons ronds s'affichent bien, rien n'est coupé (français, anglais, arabe).
- [ ] « Park my car » dehors : la position est trouvée, le pourcentage de fiabilité s'affiche.
- [ ] « Park my car » **en sous-sol** : le mode de secours propose l'étage ou la photo.
- [ ] Photo, niveau, zone, numéro, repère : enregistrés et relus correctement.
- [ ] Bluetooth : couper le contact de la voiture, vérifier la notification « Place enregistrée ».
- [ ] Bluetooth après un redémarrage du téléphone, app jamais ouverte depuis.
- [ ] « Find my car » : la boussole pointe du bon côté, la distance diminue en marchant.
- [ ] Changement d'étage (baromètre) : monter ou descendre d'un niveau, lire l'estimation.
- [ ] Alertes (Réglages > Alertes) : une alerte de durée réglée à quelques minutes sonne bien.
- [ ] Partage WhatsApp : le message et la photo arrivent.
- [ ] Onglets : Ma voiture, Historique, Réglages. L'historique n'apparaît plus sur l'accueil.

Pour chaque souci : une capture d'écran et une phrase. Claude corrige à partir de là.

## 2. Créer la clé de signature

La clé prouve que les mises à jour viennent de toi. **Si elle est perdue, plus aucune mise à jour n'est
possible** pour les personnes qui ont installé le lien direct. Elle ne doit jamais aller sur GitHub ni dans une conversation.

Dans Android Studio (sur l'ordinateur) :

1. Menu **Build > Generate Signed App Bundle / APK…**, choisir **APK**, puis **Next**.
2. **Create new…** : choisir l'emplacement et le nom `spotmycar-release.jks` (en dehors du dossier du projet).
3. Inventer un mot de passe solide. Mettre le même pour « Key store » et pour « Key ».
   Alias : `spotmycar`. Validité : 25 ans. Nom : le tien. Valider, puis fermer la fenêtre (inutile d'aller plus loin).
4. Copier le fichier `.jks` à deux endroits sûrs (clé USB + coffre de mots de passe, par exemple), avec le mot de passe.

Donner la clé à GitHub, pour qu'il signe à ta place :

1. Transformer le fichier en texte.
   - Windows (PowerShell) : `[Convert]::ToBase64String([IO.File]::ReadAllBytes("spotmycar-release.jks")) | Set-Clipboard`
   - Mac : `base64 -i spotmycar-release.jks | pbcopy`
2. Sur GitHub : dépôt > **Settings > Secrets and variables > Actions > New repository secret**. Créer quatre secrets :

| Nom | Valeur |
|---|---|
| `KEYSTORE_BASE64` | le texte copié à l'étape précédente |
| `KEYSTORE_PASSWORD` | le mot de passe |
| `KEY_ALIAS` | `spotmycar` |
| `KEY_PASSWORD` | le même mot de passe |

## 3. Publier la v1.0.0 en lien direct

Une fois les tests bons et les quatre secrets créés : sur GitHub, **Releases > Draft a new release**,
dans « Choose a tag » taper `v1.0.0` puis « Create new tag », et **Publish release**.
GitHub compile, signe et attache `SpotMyCar.apk` en quelques minutes. (Claude peut aussi poser l'étiquette sur demande.)

Lien à partager ensuite : `https://github.com/Mihiar/spotmycar/releases/latest/download/SpotMyCar.apk`

Le fichier pour Google Play (`SpotMyCar.aab`) est dans **Actions >** la compilation de `v1.0.0` **> Artifacts > SpotMyCar-google-play**.

## 4. Mettre la page web en ligne

Dépôt > **Settings > Pages** > Source : **Deploy from a branch** > Branche `main`, dossier `/docs` > **Save**.
Adresse : <https://mihiar.github.io/spotmycar/>. Politique de confidentialité : `…/spotmycar/privacy.html`
(Google Play demande cette adresse).

À décider avant : une **adresse e-mail publique de contact**. Google Play en exige une, et la politique de
confidentialité doit dire comment te joindre. Une adresse dédiée (par exemple créée pour SpotMyCar) évite
d'exposer ta boîte personnelle. Elle se règle dans `docs/index.html` et `docs/privacy.html` (`contactEmail`).

## 5. Relecture de l'arabe

Les textes arabes ont été écrits par Claude et jamais relus par un arabophone. La liste complète est dans
`tools/gen_strings.py` (quatrième colonne). Corrections : les donner à Claude, ou modifier ce fichier.

## 6. Compte Google Play

1. <https://play.google.com/console/signup> : compte **personnel**, frais uniques de 25 USD.
   [Confiance moyenne : montant et règles à vérifier sur la page d'inscription, Google les fait évoluer.]
2. Vérification d'identité (pièce d'identité) et d'un téléphone Android. Compter quelques jours.
3. Créer l'app : nom « SpotMyCar », langue par défaut français, application gratuite.
4. À la première mise en ligne, dans « Signature d'application », choisir d'**utiliser ta propre clé**
   (celle de l'étape 2). Sinon les personnes passées par le lien direct ne pourront pas basculer sur la version Play.

## 7. Test fermé

Un compte personnel récent doit faire tester l'app par **12 personnes pendant 14 jours d'affilée** avant de
pouvoir demander la production. Créer une liste d'e-mails Gmail dans la Play Console, envoyer le lien
d'inscription, et s'assurer que les 12 restent inscrites les 14 jours.

## 8. Mise en production

Formulaires à remplir dans la Play Console :

- **Position en arrière-plan** : c'est le point le plus surveillé. Explication à coller (anglais demandé) :
  *"SpotMyCar saves where the car is parked at the moment the phone disconnects from the car's Bluetooth.
  This happens while the app is closed, so the location must be read in the background. The location is
  stored only on the device and is never sent to a server."* Joindre une vidéo de 30 secondes : l'écran qui
  explique l'autorisation, puis la coupure Bluetooth et la notification « Place enregistrée ».
- **Sécurité des données** : aucune donnée collectée ni partagée ; tout reste sur le téléphone.
- **Public cible** : 18 ans et plus. **Publicités** : non. **Classification du contenu** : questionnaire, tout à « non ».
- **Pays** : Maroc d'abord.

### Fiche du Store (prête à coller)

**Nom** : SpotMyCar

**Description courte** (80 caractères max)

- FR : Retrouvez votre voiture, même en parking souterrain sans GPS.
- EN : Find your parked car, even underground where GPS fails.
- AR : اعثر على سيارتك حتى في المواقف تحت الأرض بدون GPS. *(à relire)*

**Description complète (FR)**

> Vous sortez du centre commercial et vous ne savez plus à quel étage vous êtes garé ? SpotMyCar note la
> place à votre place.
>
> • Automatique : dès que le téléphone quitte le Bluetooth de la voiture, la place est enregistrée.
> • Pensé pour les parkings couverts : niveau, zone, numéro de place, photo et repère, là où le GPS ne passe pas.
> • Retour guidé : direction, distance et estimation du changement d'étage.
> • Alertes : fermeture du parking, durée maximale, fin de la gratuité.
> • Partage en un geste par WhatsApp ou SMS.
>
> Gratuit, sans compte et sans publicité. Tout reste sur votre téléphone : aucune donnée n'est envoyée.
> Disponible en français, en anglais et en arabe.

**Description complète (EN)**

> Walking out of the mall with no idea which floor you parked on? SpotMyCar remembers for you.
>
> • Automatic: the spot is saved the moment your phone disconnects from the car's Bluetooth.
> • Built for covered car parks: level, zone, bay number, photo and landmark, where GPS does not reach.
> • Guided return: direction, distance and an estimate of the floor change.
> • Alerts: car park closing time, maximum stay, end of free parking.
> • One-tap sharing by WhatsApp or SMS.
>
> Free, no account, no ads. Everything stays on your phone: nothing is sent anywhere.
> Available in English, French and Arabic.

**Images à fournir** : icône 512 × 512, bannière 1024 × 500, et au moins deux captures d'écran du téléphone
(à prendre pendant les tests de l'étape 1 : accueil, détection, guidage, « Trouvée ! »).
