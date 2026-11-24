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

## Standing gait candidate v2
- The previous four-frame candidate passed CI (45498514159) but retained a seated trunk.
- New candidate shifts the original trunk/head/tail seven pixels upward, extends individually traced limbs, adds a shaded fourth limb derived from the original rear leg, and alternates diagonal support. All four cells retain the native palette and identical upper 80 rows after the vertical offset, with ground contact at row 115. Local validation passed.
- Both candidates remain in the developer review panel with an explicit comparison toggle. The new pose is still a constructed pixel-rig candidate, not approved hand-illustrated locomotion. Joint overlap and gait quality require visual device review. No changes to the world locomotion or saved game.

## Gait v3
- Added a separate 12-cell experimental atlas for male baby Lisyk: native seated pose, two rise intermediates, standing pose, and eight distinct step samples. All cells maintain binary alpha and ground row 115. Walking frames preserve the original raised head/upper body exactly.
- Developer comparison supports v1/v2/v3, with a 10fps sit-rise-walk-sit sequence. This constructed candidate remains review-only. Real gait anatomy and other 29 missing forms are not complete; no release or save changes.

## Gait v3 scene review
- Explicit developer WALK action with v3 selected renders the candidate on the normal scene, normalized to the same 192px cell and ground pivot. Route moves only on eight walking phases, with seated reversals at endpoints and a return to origin. Gene accents are applied as for the original male sprite. No changes to normal gameplay, art catalog completeness, progression, storage or stable release.
- Added route continuity and developer isolation tests. Candidate is still not a completed artist-approved gait; test on Android before expanding to other forms.

## Shared male baby gait review
- Four more 12-cell constructed gait candidates, using each species' own original raster and individually traced limbs. All five male baby forms now share the same explicit developer-only route and image loader. Female and older forms are not changed. The new 48 cells retain the native upper-body pixels during gait and binary alpha. Pishchanyk and Khmaryk each have one frame with a one-pixel ground discrepancy; tolerance is explicit in the test. These are experimental seated-to-standing pixel rigs, not artist-approved walk cycles; no production movement is unlocked. Added species-specific identity and isolation tests.

## Teen male gait review
- Five additional 12-cell candidate atlases, each with individual limb regions and 5px body lift; 60 cells preserve binary alpha, exact original seated pose, unchanged raised upper body during walking and ground row 115. Removed isolated lower fragments of up to five pixels after visual review. Candidates are constructed pixel rigs, not approved new illustrations.
- Shared developer scene now selects stage-specific PNG, uses the normal age-dependent 128/147px size and pivot, and limits older v1/v2 comparison to the original baby Lisyk. Ten male forms have review routes; female and adult/Ultra candidates remain unchanged. No release or save changes.

## Adult male gait review
- Five additional 12-cell atlases constructed from existing adult limbs, not duplicated limbs. All 60 cells have unchanged upper 94 rows, binary alpha and identical ground row 115; eight distinct gait poses per species. The neutral cell is pixel-identical to the original. Lower limb masks and overlap remain experimental and require on-device visual review.
- Shared developer route and age-dependent scaling now cover all 15 male baby, teen and adult forms. No change to normal gameplay, stable release or saved games. Ultra and female forms still require completion.

## Ultra male gait review
- Five more 12-cell atlases constructed by shifting and sampling lower limb regions from each original Ultra raster. Local pixel checks passed: 60 cells, binary alpha, identical upper 94 rows and ground row 115, eight distinct walking frames per species. The neutral cell matches the original. Visual review shows subtle paw movement with a mostly stationary torso; full natural gait is not yet established.
- All 20 male forms now have developer-only gait candidates (240 cells). Age scaling and existing Ultra effect assets remain unchanged. Female gait work, device visual approval and release integration remain open.

## Female teen gait candidates
- Original catalog contains ten female baby/adult authored two-pose walking previews. These remain untouched. The missing female teen and Ultra sets are separate work.
- Five female teen 12-cell candidates (60 cells) now use their own native sprites, three visible limb regions and 3px rise. Exact neutral, upper-body identity after rise, binary alpha, unique walking cells and ground row 115 checked locally. Visual review shows seated-looking body and subtle paw movement; these are review-only, not natural completed gait.
- Shared loader and developer route select species, age and sex. No change to live gameplay or original female artwork. Female Ultra remains missing.

## Female Ultra gait candidates
- Five additional 12-cell atlases built from each female Ultra's three visible limb regions. All 60 cells pass local binary-alpha, ground-row 115, unchanged upper 94 rows and unique walking-cell checks. Neutral cells retain original pixels. An isolated ten-pixel fragment beneath Iskryk's original paws was removed from generated movement cells, not from original art.
- All 30 planned candidate sets now exist (360 cells); ten female baby/adult forms retain their original two-pose previews. These are review assets, not completed natural walking: female Ultra torso remains mostly seated and static. Developer-only access, no production movement unlock or save changes. Device visual review and CI remain required.