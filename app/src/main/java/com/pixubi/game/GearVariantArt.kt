package com.pixubi.game
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

private fun DrawScope.gearPlate(c:Color,a:Color,vararg xy:Float){
    val path=Path().apply{moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)lineTo(xy[i],xy[i+1]);close()}
    val b=path.getBounds()
    drawPath(path,Brush.linearGradient(listOf(c,Color(0xFF182336)),Offset(b.left,b.top),Offset(b.right,b.bottom)))
    drawPath(path,Color(0xFF101523),style=Stroke(1.4f))
    drawLine(a,Offset(xy[0],xy[1]),Offset(xy[2],xy[3]),1.5f)
}
internal fun DrawScope.drawVariantHelmet(g:RobotGear?,side:Boolean,a:Color,grade:Int,animal:Boolean=false){
    val a=gearLight(g,a)
    val width=if(animal).83f else 1f+grade*.035f
    scale(width,if(animal).85f else 1f,Offset.Zero){
        when(g){
            RobotGear.SCANNER->{gearPlate(Color(0xFF4D92A9),a,-30f,-32f,-8f,-38f,4f,-25f,-10f,-12f,-30f,-16f);drawCircle(Color(0xFF15283D),8f,Offset(-16f,-24f));drawCircle(a,5f,Offset(-16f,-24f));drawCircle(Color.White,1.8f,Offset(-18f,-26f))}
            RobotGear.HELMET->{gearPlate(Color(0xFFA3B8D0),a,-32f,-20f,-25f,-39f,25f,-39f,32f,-20f,23f,-15f,-23f,-15f);for(s in if(side)listOf(1f)else listOf(-1f,1f))scale(s,1f,Offset.Zero){gearPlate(Color(0xFF627C93),a,-31f,-20f,-23f,-16f,-19f,8f,-29f,11f)}}
            RobotGear.VISOR->gearPlate(Color(0xFF405F79),a,-26f,-26f,25f,-26f,32f,-18f,23f,-13f,-23f,-13f)
            RobotGear.HELMET_SHADOW->{gearPlate(Color(0xFF363B54),a,-32f,-18f,-24f,-40f,7f,-44f,31f,-30f,24f,-24f,-18f,-27f);gearPlate(Color(0xFF40495D),a,-32f,-18f,-22f,-24f,-17f,2f,-27f,12f)}
            RobotGear.HELMET_TITAN->{gearPlate(Color(0xFF8795A7),a,-34f,-20f,-29f,-43f,24f,-43f,34f,-23f,29f,-17f,-24f,-17f);gearPlate(Color(0xFF586779),a,-34f,-20f,-23f,-17f,-20f,13f,-33f,16f);if(!side)scale(-1f,1f,Offset.Zero){gearPlate(Color(0xFF586779),a,-34f,-20f,-23f,-17f,-20f,13f,-33f,16f)}}
            RobotGear.HELMET_PRISM->{gearPlate(Color(0xFFB2ACDA),a,-27f,-26f,0f,-51f,27f,-26f,19f,-20f,0f,-37f,-19f,-20f);drawCircle(a,4f,Offset(0f,-35f))}
            else->Unit
        }
    }
}
internal fun DrawScope.drawVariantArmor(g:RobotGear?,side:Boolean,a:Color,grade:Int,animal:Boolean=false){
    val a=gearLight(g,a)
    val w=if(animal)1.5f else if(side).8f else 1f+grade*.035f
    scale(w,if(animal).72f else 1f,Offset.Zero){
        translate(0f,if(animal)-16f else 0f){
            when(g){
                RobotGear.ARMOR->{gearPlate(Color(0xFF819EB2),a,-27f,-10f,0f,-2f,27f,-10f,22f,23f,0f,37f,-22f,23f);gearPlate(Color(0xFF476981),a,-12f,4f,12f,4f,9f,22f,-9f,22f)}
                RobotGear.REACTOR->{gearPlate(Color(0xFF52697A),a,-15f,0f,15f,0f,21f,16f,12f,31f,-12f,31f,-21f,16f);drawCircle(Color(0xFF172938),11f,Offset(0f,15f));drawCircle(a,9f,Offset(0f,15f),style=Stroke(2f));drawCircle(a,4f,Offset(0f,15f))}
                RobotGear.LIGHT_ARMOR,RobotGear.ARMOR_SHADOW->{
                    val c=if(g==RobotGear.ARMOR_SHADOW)Color(0xFF454961)else Color(0xFF8696A6)
                    gearPlate(c,a,-29f,-10f,-18f,-13f,-10f,17f,-21f,31f,-31f,8f)
                    if(!side||animal)scale(-1f,1f,Offset.Zero){gearPlate(c,a,-29f,-10f,-18f,-13f,-10f,17f,-21f,31f,-31f,8f)}
                }
                RobotGear.HEAVY_ARMOR->{for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){gearPlate(Color(0xFF8494A3),a,13f,-12f,35f,-18f,41f,1f,31f,31f,13f,23f);gearPlate(Color(0xFF4C596B),a,17f,25f,31f,31f,26f,43f,12f,38f)}}
                RobotGear.ARMOR_PRISM->{gearPlate(Color(0xFFB1A6D2),a,-28f,-8f,0f,3f,28f,-8f,25f,11f,0f,28f,-25f,11f);drawCircle(a,5f,Offset(0f,13f))}
                else->Unit
            }
        }
    }
}

