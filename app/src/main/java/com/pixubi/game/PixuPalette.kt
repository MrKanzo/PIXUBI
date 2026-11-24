package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

/** Retain the species' natural colors; inherited colors alter cyber accents,
 * while the female coat has a slightly brighter secondary accent. */
internal object PixuPalette {
    fun apply(source:Bitmap,species:Species,sex:Sex,gene:Int):Bitmap {
        if(sex==Sex.BOY && gene.coerceIn(0,5)==0)return source
        val w=source.width;val h=source.height
        val pixels=IntArray(w*h);source.getPixels(pixels,0,w,0,0,w,h)
        val hue=when(species){
            Species.LISYK -> 185f
            Species.ISKRYK -> 280f
            Species.KRAPLYK -> 187f
            Species.PISHCHANYK -> 43f
            Species.KHMARYK -> 255f
        }
        val shift=floatArrayOf(0f,12f,-14f,25f,-26f,36f)[gene.coerceIn(0,5)] +
            if(sex==Sex.GIRL) 8f else 0f
        val hsv=FloatArray(3)
        for(i in pixels.indices){
            val c=pixels[i];val alpha=Color.alpha(c)
            if(alpha<128)continue
            Color.colorToHSV(c,hsv)
            val distance=abs((hsv[0]-hue+540f)%360f-180f)
            if(distance>38f || hsv[1]<.42f || hsv[2]<.36f)continue
            hsv[0]=(hsv[0]+shift+360f)%360f
            if(sex==Sex.GIRL)hsv[2]=(hsv[2]*1.10f).coerceAtMost(1f)
            pixels[i]=Color.HSVToColor(alpha,hsv)
        }
        return Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888)
    }
}
