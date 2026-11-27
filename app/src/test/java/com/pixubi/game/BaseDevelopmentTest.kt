package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class BaseDevelopmentTest{
    private fun pet(series:RobotSeries=RobotSeries.KAGE)=WorldRules.hatch(series,"Test",1000L).copy(coins=2000)
    @Test fun incompatibleGearCanBeBoughtButNotEquipped(){
        val p=pet().copy(xp=RobotProgress.requiredXp(6))
        val bought=WorldRules.buy(p,RobotGear.SHIELD_L)
        assertTrue(RobotGear.SHIELD_L in bought.owned)
        assertEquals(bought.equipment,WorldRules.equip(bought,RobotGear.SHIELD_L).equipment)
        val both=WorldRules.buy(WorldRules.buy(bought,RobotGear.BLADE_L),RobotGear.BLADE_R)
        val one=WorldRules.equip(both,RobotGear.BLADE_L)
        assertEquals(one.equipment,WorldRules.equip(one,RobotGear.BLADE_R).equipment)
        val advanced=one.copy(xp=RobotProgress.requiredXp(12))
        assertEquals(2,WorldRules.equip(advanced,RobotGear.BLADE_R).equipment.items.size)
    }
    @Test fun buildingsAndBulkRecruitChargeOnlyOnce(){
        val p=pet()
        assertEquals(0,BaseRules.recruitLimit(p,SquadKind.ASSAULT))
        val built=BaseRules.buy(p,BaseBuilding.BARRACKS)
        assertEquals(p.coins-120,built.coins)
        assertEquals(built.coins,BaseRules.buy(built,BaseBuilding.BARRACKS).coins)
        val q=BaseRules.recruit(built,SquadKind.ASSAULT,3)
        assertEquals(3,SquadRules.stackCount(q.adventure,SquadKind.ASSAULT))
        assertEquals(built.coins-225,q.coins)
        assertEquals(q.coins,BaseRules.recruit(q,SquadKind.ASSAULT,3).coins)
    }
    @Test fun neuralRequiresBuildingAndLevel(){
        val p=pet().copy(xp=RobotProgress.requiredXp(2))
        assertEquals(0,BaseRules.buyNeural(p).neuralRank)
        val built=BaseRules.buy(p,BaseBuilding.NEURAL)
        val q=BaseRules.buyNeural(built)
        assertEquals(1,q.neuralRank)
        assertEquals(built.coins-100,q.coins)
        assertEquals(1,BaseRules.buyNeural(q).neuralRank)
        assertEquals(built.buildings,BaseBuilding.decode(built.buildings.joinToString(","){it.name}))
    }
}
