package com.pixubi.game

import android.content.Context

internal enum class RobotStat(val title:String) {
    DURABILITY("Міцність"), ENERGY("Енергія"), ATTACK("Атака"), DEFENSE("Захист"),
    SPEED("Швидкість"), INTELLIGENCE("Інтелект"), HEAT("Термостійкість")
}

internal data class RobotStats(
    val durability:Int=0,val energy:Int=0,val attack:Int=0,val defense:Int=0,
    val speed:Int=0,val intelligence:Int=0,val heat:Int=0
) {
    operator fun plus(other:RobotStats)=RobotStats(
        durability+other.durability,energy+other.energy,attack+other.attack,
        defense+other.defense,speed+other.speed,intelligence+other.intelligence,heat+other.heat)
    operator fun get(stat:RobotStat):Int=when(stat) {
        RobotStat.DURABILITY->durability; RobotStat.ENERGY->energy; RobotStat.ATTACK->attack
        RobotStat.DEFENSE->defense; RobotStat.SPEED->speed; RobotStat.INTELLIGENCE->intelligence; RobotStat.HEAT->heat
    }
    fun bounded()=RobotStats(durability.coerceAtLeast(1),energy.coerceAtLeast(1),attack.coerceAtLeast(0),
        defense.coerceAtLeast(0),speed.coerceAtLeast(1),intelligence.coerceAtLeast(0),heat.coerceAtLeast(1))
    fun summary():String=RobotStat.entries.filter{this[it]!=0}.joinToString(" · "){
        val n=this[it]; "${it.title} ${if(n>0) "+" else ""}$n"
    }.ifEmpty{"Без змін"}
}
internal enum class RobotTier(val title:String,val requiredLevel:Int) {
    MK1("MK-1",1), MK2("MK-2",5), MK3("MK-3",12), MK4("MK-4",20), OMEGA("MK-X · OMEGA",30)
}
internal data class RobotGearSpec(val delta:RobotStats,val minTier:RobotTier=RobotTier.MK1)
internal object RobotBalance {
    fun base(tier:RobotTier):RobotStats=when(tier) {
        RobotTier.MK1->RobotStats(100,100,10,10,50,20,50)
        RobotTier.MK2->RobotStats(140,125,20,18,55,35,65)
        RobotTier.MK3->RobotStats(180,150,32,28,62,50,80)
        RobotTier.MK4->RobotStats(240,180,50,45,70,65,100)
        RobotTier.OMEGA->RobotStats(310,235,65,60,85,90,135)
    }
    fun spec(g:RobotGear):RobotGearSpec=RobotGearSpec(when(g) {
        RobotGear.SCANNER->RobotStats(energy=-5,intelligence=12)
        RobotGear.HELMET->RobotStats(durability=8,defense=5,speed=-2)
        RobotGear.ARMOR->RobotStats(durability=25,defense=15,speed=-10,heat=-3)
        RobotGear.REACTOR->RobotStats(energy=30,attack=4,heat=-8)
        RobotGear.SHIELD_L,RobotGear.SHIELD_R->RobotStats(defense=12,speed=-5)
        RobotGear.TOOL_L,RobotGear.TOOL_R->RobotStats(intelligence=7,energy=-3)
        RobotGear.BLASTER_L,RobotGear.BLASTER_R->RobotStats(attack=14,energy=-8,heat=-5)
        RobotGear.BLADE_L,RobotGear.BLADE_R->RobotStats(attack=22,speed=3,energy=-6)
        RobotGear.CANNON_L,RobotGear.CANNON_R->RobotStats(attack=34,energy=-18,speed=-8,heat=-9)
        RobotGear.BATTERY->RobotStats(energy=35,speed=-5)
        RobotGear.BOOSTER->RobotStats(speed=16,energy=-10,heat=-6)
        RobotGear.BOOTS->RobotStats(durability=10,defense=6,speed=-4)
        RobotGear.SPEED_BOOTS->RobotStats(speed=12,energy=-4,defense=-2)
    })
    fun compatible(gear:RobotGear,tier:RobotTier)=tier.ordinal>=spec(gear).minTier.ordinal
    fun total(tier:RobotTier,equipment:RobotEquipment,level:Int=1,skillBonus:RobotStats=RobotStats()):RobotStats {
        val l=(level.coerceIn(1,RobotProgress.MAX_LEVEL)-1)
        val innate=base(tier)+RobotStats(l*3,l*2,l,l,l/2,l,l)+skillBonus
        return RobotSlot.entries.mapNotNull{equipment.equipped(it)}
            .filter{compatible(it,tier)}.fold(innate){acc,g->acc+spec(g).delta}.bounded()
    }
}
internal data class RobotProgress(val xp:Int=0) {
    val safeXp get()=xp.coerceIn(0,MAX_XP)
    val level get()=(1..MAX_LEVEL).last{requiredXp(it)<=safeXp}
    val nextTier get()=RobotTier.entries.last{it!=RobotTier.OMEGA && level>=it.requiredLevel}
    val remaining get()=if(level==MAX_LEVEL)0 else requiredXp(level+1)-safeXp
    val fraction get()=if(level==MAX_LEVEL)1f else (safeXp-requiredXp(level)).toFloat()/(requiredXp(level+1)-requiredXp(level))
    fun gain(amount:Int)=RobotProgress((safeXp.toLong()+amount.coerceAtLeast(0)).coerceAtMost(MAX_XP.toLong()).toInt())
    companion object {
        const val MAX_LEVEL=30
        const val MAX_XP=43500
        fun requiredXp(level:Int):Int { val n=level.coerceIn(1,MAX_LEVEL)-1;return 50*n*(n+1) }
    }
}
internal object RobotProgressStorage {
    private const val PREFS="robot_lab_progress_v1"
    fun load(context:Context)=RobotProgress(context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt("xp",0).coerceIn(0,RobotProgress.MAX_XP))
    fun save(context:Context,progress:RobotProgress) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt("xp",progress.safeXp).apply()
    }
}
