package com.pixubi.game

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** All notification preferences stay on the device. Opt-in by default. */
data class PixubiNotificationSettings(
    val enabled: Boolean = false,
    val quietHours: Boolean = true,
    val food: Boolean = true,
    val water: Boolean = true,
    val sleep: Boolean = true,
    val play: Boolean = true
)

class PixubiNotificationSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("pixubi_notification_settings", Context.MODE_PRIVATE)

    fun load() = PixubiNotificationSettings(
        enabled = prefs.getBoolean("enabled", false),
        quietHours = prefs.getBoolean("quietHours", true),
        food = prefs.getBoolean("food", true),
        water = prefs.getBoolean("water", true),
        sleep = prefs.getBoolean("sleep", true),
        play = prefs.getBoolean("play", true)
    )

    fun save(settings: PixubiNotificationSettings) {
        prefs.edit()
            .putBoolean("enabled", settings.enabled)
            .putBoolean("quietHours", settings.quietHours)
            .putBoolean("food", settings.food)
            .putBoolean("water", settings.water)
            .putBoolean("sleep", settings.sleep)
            .putBoolean("play", settings.play)
            .apply()
    }
}

enum class PixubiNeed { WATER, FOOD, SLEEP, PLAY }
data class PixubiNeedAlert(val need: PixubiNeed, val title: String, val message: String)

/** Pure rules are tested without Android services. */
object PixubiNeedRules {
    fun isQuietHour(hour: Int): Boolean = hour >= 22 || hour < 8

    fun findNeeds(pet: Pet, settings: PixubiNotificationSettings, awayMinutes: Long = 0L): List<PixubiNeedAlert> {
        if (!settings.enabled || !pet.alive || pet.mode == Mode.HIBERNATION) return emptyList()
        val name = pet.name
        val female = pet.sex == Sex.GIRL
        val alerts = mutableListOf<PixubiNeedAlert>()
        if (settings.water && pet.thirst <= 64.0) {
            alerts += PixubiNeedAlert(PixubiNeed.WATER, "💧 ${name} хоче водички",
                if (pet.waterStock > 0) "Мені хочеться пити! Напоїш мене? 💙"
                else "У мене скінчилася водичка. Зазирни до крамнички! 💧")
        }
        if (settings.food && pet.hunger <= 72.0) {
            alerts += PixubiNeedAlert(PixubiNeed.FOOD, "🍓 ${name} ${if (female) "зголодніла" else "зголоднів"}",
                if (pet.foodStock > 0) "У животі бурчить! Можна щось смачненьке? 🍓"
                else "Їжа закінчилася… Допоможеш мені поповнити запаси? 🥺")
        }
        if (pet.mode == Mode.AWAKE && settings.sleep && pet.energy <= 62.0) {
            alerts += PixubiNeedAlert(PixubiNeed.SLEEP, "💤 ${name} ${if (female) "втомилася" else "втомився"}",
                "Мої батарейки сідають. Хочу трохи поспати! 🌙")
        }
        if (pet.mode == Mode.AWAKE && settings.play && (pet.happiness <= 53.0 || awayMinutes >= 6 * 60) &&
            PetGrowth.stage(pet) !in listOf(GrowthStage.EGG, GrowthStage.COCOON)) {
            alerts += PixubiNeedAlert(PixubiNeed.PLAY, "🎮 ${name} скучає",
                if (awayMinutes >= 6 * 60)
                    "Привіт! Я за тобою скучив. Зайди, пограємося? 🥺💜"
                else "Мені нудно самому. Пограємося разом? 🥺💜")
        }
        return alerts
    }
}

/** WorkManager makes inexact periodic checks without a battery-draining service. */
object PixubiNeedsNotifier {
    private const val CHANNEL = "pixubi_pet_needs_v1"
    private const val UNIQUE_WORK = "pixubi_needs_periodic"
    private const val NOTICE_ID = 1011
    private const val MINUTE = 60_000L
    private const val HOUR = 60L * MINUTE
    const val ANY_COOLDOWN = 4L * HOUR
    const val KIND_COOLDOWN = 12L * HOUR

    @Volatile var isVisible: Boolean = false
        private set

    fun markVisible(visible: Boolean) { isVisible = visible }

