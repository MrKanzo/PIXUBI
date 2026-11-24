package com.pixubi.game

/**
 * Four pixel-aligned fin/tail poses built from the original baby illustration.
 * Only the distal gills and tail move. The face, chest and feet stay untouched;
 * no full-body scaling, mesh interpolation or extra eyes.
 */
internal object KraplykSwimFrames {
    const val SIZE = 192
    fun matches(species: Species, stage: GrowthStage, clip: SpriteClip) =
        species == Species.KRAPLYK && stage == GrowthStage.BABY && clip == SpriteClip.SWIM

    private val leftFin = intArrayOf(0, -6, -2, 5)
    private val rightFin = intArrayOf(0, 4, -5, -2)
    private val tail = intArrayOf(0, 6, -5, 3)

    /** Integer column offsets keep the original palette and transparency. */
    fun pose(source: IntArray, frame: Int): IntArray {
        require(source.size == SIZE * SIZE)
        val i = frame.mod(4)
        if (i == 0) return source.copyOf()
        val out = source.copyOf()
        for (x in 0 until SIZE) {
            val gill = when {
                x < 43 -> leftFin[i] * (43 - x) / 43
                x > 149 -> rightFin[i] * (x - 149) / (SIZE - 150)
                else -> 0
            }
            val finTail = if (x > 145) tail[i] * (x - 145) / (SIZE - 146) else 0
            if (gill != 0) shiftColumn(source, out, x, 0, 116, gill)
            if (finTail != 0) shiftColumn(source, out, x, 120, SIZE, finTail)
        }
        return out
    }

    private fun shiftColumn(source: IntArray, target: IntArray, x: Int, start: Int, end: Int, dy: Int) {
        for (y in start until end) target[y * SIZE + x] = 0
        for (y in start until end) {
            val pixel = source[y * SIZE + x]
            val to = y + dy
            if (to in start until end && pixel ushr 24 != 0)
                target[to * SIZE + x] = pixel
        }
    }
}
