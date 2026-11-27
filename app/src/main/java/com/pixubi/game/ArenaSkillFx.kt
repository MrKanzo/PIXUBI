package com.pixubi.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

internal enum class ArenaSkillFx(val title:String,val millis:Int){
    IMPULSE("Бойовий імпульс",740), PRECISION("Точний постріл",700),
    ASSAULT("Посилений удар",650), DOME("Захисний купол",820),
    REPAIR("Масовий ремонт",900), SIEGE("Перевантаження",900),
    ARC("Енергоімпульс",800);
    companion object{
        fun forActor(kind:SquadKind?)=when(kind){
            SquadKind.RANGER->PRECISION;SquadKind.ASSAULT->ASSAULT;SquadKind.GUARD->DOME
            SquadKind.MEDIC->REPAIR;SquadKind.SIEGE->SIEGE;SquadKind.AERIAL->ARC;null->IMPULSE
        }
    }
}

/** Affected cells match the rule engine. Capture before the action so destroyed
 * targets and newly repaired allies retain their original visual anchor. */
internal fun skillFxCells(b:ArenaBattle,actor:Int,index:Int):List<GridPoint>{
    val kind=b.allies.getOrNull(actor)?.kind
    val source=SquadRules.position(b,actor)
    val friends=buildList{
        if(b.hp>0&&(kind!=SquadKind.MEDIC||b.hp<b.maxHp))add(b.pos)
        b.allies.filter{it.hp>0&&(kind!=SquadKind.MEDIC||it.hp<it.maxHp)}.forEach{add(it.pos)}
    }
    if(kind==SquadKind.GUARD)return friends
    if(kind==SquadKind.MEDIC)return friends.filter{AdventureRules.arenaDistance(source,it)<=2}
    val target=b.enemies.getOrNull(index)?.takeIf{it.hp>0}?:return emptyList()
    val radius=when(kind){SquadKind.SIEGE->1;SquadKind.AERIAL->2;else->0}
    return b.enemies.filter{it.hp>0&&AdventureRules.arenaDistance(target.pos,it.pos)<=radius}.map{it.pos}
}

