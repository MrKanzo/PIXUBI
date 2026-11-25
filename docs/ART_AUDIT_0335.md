# PIXUBI 0.33.5 — Лісик, окремі пози та відновлений архів

## Відновлені джерела
Git commit 41c212dc36efd3898c933ae73527e2cc7edbae8c contained the original 40 sex/stage/species-specific sheets, catalog and old motion candidates. Restored unchanged under art_archive/imported_v2 (outside Android assets; no unused APK payload). Restored animation reference document. Archive young = current BABY, teen = current YOUNG. Archive's forest-themed art differs from the current cyan-tech Lisyk. The old gait and rest candidates were expressly not approved and are not automatically enabled.

## Changes in the actual renderer
- Only Lisyk changed in this iteration; other species stay on their previous paths.
- Preserved original baby and young artwork. Baby sniff alternates the original lowered and standing pose, not a new detailed sniff animation.
- Four new native 96px mature pose sheets (adult/ultra × boy/girl), 24 cells each, with 23+ distinct rasters: gait 0–3, sniff 4–7, hop 8–11, idle/half/closed 12–14, joy/sad/tired/surprise 15–18, eating/drinking 19/20/22/23, curled sleep 21. Each is drawn from explicit pixel outlines and pose geometry, not a modified older raster. The source is reproducible in tools/generate_lisyk_mature.mjs.
- Real separate standing, movement, care, and low sleeping silhouettes. The new art is simpler and less shaded than the old detailed illustration. Girl variants have different ear, eye and body geometry and palette, but their silhouettes remain close; do not claim independently hand-painted art.
- Dedicated active asset routing bypasses native displacement for the mature Lisyk forms. No gender silhouette patch for the new packs. Fixed the double external hop arc for the mature pack and made the developer world blink repeat.
- Added automated atlas, state index, size, transparent edge, sleep posture and distinction tests, and a four-sheet 96-cell CI preview.

## Pending review
These new pose sheets are an implemented first pass, not final art approval. Compare against the restored archive and the existing Lisyk on-device. The existing baby/young girls remain derived; full independent art and richer intermediate frames for these two stages are pending. Other four species were deliberately not changed. The fal plugin upload was blocked by exhausted credits; no paid generation was performed. CI/device validation must be distinguished.
