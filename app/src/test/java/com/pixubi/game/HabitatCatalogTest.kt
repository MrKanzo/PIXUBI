package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class HabitatCatalogTest{
    @Test fun allSeriesHaveUniqueHomes(){
        val homes=RobotSeries.entries.map(HabitatCatalog::of)
        assertEquals(8,homes.size)
        assertEquals(homes.size,homes.map{it.title}.distinct().size)
        assertTrue(homes.all{it.description.isNotBlank()})
    }
}
