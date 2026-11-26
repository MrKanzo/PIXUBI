package com.pixubi.game

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context=applicationContext
            val audioStore=remember{SoundSettingsStore(context)}
            val notificationStore=remember{PixubiNotificationSettingsStore(context)}
            var soundSettings by remember{mutableStateOf(audioStore.load())}
            var notificationSettings by remember{mutableStateOf(notificationStore.load())}
            var showSettings by remember{mutableStateOf(false)}
            var currentSeries by remember{mutableStateOf(WorldStorage.load(context)?.series?:RobotSeries.CLASSIC)}
            var auditionSeries by remember{mutableStateOf<RobotSeries?>(null)}
            val audio=remember{PixubiAudio(context)}
            val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){granted->
                if(granted){
                    notificationSettings=notificationSettings.copy(enabled=true)
                    notificationStore.save(notificationSettings)
                    PixubiNeedsNotifier.sync(context,notificationSettings)
                }else Toast.makeText(context,"Дозволь сповіщення в Android.",Toast.LENGTH_LONG).show()
            }
            fun saveNotifications(next:PixubiNotificationSettings){
                if(next.enabled&&!PixubiNeedsNotifier.permissionGranted(context)){
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS);return
                }
                notificationSettings=next;notificationStore.save(next);PixubiNeedsNotifier.sync(context,next)
            }
            LaunchedEffect(Unit){
                PixubiNeedsNotifier.createChannel(context)
                PixubiNeedsNotifier.sync(context,notificationSettings)
            }
            LaunchedEffect(soundSettings,currentSeries,auditionSeries){
                audio.configure(soundSettings,WorldCatalog.audio(auditionSeries?:currentSeries))
            }
            fun closeSettings(){auditionSeries=null;showSettings=false}
            BackHandler(enabled=showSettings){closeSettings()}
            val lifecycle=LocalLifecycleOwner.current
            DisposableEffect(lifecycle){
                val observer=LifecycleEventObserver{_,event->
                    when(event){
                        Lifecycle.Event.ON_RESUME->{PixubiNeedsNotifier.markVisible(true);PixubiNeedsNotifier.clear(context);audio.setForeground(true)}
                        Lifecycle.Event.ON_PAUSE,Lifecycle.Event.ON_STOP->{PixubiNeedsNotifier.markVisit(context);PixubiNeedsNotifier.markVisible(false);audio.setForeground(false)}
                        else->Unit
                    }
                }
                audio.setForeground(lifecycle.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
                lifecycle.lifecycle.addObserver(observer)
                onDispose{lifecycle.lifecycle.removeObserver(observer);PixubiNeedsNotifier.markVisible(false);audio.close()}
            }
            val updater=remember{PixubiUpdater(this@MainActivity)}
            var updateDialog by remember{mutableStateOf<PixubiUpdateResult?>(null)}
            var checkingUpdates by remember{mutableStateOf(false)}
            fun showUpdateToast(message:String){Toast.makeText(context,message,Toast.LENGTH_LONG).show()}
            fun openUpdates(){
                if(checkingUpdates)return
                checkingUpdates=true
                updater.check{result->checkingUpdates=false;updateDialog=result}
            }
            LaunchedEffect(Unit){updater.checkDaily{release->updateDialog=PixubiUpdateResult.NewRelease(release)}}
            MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF98E6CD),secondary=Color(0xFFFFD58A),
                background=Color(0xFF101A2E),surface=Color(0xFF19253C),onBackground=Color.White,onSurface=Color.White)){
                Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
                    if(showSettings) PixubiSettingsScreen(
                        settings=soundSettings,notificationSettings=notificationSettings,
                        onNotificationSettingsChange={saveNotifications(it)},
                        onTestNotification={
                            if(PixubiNeedsNotifier.permissionGranted(context))PixubiNeedsNotifier.sendWorldTest(context)
                            else showUpdateToast("Потрібно дозволити сповіщення PIXUBI.")
                        },
                        currentSeries=currentSeries,previewSeries=auditionSeries,
                        onSettingsChange={soundSettings=it;audioStore.save(it)},
                        onPreviewSeries={auditionSeries=it;audio.configure(soundSettings,WorldCatalog.audio(it));audio.effect(PixubiSound.STAR)},
                        onTestSound={audio.effect(PixubiSound.HAPPY)},onCheckUpdates={openUpdates()},
                        isStableInstall=updater.isStableInstall,checkingForUpdates=checkingUpdates,onBack={closeSettings()}
                    )else PixubiWorldScreen(onSettings={auditionSeries=null;showSettings=true;audio.effect(PixubiSound.TAP)},
                        onSeries={currentSeries=it},onSound={audio.effect(it)})
                }
                val notice = updateDialog
                if (notice != null) {
                    val stable = updater.isStableInstall
                    AlertDialog(
                        onDismissRequest = { updateDialog = null },
                        title = {
                            Text(when (notice) {
                                is PixubiUpdateResult.NewRelease -> "🤖 Новий PIXUBI ${notice.release.version}"
                                is PixubiUpdateResult.UpToDate -> "✅ PIXUBI актуальний"
                                is PixubiUpdateResult.Unavailable -> "ℹ️ Оновлення"
                            })
                        },
                        text = {
                            Text(when (notice) {
                                is PixubiUpdateResult.NewRelease -> if (stable)
                                    "Є новіша підписана версія PIXUBI. Завантажити перевірений APK та оновити гру поверх зі збереженням прогресу роботика? Підтвердження встановлення покаже Android."
                                else
                                    "Це тестова збірка PIXUBI. Стабільна версія встановлюється окремо. Оновлення поверх запрацюють після встановлення першого підписаного релізу."
                                is PixubiUpdateResult.UpToDate ->
                                    "Встановлено PIXUBI ${notice.version}. Нових підписаних випусків наразі немає."
                                is PixubiUpdateResult.Unavailable -> notice.message
                            })
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                updateDialog = null
                                if (notice is PixubiUpdateResult.NewRelease) {
                                    if (stable) {
                                        updater.downloadAndInstall(notice.release) { msg ->
                                            showUpdateToast(msg)
                                        }
                                    } else {
                                        runCatching {
                                            startActivity(
                                                Intent(Intent.ACTION_VIEW,
                                                    Uri.parse("https://github.com/MrKanzo/PIXUBI/releases"))
                                            )
                                        }.onFailure { showUpdateToast("Браузер недоступний") }
                                    }
                                }
                            }) {
                                Text(when {
                                    notice is PixubiUpdateResult.NewRelease && stable -> "Завантажити й оновити"
                                    notice is PixubiUpdateResult.NewRelease -> "Відкрити релізи"
                                    else -> "Гаразд"
                                })
                            }
                        },
                        dismissButton = {
                            if (notice is PixubiUpdateResult.NewRelease) {
                                TextButton(onClick = { updateDialog = null }) { Text("Пізніше") }
                            }
                        }
                    )
                }
            }
        }
    }
}
