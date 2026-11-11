#!/usr/bin/env python3
"""Génère les trois fichiers strings.xml (en, fr, ar) depuis une seule table,
pour garantir que chaque texte existe dans les trois langues."""
import sys, pathlib

S = [
 # key, en, fr, ar
 ("park_title", "Just parked?", "Vous venez de vous garer ?", "هل ركنت سيارتك للتو؟"),
 ("park_button", "PARK HERE", "GARER ICI", "اركن هنا"),
 ("park_hint", "One tap saves your spot. Floor and photo are optional.", "Un appui enregistre votre place. Étage et photo en option.", "ضغطة واحدة تحفظ مكانك. الطابق والصورة اختياريان."),
 ("park_auto_on", "Automatic save is on: nothing to do when you park.", "Enregistrement automatique actif : rien à faire en vous garant.", "الحفظ التلقائي مفعّل: لا شيء تفعله عند الركن."),
 ("park_auto_cta", "Save automatically with Bluetooth", "Enregistrer automatiquement via Bluetooth", "الحفظ تلقائياً عبر البلوتوث"),
 ("settings_title", "Settings", "Réglages", "الإعدادات"),
 ("details_title", "Spot saved", "Place enregistrée", "تم حفظ المكان"),
 ("details_subtitle", "Add a landmark to find it faster. It's optional.", "Ajoutez un repère pour la retrouver plus vite. C'est facultatif.", "أضف علامة لتجد سيارتك أسرع. هذا اختياري."),
 ("gps_searching", "Looking for a GPS signal…", "Recherche du signal GPS…", "جارٍ البحث عن إشارة GPS…"),
 ("gps_precise", "GPS position saved (±%1$d m)", "Position GPS enregistrée (± %1$d m)", "تم حفظ موقع GPS (±%1$d م)"),
 ("gps_rough", "Approximate position (±%1$d m)", "Position approximative (± %1$d m)", "موقع تقريبي (±%1$d م)"),
 ("gps_last_known", "Approximate position: last GPS signal, often the car park entrance.", "Position approximative : dernier signal GPS, souvent l'entrée du parking.", "موقع تقريبي: آخر إشارة GPS، غالباً عند مدخل الموقف."),
 ("gps_none", "No GPS here. A photo or the floor will do the job.", "Pas de GPS ici. Une photo ou l'étage feront l'affaire.", "لا توجد إشارة GPS هنا. صورة أو رقم الطابق يكفيان."),
 ("photo_take", "Take a photo", "Prendre une photo", "التقاط صورة"),
 ("photo_retake", "Retake the photo", "Reprendre la photo", "إعادة التقاط الصورة"),
 ("photo_hint", "Pillar number, sign, colour of the level…", "Numéro de pilier, panneau, couleur du niveau…", "رقم العمود، لافتة، لون الطابق…"),
 ("photo_description", "Photo of the parking spot", "Photo de la place", "صورة مكان الركن"),
 ("floor_label", "Floor / zone", "Étage / zone", "الطابق / المنطقة"),
 ("floor_placeholder", "e.g. -2, B14", "ex. -2, B14", "مثال: -2، B14"),
 ("note_label", "Note", "Note", "ملاحظة"),
 ("note_placeholder", "e.g. near the lift", "ex. près de l'ascenseur", "مثال: قرب المصعد"),
 ("action_done", "Done", "Terminé", "تم"),
 ("find_title", "My car", "Ma voiture", "سيارتي"),
 ("find_parked", "Parked %1$s", "Garée %1$s", "رُكنت %1$s"),
 ("find_parked_at", "at %1$s", "à %1$s", "الساعة %1$s"),
 ("time_just_now", "just now", "à l'instant", "الآن"),
 ("find_no_details", "No landmark yet.", "Aucun repère pour l'instant.", "لا توجد علامة بعد."),
 ("find_add_details", "Add floor, note or photo", "Ajouter étage, note ou photo", "إضافة طابق أو ملاحظة أو صورة"),
 ("find_navigate", "Take me there", "M'y conduire", "خذني إليها"),
 ("find_share", "Share the location", "Partager l'emplacement", "مشاركة الموقع"),
 ("find_edit", "Edit", "Modifier", "تعديل"),
 ("find_found", "I found my car", "J'ai retrouvé ma voiture", "وجدت سيارتي"),
 ("find_auto_badge", "Saved automatically", "Enregistrée automatiquement", "حُفظت تلقائياً"),
 ("share_with_coords", "🚗 My car is parked here: %1$s", "🚗 Ma voiture est garée ici : %1$s", "🚗 سيارتي مركونة هنا: %1$s"),
 ("share_without_coords", "🚗 Where my car is parked:", "🚗 Où ma voiture est garée :", "🚗 مكان ركن سيارتي:"),
 ("share_floor", "Floor / zone: %1$s", "Étage / zone : %1$s", "الطابق / المنطقة: %1$s"),
 ("share_note", "Note: %1$s", "Note : %1$s", "ملاحظة: %1$s"),
 ("share_chooser", "Share with", "Partager avec", "مشاركة مع"),
 ("error_no_maps", "No maps app found.", "Aucune application de cartes trouvée.", "لم يُعثر على تطبيق خرائط."),
 ("error_no_camera", "No camera app found.", "Aucune application appareil photo trouvée.", "لم يُعثر على تطبيق كاميرا."),
 ("auto_title", "Automatic save", "Enregistrement automatique", "الحفظ التلقائي"),
 ("auto_description", "SpotMyCar saves your spot by itself when your phone disconnects from your car's Bluetooth. Nothing to do when you park.", "SpotMyCar note votre place tout seul quand votre téléphone se déconnecte du Bluetooth de votre voiture. Rien à faire en vous garant.", "يحفظ SpotMyCar مكانك تلقائياً عندما ينفصل هاتفك عن بلوتوث سيارتك. لا شيء تفعله عند الركن."),
 ("auto_switch", "Save my spot automatically", "Enregistrer ma place automatiquement", "حفظ مكاني تلقائياً"),
 ("auto_active", "Active. Nothing more to do.", "Actif. Plus rien à faire.", "مفعّل. لا شيء آخر مطلوب."),
 ("auto_incomplete", "Finish the steps below to activate it.", "Terminez les étapes ci-dessous pour l'activer.", "أكمل الخطوات التالية لتفعيله."),
 ("step_bluetooth_title", "Allow Bluetooth access", "Autoriser l'accès au Bluetooth", "السماح بالوصول إلى البلوتوث"),
 ("step_bluetooth_desc", "So the app can recognise your car.", "Pour que l'application reconnaisse votre voiture.", "ليتعرّف التطبيق على سيارتك."),
 ("step_car_title", "Choose your car", "Choisir votre voiture", "اختر سيارتك"),
 ("step_car_desc", "Pick your car's Bluetooth in the list.", "Sélectionnez le Bluetooth de votre voiture dans la liste.", "اختر بلوتوث سيارتك من القائمة."),
 ("step_car_none", "No paired device found. Pair your phone with your car's Bluetooth first, then come back here.", "Aucun appareil associé. Associez d'abord votre téléphone au Bluetooth de votre voiture, puis revenez ici.", "لا يوجد جهاز مقترن. اقرن هاتفك ببلوتوث سيارتك أولاً ثم عد إلى هنا."),
 ("step_car_open_bluetooth", "Open Bluetooth settings", "Ouvrir les réglages Bluetooth", "فتح إعدادات البلوتوث"),
 ("step_location_title", "Allow location \"all the time\"", "Autoriser la position « toujours »", "السماح بالموقع «طوال الوقت»"),
 ("step_location_desc", "So your spot is saved even when the app is closed.", "Pour noter la place même quand l'application est fermée.", "ليُحفظ مكانك حتى عندما يكون التطبيق مغلقاً."),
 ("step_notifications_title", "Allow notifications (optional)", "Autoriser les notifications (facultatif)", "السماح بالإشعارات (اختياري)"),
 ("step_notifications_desc", "To confirm that your spot was saved.", "Pour vous confirmer que la place est notée.", "لتأكيد حفظ مكانك."),
 ("action_allow", "Allow", "Autoriser", "سماح"),
 ("disclosure_title", "Location in the background", "Position en arrière-plan", "الموقع في الخلفية"),
 ("disclosure_body",
  "SpotMyCar collects location data to automatically save where your car is parked when your phone disconnects from your car's Bluetooth, even when the app is closed or not in use.\n\nYour location stays on your phone. It is never sent to a server and never shared.\n\nOn the next screen, choose \"Allow all the time\".",
  "SpotMyCar collecte des données de localisation pour enregistrer automatiquement l'emplacement de votre voiture quand votre téléphone se déconnecte du Bluetooth de votre voiture, même lorsque l'application est fermée ou non utilisée.\n\nVotre position reste sur votre téléphone. Elle n'est jamais envoyée à un serveur ni partagée.\n\nSur l'écran suivant, choisissez « Toujours autoriser ».",
  "يجمع SpotMyCar بيانات الموقع ليحفظ تلقائياً مكان ركن سيارتك عندما ينفصل هاتفك عن بلوتوث سيارتك، حتى عندما يكون التطبيق مغلقاً أو غير مستخدم.\n\nيبقى موقعك على هاتفك. لا يُرسل أبداً إلى أي خادم ولا يُشارك.\n\nفي الشاشة التالية، اختر «السماح طوال الوقت»."),
 ("disclosure_accept", "Continue", "Continuer", "متابعة"),
 ("disclosure_decline", "No thanks", "Non merci", "لا، شكراً"),
 ("reliability_title", "Reliability on Xiaomi, Oppo, Huawei…", "Fiabilité sur Xiaomi, Oppo, Huawei…", "الموثوقية على Xiaomi وOppo وHuawei…"),
 ("reliability_description", "Some phones stop apps in the background. For automatic save to work every time, allow autostart and remove battery restrictions for SpotMyCar.", "Certains téléphones arrêtent les applications en arrière-plan. Pour que l'enregistrement automatique marche à chaque fois, autorisez le démarrage automatique et retirez les restrictions de batterie pour SpotMyCar.", "بعض الهواتف توقف التطبيقات في الخلفية. ليعمل الحفظ التلقائي في كل مرة، اسمح بالتشغيل التلقائي وأزل قيود البطارية عن SpotMyCar."),
 ("reliability_autostart", "Allow autostart", "Autoriser le démarrage automatique", "السماح بالتشغيل التلقائي"),
 ("reliability_battery", "Remove battery restrictions", "Retirer les restrictions de batterie", "إزالة قيود البطارية"),
 ("action_app_settings", "Open app settings", "Ouvrir les paramètres de l'application", "فتح إعدادات التطبيق"),
 ("about_title", "About", "À propos", "حول التطبيق"),
 ("about_summary", "Everything stays on your phone: no account, no server, no ads.", "Tout reste sur votre téléphone : pas de compte, pas de serveur, pas de publicité.", "كل شيء يبقى على هاتفك: بلا حساب، بلا خادم، بلا إعلانات."),
 ("about_version", "Version %1$s", "Version %1$s", "الإصدار %1$s"),
 ("about_privacy", "Privacy policy", "Politique de confidentialité", "سياسة الخصوصية"),
 ("notification_channel", "Parking spot", "Place de parking", "مكان الركن"),
 ("notification_saved_title", "Spot saved 📍", "Place enregistrée 📍", "تم حفظ المكان 📍"),
 ("notification_saved_text", "Tap to add the floor or a photo.", "Touchez pour ajouter l'étage ou une photo.", "اضغط لإضافة الطابق أو صورة."),
 ("notification_saved_text_no_gps", "No GPS here. Tap to add the floor or a photo.", "Pas de GPS ici. Touchez pour ajouter l'étage ou une photo.", "لا توجد إشارة GPS. اضغط لإضافة الطابق أو صورة."),
 ("action_back", "Back", "Retour", "رجوع"),
]

