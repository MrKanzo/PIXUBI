package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/** Dedicated right-facing pixel illustration of baby Kraplyk.
 * Four authored gait poses; every limb has its own silhouette and joint.
 * The frontal evolution atlas is not used for walking. */
internal object KraplykSideArtwork {
    const val CELL = 192
    fun matches(s:Species,g:GrowthStage) = s==Species.KRAPLYK && g==GrowthStage.BABY
    fun usesSide(c:SpriteClip) = c==SpriteClip.WALK
    fun index(frame:Int) = frame.mod(4)
    private val cache=mutableMapOf<Int,Bitmap>()

    // Far hind, far front, near hind, near front: foot offset X, lift.
    private val gait=arrayOf(
        intArrayOf(5,0,-6,0,-6,0,6,0),
        intArrayOf(0,0,0,0,2,5,-1,3),
        intArrayOf(-6,0,6,0,6,0,-6,0),
        intArrayOf(2,5,-1,4,0,0,0,0)
    )
    fun feet(frame:Int)=gait[index(frame)].copyOf()

    @Synchronized fun get(frame:Int):Bitmap {
        val i=index(frame)
        return cache.getOrPut(i) {
            val bitmap=Bitmap.createBitmap(CELL,CELL,Bitmap.Config.ARGB_8888)
            val canvas=Canvas(bitmap)
            canvas.scale(2f,2f)
            draw(canvas,i)
            bitmap
        }
    }