    fun permissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Стан роботика PIXUBI", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Енергія, ремонт, синхронізація та місії" }
        )
    }

    fun sync(context: Context, settings: PixubiNotificationSettings) {
        val work = WorkManager.getInstance(context)
        if (!settings.enabled || !permissionGranted(context)) {
            work.cancelUniqueWork(UNIQUE_WORK)
            if (!settings.enabled) clear(context)
            return
        }
        createChannel(context)
        val request = PeriodicWorkRequestBuilder<PixubiNeedsWorker>(30, TimeUnit.MINUTES)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()
        work.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun markVisit(context: Context, now: Long = System.currentTimeMillis()) {
        context.getSharedPreferences("pixubi_notification_history", Context.MODE_PRIVATE)
            .edit().putLong("lastVisit", now).apply()
    }

    fun clear(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTICE_ID)
    }

    private fun post(context: Context, alert: PixubiNeedAlert) {
        if (!permissionGranted(context)) return
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_pixubi_notification)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTICE_ID, notification)
    }

    /** Explicit preview requested by the owner, independent of timers/quiet hours. */
    fun sendTest(context: Context, pet: Pet?) {
        if (!permissionGranted(context)) return
        post(context, PixubiNeedAlert(PixubiNeed.PLAY,
            "🐣 ${pet?.name ?: "Піксубі"} на зв'язку!",
            "Привіт! Це тест сповіщень. Я нагадаю, коли мені знадобишся 💜"))
    }

    fun sendWorldTest(context:Context){
        val name=WorldStorage.load(context)?.name?:"PIXUBI"
        post(context,PixubiNeedAlert(PixubiNeed.PLAY,"🤖 $name · системи активні","Тест каналу: енергія, корпус, синхронізація та місії."))
    }

    fun check(context: Context, now: Long = System.currentTimeMillis()) {
        if (isVisible || !permissionGranted(context)) return
        val settings = PixubiNotificationSettingsStore(context).load()
        if (!settings.enabled) return
        val h = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        if (settings.quietHours && PixubiNeedRules.isQuietHour(h)) return
        val history = context.getSharedPreferences("pixubi_notification_history", Context.MODE_PRIVATE)
        if (now - history.getLong("lastVisit", 0L) < 20L * MINUTE) return
        if (now - history.getLong("lastAny", 0L) < ANY_COOLDOWN) return
        val saved = WorldStorage.load(context) ?: return
        val simulated = WorldRules.tick(saved,now)
        val candidate=WorldNeedRules.find(saved,simulated,settings).firstOrNull{
            now-history.getLong("last_${it.need.name}",0L)>=KIND_COOLDOWN
        }?:return
        post(context, candidate)
        history.edit()
            .putLong("lastAny", now)
            .putLong("last_${candidate.need.name}", now)
            .apply()
    }
}

class PixubiNeedsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            PixubiNeedsNotifier.check(applicationContext)
            Result.success()
        } catch (error: Exception) {
            Result.retry()
        }
    }
}

internal object WorldNeedRules{
    fun find(saved:WorldPet,p:WorldPet,settings:PixubiNotificationSettings):List<PixubiNeedAlert>{
        if(!settings.enabled||p.mode==WorldMode.HIBERNATE)return emptyList()
        val result=mutableListOf<PixubiNeedAlert>()
        if(settings.food&&p.energy<=30&&p.mode==WorldMode.AWAKE)result+=PixubiNeedAlert(PixubiNeed.FOOD,"🔋 ${p.name} · низька енергія","Підключи енергокомірку або активуй відпочинок.")
        if(settings.water&&p.integrity<=55)result+=PixubiNeedAlert(PixubiNeed.WATER,"🛠 ${p.name} · ремонт","Цілісність корпусу ${p.integrity}%. Потрібен ремонтний модуль.")
        if(settings.play&&saved.mission!=null&&p.mission==null)result+=PixubiNeedAlert(PixubiNeed.PLAY,"📡 ${p.name} · місія завершена","Повернися до світу, щоб переглянути результат.")
        if(settings.sleep&&p.energy<=50&&p.mode==WorldMode.AWAKE&&p.mission==null)result+=PixubiNeedAlert(PixubiNeed.SLEEP,"🌙 ${p.name} · відпочинок","Активуй режим сну для відновлення енергії.")
        if(settings.play&&p.mood<=40&&p.mode==WorldMode.AWAKE)result+=PixubiNeedAlert(PixubiNeed.PLAY,"💠 ${p.name} · синхронізація","Підтримай стабільність цифрової свідомості.")
        return result
    }
}
