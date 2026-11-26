package com.pixubi.game

internal enum class WorldCategory(val title:String){
    WEAPONS("Зброя"), ARMOR("Захист"), ENERGY("Енергія"), REPAIR("Ремонт"), AMMO("Боєприпаси"), OTHER("Інші модулі")
}
internal enum class WorldSupply(val title:String,val category:WorldCategory,val price:Int,val level:Int,val pack:Int,val amount:Int,val description:String){
    PULSE("Імпульсні касети",WorldCategory.AMMO,20,1,12,0,"Бластер і гвинтівка"),
    ION("Іонні капсули",WorldCategory.AMMO,35,1,8,0,"Іонна гармата"),
    ARC("Дугові заряди",WorldCategory.AMMO,45,1,6,0,"Дуговий випромінювач"),
    CELL_SMALL("Комірка MINI",WorldCategory.ENERGY,12,1,2,25,"Відновлює 25% енергії"),
    CELL_MAX("Комірка MAX",WorldCategory.ENERGY,45,4,2,80,"Відновлює 80% енергії"),
    PATCH("Мікропатч корпусу",WorldCategory.REPAIR,15,1,2,20,"Відновлює 20% корпусу"),
    NANOKIT("Наноремонтний комплект",WorldCategory.REPAIR,45,4,2,75,"Відновлює 75% корпусу"),
    SYNC("Преміум синхромодуль",WorldCategory.OTHER,32,3,2,75,"Відновлює 75% настрою");
    val isAmmo get()=category==WorldCategory.AMMO
}
internal object WorldGearCatalog {
    fun ammo(g:RobotGear):Pair<WorldSupply,Int>?=when(g){
        RobotGear.BLASTER_L,RobotGear.BLASTER_R->WorldSupply.PULSE to 2
        RobotGear.RIFLE_L,RobotGear.RIFLE_R->WorldSupply.PULSE to 4
        RobotGear.CANNON_L,RobotGear.CANNON_R->WorldSupply.ION to 2
        RobotGear.ARC_L,RobotGear.ARC_R->WorldSupply.ARC to 2
        else->null
    }
    fun category(g:RobotGear)=when(g){
        RobotGear.BLASTER_L,RobotGear.BLASTER_R,RobotGear.BLADE_L,RobotGear.BLADE_R,RobotGear.CANNON_L,RobotGear.CANNON_R,
        RobotGear.RIFLE_L,RobotGear.RIFLE_R,RobotGear.ARC_L,RobotGear.ARC_R->WorldCategory.WEAPONS
        RobotGear.HELMET,RobotGear.ARMOR,RobotGear.LIGHT_ARMOR,RobotGear.HEAVY_ARMOR,RobotGear.SHIELD_L,RobotGear.SHIELD_R,
        RobotGear.BOOTS,RobotGear.MAG_BOOTS->WorldCategory.ARMOR
        RobotGear.REACTOR,RobotGear.BATTERY,RobotGear.POWERBANK_MINI,RobotGear.POWERBANK_MAX->WorldCategory.ENERGY
        RobotGear.TOOL_L,RobotGear.TOOL_R,RobotGear.REPAIR_L,RobotGear.REPAIR_R,RobotGear.REPAIR_DRONE->WorldCategory.REPAIR
        else->WorldCategory.OTHER
    }
    fun repairBonus(e:RobotEquipment)=e.items.values.sumOf{g->when(g){
        RobotGear.TOOL_L,RobotGear.TOOL_R->5
        RobotGear.REPAIR_L,RobotGear.REPAIR_R->15
        RobotGear.REPAIR_DRONE->20
        else->0
    }}
    fun energyBonus(e:RobotEquipment)=when(e.equipped(RobotSlot.BACK)){
        RobotGear.POWERBANK_MINI->8
        RobotGear.BATTERY->12
        RobotGear.POWERBANK_MAX->25
        else->0
    }
    fun ammoCost(e:RobotEquipment):Map<WorldSupply,Int>{
        val result=mutableMapOf<WorldSupply,Int>()
        e.items.values.forEach{g->ammo(g)?.let{(type,count)->result[type]=(result[type]?:0)+count}}
        return result
    }
    fun description(g:RobotGear):String=when(g){
        RobotGear.POWERBANK_MINI,RobotGear.BATTERY,RobotGear.POWERBANK_MAX->"Додаткова енергоємність і бонус заряджання: +"+energyBonus(RobotEquipment().equip(g))+"%."
        RobotGear.REPAIR_L,RobotGear.REPAIR_R,RobotGear.REPAIR_DRONE,RobotGear.TOOL_L,RobotGear.TOOL_R->"Підсилює ремонт на "+repairBonus(RobotEquipment().equip(g))+"%."
        else->ammo(g)?.let{(a,n)->"${a.title}: $n шт. за бойову місію."}?:if(category(g)==WorldCategory.WEAPONS)"Енергетична зброя ближнього бою · не потребує касет." else "Постійний модуль спорядження."
    }
    fun visual(g:RobotGear)=when(g){
        RobotGear.VISOR->RobotGear.SCANNER
        RobotGear.LIGHT_ARMOR,RobotGear.HEAVY_ARMOR->RobotGear.ARMOR
        RobotGear.POWERBANK_MINI,RobotGear.POWERBANK_MAX->RobotGear.BATTERY
        RobotGear.REPAIR_DRONE->RobotGear.BOOSTER
        RobotGear.MAG_BOOTS->RobotGear.BOOTS
        else->g
    }
    fun visual(e:RobotEquipment)=RobotEquipment(e.items.mapValues{visual(it.value)})
}
