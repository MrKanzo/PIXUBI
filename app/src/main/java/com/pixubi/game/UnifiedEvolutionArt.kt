package com.pixubi.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Color
import android.util.LruCache

/** A shared native pixel grid, palette and pose vocabulary for every mature species.
 * Each frame is drawn from anatomy, not a warped image or overlaid eyes.
 * Existing illustrated baby packs remain independent. */
internal object UnifiedEvolutionArt {
    const val GRID = 64
    private data class Key(val species:Species,val stage:GrowthStage,val sex:Sex,val gene:Int,
        val clip:SpriteClip,val frame:Int,val blink:Boolean)
    private val cache by lazy { LruCache<Key,Bitmap>(256) }
    fun matches(stage:GrowthStage)=stage==GrowthStage.YOUNG||stage==GrowthStage.ADULT
    fun locomotion(clip:SpriteClip)=clip in listOf(SpriteClip.WALK,SpriteClip.HOP,SpriteClip.DIG,SpriteClip.SWIM,SpriteClip.FLY)
    private fun rgb(n:Int)=n or 0xff000000.toInt()
    private fun mix(a:Int,b:Int,t:Float):Int {
        fun ch(s:Int)=(((a shr s and 255)*(1-t)+(b shr s and 255)*t).toInt().coerceIn(0,255))
        return Color.rgb(ch(16),ch(8),ch(0))
    }
    @Synchronized fun get(species:Species,stage:GrowthStage,sex:Sex,gene:Int,clip:SpriteClip,frame:Int,blink:Boolean):Bitmap {
        require(matches(stage))
        val key=Key(species,stage,sex,gene.coerceIn(0,5),clip,frame.mod(clip.count),blink)
        return cache.get(key)?:draw(key).also{cache.put(key,it)}
    }
    private fun draw(k:Key):Bitmap {
        val out=Bitmap.createBitmap(GRID,GRID,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(out)
        val paint=Paint().apply{isAntiAlias=false;isDither=false}
        val ink=rgb(0x17182b)
        val armor=rgb(0x283648)
        val steel=rgb(0x586b80)
        val lightSteel=rgb(0xa6bacc)
        val p=when(k.species){
            Species.LISYK -> intArrayOf(0xf28b37,0xffe0a7,0x38ddec)
            Species.ISKRYK -> intArrayOf(0xffc32c,0xffe8ae,0xc448ff)
            Species.KRAPLYK -> intArrayOf(0x38b9dd,0xa8f5fa,0x9873ff)
            Species.PISHCHANYK -> intArrayOf(0xe6a260,0xffe5ba,0xffa22d)
            Species.KHMARYK -> intArrayOf(0x9374db,0xe6ddff,0x50e0fc)
        }
        val accent=rgb(p[2])
        val tint=rgb(intArrayOf(0xf3a65d,0x88dfc1,0x9b9dfb,0xf29cbf,0x79ccfa,0xf6d471)[k.gene])
        val fur=mix(rgb(p[0]),tint,if(k.gene==0)0f else .19f)
        val cream=rgb(p[1])
        val shade=mix(fur,ink,.40f)
        val highlight=mix(fur,cream,.47f)
        val darkAccent=mix(accent,ink,.50f)
        val adult=k.stage==GrowthStage.ADULT
        val side=locomotion(k.clip)||k.clip==SpriteClip.SLEEP
        val sleeping=k.clip==SpriteClip.SLEEP
        val digging=k.clip==SpriteClip.DIG
        val swimming=k.clip==SpriteClip.SWIM
        val flying=k.clip==SpriteClip.FLY
        val step=intArrayOf(-3,0,3,0)[k.frame%4]
        val lift=intArrayOf(0,2,0,2)[k.frame%4]
        val hop=if(k.clip==SpriteClip.HOP&&k.frame%4 in 1..2)3 else 0
        val headDrop=if(digging)5 else if(sleeping)9 else 0
        val headX=if(side)42 else 32
        val headY=(if(adult)25 else 27)+headDrop-hop
        val r=if(adult)12 else 13
        fun box(x:Int,y:Int,w:Int,h:Int,c:Int){
            paint.color=c;canvas.drawRect(x.toFloat(),y.toFloat(),(x+w).toFloat(),(y+h).toFloat(),paint)
        }
        fun poly(c:Int,vararg xy:Int){
            paint.color=c
            val path=Path();path.moveTo(xy[0].toFloat(),xy[1].toFloat())
            for(i in 2 until xy.size step 2)path.lineTo(xy[i].toFloat(),xy[i+1].toFloat())
            path.close();canvas.drawPath(path,paint)
        }
        // Pixel ellipses have manually shaded edges; all coordinates are on the same grid.
        fun ellipse(x:Int,y:Int,rx:Int,ry:Int,c:Int,border:Int=ink){
            for(dy in -ry..ry)for(dx in -rx..rx){
                val q=dx*dx*ry*ry+dy*dy*rx*rx
                if(q<=rx*rx*ry*ry){
                    val inner=(rx-1)*(rx-1)*(ry-1)*(ry-1)
                    val iq=dx*dx*(ry-1)*(ry-1)+dy*dy*(rx-1)*(rx-1)
                    box(x+dx,y+dy,1,1,if(iq>inner)border else c)
                }
            }
        }
        fun gem(x:Int,y:Int,size:Int){
            poly(ink,x,y-size-1,x+size+1,y,x,y+size+1,x-size-1,y)
            poly(accent,x,y-size,x+size,y,x,y+size,x-size,y)
            box(x-1,y-2,2,2,Color.WHITE)
        }
        fun paw(x:Int,y:Int,back:Boolean){
            ellipse(x,y-3,3,5,if(back)shade else fur)
            ellipse(x+1,y,4,2,if(back)shade else cream)
            box(x+2,y,1,1,shade)
        }
        fun wing(x:Int,y:Int,up:Boolean){
            val tipY=y+if(up)-16 else 9
            poly(ink,x+4,y-6,x-6,tipY,x-18,tipY-2,x-15,y+1,x-10,y+8,x+4,y+6)
            poly(shade,x+3,y-4,x-6,tipY+2,x-15,tipY,x-12,y+1,x-8,y+6,x+2,y+4)
            poly(highlight,x+1,y-3,x-6,tipY+3,x-12,tipY+2,x-9,y+1,x-5,y+3)
            box(x-9,y+2,5,1,cream)
            if(adult)box(x-13,tipY+2,4,2,accent)
        }
        // Rear silhouette: each species owns its own tail, fins, feathers or fur.
        when(k.species){
            Species.KRAPLYK -> {
                val wave=if(swimming)step else 0
                poly(ink,29,43,16,38,5,30+wave,3,43+wave,11,49+wave,23,50,31,48)
                poly(fur,28,44,15,40,6,33+wave,5,43+wave,12,47+wave,24,48)
                poly(highlight,5,35+wave,14,40,22,44,10,43+wave)
                if(adult)poly(accent,5,32+wave,11,37+wave,4,40+wave)
            }
            Species.KHMARYK -> {
                ellipse(12,44,8,7,cream);ellipse(10,42,4,4,highlight)
                wing(if(side)29 else 23,40,flying&&k.frame%4 in 0..1)
            }
            else -> {
                val tip=if(sleeping)42 else 32+step/2
                poly(ink,29,48,16,51,6,46,3,tip+3,8,tip-5,16,tip-2,21,38,32,40)
                poly(shade,28,47,16,49,8,44,5,tip+3,9,tip-3,15,tip,20,40,30,42)
                poly(fur,7,tip+2,10,tip-2,15,tip+1,19,41,26,44,16,47,9,43)
                val tailTip=if(k.species==Species.ISKRYK)accent else cream
                poly(tailTip,6,tip+2,9,tip-3,15,tip,17,tip+5,11,tip+7)
                box(9,tip,3,2,Color.WHITE)
            }
        }
        val bodyX=if(side)30 else 32
        val bodyY=if(sleeping)46 else if(swimming||flying)41-hop else 43-hop
        if(side&&!sleeping&&!swimming&&!flying){
            paw(bodyX-8-step,54-lift,true);paw(bodyX+9+step,54-(2-lift),true)
        }
        ellipse(bodyX,bodyY,if(adult)16 else 13,if(sleeping)8 else 11,shade)
        ellipse(bodyX,bodyY-2,if(adult)15 else 12,if(sleeping)6 else 9,fur)
        poly(highlight,bodyX-9,bodyY-8,bodyX+3,bodyY-9,bodyX+9,bodyY-5,bodyX-3,bodyY-3)
        if(!sleeping){
            // Different armor silhouettes rather than a scale-only stage change.
            val left=bodyX-if(adult)11 else 8
            val right=bodyX+if(adult)11 else 8
            poly(ink,left,bodyY-7,right,bodyY-7,right+2,bodyY+2,bodyX,bodyY+10,left-2,bodyY+2)
            poly(armor,left+1,bodyY-6,right-1,bodyY-6,right,bodyY+2,bodyX,bodyY+8,left,bodyY+2)
            poly(steel,left+2,bodyY-5,right-2,bodyY-5,bodyX,bodyY+1)
            gem(bodyX,bodyY+1,if(adult)3 else 2)
            if(adult){
                box(left,bodyY+1,3,5,lightSteel);box(right-2,bodyY+1,3,5,steel)
                box(left,bodyY+4,2,1,accent);box(right-1,bodyY+4,2,1,accent)
            }
        }
        if(!sleeping){
            if(side){
                val y=if(swimming||flying)bodyY+7 else 55-hop
                paw(bodyX-8+step,y-(2-lift),false)
                paw(bodyX+10-step,y-lift-(if(digging&&k.frame%2==0)4 else 0),false)
            }else{
                paw(24,55-hop,false);paw(40,55-hop,false)
            }
        }else ellipse(37,51,7,3,cream)
        if(k.species==Species.KHMARYK)wing(if(side)31 else 45,39,flying&&k.frame%4 in 0..1)
        // Ear and gill silhouettes are drawn behind the face.
        fun ear(x:Int,dir:Int){
            val tall=when(k.species){Species.PISHCHANYK->17;Species.KHMARYK->11;else->if(adult)14 else 12}
            poly(ink,x-dir*5,headY-7,x+dir*2,headY-tall-7,x+dir*9,headY-9,x+dir*7,headY-3)
            poly(fur,x-dir*3,headY-8,x+dir*2,headY-tall-4,x+dir*7,headY-9,x+dir*5,headY-5)
            poly(if(k.species==Species.KHMARYK)cream else rgb(0xf7aeae),x,headY-9,x+dir*2,headY-tall,x+dir*5,headY-9)
        }
        if(k.species==Species.KRAPLYK){
            for(dir in if(side)intArrayOf(-1) else intArrayOf(-1,1)){
                for(i in 0..2){
                    val x=headX+dir*10
                    val y=headY-8+i*7
                    val tip=x+dir*(if(adult)12 else 9)
                    poly(ink,x,y-2,tip,y-7+i*3,tip+dir*2,y-3+i*3,x,y+4)
                    poly(darkAccent,x,y-1,tip,y-5+i*3,tip+dir,y-3+i*3,x,y+2)
                    box(tip-1,y-4+i*3,3,2,highlight)
                }
            }
        }else{
            ear(headX-7,-1);ear(headX+7,1)
        }
        ellipse(headX,headY,r,11,shade)
        ellipse(headX,headY-1,r-1,10,fur)
        poly(highlight,headX-r+3,headY-5,headX-4,headY-9,headX+4,headY-9,headX+7,headY-5,headX,headY-3)
        if(k.species==Species.ISKRYK){
            for(i in 0..2)poly(shade,headX-9+i*8,headY-8,headX-7+i*8,headY-10,headX-5+i*8,headY-5)
            poly(accent,headX+1,headY-12,headX-3,headY-5,headX+1,headY-5,headX-1,headY+1,headX+6,headY-7,headX+2,headY-7)
        }else if(k.species==Species.PISHCHANYK){
            ellipse(headX-5,headY-9,4,3,armor);ellipse(headX+4,headY-9,4,3,armor)
            box(headX-7,headY-10,4,2,accent);box(headX+2,headY-10,4,2,accent)
        }else if(k.species==Species.KRAPLYK||k.species==Species.KHMARYK)gem(headX,headY-9,3)
        else{
            poly(armor,headX-r,headY-7,headX-6,headY-13,headX+4,headY-12,headX+8,headY-9,headX-5,headY-10)
            ellipse(headX-r+2,headY,4,5,armor)
            ellipse(headX-r+2,headY,2,3,accent,steel)
        }
        if(adult&&k.species!=Species.PISHCHANYK){
            box(headX-5,headY-11,2,2,lightSteel);box(headX+3,headY-11,2,2,lightSteel)
        }
        // Muzzle and eye pixels are part of the newly rasterized full frame.
        val muzzleX=headX+if(side)5 else 0
        ellipse(muzzleX,headY+5,if(side)7 else 9,5,cream,shade)
        val closed=k.blink||sleeping||k.clip==SpriteClip.HAPPY||(k.clip==SpriteClip.EAT&&k.frame%2==1)||(k.clip==SpriteClip.DRINK&&k.frame%2==1)
        val eyes=if(side)intArrayOf(headX+2) else intArrayOf(headX-6,headX+6)
        for(x in eyes){
            val y=headY-1
            if(closed){
                box(x-3,y,2,1,ink);box(x-1,y-1,3,1,ink);box(x+2,y,2,1,ink)
            }else{
                ellipse(x,y,4,5,Color.WHITE)
                ellipse(x+1,y+1,3,4,darkAccent,ink)
                box(x,y+1,2,3,ink);box(x-1,y-2,2,2,Color.WHITE)
                box(x+2,y+2,1,1,accent)
                if(k.clip==SpriteClip.TIRED)box(x-4,y-4,9,3,fur)
                if(k.clip==SpriteClip.SAD)box(x-3,y-4,3,1,shade)
            }
            if(k.sex==Sex.GIRL&&!closed)box(x+3,y-4,2,1,ink)
        }
        box(muzzleX-1,headY+3,3,2,ink)
        when{
            k.clip==SpriteClip.SURPRISE||k.clip==SpriteClip.HAPPY||k.clip==SpriteClip.EAT&&k.frame%2==1 -> {
                ellipse(muzzleX,headY+8,3,2,ink);box(muzzleX-1,headY+8,3,1,rgb(0xff879b))
            }
            k.clip==SpriteClip.SAD -> {box(muzzleX-2,headY+8,5,1,ink);box(muzzleX-3,headY+9,1,1,ink);box(muzzleX+3,headY+9,1,1,ink)}
            else -> {box(muzzleX-2,headY+7,2,1,ink);box(muzzleX,headY+8,2,1,ink)}
        }
        if(k.sex==Sex.GIRL){box(headX-r+3,headY-9,3,2,accent);box(headX-r+1,headY-10,2,4,cream)}
        if(k.clip==SpriteClip.EAT&&k.frame%2==1){ellipse(muzzleX,headY+12,3,3,rgb(0xdca352));box(muzzleX,headY+11,1,1,shade)}
        if(k.clip==SpriteClip.DRINK&&k.frame%2==1){box(muzzleX-3,headY+10,6,5,ink);box(muzzleX-2,headY+11,4,3,rgb(0x55d9f8))}
        return out
    }
}
