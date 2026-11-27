package com.pixubi.game

import android.content.Context
import kotlin.random.Random

internal enum class WorldMode { AWAKE,SLEEP,HIBERNATE }
internal enum class WorldTask(val title:String,val seconds:Int,val energy:Int,val mood:Int,val xp:Int,val coins:Int,val action:RobotAction,val skill:RobotSkill){
    WALK("Прогулянка",15,5,5,30,12,RobotAction.WALK,RobotSkill.MOBILITY),
    SEARCH("Експедиція",45,16,-3,95,45,RobotAction.SCAN,RobotSkill.EXPLORATION),
    TRAIN("Бойова арена",25,14,-4,75,35,RobotAction.ATTACK,RobotSkill.COMBAT),
    PUZZLE("Синхронізація",20,8,7,45,20,RobotAction.WAVE,RobotSkill.LOGIC)
}
internal data class WorldMission(val task:WorldTask,val until:Long,val power:Int=0)
internal data class WorldPet(val series:RobotSeries,val skin:RobotSkin,val name:String,val trait:GenesisTrait,
    val xp:Int=0,val coins:Int=180,val energy:Int=100,val integrity:Int=100,val mood:Int=100,
    val cells:Int=4,val repair:Int=3,val snacks:Int=4,val mode:WorldMode=WorldMode.AWAKE,
    val equipment:RobotEquipment=RobotEquipment(),val owned:Set<RobotGear> = emptySet(),
    val skills:RobotSkills=RobotSkills(),val wins:Int=0,val expeditions:Int=0,val last:Long=0,
    val mission:WorldMission?=null,val message:String="GENESIS завершено. Системи активні!",
    val supplies:Map<WorldSupply,Int> = mapOf(WorldSupply.PULSE to 12,WorldSupply.ION to 4,WorldSupply.ARC to 2),
    val adventure:AdventureState=AdventureState(),val buildings:Set<BaseBuilding> = emptySet(),val neuralRank:Int=0){
    fun stock(kind:WorldSupply)=(supplies[kind]?:0).coerceIn(0,99)
    val level get()=RobotProgress(xp).level
    val tier get()=if(level>=30&&wins>=10&&expeditions>=10)RobotTier.OMEGA else RobotProgress(xp).nextTier
    val stats get()=RobotBalance.total(tier,equipment,level,skills.bonuses())+WorldRules.innate(series)
}
internal data class WorldOffer(val gear:RobotGear,val price:Int,val level:Int)
internal object WorldRules{
    fun innate(s:RobotSeries)=when(s){
        RobotSeries.CLASSIC->RobotStats(durability=10,defense=5)
        RobotSeries.COMPANION->RobotStats(energy=15,intelligence=5)
        RobotSeries.KAGE->RobotStats(speed=15,attack=5)
        RobotSeries.GLITCH->RobotStats(intelligence=15,speed=5)
        RobotSeries.VALKYRIE->RobotStats(defense=12,heat=5)
        RobotSeries.NEXUS->RobotStats(intelligence=22,energy=5)
        RobotSeries.SYMBIONT->RobotStats(attack=14,durability=12)
        RobotSeries.MYCELIA->RobotStats(energy=12,durability=10)
    }
    val shop=RobotGear.entries.map{g->
        when(g){
            RobotGear.VISOR->WorldOffer(g,160,3)
            RobotGear.LIGHT_ARMOR->WorldOffer(g,145,3)
            RobotGear.HEAVY_ARMOR->WorldOffer(g,480,12)
            RobotGear.POWERBANK_MINI->WorldOffer(g,90,1)
            RobotGear.POWERBANK_MAX->WorldOffer(g,360,8)
            RobotGear.REPAIR_DRONE->WorldOffer(g,330,8)
            RobotGear.MAG_BOOTS->WorldOffer(g,220,5)
            RobotGear.RIFLE_L,RobotGear.RIFLE_R->WorldOffer(g,280,6)
            RobotGear.ARC_L,RobotGear.ARC_R->WorldOffer(g,580,16)
            RobotGear.REPAIR_L,RobotGear.REPAIR_R->WorldOffer(g,150,3)
            RobotGear.BLADE_L,RobotGear.BLADE_R->WorldOffer(g,220,5)
            RobotGear.CANNON_L,RobotGear.CANNON_R->WorldOffer(g,410,12)
            RobotGear.BLASTER_L,RobotGear.BLASTER_R->WorldOffer(g,140,2)
            RobotGear.ARMOR,RobotGear.REACTOR,RobotGear.BOOSTER,RobotGear.SPEED_BOOTS->WorldOffer(g,180,4)
            RobotGear.HELMET,RobotGear.BATTERY,RobotGear.BOOTS->WorldOffer(g,110,2)
            else->WorldOffer(g,75,1)
        }
    }
    fun hatch(s:RobotSeries,name:String,now:Long,random:Random=Random.Default):WorldPet{
        val start=GenesisRules.create(s,random)
        return WorldPet(s,start.skin,name.trim().take(24).ifBlank{s.title},start.trait,last=now)
    }
    fun adoptGenesis(s:GenesisState,now:Long):WorldPet?{
        if(!s.born||s.energy!=100||s.careMask!=7||s.name.isBlank())return null
        return WorldPet(s.series,s.skin,s.name.trim().take(24),s.trait,last=now,
            message=if(s.rare)"GENESIS завершено. Рідкісне золоте ядро активоване!" else "GENESIS завершено. Системи активні!")
    }
    internal fun gain(p:WorldPet,skill:RobotSkill,xp:Int,points:Int,coins:Int):WorldPet=
        p.copy(xp=RobotProgress(p.xp).gain(xp).safeXp,coins=(p.coins+coins).coerceAtMost(999999),
            skills=p.skills.copy(points=p.skills.points+(skill to (p.skills.xp(skill)+points).coerceAtMost(RobotSkills.MAX_XP))))
    fun tick(p:WorldPet,now:Long):WorldPet{
        val safeNow=now.coerceAtLeast(p.last)
        if(p.mode==WorldMode.HIBERNATE||p.adventure.battle!=null)return p
        val minutes=((safeNow-p.last)/60000L).coerceIn(0L,10080L).toInt()
        val blocks=minutes/15
        var q=if(p.mode==WorldMode.SLEEP&&minutes>0)p.copy(energy=(p.energy+minutes*2).coerceAtMost(100),last=p.last+minutes*60000L)
        else if(p.mode==WorldMode.AWAKE&&blocks>0)p.copy(energy=(p.energy-blocks*2).coerceAtLeast(0),mood=(p.mood-blocks).coerceAtLeast(0),
            integrity=(p.integrity-(blocks-p.energy/2).coerceAtLeast(0)).coerceAtLeast(1),last=p.last+blocks*900000L)
        else p
        val m=q.mission?:return q
        if(safeNow<m.until)return q
        val t=m.task
        q=q.copy(mission=null)
        if(t==WorldTask.TRAIN){
            val target=24+q.level*5
            val win=m.power>=target
            val damage=((if(win)5 else 16)-(q.stats.defense/30)).coerceAtLeast(1)
            q=q.copy(integrity=(q.integrity-damage).coerceAtLeast(1),wins=q.wins+if(win)1 else 0,
                message=if(win)"Перемога! +${t.xp} XP · +${t.coins} монет" else "Арена: поразка. Покращ спорядження й відремонтуй корпус.")
            return gain(q,t.skill,if(win)t.xp else 15,if(win)30 else 10,if(win)t.coins else 0)
        }
        q=q.copy(expeditions=q.expeditions+if(t==WorldTask.SEARCH)1 else 0,
            message="${t.title} завершено · +${t.xp} XP · +${t.coins} монет")
        return gain(q,t.skill,t.xp,25,t.coins)
    }
    fun start(source:WorldPet,t:WorldTask,now:Long):WorldPet{
        val p=tick(source,now)
        if(p.adventure.battle!=null)return p
        if(p.mission!=null||p.mode!=WorldMode.AWAKE)return p.copy(message="Спочатку заверши дію або розбуди персонажа.")
        val cost=AdventureBalance.taskEnergy(p,t)
        if(p.energy<cost||p.integrity<20)return p.copy(message="Потрібна зарядка або ремонт.")
        val ammo=if(t==WorldTask.TRAIN)WorldGearCatalog.ammoCost(p.equipment) else emptyMap()
        val shortage=ammo.entries.firstOrNull{p.stock(it.key)<it.value}
        if(shortage!=null)return p.copy(message="Недостатньо боєприпасів: ${shortage.key.title}. Поповни запас або зніми зброю.")
        val paidAmmo=p.supplies.toMutableMap()
        ammo.forEach{(type,count)->paidAmmo[type]=p.stock(type)-count}
        val bonus=if(t==WorldTask.TRAIN)p.stats.attack+p.stats.defense/2+p.stats.speed/5 else 0
        val duration=(t.seconds-(p.stats.speed-50).coerceAtLeast(0)/10).coerceAtLeast(8)
        return p.copy(energy=p.energy-cost,mood=(p.mood+t.mood).coerceIn(0,100),supplies=paidAmmo,
            mission=WorldMission(t,now+duration*1000L,bonus),message="${t.title} розпочато")
    }
    fun care(p:WorldPet,kind:Int):WorldPet{
        if(p.adventure.battle!=null)return p
        if(p.mode!=WorldMode.AWAKE||p.mission!=null)return p.copy(message="Спочатку заверши дію або розбуди персонажа.")
        return when(kind){
            0->if(p.cells>0&&p.energy<100)gain(p.copy(cells=p.cells-1,energy=(p.energy+45+WorldGearCatalog.energyBonus(p.equipment)).coerceAtMost(100),message="Заряд поповнено"),RobotSkill.ENERGY,10,15,0) else p.copy(message="Немає комірок або заряд повний.")
            1->if(p.repair>0&&p.integrity<100)gain(p.copy(repair=p.repair-1,integrity=(p.integrity+40+WorldGearCatalog.repairBonus(p.equipment)).coerceAtMost(100),message="Корпус відновлено"),RobotSkill.ENGINEERING,10,15,0) else p.copy(message="Немає ремонтних наборів або корпус цілий.")
            else->if(p.snacks>0&&p.mood<100)p.copy(snacks=p.snacks-1,mood=(p.mood+40).coerceAtMost(100),message="Настрій покращено") else p.copy(message="Немає модулів синхронізації або настрій повний.")
        }
    }
    fun mode(source:WorldPet,mode:WorldMode,now:Long):WorldPet{
        val p=tick(source,now)
        if(p.adventure.battle!=null)return p
        if(p.mission!=null)return p.copy(message="Спочатку заверши завдання.")
        return p.copy(mode=mode,last=now.coerceAtLeast(p.last),message=when(mode){WorldMode.AWAKE->"Прокинувся";WorldMode.SLEEP->"Відпочиває і відновлює енергію";WorldMode.HIBERNATE->"Сплячка · час потреб зупинено"})
    }
    fun buy(p:WorldPet,g:RobotGear):WorldPet{
        val o=shop.first{it.gear==g}
        if(g in p.owned)return p.copy(message="Предмет уже в інвентарі.")
        if(p.level<o.level||p.coins<o.price)return p.copy(message="Недостатній рівень або монети.")
        return p.copy(coins=p.coins-o.price,owned=p.owned+g,message="Придбано: ${g.title}")
    }
    fun equip(p:WorldPet,g:RobotGear):WorldPet{
        if(p.adventure.battle!=null)return p
        if(p.mission!=null||g !in p.owned)return p.copy(message="Спорядження недоступне під час завдання або не куплене.")
        if(p.equipment.equipped(g.slot)==g)return p.copy(equipment=p.equipment.remove(g.slot),message="Спорядження знято")
        GearAccess.reason(p,g)?.let{return p.copy(message=it)}
        return p.copy(equipment=p.equipment.equip(g),message="Спорядження змінено")
    }
    fun buySupply(p:WorldPet,kind:WorldSupply):WorldPet{
        if(p.level<kind.level||p.coins<kind.price||p.stock(kind)+kind.pack>99)return p.copy(message="Не вистачає монет, рівня або місця.")
        return p.copy(coins=p.coins-kind.price,supplies=p.supplies+(kind to p.stock(kind)+kind.pack),message="Придбано: ${kind.title} ×${kind.pack}")
    }
    fun useSupply(p:WorldPet,kind:WorldSupply):WorldPet{
        if(p.adventure.battle!=null)return p
        if(kind.isAmmo||p.mode!=WorldMode.AWAKE||p.mission!=null||p.stock(kind)==0)return p.copy(message="Модуль зараз недоступний.")
        val value=when(kind.category){WorldCategory.ENERGY->p.energy;WorldCategory.REPAIR->p.integrity;else->p.mood}
        if(value==100)return p.copy(message="Показник уже повний.")
        val q=p.copy(supplies=p.supplies+(kind to p.stock(kind)-1),message="Використано: ${kind.title}")
        return when(kind.category){
            WorldCategory.ENERGY->gain(q.copy(energy=(p.energy+kind.amount+WorldGearCatalog.energyBonus(p.equipment)).coerceAtMost(100)),RobotSkill.ENERGY,10,15,0)
            WorldCategory.REPAIR->gain(q.copy(integrity=(p.integrity+kind.amount+WorldGearCatalog.repairBonus(p.equipment)).coerceAtMost(100)),RobotSkill.ENGINEERING,10,15,0)
            else->q.copy(mood=(p.mood+kind.amount).coerceAtMost(100))
        }
    }
    fun supplies(p:WorldPet,kind:Int):WorldPet{
        val stock=when(kind){0->p.cells;1->p.repair;else->p.snacks}
        if(p.coins<25||stock>27)return p.copy(message="Не вистачає монет або запаси повні.")
        val q=p.copy(coins=p.coins-25,message="Запаси поповнено")
        return when(kind){0->q.copy(cells=p.cells+3);1->q.copy(repair=p.repair+3);else->q.copy(snacks=p.snacks+3)}
    }
}
internal object WorldStorage{
    private fun prefs(c:Context)=c.getSharedPreferences("pixubi_world_v1",Context.MODE_PRIVATE)
    fun load(c:Context):WorldPet?{
        val p=prefs(c)
        val s=RobotSeries.entries.firstOrNull{it.name==p.getString("series",null)}?:return null
        val skin=RobotSkin.entries.firstOrNull{it.name==p.getString("skin",null)}?:RobotSkin.BLUE
        val trait=GenesisTrait.entries.firstOrNull{it.name==p.getString("trait",null)}?:GenesisTrait.CALM
        val owned=p.getString("owned","").orEmpty().split(",").mapNotNull{id->RobotGear.entries.firstOrNull{it.name==id}}.toSet()
        val equipment=RobotEquipment(RobotEquipment.decode(p.getString("gear","").orEmpty()).items.filterValues{it in owned})
        val task=WorldTask.entries.firstOrNull{it.name==p.getString("task",null)}
        val mission=task?.let{WorldMission(it,p.getLong("until",0),p.getInt("power",0))}
        val restored=WorldPet(s,skin,p.getString("name",s.title).orEmpty(),trait,
            xp=p.getInt("xp",0).coerceIn(0,RobotProgress.MAX_XP),coins=p.getInt("coins",180).coerceIn(0,999999),
            energy=p.getInt("energy",100).coerceIn(0,100),integrity=p.getInt("integrity",100).coerceIn(1,100),mood=p.getInt("mood",100).coerceIn(0,100),
            cells=p.getInt("cells",4).coerceIn(0,30),repair=p.getInt("repair",3).coerceIn(0,30),snacks=p.getInt("snacks",4).coerceIn(0,30),
            mode=WorldMode.entries.firstOrNull{it.name==p.getString("mode",null)}?:WorldMode.AWAKE,equipment=equipment,owned=owned,
            skills=RobotSkills.decode(p.getString("skills","").orEmpty()),wins=p.getInt("wins",0).coerceAtLeast(0),expeditions=p.getInt("expeditions",0).coerceAtLeast(0),
            last=p.getLong("last",System.currentTimeMillis()),mission=mission,message=p.getString("message","").orEmpty(),
            supplies=WorldSupply.entries.associateWith{kind->p.getInt("supply_"+kind.name,when(kind){WorldSupply.PULSE->12;WorldSupply.ION->4;WorldSupply.ARC->2;else->0}).coerceIn(0,99)},
            adventure=AdventureCodec.decode(p.getString("adventure","").orEmpty()),buildings=BaseBuilding.decode(p.getString("buildings","").orEmpty()),neuralRank=p.getInt("neural_rank",0).coerceIn(0,3))
        val migrated=if(p.contains("buildings"))restored.buildings else buildSet{if(restored.adventure.squad.isNotEmpty()||restored.adventure.barracks>1)add(BaseBuilding.BARRACKS);if(restored.adventure.squad.keys.any{it.requiredBarracks>1})add(BaseBuilding.FOUNDRY)}
        return restored.copy(buildings=migrated,equipment=GearAccess.sanitize(restored))
    }
    fun save(c:Context,p:WorldPet){
        val editor=prefs(c).edit().putString("series",p.series.name).putString("skin",p.skin.name).putString("name",p.name).putString("trait",p.trait.name)
            .putInt("xp",p.xp).putInt("coins",p.coins).putInt("energy",p.energy).putInt("integrity",p.integrity).putInt("mood",p.mood)
            .putInt("cells",p.cells).putInt("repair",p.repair).putInt("snacks",p.snacks).putString("mode",p.mode.name)
            .putString("owned",p.owned.joinToString(","){it.name}).putString("gear",p.equipment.encode()).putString("skills",p.skills.encode())
            .putInt("wins",p.wins).putInt("expeditions",p.expeditions).putLong("last",p.last).putString("task",p.mission?.task?.name)
            .putLong("until",p.mission?.until?:0).putInt("power",p.mission?.power?:0).putString("message",p.message)
        WorldSupply.entries.forEach{editor.putInt("supply_"+it.name,p.stock(it))}
        editor.putString("adventure",AdventureCodec.encode(p.adventure)).putString("buildings",p.buildings.joinToString(","){it.name}).putInt("neural_rank",p.neuralRank)
        editor.apply()
    }
}
