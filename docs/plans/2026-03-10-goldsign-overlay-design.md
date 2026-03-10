# Goldsign Overlay Rendering — Design

> Date: 2026-03-10

---

## Goal

Render path-specific goldsign textures on the player model when they advance to Gold (Lowgold+) via remnant absorption. Visible to all players in multiplayer.

---

## Lore-Accurate Goldsigns

The current enum names and descriptions are placeholders. Lore-accurate goldsigns per path:

| Path | Goldsign | Visual Type | Texture Approach |
|------|----------|-------------|-----------------|
| Black Flame | Dragon tail + black/red eyes when cycling | 3D model + temporary overlay | Future work (needs Blockbench) |
| Endless Sword | Silver sword-arms from shoulder blades | 3D model | Future work (needs Blockbench) |
| Stellar Spear | Rigid metallic silver hair | Texture overlay (head + hat layer) | **Ready now** |
| Cloud Hammer | Overhead floating cloud | Particle effect / entity | Future work |
| Hollow King | No standard goldsign (pure madra) | Subtle particle aura | Future work |

This design implements the **overlay rendering system** and ships with the **Stellar Spear** metallic hair as the first goldsign texture. Other paths will be added as their art is created.

---

## Architecture

### Approach: Custom RenderLayer (Fabric API)

A `GoldsignFeatureRenderer` class extending `RenderLayer<AbstractClientPlayer>` registered via Fabric's `LivingEntityFeatureRendererRegistrationCallback`. This is how Minecraft handles armor, capes, and elytra — the intended extension point for player model overlays.

### Components

#### 1. GoldsignFeatureRenderer (client)

**Location:** `src/client/java/com/cradle/mod/render/GoldsignFeatureRenderer.java`

- Extends `RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>`
- In `render()`: look up the target player's goldsign, skip if NONE
- Resolve the texture path for that goldsign
- Render the overlay using the player model's hat-layer parts (outer layer, slightly larger than skin)
- Uses `RenderType.entityTranslucent()` for future glow/transparency support

#### 2. Texture Lookup

**Location:** Static map in `GoldsignFeatureRenderer` or a small utility class

```
Goldsign.NONE            -> null (skip rendering)
Goldsign.BLACK_FLAME_EYES -> null (future: 3D model, not overlay)
Goldsign.SWORD_ARMS       -> null (future: 3D model, not overlay)
Goldsign.SPEAR_LIGHT      -> "cradlemod:textures/entity/goldsigns/stellar_spear.png"
Goldsign.CRACKLING_SKIN   -> null (future: particle effect)
Goldsign.PALE_AURA        -> null (future: particle effect)
```

#### 3. Multiplayer Goldsign Sync

Currently `CradleSyncPayload` sends goldsign data but `ClientCradleData` only stores it for the local player. For other players' goldsigns to be visible:

- Add a client-side `HashMap<UUID, Integer>` in `ClientCradleData` mapping player UUIDs to goldsign ordinals
- The existing `CradleSyncPayload` already broadcasts per-player — just store the goldsign for each player UUID on receipt
- `GoldsignFeatureRenderer` queries this map by the rendered player's UUID

#### 4. Registration

**Location:** `CradleModClient.java` (client entrypoint)

Register the feature renderer callback:
```java
LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
    if (entityRenderer instanceof PlayerRenderer playerRenderer) {
        registrationHelper.register(new GoldsignFeatureRenderer(playerRenderer, context.getModelSet()));
    }
});
```

### Texture Format

- Standard 64x64 PNG, same UV layout as player skins
- Fully transparent except for the goldsign pixels
- Uses both inner head layer (base hair mass) and outer/hat layer (spike height extensions)
- Per-goldsign choice of inner vs outer layer rendering depending on whether the overlay should sit flush (tattoo-like) or raised (hair-like)

### Texture File

- `src/main/resources/assets/cradlemod/textures/entity/goldsigns/stellar_spear.png` (created)
- Silver metallic palette: #E8E8F0 (highlight), #C0C0CC (base), #9898A8 (mid shadow), #707080 (roots)

---

## Future Extensions

- **Temporary cycling overlays:** Same renderer, toggled by a `isCycling` or `isUsingAbility` flag (e.g., Black Flame glowing eyes only while cycling)
- **3D model goldsigns:** Separate feature renderers that add model parts (tail, sword-arms) instead of texture overlays
- **Goldsign growth animation:** Interpolate overlay alpha or scale from 0 to 1 over a few seconds when first advancing to Gold
- **Emissive rendering:** Switch to `RenderType.eyes()` for glow effect on specific goldsigns

---

## What Changes

| File | Change |
|------|--------|
| `src/client/java/.../render/GoldsignFeatureRenderer.java` | New — overlay render layer |
| `src/client/java/.../CradleModClient.java` | Register feature renderer callback |
| `src/client/java/.../ClientCradleData.java` | Add `UUID -> goldsignOrdinal` map for other players |
| `src/main/resources/.../textures/entity/goldsigns/stellar_spear.png` | Already created |
