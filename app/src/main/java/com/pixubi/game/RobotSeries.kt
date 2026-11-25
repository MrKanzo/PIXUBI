package com.pixubi.game

internal enum class RobotSeries(val title:String) {
    CLASSIC("Хлопчик"), COMPANION("Дівчинка"), KAGE("Ніндзя")
}
internal data class CompanionDesign(val title:String,val scale:Float,val hairLength:Float,val armor:Float)
internal object CompanionCatalog {
    fun of(tier:RobotTier):CompanionDesign=when(tier) {
        RobotTier.MK1->CompanionDesign("Початкова",.82f,0f,0f)
        RobotTier.MK2->CompanionDesign("Мандрівниця",.91f,28f,1f)
        RobotTier.MK3->CompanionDesign("Майстриня",.97f,48f,2f)
        RobotTier.MK4->CompanionDesign("Ультра",1f,66f,3f)
        RobotTier.OMEGA->CompanionDesign("OMEGA",1.03f,78f,4f)
    }
}

internal data class KageDesign(val title:String,val scale:Float,val armor:Float,val scarfLength:Float)
internal object KageCatalog {
    fun of(tier:RobotTier):KageDesign=when(tier) {
        RobotTier.MK1->KageDesign("Учень",.78f,0f,34f)
        RobotTier.MK2->KageDesign("Розвідник",.87f,1f,48f)
        RobotTier.MK3->KageDesign("Тінь",.95f,2f,63f)
        RobotTier.MK4->KageDesign("Майстер тіней",1f,3f,78f)
        RobotTier.OMEGA->KageDesign("KAGE · OMEGA",1.04f,4f,90f)
    }
}
