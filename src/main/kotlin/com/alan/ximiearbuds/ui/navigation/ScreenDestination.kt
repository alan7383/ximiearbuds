package com.alan.ximiearbuds.ui.navigation

/**
 * Screen destinations matching the official Xiaomi Earbuds (com.mi.earphone) Fragment hierarchy.
 */
sealed class ScreenDestination(val title: String) {
    object MainSettings : ScreenDestination("Paramètres de l'appareil")
    object MoreSettings : ScreenDestination("Plus de paramètres")
    object CustomizedEq : ScreenDestination("Égaliseur personnalisé")
    object GestureControl : ScreenDestination("Commandes tactiles")
    object SoundEffects : ScreenDestination("Effets sonores")
    object FindDevice : ScreenDestination("Localiser les écouteurs")
    object FitDetection : ScreenDestination("Test d'ajustement")
    object EarboxSound : ScreenDestination("Sons du boîtier")
    object DeviceInfo : ScreenDestination("À propos de l'appareil")
    object AddDevice : ScreenDestination("Ajouter un appareil")
    object ScanDevice : ScreenDestination("Recherche des écouteurs")
}
