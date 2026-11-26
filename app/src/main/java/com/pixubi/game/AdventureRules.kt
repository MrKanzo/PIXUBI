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
internal data class ArenaBattle(val encounter:Int,val pos:GridPoint,val hp:Int,val maxHp:Int,val enemies:List<ArenaBot>,val turn:Int=1,val log:String="Твій хід",val allies:List<ArenaAlly> = emptyList(),val acted:Set<Int> = emptySet(),val guards:Set<Int> = emptySet(),val enemyPending:Boolean=false,val enemyCursor:Int=0)
internal data class AdventureState(val stage:Int=1,val pos:GridPoint=GridPoint(0,2),val cleared:Set<Int> = emptySet(),
    val seen:Set<Int> = emptySet(),val steps:Int=0,val battle:ArenaBattle?=null,val credited:Boolean=false,val squad:Map<SquadKind,Int> = emptyMap(),val barracks:Int=1,val squadRanks:Map<SquadKind,Int> = emptyMap(),val pastCampaignWins:Int=0,val licensed:Set<SquadKind> = emptySet())
internal object AdventureRules{
    const val MAP_W=16;const val MAP_H=10;const val ARENA_W=9;const val ARENA_H=5
    val base=GridPoint(0,2)
    val legacyEncounters=listOf(GridPoint(3,1),GridPoint(5,3),GridPoint(7,0),GridPoint(7,5))
    private val legacyWalls=setOf(GridPoint(2,0),GridPoint(2,1),GridPoint(4,2),GridPoint(4,3),GridPoint(4,4),GridPoint(6,1),GridPoint(6,4),GridPoint(1,5))
    private val legacyResources=mapOf(GridPoint(1,1) to AdventureTile.COINS,GridPoint(2,3) to AdventureTile.ENERGY,
        GridPoint(3,5) to AdventureTile.REPAIR,GridPoint(5,0) to AdventureTile.AMMO,GridPoint(6,2) to AdventureTile.COINS,
        GridPoint(5,5) to AdventureTile.ENERGY,GridPoint(7,3) to AdventureTile.REPAIR)
    // The original 8x6 corner is preserved. The rest is a deterministic frontier
    // with open main roads; each sector has a different arrangement of objects.
    internal data class SectorLayout(val encounters:List<GridPoint>,val walls:Set<GridPoint>,val resources:Map<GridPoint,AdventureTile>)
    private val sectors=java.util.concurrent.ConcurrentHashMap<Int,SectorLayout>()
    fun mainRoad(q:GridPoint):Boolean=(q.x>=8&&(q.y in listOf(0,2,5,6,9)||q.x in listOf(8,12,15)))||
        (q.y>=6&&(q.y in listOf(6,9)||q.x in listOf(0,3,5,7,8,12,15)))
    fun layout(stage:Int):SectorLayout=sectors.computeIfAbsent(stage){n->
        val rng=kotlin.random.Random(n*7907+31)
        val extension=(0 until MAP_W*MAP_H).map{GridPoint(it%MAP_W,it/MAP_W)}.filter{it.x>=8||it.y>=6}
        val river=setOf(GridPoint(4,7),GridPoint(4,8))
        val candidates=extension.filter{!mainRoad(it)&&it !in river&&it.neighbors().any(::mainRoad)}.shuffled(rng)
        val newEnemies=candidates.take(6)
        val newResources=candidates.drop(6).take(18)
        val terrain=extension.filter{!mainRoad(it)&&it !in newEnemies&&it !in newResources&&it !in river}.shuffled(rng).take(12)
        val kinds=listOf(AdventureTile.COINS,AdventureTile.ENERGY,AdventureTile.REPAIR,AdventureTile.AMMO)
        SectorLayout(legacyEncounters+newEnemies,legacyWalls+river+terrain,
            legacyResources+newResources.mapIndexed{i,q->q to kinds[(i+n)%kinds.size]}.toMap())
    }
    fun encounters(a:AdventureState)=layout(a.stage).encounters
    fun id(q:GridPoint)=q.y*MAP_W+q.x
    fun inMap(q:GridPoint)=q.x in 0 until MAP_W&&q.y in 0 until MAP_H
    // Flat-top columns: 5–3–5–3–5–3–5–3–5. Odd columns are inset.
    fun inArena(q:GridPoint)=q.x in 0 until ARENA_W&&q.y in 0 until ARENA_H&&(q.x%2==0||q.y in 1..3)
    val arenaCells:List<GridPoint> = (0 until ARENA_W).flatMap{x->(0 until ARENA_H).map{y->GridPoint(x,y)}.filter(::inArena)}
    fun arenaNeighbors(q:GridPoint):List<GridPoint>{
        val shift=if(q.x%2==0)-1 else 0
        return listOf(GridPoint(q.x,q.y-1),GridPoint(q.x,q.y+1),
            GridPoint(q.x-1,q.y+shift),GridPoint(q.x-1,q.y+shift+1),
            GridPoint(q.x+1,q.y+shift),GridPoint(q.x+1,q.y+shift+1))
    }
    // Distance for attacks is the geometric hex distance; walking uses the actual walkable board.
    fun arenaDistance(a:GridPoint,b:GridPoint):Int{
        val ay=a.y-(a.x-(a.x and 1))/2
        val by=b.y-(b.x-(b.x and 1))/2
        val dx=a.x-b.x;val dy=ay-by
        return maxOf(abs(dx),abs(dy),abs(dx+dy))
    }
    fun tile(a:AdventureState,q:GridPoint):AdventureTile=when{
        q==base->AdventureTile.BASE
        q in layout(a.stage).walls->AdventureTile.WALL
        id(q) in a.cleared->AdventureTile.EMPTY
        q in encounters(a)->AdventureTile.ENEMY
        else->layout(a.stage).resources[q]?:AdventureTile.EMPTY
    }
    // Keep explored cells navigable, but hide live objects outside the scanner radius.
    fun inSight(a:AdventureState,q:GridPoint)=inMap(q)&&(a.pos.distance(q)<=2||q==base)
    fun visible(a:AdventureState,q:GridPoint)=inMap(q)&&(inSight(a,q)||id(q) in a.seen)
    fun knownTile(a:AdventureState,q:GridPoint):AdventureTile?=if(inSight(a,q))tile(a,q)else null
    private fun reveal(a:AdventureState,q:GridPoint)=a.seen+(0 until MAP_W*MAP_H).filter{q.distance(GridPoint(it%MAP_W,it/MAP_W))<=2}
    fun enemySeries(a:AdventureState,q:GridPoint)=RobotSeries.entries[(encounters(a).indexOf(q).coerceAtLeast(0)+(a.stage-1)*3)%RobotSeries.entries.size]
    fun complete(a:AdventureState)=encounters(a).all{id(it) in a.cleared}
    fun canAdvance(a:AdventureState)=complete(a)||a.credited
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
        if(a.battle!=null||!canAdvance(a)||a.stage>=100||!canExplore(p))return p
        val next=AdventureState(stage=a.stage+1,squad=a.squad,barracks=a.barracks,squadRanks=a.squadRanks,pastCampaignWins=SquadResearch.victories(a),licensed=a.licensed+a.squad.keys+SquadKind.entries.filter{SquadResearch.unlocked(a,it)})
        val newly=SquadKind.entries.filter{!SquadResearch.unlocked(a,it)&&SquadResearch.unlocked(next,it)}
        val notice=if(newly.isEmpty())"" else " · 🔓 Відкрито: "+newly.joinToString(", "){it.title}
        return p.copy(adventure=next.copy(licensed=next.licensed+newly),message="Відкрито сектор ${a.stage+1}"+notice)
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
        val encounter=encounters(a).indexOf(at).coerceAtLeast(0)
        val tier=RobotTier.entries[(p.tier.ordinal-(if(encounter<2)1 else 0)).coerceAtLeast(0)]
        val count=if(encounter>=2)2 else 1
        val bots=(0 until count).map{i->
            val series=RobotSeries.entries[(enemySeries(a,at).ordinal+i)%RobotSeries.entries.size]
            val stats=RobotBalance.base(tier)+WorldRules.innate(series)
            val extra=a.squad.keys.sumOf{SquadRules.stackCount(a,it)}
            val scale=(1f+(a.stage-1).coerceAtMost(12)*.045f)*(1f+extra*.18f)
            val hp=(stats.durability*(if(count==2).36f else .55f)*scale).toInt().coerceAtLeast(25)
            ArenaBot(series,tier,GridPoint(ARENA_W-1,if(count==1)2 else 1+i*2),hp,hp,
                (stats.attack*.60f+4+a.stage.coerceAtMost(12)).toInt(),(stats.defense*.55f).toInt(),stats.speed)
        }
        val max=p.stats.durability.coerceAtLeast(1)
        return ArenaBattle(id(at),GridPoint(0,2),(max*p.integrity/100).coerceAtLeast(1),max,bots,allies=SquadRules.deploy(a.squad,a.squadRanks,SquadRules.slots(a)))
    }
    fun movement(p:WorldPet)=(1+p.stats.speed/45).coerceIn(2,4)
    fun path(from:GridPoint,to:GridPoint,blocked:Set<GridPoint>):Int?{
        if(!inArena(from)||!inArena(to)||to in blocked)return null
        val queue=ArrayDeque<Pair<GridPoint,Int>>()
        val visited=mutableSetOf(from);queue.add(from to 0)
        while(queue.isNotEmpty()){
            val (q,d)=queue.removeFirst()
            if(q==to)return d
            arenaNeighbors(q).filter{inArena(it)&&it !in blocked&&visited.add(it)}.forEach{queue.add(it to d+1)}
        }
        return null
    }
    fun reachable(p:WorldPet,b:ArenaBattle,q:GridPoint)=SquadRules.reachable(p,b,-1,q)
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
        if(!SquadRules.ready(b,-1)||enemy.hp<=0||arenaDistance(b.pos,enemy.pos)>range(g))return false
        if(g!=null&&g !in weapons(p))return false
        return attackCost(g)?.let{p.stock(it)>0}?:true
    }
    private fun damage(attack:Int,defense:Int)=(attack+5-defense/3).coerceAtLeast(3)
    fun moveArena(p:WorldPet,to:GridPoint,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        if(!reachable(p,b,to))return p
        return afterAction(p,b.copy(pos=to),-1,false,"Переміщення командира",now)
    }
    fun previewDamage(p:WorldPet,b:ArenaBattle,actor:Int,index:Int,gear:RobotGear?):Int?{
        val e=b.enemies.getOrNull(index)?:return null
        if(actor==-1){
            if(!canHit(p,b,e,gear))return null
            val base=(p.stats.attack-weapons(p).sumOf{RobotBalance.spec(it).delta.attack}).coerceAtLeast(1)
            return damage(base+(gear?.let{RobotBalance.spec(it).delta.attack}?:0),e.defense)
        }
        val ally=b.allies.getOrNull(actor)?:return null
        if(!SquadRules.canHit(b,actor,e))return null
        return damage(ally.attack+p.level/3+SquadTactics.attackBonus(b,ally.kind),e.defense)
    }
    fun strike(p:WorldPet,index:Int,gear:RobotGear?,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        val e=b.enemies.getOrNull(index)?:return p
        if(!canHit(p,b,e,gear))return p.copy(message="Ціль поза досяжністю або немає зарядів.")
        val cost=attackCost(gear)
        val q=if(cost==null)p else p.copy(supplies=p.supplies+(cost to p.stock(cost)-1))
        val hit=previewDamage(p,b,-1,index,gear)?:return p
        val enemies=b.enemies.mapIndexed{i,bot->if(i==index)bot.copy(hp=(bot.hp-hit).coerceAtLeast(0))else bot}
        val next=b.copy(enemies=enemies)
        if(enemies.none{it.hp>0})return finish(q,next,true,now)
        return afterAction(q,next,-1,false,"Удар по ${e.series.title}: −$hit HP",now)
    }
    fun guard(p:WorldPet,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        if(!SquadRules.ready(b,-1))return p
        return afterAction(p,b,-1,true,"Захист активовано",now)
    }
    fun repair(p:WorldPet,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        if(!SquadRules.ready(b,-1)||p.repair==0||b.hp>=b.maxHp)return p
        val amount=(b.maxHp*(40+WorldGearCatalog.repairBonus(p.equipment))/100).coerceAtLeast(1)
        return afterAction(p.copy(repair=p.repair-1),b.copy(hp=(b.hp+amount).coerceAtMost(b.maxHp)),-1,false,"Корпус відновлено",now)
    }
    fun retreat(p:WorldPet,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        if(b.enemyPending)return p
        return finish(p,b,false,now,true)
    }
    fun allyMove(p:WorldPet,index:Int,to:GridPoint,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        val ally=b.allies.getOrNull(index)?:return p
        if(!SquadRules.reachable(p,b,index,to))return p
        val next=b.copy(allies=b.allies.mapIndexed{i,u->if(i==index)u.copy(pos=to)else u})
        return afterAction(p,next,index,false,"${ally.kind.title}: переміщення",now)
    }
    fun allyStrike(p:WorldPet,index:Int,target:Int,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        val ally=b.allies.getOrNull(index)?:return p
        val enemy=b.enemies.getOrNull(target)?:return p
        if(!SquadRules.canHit(b,index,enemy))return p
        val hit=previewDamage(p,b,index,target,null)?:return p
        val next=b.copy(allies=b.allies.mapIndexed{i,u->if(i==index&&u.kind.range>1)u.copy(charges=u.charges-1)else u},
            enemies=b.enemies.mapIndexed{i,e->if(i==target)e.copy(hp=(e.hp-hit).coerceAtLeast(0))else e})
        if(next.enemies.none{it.hp>0})return finish(p,next,true,now)
        return afterAction(p,next,index,false,"${ally.kind.title}: удар −$hit HP",now)
    }
    fun allyGuard(p:WorldPet,index:Int,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        if(index<0||!SquadRules.ready(b,index))return p
        return afterAction(p,b,index,true,"${b.allies[index].kind.title}: захист",now)
    }
    fun allyRepair(p:WorldPet,index:Int,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        val target=SquadRules.repairTarget(b,index)?:return p
        val amount=(20+p.level/2+5*(b.allies[index].rank-1))*b.allies[index].count+SquadTactics.repairBonus(b)
        val next=b.copy(hp=if(target==-1)(b.hp+amount).coerceAtMost(b.maxHp)else b.hp,
            allies=b.allies.mapIndexed{i,a->a.copy(hp=if(i==target)(a.hp+amount).coerceAtMost(a.maxHp)else a.hp,charges=if(i==index)a.charges-1 else a.charges)})
        return afterAction(p,next,index,false,"Ремонтний дрон: +HP",now)
    }
    private fun afterAction(p:WorldPet,b:ArenaBattle,actor:Int,guard:Boolean,log:String,now:Long):WorldPet{
        val next=b.copy(acted=b.acted+actor,guards=if(guard)b.guards+actor else b.guards-actor,log=log,enemyPending=true)
        return p.copy(adventure=p.adventure.copy(battle=next),message=log)
    }
    internal data class EnemyAction(val index:Int,val from:GridPoint,val to:GridPoint,val target:Int?)
    fun enemyPlan(b:ArenaBattle):EnemyAction?{
        if(!b.enemyPending)return null
        val i=(b.enemies.indices).map{(b.enemyCursor+it)%b.enemies.size}.firstOrNull{b.enemies[it].hp>0}?:return null
        val e=b.enemies[i]
        val targets=buildList{add(-1 to b.pos);b.allies.forEachIndexed{j,u->if(u.hp>0)add(j to u.pos)}}
        val (actor,position)=targets.minWithOrNull(compareBy<Pair<Int,GridPoint>>{arenaDistance(e.pos,it.second)}.thenBy{if(it.first>=0&&b.allies[it.first].kind==SquadKind.GUARD)0 else 1}.thenBy{it.first})!!
        if(arenaDistance(e.pos,position)==1)return EnemyAction(i,e.pos,position,actor)
        val blocked=b.enemies.filterIndexed{j,bot->j!=i&&bot.hp>0}.map{it.pos}.toSet()+targets.map{it.second}
        val reach=(1+e.speed/55).coerceIn(1,3)
        val next=arenaCells
            .filter{it !in blocked&&(path(e.pos,it,blocked)?:999)<=reach}
            .minWithOrNull(compareBy<GridPoint>{arenaDistance(it,position)}.thenBy{it.y}.thenBy{it.x})?:e.pos
        return EnemyAction(i,e.pos,next,null)
    }
    fun nextEnemy(b:ArenaBattle):ArenaBot?=(b.enemies.indices).map{(b.enemyCursor+it)%b.enemies.size}.firstOrNull{b.enemies[it].hp>0}?.let{b.enemies[it]}
    fun enemyTurn(p:WorldPet,now:Long):WorldPet{
        val b=p.adventure.battle?:return p
        val plan=enemyPlan(b)?:return p
        val e=b.enemies[plan.index];val actor=plan.target
        var hp=b.hp;var allies=b.allies;var enemies=b.enemies;var guards=b.guards
        val log=if(actor!=null){
            val def=(if(actor==-1)p.stats.defense else allies[actor].defense)+SquadTactics.defenseBonus(b,actor)
            val hit=damage(e.attack,def).let{if(actor in guards)(it+1)/2 else it}
            if(actor==-1)hp=(hp-hit).coerceAtLeast(0)else allies=allies.mapIndexed{i,u->if(i==actor)u.copy(hp=(u.hp-hit).coerceAtLeast(0))else u}
            guards=guards-actor
            "${e.series.title}: −$hit HP · "+(if(actor==-1)"командир" else allies[actor].kind.title)
        }else{
            enemies=enemies.mapIndexed{i,u->if(i==plan.index)u.copy(pos=plan.to)else u}
            if(plan.from==plan.to)"${e.series.title}: очікує" else "${e.series.title}: переміщення"
        }
        var next=b.copy(hp=hp,enemies=enemies,allies=allies,guards=guards,enemyPending=false,enemyCursor=(plan.index+1)%enemies.size,log=log)
        if(hp<=0)return finish(p,next,false,now)
        if(SquadRules.available(next).isEmpty())next=next.copy(acted=emptySet(),turn=b.turn+1)
        return p.copy(adventure=p.adventure.copy(battle=next),message=log)
    }
    private fun finish(p:WorldPet,b:ArenaBattle,win:Boolean,now:Long,retreat:Boolean=false):WorldPet{
        val a=p.adventure
        val integrity=ceil(b.hp*100.0/b.maxHp).toInt().coerceIn(1,100)
        val cleared=if(win)a.cleared+b.encounter else a.cleared
        val pos=if(win)GridPoint(b.encounter%MAP_W,b.encounter/MAP_W) else if(retreat)a.pos else base
        val xp=if(win)70+b.enemies.size*30+a.stage*5 else if(retreat)0 else 10
        val coins=if(win)25+b.enemies.size*15+a.stage*4 else 0
        val survivors=if(b.allies.isEmpty())a.squad else b.allies.filter{it.hp>0}.associate{it.kind to it.hp}
        val updated=a.copy(battle=null,cleared=cleared,pos=pos,squad=survivors,squadRanks=a.squadRanks.filterKeys{it in survivors})
        val earnsCredit=win&&!a.credited&&complete(updated)
        val newTech=SquadKind.entries.filter{!SquadResearch.unlocked(a,it)&&SquadResearch.unlocked(updated,it)}
        val techMessage=if(newTech.isEmpty())"" else " · 🔓 Відкрито: "+newTech.joinToString(", "){it.title}
        val q=p.copy(adventure=updated.copy(credited=a.credited||earnsCredit,licensed=updated.licensed+newTech),integrity=integrity,wins=p.wins+if(win)1 else 0,
            expeditions=p.expeditions+if(earnsCredit)1 else 0,last=now,
            message=if(win)"Перемога! +$xp XP · +$coins монет"+techMessage else if(retreat)"Відступ. Ворог залишився на карті." else "Поразка. Повернення на базу — потрібен ремонт.")
        return WorldRules.gain(q,RobotSkill.COMBAT,xp,if(win)30 else 0,coins)
    }
}
internal object AdventureCodec{
    private fun point(p:GridPoint)="${p.x},${p.y}"
    private fun parse(s:String):GridPoint{val v=s.split(",");return GridPoint(v[0].toInt(),v[1].toInt())}
    fun encode(a:AdventureState):String{
        val b=a.battle
        val battle=if(b==null)"" else listOf(b.encounter.toString(),point(b.pos),b.hp.toString(),b.maxHp.toString(),b.turn.toString(),
            b.enemies.joinToString(";"){e->listOf(e.series.name,e.tier.name,e.pos.x,e.pos.y,e.hp,e.maxHp,e.attack,e.defense,e.speed).joinToString(",")},
            b.allies.joinToString(";"){u->listOf(u.kind.name,u.pos.x,u.pos.y,u.hp,u.charges,u.rank).joinToString(",")},b.acted.sorted().joinToString(","),b.guards.sorted().joinToString(","),if(b.enemyPending)"1" else "0",b.enemyCursor.toString()).joinToString("~")
        return listOf(a.stage.toString(),point(a.pos),a.cleared.sorted().joinToString(","),a.seen.sorted().joinToString(","),a.steps.toString(),battle,"16",if(a.credited)"1" else "0",a.squad.entries.sortedBy{it.key.ordinal}.joinToString(";"){(k,h)->"${k.name},$h"},a.barracks.toString(),a.squadRanks.entries.sortedBy{it.key.ordinal}.joinToString(";"){(k,rank)->"${k.name},$rank"},a.pastCampaignWins.toString(),a.licensed.sortedBy{it.ordinal}.joinToString(","){it.name},"arena9").joinToString("|")
    }
    fun decode(s:String):AdventureState=runCatching{
        val v=s.split("|")
        val stage=v[0].toInt().coerceIn(1,100)
        val old=v.getOrNull(6)!="16"
        fun migrateId(id:Int):Int?=if(old){
            if(id !in 0 until 48)null else AdventureRules.id(GridPoint(id%8,id/8))
        }else id.takeIf{it in 0 until AdventureRules.MAP_W*AdventureRules.MAP_H}
        fun ids(value:String)=value.split(",").mapNotNull{it.toIntOrNull()?.let(::migrateId)}.toSet()
        val cleared=ids(v[2])
        val credited=v.getOrNull(7)=="1"||(old&&AdventureRules.legacyEncounters.all{AdventureRules.id(it) in cleared})
        val pos=parse(v[1]);require(AdventureRules.inMap(pos))
        val barracks=(v.getOrNull(9)?.toIntOrNull()?:1).coerceIn(1,3)
        val ranks=v.getOrElse(10){""}.split(";").mapNotNull{line->val k=line.split(",");val kind=SquadKind.entries.firstOrNull{it.name==k.firstOrNull()};val rank=k.getOrNull(1)?.toIntOrNull();if(kind!=null&&rank!=null)kind to rank.coerceIn(1,barracks) else null}.toMap()
        val squad=v.getOrElse(8){""}.split(";").mapNotNull{line->val k=line.split(",");val kind=SquadKind.entries.firstOrNull{it.name==k.firstOrNull()};val hp=k.getOrNull(1)?.toIntOrNull();if(kind!=null&&hp!=null&&hp>0)kind to hp.coerceAtMost(SquadRules.unitHp(kind,ranks[kind]?:1)*SquadRules.MAX_STACK) else null}.take(if(barracks>=2)4 else 3).toMap()
        val b=v.getOrElse(5){""}.takeIf{it.isNotBlank()}?.let{value->
            val z=value.split("~")
            val oldArena=v.getOrNull(13)!="arena9"
            val occupied=mutableSetOf<GridPoint>()
            fun battlePoint(raw:GridPoint,alive:Boolean=true):GridPoint{
                if(!oldArena){require(AdventureRules.inArena(raw));return raw}
                require(raw.x in 0..6&&raw.y in 0..4)
                val desiredX=raw.x*8f/6f
                val desiredY=raw.y.toFloat()
                val pt=AdventureRules.arenaCells.filter{!alive||it !in occupied}.minByOrNull{
                    val dx=it.x-desiredX;val dy=it.y+(if(it.x%2==1).5f else 0f)-desiredY
                    dx*dx+dy*dy
                }?:error("Arena full")
                if(alive)occupied.add(pt)
                return pt
            }
            val p=battlePoint(parse(z[1]));val max=z[3].toInt();require(max>0)
            val bots=z[5].split(";").map{line->val e=line.split(",");val pt=battlePoint(GridPoint(e[2].toInt(),e[3].toInt()),e[4].toInt()>0);val m=e[5].toInt()
                require(AdventureRules.inArena(pt)&&m>0);ArenaBot(RobotSeries.valueOf(e[0]),RobotTier.valueOf(e[1]),pt,e[4].toInt().coerceIn(0,m),m,e[6].toInt(),e[7].toInt(),e[8].toInt())}
            require(bots.isNotEmpty()&&bots.any{it.hp>0}&&bots.size<=2)
            val encounter=migrateId(z[0].toInt())?:error("Invalid encounter");require(AdventureRules.layout(stage).encounters.any{AdventureRules.id(it)==encounter})
            val allies=z.getOrElse(6){""}.split(";").filter{it.isNotBlank()}.map{line->
                val u=line.split(",");val kind=SquadKind.valueOf(u[0]);val point=battlePoint(GridPoint(u[1].toInt(),u[2].toInt()),u[3].toInt()>0)
                require(AdventureRules.inArena(point));val rank=(u.getOrNull(5)?.toIntOrNull()?:1).coerceIn(1,barracks)
                ArenaAlly(kind,point,u[3].toInt().coerceIn(0,SquadRules.unitHp(kind,rank)*SquadRules.MAX_STACK),u[4].toInt().coerceIn(0,kind.charges*SquadRules.MAX_STACK),rank)
            }
            require(allies.size<=(if(barracks>=2)4 else 3)&&allies.map{it.kind}.distinct().size==allies.size)
            fun actors(index:Int)=z.getOrElse(index){""}.split(",").mapNotNull{it.toIntOrNull()}.filter{it in -1 until allies.size}.toSet()
            ArenaBattle(encounter,p,z[2].toInt().coerceIn(1,max),max,bots,z[4].toInt().coerceAtLeast(1),"Бій відновлено. Твій хід.",allies,actors(7),actors(8),z.getOrNull(9)=="1",(z.getOrNull(10)?.toIntOrNull()?:0).coerceIn(0,bots.lastIndex))
        }
        // Older saves don't record completed-sector totals; preserve at least four
        // wins per previous sector (the original map had four encounters).
        val past=(v.getOrNull(11)?.toIntOrNull()?:((stage-1)*4)).coerceIn(0,1000)
        val licensed=if(v.size<=12)squad.keys+(b?.allies?.map{it.kind}?:emptyList()) else v[12].split(",").mapNotNull{n->SquadKind.entries.firstOrNull{it.name==n}}.toSet()
        AdventureState(stage,pos,cleared,ids(v[3]),v[4].toInt().coerceAtLeast(0),b,credited,squad,barracks,ranks.filterKeys{it in squad},past,licensed)
    }.getOrElse{AdventureState()}
}
