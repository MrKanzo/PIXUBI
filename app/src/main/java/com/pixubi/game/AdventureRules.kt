package com.pixubi.game

import kotlin.math.abs
import kotlin.math.ceil

internal data class GridPoint(val x:Int,val y:Int){
    fun distance(other:GridPoint)=abs(x-other.x)+abs(y-other.y)
    fun neighbors()=listOf(GridPoint(x+1,y),GridPoint(x-1,y),GridPoint(x,y+1),GridPoint(x,y-1))
}
internal enum class AdventureTile(val title:String,val symbol:String){
    EMPTY("Стежка",""), WALL("Перешкода","▧"), BASE("База","⌂"), COINS("Кредитний контейнер","🪙"),
    ENERGY("Енергокомірки","⚡"), REPAIR("Ремонтні модулі","🛠"), AMMO("Боєприпаси","▣"), ENEMY("Ворожий бот","!")
}
internal data class ArenaBot(val series:RobotSeries,val tier:RobotTier,val pos:GridPoint,val hp:Int,val maxHp:Int,val attack:Int,val defense:Int,val speed:Int)
internal data class ArenaBattle(val encounter:Int,val pos:GridPoint,val hp:Int,val maxHp:Int,val enemies:List<ArenaBot>,val turn:Int=1,val log:String="Твій хід")
internal data class AdventureState(val stage:Int=1,val pos:GridPoint=GridPoint(0,2),val cleared:Set<Int> = emptySet(),
    val seen:Set<Int> = emptySet(),val steps:Int=0,val battle:ArenaBattle?=null)
