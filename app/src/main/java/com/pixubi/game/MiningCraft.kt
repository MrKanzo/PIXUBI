package com.pixubi.game

internal enum class CraftMaterial(val title:String,val code:String){
    IRON("Феросплав","Fe"),COPPER("Мідь","Cu"),SILICON("Кремній","Si"),CRYSTAL("Енергокристал","CR"),TITANIUM("Титан","Ti"),CORE("Нейроядро","NX");
}
internal data class MineralSite(val material:CraftMaterial,val mine:Boolean,val amount:Int){
    val energy get()=if(mine)4 else 2
    val title get()=if(mine)"Шахта · ${material.title}" else "Родовище · ${material.title}"
}
internal object MiningRules{
    private val cache=java.util.concurrent.ConcurrentHashMap<Int,Map<GridPoint,MineralSite>>()
    fun sites(stage:Int):Map<GridPoint,MineralSite> = cache.computeIfAbsent(stage){s->
        val layout=AdventureRules.layout(s)
        val forbidden=layout.walls+layout.encounters+layout.resources.keys+FrontierSites.sites(s).keys+FrontierNetwork.portals(s).keys+FrontierNetwork.gates(s)+AdventureRules.base
        // Three starter deposits and a second group beyond the old map; never replace existing content.
        val early=listOf(GridPoint(1,3),GridPoint(2,4),GridPoint(3,0),GridPoint(0,4),GridPoint(3,4),GridPoint(5,2)).filter{it !in forbidden}.take(3)
        val late=(0 until AdventureRules.MAP_W*AdventureRules.MAP_H).map{GridPoint(it%AdventureRules.MAP_W,it/AdventureRules.MAP_W)}
            .filter{(it.x>=16||it.y>=10)&&it !in forbidden&&it.neighbors().any{q->AdventureRules.inMap(q)&&q !in forbidden}}
            .shuffled(kotlin.random.Random(s*6239+39)).take(7)
        buildMap{
            early.forEachIndexed{i,q->put(q,MineralSite(listOf(CraftMaterial.IRON,CraftMaterial.COPPER,CraftMaterial.SILICON)[i],false,4))}
            late.forEachIndexed{i,q->val k=CraftMaterial.entries[(i+s-1)%CraftMaterial.entries.size];val mine=i%2==0;put(q,MineralSite(k,mine,if(mine)8+s.coerceAtMost(10) else 4+s.coerceAtMost(5)))}
        }
    }
    fun node(a:AdventureState,q:GridPoint)=sites(a.stage)[q]?.takeIf{AdventureRules.id(q) !in a.cleared}
    fun reason(p:WorldPet):String?{
        val node=node(p.adventure,p.adventure.pos)?:return "Немає доступного родовища"
        return when{
            !AdventureRules.canExplore(p)->"Спочатку заверши поточну дію"
            p.energy<node.energy->"Потрібно ${node.energy}% енергії"
            p.material(node.material)+node.amount>999->"Склад заповнений"
            else->null
        }
    }
    fun harvest(p:WorldPet):WorldPet{
        if(reason(p)!=null)return p
        val a=p.adventure;val n=node(a,a.pos)?:return p
        return WorldRules.gain(p.copy(energy=p.energy-n.energy,materials=p.materials+(n.material to p.material(n.material)+n.amount),
            adventure=a.copy(cleared=a.cleared+AdventureRules.id(a.pos)),message="Видобуто: ${n.material.title} ×${n.amount}"),RobotSkill.EXPLORATION,12,4,0)
    }
}
internal enum class CraftRecipe(val title:String,val level:Int,val cost:Map<CraftMaterial,Int>,val gear:RobotGear?=null,val unit:SquadKind?=null){
    BLADE("Плазмовий клинок",2,mapOf(CraftMaterial.IRON to 4,CraftMaterial.COPPER to 4),gear=RobotGear.BLADE_L),
    SHADOW("Шолом SHADOW",4,mapOf(CraftMaterial.IRON to 4,CraftMaterial.SILICON to 4,CraftMaterial.CRYSTAL to 2),gear=RobotGear.HELMET_SHADOW),
    PRISM("Броня PRISM",12,mapOf(CraftMaterial.TITANIUM to 6,CraftMaterial.CRYSTAL to 6,CraftMaterial.CORE to 2),gear=RobotGear.ARMOR_PRISM),
    REPAIR("Модуль ремонту",3,mapOf(CraftMaterial.COPPER to 4,CraftMaterial.SILICON to 4),gear=RobotGear.REPAIR_L),
    ASSAULT("Штурмовик KAGE",3,mapOf(CraftMaterial.IRON to 4,CraftMaterial.SILICON to 3),unit=SquadKind.ASSAULT),
    MEDIC("Ремонтний дрон",5,mapOf(CraftMaterial.COPPER to 5,CraftMaterial.SILICON to 4,CraftMaterial.CRYSTAL to 2),unit=SquadKind.MEDIC),
    AERIAL("Повітряний розвідник",15,mapOf(CraftMaterial.TITANIUM to 8,CraftMaterial.CRYSTAL to 5,CraftMaterial.CORE to 4),unit=SquadKind.AERIAL);
}
internal object CraftRules{
    fun reason(p:WorldPet,r:CraftRecipe):String?{
        if(BaseBuilding.WORKSHOP !in p.buildings)return "Потрібна майстерня"
        if(!BaseRules.available(p))return "Потрібно повернутися на базу"
        val required=maxOf(r.level,r.gear?.let{g->WorldRules.shop.first{it.gear==g}.level}?:1)
        if(p.level<required)return "Рівень $required"
        if(r.gear!=null&&r.gear in p.owned)return "У власності"
        r.unit?.let{k->
            val a=p.adventure
            if(BaseRules.buildingFor(k) !in p.buildings||a.barracks<k.requiredBarracks)return "Потрібні відповідні казарми або завод"
            if(!SquadResearch.unlocked(a,k))return "Технологію ще не відкрито"
            if(SquadRules.stackCount(a,k)>=SquadRules.MAX_STACK||(k !in a.squad&&a.squad.size>=SquadRules.slots(a)))return "Загін заповнений"
        }
        if(r.cost.any{(k,n)->p.material(k)<n})return "Не вистачає матеріалів"
        return null
    }
    fun craft(p:WorldPet,r:CraftRecipe):WorldPet{
        if(reason(p,r)!=null)return p
        val stock=p.materials.toMutableMap()
        r.cost.forEach{(k,n)->val left=p.material(k)-n;if(left==0)stock.remove(k)else stock[k]=left}
        val a=p.adventure
        val k=r.unit
        val next=if(k==null)a else a.copy(squad=a.squad+(k to (a.squad[k]?:0)+SquadRules.unitHp(k,SquadRules.rank(a,k))),licensed=a.licensed+k)
        return p.copy(materials=stock,owned=p.owned+listOfNotNull(r.gear),adventure=next,message="Створено: ${r.title}")
    }
}
