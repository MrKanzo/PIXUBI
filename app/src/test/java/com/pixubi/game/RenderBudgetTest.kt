package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test
class RenderBudgetTest{
    @Test fun rasterNeverExceedsMemoryBudget(){
        for((w,h) in listOf(1080 to 1920,4320 to 2800,240 to 320,1 to 8000,0 to 0)){
            val size=RenderBudget.rasterSize(w,h)
            assertTrue(size.width in 1..2048)
            assertTrue(size.height in 1..2048)
            assertTrue(size.width.toLong()*size.height<=RenderBudget.MAX_PIXELS.toLong())
            assertTrue(size.width<=w.coerceAtLeast(1))
            assertTrue(size.height<=h.coerceAtLeast(1))
        }
    }
    @Test fun animationTimeIndependentOfRefreshRate(){
        val full=RenderBudget.phase(0f,500_000_000L,1000)
        var partial=0f
        repeat(5){partial=RenderBudget.phase(partial,100_000_000L,1000)}
        assertEquals(full,partial,.0001f)
        assertEquals(.2f,RenderBudget.phase(.9f,300_000_000L,1000),.0001f)
    }
}
