# Robot Lab — 0.34.0

First independent MK-1 prototype. Settings > Robot Lab. Front and right profile use distinct geometry; left profile mirrors the right. Art is a layered procedural 2D vector prototype, not an imported raster concept sheet. Near/far limbs have separate rotations; neutral sockets are defined per projection. Demo scanner, shield and tool attach to head or arm; accent color is replaceable. Animation clock can pause, scrub and step. Tests cover motion math and socket mirroring, not visual quality.

Next: approve MK-1 silhouette and gait on device; refine front walking and attack, per-frame attachment transforms and asymmetric equipment handedness. Add independent skin/gear IDs, persistent skill/action-event counters and evolution rules only after approval. MK-2–MK-4, combat mechanics and adaptive skills are intentionally not implemented. No changes to player save format, game logic or existing pet views.

## 0.34.3 Equipment
Six independent slots, stable gear IDs and six accent skins. Front and profile attach limbs and feet inside the existing rotation transforms. Right and left views resolve anatomical handedness separately. Lab-only inventory is unlimited; equipment and skin are persisted in separate preferences. Stats, unlocks, combat, evolution and adaptive skills are not connected. Neutral socket display remains illustrative. Verify appearance with all gear combinations on device.

## 0.35.0 Balance and progression
Seven derived stats, gear trade-offs, independent cosmetic skins, data-driven tier compatibility, capped XP and level 1–30, test-only training with separate persistence. Tier unlock thresholds 5/12/20 are provisional; MK-1 is the only rendered tier. No combat, gameplay events, adaptive skills or movement-rate effects connected yet. Test XP never migrates to the real game. Existing equipment and render geometry remain unchanged.

## 0.36.0 Tamagotchi-first adaptive skills
PIXUBI LAB is an offline virtual robot pet, not primarily a combat RPG. Future minigames live in its pet screen; autonomous offline gathering and optional multiplayer arenas are future systems. Seven independent skill tracks, levels 0–10 and additive stat bonuses. Six explicit test activity events grant skill XP and general XP; battle track remains reserved. Rendering and frame loops never award XP. Separate lab-only skill persistence; no real gameplay event integration, resource spending, offline gathering or multiplayer in this version. All balance values provisional.

## 0.37.0 Chassis evolution preview
MK-1 remains visually unchanged. MK-2 Wanderer, MK-3 Maker and MK-4 Ultra have distinct shell materials, head/body contours, articulated limb plating and tier-specific trim in front and profile. All use the existing rig, equipment transforms and expressions; no new animation poses. Lab tier selection is unrestricted and transient; level requirements are informational, not actual game evolution. Stats follow the previewed chassis. Device visual review is required, especially layered gear and running poses. Main pet screen remains unchanged.

## 0.37.1 Secret OMEGA chassis
A fifth, concept-inspired procedural 2D chassis, with articulated faceted limb armor, crown, reactor and animated rear crystal wings, front and mirrored profile views. Existing shared pivots and equipment slots preserved. Lab preview unrestricted. Level XP alone never unlocks OMEGA; actual achievement requirements, transformation animation, flying and main game integration are deferred. Provisional stats and visual review needed. Earlier four chassis unchanged.

## 0.37.2 Companion series
Five new female companion chassis rendered procedurally from the approved concept: rounded face, heart reactor, ear tips at MK1, growing articulated energy hair, progressive armor, and pink OMEGA wings and halo. Shared limb rotation, gear sockets, animations, stats and lab XP; both series have equal gameplay stats at each tier. Preview series switch and independent saved accent colors; lab inventory/XP remain shared for testing. No main pet screen integration yet. Device visual checks required; generated concept art is not an exact runtime sprite.

## 0.37.3 Companion visual rebuild
Reference-led procedural rebuild of the female head, face bezel, shoulders, chest, hip plating, articulated arms, hands, knees and boots, with layered metal shading and tinted hair tips. Independent limb renderer uses shared action angles and gear IDs. Classic series unchanged. Concept is three-quarter artwork rather than a layered orthographic animation asset; runtime is a hand-built approximation, not a pixel-identical import. Check all tiers and gear combinations on device. Main pet screen remains separate.

## 0.37.4 PX-N7 KAGE
Third series, five preview stages with independent hood/mask, angular eyes, articulated dark metal limbs, hex core, scarf and tier-dependent armor. Default cyan accent stored separately; animated scarf follows shared action phase. Cosmetic sheathed katana from MK2 when back slot is empty, drawn in attack if left hand is free; not a new combat mechanic or inventory item. Existing gear slots, stats, XP and motion shared. Existing Classic and Companion render paths preserved. Concept-inspired vector recreation, not exact artwork; visual checks on device required. No new main-screen integration.

## 0.37.5 KAGE detail and ninja dash
Refined hood, face mask, panel highlights, articulated two-segment legs and armor details. KAGE-only one-shot 1250 ms dash, kneel preparation, forward translation, recovery and speed lines; side direction mirrors with view, front uses depth scale. Frame controls support pause, stepping and replay. This is a lab animation, not movement in the actual pet world. Concept remains a procedural interpretation; visual fit and foot contact need device verification. Other two series are unchanged.

## 0.37.6 KAGE proportion polish
Hood, face and head gear scale about their fixed neck anchor by tier (94%, 91%, 89%, 88%, 88% of previous geometry); neck, mask seams, chest plate edges, shoulder highlights and thigh details refined. KAGE neutral head socket follows scale. Existing dash timeline and limb pivots unchanged. Classic and Companion unchanged. Device visual review still required.

