package com.pixubi.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Regression tests for the fully offline economy and elapsed-time simulation. */
class PixubiEconomyTest {
    private val start = 1_795_440_000_000L
    private fun pet() = GameEngine.hatch(
        Species.KRAPLYK, Sex.GIRL, Region.LAKES, "Крапка", start, Random(7)
    )

    private fun walkingPet() = pet().copy(activeMinutes = 15L, xp = 15)

    @Test fun starterSuppliesAreFiniteAndEmptyActionsCannotGrantXp() {
        var p = pet()
        assertEquals(30, p.coins)
        assertEquals(4, p.foodStock)
        repeat(4) { p = GameEngine.act(p, Action.FEED, start + it + 1) }
        assertEquals(0, p.foodStock)
        val before = p.xp
        val failed = GameEngine.act(p, Action.FEED, start + 10)
        assertEquals(before, failed.xp)
        assertEquals(0, failed.foodStock)
    }

    @Test fun shopPurchasesArePersistentInPetAndCannotBeBoughtTwice() {
        val first = pet().copy(coins = 100)
        val house = GameEngine.buy(first, ShopItem.HOME, start + 1)
        assertTrue(house.hasHome)
        assertEquals(45, house.coins)
        val again = GameEngine.buy(house, ShopItem.HOME, start + 2)
        assertEquals(house.coins, again.coins)
        assertTrue(again.hasHome)
        val food = GameEngine.buy(again, ShopItem.FOOD, start + 3)
        assertEquals(7, food.foodStock)
        assertEquals(33, food.coins)
    }

    @Test fun shopNeverChargesForFullStock() {
        val stocked = pet().copy(coins = 100, foodStock = 29)
        val bought = GameEngine.buy(stocked, ShopItem.FOOD, start + 1)
        assertEquals(29, bought.foodStock)
        assertEquals(100, bought.coins)
    }

    @Test fun explorationPaysWhenTimePassesEvenWithoutForegroundTick() {
        val started = GameEngine.act(walkingPet(), Action.EXPLORE, start + 1_000L)
        assertTrue(started.expeditionStartedAt > 0L)
        assertEquals(30, started.coins) // starting an expedition costs energy, not coins
        val returned = GameEngine.advance(started,
            start + 21 * 60_000L + 1_000L)
        assertEquals(38, returned.coins)
        assertEquals(5, returned.foodStock)
        assertEquals(1, returned.expeditionsDone)
        assertEquals(0L, returned.expeditionStartedAt)
        val noRepeat = GameEngine.advance(returned, start + 50 * 60_000L)
        assertEquals(38, noRepeat.coins)
    }

    @Test fun explorationIsLimitedPerDayAndHibernationCancelsActiveSearch() {
        var p = walkingPet()
        repeat(4) { i ->
            val t = start + (i * 40L + 1L) * 60_000L
            p = GameEngine.act(p, Action.EXPLORE, t)
            p = GameEngine.advance(p, t + 21 * 60_000L)
        }
        assertEquals(4, p.expeditionsDone)
        val denied = GameEngine.act(p, Action.EXPLORE, start + 200 * 60_000L)
        assertEquals(0L, denied.expeditionStartedAt)
        val nextDay = GameEngine.act(denied, Action.EXPLORE, start + 86_400_000L + 1000)
        assertTrue(nextDay.expeditionStartedAt > 0)
        val frozen = GameEngine.act(nextDay, Action.HIBERNATE, start + 86_400_000L + 2000)
        val later = GameEngine.advance(frozen, start + 4 * 86_400_000L)
        assertEquals(0L, later.expeditionStartedAt)
        assertEquals(Mode.HIBERNATION, later.mode)
    }

    @Test fun gameCoinsHaveDailyCap() {
        var p = pet()
        repeat(12) { p = GameEngine.act(p, Action.PLAY_WIN, start + it * 1000L) }
        assertEquals(60, p.coins)
        assertEquals(30, p.rewardCoinsToday)
    }

    @Test fun offlineCatchUpIsCappedAndHibernationNeverAges() {
        val muchLater = start + 30 * 60 * 60_000L
        val progressed = GameEngine.restoreAfterAbsence(pet(), muchLater)
        assertEquals(720L, progressed.activeMinutes)
        assertEquals(muchLater, progressed.lastUpdatedAt)
        assertTrue(progressed.alive)
        val frozen = GameEngine.act(pet(), Action.HIBERNATE, start + 1000)
        val resumed = GameEngine.restoreAfterAbsence(frozen, muchLater)
        assertEquals(0L, resumed.activeMinutes)
        assertTrue(resumed.alive)
        assertFalse(resumed.mode == Mode.AWAKE)
    }
}