internal object AdventureRules{
    const val MAP_W=8;const val MAP_H=6;const val ARENA_W=7;const val ARENA_H=5
    val base=GridPoint(0,2)
    val encounters=listOf(GridPoint(3,1),GridPoint(5,3),GridPoint(7,0),GridPoint(7,5))
    private val walls=setOf(GridPoint(2,0),GridPoint(2,1),GridPoint(4,2),GridPoint(4,3),GridPoint(4,4),GridPoint(6,1),GridPoint(6,4),GridPoint(1,5))
    private val resources=mapOf(GridPoint(1,1) to AdventureTile.COINS,GridPoint(2,3) to AdventureTile.ENERGY,
        GridPoint(3,5) to AdventureTile.REPAIR,GridPoint(5,0) to AdventureTile.AMMO,GridPoint(6,2) to AdventureTile.COINS,
        GridPoint(5,5) to AdventureTile.ENERGY,GridPoint(7,3) to AdventureTile.REPAIR)
    fun id(q:GridPoint)=q.y*MAP_W+q.x
    fun inMap(q:GridPoint)=q.x in 0 until MAP_W&&q.y in 0 until MAP_H
    fun inArena(q:GridPoint)=q.x in 0 until ARENA_W&&q.y in 0 until ARENA_H
    // Odd-r pointy-top hex coordinates. Map movement remains four-directional.
    fun arenaNeighbors(q:GridPoint):List<GridPoint>{
        val shift=if(q.y%2==0)-1 else 0
        return listOf(GridPoint(q.x-1,q.y),GridPoint(q.x+1,q.y),
            GridPoint(q.x+shift,q.y-1),GridPoint(q.x+shift+1,q.y-1),
            GridPoint(q.x+shift,q.y+1),GridPoint(q.x+shift+1,q.y+1))
    }
    fun arenaDistance(a:GridPoint,b:GridPoint):Int{
        val ax=a.x-(a.y-(a.y and 1))/2
        val bx=b.x-(b.y-(b.y and 1))/2
        val dx=ax-bx;val dy=a.y-b.y
        return maxOf(abs(dx),abs(dy),abs(dx+dy))
    }
    fun tile(a:AdventureState,q:GridPoint):AdventureTile=when{
        q==base->AdventureTile.BASE
        q in walls->AdventureTile.WALL
        id(q) in a.cleared->AdventureTile.EMPTY
        q in encounters->AdventureTile.ENEMY
        else->resources[q]?:AdventureTile.EMPTY
    }
    // Keep explored cells navigable, but hide live objects outside the scanner radius.
    fun inSight(a:AdventureState,q:GridPoint)=inMap(q)&&(a.pos.distance(q)<=2||q==base)
    fun visible(a:AdventureState,q:GridPoint)=inMap(q)&&(inSight(a,q)||id(q) in a.seen)
    fun knownTile(a:AdventureState,q:GridPoint):AdventureTile?=if(inSight(a,q))tile(a,q)else null
    private fun reveal(a:AdventureState,q:GridPoint)=a.seen+(0 until MAP_W*MAP_H).filter{q.distance(GridPoint(it%MAP_W,it/MAP_W))<=2}
    fun enemySeries(a:AdventureState,q:GridPoint)=RobotSeries.entries[(encounters.indexOf(q).coerceAtLeast(0)+(a.stage-1)*3)%RobotSeries.entries.size]
    fun complete(a:AdventureState)=encounters.all{id(it) in a.cleared}
    fun canExplore(p:WorldPet)=p.mode==WorldMode.AWAKE&&p.mission==null&&p.integrity>=20&&p.energy>0&&p.adventure.battle==null
    // Route through explored, walkable ground. Hostile cells can only be the destination.
    fun mapRoute(a:AdventureState,to:GridPoint):List<GridPoint>{
        if(!inMap(to)||to==a.pos||!visible(a,to)||tile(a,to)==AdventureTile.WALL)return emptyList()
        val queue=ArrayDeque<GridPoint>()
        val parent=mutableMapOf<GridPoint,GridPoint>()
        queue.add(a.pos);parent[a.pos]=a.pos
        while(queue.isNotEmpty()){
            val q=queue.removeFirst()
            if(q==to){
                val route=mutableListOf<GridPoint>();var at=q
                while(at!=a.pos){route.add(at);at=parent.getValue(at)}
                return route.asReversed()
            }
            q.neighbors().filter{inMap(it)&&it !in parent&&visible(a,it)&&tile(a,it)!=AdventureTile.WALL&&
                (it==to||tile(a,it)!=AdventureTile.ENEMY)}.forEach{parent[it]=q;queue.add(it)}
        }
        return emptyList()
    }
    fun advanceMap(p:WorldPet):WorldPet{
        val a=p.adventure
        if(a.battle!=null||!complete(a)||a.stage>=100||!canExplore(p))return p
        return p.copy(adventure=AdventureState(stage=a.stage+1),message="Відкрито сектор ${a.stage+1}")
    }
    fun move(p:WorldPet,to:GridPoint,now:Long):WorldPet{
        val a=p.adventure
        if(!canExplore(p))return p.copy(message="Потрібна зарядка, ремонт або завершення місії.")
        if(!inMap(to)||a.pos.distance(to)!=1||tile(a,to)==AdventureTile.WALL)return p
        val t=tile(a,to)
        val seen=reveal(a,a.pos)+reveal(a,to)
        if(t==AdventureTile.ENEMY){
            if(p.energy<5)return p.copy(message="Для входу в бій потрібно 5% енергії.")
            val b=begin(p,to)
            return p.copy(energy=p.energy-5,adventure=a.copy(seen=seen,battle=b),message="Зустріч із ворожими ботами",last=now)
        }
        val steps=a.steps+1
        var q=p.copy(energy=(p.energy-if(steps%4==0)1 else 0).coerceAtLeast(0),
            adventure=a.copy(pos=to,seen=seen,steps=steps),message="Сектор ${a.stage} · дослідження")
        if(t in listOf(AdventureTile.EMPTY,AdventureTile.BASE))return q
        q=q.copy(adventure=q.adventure.copy(cleared=a.cleared+id(to)))
        q=when(t){
            AdventureTile.COINS->q.copy(coins=(q.coins+25+a.stage*3).coerceAtMost(999999),message="Знайдено ${25+a.stage*3} монет")
            AdventureTile.ENERGY->q.copy(cells=(q.cells+2).coerceAtMost(30),message="Знайдено 2 енергокомірки")
            AdventureTile.REPAIR->q.copy(repair=(q.repair+2).coerceAtMost(30),message="Знайдено 2 ремонтні набори")
            AdventureTile.AMMO->q.copy(supplies=q.supplies+(WorldSupply.PULSE to (q.stock(WorldSupply.PULSE)+6).coerceAtMost(99))+
                (WorldSupply.ION to (q.stock(WorldSupply.ION)+2).coerceAtMost(99))+(WorldSupply.ARC to (q.stock(WorldSupply.ARC)+2).coerceAtMost(99)),message="Знайдено боєприпаси")
            else->q
        }
        return WorldRules.gain(q,RobotSkill.EXPLORATION,15,5,0)
    }
    private fun begin(p:WorldPet,at:GridPoint):ArenaBattle{
        val a=p.adventure
        val encounter=encounters.indexOf(at).coerceAtLeast(0)
        val tier=RobotTier.entries[(p.tier.ordinal-(if(encounter<2)1 else 0)).coerceAtLeast(0)]
        val count=if(encounter>=2)2 else 1
        val bots=(0 until count).map{i->
            val series=RobotSeries.entries[(enemySeries(a,at).ordinal+i)%RobotSeries.entries.size]
            val stats=RobotBalance.base(tier)+WorldRules.innate(series)
            val scale=1f+(a.stage-1).coerceAtMost(12)*.045f
            val hp=(stats.durability*(if(count==2).36f else .55f)*scale).toInt().coerceAtLeast(25)
            ArenaBot(series,tier,GridPoint(ARENA_W-1,if(count==1)2 else 1+i*2),hp,hp,
                (stats.attack*.60f+4+a.stage.coerceAtMost(12)).toInt(),(stats.defense*.55f).toInt(),stats.speed)
        }
        val max=p.stats.durability.coerceAtLeast(1)
        return ArenaBattle(id(at),GridPoint(0,2),(max*p.integrity/100).coerceAtLeast(1),max,bots)
    }
    fun movement(p:WorldPet)=(1+p.stats.speed/45).coerceIn(2,4)
    fun path(from:GridPoint,to:GridPoint,blocked:Set<GridPoint>):Int?{
        if(!inArena(to)||to in blocked)return null
        val queue=ArrayDeque<Pair<GridPoint,Int>>()
        val visited=mutableSetOf(from);queue.add(from to 0)
        while(queue.isNotEmpty()){
            val (q,d)=queue.removeFirst()
            if(q==to)return d
            arenaNeighbors(q).filter{inArena(it)&&it !in blocked&&visited.add(it)}.forEach{queue.add(it to d+1)}
        }
        return null
    }
    fun reachable(p:WorldPet,b:ArenaBattle,q:GridPoint)=q!=b.pos&&(path(b.pos,q,b.enemies.filter{it.hp>0}.map{it.pos}.toSet())?:999)<=movement(p)
    fun weapons(p:WorldPet)=p.equipment.items.values.filter{WorldGearCatalog.category(it)==WorldCategory.WEAPONS}.sortedBy{it.ordinal}
    fun range(g:RobotGear?)=when(g){
        RobotGear.BLASTER_L,RobotGear.BLASTER_R->3
        RobotGear.RIFLE_L,RobotGear.RIFLE_R->4
        RobotGear.CANNON_L,RobotGear.CANNON_R->5
        RobotGear.ARC_L,RobotGear.ARC_R->3
        else->1
    }
    fun attackCost(g:RobotGear?)=if(g==null)null else WorldGearCatalog.ammo(g)?.first
    fun canHit(p:WorldPet,b:ArenaBattle,enemy:ArenaBot,g:RobotGear?):Boolean{
        if(enemy.hp<=0||arenaDistance(b.pos,enemy.pos)>range(g))return false
        if(g!=null&&g !in weapons(p))return false
        return attackCost(g)?.let{p.stock(it)>0}?:true
    }
    private fun damage(attack:Int,defense:Int)=(attack+5-defense/3).coerceAtLeast(3)
    fun moveArena(p:WorldPet,to:GridPoint,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        if(!reachable(p,b,to))return p
        return enemiesTurn(p,b.copy(pos=to),false,"Переміщення",now)
    }
    fun strike(p:WorldPet,index:Int,gear:RobotGear?,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        val e=b.enemies.getOrNull(index)?:return p
        if(!canHit(p,b,e,gear))return p.copy(message="Ціль поза досяжністю або немає зарядів.")
        val cost=attackCost(gear)
        val q=if(cost==null)p else p.copy(supplies=p.supplies+(cost to p.stock(cost)-1))
        val base=(p.stats.attack-weapons(p).sumOf{RobotBalance.spec(it).delta.attack}).coerceAtLeast(1)
        val hit=damage(base+(gear?.let{RobotBalance.spec(it).delta.attack}?:0),e.defense)
        val enemies=b.enemies.mapIndexed{i,bot->if(i==index)bot.copy(hp=(bot.hp-hit).coerceAtLeast(0))else bot}
        val next=b.copy(enemies=enemies)
        if(enemies.none{it.hp>0})return finish(q,next,true,now)
        return enemiesTurn(q,next,false,"Удар по ${e.series.title}: −$hit HP",now)
    }
    fun guard(p:WorldPet,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        return enemiesTurn(p,b,true,"Захист активовано",now)
    }
    fun repair(p:WorldPet,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        if(p.repair==0||b.hp>=b.maxHp)return p
        val amount=(b.maxHp*(40+WorldGearCatalog.repairBonus(p.equipment))/100).coerceAtLeast(1)
        return enemiesTurn(p.copy(repair=p.repair-1),b.copy(hp=(b.hp+amount).coerceAtMost(b.maxHp)),false,"Корпус відновлено",now)
    }
    fun retreat(p:WorldPet,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        return finish(p,b,false,now,true)
    }
    private fun enemiesTurn(p:WorldPet,b:ArenaBattle,guard:Boolean,log:String,now:Long):WorldPet{
        var hp=b.hp;val bots=b.enemies.toMutableList();val logs=mutableListOf(log)
        for(i in bots.indices){
            val e=bots[i];if(e.hp<=0||hp<=0)continue
            if(arenaDistance(e.pos,b.pos)==1){
                val hit=damage(e.attack,p.stats.defense).let{if(guard)(it+1)/2 else it}
                hp=(hp-hit).coerceAtLeast(0);logs+="${e.series.title}: −$hit HP"
            }else{
                val blocked=bots.filterIndexed{j,bot->j!=i&&bot.hp>0}.map{it.pos}.toSet()+b.pos
                val reach=(1+e.speed/55).coerceIn(1,3)
                val options=(0 until ARENA_W*ARENA_H).map{GridPoint(it%ARENA_W,it/ARENA_W)}
                    .filter{it !in blocked&&(path(e.pos,it,blocked)?:999)<=reach}
                val next=options.minWithOrNull(compareBy<GridPoint>{arenaDistance(it,b.pos)}.thenBy{it.y}.thenBy{it.x})?:e.pos
                bots[i]=e.copy(pos=next)
                if(next!=e.pos)logs+="${e.series.title} наближається"
            }
        }
        val next=b.copy(hp=hp,enemies=bots,turn=b.turn+1,log=logs.joinToString(" · "))
        if(hp<=0)return finish(p,next,false,now)
        return p.copy(adventure=p.adventure.copy(battle=next),message=next.log)
    }
    private fun finish(p:WorldPet,b:ArenaBattle,win:Boolean,now:Long,retreat:Boolean=false):WorldPet{
        val a=p.adventure
        val integrity=ceil(b.hp*100.0/b.maxHp).toInt().coerceIn(1,100)
        val cleared=if(win)a.cleared+b.encounter else a.cleared
        val pos=if(win)GridPoint(b.encounter%MAP_W,b.encounter/MAP_W) else if(retreat)a.pos else base
        val xp=if(win)70+b.enemies.size*30+a.stage*5 else if(retreat)0 else 10
        val coins=if(win)25+b.enemies.size*15+a.stage*4 else 0
        val updated=a.copy(battle=null,cleared=cleared,pos=pos)
        val q=p.copy(adventure=updated,integrity=integrity,wins=p.wins+if(win)1 else 0,
            expeditions=p.expeditions+if(win&&complete(updated))1 else 0,last=now,
            message=if(win)"Перемога! +$xp XP · +$coins монет" else if(retreat)"Відступ. Ворог залишився на карті." else "Поразка. Повернення на базу — потрібен ремонт.")
        return WorldRules.gain(q,RobotSkill.COMBAT,xp,if(win)30 else 0,coins)
    }
}
internal object AdventureCodec{
    private fun point(p:GridPoint)="${p.x},${p.y}"
    private fun parse(s:String):GridPoint{val v=s.split(",");return GridPoint(v[0].toInt(),v[1].toInt())}
    fun encode(a:AdventureState):String{
        val b=a.battle
        val battle=if(b==null)"" else listOf(b.encounter.toString(),point(b.pos),b.hp.toString(),b.maxHp.toString(),b.turn.toString(),
            b.enemies.joinToString(";"){e->listOf(e.series.name,e.tier.name,e.pos.x,e.pos.y,e.hp,e.maxHp,e.attack,e.defense,e.speed).joinToString(",")}).joinToString("~")
        return listOf(a.stage.toString(),point(a.pos),a.cleared.sorted().joinToString(","),a.seen.sorted().joinToString(","),a.steps.toString(),battle).joinToString("|")
    }
    fun decode(s:String):AdventureState=runCatching{
        val v=s.split("|")
        fun ids(value:String)=value.split(",").mapNotNull{it.toIntOrNull()}.filter{it in 0 until AdventureRules.MAP_W*AdventureRules.MAP_H}.toSet()
        val pos=parse(v[1]);require(AdventureRules.inMap(pos))
        val b=v.getOrElse(5){""}.takeIf{it.isNotBlank()}?.let{value->
            val z=value.split("~");val p=parse(z[1]);val max=z[3].toInt();require(AdventureRules.inArena(p)&&max>0)
            val bots=z[5].split(";").map{line->val e=line.split(",");val pt=GridPoint(e[2].toInt(),e[3].toInt());val m=e[5].toInt()
                require(AdventureRules.inArena(pt)&&m>0);ArenaBot(RobotSeries.valueOf(e[0]),RobotTier.valueOf(e[1]),pt,e[4].toInt().coerceIn(0,m),m,e[6].toInt(),e[7].toInt(),e[8].toInt())}
            require(bots.isNotEmpty()&&bots.any{it.hp>0}&&bots.size<=2)
            val encounter=z[0].toInt();require(AdventureRules.encounters.any{AdventureRules.id(it)==encounter})
            ArenaBattle(encounter,p,z[2].toInt().coerceIn(1,max),max,bots,z[4].toInt().coerceAtLeast(1),"Бій відновлено. Твій хід.")
        }
        AdventureState(v[0].toInt().coerceIn(1,100),pos,ids(v[2]),ids(v[3]),v[4].toInt().coerceAtLeast(0),b)
    }.getOrElse{AdventureState()}
}
