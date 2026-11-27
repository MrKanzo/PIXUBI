# PIXUBI 0.99 — release candidate checklist

## Distribution
- Direct testing: existing signed APK, same package com.pixubi.game.stable and permanent signing key.
- Google Play: `gradle -PpixubiPlay=true bundleRelease`. Uses separate manifest and updater stub, no external APK installer or update network requests, no developer lab button.
- CI uploads AAB as PIXUBI-Google-Play-AAB artifact. Never publish a Play release automatically.
- Before enrolling in Play App Signing, decide how to preserve the existing app signing certificate. The upload key and app signing key may differ. Do not assume existing sideloaded installs can update from Play unless signatures match.
- Verify merged Play manifest, bundle signature, actual installation and upgrade on physical devices.

## Manual acceptance checklist (not yet executed)
- Fresh GENESIS, all 8 series, academy, care, sleep and hibernation.
- Existing signed APK upgrade without losing pet, inventory, army, artifacts or active battle.
- Offline launch, offline missions and exploration; time elapsed during process death and restart.
- Full battle: deployment, each of seven skills, guard, repair, victory, defeat, retreat, reload.
- All four sector biomes, optional sites, boss, artifact bonuses, next sector.
- Screen rotation, background/foreground, small and large displays, Android 8 through current supported Android versions.
- Audio mute/volume, notification permission denial/grant, quiet hours.
- Verify graphics visibility, performance, and battery use on a real device.
- Check actual Play Console requirements, store assets, content rating, data safety, privacy policy and testing eligibility. Release only after acceptance tests pass.
