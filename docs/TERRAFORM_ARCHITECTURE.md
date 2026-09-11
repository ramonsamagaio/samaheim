# Terraform architecture

`TerrainState` owns deterministic terrain data. `TerraformToolSystem` owns tool costs and mutation rules. `SamaheimTerraformGame` owns rendering, input, inventory deduction, stamina deduction, placement feedback and save orchestration.

The separation is intentional:

- tests can validate terrain math without launching OpenGL;
- resource gating can be tested without a rendered client;
- rendering can consume `DirtyRegion` and touch only changed mesh vertices;
- save data remains sparse and independent from GPU representation;
- future multiplayer code can replicate deterministic terrain operations rather than transmitting whole meshes.

The next architectural step is chunking the heightfield into independently streamed terrain sectors so world size can grow without keeping one giant editable mesh resident.
