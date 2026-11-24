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
40 PNG atlases are embedded directly in app/src/main/assets/imported_v2/ alongside catalog.json. The workflow verifies SHA-256, dimensions, and 180 cell references before compiling. Run gradle testDebugUnitTest assembleDebug. Debug installs alongside stable. Do not publish a stable release yet.

## Next pass: native eye animation
- 40 neutral portraits now have individual eye regions, with five eyelid positions using the existing local-pixel reconstruction. Original atlases and alpha remain untouched. This does not claim blink support for every authored action/emotion pose.
- Two bounded caches prevent repeated decoding of atlas and blink frames. Added JUnit tests for alpha invariance and changed-pixel bounds, plus a real 40-form contact sheet. Visual approval on device remains pending.

## Procedural expression preview
- Individually measured mouth regions for 40 forms. Authored frames remain preferred; missing happy/sad/surprise/tired and sleep poses plus eat/drink face cycles use local native pixel reconstruction. No new eyes or external emoji overlay. These are candidate frames, not finished illustrated full-body actions.
- Existing jump poses now use neutral at start/end instead of a continuous held jump. Other missing locomotion remains explicitly unsupported.
- Lazy bounded Android caches avoid JVM test initialization. Added contact sheet and tests for alpha and edit bounds. Visual review pending.
