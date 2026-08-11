package com.personal.timetracker.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.personal.timetracker.data.db.AppDatabase
import com.personal.timetracker.data.entity.AttendanceEntity
import com.personal.timetracker.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object GeoHelper {
    private var lastAlertAt = 0L
    private const val ALERT_COOLDOWN_MS = 10 * 60 * 1000L // 10 min

    fun hasLocationPermission(ctx: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun lastLocation(ctx: Context): Location? {
        if (!hasLocationPermission(ctx)) return null
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (p in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            try {
                if (!lm.isProviderEnabled(p)) continue
                val loc = lm.getLastKnownLocation(p) ?: continue
                if (best == null || (loc.time > best.time)) best = loc
            } catch (_: Exception) {}
        }
        return best
    }

    /** Request a fresh location once (timeout 8s) then check workplace */
    @SuppressLint("MissingPermission")
    fun requestAndCheck(ctx: Context) {
        if (!hasLocationPermission(ctx)) {
            Log.w("PTT", "No location permission")
            return
        }
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val handler = Handler(Looper.getMainLooper())
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                try { lm.removeUpdates(this) } catch (_: Exception) {}
                checkWorkplace(ctx, location)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        try {
            val provider = when {
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                else -> null
            }
            if (provider != null) {
                lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                handler.postDelayed({
                    try { lm.removeUpdates(listener) } catch (_: Exception) {}
                    lastLocation(ctx)?.let { checkWorkplace(ctx, it) }
                }, 8000)
            } else {
                lastLocation(ctx)?.let { checkWorkplace(ctx, it) }
            }
        } catch (e: Exception) {
            Log.e("PTT", "request location", e)
            lastLocation(ctx)?.let { checkWorkplace(ctx, it) }
        }
    }

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toFloat()
    }

    fun checkWorkplace(ctx: Context, location: Location? = null) {
        CoroutineScope(Dispatchers.IO).launch { checkWorkplaceSuspend(ctx, location) }
    }

    /**
     * Suspend twin of [checkWorkplace] that completes before returning, so callers that need
     * to guarantee the check finished (e.g. a background WorkManager job) can await it instead
     * of firing-and-forgetting a detached coroutine that the OS might kill mid-flight.
     */
    suspend fun checkWorkplaceSuspend(ctx: Context, location: Location? = null) {
        try {
            val s = AppDatabase.get(ctx).settingsDao().get() ?: return
            if (s.workLat == 0.0 && s.workLng == 0.0) {
                Log.i("PTT", "Workplace not set")
                return
            }
            if (!s.geoAutoCheckIn && !s.geoAlertOnly && !s.geoAutoCheckOut) return
            val loc = location ?: lastLocation(ctx) ?: return
            val dist = distanceMeters(loc.latitude, loc.longitude, s.workLat, s.workLng)
            val inside = dist <= s.workRadiusMeters
            val active = AppDatabase.get(ctx).attendanceDao().getActive()
            val now = System.currentTimeMillis()
            Log.i("PTT", "Geo dist=${dist.toInt()}m inside=$inside active=${active != null}")

            if (inside && (active == null || active.exitTime != null)) {
                if (s.geoAutoCheckIn) {
                    AppDatabase.get(ctx).attendanceDao().insert(
                        AttendanceEntity(
                            date = TimeUtils.today(),
                            entryTime = TimeUtils.nowTime(),
                            status = "active"
                        )
                    )
                    NotifHelper.show(ctx, "ورود خودکار", "به محل کار رسیدید و ورود ثبت شد", NotifHelper.GEO_NOTIF_ID)
                } else if (s.geoAlertOnly && now - lastAlertAt > ALERT_COOLDOWN_MS) {
                    lastAlertAt = now
                    NotifHelper.show(
                        ctx,
                        "یادآوری ورود",
                        "در محدوده محل کار هستید (${dist.toInt()} متر) اما ورود ثبت نشده",
                        NotifHelper.GEO_NOTIF_ID
                    )
                }
            } else if (!inside && active != null && active.exitTime == null) {
                if (s.geoAutoCheckOut) {
                    try {
                        (ctx.applicationContext as App).repository.checkOut()
                        NotifHelper.show(ctx, "خروج خودکار", "از محل کار خارج شدید و خروج ثبت شد", NotifHelper.GEO_NOTIF_ID)
                    } catch (e: Exception) {
                        Log.e("PTT", "auto checkout", e)
                    }
                } else if ((s.geoAlertOnly || s.geoAutoCheckIn) && now - lastAlertAt > ALERT_COOLDOWN_MS) {
                    lastAlertAt = now
                    NotifHelper.show(
                        ctx,
                        "یادآوری خروج",
                        "محل کار را ترک کرده‌اید (${dist.toInt()} متر) اما خروج ثبت نشده",
                        NotifHelper.GEO_NOTIF_ID
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PTT", "geo check", e)
        }
    }
}
