package com.pixubi.game
import org.junit.Assert.*
import org.junit.Test

class GuideProgressTest{
    @Test fun wrongAnswersDoNotAdvanceAndCorrectAnswersAreIdempotent(){
        val p=GuideProgress()
        val lesson=GuideLesson.CARE
        assertEquals(p,p.answer(lesson,(lesson.correct+1)%lesson.answers.size))
        val next=p.answer(lesson,lesson.correct)
        assertEquals(setOf(lesson),next.completed)
        assertEquals(GuideLesson.MISSIONS,next.next)
        assertEquals(next,next.answer(lesson,lesson.correct))
    }
    @Test fun allLessonsCanBeCompletedAndRevisited(){
        var p=GuideProgress()
        GuideLesson.entries.forEach{p=p.answer(it,it.correct)}
        assertTrue(p.finished)
        assertNull(p.next)
        assertEquals(7,p.completed.size)
        assertTrue(p.copy(dismissed=true).finished)
    }
    @Test fun guideSaveIsTolerantOfUnknownAndDuplicateLessons(){
        val p=GuideStore.decode("CARE,CARE,MAP,UNKNOWN",true)
        assertEquals(setOf(GuideLesson.CARE,GuideLesson.MAP),p.completed)
        assertTrue(p.dismissed)
        assertEquals(GuideLesson.MISSIONS,p.next)
    }
    @Test fun lessonsHaveValidAnswersAndDestinations(){
        GuideLesson.entries.forEach{
            assertTrue(it.correct in it.answers.indices)
            assertTrue(it.description.isNotBlank())
            assertTrue(it.page in setOf("home","missions","shop","adventure","stats"))
        }
    }
}
