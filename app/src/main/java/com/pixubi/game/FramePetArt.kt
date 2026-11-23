package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color

/**
 * New original 112px character artwork. Each keyframe is a complete transparent
 * bitmap, drawn from a blank canvas. No old atlas sampling, mesh or face patches.
 * Feet, tails, ears, wings and expressions are authored as different silhouettes.
 */
internal object FramePetArt {
    private const val N=112
    private val ink=Color.rgb(35,32,56)
    private val white=Color.rgb(255,243,232)
    private val blush=Color.rgb(242,137,151)
    private val dark=Color.rgb(37,45,69)
    private val metal=Color.rgb(83,98,131)
    private val neon=Color.rgb(80,236,225)
    private data class Palette(val fur:Int,val shade:Int,val light:Int,val eye:Int)
    private fun rgb(s:Long)= (0xff000000L or s).toInt()
    private fun mix(a:Int,b:Int,t:Float)=Color.rgb(
        (Color.red(a)*(1-t)+Color.red(b)*t).toInt(),
        (Color.green(a)*(1-t)+Color.green(b)*t).toInt(),
        (Color.blue(a)*(1-t)+Color.blue(b)*t).toInt())
    private fun palette(species:Species,gene:Int):Palette {
        val base=when(species) {
            Species.LISYK -> rgb(0xef8b42)
            Species.ISKRYK -> rgb(0xeebc46)
            Species.KRAPLYK -> rgb(0x46c7d9)
            Species.PISHCHANYK -> rgb(0xd9a76c)
            Species.KHMARYK -> rgb(0xa7a1ed)
        }
        val colors=intArrayOf(base,rgb(0x80c98b),rgb(0x6bb2ec),rgb(0xae87e5),rgb(0xe987bb),rgb(0xe4cf66))
        val f=colors[gene.coerceIn(0,5)]
        return Palette(f,mix(f,ink,.40f),mix(f,white,.34f),
            if(species==Species.ISKRYK) rgb(0xab84fb) else rgb(0x56d6e2))
    }
    private class Brush(val c:Canvas) {
        val p=Paint().apply { isAntiAlias=false; isFilterBitmap=false; strokeJoin=Paint.Join.ROUND; strokeCap=Paint.Cap.ROUND }
        fun oval(x:Float,y:Float,rx:Float,ry:Float,color:Int,edge:Boolean=true) {
            p.style=Paint.Style.FILL;p.color=if(edge) ink else color
            c.drawOval(x-rx,y-ry,x+rx,y+ry,p)
            if(edge && rx>1.5f && ry>1.5f) {
                p.color=color;c.drawOval(x-rx+1.5f,y-ry+1.5f,x+rx-1.5f,y+ry-1.5f,p)
            }
        }
        fun box(x:Float,y:Float,w:Float,h:Float,color:Int) {
            p.style=Paint.Style.FILL;p.color=color;c.drawRect(x,y,x+w,y+h,p)
        }
        fun polygon(color:Int,vararg coords:Float,edge:Boolean=true) {
            val path=Path();path.moveTo(coords[0],coords[1])
            for(i in 2 until coords.size step 2) path.lineTo(coords[i],coords[i+1])
            path.close();p.style=Paint.Style.FILL;p.color=color;c.drawPath(path,p)
            if(edge) {p.style=Paint.Style.STROKE;p.strokeWidth=1.6f;p.color=ink;c.drawPath(path,p);p.style=Paint.Style.FILL}
        }
        fun line(color:Int,width:Float,vararg coords:Float) {
            val path=Path();path.moveTo(coords[0],coords[1])
            for(i in 2 until coords.size step 2) path.lineTo(coords[i],coords[i+1])
            p.style=Paint.Style.STROKE;p.strokeWidth=width;p.color=color;c.drawPath(path,p);p.style=Paint.Style.FILL
        }
        fun curve(color:Int,width:Float,x1:Float,y1:Float,cx:Float,cy:Float,x2:Float,y2:Float) {
            val path=Path();path.moveTo(x1,y1);path.quadTo(cx,cy,x2,y2)
            p.style=Paint.Style.STROKE;p.strokeWidth=width;p.color=color;c.drawPath(path,p);p.style=Paint.Style.FILL
        }
    }

