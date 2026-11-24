package com.pixubi.game

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/** Draw approved artwork at its native resolution. A missing pose never
 * replaces an illustrated character with a geometric lookalike. */
@Composable
internal fun FramePetSprite(species:Species,stage:GrowthStage,sex:Sex,gene:Int,
    emotion:PetEmotion,motion:PetMotion,phase:Float,blink:Boolean,eyeClosure:Float,
    modifier:Modifier=Modifier,facingLeft:Boolean=false) {
    val context=LocalContext.current.applicationContext
    val clip=SpriteTimeline.clip(motion,emotion)
    val frame=SpriteTimeline.index(clip,phase)
    val pack=PetPosePack.forPet(species,stage)
    val closed=blink||eyeClosure>.50f
    if(pack!=null) {
        val image=remember(pack,clip,emotion,frame,closed) {
            pack.bitmap(context,clip,emotion,frame,closed).asImageBitmap()
        }
        Image(bitmap=image,contentDescription=species.title+" — "+stage.title,
            modifier=modifier.graphicsLayer{scaleX=if(facingLeft&&pack.locomotion(clip))-1f else 1f},
            contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
        return
    }

    // Future complete illustrated pose packs can be added without changing
    // saves or touching the original portrait.
    val authored=remember(context,species,stage,sex,gene,clip) {
        PixuArtPack.loadClip(context,species,stage,sex,gene,clip)?.map{
            PixuArtPack.composite(it).asImageBitmap()
        }
    }
    if(authored!=null) {
        val directional=clip in listOf(SpriteClip.WALK,SpriteClip.HOP,SpriteClip.DIG,SpriteClip.SWIM,SpriteClip.FLY)
        Image(bitmap=authored[frame],contentDescription=species.title+" — "+stage.title,
            modifier=modifier.graphicsLayer{scaleX=if(directional&&facingLeft)-1f else 1f},
            contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
        return
    }

    // No invented side silhouettes, no cropped or displaced limbs. Retain
    // the original male/female adult art or the independent juvenile art.
    val source=remember(context,species,stage,sex,gene) {
        if(stage==GrowthStage.ADULT) PixubiSpriteCache.get(context,species,sex,gene)
        else EvolutionSprites.get(context,species,stage)
    }
    val faceEmotion=when(emotion){
        PetEmotion.EATING,PetEmotion.DRINKING -> emotion
        else -> PetEmotion.IDLE
    }
    val faces=remember(source,species,stage,faceEmotion) {
        NativeSpriteEyes.frames(source,species,stage,faceEmotion).map{it.asImageBitmap()}
    }
    val step=NativeEyeMotion.frameIndex(NativeEyeMotion.closure(emotion,eyeClosure,blink))
    val mouth=FaceExpression.animatedMouth(faceEmotion)
    val image=faces[step+if(mouth&&frame%2==1)5 else 0]
    Image(bitmap=image,contentDescription=species.title+" — "+stage.title,
        modifier=modifier,contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
