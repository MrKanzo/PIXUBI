package com.pixubi.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

// Three-quarter projection. All art and input share the exact same logical coordinates.
internal object FrontierProjection{
    const val W=1820f
    const val H=970f
    fun center(q:GridPoint)=Offset(78f+q.x*100f+(9-q.y)*17f,80f+q.y*91f)
    fun corners(q:GridPoint):List<Offset>{
        val c=center(q)
        return listOf(c+Offset(-41.5f,-45.5f),c+Offset(58.5f,-45.5f),c+Offset(41.5f,45.5f),c+Offset(-58.5f,45.5f))
    }
    fun at(x:Float,y:Float):GridPoint?{
        val ry=(y-80f)/91f
        val row=ry.roundToInt()
        val col=((x-78f-(9f-ry)*17f)/100f).roundToInt()
        val q=GridPoint(col,row)
        return q.takeIf{AdventureRules.inMap(it)&&abs(ry-row)<=.5f}
    }
}
private data class FrontierPalette(val name:String,val grass:Color,val light:Color,val dark:Color,val water:Color,val leaves:Color,val glow:Color)
private fun palette(stage:Int)=when((stage-1)%4){
    0->FrontierPalette("Неоновий мегасектор",Color(0xFF334B50),Color(0xFF647E83),Color(0xFF202F42),Color(0xFF167D9C),Color(0xFF426B80),Color(0xFF6CE7FB))
    1->FrontierPalette("Кристалові руїни",Color(0xFF5F7184),Color(0xFF8A9DA2),Color(0xFF38465F),Color(0xFF25589C),Color(0xFF6C87C6),Color(0xFF85E8FF))
    2->FrontierPalette("Біотехнологічний сектор",Color(0xFF3E5D56),Color(0xFF67968B),Color(0xFF243B42),Color(0xFF255F68),Color(0xFF8B5CB4),Color(0xFFB7FC9B))
    else->FrontierPalette("Промисловий каньйон",Color(0xFF62585A),Color(0xFF958477),Color(0xFF3D3948),Color(0xFF286D7C),Color(0xFFD78A53),Color(0xFFFFD389))
}
@Composable
internal fun FrontierMap(p:WorldPet,onChange:(WorldPet)->Unit,onSound:(PixubiSound)->Unit){
    val a=p.adventure
    val latest by rememberUpdatedState(p)
    val change by rememberUpdatedState(onChange)
    val sound by rememberUpdatedState(onSound)
    var target by remember(a.stage){mutableStateOf<GridPoint?>(null)}
    var zoom by remember{mutableIntStateOf(1)}
    var follow by remember{mutableStateOf(true)}
    var centerRequest by remember{mutableIntStateOf(0)}
    var handledCenter by remember{mutableIntStateOf(0)}
    var direction by remember{mutableStateOf(RobotView.RIGHT)}
    val phase by rememberGamePhase(7000,RenderBudget.SCENERY_FPS)
    val gait by rememberGamePhase(WorldMotion.period(p.series,RobotAction.WALK),enabled=target!=null)
    val idle by rememberGamePhase(WorldMotion.period(p.series,RobotAction.IDLE),RenderBudget.SCENERY_FPS)
    val foreground=LocalPixubiForeground.current
    LaunchedEffect(target,a.stage,foreground){
        if(!foreground)return@LaunchedEffect
        val goal=target?:return@LaunchedEffect
        // One logical step per animation; interruption never grants unearned resources.
        var current=latest
        val route=AdventureRules.mapRoute(current.adventure,goal)
        for(q in route){
            current=latest
            if(!AdventureRules.canExplore(current))break
            direction=if(q.x<current.adventure.pos.x||q.x==current.adventure.pos.x&&q.y>current.adventure.pos.y)RobotView.LEFT else RobotView.RIGHT
            val next=AdventureRules.move(current,q,System.currentTimeMillis())
            if(next==current)break
            change(next);sound(PixubiSound.TAP)
            if(next.adventure.battle!=null||next.adventure.pos!=q||AdventureRules.tile(current.adventure,q) in listOf(AdventureTile.LAB,AdventureTile.CACHE))break
            delay(340L)
        }
        target=null
    }
    val colors=palette(a.stage)
    Row(verticalAlignment=Alignment.CenterVertically){
        Column(Modifier.weight(1f)){
            Text(colors.name,style=MaterialTheme.typography.titleLarge)
            Text(if(target!=null)"Робот прямує до цілі" else "Сектор ${a.stage} · відкрий територію",style=MaterialTheme.typography.labelSmall)
            val explored=(0 until AdventureRules.MAP_W*AdventureRules.MAP_H).count{AdventureRules.visible(a,GridPoint(it%AdventureRules.MAP_W,it/AdventureRules.MAP_W))}
            Text("Відкрито ${explored*100/(AdventureRules.MAP_W*AdventureRules.MAP_H)}% · $explored/160 зон",style=MaterialTheme.typography.labelSmall,color=Color(0xFF9CE2C7))
        }
        OutlinedButton(onClick={zoom=(zoom+1)%3}){Text(listOf("Огляд","2,5×","3,5×")[zoom])}
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
        FilterChip(follow,{follow=!follow},label={Text("Стежити за роботом")})
        OutlinedButton(onClick={centerRequest++}){Text("До робота")}
    }
    BoxWithConstraints(Modifier.fillMaxWidth()){
        val mapWidth=maxWidth*listOf(1f,2.5f,3.5f)[zoom]
        val mapHeight=mapWidth*(FrontierProjection.H/FrontierProjection.W)
        val route=remember(a,target){target?.let{AdventureRules.mapRoute(a,it)}?:emptyList()}
        val viewportHeight=minOf(mapHeight,maxWidth*1.05f)
        val hScroll=rememberScrollState()
        val vScroll=rememberScrollState()
        val density=LocalDensity.current
        val center=FrontierProjection.center(a.pos)
        val cx=with(density){(mapWidth*(center.x/FrontierProjection.W)-maxWidth/2).toPx().roundToInt()}
        val cy=with(density){(mapHeight*(center.y/FrontierProjection.H)-viewportHeight/2).toPx().roundToInt()}
        LaunchedEffect(a.pos,zoom,follow,centerRequest){
            if(follow||centerRequest!=handledCenter){
                handledCenter=centerRequest
                delay(35)
                coroutineScope{
                    launch{hScroll.animateScrollTo(cx.coerceIn(0,hScroll.maxValue),tween(240))}
                    launch{vScroll.animateScrollTo(cy.coerceIn(0,vScroll.maxValue),tween(240))}
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(viewportHeight).clip(RoundedCornerShape(14.dp)).background(Color(0xFF182E36))){
          Column(Modifier.fillMaxSize().verticalScroll(vScroll)){
            Row(Modifier.fillMaxWidth().horizontalScroll(hScroll)){
            Box(Modifier.width(mapWidth).height(mapHeight)){
                Canvas(Modifier.fillMaxSize().pointerInput(a, target, p.energy, p.integrity,p.mode,p.mission){
                    detectTapGestures{tap->
                        if(target==null&&AdventureRules.canExplore(p)){
                            val q=FrontierProjection.at(tap.x/size.width*FrontierProjection.W,tap.y/size.height*FrontierProjection.H)
                            if(q!=null&&AdventureRules.mapRoute(a,q).isNotEmpty())target=q
                        }
                    }
                }){
                    scale(size.width/FrontierProjection.W,size.height/FrontierProjection.H,Offset.Zero){
                        landscape(a,colors,phase,route)
                    }
                }
                val actorSize=mapWidth*(96f/FrontierProjection.W)
                for(y in 0 until AdventureRules.MAP_H)for(x in 0 until AdventureRules.MAP_W){
                    val q=GridPoint(x,y)
                    if(AdventureRules.knownTile(a,q)==AdventureTile.ENEMY){
                        val c=FrontierProjection.center(q)
                        Box(Modifier.offset(mapWidth*(c.x/FrontierProjection.W)-actorSize/2,mapHeight*(c.y/FrontierProjection.H)-actorSize*.83f)
                            .size(actorSize).zIndex(c.y)){
                            RobotPreview(RobotView.LEFT,RobotAction.IDLE,RobotFace.ANGRY,RobotLoadout(RobotSkin.RED.accent,RobotEquipment(),RobotTier.entries[(p.tier.ordinal-1).coerceAtLeast(0)],AdventureRules.enemySeries(a,q)),idle,false,Modifier.fillMaxSize())
                        }
                    }
                }
                val c=FrontierProjection.center(a.pos)
                val px by animateDpAsState(mapWidth*(c.x/FrontierProjection.W)-actorSize/2,tween(315,easing=LinearEasing),label="hero x")
                val py by animateDpAsState(mapHeight*(c.y/FrontierProjection.H)-actorSize*.83f,tween(315,easing=LinearEasing),label="hero y")
                Box(Modifier.offset(px,py).size(actorSize).zIndex(c.y+1)){
                    Canvas(Modifier.fillMaxSize()){
                        drawOval(Color(0xFF0C2021).copy(alpha=.55f),Offset(size.width*.17f,size.height*.78f),Size(size.width*.66f,size.height*.18f))
                        drawOval(Color(0xFFB9F8C2),Offset(size.width*.14f,size.height*.75f),Size(size.width*.72f,size.height*.22f),style=Stroke(1.5.dp.toPx()))
                    }
                    RobotPreview(direction,if(target!=null)RobotAction.WALK else RobotAction.IDLE,RobotFace.NEUTRAL,RobotLoadout(p.skin.accent,p.equipment,p.tier,p.series),if(target!=null)gait else idle,false,Modifier.fillMaxSize())
                }
                // Draw the opaque shroud last, above terrain AND every actor.
                Box(Modifier.fillMaxSize().zIndex(10000f).then(remember(a.pos,a.seen){
                    Modifier.cachedScene{
                        scale(size.width/FrontierProjection.W,size.height/FrontierProjection.H,Offset.Zero){fogOfWar(a)}
                    }
                }))
            }
            }
          }
        }
    }
    FrontierOverview(a)
    Text("Темне — невідоме · приглушене — розвідане · яскраве — зона сканера",style=MaterialTheme.typography.labelSmall)
    if(target!=null)TextButton(onClick={target=null}){Text("Зупинити рух")}
}
private fun DrawScope.shape(c:Color,vararg xy:Float){
    val path=Path().apply{moveTo(xy[0],xy[1]);for(i in 2 until xy.size step 2)lineTo(xy[i],xy[i+1]);close()}
    drawPath(path,c)
}
private fun DrawScope.poly(points:List<Offset>,color:Color){
    drawPath(Path().apply{moveTo(points.first().x,points.first().y);points.drop(1).forEach{lineTo(it.x,it.y)};close()},color)
}
private fun DrawScope.oval(c:Color,x:Float,y:Float,rx:Float,ry:Float)=drawOval(c,Offset(x-rx,y-ry),Size(rx*2,ry*2))
private fun DrawScope.landscape(a:AdventureState,p:FrontierPalette,phase:Float,route:List<GridPoint>){
    val water=Path().apply{
        val first=FrontierProjection.center(GridPoint(4,0))
        moveTo(first.x+15f,0f)
        for(y in 0 until AdventureRules.MAP_H){
            val c=FrontierProjection.center(GridPoint(4,y))
            quadraticBezierTo(c.x+(if(y%2==0)21f else -21f),c.y-42f,c.x,c.y)
        }
        val last=FrontierProjection.center(GridPoint(4,AdventureRules.MAP_H-1))
        lineTo(last.x-18f,FrontierProjection.H)
    }
    drawRect(Brush.verticalGradient(listOf(p.light,p.grass,p.dark),endY=FrontierProjection.H))
    // Uneven meadow patches, no visible gameplay grid.
    val random=Random(a.stage*271)
    // Stable pixel clusters: vegetation, stone chips and meadow texture.
    repeat(4600){i->
        val x=(random.nextInt((FrontierProjection.W/4).toInt())*4).toFloat();val y=(random.nextInt((FrontierProjection.H/4).toInt())*4).toFloat()
        val tone=when(i%5){0->p.dark.copy(alpha=.37f);1->p.light.copy(alpha=.46f);2->Color(0xFFDFD99A).copy(alpha=.22f);else->p.grass.copy(alpha=.62f)}
        drawRect(tone,Offset(x,y),Size((1+random.nextInt(3))*4f,4f))
    }
    repeat(530){
        val x=random.nextFloat()*FrontierProjection.W;val y=random.nextFloat()*FrontierProjection.H
        oval(if(it%2==0)p.light.copy(alpha=.20f)else p.dark.copy(alpha=.16f),x,y,7f+random.nextFloat()*32,4f+random.nextFloat()*15)
    }
    drawPath(water,p.dark.copy(alpha=.82f),style=Stroke(108f,cap=StrokeCap.Round))
    drawPath(water,Color(0xFF7C9273).copy(alpha=.78f),style=Stroke(98f,cap=StrokeCap.Round))
    drawPath(water,Color(0xFF233A3D),style=Stroke(88f,cap=StrokeCap.Round))
    drawPath(water,p.water,style=Stroke(69f,cap=StrokeCap.Round))
    drawPath(water,p.glow.copy(alpha=.17f),style=Stroke(33f,cap=StrokeCap.Round))
    drawPath(water,Color(0xFFB5E8DE).copy(alpha=.24f),style=Stroke(3f,cap=StrokeCap.Round))
    drawPath(water,p.glow.copy(alpha=.20f),style=Stroke(57f,cap=StrokeCap.Round))
    val riverMeasure=PathMeasure().apply{setPath(water,false)}
    for(i in 0..23){
        val at=riverMeasure.getPosition(((phase+i/24f)%1f)*riverMeasure.length)
        drawLine(p.glow.copy(alpha=.25f),at+Offset(-13f,-2f),at+Offset(12f,5f),2f,StrokeCap.Round)
    }
    // Stepped cliffs and the animated river gorge; only on blocked river cells.
    for(y in listOf(2,3,4,7,8)){
        val c=FrontierProjection.center(GridPoint(4,y))
        translate(c.x,c.y){
            for(side in listOf(-1f,1f)){
                val x=side*34f
                shape(Color(0xFF253F43),x-12f,-35f,x+10f,-39f,x+18f,21f,x-17f,27f)
                shape(Color(0xFF50655D),x-12f,-35f,x+10f,-39f,x+15f,-22f,x-14f,-18f)
                for(j in 0..7){val yy=-15f+j*5f
                    drawLine(Color(0xFF7C8774).copy(alpha=.5f),Offset(x-11f,yy),Offset(x+12f,yy-4f),2f)}
            }
            if(y==3||y==8){
                for(i in 0..9){
                    val t=(phase*2f+i*.1f)%1f
                    val x=-17f+i*4f
                    drawLine(Color(0xFF91E8E3).copy(alpha=.35f+t*.35f),Offset(x,-24f+t*40f),Offset(x-4f,-13f+t*40f),3f)
                }
                for(i in 0..7){val t=(phase+i/8f)%1f;oval(Color(0xFFB7F5E6).copy(alpha=(1-t)*.4f),0f,30f,8f+t*23f,2f+t*5f)}
            }
        }
    }
    // Road network and bridges on passable crossings.
    val roads=mutableListOf(
        listOf(GridPoint(0,2),GridPoint(1,2),GridPoint(2,2),GridPoint(3,2),GridPoint(3,1),GridPoint(3,0),GridPoint(4,0),GridPoint(5,0),GridPoint(6,0),GridPoint(7,0)),
        listOf(GridPoint(3,2),GridPoint(3,3),GridPoint(3,4),GridPoint(3,5),GridPoint(4,5),GridPoint(5,5),GridPoint(6,5),GridPoint(7,5)),
        listOf(GridPoint(5,0),GridPoint(5,1),GridPoint(5,2),GridPoint(5,3),GridPoint(5,4),GridPoint(5,5)),
        listOf(GridPoint(5,2),GridPoint(6,2),GridPoint(7,2),GridPoint(7,3),GridPoint(7,4),GridPoint(7,5)),
        listOf(GridPoint(1,2),GridPoint(1,1)),listOf(GridPoint(2,2),GridPoint(2,3)),listOf(GridPoint(3,1),GridPoint(4,1),GridPoint(5,1)))
    // Extended road network: east-west links, north-south routes and southern passages.
    for(y in listOf(0,2,5))roads.add((7 until AdventureRules.MAP_W).map{GridPoint(it,y)})
    for(y in listOf(6,9))roads.add((0 until AdventureRules.MAP_W).map{GridPoint(it,y)})
    for(x in listOf(8,12,15))roads.add((0 until AdventureRules.MAP_H).map{GridPoint(x,it)})
    for(x in listOf(0,3,5,7))roads.add((5 until AdventureRules.MAP_H).map{GridPoint(x,it)})
    // Rounded corners are real quadratic curves, not just round stroke caps.
    val roadPaths=roads.map{roundedRoad(it.map(FrontierProjection::center))}
    // Layer all roads together: junctions don't have dark seams.
    for(path in roadPaths)drawPath(path,p.dark.copy(alpha=.75f),style=Stroke(36f,cap=StrokeCap.Round,join=StrokeJoin.Round))
    for(path in roadPaths)drawPath(path,Color(0xFF354552),style=Stroke(29f,cap=StrokeCap.Round,join=StrokeJoin.Round))
    for(path in roadPaths)drawPath(path,Color(0xFF536573),style=Stroke(23f,cap=StrokeCap.Round,join=StrokeJoin.Round))
    for(path in roadPaths)drawPath(path,Color(0xFF233746),style=Stroke(13f,cap=StrokeCap.Round,join=StrokeJoin.Round))
    for(path in roadPaths){
        val measure=PathMeasure().apply{setPath(path,false)}
        for(j in 0..6){val f=(phase+j/7f)%1f;val at=measure.getPosition(f*measure.length);oval(p.glow.copy(alpha=.24f),at.x,at.y,9f,5f);drawCircle(p.glow,2.7f,at)}
        val count=(measure.length/8f).toInt()
        for(i in 1..count){
            val distance=(i*8f).coerceAtMost(measure.length)
            val at=measure.getPosition(distance)
            val tangent=measure.getTangent(distance)
            val o=at+Offset(-tangent.y,tangent.x)*((i%5-2)*3f)
            drawRect(if(i%2==0)p.glow.copy(alpha=.65f)else Color(0xFF627F8D),o,Size(4f,2f))
        }
    }
    // Grass, flowers, crystal clusters and scenic rubble, placed away from the center of walkable cells.
    for(y in 0 until AdventureRules.MAP_H)for(x in 0 until AdventureRules.MAP_W){
        val q=GridPoint(x,y);val c=FrontierProjection.center(q)
        if(!AdventureRules.visible(a,q))continue
        // Use terrain only, never a resource's presence, for remembered decoration.
        val wall=AdventureRules.tile(a,q)==AdventureTile.WALL
        for(i in 0..3){
            val dx=if(i%2==0)-31f else 32f
            val dy=if(i<2)-28f else 29f
            val o=c+Offset(dx,dy)
            if(x==4&&y in listOf(2,3,4,7,8))continue
            oval(p.dark.copy(alpha=.28f),o.x,o.y,10f,4f)
            for(j in 0..2)drawLine(if(j==1)p.light else p.dark,Offset(o.x+j*3f-3f,o.y+1f),Offset(o.x+j*5f-5f,o.y-6f-j),1.5f)
            if((x+y+i)%3==0)drawCircle(p.leaves,2.2f,o+Offset(0f,-6f))
        }
        if(wall&&x!=4){
            translate(c.x,c.y){
                if((x+y)%3==0){techTower(p,phase,x+y)}else if((x+y)%2==0){
                    translate(-27f,-13f){scale(.65f,.65f,Offset.Zero){tree(p,phase,x+y+4)}}
                    translate(24f,-4f){scale(.75f,.75f,Offset.Zero){tree(p,phase,x+y+7)}}
                    tree(p,phase,x+y)
                }else if(x==6||x==1)rockRidge(p,x+y)else ruins(p)
            }
        }else if(!wall&&!AdventureRules.mainRoad(q)&&q!=AdventureRules.base&&x!=4&&(x+y)%3==0){
            translate(c.x+34f,c.y-28f){scale(.46f,.46f,Offset.Zero){tree(p,phase,x+y)}}
        }
    }
    // Small data pylons beside roads, never over resources or hidden objects.
    for(y in 0 until AdventureRules.MAP_H)for(x in 0 until AdventureRules.MAP_W){
        val q=GridPoint(x,y);if(!AdventureRules.visible(a,q)||!AdventureRules.mainRoad(q)||(x+y)%4!=0)continue
        val c=FrontierProjection.center(q);translate(c.x+31f,c.y-25f){scale(.45f,.45f,Offset.Zero){techTower(p,phase,x+y)}}
    }
    // The bridges are navigable map nodes, not decorative obstacles.
    for(y in listOf(0,1,5,6,9)){
        val c=FrontierProjection.center(GridPoint(4,y))
        translate(c.x,c.y){
            oval(Color(0xFF173A40).copy(alpha=.65f),0f,8f,42f,19f)
            shape(Color(0xFF4F5B60),-43f,-10f,41f,-17f,45f,10f,-41f,17f)
            shape(Color(0xFF96938B),-43f,-15f,41f,-22f,45f,4f,-41f,11f)
            for(i in 0..7){val x=-37f+i*10f;drawLine(Color(0xFF515657),Offset(x,-15f-i*.8f),Offset(x+3f,10f-i*.8f),1.6f)}
            for(y2 in listOf(-17f,9f)){drawLine(Color(0xFF414D5B),Offset(-44f,y2),Offset(43f,y2-7f),3f);for(i in 0..3)drawRect(p.glow,Offset(-43f+i*28f,y2-i*2f-4f),Size(3f,5f))}
        }
    }
    for(y in 0 until AdventureRules.MAP_H)for(x in 0 until AdventureRules.MAP_W){
        val q=GridPoint(x,y);val c=FrontierProjection.center(q)
        translate(c.x,c.y){when(AdventureRules.knownTile(a,q)){
            AdventureTile.BASE->baseBuilding(p)
            AdventureTile.COINS->crate(p,Color(0xFFEBC45C),0)
            AdventureTile.ENERGY->crate(p,Color(0xFF63DFF8),1)
            AdventureTile.REPAIR->crate(p,Color(0xFF7BF2A5),2)
            AdventureTile.AMMO->crate(p,Color(0xFFF7A65B),3)
            AdventureTile.LAB->frontierSite(p,0,phase)
            AdventureTile.CACHE->frontierSite(p,1,phase)
            AdventureTile.VAULT->frontierSite(p,2,phase)
            AdventureTile.ENEMY->{if(q==AdventureRules.encounters(a).last())portal(p,phase)else{oval(Color(0xFF4C3036).copy(alpha=.65f),0f,4f,28f,13f);drawOval(Color(0xFFE36C77),Offset(-27f,-9f),Size(54f,24f),style=Stroke(2f))}}
            else->Unit
        }}
    }
    // Edge forest / rock faces, never place obstacles on walkable centers.
    for(i in 0..18){
        val x=18f+i*95f
        translate(x,27f){scale(.58f,.58f,Offset.Zero){if(i%3==0)ruins(p)else tree(p,phase,i)}}
        translate(x,FrontierProjection.H-34f){scale(.62f,.62f,Offset.Zero){if(i%3==1)ruins(p)else tree(p,phase,i+3)}}
    }
    // Floating motes only above explored terrain, not a resource marker.
    for(i in 0..63){
        val x=(i*157%1770+25).toFloat()+sin(phase*6.283f+i)*7f
        val y=(i*97%890+40).toFloat()+cos(phase*6.283f+i)*5f
        val q=FrontierProjection.at(x,y)
        if(q!=null&&AdventureRules.inSight(a,q)){
            val alpha=.15f+.30f*(.5f+.5f*sin(phase*6.283f+i))
            drawRect(p.glow.copy(alpha=alpha),Offset(x,y),Size(3f,3f))
        }
    }
    // Route preview, only over reachable terrain.
    if(route.isNotEmpty()){
        var prev=FrontierProjection.center(a.pos)
        route.forEach{q->val next=FrontierProjection.center(q);drawLine(p.glow.copy(alpha=.7f),prev,next,3f,StrokeCap.Round);drawCircle(p.glow,3.5f,next);prev=next}
    }
    drawRect(Brush.radialGradient(listOf(Color.Transparent,Color.Transparent,Color(0xFF10212B).copy(alpha=.44f)),Offset(FrontierProjection.W/2,FrontierProjection.H/2),FrontierProjection.W*.68f))
}
private fun DrawScope.tree(p:FrontierPalette,phase:Float,seed:Int){
    oval(Color(0xFF243A34).copy(alpha=.7f),0f,5f,31f,12f)
    drawLine(Color(0xFF594436),Offset(-2f,4f),Offset(2f,-37f),11f,StrokeCap.Round)
    drawLine(Color(0xFF82634E),Offset(-5f,-3f),Offset(0f,-32f),3f,StrokeCap.Round)
    for(i in 0..6){
        val t=i*2.399f+seed;val x=cos(t)*20f;val y=-38f+sin(t)*12f
        drawLine(Color(0xFF594436),Offset(0f,-18f),Offset(x,y),4f,StrokeCap.Round)
        val px=(x/4f).roundToInt()*4f;val py=(y/4f).roundToInt()*4f
        for(row in -3..3){val w=if(abs(row)==3)12f else if(abs(row)==2)24f else 32f
            drawRect(p.dark,Offset(px-w/2,py+row*4f+4f),Size(w,4f))
            drawRect(if(row<0)p.light else p.leaves,Offset(px-w/2,py+row*4f),Size(w,4f))}
        drawRect(p.glow.copy(alpha=.25f),Offset(px-8f,py-8f),Size(8f,4f))
        if(i%2==0)drawCircle(p.glow.copy(alpha=.5f+.25f*sin(phase*6.283f+i)),2.2f,Offset(x,y+6f))
    }
}
private fun DrawScope.ruins(p:FrontierPalette){
    oval(Color(0xFF263A38).copy(alpha=.75f),0f,5f,34f,15f)
    for(i in 0..2){
        val x=-25f+i*21f;val h=26f+i%2*24f
        shape(Color(0xFF626C70),x-7f,3f,x+7f,3f,x+7f,-h,x-7f,-h)
        shape(Color(0xFF414C55),x+7f,3f,x+14f,-3f,x+14f,-h-5f,x+7f,-h)
        shape(Color(0xFFA3A9A0),x-7f,-h,x+7f,-h,x+14f,-h-5f,x,-h-5f)
        for(j in 0 until (h/7f).toInt()){
            val y=-h+j*7f
            drawLine(Color(0xFF424E52),Offset(x-7f,y),Offset(x+7f,y),1.5f)
            drawRect(Color(0xFFB2B8A4).copy(alpha=.65f),Offset(x-5f+(j%2)*4f,y+2f),Size(5f,3f))
        }
        drawLine(p.glow.copy(alpha=.65f),Offset(x,-h+5),Offset(x,-h+16),2f)
    }
    drawArc(Color(0xFF788482),190f,155f,false,Offset(-27f,-65f),Size(64f,62f),style=Stroke(9f))
    for(i in 0..3){val x=-25f+i*16f;shape(p.glow.copy(alpha=.8f),x,4f,x-5f,-12f,x+1f,-23f,x+6f,-8f)}
}
private fun DrawScope.crate(p:FrontierPalette,c:Color,type:Int){
    oval(Color(0xFF26332C).copy(alpha=.7f),0f,6f,26f,11f)
    shape(Color(0xFF303D42),-20f,-18f,15f,-18f,24f,-8f,24f,8f,-20f,8f)
    shape(Color(0xFF858A81),-20f,-18f,-9f,-28f,25f,-28f,15f,-18f)
    shape(Color(0xFF515C61),15f,-18f,25f,-28f,25f,-3f,15f,8f)
    drawRect(Color(0xFF777E77),Offset(-17f,-15f),Size(30f,21f))
    drawRect(Color(0xFF263A3D),Offset(-12f,-12f),Size(20f,15f))
    when(type){
        0->{for(i in 0..3)oval(c,-9f+i*5f,-4f-i%2*3f,4f,3f)}
        1->shape(c,-2f,-14f,-9f,-3f,-2f,-3f,-6f,5f,8f,-7f,0f,-7f,4f,-14f)
        2->{drawRect(c,Offset(-3f,-11f),Size(5f,15f));drawRect(c,Offset(-8f,-6f),Size(15f,5f))}
        else->for(i in 0..2)drawRoundRect(c,Offset(-10f+i*7f,-11f),Size(4f,13f),CornerRadius(1f))
    }
    drawLine(c,Offset(-18f,-18f),Offset(14f,-18f),2f)
}
private fun DrawScope.baseBuilding(p:FrontierPalette){
    oval(Color(0xFF1B3536).copy(alpha=.65f),0f,10f,40f,20f)
    shape(Color(0xFF545F68),-36f,-26f,23f,-26f,34f,-13f,34f,12f,-36f,12f)
    shape(Color(0xFFB5B9AE),-36f,-26f,-17f,-44f,39f,-44f,23f,-26f)
    shape(Color(0xFF3C4D56),23f,-26f,39f,-44f,39f,-5f,34f,12f)
    drawRect(Color(0xFF182B36),Offset(-17f,-20f),Size(27f,31f))
    drawRect(p.glow.copy(alpha=.65f),Offset(-13f,-17f),Size(19f,23f))
    for(i in 0..2)drawLine(Color(0xFFDCF7EB).copy(alpha=.7f),Offset(-11f,-15f+i*7f),Offset(4f,-15f+i*7f),1f)
    drawLine(Color(0xFFBCC9BD),Offset(8f,-42f),Offset(8f,-65f),3f)
    drawOval(Color(0xFFB8C2C1),Offset(-4f,-69f),Size(27f,10f))
    drawCircle(p.glow,3f,Offset(9f,-65f))
    oval(p.glow.copy(alpha=.4f),0f,15f,20f,6f)
}

private fun DrawScope.rockRidge(p:FrontierPalette,seed:Int){
    oval(Color(0xFF243B36).copy(alpha=.72f),0f,8f,46f,17f)
    for(i in 0..2){
        val x=-29f+i*27f;val h=35f+((seed+i*7)%5)*8f
        shape(Color(0xFF3B4949),x-21f,7f,x-14f,-h*.55f,x-2f,-h,x+12f,-h*.76f,x+24f,3f)
        shape(Color(0xFF778178),x-21f,7f,x-14f,-h*.55f,x-2f,-h,x+1f,-h*.45f,x-4f,3f)
        shape(Color(0xFFB0B39A),x-14f,-h*.55f,x-2f,-h,x+12f,-h*.76f,x+1f,-h*.55f)
        for(j in 0..4)drawRect(p.dark.copy(alpha=.65f),Offset(x-14f+j*5f,-j*5f),Size(8f,3f))
    }
}
private fun DrawScope.portal(p:FrontierPalette,phase:Float){
    oval(Color(0xFF202A33),0f,10f,36f,16f)
    oval(p.glow.copy(alpha=.24f),0f,7f,30f,12f)
    drawOval(Color(0xFF46535B),Offset(-32f,-69f),Size(64f,77f),style=Stroke(10f))
    drawOval(Color(0xFF9AAFA9),Offset(-30f,-67f),Size(60f,73f),style=Stroke(3f))
    drawOval(Brush.radialGradient(listOf(p.glow.copy(alpha=.10f),p.glow.copy(alpha=.55f),Color(0xFF234B63)),Offset(0f,-31f),35f),Offset(-25f,-61f),Size(50f,62f))
    for(i in 0..7){
        val angle=(phase+i/8f)*6.283f
        val x=cos(angle)*23f;val y=-31f+sin(angle)*29f
        drawRect(p.glow,Offset(x-2f,y-2f),Size(4f,4f))
    }
}

private fun roundedRoad(points:List<Offset>):Path=Path().apply{
    if(points.isEmpty())return@apply
    moveTo(points.first().x,points.first().y)
    for(i in 1 until points.lastIndex){
        val a=points[i-1];val b=points[i];val c=points[i+1]
        val enter=b+(a-b)*.27f;val exit=b+(c-b)*.27f
        lineTo(enter.x,enter.y)
        quadraticBezierTo(b.x,b.y,exit.x,exit.y)
    }
    lineTo(points.last().x,points.last().y)
}
private fun regionPath(a:AdventureState,live:Boolean):Path=Path().apply{
    for(y in 0 until AdventureRules.MAP_H)for(x in 0 until AdventureRules.MAP_W){
        val q=GridPoint(x,y)
        if(if(live)AdventureRules.inSight(a,q)else AdventureRules.visible(a,q)){
            val v=FrontierProjection.corners(q)
            moveTo(v[0].x,v[0].y);v.drop(1).forEach{lineTo(it.x,it.y)};close()
        }
    }
}
private fun DrawScope.fogOfWar(a:AdventureState){
    val explored=regionPath(a,false)
    val live=regionPath(a,true)
    // Previously explored terrain is subdued. Live objects are not rendered here.
    clipPath(live,ClipOp.Difference){clipPath(explored){
        drawRect(Color(0xFF111E30).copy(alpha=.47f),size=Size(FrontierProjection.W,FrontierProjection.H))
    }}
    // Unknown area is FULLY opaque; no object, road, sprite or glow can show through.
    // Its texture is independent of the underlying map.
    clipPath(explored,ClipOp.Difference){
        drawRect(Color(0xFF0B1420),size=Size(FrontierProjection.W,FrontierProjection.H))
        val rng=Random(7717)
        repeat(420){
            val x=rng.nextFloat()*FrontierProjection.W;val y=rng.nextFloat()*FrontierProjection.H
            oval(Color(0xFF162334).copy(alpha=.4f),x,y,12f+rng.nextFloat()*22f,5f+rng.nextFloat()*12f)
        }
        drawRect(Brush.verticalGradient(listOf(Color(0xFF162130).copy(alpha=.28f),Color.Transparent,Color(0xFF050B13).copy(alpha=.40f)),endY=FrontierProjection.H),size=Size(FrontierProjection.W,FrontierProjection.H))
    }
}

private fun DrawScope.techTower(p:FrontierPalette,phase:Float,seed:Int){
    oval(Color(0xFF0C1927).copy(alpha=.75f),0f,6f,35f,14f)
    shape(Color(0xFF435363),-27f,-8f,20f,-8f,34f,-18f,-13f,-18f)
    shape(Color(0xFF233346),-27f,-8f,20f,-8f,20f,6f,-27f,6f)
    shape(Color(0xFF314B61),-16f,-12f,13f,-12f,13f,-59f,-16f,-59f)
    shape(Color(0xFF152638),13f,-12f,23f,-23f,23f,-68f,13f,-59f)
    shape(Color(0xFF8195A2),-16f,-59f,13f,-59f,23f,-68f,-6f,-68f)
    for(i in 0..5){val y=-53f+i*7f;drawRect(p.glow.copy(alpha=.25f+.3f*(.5f+.5f*sin(phase*6.283f+i+seed))),Offset(-12f,y),Size(20f,3f))}
    drawLine(Color(0xFF859DA8),Offset(0f,-68f),Offset(0f,-86f),3f)
    val pulse=.5f+.5f*sin(phase*6.283f+seed)
    drawCircle(p.glow.copy(alpha=.18f),9f+pulse*6f,Offset(0f,-86f))
    drawCircle(p.glow,3f,Offset(0f,-86f))
    drawLine(p.glow.copy(alpha=.8f),Offset(-25f,-5f),Offset(17f,-5f),2f)
}

@Composable private fun FrontierOverview(a:AdventureState){
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Canvas(Modifier.width(132.dp).height(84.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF101827))){
            val w=size.width/AdventureRules.MAP_W;val h=size.height/AdventureRules.MAP_H
            for(y in 0 until AdventureRules.MAP_H)for(x in 0 until AdventureRules.MAP_W){
                val q=GridPoint(x,y)
                val visible=AdventureRules.visible(a,q);val live=AdventureRules.inSight(a,q)
                val c=when{
                    q==a.pos->Color(0xFFEAFFF8)
                    q==AdventureRules.base->Color(0xFF77CEDE)
                    !visible->Color(0xFF151C2B)
                    live->Color(0xFF4E9293)
                    else->Color(0xFF334C60)
                }
                drawRect(c,Offset(x*w+.5f,y*h+.5f),Size((w-1f).coerceAtLeast(1f),(h-1f).coerceAtLeast(1f)))
            }
            // No resource, enemy, obstacle or portal location is shown here.
            drawCircle(Color(0xFFA5FFE1),w*.65f,Offset((a.pos.x+.5f)*w,(a.pos.y+.5f)*h),style=Stroke(1.5f))
        }
        Column(Modifier.weight(1f)){
            Text("Радар сектору",style=MaterialTheme.typography.titleSmall)
            Text("Світла точка — робот, блакитна — база. Радар не розкриває невідомі об'єкти.",style=MaterialTheme.typography.labelSmall)
        }
    }
}

