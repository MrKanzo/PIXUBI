package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class ImportedCareMotionTest {
    @Test fun careEffectsRequireActualConsumption(){
        val p=DevSpec().pet(DevAction.IDLE)
        assertEquals(CareVisual.FOOD,ImportedCareMotion.accepted(p,p.copy(foodStock=p.foodStock-1),Action.FEED))
        assertEquals(CareVisual.WATER,ImportedCareMotion.accepted(p,p.copy(waterStock=p.waterStock-1),Action.WATER))
        assertEquals(CareVisual.CHARGE,ImportedCareMotion.accepted(p,p.copy(chargeStock=p.chargeStock-1),Action.CHARGE))
        for(a in listOf(Action.FEED,Action.WATER,Action.CHARGE))assertNull(ImportedCareMotion.accepted(p,p,a))
        assertNull(ImportedCareMotion.accepted(p.copy(mode=Mode.SLEEP),p.copy(foodStock=p.foodStock-1),Action.FEED))
        assertFalse(ImportedCareMotion.allowed(p,true))
        assertTrue(ImportedCareMotion.allowed(p,false))
        assertEquals(CareVisual.CHARGE,ImportedCareMotion.fromDeveloper(DevAction.CHARGE))
        assertNull(ImportedCareMotion.fromDeveloper(DevAction.IDLE))
    }
    @Test fun fortyHeadNodsKeepPawsAndPalette(){
        val root=listOf(File("src/main/assets"),File("app/src/main/assets")).first{it.isDirectory}
        for(s in Species.entries)for(g in GrowthStage.playable)for(sex in Sex.entries){
            val gender=if(sex==Sex.BOY)"male" else "female"
            val px=ImageIO.read(File(root,"imported_v2/pixubi_${s.name.lowercase(java.util.Locale.ROOT)}_${gender}_${ImportedPixuArt.stageName(g)}.png")).getRGB(0,0,128,128,null,0,128)
            val palette=px.toSet()
            for(clip in listOf(SpriteClip.EAT,SpriteClip.DRINK)){
                val f=ImportedCareMotion.nod(px,s,g,sex,clip,1)
                assertFalse(px.contentEquals(f))
                assertArrayEquals(px.copyOfRange(94*128,px.size),f.copyOfRange(94*128,f.size))
                assertTrue(f.all{it in palette})
            }
            assertTrue(ImportedCareMotion.propX(s,g,sex) in -97f..97f)
        }
    }
}
