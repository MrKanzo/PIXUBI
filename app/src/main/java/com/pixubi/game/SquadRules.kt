package com.pixubi.game

internal enum class SquadKind(val title:String,val price:Int,val hp:Int,val attack:Int,val defense:Int,val move:Int,val range:Int,val charges:Int,val series:RobotSeries,val gear:RobotGear,val skin:RobotSkin,val requiredBarracks:Int=1,val tier:RobotTier=RobotTier.MK1){
    RANGER("Стрілець",90,44,15,5,2,4,8,RobotSeries.GLITCH,RobotGear.RIFLE_L,RobotSkin.CYAN),
    ASSAULT("Штурмовик",75,65,19,9,3,1,0,RobotSeries.KAGE,RobotGear.BLADE_L,RobotSkin.RED),
    GUARD("Захисник",100,95,10,24,2,1,0,RobotSeries.CLASSIC,RobotGear.SHIELD_L,RobotSkin.GOLD),
    MEDIC("Ремонтний дрон",110,50,7,8,2,1,3,RobotSeries.NEXUS,RobotGear.REPAIR_L,RobotSkin.GREEN),
    SIEGE("Облогова машина",240,125,30,20,1,5,5,RobotSeries.CLASSIC,RobotGear.CANNON_L,RobotSkin.GRAPHITE,2,RobotTier.MK3),
    AERIAL("Повітряний розвідник",260,64,23,8,4,3,6,RobotSeries.MYCELIA,RobotGear.ARC_L,RobotSkin.PURPLE,3,RobotTier.OMEGA);
    fun loadout()=RobotLoadout(skin.accent,RobotEquipment().equip(gear),tier,series)
}
internal data class ArenaAlly(val kind:SquadKind,val pos:GridPoint,val hp:Int=kind.hp,val charges:Int=kind.charges,val rank:Int=1){
    val unitHp get()=SquadRules.unitHp(kind,rank)
    val count get()=SquadRules.count(hp,unitHp)
    val maxHp get()=count.coerceAtLeast(1)*unitHp
    val attack get()=(kind.attack+4*(rank-1))*count
    val defense get()=kind.defense+5*(rank-1)
    val move get()=kind.move
    val range get()=kind.range
    val maxCharges get()=kind.charges*count
}
internal object SquadRules{
    const val MAX=3
    const val MAX_STACK=5
    fun slots(a:AdventureState)=if(a.barracks>=2)4 else MAX
    fun rank(a:AdventureState,k:SquadKind)=(a.squadRanks[k]?:1).coerceIn(1,3)
    fun unitHp(k:SquadKind,rank:Int)=k.hp+(rank-1)*((k.hp+3)/4)
    fun count(hp:Int,unitHp:Int)=if(hp<=0)0 else (hp+unitHp-1)/unitHp
    fun stackCount(a:AdventureState,k:SquadKind)=count(a.squad[k]?:0,unitHp(k,rank(a,k)))
    fun maxStackHp(a:AdventureState,k:SquadKind)=stackCount(a,k)*unitHp(k,rank(a,k))
    fun atBase(p:WorldPet)=p.adventure.pos==AdventureRules.base&&p.adventure.battle==null&&p.mission==null
    fun barracksPrice(a:AdventureState)=if(a.barracks==1)220 else 420
    fun expand(p:WorldPet):WorldPet{
        val a=p.adventure;val price=barracksPrice(a)
        if(!atBase(p)||a.barracks>=3||p.coins<price)return p
        return p.copy(coins=p.coins-price,adventure=a.copy(barracks=a.barracks+1),message="Казарми: рівень ${a.barracks+1}")
    }
    fun recruit(p:WorldPet,k:SquadKind):WorldPet{
        val a=p.adventure;val hp=a.squad[k]?:0
        if(!atBase(p)||!SquadResearch.unlocked(a,k)||a.barracks<k.requiredBarracks||(hp==0&&a.squad.size>=slots(a))||stackCount(a,k)>=MAX_STACK||p.coins<k.price)return p
        return p.copy(coins=p.coins-k.price,adventure=a.copy(squad=a.squad+(k to hp+unitHp(k,rank(a,k))),licensed=a.licensed+k),message="${k.title}: +1 боєць")
    }
    fun upgradePrice(a:AdventureState,k:SquadKind)=k.price*rank(a,k)*stackCount(a,k)
    fun upgrade(p:WorldPet,k:SquadKind):WorldPet{
        val a=p.adventure;val hp=a.squad[k]?:return p;val rank=rank(a,k);val price=upgradePrice(a,k)
        if(!atBase(p)||rank>=3||rank>=a.barracks||p.coins<price)return p
        val old=unitHp(k,rank);val next=unitHp(k,rank+1)
        val newHp=((hp.toLong()*next+old-1)/old).toInt().coerceAtLeast(1)
        return p.copy(coins=p.coins-price,adventure=a.copy(squad=a.squad+(k to newHp),squadRanks=a.squadRanks+(k to rank+1)),message="${k.title}: ранг ${rank+1}")
    }
    fun restore(p:WorldPet,k:SquadKind):WorldPet{
        val a=p.adventure;val hp=a.squad[k]?:return p;val max=maxStackHp(a,k)
        if(!atBase(p)||hp>=max||p.coins<15)return p
        return p.copy(coins=p.coins-15,adventure=a.copy(squad=a.squad+(k to max)),message="${k.title}: відновлено HP")
    }
    fun dismiss(p:WorldPet,k:SquadKind):WorldPet{
        val a=p.adventure
        if(!atBase(p)||k !in a.squad)return p
        return p.copy(adventure=a.copy(squad=a.squad-k,squadRanks=a.squadRanks-k),message="${k.title}: виведено із загону")
    }
    fun deploy(roster:Map<SquadKind,Int>,ranks:Map<SquadKind,Int> = emptyMap(),limit:Int=MAX):List<ArenaAlly>{
        val slots=listOf(GridPoint(0,1),GridPoint(0,3),GridPoint(1,2),GridPoint(0,4))
        return roster.entries.sortedBy{it.key.ordinal}.filter{it.value>0}.take(limit.coerceIn(1,4)).mapIndexed{i,(k,h)->
            val rank=(ranks[k]?:1).coerceIn(1,3);val hp=h.coerceAtMost(unitHp(k,rank)*MAX_STACK)
            ArenaAlly(k,slots[i],hp,k.charges*count(hp,unitHp(k,rank)),rank)
        }
    }
    fun available(b:ArenaBattle):List<Int> = buildList{
        if(b.hp>0&&-1 !in b.acted)add(-1)
        b.allies.forEachIndexed{i,u->if(u.hp>0&&i !in b.acted)add(i)}
    }
    fun ready(b:ArenaBattle,i:Int)=i in available(b)
    fun position(b:ArenaBattle,i:Int)=if(i==-1)b.pos else b.allies[i].pos
    fun moveRange(p:WorldPet,b:ArenaBattle,i:Int)=if(i==-1)AdventureRules.movement(p) else b.allies[i].move
    fun reachable(p:WorldPet,b:ArenaBattle,i:Int,q:GridPoint):Boolean{
        if(!ready(b,i))return false
        val from=position(b,i)
        val blocked=b.enemies.filter{it.hp>0}.map{it.pos}.toSet()+b.allies.filterIndexed{j,u->j!=i&&u.hp>0}.map{it.pos}+(if(i!=-1)setOf(b.pos)else emptySet())
        if(q==from||q in blocked||!AdventureRules.inArena(q))return false
        val distance=if(i>=0&&b.allies[i].kind==SquadKind.AERIAL)AdventureRules.arenaDistance(from,q) else AdventureRules.path(from,q,blocked)?:999
        return distance<=moveRange(p,b,i)
    }
    fun canHit(b:ArenaBattle,i:Int,e:ArenaBot):Boolean{
        val u=b.allies.getOrNull(i)?:return false
        return ready(b,i)&&e.hp>0&&(u.kind.range==1||u.charges>0)&&AdventureRules.arenaDistance(u.pos,e.pos)<=u.kind.range
    }
    fun repairTarget(b:ArenaBattle,i:Int):Int?{
        val u=b.allies.getOrNull(i)?:return null
        if(u.kind!=SquadKind.MEDIC||!ready(b,i)||u.charges<=0)return null
        val options=buildList{
            if(b.hp in 1 until b.maxHp&&AdventureRules.arenaDistance(u.pos,b.pos)<=2)add(-1 to b.hp.toFloat()/b.maxHp)
            b.allies.forEachIndexed{j,a->if(a.hp in 1 until a.maxHp&&AdventureRules.arenaDistance(u.pos,a.pos)<=2)add(j to a.hp.toFloat()/a.maxHp)}
        }
        return options.minByOrNull{it.second}?.first
    }
}

