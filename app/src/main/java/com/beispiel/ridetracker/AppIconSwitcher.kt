package com.beispiel.ridetracker

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconSwitcher {

    private val allBrands = listOf(
        "Kawasaki", "Ducati", "Yamaha", "KTM", "Honda",
        "BMW", "Triumph", "Husqvarna", "Cyan", "White"
    )

    fun switch(context: Context, colorName: String) {
        val pm = context.packageManager
        val pkg = context.packageName
        val target = normalise(colorName)

        // Enable target first so the launcher always has one active entry
        val enableName = ComponentName(pkg, "$pkg.icon.$target")
        pm.setComponentEnabledSetting(
            enableName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable all others
        for (brand in allBrands) {
            if (brand != target) {
                pm.setComponentEnabledSetting(
                    ComponentName(pkg, "$pkg.icon.$brand"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    // Map legacy / display names to alias suffix names
    private fun normalise(name: String) = when (name) {
        "Kawasaki", "Kawasaki Green" -> "Kawasaki"
        "Ducati", "Ducati Red"       -> "Ducati"
        "Yamaha", "Yamaha Blue"      -> "Yamaha"
        "KTM"                        -> "KTM"
        "Honda"                      -> "Honda"
        "BMW"                        -> "BMW"
        "Triumph"                    -> "Triumph"
        "Husqvarna"                  -> "Husqvarna"
        "Cyan"                       -> "Cyan"
        "White", "Pure White"        -> "White"
        else                         -> "Kawasaki"
    }
}
