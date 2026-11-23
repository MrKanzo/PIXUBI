package com.pixubi.game

/**
 * Pixel-accurate positions in the source artwork, NOT in scaled screen DP.
 * Adult/child portraits look forward, but teenage sprites turn their heads,
 * and one aquatic eye is partly outside the visible side of the face.
 */
internal object NativeEyeGeometry {
    fun regions(species: Species, stage: GrowthStage): List<EyeRegion> {
        val positions = when (stage) {
            GrowthStage.BABY -> when (species) {
                Species.LISYK -> floatArrayOf(73f, 125f, 102f, 19f, 19f)
                Species.ISKRYK -> floatArrayOf(66f, 127f, 103f, 19f, 19f)
                Species.KRAPLYK -> floatArrayOf(70f, 122f, 107f, 19f, 18f)
                Species.PISHCHANYK -> floatArrayOf(67f, 126f, 107f, 18f, 19f)
                Species.KHMARYK -> floatArrayOf(71f, 127f, 101f, 19f, 19f)
            }
            GrowthStage.YOUNG -> when (species) {
                // These are based on a coordinate-grid inspection of the actual
                // 192px sideways/three-quarter portraits.
                Species.LISYK -> floatArrayOf(61f, 90f, 75f, 11f, 10f)
                Species.ISKRYK -> floatArrayOf(61f, 89f, 69f, 12f, 11f)
                Species.KRAPLYK -> floatArrayOf(39f, 78f, 72f, 11f, 10f)
                Species.PISHCHANYK -> floatArrayOf(65f, 103f, 73f, 12f, 11f)
                Species.KHMARYK -> floatArrayOf(59f, 89f, 64f, 12f, 10f)
            }
            GrowthStage.ADULT -> when (species) {
                Species.LISYK -> floatArrayOf(73f, 111f, 72f, 14f, 15f)
                Species.ISKRYK -> floatArrayOf(71f, 112f, 72f, 14f, 15f)
                Species.KRAPLYK -> floatArrayOf(72f, 111f, 71f, 15f, 15f)
                Species.PISHCHANYK -> floatArrayOf(72f, 112f, 72f, 14f, 16f)
                Species.KHMARYK -> floatArrayOf(70f, 111f, 72f, 15f, 16f)
            }
            else -> return emptyList()
        }
        return listOf(
            EyeRegion(positions[0], positions[2], positions[3], positions[4]),
            EyeRegion(positions[1], positions[2], positions[3], positions[4])
        )
    }
}
