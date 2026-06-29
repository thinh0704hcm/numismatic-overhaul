# Numismatic Overhaul — MC 26.2 Port Handoff Document

> **For:** Next agent/session continuing this port
> **Date:** 2026-06-28
> **Status:** Compiles GREEN (0 errors). Runtime untested. ~20 stubs need implementing.
> **Branch:** `1.21` | **HEAD:** `52753a4`

---

## Executive Summary

Numismatic Overhaul (Fabric mod for MC 1.21.1) is being ported to Minecraft 26.2 / Fabric 0.152.2 / owo-lib 0.13.

**What's done:**
- All 66 Java files renamed from Yarn to Mojang mappings
- All method-level Yarn→Mojang renames applied
- CCA removed, replaced with file-based persistence
- Build infrastructure (Loom 1.17, Gradle 9.6.1, Java 25, owo-lib 0.13)
- All package paths verified against actual MC 26.2 jar via javap
- Compiles with 0 errors

**What's NOT done (stubs):**
1. 🔴 Villager trade system completely gutted (14 adapters + 3 registry files = all return null)
2. 🔴 Shop block entity renderer is no-op (floating item doesn't render)
3. 🔴 Currency tooltip provider is empty (shows nothing)
4. 🔴 Money bag loot entry not registered (can't appear in chests)
5. 🟡 Piggy bank creative-mode drop loses inventory data
6. 🟡 Config hot-reload broken
7. 🟡 Offline player currency lookup removed
8. 🟡 Shop screen input validation missing
9. 🟢 Coin/money bag trade slot interaction missing

**Estimated remaining work:** ~40% of functional behavior (compilation was 60%).

---

## Quick Start

```bash
cd /home/thinh0704hcm/minecraft-server/numismatic-overhaul
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew compileJava --no-daemon
```

To see original 1.21.1 code for any file:
```bash
git show HEAD:<path/to/file.java>
```

---

## Architecture

### Mod Structure
```
com.glisco.numismaticoverhaul
├── NumismaticOverhaul.java          — Main mod init, registration, gamerules, loot injection
├── NumismaticOverhaulConfigModel.java — owo-sentinel config (auto-generated NumismaticOverhaulConfig)
├── ModComponents.java              — CCA replacement: static file-based currency persistence
├── NumismaticCommand.java          — /currency commands
├── block/
│   ├── ShopBlock.java              — Shop block placement/interaction
│   ├── ShopBlockEntity.java        — Shop inventory, offers, currency storage
│   ├── ShopMerchant.java           — Merchant interface for trade UI
│   ├── ShopScreenHandler.java      — Container menu for shop
│   ├── ShopOffer.java              — Record: sell item + price + optional buyer item
│   ├── PiggyBankBlock.java         — Piggy bank block (breakable by heavy blocks)
│   ├── PiggyBankBlockEntity.java   — Piggy bank inventory (3 slots: bronze/silver/gold)
│   ├── PiggyBankScreenHandler.java — Container menu for piggy bank
│   └── NumismaticOverhaulBlocks.java — Block registration via AutoRegistryContainer
├── item/
│   ├── CoinItem.java               — Bronze/silver/gold coin items
│   ├── MoneyBagItem.java           — Money bag (stores value as DataComponent)
│   ├── MoneyBagComponent.java      — DataComponentType for money bag value
│   ├── CurrencyItem.java           — Base class for currency items
│   ├── CurrencyTooltipProvider.java — Tooltip data for currency (STUBBED)
│   └── NumismaticOverhaulItems.java — Item registration via AutoRegistryContainer
├── currency/
│   ├── Currency.java               — Enum: BRONZE, SILVER, GOLD with exchange rates
│   ├── CurrencyConverter.java      — Value↔item stack conversion
│   ├── CurrencyHelper.java         — Utility for closest trade item, container reading
│   ├── CurrencyComponent.java      — Per-player currency (wraps ModComponents)
│   └── MoneyBagLootEntry.java      — Custom loot entry for money bags (NOT REGISTERED)
├── client/
│   ├── NumismaticOverhaulClient.java — Client init: screens, tooltips, HUD layers
│   ├── ShopBlockEntityRender.java  — Floating item render (STUBBED)
│   └── gui/
│       ├── ShopScreen.java         — Shop UI with tabs, offers, editing
│       ├── PiggyBankScreen.java    — Piggy bank UI
│       ├── PurseLayerElement.java  — HUD purse overlay
│       └── CurrencyTooltipComponent.java — Tooltip rendering (drawText/drawItems STUBBED)
├── network/                        — Packet classes (ported, working)
├── mixin/                          — 7 server mixins + 1 client (ported)
└── villagers/                      — ALL STUBBED
    ├── data/
    │   ├── NumismaticVillagerTradesRegistry.java — Trade registration (EMPTY)
    │   ├── RemappingTradeWrapper.java — Emerald→coin conversion (NO-OP)
    │   └── VillagerTradesResourceListener.java — JSON trade loading (NO-OP)
    └── json/
        ├── TradeJsonAdapter.java          — Abstract adapter (returns Object)
        ├── VillagerTradesHandler.java     — JSON parser (compiles but does nothing)
        ├── VillagerJsonHelper.java        — JSON utilities (working)
        └── adapters/                      — 14 adapters (ALL return null)
```

---

## Implementation Roadmap

### Phase A: Critical — Villager Trade System
**Priority:** 🔴 HIGHEST — Core mod feature
**Difficulty:** HIGH — API fundamentally changed

**The problem:** MC 26.2 removed `VillagerTrades.TRADES` static map, `VillagerTrades.ItemListing` interface, and Fabric's `TradeOfferInternals`/`TradeOfferHelper`. Trades are now data-driven.

**Research needed:**
1. How does MC 26.2 define villager trades? (Data-driven via JSON? Datapacks?)
2. Is there a Fabric API for modifying trades at runtime?
3. Can we mixin into villager trade loading to intercept and modify trades?
4. Is there a `VillagerTradesEvent` or similar in Fabric 26.2?

**Original approach (1.21):**
- `VillagerTradesResourceListener` loaded JSON trade definitions from datapacks
- `NumismaticVillagerTradesRegistry` accumulated trades per profession/level
- `RemappingTradeWrapper.wrap(factory)` intercepted each trade and replaced emerald costs with coin costs
- At load time, modified trades were injected into `TradeOffers.PROFESSION_TO_LEVELED_TRADE.putAll(...)`
- `RemappingTradeWrapper` called `CurrencyHelper.getClosestTradeItem(convertEmeraldsToCoins(emeraldCount))` to swap payment items

**Option paths:**
- **A1:** Use vanilla's data-driven trade system with custom JSON trade definitions
- **A2:** Mixin into `Villager` or `AbstractVillager` to modify offers at spawn time
- **A3:** Use Fabric's `TradeOfferEvents` if available in 26.2 (check Fabric API jar)
- **A4:** Remove feature, keep only shop/piggy bank functionality

**Files to implement (in order):**
1. `TradeJsonAdapter.java` — Change return type from `Object` to a custom functional interface or `MerchantOffer`
2. All 14 adapter files — Reimplement `deserialize()` with MC 26.2 APIs
3. `RemappingTradeWrapper.java` — Reimplement emerald→coin conversion with new `ItemCost` API
4. `NumismaticVillagerTradesRegistry.java` — Reimplement trade accumulation
5. `VillagerTradesResourceListener.java` — Reimplement JSON loading with new reload listener API
6. `VillagerTradesHandler.java` — Update type references

### Phase B: Shop Block Entity Renderer
**Priority:** 🔴 HIGH — Visual feature
**Difficulty:** MEDIUM — API exists, just need correct signature

**File:** `ShopBlockEntityRender.java`

**MC 26.2 API:**
```java
public class ShopBlockEntityRender implements BlockEntityRenderer<ShopBlockEntity, BlockEntityRenderState> {
    public ShopBlockEntityRender(BlockEntityRendererProvider.Context ctx) { ... }
    
    @Override
    public BlockEntityRenderState createRenderState() {
        return new BlockEntityRenderState();
    }
    
    @Override
    public void submit(BlockEntityRenderState state, PoseStack matrices, SubmitNodeCollector nodeCollector, CameraRenderState camera) {
        // Get entity from state (need to figure out how entity is passed)
        // Render spinning item above shop
    }
}
```

**Key challenge:** The `submit` method receives a `BlockEntityRenderState`, not the entity directly. Need to figure out how to get `ShopBlockEntity` from the state. Check if `BlockEntityRenderState` has an entity reference or if we need to store data in the state via `extractRenderState`.

**Original render logic:**
```java
// Spinning item above shop block
matrices.push();
matrices.translate(0.5, isBlockItem ? 0.85 : 0.95, 0.5);
float scale = isBlockItem ? 0.95f : 0.85f;
matrices.scale(scale, scale, scale);
matrices.multiply(Axis.YP.rotationDegrees((float)(System.currentTimeMillis() / 20d % 360d)));
client.getItemRenderer().renderItem(toRender, ItemDisplayContext.GROUND, lightAbove, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, world, 0);
matrices.pop();
```

**Replacement:** Use `Minecraft.getInstance().getItemRenderer().render()` with new `SubmitNodeCollector` API. May need `ItemRenderer.renderStatic()` or equivalent.

### Phase C: Currency Tooltip Provider
**Priority:** 🔴 HIGH — Items show no tooltip info
**Difficulty:** LOW

**File:** `CurrencyTooltipProvider.java` (line 36)

**Fix:**
```java
@Override
public void addToTooltip(Item.TooltipContext context, Consumer<Component> lines, TooltipFlag flag, DataComponentGetter componentGetter) {
    if (value[0] != -1) {
        CurrencyConverter.getAsItemStackList(value).forEach(stack ->
            lines.accept(Component.literal(String.valueOf(stack.getCount())).withStyle(ChatFormatting.GRAY)));
    }
    if (original[0] != -1) {
        lines.accept(Component.literal("Was:").withStyle(ChatFormatting.STRIKETHROUGH, ChatFormatting.GRAY));
        CurrencyConverter.getAsItemStackList(original).forEach(stack ->
            lines.accept(Component.literal(String.valueOf(stack.getCount())).withStyle(ChatFormatting.GRAY)));
    }
}
```

### Phase D: Currency Tooltip Component Rendering
**Priority:** 🟡 MEDIUM — Visual polish
**Difficulty:** MEDIUM — New render API

**File:** `CurrencyTooltipComponent.java`

**Need to implement:**
- `extractText(GuiGraphicsExtractor, Font, int, int)` — Draw text lines
- `extractImage(Font, int, int, int, int, GuiGraphicsExtractor)` — Draw coin item icons

### Phase E: Money Bag Loot Entry Registration
**Priority:** 🟡 MEDIUM — Loot feature
**Difficulty:** HIGH — Loot type unrolling unclear

**File:** `NumismaticOverhaul.java` (line 75, 114)

**Problem:** `LootPoolEntryType` registry removed. Need to figure out how custom loot entries work in 26.2.
**Options:**
- Check if `LootPoolEntry` interface can be used directly
- Use `LootPool.lootPool().add(LootItem.lootTableReference(...))` pattern
- Use `LootTableEvents.MODIFY` to inject regular loot pool entries instead of custom type

### Phase F: Piggy Bank Component Collection
**Priority:** 🟡 MEDIUM — Data preservation
**Difficulty:** LOW

**File:** `PiggyBankBlock.java` (line 138)

**Fix:** When piggy bank breaks in creative mode, transfer inventory contents to dropped item stack using `DataComponents.CONTAINER` or similar.

### Phase G: Config Reload
**Priority:** 🟡 MEDIUM
**Difficulty:** LOW

**File:** `NumismaticOverhaul.java` (line 191)

**Fix:** Uncomment `CONFIG.load()` or use equivalent owo-sentinel method. May need to verify owo-lib 0.13 config API.

### Phase H: Shop Screen Input Validation
**Priority:** 🟢 LOW
**Difficulty:** LOW

**File:** `ShopScreen.java` (line 217)

**Fix:** Add digit-only filter to EditBox. Use `TextFormatter` or equivalent.

### Phase I: Offline Player Support
**Priority:** 🟢 LOW
**Difficulty:** MEDIUM — Need file I/O

**File:** `NumismaticCommand.java`

**Fix:** Read player `.dat` files from world folder to get currency for offline players.

---

## Verified API Reference

### Key classes confirmed in MC 26.2 jar
Use `javap -cp <jar> <class>` to verify any uncertain API:
- Merged jar: `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/minecraft-merged-deobf-26.2.jar`
- owo-lib: `~/.gradle/caches/modules-2/files-2.1/io.wispforest/owo-lib/0.13.0+26.1/`
- Fabric API: via Gradle dependency cache

### Verified owo-lib 0.13 paths
```
io.wispforest.owo.client.screens.MenuUtils
io.wispforest.owo.client.screens.SlotGenerator
io.wispforest.owo.client.screens.SyncedProperty
io.wispforest.owo.client.screens.ValidatingSlot
io.wispforest.owo.client.screens.OwoAbstractContainerMenu
io.wispforest.owo.ui.base.BaseOwoContainerScreen
io.wispforest.owo.ui.container.UIContainers
io.wispforest.owo.ui.core.UIComponent
io.wispforest.owo.ui.core.OwoUIGraphics
io.wispforest.owo.ui.layers.Layer
io.wispforest.owo.ui.layers.Layers
io.wispforest.owo.mixin.ui.access.BaseOwoHandledScreenAccessor
io.wispforest.owo.ops.LootOps
io.wispforest.owo.serialization.CodecUtils
io.wispforest.owo.serialization.RegistriesAttribute
io.wispforest.owo.registration.reflect.AutoRegistryContainer
io.wispforest.owo.registration.reflect.BlockEntityRegistryContainer
io.wispforest.owo.network.OwoNetChannel
io.wispforest.owo.text.TextOps
io.wispforest.owo.config.ConfigSynchronizer
io.wispforest.owo.itemgroup.OwoItemGroup
io.wispforest.owo.itemgroup.Icon
```

### Verified owo-lib 0.13 REMOVED classes
```
io.wispforest.owo.ops.WorldOps              — REMOVED
io.wispforest.owo.ui.core.Component          — Use UIComponent
io.wispforest.owo.ui.core.OwoUIDrawContext   — Use OwoUIGraphics
io.wispforest.owo.config.OfflineDataLookup   — REMOVED
io.wispforest.owo.ui.core.ComponentColor     — REMOVED
```

---

## Known Gotchas

1. **`InteractionResult` is NOT generic.** It's a sealed interface with constants: `SUCCESS`, `PASS`, `FAIL`. No `sidedSuccess()`, no `success(ItemStack)`.

2. **`ValueOutput.store()` takes `Codec<T>`, NOT `Endec<T>`.** Use `CodecUtils.toCodec(endec)` to convert.

3. **`BaseContainerBlockEntity.saveAdditional(ValueOutput)` and `loadAdditional(ValueInput)`** — NOT `CompoundTag`. The `ValueOutput`/`ValueInput` interfaces are the new serialization system.

4. **`CompoundTag` still exists** but `BaseContainerBlockEntity` no longer takes it in save/load. Use `ValueOutput`/`ValueInput` directly. `CompoundTag` may be accessible via `ValueOutput` for legacy compatibility.

5. **`Level.getGameRules()`** only exists on `ServerLevel`/`MinecraftServer`, NOT on `Level`. Client-side code must not call it.

6. **`Entity.spawnAtLocation(ItemStack)`** now requires `ServerLevel` as first param: `spawnAtLocation(ServerLevel, ItemStack)`.

7. **`BlockEntityType` constructor** is `(BlockEntitySupplier, Set<Block>)` — NO Builder. Use `new BlockEntityType<>(MyBlockEntity::new, Set.of(BLOCK1, BLOCK2))`.

8. **`Minecraft.getInstance().gui.screen()`** — screen is accessed via `Gui` method, not a field.

9. **`Screen.extractRenderState`** replaces `Screen.render` — takes `(GuiGraphicsExtractor, int, int, float)`.

10. **`HasClickedOutside`** takes 4 params (no button): `(double, double, int, int)`.

11. **`EditBox`** (vanilla): `getValue()`/`setValue()` NOT `getText()`/`setText()`. Use `setResponder(Consumer<String>)` for change listener, `addFormatter(TextFormatter)` for input filtering.

12. **`Slot.getItem()`** NOT `getStack()`. `Slot.set(ItemStack)` NOT `setStack()`.

13. **`Container.getContainerSize()`** NOT `size()`. `Container.stillValid(Player)` NOT `canPlayerUse()`.

14. **`NonNullList.withSize(int, E)`** NOT `ofSize()`.

15. **`Block.box(...)`** replaces `Block.createCuboidShape(...)`.

16. **`BlockBehaviour.simpleCodec(Function)`** for blocks without complex codecs.

17. **`EntityBlock.newBlockEntity(BlockPos, BlockState)`** NOT `createBlockEntity`.

18. **`BlockBehaviour.affectNeighborsAfterRemoval`** takes `ServerLevel` NOT `Level`.

---

## Regression Checklist

After implementing all stubs, verify:

| Feature | Test |
|---|---|
| Shop block place/open | Place shop, right-click, UI opens |
| Shop offer create/edit/delete | Add offer, edit price, delete |
| Shop item transfer | Insert/remove items from hopper tab |
| Shop currency extract | Extract coins from earned balance |
| Shop floating render | See spinning item above shop |
| Piggy bank place/open | Place piggy bank, right-click, UI opens |
| Piggy bank coin insert | Insert bronze/silver/gold coins |
| Piggy bank break (normal) | Break drops block item |
| Piggy bank break (creative) | Break drops block with inventory |
| Piggy bank heavy block break | Heavy block on piggy bank = break + drops |
| Purse HUD overlay | See coin count on inventory/creative/merchant screens |
| Currency tooltip | Hover over coins/money bags shows values |
| Death currency drop | Player death drops configured % of currency |
| Mob death currency drop | Mob kills drop currency based on config |
| Loot table injection | Gold coins appear in dungeon chests |
| Money bag loot | Money bags appear in structure chests |
| `/currency` commands | Check balance, pay, total |
| Config gamerules | `/gamerule moneyDropPercentage 20` changes behavior |
| Villager trades (if implemented) | Villager trades use coins instead of emerald |
| Shop merchant UI | Open shop as customer, trade works |
| Config hot-reload | Change config, `/reload` applies |
| Piggy bank block state | Directional placement works |

---

## Reference Files

- `FINDINGS.md` — Complete API reference, mapping tables, error analysis
- `.firecrawl/porting-fabric-api.md` — 601-line Fabric API rename list
- `.firecrawl/porting-overview.md` — Build infrastructure changes
- `.firecrawl/fabric-blog-261.md` — Breaking changes blog post
- `.firecrawl/neoforge-primer-261.md` — Vanilla class changes reference (5034 lines)
- `git show HEAD:<filepath>` — Original 1.21.1 code for any file
- `javap -cp <jar> <class>` — Verify API signatures against actual jars
