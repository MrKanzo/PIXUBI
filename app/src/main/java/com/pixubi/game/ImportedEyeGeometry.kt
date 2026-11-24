package com.pixubi.game

/** Eye regions measured on each original 128px neutral frame. The far eye is
 * included only where a separate visible eye can be identified. No old atlas coordinates. */
internal object ImportedEyeGeometry {
    private val coordinates=mapOf(
        Species.LISYK to arrayOf(
            intArrayOf(50,60,10,10,75,60,10,10),
            intArrayOf(70,49,9,9,89,49,5,8),
            intArrayOf(84,42,7,6,102,42,4,6),
            intArrayOf(83,51,6,6,98,51,3,5),
            intArrayOf(82,60,9,9,102,58,4,8),
            intArrayOf(89,46,8,6,106,46,3,5),
            intArrayOf(90,45,7,5),
            intArrayOf(89,45,7,4)
        ),
        Species.ISKRYK to arrayOf(
            intArrayOf(51,60,10,10,77,60,10,10),
            intArrayOf(74,44,9,9,95,44,5,8),
            intArrayOf(85,38,8,7,104,38,4,6),
            intArrayOf(86,49,7,6,105,49,4,6),
            intArrayOf(87,61,9,9,104,61,4,8),
            intArrayOf(91,51,7,6,107,51,3,5),
            intArrayOf(89,49,7,5),
            intArrayOf(92,49,6,5)
        ),
        Species.KRAPLYK to arrayOf(
            intArrayOf(49,59,10,10,76,59,10,10),
            intArrayOf(77,50,9,10,100,50,5,8),
            intArrayOf(78,48,7,6,94,47,4,5),
            intArrayOf(90,45,8,7,108,45,3,5),
            intArrayOf(83,59,10,9,104,58,4,7),
            intArrayOf(90,46,8,7,109,46,3,5),
            intArrayOf(90,42,7,5),
            intArrayOf(87,43,7,4)
        ),
        Species.PISHCHANYK to arrayOf(
            intArrayOf(53,64,9,9,76,64,9,9),
            intArrayOf(80,49,9,9,99,49,4,8),
            intArrayOf(80,46,7,6,98,44,4,5),
            intArrayOf(87,46,7,6,105,46,4,5),
            intArrayOf(85,61,9,9,105,60,4,8),
            intArrayOf(91,50,7,6,107,50,3,5),
            intArrayOf(89,45,7,5),
            intArrayOf(91,45,6,4)
        ),
        Species.KHMARYK to arrayOf(
            intArrayOf(53,54,10,10,81,54,10,10),
            intArrayOf(78,51,9,9,99,51,4,8),
            intArrayOf(77,39,7,6,94,38,4,5),
            intArrayOf(82,46,7,6,99,46,4,5),
            intArrayOf(85,61,10,9,105,59,4,8),
            intArrayOf(94,47,8,7,110,47,3,5),
            intArrayOf(91,41,7,5),
            intArrayOf(88,43,7,5)
        )
    )
    fun regions(s:Species,g:GrowthStage,sex:Sex):List<EyeRegion> {
        val stage=GrowthStage.playable.indexOf(GrowthStage.migrate(g))
        require(stage>=0)
        val values=coordinates.getValue(s)[stage+(if(sex==Sex.GIRL)0 else 4)]
        return values.toList().chunked(4).map{v->
            EyeRegion(v[0].toFloat(),v[1].toFloat(),v[2].toFloat(),v[3].toFloat())
        }
    }
    // The coordinates describe neutral art only, never a separately drawn action pose.
    fun applies(k:PixuFrameKey,pose:String="neutral")=pose=="neutral" && k.emotion==PetEmotion.IDLE && k.closure>0
}
