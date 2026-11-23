package com.pixubi.game

/** Each eye has independent coordinates and a species/stage-specific lid. */
internal object NativeEyeGeometry {
    fun regions(species: Species, stage: GrowthStage): List<EyeRegion> {
        fun e(x: Int, y: Int, rx: Int, ry: Int, curve: Float = .22f, tilt: Float = 0f) =
            EyeRegion(x.toFloat(),y.toFloat(),rx.toFloat(),ry.toFloat(),curve,tilt)
        return when (stage) {
            GrowthStage.BABY -> when (species) {
                Species.LISYK -> listOf(e(76,102,19,17,.22f,.06f),e(132,102,19,17,.22f,-.06f))
                Species.ISKRYK -> listOf(e(60,106,17,17,.18f,.07f),e(111,106,19,17,.18f,-.07f))
                Species.KRAPLYK -> listOf(e(71,95,17,16,.25f),e(121,95,17,16,.25f))
                Species.PISHCHANYK -> listOf(e(69,110,16,16,.18f,.05f),e(118,109,17,16,.18f,-.05f))
                Species.KHMARYK -> listOf(e(55,96,18,18,.25f,.06f),e(107,95,19,18,.25f,-.06f))
            }
            GrowthStage.YOUNG -> when (species) {
                Species.LISYK -> listOf(e(61,74,12,9,.14f,.20f),e(90,74,12,9,.14f,-.20f))
                Species.ISKRYK -> listOf(e(60,69,12,10,.12f,.18f),e(88,69,12,10,.12f,-.18f))
                Species.KRAPLYK -> listOf(e(39,71,10,9,.15f,.10f),e(77,70,12,10,.15f,-.18f))
                Species.PISHCHANYK -> listOf(e(65,73,12,10,.15f,.16f),e(103,73,12,10,.15f,-.16f))
                Species.KHMARYK -> listOf(e(59,64,12,10,.15f,.18f),e(89,64,12,10,.15f,-.18f))
            }
            GrowthStage.ADULT -> when (species) {
                Species.LISYK -> listOf(e(74,72,12,12,.22f,.05f),e(111,72,12,12,.22f,-.05f))
                Species.ISKRYK -> listOf(e(76,76,13,13,.18f,.07f),e(112,76,13,13,.18f,-.07f))
                Species.KRAPLYK -> listOf(e(62,68,13,12,.24f),e(104,68,13,12,.24f))
                Species.PISHCHANYK -> listOf(e(61,74,12,12,.18f,.06f),e(94,74,12,12,.18f,-.06f))
                Species.KHMARYK -> listOf(e(55,65,15,14,.20f,.08f),e(98,65,15,14,.20f,-.08f))
            }
            else -> emptyList()
        }
    }
}
