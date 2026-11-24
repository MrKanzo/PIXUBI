"""Validate planned gait coverage against the original catalog and report actual source."""
import csv
import json
import pathlib
import struct

root = pathlib.Path("app/src/main/assets")
forms = json.loads((root / "imported_v2/catalog.json").read_text())["forms"]
out = pathlib.Path("app/build/imported-preview")
out.mkdir(parents=True, exist_ok=True)
rows = []
expected = set()
for f in forms:
    _, species, sex, stage = f["prefix"].split("/")
    poses = set(f["frames"])
    authored = {"walk_a", "walk_b"} <= poses
    path = root / "imported_v2/candidates" / f"{species}_{sex}_{stage}_gait_v3.png"
    if not authored:
        expected.add(path)
        raw = path.read_bytes()
        assert raw[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", raw[16:24]) == (512,384), path
    else:
        assert not path.exists(), path
    rows.append([f["prefix"],len(poses),"original_2_pose" if authored else "developer_candidate_12_cells",
                 "original" if {"happy","sad","surprised","tired"} <= poses else "procedural",
                 "original_lying_with_breathing" if "sleep" in poses else "constructed_rest_candidate" if sex=="male" and stage=="young" else "procedural_eyes_only",
                 "original_pose" if "jump" in poses else "missing",
                 "procedural_neutral", "procedural_mouth", "pending"])
assert len(rows)==40 and len(expected)==30
assert set((root/"imported_v2/candidates").glob("*_gait_v3.png"))==expected
with (out/"animation-coverage.csv").open("w",newline="",encoding="utf-8") as fp:
    w=csv.writer(fp)
    w.writerow(["form","original_cells","walk","emotion","sleep","jump","blink","eat_drink","device_visual_review"])
    w.writerows(rows)
print("40 forms: 10 original 2-pose walk sets, 30 developer candidates; device review pending")
