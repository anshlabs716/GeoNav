package org.geonav.app.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class GeoNavCarSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return GeoNavCarNavigationScreen(carContext)
    }
}
