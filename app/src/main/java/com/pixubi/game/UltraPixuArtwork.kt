package com.pixubi.game

import android.graphics.Bitmap
import java.util.Base64
import java.util.zip.InflaterInputStream

/** Five individual transparent sprites traced from the approved evolution sheet.
 * Indexed 48 x 48 pixel artwork, no network, no emoji or shell fallback. */
internal object UltraPixuArtwork {
    private const val N=48
    private val data=listOf(
        "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"
    )

    fun pixels(species:Species):IntArray {
        val bytes=InflaterInputStream(Base64.getDecoder().decode(data[species.ordinal]).inputStream()).use{it.readBytes()}
        require(bytes.size==69+N*N)
        fun b(i:Int)=bytes[i].toInt() and 255
        return IntArray(N*N){i->
            val index=b(69+i)
            if(index==0)0 else {
                val j=(index-1)*3
                0xff000000.toInt() or (b(j) shl 16) or (b(j+1) shl 8) or b(j+2)
            }
        }
    }

    private fun eye(s:Species):EyeRegion=when(s){
        Species.LISYK -> EyeRegion(38f,19f,2.4f,1.8f)
        Species.ISKRYK -> EyeRegion(38f,19f,2.4f,1.8f)
        Species.KRAPLYK -> EyeRegion(37f,19f,2.4f,1.8f)
        Species.PISHCHANYK -> EyeRegion(36f,19f,2.4f,1.8f)
        Species.KHMARYK -> EyeRegion(27f,17f,2.4f,1.8f)
    }

    /** Only move opaque foot tips by one pixel. Do not stretch the body,
     * duplicate facial pixels or copy the background into the sprite. */
    fun step(source:IntArray,clip:SpriteClip,frame:Int):IntArray {
        val result=source.copyOf()
        if(clip !in listOf(SpriteClip.WALK,SpriteClip.DIG,SpriteClip.HOP,SpriteClip.SWIM,SpriteClip.FLY)||frame%2==0)return result
        val start=if(frame%4==1) 8 else 27
        for(x in start until start+13) {
            val bottom=(45 downTo 38).firstOrNull{source[it*N+x] ushr 24>0}?:continue
            if(bottom<41 || source[(bottom-3)*N+x] ushr 24==0)continue
            for(y in bottom-2..bottom)result[y*N+x]=0
            for(y in bottom-2..bottom)result[(y-1)*N+x]=source[y*N+x]
        }
        return result
    }

    fun bitmap(species:Species,sex:Sex,gene:Int,clip:SpriteClip,frame:Int,emotion:PetEmotion,closure:Int):Bitmap {
        val original=pixels(species)
        val blink=if(emotion==PetEmotion.SLEEPING||emotion==PetEmotion.HIBERNATING)4 else closure.coerceIn(0,4)
        val face=if(blink==0)original else EyePixelAnimator.frame(original,N,N,listOf(eye(species)),blink)
        val moved=step(face,clip,frame)
        return PixuPalette.apply(Bitmap.createBitmap(moved,N,N,Bitmap.Config.ARGB_8888),species,sex,gene)
    }
}
