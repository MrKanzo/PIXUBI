package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Pixel-aligned props and particles, separate from the pet raster. */
@Composable
internal fun CareSceneProps(pet:Pet,stage:GrowthStage,care:CareVisual,phase:Float,modifier:Modifier=Modifier){
    Canvas(modifier){
        val p=2.dp.toPx()
        val ground=size.height/2f+95.dp.toPx()
        val cx=size.width/2f
        val mouthX=cx+ImportedCareMotion.propX(pet.species,stage,pet.sex).dp.toPx()
        fun rect(c:Color,x:Float,y:Float,w:Int,h:Int)=drawRect(c,Offset(x,y),Size(w*p,h*p))
        when(care){
            CareVisual.FOOD->{
                val x=mouthX-8*p
                if(pet.hasBowl){
                    rect(Color(0xFF354B62),x,ground-5*p,16,5)
                    rect(Color(0xFF92B5C8),x,ground-5*p,16,1)
                    rect(Color(0xFFE4A45D),x+2*p,ground-6*p,12,2)
                }else rect(Color(0xFFE98554),x+4*p,ground-3*p,8,3)
                repeat(3){i->
                    val t=(phase+i*.25f)%1f
                    rect(Color(0xFFFFC278).copy(alpha=1f-t),mouthX+(i-1)*3*p,ground-(9+18*t)*p,1,1)
                }
            }
            CareVisual.WATER->{
                val x=mouthX-6*p
                if(pet.hasCup){
                    rect(Color(0xFF335E87),x,ground-9*p,12,9)
                    rect(Color(0xFF7CD9F5),x+p,ground-8*p,10,4)
                    rect(Color(0xFFBDEEFF),x,ground-9*p,12,1)
                }else rect(Color(0xFF67CFF2),x+1*p,ground-2*p,10,2)
                repeat(3){i->
                    val t=(phase+i*.33f)%1f
                    rect(Color(0xFF8BE9FF).copy(alpha=1f-t),mouthX+(i-1)*3*p,ground-(10+18*t)*p,1,2)
                }
            }
            CareVisual.CHARGE->{
                val x=cx-19*p
                rect(Color(0xFF253D55),x,ground-4*p,38,4)
                rect(Color(0xFF58BDA6),x+2*p,ground-5*p,34,1)
                if(pet.hasCharger){
                    rect(Color(0xFF456781),x-4*p,ground-15*p,4,15)
                    rect(Color(0xFF9AFCE1),x-3*p,ground-14*p,2,3)
                }
                repeat(8){i->
                    val t=(phase+i*.125f)%1f
                    val xx=cx+(i-3.5f)*5*p
                    rect(Color(0xFF9DFFE0).copy(alpha=1f-t),xx,ground-(6+35*t)*p,1,2)
                }
                val filled=(phase*10).toInt().coerceIn(0,10)
                rect(Color(0xFF253D55),cx-11*p,ground-59*p,22,5)
                rect(Color(0xFF97F4AE),cx-10*p,ground-58*p,filled*2,3)
            }
        }
    }
}
