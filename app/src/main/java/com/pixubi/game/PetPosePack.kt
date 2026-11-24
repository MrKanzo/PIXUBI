package com.pixubi.game

import android.content.Context
import android.graphics.Bitmap

/** One source of truth for the actual illustrated poses. Never advertise an
 * animation for a portrait without a matching full-body pose pack. */
internal enum class PetPosePack {
    LISYK_BABY, ISKRYK_BABY, KRAPLYK_BABY, PISHCHANYK_BABY, KHMARYK_BABY, LISYK_YOUNG;
    fun locomotion(clip:SpriteClip):Boolean=when(this){
        LISYK_BABY -> LisykBabyArtwork.locomotion(clip)
        ISKRYK_BABY -> IskrykBabyArtwork.locomotion(clip)
        KRAPLYK_BABY -> KraplykBabyArtwork.locomotion(clip)
        PISHCHANYK_BABY -> PishchanykBabyArtwork.locomotion(clip)
        KHMARYK_BABY -> KhmarykBabyArtwork.locomotion(clip)
        LISYK_YOUNG -> LisykYoungArtwork.locomotion(clip)
    }

    fun bitmap(context:Context,clip:SpriteClip,emotion:PetEmotion,frame:Int,closed:Boolean):Bitmap =
        when(this){
            LISYK_BABY -> if(LisykFrontArtwork.usesFront(clip))
                LisykFrontArtwork.get(context,LisykFrontArtwork.index(emotion,frame,closed))
                else LisykBabyArtwork.get(context,LisykBabyArtwork.cell(clip,frame))
            ISKRYK_BABY -> IskrykBabyArtwork.get(context,IskrykBabyArtwork.index(clip,emotion,frame,closed))
            KRAPLYK_BABY -> KraplykBabyArtwork.get(context,KraplykBabyArtwork.index(clip,emotion,frame,closed))
            PISHCHANYK_BABY -> PishchanykBabyArtwork.get(context,PishchanykBabyArtwork.index(clip,emotion,frame,closed))
            KHMARYK_BABY -> KhmarykBabyArtwork.get(context,KhmarykBabyArtwork.index(clip,emotion,frame,closed))
            LISYK_YOUNG -> LisykYoungArtwork.get(context,LisykYoungArtwork.index(clip,emotion,frame,closed))
        }

    companion object {
        fun forPet(s:Species,g:GrowthStage):PetPosePack? = when(g) {
            GrowthStage.BABY -> when(s){
                Species.LISYK -> LISYK_BABY
                Species.ISKRYK -> ISKRYK_BABY
                Species.KRAPLYK -> KRAPLYK_BABY
                Species.PISHCHANYK -> PISHCHANYK_BABY
                Species.KHMARYK -> KHMARYK_BABY
            }
            GrowthStage.YOUNG -> if(s==Species.LISYK)LISYK_YOUNG else null
            else -> null
        }
    }
}
