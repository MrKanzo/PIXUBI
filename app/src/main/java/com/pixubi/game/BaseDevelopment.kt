package com.pixubi.game

internal object GearAccess{
    const val DUAL_LEVEL=12
    private fun family(g:RobotGear)=g.name.removeSuffix("_L").removeSuffix("_R")
    private fun hand(g:RobotGear)=g.slot==RobotSlot.LEFT_HAND||g.slot==RobotSlot.RIGHT_HAND
    private fun allowed(series:RobotSeries,g:RobotGear):Boolean{
        if(!hand(g))return true
        val kinds=when(series){
            RobotSeries.CLASSIC->setOf("SHIELD","TOOL","REPAIR","BLASTER","RIFLE","CANNON","BLADE")
            RobotSeries.COMPANION->setOf("BLASTER","TOOL","REPAIR","SHIELD")
            RobotSeries.KAGE->setOf("BLADE")
            RobotSeries.GLITCH->setOf("BLASTER","RIFLE","ARC","TOOL")
            RobotSeries.VALKYRIE->setOf("SHIELD","BLADE","ARC","REPAIR")
            RobotSeries.NEXUS->setOf("ARC","TOOL","REPAIR","BLASTER")
            RobotSeries.SYMBIONT->setOf("BLADE")
            RobotSeries.MYCELIA->setOf("ARC","REPAIR")
        }
        return family(g) in kinds
    }
    fun title(series:RobotSeries,g:RobotGear)=if(series==RobotSeries.KAGE&&family(g)=="BLADE")"КАТАНА · плазмовий клинок" else g.title
    fun reason(p:WorldPet,g:RobotGear):String?{
        if(!allowed(p.series,g))return "Несумісно з ${p.series.title}"
        if(p.series==RobotSeries.KAGE&&family(g)=="BLADE"&&p.level<DUAL_LEVEL&&p.equipment.items.values.any{it.slot!=g.slot&&family(it)=="BLADE"})return "Друга катана · рівень $DUAL_LEVEL"
        return null
    }
    fun sanitize(p:WorldPet):RobotEquipment{
        var e=RobotEquipment()
        RobotSlot.entries.forEach{slot->p.equipment.equipped(slot)?.let{g->if(reason(p.copy(equipment=e),g)==null)e=e.equip(g)}}
        return e
    }
}
internal enum class BaseBuilding(val title:String,val code:String,val price:Int,val level:Int,val description:String){
    BARRACKS("Казарми","UNIT HUB",120,1,"Створення звичайних загонів, ремонт та ранги."),
    FOUNDRY("Завод прототипів","OMEGA FORGE",420,6,"Важкі та повітряні юніти після відкриття технологій."),
    NEURAL("Голографічний термінал","NEURAL LINK",180,2,"Купівля рівнів бойового модуля командира."),
    SERVICE("Сервісна станція","REPAIR LAB",150,1,"Купівля зарядних, ремонтних і синхромодулів."),
    WORKSHOP("Майстерня","FABRICATOR",160,2,"Крафт спорядження й бойових юнітів із видобутих матеріалів.");
    companion object{
        fun decode(raw:String)=raw.split(",").mapNotNull{id->entries.firstOrNull{it.name==id}}.toSet()
    }
}
internal object BaseRules{
    fun buildingFor(k:SquadKind)=if(k.requiredBarracks>1)BaseBuilding.FOUNDRY else BaseBuilding.BARRACKS
    fun available(p:WorldPet)=SquadRules.atBase(p)&&p.mode==WorldMode.AWAKE
    fun buyReason(p:WorldPet,b:BaseBuilding):String?=when{
        b in p.buildings->"Уже побудовано"
        !available(p)->"Потрібно повернутися на базу й розбудити робота"
        p.level<b.level->"Потрібен рівень ${b.level}"
        b==BaseBuilding.FOUNDRY&&BaseBuilding.BARRACKS !in p.buildings->"Спочатку побудуй казарми"
        p.coins<b.price->"Не вистачає монет"
        else->null
    }
    fun buy(p:WorldPet,b:BaseBuilding):WorldPet{
        buyReason(p,b)?.let{return p.copy(message=it)}
        return p.copy(coins=p.coins-b.price,buildings=p.buildings+b,message="Побудовано: ${b.title}")
    }
    fun recruitLimit(p:WorldPet,k:SquadKind):Int{
        val a=p.adventure
        if(!available(p)||buildingFor(k) !in p.buildings||!SquadResearch.unlocked(a,k)||a.barracks<k.requiredBarracks||(k !in a.squad&&a.squad.size>=SquadRules.slots(a)))return 0
        return minOf(SquadRules.MAX_STACK-SquadRules.stackCount(a,k),p.coins/k.price).coerceAtLeast(0)
    }
    fun recruit(p:WorldPet,k:SquadKind,count:Int):WorldPet{
        if(count !in 1..recruitLimit(p,k))return p.copy(message="Недоступна кількість юнітів")
        var next=p
        repeat(count){next=SquadRules.recruit(next,k)}
        return next.copy(message="${k.title}: +$count бійців")
    }
    fun neuralPrice(p:WorldPet)=listOf(100,220,380).getOrElse(p.neuralRank){0}
    fun neuralReason(p:WorldPet):String?=when{
        BaseBuilding.NEURAL !in p.buildings->"Потрібен NEURAL LINK"
        !available(p)->"Потрібно повернутися на базу"
        p.neuralRank>=3->"Максимальний рівень"
        p.level<2+p.neuralRank*4->"Потрібен рівень ${2+p.neuralRank*4}"
        p.coins<neuralPrice(p)->"Не вистачає монет"
        else->null
    }
    fun buyNeural(p:WorldPet):WorldPet{
        neuralReason(p)?.let{return p.copy(message=it)}
        return p.copy(coins=p.coins-neuralPrice(p),neuralRank=p.neuralRank+1,message="NEURAL LINK: рівень ${p.neuralRank+1}")
    }
}
