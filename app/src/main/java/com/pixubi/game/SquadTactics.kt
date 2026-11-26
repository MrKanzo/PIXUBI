package com.pixubi.game

internal enum class SquadCombo(val title:String,val detail:String,val kinds:Set<SquadKind>){
    COVER("Вогневе прикриття","Стрільці + захисник: +6 атаки стрільця за загін.",setOf(SquadKind.RANGER,SquadKind.GUARD)),
    RAID("Штурмова ланка","Стрільці + штурмовик: +5 атаки штурмовика за загін.",setOf(SquadKind.RANGER,SquadKind.ASSAULT)),
    FORTIFY("Ремонтний бастіон","Захисник + ремонт: +8 захисту захисника, +15 HP за ремонт.",setOf(SquadKind.GUARD,SquadKind.MEDIC)),
    LINK("Важка координація","Облогова машина + повітряний розвідник: +10 атаки кожного з цих загонів.",setOf(SquadKind.SIEGE,SquadKind.AERIAL));
}
internal object SquadTactics{
    fun active(kinds:Set<SquadKind>)=SquadCombo.entries.filter{kinds.containsAll(it.kinds)}
    fun active(b:ArenaBattle)=active(b.allies.filter{it.hp>0}.map{it.kind}.toSet())
    fun attackBonus(b:ArenaBattle,k:SquadKind):Int{
        val a=active(b)
        return (if(k==SquadKind.RANGER&&SquadCombo.COVER in a)6 else 0)+
            (if(k==SquadKind.ASSAULT&&SquadCombo.RAID in a)5 else 0)+
            (if(k in SquadCombo.LINK.kinds&&SquadCombo.LINK in a)10 else 0)
    }
    fun defenseBonus(b:ArenaBattle,actor:Int)=if(b.allies.getOrNull(actor)?.kind==SquadKind.GUARD&&SquadCombo.FORTIFY in active(b))8 else 0
    fun repairBonus(b:ArenaBattle)=if(SquadCombo.FORTIFY in active(b))15 else 0
}
