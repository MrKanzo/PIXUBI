package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** Review-only, four individually posed limb candidates from the male baby
 * Lisyk raster. Never substitutes for an approved walking clip in the world. */
internal object ImportedWalkCandidate {
    const val ASSET="imported_v2/candidates/lisyk_male_young_walk.png"
    const val STANDING_ASSET="imported_v2/candidates/lisyk_male_young_standing_walk.png"
    const val CELL=128
    fun available(s:Species,g:GrowthStage,sex:Sex)=
        s==Species.LISYK && g==GrowthStage.BABY && sex==Sex.BOY
    fun index(phase:Float)=ImportedMotionTiming.frame(SpriteClip.WALK,phase)
}
@Composable
internal fun WalkCandidatePreview(phase:Float,left:Boolean,modifier:Modifier=Modifier,standing:Boolean=false){
    val context=LocalContext.current.applicationContext
    val atlas=remember(context,standing){
        context.assets.open(if(standing)ImportedWalkCandidate.STANDING_ASSET else ImportedWalkCandidate.ASSET).use{BitmapFactory.decodeStream(it)}
            ?.also{require(it.width==512&&it.height==128)}
            ?:error("Invalid walk candidate")
    }
    val cells=remember(atlas){
        List(4){i->Bitmap.createBitmap(atlas,i*128,0,128,128).asImageBitmap()}
    }
    Image(bitmap=cells[ImportedWalkCandidate.index(phase)],
        contentDescription="Тестові кроки хлопчика Лісика",
        modifier=modifier.graphicsLayer{scaleX=if(left)-1f else 1f},
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
