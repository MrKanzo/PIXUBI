package com.pixubi.game

/** Shared balance constants, independent of saved player values. */
internal object AdventureBalance {
    const val BATTLE_ENERGY=5
    const val MIN_INTEGRITY=20
    const val EMERGENCY_ENERGY=25
    const val EMERGENCY_INTEGRITY=35
    // A larger squad should give an advantage, not raise enemy strength one for one.
    fun enemyScale(stage:Int,units:Int):Float =
        (1f+(stage-1).coerceIn(0,24)*.035f)*(1f+units.coerceIn(0,20)*.065f)
    fun battleXp(stage:Int,enemies:Int,elite:Boolean)=70+enemies*30+stage.coerceIn(1,25)*5+(if(elite)80 else 0)
    fun battleCoins(stage:Int,enemies:Int,elite:Boolean)=25+enemies*15+stage.coerceIn(1,25)*4+(if(elite)100 else 0)
    fun needsRecovery(p:WorldPet)=p.adventure.battle==null&&p.mission==null&&p.mode==WorldMode.AWAKE&&
        (p.energy<BATTLE_ENERGY||p.integrity<MIN_INTEGRITY)
    /** Emergency extraction keeps all discoveries, but grants no money, XP or equipment. */
    fun recover(p:WorldPet):WorldPet{
        if(!needsRecovery(p))return p
        val home=p.adventure.pos==AdventureRules.base
        return if(!home)p.copy(adventure=p.adventure.copy(pos=AdventureRules.base),
            message="Аварійна евакуація на базу. Доступний мінімальний ремонт.")
        else p.copy(energy=maxOf(p.energy,EMERGENCY_ENERGY),integrity=maxOf(p.integrity,EMERGENCY_INTEGRITY),
            message="Аварійний сервіс: мінімальний заряд і цілісність відновлено.")
    }
    fun taskEnergy(p:WorldPet,t:WorldTask)=(t.energy-(p.stats.energy-100).coerceAtLeast(0)/35).coerceAtLeast(3)
    fun repairPrice(missingHp:Int)=((missingHp.coerceAtLeast(1)+9)/10*2).coerceIn(5,30)
}
