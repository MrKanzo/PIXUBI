# PIXUBI art integration audit
Base: 84ad961fcef0fed2cd0d1ce49adc031ef36eafe5; previous CI succeeded (run 45455347188). This is source review, not device testing.

- Preserve Species, Sex, GrowthStage, package ID, signing, save keys, collection IDs, economy, XP, offline cap and hibernation. Archive young/teen/adult/ultra maps to BABY/YOUNG/ADULT/ULTRA. EGG/COCOON migrate as before.
- Offline catch-up caps at 720 minutes. Hibernation cancels expeditions. These are existing gameplay choices.
- FramePetSprite shares the art loader with GrowthSprite and PixelPetSprite. Existing 6 original and 14 redesigned pose sets used generated female silhouettes. New independent sex-specific models must not receive those silhouette edits.
- Archive has 40 forms, 25 full five-emotion sets, 15 neutral-only older male forms, 10 partial motion sets. No finished authored blink/eat/drink/run/species-action cycles. Debug preview must not be treated as a final art release.
- Import is debug-only, all-or-nothing, with validation of all 40 form paths and 128px PNG frame dimensions. Unsupported actions display a documented static portrait without fake movement or old art substitution. Developer screen shows missing clips.
- Nearest-neighbor rendering; sizes 128/147/166/192 on the existing 192px frame. Ground pivot preserved. Gene accent shift applied without duplicate sex transformation.
- No changes to saves, notifications, updater, audio, shop, quiz, or collection logic. Device, performance, signing and all 40 visual/action acceptance tests remain pending.

## Test
Upload the original PIXUBI_GameAssets_v2.zip to app/src/main/assets/PIXUBI_GameAssets_v2.zip on this branch. Run gradle testDebugUnitTest assembleDebug. Debug installs alongside stable. Do not publish a stable release yet.
