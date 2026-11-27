package com.pixubi.game
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
@Composable internal fun PixubiGlyph(id:String,active:Boolean){
    Canvas(Modifier.size(22.dp)){
        val w=size.width;val h=size.height;val c=if(active)Color(0xFF8BF8E7)else Color(0xFF9DBDD7)
        fun line(x:Float,y:Float,u:Float,v:Float){drawLine(c,Offset(x*w,y*h),Offset(u*w,v*h),1.9.dp.toPx(),StrokeCap.Round)}
        fun box(x:Float,y:Float,s:Float,t:Float){drawRoundRect(c,Offset(x*w,y*h),Size(s*w,t*h),CornerRadius(1.2.dp.toPx()))}
        fun poly(vararg a:Float){val p=Path().apply{moveTo(a[0]*w,a[1]*h);for(i in 2 until a.size step 2)lineTo(a[i]*w,a[i+1]*h);close()};drawPath(p,c.copy(alpha=.23f));drawPath(p,c,style=Stroke(1.6.dp.toPx()))}
        when(id){
            "ALL"->for(x in 0..2)for(y in 0..2)box(.12f+x*.29f,.12f+y*.29f,.18f,.18f)
            "WEAPON"->{poly(.20f,.69f,.70f,.15f,.86f,.13f,.83f,.29f,.34f,.81f);line(.14f,.60f,.42f,.88f);line(.25f,.76f,.11f,.90f);line(.14f,.22f,.52f,.59f)}
            "HEAD"->{poly(.12f,.34f,.29f,.14f,.71f,.14f,.88f,.34f,.81f,.79f,.62f,.85f,.62f,.57f,.38f,.57f,.38f,.85f,.19f,.79f);line(.25f,.38f,.75f,.38f)}
            "CHEST"->{poly(.10f,.25f,.31f,.14f,.50f,.31f,.69f,.14f,.90f,.25f,.73f,.75f,.50f,.88f,.27f,.75f);line(.50f,.38f,.50f,.72f)}
            "HAND"->{poly(.18f,.41f,.28f,.18f,.42f,.18f,.42f,.39f,.48f,.12f,.63f,.12f,.63f,.39f,.75f,.26f,.87f,.36f,.76f,.70f,.62f,.87f,.32f,.87f);line(.32f,.62f,.68f,.62f)}
            "FEET"->{poly(.25f,.12f,.57f,.12f,.57f,.54f,.86f,.67f,.86f,.85f,.20f,.85f,.20f,.58f);line(.26f,.69f,.76f,.69f)}
            "BACK"->{poly(.24f,.24f,.76f,.24f,.76f,.76f,.24f,.76f);box(.40f,.40f,.20f,.20f);for(i in 0..2){val z=.31f+i*.19f;line(z,.10f,z,.22f);line(z,.78f,z,.90f);line(.10f,z,.22f,z);line(.78f,z,.90f,z)}}
            "SUPPLY"->{poly(.25f,.20f,.75f,.20f,.75f,.88f,.25f,.88f);box(.39f,.08f,.22f,.13f);line(.53f,.31f,.39f,.54f);line(.39f,.54f,.61f,.54f);line(.61f,.54f,.46f,.77f)}
            "home"->{poly(.10f,.45f,.50f,.12f,.90f,.45f,.80f,.45f,.80f,.87f,.20f,.87f,.20f,.45f);box(.43f,.58f,.16f,.29f)}
            "missions"->{poly(.22f,.14f,.78f,.14f,.78f,.88f,.22f,.88f);for(y in listOf(.35f,.55f,.73f)){line(.35f,y,.65f,y)}}
            "adventure"->{drawCircle(c,w*.39f,Offset(w*.5f,h*.5f),style=Stroke(1.6.dp.toPx()));poly(.27f,.73f,.42f,.42f,.74f,.26f,.58f,.58f)}
            "shop"->{poly(.16f,.34f,.84f,.34f,.89f,.85f,.11f,.85f);drawArc(c,180f,180f,false,Offset(w*.32f,h*.10f),Size(w*.36f,h*.40f),style=Stroke(1.7.dp.toPx()))}
            "construction"->{poly(.12f,.87f,.12f,.37f,.42f,.37f,.42f,.12f,.83f,.12f,.83f,.87f);for(y in listOf(.31f,.50f,.69f))box(.55f,y,.12f,.10f)}
            "inventory"->{poly(.12f,.30f,.50f,.11f,.88f,.30f,.88f,.75f,.50f,.91f,.12f,.75f);line(.12f,.30f,.50f,.49f);line(.88f,.30f,.50f,.49f);line(.50f,.49f,.50f,.91f)}
            "stats"->{box(.14f,.60f,.18f,.28f);box(.41f,.38f,.18f,.50f);box(.68f,.14f,.18f,.74f)}
            "archive"->{for(x in listOf(.13f,.39f,.65f))poly(x,.16f,x+.21f,.16f,x+.21f,.86f,x,.86f)}
            else->{drawCircle(c,w*.18f,Offset(w*.5f,h*.29f));poly(.16f,.86f,.22f,.61f,.50f,.50f,.78f,.61f,.84f,.86f)}
        }
    }
}
