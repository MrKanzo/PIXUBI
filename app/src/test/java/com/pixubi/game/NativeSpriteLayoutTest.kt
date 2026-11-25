package com.pixubi.game

import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class NativeSpriteLayoutTest {
    private val n=192
    @Test fun fixedLayoutPreservesGroundAndMirror(){
        val src=IntArray(n*n)
        for(y in 40..159)for(x in 50..139)src[y*n+x]=0xFF55AACC.toInt()
        val fit=NativeSpriteLayout.fit(src,GrowthStage.ADULT)
        val a=NativeSpriteLayout.render(src,fit)
        val b=NativeSpriteLayout.render(src,fit,true)
        assertArrayEquals(PixuRaster.mirror(a,n),b)
        assertTrue(a.indices.filter{a[it] ushr 24>=128}.maxOf{it/n} in 176..180)
        assertTrue(a.count{it ushr 24>=128}>src.count{it ushr 24>=128})
        val moved=src.copyOf().also{it[159*n+100]=0}
        assertEquals(fit,NativeSpriteLayout.fit(src,GrowthStage.ADULT))
        assertEquals(n*n,NativeSpriteLayout.render(moved,fit).size)
    }
    @Test fun activeNativeFormsContactSheet(){
        val young=ImageIO.read(File("src/main/assets/pixubi_evolution_10.png"))
        val older=ImageIO.read(File("src/main/assets/pixubi_older_v032.png"))
        val cases=GrowthStage.playable.flatMap{g->Species.entries.map{s->s to g}}
            .filter{(s,g)->PixuArtRouting.source(s,g)!=PixuArtSource.POSE_PACK}
        assertEquals(14,cases.size)
        val sheet=BufferedImage(n*6,n*cases.size*2,BufferedImage.TYPE_INT_ARGB)
        val graphics=sheet.createGraphics()
        for((i,entry) in cases.withIndex()){
            val (s,g)=entry
            val isYoung=g==GrowthStage.YOUNG
            val atlas=if(isYoung)young else older
            val y=if(g==GrowthStage.ADULT)0 else n
            val original=atlas.getRGB(s.ordinal*n,y,n,n,null,0,n)
            val special=when(s){
                Species.KRAPLYK->SpriteClip.SWIM
                Species.KHMARYK->SpriteClip.FLY
                else->SpriteClip.DIG
            }
            for((j,sex) in Sex.entries.withIndex()){
                val src=if(sex==Sex.GIRL)PixuSilhouette.female(original,n,n,s) else original
                val fit=NativeSpriteLayout.fit(src,g)
                val frames=(0..3).map{f->NativeSpriteLayout.nativeMotion(src,s,g,SpriteClip.WALK,f)}+
                    NativeSpriteLayout.nativeMotion(src,s,g,special,1)
                val geo=if(isYoung)null else ReferenceOlderFrames.geometry(s,g)
                val eyes=geo?.eyes?:NativeEyeGeometry.regions(s,g)
                val blink=EyePixelAnimator.frame(src,n,n,eyes,4)
                assertFalse(src.contentEquals(blink))
                for((col,pixels) in (frames+listOf(blink)).withIndex()){
                    val out=NativeSpriteLayout.render(pixels,fit,mirror=isYoung&&col<5)
                    assertTrue("Empty or damaged creature "+s.name+" "+g.name,out.count{it ushr 24>=128}>3000)
                    val img=BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB)
                    img.setRGB(0,0,n,n,out,0,n)
                    graphics.drawImage(img,col*n,(i*2+j)*n,null)
                }
            }
        }
        graphics.dispose()
        val f=File("build/eye-preview/active-native-forms.png")
        f.parentFile.mkdirs()
        assertTrue(ImageIO.write(sheet,"png",f))
    }
}
