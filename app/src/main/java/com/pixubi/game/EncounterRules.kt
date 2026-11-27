package com.pixubi.game

internal enum class EncounterMood(val title:String){ HOSTILE("Агресивний"), CAUTIOUS("Обережний"), MERCENARY("Найманець"), PRAGMATIC("Прагматик") }
internal enum class EncounterOffer { FIGHT, TOLL, JOIN }
internal data class EncounterDeal(val at:GridPoint,val enemies:List<ArenaBot>,val mood:EncounterMood,val ratio:Float,val offer:EncounterOffer,val price:Int,val recruit:SquadKind?,val message:String)

internal object EncounterRules{
    fun approach(p:WorldPet,to:GridPoint,now:Long):WorldPet{
        val a=p.adventure
        if(!AdventureRules.canExplore(p)||!AdventureRules.inMap(to)||a.pos.distance(to)!=1)return p
        if(!FrontierSites.hostile(AdventureRules.tile(a,to)))return AdventureRules.move(p,to,now)
        return p.copy(adventure=a.copy(contact=to,seen=AdventureRules.reveal(a,a.pos)),message="Встановлено контакт · вхідний сигнал",last=now)
    }
    private fun strength(hp:Int,attack:Int,defense:Int)=hp.coerceAtLeast(0)*(attack.coerceAtLeast(1)+defense.coerceAtLeast(0)*.45f+12f)
    fun assess(p:WorldPet):EncounterDeal?{
        val a=p.adventure;val at=a.contact?:return null
        if(a.battle!=null||a.pos.distance(at)!=1||!FrontierSites.hostile(AdventureRules.tile(a,at)))return null
        val b=AdventureRules.begin(p,at)
        val own=strength(b.hp,p.stats.attack,p.stats.defense)+b.allies.sumOf{strength(it.hp,it.attack,it.defense).toDouble()}.toFloat()
        val enemy=b.enemies.sumOf{strength(it.hp,it.attack,it.defense).toDouble()}.toFloat().coerceAtLeast(1f)
        val ratio=own/enemy
        val fixed=FrontierSites.isBoss(a,at)||FrontierSites.isVault(a,at)
        val mood=if(fixed)EncounterMood.HOSTILE else EncounterMood.entries[(a.stage*17+at.x*7+at.y*13)%EncounterMood.entries.size]
        val first=b.enemies.first()
        // Only already implemented battlefield classes can join; never substitute an unrelated species.
        val recruit=SquadKind.entries.firstOrNull{it.series==first.series&&it.requiredBarracks<=a.barracks&&SquadResearch.unlocked(a,it)&&
            SquadRules.stackCount(a,it)<SquadRules.MAX_STACK&&(it in a.squad||a.squad.size<SquadRules.slots(a))}
        val threshold=when(mood){EncounterMood.CAUTIOUS->1.45f;EncounterMood.MERCENARY->1.8f;EncounterMood.PRAGMATIC->2.2f;EncounterMood.HOSTILE->Float.MAX_VALUE}
        val offer=when{
            fixed||mood==EncounterMood.HOSTILE->EncounterOffer.FIGHT
            recruit!=null&&ratio>=threshold->EncounterOffer.JOIN
            mood==EncounterMood.MERCENARY||mood==EncounterMood.PRAGMATIC||ratio>=.85f->EncounterOffer.TOLL
            else->EncounterOffer.FIGHT
        }
        val price=(25+a.stage*8+b.enemies.size*15+(if(ratio<1f)20 else 0)).coerceAtMost(1500)
        val message=when(offer){
            EncounterOffer.JOIN->"Сканування завершено. Твій загін сильніший. Я здаюся й готовий служити тобі."
            EncounterOffer.TOLL->if(ratio>=1f)"Нам не обов’язково знищувати одне одного. Сплати за прохід — і я відступлю." else "Твій загін слабший. Сплати за прохід, і я дозволю рухатися далі."
            EncounterOffer.FIGHT->if(fixed)"Зона під охороною. Протокол відступу заблоковано." else "Ти увійшов на мою територію. Готуйся до бою!"
        }
        return EncounterDeal(at,b.enemies,mood,ratio,offer,price,recruit.takeIf{offer==EncounterOffer.JOIN},message)
    }
    fun leave(p:WorldPet)=if(p.adventure.contact==null)p else p.copy(adventure=p.adventure.copy(contact=null),message="Контакт перервано. Ворог залишився на карті.")
    fun fight(p:WorldPet,now:Long):WorldPet{
        val d=assess(p)?:return p
        if(p.energy<AdventureBalance.BATTLE_ENERGY||p.integrity<AdventureBalance.MIN_INTEGRITY||p.mode!=WorldMode.AWAKE||p.mission!=null)return p
        return AdventureRules.move(p.copy(adventure=p.adventure.copy(contact=null)),d.at,now)
    }
    fun accept(p:WorldPet,now:Long):WorldPet{
        val d=assess(p)?:return p
        if(p.mode!=WorldMode.AWAKE||p.mission!=null)return p
        val a=p.adventure
        if(d.offer==EncounterOffer.FIGHT||d.offer==EncounterOffer.TOLL&&p.coins<d.price)return p
        val recruit=d.recruit
        val key=AdventureRules.id(d.at)
        val roster=if(recruit==null)a.squad else a.squad+(recruit to (a.squad[recruit]?:0)+SquadRules.unitHp(recruit,SquadRules.rank(a,recruit)))
        val resolved=a.copy(contact=null,pos=d.at,seen=AdventureRules.reveal(a,d.at),cleared=a.cleared+key,peaceful=a.peaceful+key,squad=roster,licensed=a.licensed+listOfNotNull(recruit))
        val complete=!a.credited&&AdventureRules.complete(resolved)
        return p.copy(coins=p.coins-if(d.offer==EncounterOffer.TOLL)d.price else 0,adventure=resolved.copy(credited=a.credited||complete),expeditions=p.expeditions+if(complete)1 else 0,last=now,message=if(recruit!=null)"${recruit.title} приєднався до загону. +1 боєць" else "Прохід відкрито · −${d.price} монет")
    }
}
