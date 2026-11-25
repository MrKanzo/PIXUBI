# PIXUBI 0.33.4 — model and animation defect log

Evidence: downloaded successful CI 0.33.3 artifact 15012514027 (active-native-forms.png, 168 frames; 14 generated forms, both sexes). Inspected all 28 rows and six original illustrated pose sheets. The prior preview does not cover every emotional pose or the live Android scene.

| Species | Baby | Young | Adult | Ultra |
|---|---|---|---|---|
| Lisyk | Keep authored gait and curled sleep; sniff uses one pose | Keep authored gait and sleep | Tail/leg seams and weak dig | Same seams; crystal tail needs careful motion |
| Iskryk | Keep authored gait and curled sleep; sniff one pose | Over-armored, near frontal, paw seams | Legs/tail seams, dig equals walk | Same; lightning tip needs continuity |
| Kraplyk | Walk/swim authored, sleep seated | Over-armored, upright swim, paw seams | Swim too similar to walk; tail/gill seams | Same; complex fins need clean motion |
| Pishchanyk | Authored dig, sleep seated | Over-armored, dig equals walk | Dig equals walk, tail seams | Same; cloak/tail need continuity |
| Khmaryk | Authored four flight poses, sleep seated | Over-armored, weak wingbeat | Tiny wing motion and upright feet in flight | Same; large wing silhouette vulnerable to cuts |

## Corrected in code
- Generated forms: no destructive rectangular/polygon erase and paste. Continuous local inverse displacement sampling keeps joints attached; no color interpolation.
- Separate action-specific gait, dig crouch, swim tail and tucked-leg pose, flight wing and tucked-leg pose.
- Sleep has closed eyes and a lower body posture with a fixed ground line, not the previous identical standing silhouette. This is a derived crouch, NOT an independently drawn curled pose.
- Juvenile idle and movement use the same orientation; no instant 180-degree flip on action change.
- Matrix now includes swim vertical motion. New 280-frame review sheet covers four gait frames, four species-action frames, blink and sleep for 28 generated variants, plus regression assertions.

## Still unresolved / requires art or device review
- Four younger models have too much armor and face near front. Need genuinely new side-view juvenile pose packs, not further rescaling.
- 14 generated forms still need full independently drawn poses matching Lisyk; no claim that a derived crouch is final sleep art. Kraplyk/Pishchanyk/Khmaryk babies still lack authored curled sleep. Lisyk/Iskryk baby dig/sniff needs independent cycles.
- Male/female art remains derived from one source per form; separate silhouettes are needed. The visual review artifact excludes actual color transformation and live scene effects.
- No device validation or claim of completed visual correction. Review the new CI artifact before further changes.
