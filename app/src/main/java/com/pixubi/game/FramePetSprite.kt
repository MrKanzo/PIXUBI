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

/** Preserve original art until the selected action has a complete authored pose pack. */
@Composable
internal fun FramePetSprite(species:Species,stage:GrowthStage,sex:Sex,gene:Int,
    emotion:PetEmotion,motion:PetMotion,phase:Float,blink:Boolean,eyeClosure:Float,
    modifier:Modifier=Modifier,facingLeft:Boolean=false) {
    val context=LocalContext.current.applicationContext
    val clip=SpriteTimeline.clip(motion,emotion)
    val frame=SpriteTimeline.index(clip,phase)
    if(IskrykBabyArtwork.matches(species,stage)) {
        val index=IskrykBabyArtwork.index(clip,emotion,frame,blink||eyeClosure>.50f)
        val image=remember(index){IskrykBabyArtwork.get(context,index).asImageBitmap()}
        Image(bitmap=image,contentDescription=species.title+" — "+stage.title,
            modifier=modifier.graphicsLayer{scaleX=if(facingLeft&&IskrykBabyArtwork.locomotion(clip))-1f else 1f},
            contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
        return
    }
    if(LisykBabyArtwork.matches(species,stage)) {
        val cell=LisykBabyArtwork.cell(clip,frame)
        val image=if(LisykFrontArtwork.usesFront(clip)) {
            val index=LisykFrontArtwork.index(emotion,frame,blink||eyeClosure>.50f)
            remember(index){LisykFrontArtwork.get(context,index).asImageBitmap()}
        } else remember(cell){LisykBabyArtwork.get(context,cell).asImageBitmap()}
        Image(bitmap=image,contentDescription=species.title+" — "+stage.title,
            modifier=modifier.graphicsLayer{scaleX=if(facingLeft&&LisykBabyArtwork.locomotion(clip))-1f else 1f},
            contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
        return
    }
    if(KraplykBabyArtwork.matches(species,stage)) {
        val index=KraplykBabyArtwork.index(clip,emotion,frame,blink||eyeClosure>.50f)
        val image=remember(index){KraplykBabyArtwork.get(context,index).asImageBitmap()}
        Image(bitmap=image,contentDescription=species.title+" — "+stage.title,
            modifier=modifier.graphicsLayer{scaleX=if(facingLeft&&KraplykBabyArtwork.locomotion(clip))-1f else 1f},
            contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
        return
    }
    val source=remember(species,stage,sex,gene) {
        if(stage==GrowthStage.ADULT)PixubiSpriteCache.get(context,species,sex,gene)
        else EvolutionSprites.get(context,species,stage)
    }
    val n=source.width
    val authored=remember(species,stage,sex,gene,clip) {
        PixuArtPack.loadClip(context,species,stage,sex,gene,clip)?.map{PixuArtPack.composite(it).asImageBitmap()}
    }
    // The native mouth changes only while actually eating/drinking; the original
    // face remains untouched between blinks during all other states.
    val faceEmotion=when(emotion){PetEmotion.EATING,PetEmotion.DRINKING->emotion;else->PetEmotion.IDLE}
    val faces=remember(source,species,stage,faceEmotion) {
        NativeSpriteEyes.frames(source,species,stage,faceEmotion).map{ bitmap->
            val px=IntArray(n*n);bitmap.getPixels(px,0,n,0,0,n,n)
            PixuArtPack.composite(PixuRig.partition(px,n,species,stage)).asImageBitmap()
        }
    }
    val step=NativeEyeMotion.frameIndex(NativeEyeMotion.closure(emotion,eyeClosure,blink))
    val mouth=faceEmotion==PetEmotion.EATING||faceEmotion==PetEmotion.DRINKING
    val image=authored?.get(frame)?:faces[step+if(mouth&&frame%2==1)5 else 0]
    Image(bitmap=image,contentDescription=species.title+" — "+stage.title,
        modifier=modifier,contentScale=ContentScale.Fit,filterQuality=FilterQuality.None)
}
