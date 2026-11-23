package com.pixubi.game

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * The original detailed art is the source of truth. Cache completed motion
 * frames and copy only the changing facial pixels from the existing face atlas.
 * No bitmap mesh, geometry stretching or simplified vector replacement.
 */
@Composable
internal fun FramePetSprite(species:Species,stage:GrowthStage,sex:Sex,gene:Int,
    emotion:PetEmotion,motion:PetMotion,phase:Float,blink:Boolean,eyeClosure:Float,
    modifier:Modifier=Modifier) {
    val context=LocalContext.current.applicationContext
    val source=remember(species,stage,sex,gene) {
        if(stage==GrowthStage.ADULT) PixubiSpriteCache.get(context,species,sex,gene)
        else EvolutionSprites.get(context,species,stage)
    }
    val n=source.width
    val original=remember(source) {
        IntArray(n*n).also { source.getPixels(it,0,n,0,0,n,n) }
    }
    val clip=SpriteTimeline.clip(motion,emotion)
    val body=remember(source,species,stage,clip) {
        List(clip.count){ i -> IllustratedMotionFrames.frame(original,n,species,stage,clip,i) }
    }
    val faces=remember(source,species,stage,emotion) {
        NativeSpriteEyes.frames(source,species,stage,emotion).map { bitmap ->
            IntArray(n*n).also { bitmap.getPixels(it,0,n,0,0,n,n) }
        }
    }
    val frame=SpriteTimeline.index(clip,phase)
    val closure=NativeEyeMotion.closure(emotion,eyeClosure,blink)
    val faceIndex=FaceExpression.frameIndex(emotion,closure,frame%2==1)
    val image=remember(body,faces,frame,faceIndex) {
        val out=body[frame].copyOf()
        val face=faces[faceIndex]
        for(i in original.indices) if(face[i]!=original[i]) out[i]=face[i]
        Bitmap.createBitmap(out,n,n,Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    Image(bitmap=image,contentDescription=species.title+" — "+stage.title,
        modifier=modifier,contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
