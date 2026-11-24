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

    private val adultData=listOf(
        "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"
    )
    fun pixels(species:Species,stage:GrowthStage=GrowthStage.ULTRA):IntArray {
        require(stage==GrowthStage.ADULT||stage==GrowthStage.ULTRA)
        val encoded=(if(stage==GrowthStage.ADULT)adultData else data)[species.ordinal]
        val bytes=InflaterInputStream(Base64.getDecoder().decode(encoded).inputStream()).use{it.readBytes()}
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

    private fun face(s:Species,stage:GrowthStage):Pair<EyeRegion,EyeRegion>{
        val positions=if(stage==GrowthStage.ADULT)when(s){
            Species.LISYK -> intArrayOf(16,23,21,26)
            Species.ISKRYK -> intArrayOf(20,22,24,25)
            Species.KRAPLYK -> intArrayOf(39,22,40,25)
            Species.PISHCHANYK -> intArrayOf(23,25,27,28)
            Species.KHMARYK -> intArrayOf(26,24,30,26)
        }else when(s){
            Species.LISYK -> intArrayOf(39,23,42,26)
            Species.ISKRYK -> intArrayOf(38,22,42,25)
            Species.KRAPLYK -> intArrayOf(39,22,41,25)
            Species.PISHCHANYK -> intArrayOf(39,22,42,25)
            Species.KHMARYK -> intArrayOf(27,24,29,26)
        }
        return EyeRegion(positions[0].toFloat(),positions[1].toFloat(),2.3f,1.8f) to
            EyeRegion(positions[2].toFloat(),positions[3].toFloat(),2.2f,1.6f)
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

    fun bitmap(species:Species,sex:Sex,gene:Int,clip:SpriteClip,frame:Int,emotion:PetEmotion,closure:Int,stage:GrowthStage=GrowthStage.ULTRA):Bitmap {
        val original=pixels(species,stage)
        val blink=if(emotion==PetEmotion.SLEEPING||emotion==PetEmotion.HIBERNATING)4 else closure.coerceIn(0,4)
        val (eye,mouth)=face(species,stage)
        val face=if(blink==0&&emotion==PetEmotion.IDLE)original else
            EyePixelAnimator.faceFrames(original,N,N,listOf(eye),mouth,species==Species.KHMARYK,emotion)[FaceExpression.frameIndex(emotion,blink/4f,frame%2==1)]
        val moved=step(face,clip,frame)
        return PixuPalette.apply(Bitmap.createBitmap(moved,N,N,Bitmap.Config.ARGB_8888),species,sex,gene)
    }
}
