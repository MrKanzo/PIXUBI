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

## Local appendage idle preview
- Individual 128px rear appendage regions for 40 forms; four phases with two motion extremes, neutral on phases 0 and 2. Original PNGs unchanged. No whole-sprite translation for idle appendage motion.
- Local vertical pixel displacement tapers at the root and boundaries. This is a procedural preview of tail/wing motion, not authored full flight, swimming, or running. Existing 10 drawn walk sets remain preferred; 30 forms still require drawn locomotion. Tests verify changed pixel bounds and invariant ground row; contact sheet is produced by CI.

## Locomotion candidate pass
- All 40 native portraits have individually bounded alternating paw-lift candidates. Authored walk frames still win. Missing 30 full gait sets are deliberately stationary rather than sliding seated portraits. Fast-step developer option reuses a faster walk cycle, not authored running.
- Kraplyk and Khmaryk now have species-specific procedural tail/wing motion during swimming/flight preview. Pishchanyk uses alternating paws and centered sand concealment. Stationary search does not hide at a distant cover. All preview actions keep gameplay timing and rewards unchanged. Full-body authored locomotion and visual device review still pending.

## Gait timing and Ultra effect pass
- Ten illustrated walk sets retain original poses. Imported walk cadence is slowed to a separate gait phase; walking ground height and horizontal stride oscillation no longer fight the scene trajectory. Hop height and illustrated jump phases share a clock.
- Thirty forms without drawn walking no longer interrupt autonomous idle with stationary fake walking. Paw previews remain available in developer mode.
- All ten Ultra effect atlases from the archive are packed losslessly into one PNG (60 cells). Effects are decorative, behind the pet, during happiness, with a developer test button. No combat, resource or save changes. Shadow is suppressed in hidden and imported fly/swim states.
- CI validates the effect asset and tests motion phase, capability and effect cell mapping. No claim of artist-approved new walking frames for the remaining 30 forms.
## First identity-locked walk candidate
- Magnific draft rejected on visual inspection: duplicated/near-identical poses, ground shadows and grid lines, altered details. No generated draft entered game assets.
- Four individually specified limb poses were made from the original male baby Lisyk raster. Exact upper 94 rows preserved; candidate has transparent background and four 128px cells.
- Separate developer review panel only; the seated-body appearance still needs a proper standing gait and visual approval. No other models were changed, and the 30 missing authored walk cycles remain open. Added asset identity and distinct-frame tests.