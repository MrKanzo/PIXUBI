# Modular PIXUBI artwork

The original PNG atlases remain immutable. Visible pixels are partitioned into named layers and equipment sockets and reassembled without changing the original illustration. Hidden surfaces and rear legs are not reconstructed automatically; movement needs actual illustrated poses.

Authored pose path: rig/<species>/<stage>/<sex>/<gene>/<clip>/<00...>/pose.json with full-size transparent PNGs named by layer. A complete clip is required. All frames share a canvas size. Example:

    {"size":192,"layers":["TAIL","BODY","FRONT_LEFT","FRONT_RIGHT","HEAD"],"sockets":[{"slot":"HEAD","parent":"HEAD","x":102,"y":57,"angle":0},{"slot":"NECK","parent":"BODY","x":103,"y":139,"angle":0}]}

Layer and slot identifiers are in PixuRig.kt. Equipment composition accepts its own PNG and local pivot; inventory and saved outfit selection are not implemented yet.

Artist: preserve the original character and paint hidden surfaces; create standing, turning, walking and other genuinely distinct poses; export complete layers; check silhouettes and ground contact. Do not cut and shift a seated paw or stretch the atlas. If a pose is missing, the original portrait stays in place. Search uses opaque cover rather than ghost fades.
