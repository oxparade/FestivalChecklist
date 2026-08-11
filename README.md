# Festival Checklist

Application Android locale et sans compte pour préparer un festival chaque année.

## Fonctions
- liste préremplie pour 4 jours / 4 nuits ;
- cases cochables persistantes ;
- compteur de progression ;
- ajout et suppression d'éléments ;
- ajout et suppression de catégories ;
- bouton **Nouveau festival** : décoche tout sans effacer la liste ;
- aucune permission Internet, aucune collecte de données.

## Compiler
Ouvrir le dossier dans Android Studio, laisser Gradle synchroniser, puis :

`Build > Build APK(s)`

ou en ligne de commande :

`./gradlew assembleDebug`

APK produit : `app/build/outputs/apk/debug/app-debug.apk`