/** Family colors belong to the item, blended with the owner's emission color. */
internal fun gearLight(g:RobotGear?,owner:Color):Color{
    if(g==null)return owner
    val c=when(g.name.removeSuffix("_L").removeSuffix("_R")){
        "HELMET_SHADOW","ARMOR_SHADOW","BLADE"->Color(0xFFAC8BFF)
        "HELMET_TITAN","HEAVY_ARMOR","POWERBANK_MAX","CANNON"->Color(0xFFFFB85E)
        "HELMET_PRISM","ARMOR_PRISM","ARC"->Color(0xFFF18DEB)
        "REPAIR","REPAIR_DRONE","TOOL"->Color(0xFF7BEAA9)
        "SCANNER","VISOR","MAG_BOOTS","RIFLE"->Color(0xFF69E7FF)
        "SPEED_BOOTS","BOOSTER"->Color(0xFFFF7F80)
        "BATTERY","POWERBANK_MINI","REACTOR"->Color(0xFFE7DC72)
        else->Color(0xFF9ACFE8)
    }
    return owner.copy(alpha=.23f).compositeOver(c)
}
internal fun DrawScope.drawVariantBoot(g:RobotGear?,side:Boolean,a:Color,grade:Int,animal:Boolean=false){
    val light=gearLight(g,a)
    val sx=if(animal).73f else 1f+grade*.045f
    scale(sx,if(animal).62f else 1f,Offset.Zero){
        val toe=if(side)19f else 12f
        when(g){
            RobotGear.BOOTS->{gearPlate(Color(0xFF829BB2),light,-13f,-6f,9f,-6f,toe+4f,6f,toe+4f,13f,-14f,13f);gearPlate(Color(0xFF374D66),light,-12f,8f,toe+4f,8f,toe+4f,13f,-12f,13f)}
            RobotGear.SPEED_BOOTS->{gearPlate(Color(0xFF536780),light,-17f,-14f,6f,-7f,toe+4f,5f,toe+4f,11f,-15f,11f);gearPlate(Color(0xFFBA6675),light,-17f,-14f,-7f,-8f,-9f,6f,-19f,9f);drawLine(light,Offset(-11f,5f),Offset(toe,5f),2.6f,StrokeCap.Round)}
            RobotGear.MAG_BOOTS->{gearPlate(Color(0xFF577D91),light,-10f,-7f,10f,-7f,toe+5f,4f,toe+5f,13f,-15f,13f);for(x in listOf(-8f,5f)){drawRoundRect(Color(0xFF182B3D),Offset(x-5f,6f),Size(10f,7f),CornerRadius(2f));drawLine(light,Offset(x-3f,10f),Offset(x+3f,10f),2.6f,StrokeCap.Round)}}
            else->Unit
        }
    }
}
internal fun DrawScope.drawVariantPack(g:RobotGear?,side:Boolean,a:Color,grade:Int,animal:Boolean=false){
    val light=gearLight(g,a)
    val sx=if(animal).73f else if(side).84f else 1f+grade*.05f
    scale(sx,if(animal).68f else 1f,Offset.Zero){
        when(g){
            RobotGear.BATTERY->{gearPlate(Color(0xFF7893A5),light,-19f,-10f,19f,-10f,23f,29f,-23f,29f);for(x in listOf(-10f,10f)){drawRoundRect(Color(0xFF182D42),Offset(x-5f,-3f),Size(10f,25f),CornerRadius(3f));drawLine(light,Offset(x,0f),Offset(x,20f),3f,StrokeCap.Round)}}
            RobotGear.POWERBANK_MINI->{gearPlate(Color(0xFF8BB5BF),light,-12f,-13f,12f,-13f,16f,19f,-16f,19f);for(i in 0..2)drawRoundRect(light,Offset(-8f,-5f+i*7f),Size(16f,3f),CornerRadius(1f))}
            RobotGear.POWERBANK_MAX->{gearPlate(Color(0xFFA18D69),light,-25f,-15f,25f,-15f,29f,34f,-29f,34f);for(x in listOf(-16f,0f,16f)){drawRoundRect(Color(0xFF23334A),Offset(x-5f,-7f),Size(10f,34f),CornerRadius(3f));drawLine(light,Offset(x,-3f),Offset(x,22f),4f,StrokeCap.Round)}}
            RobotGear.BOOSTER->{gearPlate(Color(0xFF8194AA),light,-22f,-9f,22f,-9f,18f,20f,-18f,20f);for(x in listOf(-15f,15f)){gearPlate(Color(0xFF4C607B),light,x-8f,-8f,x+8f,-8f,x+6f,30f,x-6f,30f);drawOval(light,Offset(x-5f,26f),Size(10f,6f))}}
            RobotGear.REPAIR_DRONE->{gearPlate(Color(0xFF789EAC),light,-14f,-12f,14f,-12f,21f,3f,14f,18f,-14f,18f,-21f,3f);for(s in listOf(-1f,1f))scale(s,1f,Offset.Zero){gearPlate(Color(0xFF526B86),light,13f,-4f,28f,-15f,30f,5f,18f,10f)};drawCircle(Color(0xFF1C3144),9f,Offset.Zero);drawLine(light,Offset(-5f,0f),Offset(5f,0f),3f);drawLine(light,Offset(0f,-5f),Offset(0f,5f),3f)}
            else->Unit
        }
    }
}
/** Render the same modular art used on a robot in a standalone item tile. */
internal fun DrawScope.drawGearModel(g:RobotGear,owner:Color){
    val d=minOf(size.width,size.height)/100f
    translate(size.width/2,size.height/2){scale(d,d,Offset.Zero){
        when(g.slot){
            RobotSlot.HEAD->translate(0f,18f){drawVariantHelmet(g,false,owner,0)}
            RobotSlot.CHEST->translate(0f,-9f){drawVariantArmor(g,false,owner,0)}
            RobotSlot.FEET->translate(0f,2f){scale(1.65f,1.65f,Offset.Zero){drawVariantBoot(g,true,owner,0)}}
            RobotSlot.BACK->translate(0f,-7f){scale(1.2f,1.2f,Offset.Zero){drawVariantPack(g,false,owner,0)}}
            RobotSlot.LEFT_HAND,RobotSlot.RIGHT_HAND->translate(0f,-12f){drawWorldWeapon(g,owner)}
        }
    }}
}
