package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class CombatPreviewTest{
    @Test fun previewMatchesActualDamage(){
        val p=WorldRules.hatch(RobotSeries.CLASSIC,"X",1000L)
        val q=AdventureRules.move(p.copy(adventure=AdventureState(pos=GridPoint(3,2))),GridPoint(3,1),1000L)
        val b=q.adventure.battle!!
        assertNull(AdventureRules.previewDamage(q,b,-1,0,null))
        val enemy=b.enemies.first().copy(hp=999,maxHp=999)
        val ready=b.copy(pos=GridPoint(7,2),enemies=listOf(enemy))
        val current=q.copy(adventure=q.adventure.copy(battle=ready))
        val preview=AdventureRules.previewDamage(current,ready,-1,0,null)!!
        val result=AdventureRules.strike(current,0,null,1000L)
        assertEquals(999-preview,result.adventure.battle!!.enemies.first().hp)
        assertNull(AdventureRules.previewDamage(result,result.adventure.battle!!,-1,0,null))
    }
}