def esc(text: str) -> str:
    text = text.replace("\\", "\\\\") if "\\" in text else text
    text = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    text = text.replace("'", "\\'").replace('"', '\\"')
    text = text.replace("\n", "\\n")
    return text

def render(col: int, header: str) -> str:
    out = ['<?xml version="1.0" encoding="utf-8"?>', f"<!-- {header} Fichier généré par tools/gen_strings.py : modifier la table, pas ce fichier. -->", "<resources>"]
    if col == 1:
        out.append('    <string name="app_name" translatable="false">SpotMyCar</string>')
    for row in S:
        out.append(f'    <string name="{row[0]}">{esc(row[col])}</string>')
    out.append("</resources>")
    return "\n".join(out) + "\n"

root = pathlib.Path(sys.argv[1])
keys = [r[0] for r in S]
assert len(keys) == len(set(keys)), "clé en double"
for r in S:
    assert all(r[i].strip() for i in (1, 2, 3)), f"texte manquant: {r[0]}"
    ph = [x for x in ("%1$s", "%1$d") if x in r[1]]
    for i in (2, 3):
        assert all(x in r[i] for x in ph), f"placeholder manquant: {r[0]}"
(root / "values/strings.xml").write_text(render(1, "English (langue par défaut)."), encoding="utf-8")
(root / "values-fr/strings.xml").write_text(render(2, "Français."), encoding="utf-8")
(root / "values-ar/strings.xml").write_text(render(3, "العربية."), encoding="utf-8")
print(len(keys), "textes x 3 langues")
