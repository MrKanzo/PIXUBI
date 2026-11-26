package com.pixubi.game

internal enum class SquadKind(val title:String,val price:Int,val hp:Int,val attack:Int,val defense:Int,val move:Int,val range:Int,val charges:Int,val series:RobotSeries,val gear:RobotGear,val skin:RobotSkin){
    RANGER("Стрілець",90,44,15,5,2,4,8,RobotSeries.GLITCH,RobotGear.RIFLE_L,RobotSkin.CYAN),
    ASSAULT("Штурмовик",75,65,19,9,3,1,0,RobotSeries.KAGE,RobotGear.BLADE_L,RobotSkin.RED),
    GUARD("Захисник",100,95,10,24,2,1,0,RobotSeries.CLASSIC,RobotGear.SHIELD_L,RobotSkin.GOLD),
    MEDIC("Ремонтний дрон",110,50,7,8,2,1,3,RobotSeries.NEXUS,RobotGear.REPAIR_L,RobotSkin.GREEN);
    fun loadout()=RobotLoadout(skin.accent,RobotEquipment().equip(gear),RobotTier.MK1,series)
}
internal data class ArenaAlly(val kind:SquadKind,val pos:GridPoint,val hp:Int=kind.hp,val charges:Int=kind.charges)
internal object SquadRules{
    const val MAX=3
    fun atBase(p:WorldPet)=p.adventure.pos==AdventureRules.base&&p.adventure.battle==null&&p.mission==null
    fun recruit(p:WorldPet,k:SquadKind):WorldPet{
        val a=p.adventure
        if(!atBase(p)||a.squad.size>=MAX||k in a.squad||p.coins<k.price)return p
        return p.copy(coins=p.coins-k.price,adventure=a.copy(squad=a.squad+(k to k.hp)),message="${k.title} приєднався до загону")
    }
    fun restore(p:WorldPet,k:SquadKind):WorldPet{
        val a=p.adventure;val hp=a.squad[k]?:return p
        if(!atBase(p)||hp>=k.hp||p.coins<15)return p
        return p.copy(coins=p.coins-15,adventure=a.copy(squad=a.squad+(k to k.hp)),message="${k.title}: відновлено HP")
    }
    fun dismiss(p:WorldPet,k:SquadKind):WorldPet{
        if(!atBase(p)||k !in p.adventure.squad)return p
        return p.copy(adventure=p.adventure.copy(squad=p.adventure.squad-k),message="${k.title}: виведено із загону")
    }
    fun deploy(roster:Map<SquadKind,Int>):List<ArenaAlly>{
        val slots=listOf(GridPoint(0,1),GridPoint(0,3),GridPoint(1,2))
        return roster.entries.sortedBy{it.key.ordinal}.filter{it.value>0}.take(MAX).mapIndexed{i,(k,h)->ArenaAlly(k,slots[i],h.coerceAtMost(k.hp))}
    }
    fun available(b:ArenaBattle):List<Int> = buildList{
        if(b.hp>0&&-1 !in b.acted)add(-1)
        b.allies.forEachIndexed{i,u->if(u.hp>0&&i !in b.acted)add(i)}
    }
    fun ready(b:ArenaBattle,i:Int)=i in available(b)
    fun position(b:ArenaBattle,i:Int)=if(i==-1)b.pos else b.allies[i].pos
    fun moveRange(p:WorldPet,b:ArenaBattle,i:Int)=if(i==-1)AdventureRules.movement(p) else b.allies[i].kind.move
    fun reachable(p:WorldPet,b:ArenaBattle,i:Int,q:GridPoint):Boolean{
        if(!ready(b,i))return false
        val from=position(b,i)
        val blocked=b.enemies.filter{it.hp>0}.map{it.pos}.toSet()+b.allies.filterIndexed{j,u->j!=i&&u.hp>0}.map{it.pos}+(if(i!=-1)setOf(b.pos)else emptySet())
        return q!=from&&(AdventureRules.path(from,q,blocked)?:999)<=moveRange(p,b,i)
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
            b.allies.forEachIndexed{j,a->if(a.hp in 1 until a.kind.hp&&AdventureRules.arenaDistance(u.pos,a.pos)<=2)add(j to a.hp.toFloat()/a.kind.hp)}
        }
        return options.minByOrNull{it.second}?.first
    }
}