internal data class SquadBlueprint(val wins:Int,val sector:Int)
internal object SquadResearch{
    fun requirement(k:SquadKind)=when(k){
        SquadKind.ASSAULT,SquadKind.RANGER->SquadBlueprint(0,1)
        SquadKind.GUARD->SquadBlueprint(2,1)
        SquadKind.MEDIC->SquadBlueprint(4,1)
        SquadKind.SIEGE->SquadBlueprint(10,2)
        SquadKind.AERIAL->SquadBlueprint(20,3)
    }
    fun victories(a:AdventureState)=a.pastCampaignWins+AdventureRules.encounters(a).count{AdventureRules.id(it) in a.cleared}
    fun unlocked(a:AdventureState,k:SquadKind):Boolean{
        val req=requirement(k)
        return k in a.licensed||k in a.squad||(a.stage>=req.sector&&victories(a)>=req.wins)
    }
    fun progress(a:AdventureState,k:SquadKind):String{
        val req=requirement(k)
        return if(unlocked(a,k))"Технологію відкрито" else "Перемоги на карті: ${victories(a).coerceAtMost(req.wins)}/${req.wins} · сектор ${a.stage.coerceAtMost(req.sector)}/${req.sector}"
    }
}

internal data class SquadClassified(val title:String,val code:String,val secret:Boolean)
internal object SquadDisclosure{
    // This metadata contains no model, series, weapon, skin or combat statistics.
    fun locked(a:AdventureState,k:SquadKind):SquadClassified?{
        if(SquadResearch.unlocked(a,k))return null
        return when(k){
            SquadKind.GUARD->SquadClassified("Захисник","TECH 03",false)
            SquadKind.MEDIC->SquadClassified("Ремонтний дрон","TECH 04",false)
            SquadKind.SIEGE->SquadClassified("Секретний прототип","PROJECT 05",true)
            SquadKind.AERIAL->SquadClassified("Невідома технологія","PROJECT 06",true)
            else->SquadClassified("Невідомий юніт","CLASSIFIED",true)
        }
    }
}