## 0.37.7 KAGE reference silhouette
Reference-led full-body proportion pass: elongated independently articulated thigh/shin segments, longer arms with attached gear, smaller hood, inset mask, luminous hex core, layered hip plates, waist tab and shin accents. Tier-specific ground and knee math; dash lowering adjusted to the longer rig. Earlier series untouched. Still a procedural vector adaptation, not a direct layered image import; review the stance, animation and gear on device.

## 0.37.8 KAGE reference artwork
Actual front/profile crops from the approved KAGE character sheet, transparent local PNG atlas (137x140, reduced palette), MK4 only. Reference artwork is the default when selecting KAGE in Robot Lab; modular mode is available for equipment, other skins and socket inspection. Reference animation uses separately clipped limb polygons, shared action pose and dash timing, approximate visor overlays; requires device review. No new art for other evolution stages, no main pet integration. Existing procedural renderer and loadout persistence retained.

## 0.37.9 Unified KAGE series
All five KAGE forms now use a single new layered vector rig, with distinct stage dimensions and armor packages, a reference-style hood, inset visor, scarf, hip armor, limb plating, and OMEGA energy fins. Modular gear is rendered on its own anchors in every stage; all motions and dash remain accessible. The single cropped MK4 reference mode is removed. This is new hand-authored 2D art inspired by the reference, not a pixel-perfect reproduction. Needs on-device visual and motion review. Classic and Companion unchanged; lab only.

## 0.37.10 KAGE adult-led forms and articulation
MK4 reference-style adult is the base for five separately authored shell designs (hood, torso, shoulders, forearms and leg armor), including compact rounded MK1, light MK2, armored MK3 and angular OMEGA. Independent elbow, wrist, knee and ankle rotations with equipment carried by the corresponding limb. Live debug markers at moving joint pivots. Existing dash timeline retained. Vector interpretation; on-device visual and animation review needed. No changes to other series or pet gameplay.

## 0.37.11 Early KAGE visual identities
MK1 has rounded capsule chest, circular core, round visor eyes, short scarf and rounded shin guards. MK2 uses angular hood, horizontal visor eyes, diagonal chest armor and offset core, straight shin lights and shorter angular scarf. Grades 2–4 retain their existing graphics. Shared articulated joints and equipment remain. Device visual review required.

## 0.37.12 KAGE gait fix
Walk/run automatically use a side view; selecting front returns to idle. Separate cyclical foot stance/swing targets drive two-link leg inverse kinematics, with opposite arm swing, level feet and forward torso lean. Lab locomotion is an in-place gait preview, not world traversal. Applies to all five KAGE forms. Other series and ninja dash rendering retained. Tests cover foot target reconstruction and view selection; on-device review pending.

## 0.37.13 OMEGA head and articulated wings
KAGE OMEGA alone receives a new dark hood with an inset visor, separate profile geometry and no mirrored front horns in profile. Two wing assemblies with six independent overlapping metal/energy feathers per wing and mechanical hubs; wing spread responds to actions, folds at rest and after dash, and expands in jump/attack/dash. Dedicated canvas width for the wing span. Robot Lab respects safe drawing insets. Flight and back view are not added. On-device visual checks pending.

## 0.37.14 OMEGA side silhouette polish
Dedicated side torso, neck, shoulder, forearm, thigh and knee/shin armor replaces compressed frontal plating in OMEGA profile. Slightly smaller, rearward head, closer-to-spine shoulders, more compact hip armor, offset wing anchors and reduced profile scarf overlap. Shared joint transforms, dash and front artwork retained. Other forms unchanged. Device visual inspection pending.

## 0.37.15 OMEGA hood profile
Refined actual side silhouette: forward fabric brow, recessed single-eye visor, aligned jaw and draped rear hood, fabric insignia and adjusted neck anchor. Front artwork and all other models, wings and motion remain unchanged. Device review pending.

## 0.37.16 OMEGA profile finishing
Softer continuous hood rim and cheek drape, inset narrower eye, no hard diagonal fabric division. Aligns projected hip and shoulder pivots in strict side view so idle legs overlap instead of showing double knee joints; separate movement angles retained. Darker far-side shin and foot shading improves depth while moving. Front view, wings and other stages unchanged. Device verification pending.

## 0.38.0 PX-C9 GLITCH
Fourth lab series: BYTE, PROXY, GHOST, GLITCH, OVERLORD. Independent vector shells inspired by the approved purple concept, digital face, articulated limbs, profile gait, equipment and separate saved accent. GHOST and above support hologram and drone preview; GLITCH and OVERLORD support visual teleport; OVERLORD has animated wings and two companion drones. These are visual lab effects, not actual hacking, flight, combat or pet-world mechanics. Existing series artwork retained. Device review required.

## 0.39.0 GENESIS
Separate Robot Lab incubation screen with random hidden or manual series choice, four original MK-0 vessels (cube, orb, pod, seed), randomized saved color and trait, 8% golden chance for mystery mode. Charge, sync and touch; 100% energy plus all care types unlock the hatch animation. Interrupted hatch can be restarted; progress, newborn identity and name persist locally. Adopt previews the selected MK-1 in the lab. No changes to main game save, real-time incubation or actual trait-driven behavior. Test reset requires confirmation.

## 0.39.1 GLITCH polish
Refined MK-4/MK-X hood front/profile, recessed face and eye expression for MK-3 and later, extra articulated wing panel details on OVERLORD. Drone rendering, flight orbit, counts, all action timelines, GENESIS and other series unchanged. Device visual verification pending.
