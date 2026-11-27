package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class ReleaseSafetyTest{
    @Test fun corruptBattleDoesNotEraseSectorOrArtifacts(){
        val a=AdventureState(stage=3,pos=GridPoint(1,2),cleared=setOf(17,19),seen=setOf(17,19,33),steps=12,artifacts=setOf(FrontierArtifact.CORE))
        val fields=AdventureCodec.encode(a).split("|").toMutableList()
        fields[5]="damaged battle payload"
        assertEquals(a,AdventureCodec.decode(fields.joinToString("|")))
    }
    @Test fun untouchedSaveRoundTrips(){
        val a=AdventureState(stage=5,artifacts=FrontierArtifact.entries.toSet(),pastCampaignWins=40)
        assertEquals(a,AdventureCodec.decode(AdventureCodec.encode(a)))
    }
}
