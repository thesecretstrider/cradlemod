# Goldsign Overlay Rendering — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Render path-specific goldsign texture overlays on the player model at Gold+ stage, visible to all players.

**Architecture:** Custom `RenderLayer` registered via Fabric's `LivingEntityFeatureRendererRegistrationCallback`. Local player goldsign comes from existing `ClientCradleData`. Remote players' goldsigns synced via a new broadcast payload sent on goldsign change. First goldsign texture: Stellar Spear metallic hair.

**Tech Stack:** Fabric API 0.139.4+, Minecraft 1.21.11, Java 21, official Mojang mappings

---

### Task 1: Create GoldsignFeatureRenderer

**Files:**
- Create: `src/client/java/com/cradle/mod/render/GoldsignFeatureRenderer.java`

**Step 1: Create the render directory**

```bash
mkdir -p src/client/java/com/cradle/mod/render
```

**Step 2: Write GoldsignFeatureRenderer**

This is a `RenderLayer` that renders a goldsign overlay texture on top of any player who has one.

```java
package com.cradle.mod.render;

import com.cradle.mod.ClientCradleData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class GoldsignFeatureRenderer extends RenderLayer<PlayerRenderState, PlayerModel> {

    private static final Map<Integer, ResourceLocation> GOLDSIGN_TEXTURES = new HashMap<>();

    static {
        // Ordinal 3 = SPEAR_LIGHT (Stellar Spear metallic hair)
        GOLDSIGN_TEXTURES.put(3, ResourceLocation.fromNamespaceAndPath("cradlemod",
                "textures/entity/goldsigns/stellar_spear.png"));
        // Future: add other overlay-based goldsigns here
    }

    public GoldsignFeatureRenderer(RenderLayerParent<PlayerRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       PlayerRenderState state, float yRot, float xRot) {
        int goldsignOrdinal = ClientCradleData.getGoldsignForPlayer(state);
        if (goldsignOrdinal == 0) return; // NONE

        ResourceLocation texture = GOLDSIGN_TEXTURES.get(goldsignOrdinal);
        if (texture == null) return; // This goldsign doesn't have an overlay texture

        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(texture));
        this.getParentModel().renderToBuffer(poseStack, vertexConsumer, packedLight,
                getOverlayCoords(state, 0.0F), -1);
    }
}
```

> **Note:** The exact method signatures for `render()`, `getOverlayCoords()`, and `renderToBuffer()` may differ in 1.21.11 Mojang mappings. Check `RenderLayer` and `PlayerModel` source in the decompiled Minecraft classes. The pattern matches existing renderers like `RemnantRenderer`. Adjust parameter types if the compiler complains — the key concept is: get buffer with translucent render type, render parent model with that buffer.