private fun DrawScope.frontierSite(p:FrontierPalette,kind:Int,phase:Float){
    val col=when(kind){0->Color(0xFF8DEAFF);1->Color(0xFFFFD47A);else->Color(0xFFF38A9C)}
    oval(Color(0xFF111B26).copy(alpha=.8f),0f,9f,36f,14f)
    when(kind){
        0->{
            drawRect(Color(0xFF344657),Offset(-31f,-32f),Size(62f,43f))
            shape(Color(0xFF627786),-37f,-32f,0f,-51f,37f,-32f,0f,-16f)
            drawRect(Color(0xFF182934),Offset(-12f,-21f),Size(24f,32f))
            drawRect(col,Offset(-25f,-23f),Size(8f,9f))
            drawRect(col,Offset(17f,-23f),Size(8f,9f))
            drawLine(col,Offset(0f,-51f),Offset(0f,-67f),3f)
            drawCircle(col.copy(alpha=.55f+.35f*sin(phase*6.283f)),5f,Offset(0f,-67f))
        }
        1->{
            crate(p,col,0)
            val c=Offset(0f,-35f+sin(phase*6.283f)*3f)
            shape(col,c.x,c.y-10f,c.x+8f,c.y,c.x,c.y+10f,c.x-8f,c.y)
        }
        else->{
            shape(Color(0xFF42515C),-39f,6f,-28f,-31f,28f,-31f,39f,6f)
            drawRect(Color(0xFF111E2B),Offset(-21f,-22f),Size(42f,32f))
            drawRect(Color(0xFF58646D),Offset(-18f,-19f),Size(36f,5f))
            for(i in 0..4)drawRect(Color(0xFF34414B),Offset(-18f,-12f+i*4f),Size(36f,2f))
            drawLine(col,Offset(-26f,-26f),Offset(26f,-26f),3f)
            drawCircle(col.copy(alpha=.6f+.3f*sin(phase*6.283f)),4f,Offset(0f,-39f))
        }
    }
}
