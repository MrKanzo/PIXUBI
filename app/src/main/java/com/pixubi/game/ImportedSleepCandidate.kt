package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** Constructed rest poses, not authored curled-body illustrations.
 * Original sleep art always takes precedence. */
internal object ImportedSleepCandidate {
    fun available(s:Species,g:GrowthStage,sex:Sex)=g in setOf(GrowthStage.YOUNG,GrowthStage.ULTRA)||(g in setOf(GrowthStage.BABY,GrowthStage.ADULT)&&sex==Sex.BOY)
    fun asset(s:Species,g:GrowthStage=GrowthStage.BABY,sex:Sex=Sex.BOY)="imported_v2/candidates/"+s.name.lowercase(java.util.Locale.ROOT)+(if(sex==Sex.BOY)"_male_" else "_female_")+ImportedPixuArt.stageName(g)+"_sleep_v1.png"
    fun applies(f:ImportedPixuArt.Form,k:PixuFrameKey)=
        k.clip==SpriteClip.SLEEP&&"sleep" !in f.frames&&available(k.species,k.stage,k.sex)
    fun restFrame(emotion:PetEmotion,amplitude:Int)=
        if(emotion==PetEmotion.SLEEPING)3+amplitude.coerceIn(0,2) else 3
    private val sequence=intArrayOf(0,0,1,2,3,4,5,4,3,3,2,1,0,0)
    fun reviewFrame(seconds:Float):Int {
        if(!seconds.isFinite())return 0
        return sequence[(seconds.coerceAtLeast(0f)*5f).toInt().mod(sequence.size)]
    }
}
@Composable
internal fun SleepCandidatePreview(species:Species,seconds:Float,modifier:Modifier=Modifier,stage:GrowthStage=GrowthStage.BABY,sex:Sex=Sex.BOY){
    val context=LocalContext.current.applicationContext
    val cells=remember(context,species,stage,sex){
        val atlas=context.assets.open(ImportedSleepCandidate.asset(species,stage,sex)).use{BitmapFactory.decodeStream(it)}
            ?.also{require(it.width==384&&it.height==256)}?:error("Invalid sleep candidate")
        List(6){i->Bitmap.createBitmap(atlas,(i%3)*128,(i/3)*128,128,128).asImageBitmap()}
    }
    Image(bitmap=cells[ImportedSleepCandidate.reviewFrame(seconds)],contentDescription=species.title+" — тест засинання і пробудження",
        modifier=modifier,contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
