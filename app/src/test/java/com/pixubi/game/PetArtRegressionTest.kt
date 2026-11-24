package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class PetArtRegressionTest {
    private fun asset(name:String)=listOf(File("src/main/assets/$name"),File("app/src/main/assets/$name")).first{it.isFile}

    @Test fun allFiveBabiesAndApprovedYoungLisykUseActualPosePacks(){
        Species.entries.forEach { species ->
            val pack=PetPosePack.forPet(species,GrowthStage.BABY)
            assertNotNull(pack)
            assertTrue(pack!!.locomotion(SpriteClip.WALK))
            assertEquals(species==Species.LISYK,PetPosePack.forPet(species,GrowthStage.YOUNG)!=null)
            assertNull(PetPosePack.forPet(species,GrowthStage.ADULT))
            assertNull(PetPosePack.forPet(species,GrowthStage.EGG))
            assertNull(PetPosePack.forPet(species,GrowthStage.COCOON))
        }
        assertTrue(PetPosePack.KRAPLYK_BABY.locomotion(SpriteClip.SWIM))
        assertTrue(PetPosePack.KHMARYK_BABY.locomotion(SpriteClip.FLY))
        assertTrue(PetPosePack.PISHCHANYK_BABY.locomotion(SpriteClip.DIG))
    }

    @Test fun everyOriginalPortraitCellContainsDetailedArtwork(){
        fun check(name:String,size:Int,cols:Int,rows:Int){
            val image=ImageIO.read(asset(name))
            assertEquals(cols*size,image.width)
            assertEquals(rows*size,image.height)
            assertTrue(image.colorModel.hasAlpha())
            val fingerprints=mutableSetOf<Int>()
            for(y in 0 until rows)for(x in 0 until cols){
                val pixels=image.getRGB(x*size,y*size,size,size,null,0,size)
                val visible=pixels.filter{it ushr 24 >= 128}
                assertTrue("$name cell $x,$y is blank",visible.size>500)
                // Pixel art intentionally supports limited palettes; check color diversity, not photorealistic shading.
                assertTrue("$name cell $x,$y lacks a usable palette: "+visible.distinct().size,visible.distinct().size>=8)
                assertTrue("$name cell $x,$y duplicates another",fingerprints.add(pixels.contentHashCode()))
            }
        }
        check("pixubi_evolution_10.png",192,5,2)
        check("pixubi_avatars.png",160,5,12)
        for(species in Species.entries)for(sex in Sex.entries)for(gene in 0..5){
            assertTrue(PixubiSpriteIndex.position(species,sex,gene) in 0..59)
        }
        assertEquals(60,Species.entries.flatMap{s->Sex.entries.flatMap{sex->(0..5).map{g->PixubiSpriteIndex.position(s,sex,g)}}}.distinct().size)
    }
}
