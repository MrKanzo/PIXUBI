package com.pixubi.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

/** Select immutable full-body keyframes. The renderer never deforms a bitmap. */
@Composable
internal fun FramePetSprite(species:Species,stage:GrowthStage,sex:Sex,gene:Int,
    emotion:PetEmotion,motion:PetMotion,phase:Float,blink:Boolean,eyeClosure:Float,
    modifier:Modifier=Modifier) {
    val clip=SpriteTimeline.clip(motion,emotion)
    val frames=remember(species,stage,sex,gene,emotion,clip) {
        List(clip.count*2) { i ->
            FramePetArt.render(species,stage,sex,gene,emotion,clip,i%clip.count,
                i>=clip.count).asImageBitmap()
        }
    }
    val i=SpriteTimeline.index(clip,phase)+
        if((blink||eyeClosure>.50f)&&clip!=SpriteClip.SLEEP) clip.count else 0
    Image(bitmap=frames[i],contentDescription=species.title+" — "+stage.title,
        modifier=modifier,contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
