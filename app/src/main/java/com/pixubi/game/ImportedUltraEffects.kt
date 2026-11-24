package com.pixubi.game

import android.content.Context
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

/** The original six-frame effect atlases from the approved archive, packed losslessly. */
internal object ImportedUltraEffects {
    const val ASSET="imported_v2/ultra_effects.png"
    const val CELL=128
    const val COUNT=6
    fun row(species:Species,sex:Sex):Int=species.ordinal*2+if(sex==Sex.GIRL)0 else 1
    fun frame(phase:Float):Int? {
        if(!phase.isFinite())return null
        val t=((phase%1f)+1f)%1f
        // 12fps x six frames = 0.5 seconds of the 2.5-second world loop.
        if(t>=.2f)return null
        return (t*30f).toInt().coerceIn(0,COUNT-1)
    }
    fun title(s:Species)=when(s){
        Species.LISYK -> "Щит природи"
        Species.ISKRYK -> "Електричний імпульс"
        Species.KRAPLYK -> "Водяна хвиля"
        Species.PISHCHANYK -> "Кристалічний спалах"
        Species.KHMARYK -> "Вітряні хвилі"
    }
    private var atlas:Bitmap?=null
    private val cells=mutableMapOf<Int,Bitmap>()
    @Synchronized fun get(context:Context,s:Species,sex:Sex,index:Int):Bitmap{
        require(index in 0 until COUNT)
        val row=row(s,sex)
        return cells.getOrPut(row*COUNT+index){
            val source=atlas?:context.assets.open(ASSET).use{BitmapFactory.decodeStream(it)}
                ?.also{require(it.width==CELL*COUNT && it.height==CELL*10);atlas=it}
                ?:error("Invalid ultra effect atlas")
            Bitmap.createBitmap(source,index*CELL,row*CELL,CELL,CELL)
        }
    }
}
@Composable
internal fun ImportedUltraEffect(species:Species,sex:Sex,cycle:Float,modifier:Modifier=Modifier){
    val frame=ImportedUltraEffects.frame(cycle)?:return
    val context=LocalContext.current.applicationContext
    val image=remember(species,sex,frame){ImportedUltraEffects.get(context,species,sex,frame).asImageBitmap()}
    Image(bitmap=image,contentDescription=null,modifier=modifier,
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
