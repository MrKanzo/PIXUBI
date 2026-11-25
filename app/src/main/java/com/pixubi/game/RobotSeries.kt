package com.pixubi.game

internal enum class RobotSeries(val title:String) {
    CLASSIC("Хлопчик"), COMPANION("Дівчинка"), KAGE("Ніндзя"), GLITCH("Кібер"), VALKYRIE("Валькірія"), NEXUS("ШІ · NEXUS")
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

internal data class KageDesign(val title:String,val scale:Float,val armor:Float,val scarfLength:Float,val headScale:Float,val legStretch:Float,val armStretch:Float)
internal object KageCatalog {
    fun of(tier:RobotTier):KageDesign=when(tier) {
        RobotTier.MK1->KageDesign("Учень",.78f,0f,34f,.85f,1.28f,1.08f)
        RobotTier.MK2->KageDesign("Розвідник",.87f,1f,48f,.81f,1.42f,1.14f)
        RobotTier.MK3->KageDesign("Тінь",.95f,2f,63f,.77f,1.57f,1.20f)
        RobotTier.MK4->KageDesign("Майстер тіней",1f,3f,78f,.73f,1.72f,1.26f)
        RobotTier.OMEGA->KageDesign("KAGE · OMEGA",1.04f,4f,90f,.73f,1.72f,1.26f)
    }
}

internal object KageRig {
    fun ground(tier:RobotTier)=133f+38f*KageCatalog.of(tier).legStretch+9f
    fun knee(x:Float,y:Float,angle:Float,tier:RobotTier):androidx.compose.ui.geometry.Offset {
        val t=angle*kotlin.math.PI.toFloat()/180f
        val l=18f*KageCatalog.of(tier).legStretch
        return androidx.compose.ui.geometry.Offset(x-l*kotlin.math.sin(t),y+l*kotlin.math.cos(t))
    }
    fun dashDrop(p:KageDashPose,tier:RobotTier,side:Boolean):Float {
        if(p.drop==0f)return 0f
        val k=KageCatalog.of(tier).legStretch
        fun extent(thigh:Float,shin:Float):Float {
            val t=thigh*kotlin.math.PI.toFloat()/180f
            val a=shin*kotlin.math.PI.toFloat()/180f
            return 18f*k*kotlin.math.cos(t)+20f*k*kotlin.math.cos(a)+9f
        }
        val near=extent(if(side)p.nearThigh else p.nearThigh*.35f,if(side)p.nearShin else p.nearShin*.35f)
        val far=extent(if(side)p.farThigh else p.farThigh*.35f,if(side)p.farShin else p.farShin*.75f)
        return (38f*k+9f-kotlin.math.max(near,far)).coerceIn(0f,p.drop*k)
    }
}
