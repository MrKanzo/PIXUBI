package com.pixubi.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** All playable forms enter the same bounded frame library. */
@Composable
internal fun FramePetSprite(species:Species,stage:GrowthStage,sex:Sex,gene:Int,
    emotion:PetEmotion,motion:PetMotion,phase:Float,blink:Boolean,eyeClosure:Float,
    modifier:Modifier=Modifier,facingLeft:Boolean=false) {
    val context=LocalContext.current.applicationContext
    val form=GrowthStage.migrate(stage)
    val clip=PixuAnimationPolicy.clip(species,form,motion,emotion)
    val frame=SpriteTimeline.index(clip,phase)
    val closure=NativeEyeMotion.frameIndex(NativeEyeMotion.closure(emotion,eyeClosure,blink))
    val key=PixuFrameKey(species,form,sex,gene.coerceIn(0,5),clip,frame,emotion,closure)
    val image=remember(key){PixuFrameLibrary.get(context,key).asImageBitmap()}
    val mirror=PixuFrameLibrary.mirror(key,facingLeft)
    Image(bitmap=image,contentDescription=species.title+" — "+form.title,
        modifier=modifier.graphicsLayer {
            val growthScale=PixuAnimationPolicy.scale(form)
            transformOrigin=TransformOrigin(.5f,.94f)
            scaleX=if(mirror)-growthScale else growthScale
            scaleY=growthScale
        },
        contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