**Step 3: Verify it compiles (won't render yet — not registered)**

```bash
./gradlew build
```

Expected: BUILD SUCCESS (the class exists but isn't wired up yet)

**Step 4: Commit**

```bash
git add src/client/java/com/cradle/mod/render/GoldsignFeatureRenderer.java
git commit -m "feat: add GoldsignFeatureRenderer skeleton for player overlay rendering"
```

---

### Task 2: Add Remote Player Goldsign Tracking to ClientCradleData

**Files:**
- Modify: `src/client/java/com/cradle/mod/ClientCradleData.java`

The local player's goldsign already arrives via `CradleSyncPayload` (line 34: `goldsignOrdinal`). We need a map for other players' goldsigns so the renderer can look them up.

**Step 1: Add the UUID map and lookup method**

Add these fields and methods to `ClientCradleData`:

```java
// Near the top with other static fields (around line 34)
private static final HashMap<UUID, Integer> remotePlayerGoldsigns = new HashMap<>();

// New method: called by GoldsignFeatureRenderer
public static int getGoldsignForPlayer(PlayerRenderState state) {
    // For local player, use the existing goldsignOrdinal
    Minecraft mc = Minecraft.getInstance();
    if (mc.player != null && state.id == mc.player.getId()) {
        return goldsignOrdinal;
    }
    // For remote players, check the map
    UUID uuid = getUUIDFromRenderState(state);
    if (uuid != null) {
        return remotePlayerGoldsigns.getOrDefault(uuid, 0);
    }
    return 0;
}

// Helper to resolve UUID from entity ID in render state
private static UUID getUUIDFromRenderState(PlayerRenderState state) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return null;
    net.minecraft.world.entity.Entity entity = mc.level.getEntity(state.id);
    return entity != null ? entity.getUUID() : null;
}

// Called when we receive a goldsign broadcast for a remote player
public static void setRemotePlayerGoldsign(UUID playerUuid, int goldsignOrd) {
    if (goldsignOrd == 0) {
        remotePlayerGoldsigns.remove(playerUuid);
    } else {
        remotePlayerGoldsigns.put(playerUuid, goldsignOrd);
    }
}

// Call on disconnect to clear stale data
public static void clearRemoteGoldsigns() {
    remotePlayerGoldsigns.clear();
}
```

> **Note:** `PlayerRenderState` may not have an `id` field in 1.21.11. Check the decompiled class — it might use `entityId`, `getId()`, or you may need to extract it differently. The key concept: look up the entity in `mc.level` by its integer ID, get the UUID, then query the map.

**Step 2: Verify it compiles**

```bash
./gradlew build
```

**Step 3: Commit**

```bash
git add src/client/java/com/cradle/mod/ClientCradleData.java
git commit -m "feat: add remote player goldsign tracking to ClientCradleData"
```

---

### Task 3: Create GoldsignBroadcastPayload (S2C)

**Files:**
- Create: `src/main/java/com/cradle/mod/network/GoldsignBroadcastPayload.java`

This payload is sent to all nearby players whenever someone's goldsign changes (not every tick — goldsigns change rarely).

**Step 1: Create the payload record**

Follow the existing pattern from `CradleSyncPayload` and other payloads in `src/main/java/com/cradle/mod/network/`.

```java
package com.cradle.mod.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record GoldsignBroadcastPayload(UUID playerUuid, int goldsignOrdinal) implements CustomPacketPayload {

    public static final Type<GoldsignBroadcastPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cradlemod", "goldsign_broadcast"));

    public static final StreamCodec<FriendlyByteBuf, GoldsignBroadcastPayload> STREAM_CODEC =
            StreamCodec.composite(
                    StreamCodec.of(
                        (buf, uuid) -> { buf.writeLong(uuid.getMostSignificantBits()); buf.writeLong(uuid.getLeastSignificantBits()); },
                        buf -> new UUID(buf.readLong(), buf.readLong())
                    ), GoldsignBroadcastPayload::playerUuid,
                    StreamCodec.of(FriendlyByteBuf::writeVarInt, FriendlyByteBuf::readVarInt),
                    GoldsignBroadcastPayload::goldsignOrdinal,
                    GoldsignBroadcastPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
```

> **Note:** The UUID StreamCodec may need adjustment depending on what's available in 1.21.11 Fabric API. Check existing payloads in `network/` for the exact `StreamCodec.composite` pattern used. Some versions have `UUIDUtil.STREAM_CODEC` or `ByteBufCodecs.UUID` available — use that if it exists.

**Step 2: Register the payload in CradleMod.java (server side)**

In `CradleMod.onInitialize()`, find where other S2C payloads are registered (search for `PayloadTypeRegistry.playS2C()`) and add:

```java
PayloadTypeRegistry.playS2C().register(GoldsignBroadcastPayload.TYPE, GoldsignBroadcastPayload.STREAM_CODEC);
```

**Step 3: Verify it compiles**

```bash
./gradlew build
```

**Step 4: Commit**

```bash
git add src/main/java/com/cradle/mod/network/GoldsignBroadcastPayload.java
git add src/main/java/com/cradle/mod/CradleMod.java
git commit -m "feat: add GoldsignBroadcastPayload for multiplayer goldsign sync"
```

---

### Task 4: Send Goldsign Broadcasts from Server

**Files:**
- Modify: `src/main/java/com/cradle/mod/CradlePlayerData.java` (goldsign setter)
- Modify: `src/main/java/com/cradle/mod/CradleMod.java` or `CyclingManager.java` (broadcast logic)

Broadcast the goldsign to all nearby players whenever it changes.

**Step 1: Add broadcast on goldsign change**

Find where `setGoldsign()` is called in `CradlePlayerData` (or where the goldsign is assigned during remnant absorption in `RemnantEntity.java`). After the goldsign is set, broadcast to all players in the same level:

```java
// After setting goldsign on a player:
private static void broadcastGoldsign(ServerPlayer player, CradlePlayerData data) {
    GoldsignBroadcastPayload payload = new GoldsignBroadcastPayload(
            player.getUUID(), data.getGoldsign().ordinal());
    for (ServerPlayer other : player.serverLevel().players()) {
        ServerPlayNetworking.send(other, payload);
    }
}
```

**Step 2: Also broadcast on player join**

When a player joins, they need to receive goldsign data for all other players who already have goldsigns. Find the player join handler (likely in `CradleMod` — search for `ServerPlayConnectionEvents.JOIN` or similar) and send:

```java
// On player join: send them everyone else's goldsigns
for (Map.Entry<UUID, CradlePlayerData> entry : playerDataMap.entrySet()) {
    if (entry.getValue().getGoldsign() != CradlePlayerData.Goldsign.NONE) {
        ServerPlayNetworking.send(joiningPlayer, new GoldsignBroadcastPayload(
                entry.getKey(), entry.getValue().getGoldsign().ordinal()));
    }
}
```

**Step 3: Verify it compiles**

```bash
./gradlew build
```

**Step 4: Commit**

```bash
git add -A
git commit -m "feat: broadcast goldsign state to all players on change and join"
```

---

### Task 5: Register Client-Side Receiver and Feature Renderer

**Files:**
- Modify: `src/client/java/com/cradle/mod/CradleModClient.java`

**Step 1: Register the GoldsignBroadcastPayload receiver**

Near the existing `CradleSyncPayload` receiver registration (line ~161):

```java
ClientPlayNetworking.registerGlobalReceiver(GoldsignBroadcastPayload.TYPE,
    (payload, context) -> {
        ClientCradleData.setRemotePlayerGoldsign(
                payload.playerUuid(), payload.goldsignOrdinal());
    }
);
```

**Step 2: Register the GoldsignFeatureRenderer**

Add the Fabric feature renderer registration callback. This should go near the entity renderer registrations (line ~150):

```java
LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
    (entityType, entityRenderer, registrationHelper, context) -> {
        if (entityRenderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer) {
            registrationHelper.register(new GoldsignFeatureRenderer(playerRenderer));
        }
    }
);
```

> **Note:** Check the exact import path for `PlayerRenderer` in 1.21.11 Mojang mappings — it may be `net.minecraft.client.renderer.entity.player.PlayerRenderer` or similar. Also verify `LivingEntityFeatureRendererRegistrationCallback` is in the Fabric API version used — check `fabric.mod.json` for included modules. If not available, use a mixin on `PlayerRenderer` to add the layer in `addLayers()` as a fallback.

**Step 3: Verify it compiles**

```bash
./gradlew build
```

**Step 4: Commit**

```bash
git add src/client/java/com/cradle/mod/CradleModClient.java
git commit -m "feat: register goldsign renderer and broadcast receiver on client"
```

---

### Task 6: In-Game Testing

**Step 1: Launch the client**

```bash
./gradlew runClient
```

**Step 2: Test with /cycle command**

Use the existing debug command to set your goldsign:

```
/cycle goldsign SPEAR_LIGHT
```

> **Note:** Check that the `/cycle` command supports setting goldsign directly. If not, use `/cycle path STELLAR_SPEAR` then `/cycle stage LOWGOLD` to trigger the goldsign assignment. Read `src/main/java/com/cradle/mod/command/` to see available subcommands.

**Step 3: Verify visually**

- You should see metallic silver hair rendered on the player model
- Check in third person (F5) and in first person (should not show — feature renderer only renders on the entity model)
- Check that the overlay sits correctly on the head + hat layer with spiky height

**Step 4: Fix any issues**

Common problems:
- Texture not loading: check `ResourceLocation` path matches actual file location
- Overlay z-fighting: may need slight `poseStack.translate(0, 0, 0.001)` offset
- Wrong model parts rendering: ensure we're only rendering head + hat, not full body
- Render method signature mismatch: adjust to match actual 1.21.11 API

**Step 5: Commit working version**

```bash
git add -A
git commit -m "feat: goldsign overlay rendering working for Stellar Spear"
```

---

### Task 7: Update Roadmap

**Files:**
- Modify: `docs/ROADMAP.md`

**Step 1: Mark goldsign overlay as done, update Needs Textures section**

Move Goldsigns from "Needs Textures" to done. Update to reflect which paths still need art:

```markdown
- [x] **Goldsigns** — ~~Visual cosmetics per path at Lowgold~~ (Overlay system done in Save XX. Stellar Spear metallic hair implemented. Black Flame tail, Endless Sword blade-arms, Cloud Hammer cloud, Hollow King aura still need 3D models/particles)
```

**Step 2: Commit**

```bash
git add docs/ROADMAP.md
git commit -m "docs: update roadmap with goldsign overlay progress"
```