internal fun DrawScope.drawSkillFx(kind:ArenaSkillFx,plane:ArenaProjection,source:GridPoint,target:GridPoint,affected:List<GridPoint>,progress:Float){
    val t=progress.coerceIn(0f,1f)
    val w=min(plane.cell,plane.row*1.45f)
    fun torso(q:GridPoint)=plane.foot(q)-Offset(0f,w*.38f)
    val from=torso(source);val to=torso(target)
    val impact=((t-.35f)/.65f).coerceIn(0f,1f)
    val opacity=(1f-impact).coerceIn(0f,1f)
    val cyan=Color(0xFF78F5FF);val gold=Color(0xFFFFCF80);val green=Color(0xFF8CFFB3)
    fun beam(a:Offset,b:Offset,color:Color,width:Float,alpha:Float){
        drawLine(color.copy(alpha=alpha*.20f),a,b,width*3f,StrokeCap.Round)
        drawLine(color.copy(alpha=alpha),a,b,width,StrokeCap.Round)
    }
    fun burst(at:Offset,col:Color,radius:Float){
        if(t<.35f)return
        val r=w*(.12f+impact*radius)
        drawCircle(Brush.radialGradient(listOf(col.copy(alpha=opacity*.36f),Color.Transparent),at,r.coerceAtLeast(1f)),r,at)
        drawCircle(col.copy(alpha=opacity*.8f),r,at,style=Stroke(w*.035f))
        repeat(10){i->
            val a=i*6.283185f/10f
            val v=Offset(cos(a),sin(a))*r
            drawLine(col.copy(alpha=opacity),at+v*.72f,at+v,w*.025f,StrokeCap.Round)
        }
    }
    fun groundRing(q:GridPoint,col:Color,r:Float,alpha:Float){
        val f=plane.foot(q)
        drawOval(col.copy(alpha=alpha*.18f),f-Offset(r,r*.48f),Size(r*2f,r*.96f))
        drawOval(col.copy(alpha=alpha),f-Offset(r,r*.48f),Size(r*2f,r*.96f),style=Stroke(w*.025f))
    }
    when(kind){
        ArenaSkillFx.PRECISION->{
            if(t<.35f){
                val r=w*(.45f-t*.6f)
                drawCircle(gold.copy(alpha=.85f),r,to,style=Stroke(w*.022f))
                for(i in 0..3){val a=i*1.5708f;val v=Offset(cos(a),sin(a))*r
                    drawLine(gold,to+v*.60f,to+v*1.25f,w*.022f)}
            }else if(t<.52f)beam(from,to,gold,w*.065f,1f)
            burst(to,gold,.52f)
        }
        ArenaSkillFx.ASSAULT->{
            if(t<.65f){
                val sweep=(t/.65f).coerceIn(0f,1f)
                val r=w*.63f
                drawArc(gold.copy(alpha=1f-sweep*.45f),-145f,205f*sweep,false,to-Offset(r,r),Size(2*r,2*r),style=Stroke(w*.14f,cap=StrokeCap.Round))
                drawArc(Color.White.copy(alpha=.85f),-125f,170f*sweep,false,to-Offset(r*.86f,r*.86f),Size(r*1.72f,r*1.72f),style=Stroke(w*.04f))
            }
            burst(to,gold,.60f)
        }
        ArenaSkillFx.IMPULSE->{
            if(t<.5f){val v=from+(to-from)*(t*2f)
                beam(from,v,cyan,w*.10f,.95f);drawCircle(Color.White,w*.08f,v)}
            burst(to,cyan,.65f)
        }
        ArenaSkillFx.DOME->{
            val glow=sin(PI.toFloat()*t).coerceIn(0f,1f)
            affected.forEach{q->
                val f=plane.foot(q);val r=w*(.32f+.25f*glow)
                groundRing(q,cyan,r,glow)
                drawArc(cyan.copy(alpha=glow*.18f),180f,180f,true,f-Offset(r,r*1.22f),Size(r*2f,r*2.05f))
                drawArc(cyan.copy(alpha=glow),180f,180f,false,f-Offset(r,r*1.22f),Size(r*2f,r*2.05f),style=Stroke(w*.045f))
            }
        }
        ArenaSkillFx.REPAIR->{
            val glow=sin(PI.toFloat()*t).coerceIn(0f,1f)
            affected.forEach{q->
                val f=plane.foot(q);groundRing(q,green,w*(.27f+t*.30f),glow)
                if(q!=source)beam(from,torso(q),green,w*.025f,glow*.7f)
                for(i in 0..2){val k=(t+i/3f)%1f
                    val at=f+Offset((i-1)*w*.18f,-w*(.22f+k*.80f))
                    val a=sin(PI.toFloat()*k).coerceIn(0f,1f)*glow
                    drawLine(green.copy(alpha=a),at-Offset(w*.055f,0f),at+Offset(w*.055f,0f),w*.028f)
                    drawLine(green.copy(alpha=a),at-Offset(0f,w*.055f),at+Offset(0f,w*.055f),w*.028f)
                }
            }
        }
        ArenaSkillFx.SIEGE->{
            val k=(t/.35f).coerceIn(0f,1f)
            if(t<.35f){val at=from+(to-from)*k-Offset(0f,sin(k*PI.toFloat())*w*.75f)
                drawCircle(gold.copy(alpha=.40f),w*.17f,at);drawCircle(Color.White,w*.075f,at)}
            affected.forEach{q->burst(torso(q),gold,.80f);if(t>.35f)groundRing(q,gold,w*(.2f+impact*.70f),opacity*.8f)}
        }
        ArenaSkillFx.ARC->{
            val violet=Color(0xFFBDABFF)
            if(t<.65f){
                val alpha=sin(PI.toFloat()*t/.65f).coerceIn(0f,1f)
                fun bolt(a:Offset,b:Offset,seed:Int){
                    var prev=a
                    val delta=b-a;val length=delta.getDistance().coerceAtLeast(1f)
                    val normal=Offset(-delta.y/length,delta.x/length)
                    for(j in 1..7){val f=j/7f
                        val next=a+delta*f+normal*(if(j==7)0f else sin(j*5f+seed+t*28f)*w*.10f)
                        beam(prev,next,violet,w*.04f,alpha);prev=next}
                }
                bolt(from,to,0)
                affected.filter{it!=target}.forEachIndexed{i,q->bolt(to,torso(q),i+1)}
            }
            affected.forEach{burst(torso(it),violet,.48f)}
        }
    }
}
