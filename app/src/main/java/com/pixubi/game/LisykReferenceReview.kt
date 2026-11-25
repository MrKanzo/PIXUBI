package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A/B review only. Neither candidate replaces the active game or previous preview. */
internal object LisykReferenceReview {
    const val WALK="imported_v2/candidates/lisyk_male_young_gait_v4.png"
    const val SLEEP="imported_v2/candidates/lisyk_male_young_sleep_v2.png"
    const val BLINK="imported_v2/candidates/lisyk_male_young_blink_v2.png"
    private val blinkSequence=intArrayOf(0,0,1,2,3,4,4,3,2,1,0,0)
    fun blinkFrame(seconds:Float):Int=if(seconds.isFinite())blinkSequence[(seconds.coerceAtLeast(0f)*10f).toInt().mod(blinkSequence.size)] else 0
    fun matches(s:Species,g:GrowthStage,sex:Sex)=s==Species.LISYK&&g==GrowthStage.BABY&&sex==Sex.BOY
}

@Composable
internal fun LisykReferenceComparison(seconds:Float,left:Boolean,gene:Int,modifier:Modifier=Modifier){
    val gait=ImportedWalkCandidate.sequenceIndex(seconds)
    val rest=ImportedSleepCandidate.reviewFrame(seconds)
    val blink=LisykReferenceReview.blinkFrame(seconds)
    var eyesOnly by remember{mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(4.dp)){
        Text("Лісик • малюк-хлопчик • однаковий час кадрів",fontSize=12.sp,color=Color.White)
        androidx.compose.material3.OutlinedButton(onClick={eyesOnly=!eyesOnly},modifier=Modifier.fillMaxWidth()){
            Text(if(eyesOnly)"Ходьба та сон" else "Очі • порівняти моргання")
        }
        if(eyesOnly){
            Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                OriginalBlinkCell(blink,gene,Modifier.weight(1f).fillMaxHeight())
                ReferenceCell("Моргання v2 • чернетка",LisykReferenceReview.BLINK,5,5,blink,false,gene,Modifier.weight(1f).fillMaxHeight())
            }
            Text("Фаза ${blink+1}/5 • уповільнений повтор • обидві моделі мають ту саму мордочку й рот.",fontSize=12.sp,color=Color(0xFFFFD58A))
        }else{
        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            ReferenceCell("Ходьба v3 • попередня",ImportedWalkCandidate.GAIT_ASSET,4,12,gait,left,gene,Modifier.weight(1f).fillMaxHeight())
            ReferenceCell("Ходьба v4 • нова чернетка",LisykReferenceReview.WALK,4,12,gait,left,gene,Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.weight(1f).fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            ReferenceCell("Сон v1 • попередній",ImportedSleepCandidate.asset(Species.LISYK),3,6,rest,false,gene,Modifier.weight(1f).fillMaxHeight())
            ReferenceCell("Сон v2 • нова чернетка",LisykReferenceReview.SLEEP,3,6,rest,false,gene,Modifier.weight(1f).fillMaxHeight())
        }
        Text("Ходьба: ${gait+1}/12 • сон: ${rest+1}/6. Окремі пози без ігрового переміщення. Тест, не затверджені кадри.",fontSize=11.sp,color=Color(0xFFFFD58A))
        }
    }
}
@Composable
private fun ReferenceCell(title:String,path:String,columns:Int,count:Int,frame:Int,left:Boolean,gene:Int,modifier:Modifier){
    val context=LocalContext.current.applicationContext
    val cells=remember(context,path,gene){
        val atlas=context.assets.open(path).use{BitmapFactory.decodeStream(it)}?:error("Invalid comparison atlas")
        require(atlas.width==columns*128&&atlas.height==(count/columns)*128)
        List(count){i->
            val raw=Bitmap.createBitmap(atlas,i%columns*128,i/columns*128,128,128)
            PixuPalette.apply(raw,Species.LISYK,Sex.BOY,gene).asImageBitmap()
        }
    }
    Column(modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFF344352)).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Text(title,fontSize=12.sp,color=Color.White)
        Image(cells[frame.coerceIn(0,count-1)],contentDescription=title,modifier=Modifier.weight(1f).fillMaxWidth().graphicsLayer{scaleX=if(left)-1f else 1f},contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
    }
}

@Composable
private fun OriginalBlinkCell(frame:Int,gene:Int,modifier:Modifier){
    val context=LocalContext.current.applicationContext
    val images=remember(context,gene){
        val atlas=context.assets.open("imported_v2/pixubi_lisyk_male_young.png").use{BitmapFactory.decodeStream(it)}?:error("Missing original")
        val native=IntArray(128*128)
        atlas.getPixels(native,0,128,0,0,128,128)
        EyePixelAnimator.frames(native,128,128,ImportedEyeGeometry.regions(Species.LISYK,GrowthStage.BABY,Sex.BOY)).map{pixels->
            PixuPalette.apply(Bitmap.createBitmap(pixels,128,128,Bitmap.Config.ARGB_8888),Species.LISYK,Sex.BOY,gene).asImageBitmap()
        }
    }
    Column(modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFF344352)).padding(4.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Text("Моргання v1 • попереднє",fontSize=12.sp,color=Color.White)
        Image(images[frame.coerceIn(0,4)],contentDescription="Попереднє моргання",modifier=Modifier.weight(1f).fillMaxWidth(),contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
    }
}
