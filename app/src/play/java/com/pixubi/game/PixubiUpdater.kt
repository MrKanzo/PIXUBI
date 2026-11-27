package com.pixubi.game
import android.app.Activity

data class PixubiRelease(val version:String,val url:String,val bytes:Long,val sha256:String?)
sealed class PixubiUpdateResult{
    data class NewRelease(val release:PixubiRelease):PixubiUpdateResult()
    data class UpToDate(val version:String):PixubiUpdateResult()
    data class Unavailable(val message:String):PixubiUpdateResult()
}
/** Play distribution deliberately has no network downloader or package installer. */
class PixubiUpdater(private val activity:Activity){
    val isStableInstall:Boolean get()=true
    fun checkDaily(onFound:(PixubiRelease)->Unit){}
    fun check(onComplete:(PixubiUpdateResult)->Unit){
        onComplete(PixubiUpdateResult.Unavailable("Оновлення доступні через Google Play."))
    }
    fun downloadAndInstall(release:PixubiRelease,onMessage:(String)->Unit){
        onMessage("Оновлення доступні через Google Play.")
    }
}