    private fun draw(c:Canvas,i:Int) {
        val p=Paint().apply { isAntiAlias=false; isFilterBitmap=false; style=Paint.Style.FILL }
        fun color(v:Int) { p.color=v or 0xFF000000.toInt() }
        fun poly(v:Int,vararg a:Int) {
            color(v)
            val path=Path().apply {
                moveTo(a[0].toFloat(),a[1].toFloat())
                for(j in 2 until a.size step 2) lineTo(a[j].toFloat(),a[j+1].toFloat())
                close()
            }
            c.drawPath(path,p)
        }
        fun oval(v:Int,l:Int,t:Int,r:Int,b:Int) {
            color(v);c.drawOval(l.toFloat(),t.toFloat(),r.toFloat(),b.toFloat(),p)
        }
        fun rect(v:Int,x:Int,y:Int,w:Int=1,h:Int=1) {
            color(v);c.drawRect(x.toFloat(),y.toFloat(),(x+w).toFloat(),(y+h).toFloat(),p)
        }
        val ink=0x12283E; val deep=0x17466F; val blue=0x2686BB
        val cyan=0x37CFE6; val bright=0x75EEF4; val light=0xACFBFA
        val belly=0xA0E8F4; val violet=0x6653B5; val purple=0xAF83EC
        val white=0xE9FFFF
        val stride=feet(i)
        val tailY=intArrayOf(0,-2,1,2)[i]
        // Broad aquatic tail, outlined in navy and shaded with fin rays.
        c.save();c.translate(0f,tailY.toFloat())
        poly(ink,43,62,34,57,25,56,17,51,6,46,9,59,14,67,23,73,34,74,44,69)
        poly(deep,42,63,33,59,25,58,17,53,9,50,13,61,19,68,26,71,34,71,41,68)
        poly(cyan,37,63,26,60,18,56,11,51,14,60,20,66,29,68,35,68)
        poly(bright,12,52,21,58,32,61,27,63,20,61)
        poly(blue,19,66,30,65,40,64,36,69,26,70)
        poly(violet,14,59,19,62,24,64,22,65,17,63)
        rect(light,15,55,3,1);rect(bright,27,67,4,1)
        c.restore()

        // Far-side legs, then the trunk; visible behind the near limbs.
        fun leg(x:Int,root:Int,offset:Int,lift:Int,far:Boolean) {
            val foot=x+offset
            val y=83-lift
            poly(ink,x-5,root,x+5,root,x+7,root+8,foot+6,y-6,
                foot+8,y-4,foot+8,y,foot+5,y+2,foot-5,y+2,
                foot-7,y-1,foot-6,y-6,x-6,root+8)
            val base=if(far)deep else blue
            poly(base,x-4,root+1,x+4,root+1,x+5,root+8,foot+5,y-5,
                foot+6,y-3,foot+6,y,foot-4,y,foot-5,y-2,foot-4,y-6,x-4,root+7)
            poly(if(far)blue else cyan,x-3,root+2,x+2,root+2,x+2,root+8,
                foot+3,y-5,foot-3,y-5,x-3,root+7)
            oval(if(far)cyan else bright,foot-5,y-5,foot+7,y+1)
            rect(if(far)deep else blue,foot-1,y-1,1,2)
            rect(if(far)deep else blue,foot+3,y-1,1,2)
            rect(if(far)bright else white,foot-3,y-4,3,1)
        }
        leg(36,65,stride[0],stride[1],true)
        leg(65,65,stride[2],stride[3],true)
        // Chubby axolotl body, low and horizontal.
        poly(ink,24,58,32,51,44,48,60,48,72,54,76,65,71,75,61,80,44,79,30,75,23,68)
        poly(deep,26,59,34,53,44,50,59,50,70,55,73,65,69,73,60,77,44,77,31,73,25,67)
        poly(blue,28,59,35,54,46,51,60,52,69,58,71,65,65,73,48,75,33,71,27,66)
        poly(cyan,30,57,39,53,50,52,61,54,64,57,54,59,39,59,33,63,28,62)
        poly(belly,54,61,65,58,71,63,67,71,60,75,52,74,47,70,50,65)
        rect(bright,37,55,5,1);rect(0x53DFEE,44,57,3,2)
        rect(deep,33,65,3,2);rect(0x246DA4,39,69,3,2);rect(cyan,46,62,2,2)
        // Near legs articulate independently of the head and torso.
        leg(34,67,stride[4],stride[5],false)
        leg(62,67,stride[6],stride[7],false)

        // Three feathered external gills on the far side, visible above the head.
        val g=intArrayOf(0,-1,1,0)[i]
        c.save();c.translate(0f,g.toFloat())
        poly(ink,66,28,70,15,77,10,82,12,81,18,76,25,73,32,83,23,90,23,93,27,89,32,79,38,71,40)
        poly(deep,68,28,72,16,77,12,80,13,79,18,74,26,72,34,84,25,89,25,90,28,85,32,77,36,72,37)
        poly(cyan,72,24,75,15,78,13,77,20,74,26)
        poly(cyan,77,34,84,27,89,26,87,30,81,33)
        poly(purple,74,27,78,19,79,15,80,18,77,26,75,31)
        rect(bright,85,26,3,1)
        c.restore()
        // The nearest three branched gills, left of the cheek.
        c.save();c.translate(0f,(-g).toFloat())
        poly(ink,51,35,43,25,38,13,33,10,28,11,27,16,32,24,38,32,27,24,18,22,13,25,14,31,22,37,35,41,20,40,13,43,14,49,23,53,38,52,49,46)
        poly(deep,49,36,42,26,36,14,32,12,29,13,30,17,35,25,43,35,34,33,26,27,19,24,16,26,17,30,24,35,39,41,32,44,22,42,16,44,17,47,24,50,37,49,47,45)
        poly(cyan,43,34,36,24,31,15,32,13,35,15,39,24,47,35)
        poly(cyan,38,40,24,33,17,27,20,25,27,29,35,33,44,40)
        poly(cyan,40,46,27,45,19,44,16,46,24,49,37,48)
        poly(violet,46,36,42,28,39,23,37,23,40,31,44,39)
        poly(purple,43,41,35,36,27,32,29,35,37,41,43,44)
        poly(violet,44,46,35,44,27,46,32,48,41,48)
        rect(bright,31,14,3,2);rect(bright,18,26,4,2);rect(light,19,45,4,1)
        rect(0x159EBE,24,29,2,1);rect(bright,34,24,2,1)
        c.restore()
        // Head facing right: rounded forehead, distinct right-hand snout.
        poly(ink,45,36,50,29,59,25,71,25,80,30,85,37,86,45,
            91,49,92,57,88,63,80,67,66,68,52,64,44,58,41,49,42,41)
        poly(deep,46,37,52,31,60,27,70,27,79,32,83,38,84,46,
            89,50,90,56,86,61,79,65,66,66,53,62,46,57,43,49,44,41)
        poly(blue,47,37,54,31,63,28,72,29,80,34,83,43,81,51,
            87,53,86,59,78,63,66,64,53,60,46,55,45,47)
        poly(cyan,48,37,55,32,63,30,72,31,78,35,81,41,78,46,
            67,47,57,44,49,47,46,43)
        poly(bright,54,34,63,31,72,32,76,34,68,35,60,35,55,38,51,38)
        // Pale snout and throat, like the original frontal model.
        poly(belly,52,53,59,54,65,51,74,49,81,48,87,51,89,54,87,59,
            79,63,66,64,56,61,51,57)
        poly(0xC7F9FB,62,55,72,52,82,51,88,54,86,58,78,61,68,61)
        rect(bright,49,47,3,2);rect(0x2AA9D5,50,50,3,2)
        rect(0x5FE4F1,55,40,2,2);rect(0x55DFF2,58,38,2,1)
        rect(light,61,34,2,1);rect(0x67D6ED,77,39,2,2)
        // Forehead diamond and small luminous speckles.
        poly(0x159AB9,71,31,76,37,71,43,66,37)
        poly(bright,71,32,75,37,71,42,67,37)
        poly(white,71,33,74,37,71,40,68,37)
        rect(white,63,38,1,2);rect(bright,65,42,2,1)
        // Large near eye, layered original turquoise/blue/purple palette.
        oval(deep,56,43,74,60)
        oval(ink,57,43,73,59)
        oval(0x222044,58,45,72,59)
        oval(0x354FAC,59,48,71,58)
        oval(0x466FDC,60,50,70,58)
        oval(0x1CCEDF,61,53,70,58)
        oval(0x8EF6FA,62,55,68,58)
        oval(0x101C37,60,44,70,55)
        oval(white,59,45,64,50)
        rect(0xC5FFFF,64,50,2,2);rect(white,68,54,2,2)
        // Far eye follows the profile, smaller and partially occluded.
        oval(ink,79,43,87,53)
        oval(0x28385F,80,44,86,52)
        oval(0x387ACD,81,47,86,52)
        rect(0x75EFF2,82,50,3,1);rect(white,80,44,3,3)
        // Tiny nose and smiling mouth, no duplicate frontal eyes.
        rect(ink,87,53,2,2);rect(deep,86,55,1,2)
        rect(ink,81,57,2,1);rect(ink,83,58,3,1);rect(ink,86,57,1,1)
        rect(0xEA9AC5,74,58,3,1);rect(0xEA9AC5,75,59,2,1)
        rect(white,79,61,3,1)
    }
}
