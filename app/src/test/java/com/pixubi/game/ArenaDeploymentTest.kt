package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ArenaDeploymentTest{
    private fun pet():WorldPet{
        val p=WorldRules.hatch(RobotSeries.CLASSIC,"Test",1000L)
        val a=p.adventure.copy(pos=GridPoint(3,2),squad=mapOf(SquadKind.RANGER to 44,SquadKind.ASSAULT to 65))
        return AdventureRules.move(p.copy(adventure=a),GridPoint(3,1),1000L)
    }
    @Test fun deploymentCannotMoveEnemiesOrConsumeTurns(){
        val p=pet();val b=p.adventure.battle!!
        assertTrue(b.deploying)
        assertEquals(p,AdventureRules.guard(p,1000L))
        assertEquals(p,AdventureRules.deployUnit(p,-1,b.enemies.first().pos))
        val q=AdventureRules.deployUnit(p,-1,b.allies[0].pos)
        assertEquals(b.pos,q.adventure.battle!!.allies[0].pos)
        assertEquals(b.allies[0].pos,q.adventure.battle!!.pos)
        assertEquals(b.enemies,q.adventure.battle!!.enemies)
        assertEquals(b.turn,q.adventure.battle!!.turn)
        assertEquals(q.adventure.battle,AdventureCodec.decode(AdventureCodec.encode(q.adventure)).battle?.copy(log=q.adventure.battle!!.log))
        val started=AdventureRules.startBattle(q)
        assertFalse(started.adventure.battle!!.deploying)
        assertEquals(started,AdventureRules.deployUnit(started,-1,GridPoint(2,1)))
    }
    @Test fun skillConsumesOneActionAndIsPersisted(){
        val p=AdventureRules.startBattle(pet());val b=p.adventure.battle!!
        val ready=b.copy(pos=GridPoint(7,2),enemies=b.enemies.map{it.copy(hp=999,maxHp=999)})
        val q=p.copy(adventure=p.adventure.copy(battle=ready))
        val hit=AdventureRules.skillDamage(q,ready,-1,0,null)!!
        val normal=AdventureRules.previewDamage(q,ready,-1,0,null)!!
        assertTrue(hit>normal)
        val next=AdventureRules.skillStrike(q,-1,0,null,1000L)
        assertEquals(999-hit,next.adventure.battle!!.enemies.first().hp)
        assertTrue(next.adventure.battle!!.enemyPending)
        assertTrue(-1 in next.adventure.battle!!.skillUsed)
        val saved=AdventureCodec.decode(AdventureCodec.encode(next.adventure))
        assertEquals(setOf(-1),saved.battle!!.skillUsed)
        assertNull(AdventureRules.skillDamage(next,saved.battle!!,-1,0,null))
    }
}