    fun render(species:Species,stage:GrowthStage,sex:Sex,gene:Int,emotion:PetEmotion,
               clip:SpriteClip,frame:Int,blink:Boolean):Bitmap {
        val bmp=Bitmap.createBitmap(N,N,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bmp)
        val b=Brush(canvas)
        val p=palette(species,gene)
        val sleep=clip==SpriteClip.SLEEP
        val owl=species==Species.KHMARYK
        val water=species==Species.KRAPLYK
        val baby=stage==GrowthStage.BABY
        val teen=stage==GrowthStage.YOUNG
        val walking=clip==SpriteClip.WALK || clip==SpriteClip.DIG
        val flying=clip==SpriteClip.FLY
        val swimming=clip==SpriteClip.SWIM
        val leap=clip==SpriteClip.HAPPY || clip==SpriteClip.HOP || clip==SpriteClip.SURPRISE
        val wag=when(frame%4){0->-4f;1->0f;2->4f;else->0f}
        val rise=if(leap && frame%4 in 1..2) -4f else 0f
        val headBob=when {sleep->if(frame%2==0) 0f else 1f; walking->if(frame%2==0) 0f else 2f;else->rise}
        // Younger creatures have a visibly smaller torso and softer proportions.
        val bodyX=58f
        val bodyY=if(sleep) 81f else if(baby) 77f else 76f
        val bodyRx=if(sleep) 27f else if(baby) 18f else 22f
        val bodyRy=if(sleep) 14f else if(baby) 22f else 26f
        val headX=if(sleep) 43f else if(walking || swimming) 52f else 56f
        val headY=(if(sleep) 67f else if(baby) 43f else 39f)+headBob
        val hw=if(baby) 25f else 24f
        val hh=if(baby) 23f else 23f
        // Tail or wings are complete newly drawn shapes for each keyframe.
        if(owl) {
            val lift=if(flying || leap) (if(frame%2==0) -13f else 8f) else wag*1.5f
            for(side in listOf(-1f,1f)) {
                val x=bodyX+side*(bodyRx-2)
                b.polygon(p.shade,x,bodyY-17,x+side*22,bodyY-28+lift,
                    x+side*25,bodyY-11+lift,x+side*18,bodyY+12+lift,
                    x+side*7,bodyY+17)
                b.polygon(p.light,x+side*3,bodyY-14,x+side*19,bodyY-23+lift,
                    x+side*17,bodyY-4+lift,x+side*6,bodyY+8,edge=false)
                for(k in 0..2) b.line(white,1.6f,x+side*(7+k*4),bodyY-5+lift*.7f,
                    x+side*(11+k*4),bodyY+5+lift*.8f)
            }
        } else if(water) {
            val tipY=bodyY-12+wag*2
            b.polygon(p.shade,75f,bodyY+8,88f,bodyY+1,102f,tipY,
                96f,tipY+16,103f,tipY+24,87f,bodyY+16,75f,bodyY+17)
            b.polygon(p.light,86f,bodyY+5,99f,tipY+4,94f,tipY+15,edge=false)
            b.line(neon,1.5f,84f,bodyY+10,94f,tipY+12)
        } else {
            val tx=if(sleep) 91f else 92f
            val ty=if(sleep) 75f else 65f+wag*2f
            // Root at rump, then a broad, tapered tail with a colored tip.
            b.polygon(p.shade,72f,bodyY+9,84f,bodyY+11,tx+10,ty+10,
                tx+12,ty-4,tx+4,ty-14,tx-3,ty-7,tx-8,ty+1,77f,bodyY-1)
            b.polygon(p.fur,78f,bodyY+6,87f,bodyY+4,tx+7,ty+7,
                tx+9,ty-3,tx+2,ty-10,tx-5,ty+3,edge=false)
            b.polygon(white,tx+2,ty-10,tx+9,ty-3,tx+7,ty+6,
                tx-2,ty+4,tx-4,ty-2,edge=false)
            if(species==Species.ISKRYK) b.line(p.light,3f,tx-6,ty+4,tx+1,ty,
                tx-2,ty-4,tx+5,ty-8)
            else b.line(neon,2f,84f,bodyY+4,87f,bodyY+10)
        }
        // Four distinct paws on land; two webbed feet on aquatic pets and birds.
        val step=SpriteTimeline.stride(frame)
        fun foot(x:Float,y:Float,dx:Float,dy:Float,far:Boolean) {
            val color=if(far) p.shade else p.fur
            val length=if(baby) 10f else 13f
            b.polygon(color,x-5,y-length,x+5,y-length,x+dx+6,y+dy-3,
                x+dx+6,y+dy+2,x+dx-7,y+dy+2,x+dx-7,y+dy-3)
            b.oval(x+dx,y+dy-1,7.5f,4.5f,if(owl) rgb(0xefae4c) else white)
            if(!far) b.line(p.shade,1f,x+dx-2,y+dy,x+dx-2,y+dy+2)
        }
        val ground=100f+rise
        fun dx(i:Int)=if(walking) step[i].toFloat() else if(swimming) step[i]*.65f else 0f
        fun dy(i:Int)=if(walking||swimming) step[i].toFloat() else if(flying) -5f else 0f
        if(!sleep) {
            if(!owl && !water) {
                foot(47f,ground-4,dx(2),dy(3),true)
                foot(74f,ground-4,dx(0),dy(1),true)
            }
            b.oval(bodyX,bodyY+rise,bodyRx,bodyRy,p.shade)
            b.oval(bodyX-2,bodyY+rise-2,bodyRx-3,bodyRy-4,p.fur,false)
            b.oval(bodyX-6,bodyY+rise+4,bodyRx*.53f,bodyRy*.76f,white,false)
            foot(if(owl||water) 47f else 44f,ground,dx(0),dy(1),false)
            foot(if(owl||water) 69f else 67f,ground,dx(2),dy(3),false)
        } else {
            b.oval(bodyX,bodyY,bodyRx,bodyRy,p.fur)
            b.oval(bodyX-7,bodyY+3,bodyRx*.6f,bodyRy*.65f,white,false)
            b.oval(47f,94f,11f,5f,white)
            b.oval(67f,94f,10f,5f,p.light)
        }
        // Collar and growing cyber armor.
        if(!sleep) {
            b.polygon(dark,bodyX-15,bodyY-12+rise,bodyX+13,bodyY-12+rise,
                bodyX+9,bodyY+2+rise,bodyX-7,bodyY+5+rise)
            b.polygon(neon,bodyX-5,bodyY-8+rise,bodyX+5,bodyY-8+rise,
                bodyX,bodyY-1+rise,edge=false)
            if(!baby) {
                b.polygon(metal,bodyX+8,bodyY+4+rise,bodyX+19,bodyY+1+rise,
                    bodyX+17,bodyY+13+rise,bodyX+8,bodyY+16+rise)
                b.line(neon,1.8f,bodyX+10,bodyY+8+rise,bodyX+16,bodyY+6+rise)
            }
            if(teen) b.box(bodyX+15,bodyY+9+rise,3f,4f,neon)
        }
        // Ears, gills or owl horn feathers, placed behind the full head.
        for(side in listOf(-1f,1f)) {
            val ex=headX+side*17
            when(species) {
                Species.KRAPLYK -> for(k in 0..2) {
                    val gy=headY-11+k*9
                    b.polygon(p.shade,ex,gy,ex+side*(16+k%2*3),gy-12,
                        ex+side*21,gy-8,ex+side*15,gy+5,ex,gy+7)
                    b.line(p.light,2f,ex+side*4,gy,ex+side*16,gy-7)
                }
                Species.KHMARYK -> {
                    b.polygon(p.light,ex-side*6,headY-14,ex+side*8,headY-29,
                        ex+side*11,headY-17,ex+side*5,headY-8)
                }
                else -> {
                    val tall=if(species==Species.PISHCHANYK) 35f else 29f
                    b.polygon(p.fur,ex-side*8,headY-10,ex+side*11,headY-tall,
                        ex+side*15,headY-12,ex+side*6,headY+4)
                    b.polygon(if(species==Species.ISKRYK) p.light else blush,
                        ex-side*3,headY-12,ex+side*9,headY-tall+6,
                        ex+side*10,headY-13,edge=false)
                    if(species==Species.ISKRYK) b.line(neon,2f,ex+side*7,headY-tall+3,
                        ex+side*1,headY-19,ex+side*6,headY-18)
                }
            }
        }
        // Head and muzzle are new art, not reconstructed portions of old eyes.
        b.oval(headX,headY,hw,hh,p.shade)
        b.oval(headX,headY-2,hw-2,hh-3,p.fur,false)
        b.oval(headX-6,headY-9,hw*.72f,hh*.55f,p.light,false)
        if(owl) {
            b.oval(headX-10,headY+3,12f,15f,white,false)
            b.oval(headX+10,headY+3,12f,15f,white,false)
        } else {
            b.oval(headX-10,headY+12,14f,10f,white,false)
            b.oval(headX+10,headY+12,14f,10f,white,false)
        }
        // Iconic forehead tuft/mark distinguishes the five species.
        when(species) {
            Species.ISKRYK -> b.polygon(rgb(0xfdf0a2),headX-3,headY-20,headX+6,headY-20,
                headX+1,headY-10,headX+7,headY-10,headX-3,headY,headX-1,headY-9,
                headX-7,headY-9)
            Species.KRAPLYK -> { b.oval(headX,headY-15,5f,6f,neon);b.box(headX-1,headY-17,2f,2f,white) }
            Species.KHMARYK -> b.polygon(neon,headX,headY-23,headX+6,headY-16,
                headX,headY-9,headX-6,headY-16)
            else -> b.polygon(p.light,headX-9,headY-18,headX-6,headY-25,headX,headY-19,
                headX+7,headY-25,headX+11,headY-15,edge=false)
        }
        // Species' accessories.
        if(species==Species.PISHCHANYK) {
            for(side in listOf(-1f,1f)) {
                b.oval(headX+side*10,headY-20,8f,5.5f,dark)
                b.oval(headX+side*10,headY-20,5f,3f,rgb(0xffcd7a),false)
            }
            b.line(dark,2f,headX-3,headY-20,headX+3,headY-20)
        } else if(!owl && !water) {
            for(side in listOf(-1f,1f)) {
                b.oval(headX+side*(hw-1),headY+2,6f,10f,dark)
                b.oval(headX+side*(hw-1),headY+2,3.5f,7f,neon,false)
                b.oval(headX+side*(hw-1),headY+2,2f,5f,metal,false)
            }
        }
        val happy=emotion==PetEmotion.HAPPY
        val sad=emotion==PetEmotion.SAD
        val tired=emotion==PetEmotion.TIRED
        val surprised=emotion==PetEmotion.SURPRISED
        val chewing=clip==SpriteClip.EAT || clip==SpriteClip.DRINK
        val closed=sleep||blink||(happy && frame%4==1)||(chewing && frame%4==2)
        val ey=headY+1
        for(side in listOf(-1f,1f)) {
            val x=headX+side*10
            if(closed) {
                b.curve(ink,2.2f,x-6,ey,x,ey+(if(happy&&!sleep) -6f else 5f),x+6,ey)
            } else {
                val r=if(tired) 3.7f else if(surprised) 8.8f else 7.2f
                b.oval(x,ey,7.4f,r,ink)
                b.oval(x,ey+1,5.4f,(r-1.5f).coerceAtLeast(2f),p.eye,false)
                b.oval(x+if(walking) -1f else 0f,ey-1,3.9f,(r-2f).coerceAtLeast(1.5f),ink,false)
                b.oval(x-2,ey-3,1.8f,1.8f,white,false)
                if(sad) b.line(p.shade,1.8f,x-5,ey-8-side*2,x+5,ey-8+side*2)
                else if(tired) b.line(p.shade,1.8f,x-6,ey-3,x+6,ey-3)
            }
            if(happy||sad) {
                b.line(blush,2f,x-4,ey+9,x+2,ey+9)
                if(sad) b.oval(x+side*5,ey+7,1.7f,3f,p.eye,false)
            }
        }
        val my=headY+15
        if(owl) {
            val open=surprised || (chewing && frame%2==1) || (happy && frame%2==1)
            b.polygon(rgb(0xf7b64c),headX-5,my-6,headX+5,my-6,headX,my+if(open) 6 else 2)
            if(open) b.line(ink,1.8f,headX-2,my-1,headX+2,my-1)
        } else {
            b.polygon(ink,headX-3,my-6,headX+3,my-6,headX,my-3,edge=false)
            when {
                surprised || (tired && frame%4==2) || (clip==SpriteClip.DRINK && frame%2==1) ->
                    b.oval(headX,my+1,3.6f,4.4f,ink,false)
                (clip==SpriteClip.EAT && frame%2==1)||(happy && frame%2==1) -> {
                    b.oval(headX,my+1,6f,3.7f,ink,false)
                    b.oval(headX,my+3,3f,1.5f,blush,false)
                }
                sad -> b.curve(ink,1.6f,headX-5,my+3,headX,my-3,headX+5,my+3)
                sleep -> b.curve(ink,1.5f,headX-3,my,headX,my+1,headX+3,my)
                else -> b.curve(ink,1.7f,headX-5,my-1,headX,my+5,headX+5,my-1)
            }
        }
        if(sex==Sex.GIRL) {
            val bx=headX+hw-5;val by=headY-19
            b.polygon(blush,bx,by,bx-9,by-5,bx-9,by+5)
            b.polygon(blush,bx,by,bx+8,by-5,bx+8,by+5)
            b.oval(bx,by,3f,3f,rgb(0xffc9e4))
        }
        return bmp
    }
}
