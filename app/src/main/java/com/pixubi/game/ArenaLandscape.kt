package com.pixubi.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random
import kotlin.math.*

/** Original landscape: continuous terrain, rather than an artificial floating board. */
internal fun DrawScope.arenaLandscape(plane:ArenaProjection,stage:Int){
    val horizon=plane.project(3f,-.5f).y.coerceIn(size.height*.08f,size.height*.45f)
    val rng=Random(stage*113+41)
    val biome=(stage-1)%4
    val base=when(biome){0->Color(0xFF536C43);1->Color(0xFF596B72);2->Color(0xFF846F4C);else->Color(0xFF435B61)}
    val shade=when(biome){0->Color(0xFF273D2D);1->Color(0xFF384851);2->Color(0xFF524832);else->Color(0xFF293C49)}
    val sun=when(biome){0->Color(0xFFA9AF68);1->Color(0xFFBAC8C6);2->Color(0xFFC6A672);else->Color(0xFF77AAA3)}
    drawRect(Brush.verticalGradient(listOf(Color(0xFF233C52),Color(0xFF91A7AA),Color(0xFFC3C6B0)),endY=horizon))
    fun ridge(color:Color,baseY:Float,amplitude:Float,seed:Int){
        val random=Random(seed)
        val p=Path().apply{
            moveTo(0f,baseY)
            for(i in 0..24){lineTo(size.width*i/24f,baseY-amplitude*(.25f+random.nextFloat()*.75f))}
            lineTo(size.width,horizon+3f);lineTo(0f,horizon+3f);close()
        }
        drawPath(p,color)
    }
    ridge(Color(0xFF778D96),horizon,horizon*.62f,stage+12)
    ridge(Color(0xFF50696D),horizon,horizon*.38f,stage+91)
    ridge(shade,horizon+3f,horizon*.15f,stage+22)
    drawRect(Brush.verticalGradient(listOf(base,base,shade),startY=horizon,endY=size.height),
        Offset(0f,horizon),Size(size.width,size.height-horizon))
    // Atmospheric texture, independent of grid; pixel clusters grow toward the foreground.
    repeat(2600){i->
        val y=horizon+rng.nextFloat()*(size.height-horizon)
        val depth=(y-horizon)/(size.height-horizon).coerceAtLeast(1f)
        val x=rng.nextFloat()*size.width
        val step=(size.width/300f).coerceAtLeast(1f)*(.55f+depth*.95f)
        val col=when(i%5){0->sun.copy(alpha=.24f);1->shade.copy(alpha=.38f);2->sun.copy(alpha=.10f);else->base.copy(alpha=.75f)}
        drawRect(col,Offset(x,y),Size(step*(1+rng.nextInt(3)),step*.58f))
    }
    // Trees and technological ruins only along the distant skyline, never on traversable cells.
    repeat(45){i->
        val x=rng.nextFloat()*size.width;val y=horizon+rng.nextFloat()*plane.row*.10f
        val h=plane.row*(.11f+rng.nextFloat()*.33f)
        if(i%6==0){
            drawRect(shade,Offset(x-h*.22f,y-h),Size(h*.42f,h))
            drawRect(sun.copy(alpha=.6f),Offset(x-h*.22f,y-h),Size(h*.42f,h*.07f))
            drawRect(Color(0xFF73D9D4).copy(alpha=.65f),Offset(x-h*.055f,y-h*.83f),Size(h*.11f,h*.35f))
        }else{
            val p=Path().apply{moveTo(x,y-h);lineTo(x-h*.33f,y);lineTo(x+h*.33f,y);close()}
            drawPath(p,shade)
        }
    }
    // Scattered edge rocks and small plants outside the playable cell polygons.
    repeat(90){
        val x=rng.nextFloat()*size.width;val y=horizon+rng.nextFloat()*(size.height-horizon)
        if(plane.hit(Offset(x,y))==null){
            val r=plane.cell*(.014f+rng.nextFloat()*.04f)
            drawOval(shade.copy(alpha=.8f),Offset(x-r,y-r*.3f),Size(r*2f,r*.75f))
            drawRect(sun.copy(alpha=.5f),Offset(x-r*.4f,y-r*.6f),Size(r,r*.55f))
        }
    }
}
