package com.pixubi.game

/** Review-only route. Each 2.4 s leg has 0.4 s rise, 1.6 s gait and
 * 0.4 s settle. Both direction changes occur during the seated pose.
 * World motion is driven by the same clock as the eight gait samples. */
internal object GaitReviewRoute {
    const val LEG_SECONDS=2.4f
    const val DISTANCE_DP=48f
    data class Pose(val frame:Int,val xDp:Float,val left:Boolean,val walking:Boolean)
    fun pose(seconds:Float,initialLeft:Boolean):Pose{
        val tick=if(seconds.isFinite()) (seconds.coerceAtLeast(0f)*10f)%48f else 0f
        val inbound=tick>=24f
        val t=if(inbound)tick-24f else tick
        val i=t.toInt().coerceIn(0,23)
        val frame=when(i){
            0->0;1->1;2->2;3->3
            in 4..19->4+(i-4)%8
            20->3;21->2;22->1;else->0
        }
        val progress=((t-4f)/16f).coerceIn(0f,1f)
        val offset=DISTANCE_DP*(if(inbound)1f-progress else progress)
        return Pose(frame,offset*(if(initialLeft)-1f else 1f),
            if(inbound)!initialLeft else initialLeft,i in 4..19)
    }
    fun nextFrame(seconds:Float):Float {
        if(!seconds.isFinite())return .15f
        val tick=(seconds.coerceIn(0f,47.999f)*10f).toInt()
        return ((tick+1)%480+.5f)/10f
    }
    fun enabled(scene:DevScene?,s:Species,g:GrowthStage,sex:Sex,imported:Boolean)=
        imported&&scene?.gaitPreview==true&&scene.action==DevAction.WALK&&
            ImportedWalkCandidate.available(s,g,sex)
}
