package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
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
    const val GAIT_ASSET="imported_v2/candidates/lisyk_male_young_gait_v3.png"
    const val CELL=128
    // 0..3: sit/rise/stand; 4..11: eight distinct walking poses.
    private val sequence=intArrayOf(0,0,1,2,3,4,5,6,7,8,9,10,11,4,5,6,7,8,9,10,11,3,2,1,0,0)
    fun sequenceIndex(seconds:Float):Int {
        if(!seconds.isFinite())return 0
        val tick=(seconds.coerceAtLeast(0f)*10f).toInt()
        return sequence[tick.mod(sequence.size)]
    }
    fun available(s:Species,g:GrowthStage,sex:Sex)=
        g in setOf(GrowthStage.BABY,GrowthStage.YOUNG) && sex==Sex.BOY
    fun asset(s:Species,g:GrowthStage=GrowthStage.BABY):String="imported_v2/candidates/"+s.name.lowercase(java.util.Locale.ROOT)+"_male_"+ImportedPixuArt.stageName(g)+"_gait_v3.png"
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

@Composable
internal fun GaitSequencePreview(seconds:Float,left:Boolean,modifier:Modifier=Modifier,species:Species=Species.LISYK,stage:GrowthStage=GrowthStage.BABY){
    val context=LocalContext.current.applicationContext
    val cells=remember(context,species,stage){
        val atlas=context.assets.open(ImportedWalkCandidate.asset(species,stage)).use{BitmapFactory.decodeStream(it)}
            ?.also{require(it.width==512&&it.height==384)}?:error("Invalid gait atlas")
        List(12){i->Bitmap.createBitmap(atlas,(i%4)*128,(i/4)*128,128,128).asImageBitmap()}
    }
    Image(bitmap=cells[ImportedWalkCandidate.sequenceIndex(seconds)],
        contentDescription=species.title+": вставання, ходьба, повернення до спокою",
        modifier=modifier.graphicsLayer{scaleX=if(left)-1f else 1f},
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}

/** Same native cell size, scale and pivot as the imported world sprite. */
@Composable
internal fun GaitWorldPreview(frame:Int,left:Boolean,gene:Int,modifier:Modifier=Modifier,species:Species=Species.LISYK,stage:GrowthStage=GrowthStage.BABY){
    val context=LocalContext.current.applicationContext
    val cells=remember(context,gene,species,stage){
        val atlas=context.assets.open(ImportedWalkCandidate.asset(species,stage)).use{BitmapFactory.decodeStream(it)}
            ?.also{require(it.width==512&&it.height==384)}?:error("Invalid gait atlas")
        List(12){i->
            val raw=Bitmap.createBitmap(atlas,(i%4)*128,(i/4)*128,128,128)
            val colored=PixuPalette.apply(raw,species,Sex.BOY,gene)
            val cell=Bitmap.createBitmap(192,192,Bitmap.Config.ARGB_8888)
            val size=ImportedPixuArt.size(stage)
            val x=(192-size)/2
            val y=174-116*size/128
            Canvas(cell).drawBitmap(colored,null,Rect(x,y,x+size,y+size),Paint().apply{isFilterBitmap=false})
            cell.asImageBitmap()
        }
    }
    Image(bitmap=cells[frame.coerceIn(0,11)],
        contentDescription=species.title+" — експериментальна ходьба v3",
        modifier=modifier.graphicsLayer{scaleX=if(left)-1f else 1f},
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
