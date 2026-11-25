package com.pixubi.game

// Normalized, single-shot timeline; no XP or resource grants from rendering.
internal data class KageDashPose(
    val travel:Float=0f,val drop:Float=0f,val lean:Float=0f,
    val nearThigh:Float=0f,val nearShin:Float=0f,
    val farThigh:Float=0f,val farShin:Float=0f,
    val nearArm:Float=0f,val farArm:Float=0f,val trail:Float=0f
)
internal object KageDashMotion {
    const val DURATION_MS=1250f
    private val frames=listOf(
        0f to KageDashPose(),
        .30f to KageDashPose(0f,25f,12f,-72f,8f,0f,96f,32f,-42f),
        .43f to KageDashPose(0f,25f,16f,-72f,8f,0f,96f,42f,-48f),
        .58f to KageDashPose(36f,8f,36f,-60f,44f,55f,38f,55f,-70f,1f),
        .72f to KageDashPose(78f,4f,25f,-42f,28f,44f,20f,48f,-55f,.75f),
        .85f to KageDashPose(88f,10f,12f,-25f,10f,28f,10f,20f,-25f,.15f),
        1f to KageDashPose(88f)
    )
    fun pose(phase:Float):KageDashPose {
        val t=phase.coerceIn(0f,1f)
        val i=frames.indexOfFirst{it.first>=t}.coerceAtLeast(0)
        if(i==0)return frames.first().second
        val (ta,a)=frames[i-1];val (tb,b)=frames[i]
        val raw=(t-ta)/(tb-ta)
        val u=raw*raw*(3f-2f*raw)
        fun lerp(x:Float,y:Float)=x+(y-x)*u
        return KageDashPose(lerp(a.travel,b.travel),lerp(a.drop,b.drop),lerp(a.lean,b.lean),
            lerp(a.nearThigh,b.nearThigh),lerp(a.nearShin,b.nearShin),
            lerp(a.farThigh,b.farThigh),lerp(a.farShin,b.farShin),
            lerp(a.nearArm,b.nearArm),lerp(a.farArm,b.farArm),lerp(a.trail,b.trail))
    }
}
