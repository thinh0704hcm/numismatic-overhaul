# Numismatic Overhaul — MC 26.2 Port Findings (Updated)

> **Status:** Compiles green (0 errors). Runtime untested. All stubs identified.
> **Branch:** `1.21` | **HEAD:** `52753a4` | **Diff:** 67 files, +1198/-2508 lines
> **Date:** 2026-06-28

---

## 1. Build Infrastructure (RESOLVED ✅)

### build.gradle
```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version '1.17-SNAPSHOT'
    id 'maven-publish'
}
// NO mappings line — MC 26.x ships unobfuscated Mojang names
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    implementation "net.fabricmc:fabric-loader:${project.loader_version}"
    implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
    implementation("io.wispforest:owo-lib:${project.owo_version}")
    implementation("io.wispforest:owo-sentinel:${project.owo_version}")
    annotationProcessor("io.wispforest:owo-lib:${project.owo_version}")
    implementation("io.wispforest:endec:0.1.12")
}
```

### gradle.properties
```
minecraft_version=26.2
loader_version=0.19.3
fabric_version=0.152.2+26.2
owo_version=0.13.0+26.1
```

### Access Widener
```
accessWidener v2 official
accessible class net/minecraft/datafixer/fix/ItemStackComponentizationFix$ItemStackData
accessible field net/minecraft/client/gui/screens/inventory/AbstractContainerScreen leftPos I
accessible field net/minecraft/client/gui/screens/inventory/AbstractContainerScreen topPos I
```

### Key infra notes
- Plugin ID MUST be `net.fabricmc.fabric-loom` (shorthand `fabric-loom` triggers wrong artifact)
- Java 25 required (JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64)
- Gradle 9.6.1 (Loom 1.17 requires ≥9.5)
- `options.release = 25` is commented out; compiles with JVM default (Java 25 from JAVA_HOME)
- owo-lib 0.13 + endec 0.1.12 both required on classpath
- CCA, REI, ModMenu dependencies all removed
- `org.gradle.configuration-cache=false` recommended

---

## 2. MC 26.2 API Reference (javap-verified)

### Package Moves (Mojang 26.2)
| Wrong/Assumed | Correct MC 26.2 |
|---|---|
| `net.minecraft.server.network.ServerPlayer` | `net.minecraft.server.level.ServerPlayer` |
| `net.minecraft.world.item.ItemLike` | `net.minecraft.world.level.ItemLike` |
| `net.minecraft.core.component.*` | `net.minecraft.core.component.*` (OK) |
| `net.minecraft.village.*` (Merchant etc) | `net.minecraft.world.item.trading.*` |
| `net.minecraft.village.VillagerProfession` | `net.minecraft.world.entity.npc.villager.VillagerProfession` |
| `net.minecraft.resource.*` | `net.minecraft.server.packs.resources.*` |
| `net.minecraft.resource.featuretoggle.*` | `net.minecraft.world.flag.*` |
| `net.minecraft.world.entity.damage.*` | `net.minecraft.world.damagesource.*` |
| `net.minecraft.client.gui.screen.*` (singular) | `net.minecraft.client.gui.screens.*` |
| `net.minecraft.server.world.*` | `net.minecraft.server.level.*` |
| `net.minecraft.world.item.tooltip.*` | `net.minecraft.world.item.component.*` |
| `net.minecraft.component.*` | `net.minecraft.core.component.*` |
| `net.minecraft.particle.*` | `net.minecraft.core.particles.*` |
| `net.minecraft.enchantment.*` | `net.minecraft.world.item.enchantment.*` |
| `net.minecraft.world.entity.effect.*` | `net.minecraft.world.effect.*` |
| `net.minecraft.potion.*` | `net.minecraft.world.item.alchemy.*` |
| `net.minecraft.util.profiler` | `net.minecraft.util.profiling` |

### API Method Changes (javap-verified)
| Old (Yarn/1.21) | New (Mojang/26.2) |
|---|---|
| `BlockBehaviour.use` | Split into `useWithoutItem` + `useItemOn` |
| `BlockBehaviour.onRemove` | `affectNeighborsAfterRemoval(BlockState, ServerLevel, BlockPos, boolean)` |
| `EntityBlock.createBlockEntity` | `newBlockEntity(BlockPos, BlockState)` |
| `EntityBlock.getTicker` | Default method (same signature) |
| `BaseContainerBlockEntity.saveAdditional(CompoundTag, Provider)` | `saveAdditional(ValueOutput)` |
| `BaseContainerBlockEntity.readNbt(CompoundTag, Provider)` | `loadAdditional(ValueInput)` |
| `BaseContainerBlockEntity.getHeldStacks()` | `getItems()` |
| `BaseContainerBlockEntity.setHeldStacks()` | `setItems(NonNullList)` |
| `BaseContainerBlockEntity.getContainerName()` | `getDefaultName()` (abstract) |
| `ContainerHelper.readNbt/writtenNbt` | `loadAllItems/saveAllItems(ValueInput/ValueOutput, NonNullList)` |
| `AbstractContainerMenu.quickMove` | `quickMoveStack` |
| `AbstractContainerMenu.onClosed` | `removed` |
| `Slot.getStack()` | `getItem()` |
| `Slot.setStack()` | `set(ItemStack)` |
| `Slot.hasStack()` | `hasItem()` |
| `Slot.canInsert()` | `mayPlace(ItemStack)` |
| `Container.size()` | `getContainerSize()` |
| `Container.canPlayerUse()` | `stillValid(Player)` |
| `InteractionResult<ItemStack>` (generic) | `InteractionResult` (sealed: SUCCESS, PASS, FAIL) |
| `Item.Settings()` | `Item.Properties()` |
| `Item.maxCount(n)` | `stacksTo(n)` |
| `Entity.getWorld()` | `Entity.level()` |
| `Entity.getUuid()` | `Entity.getUUID()` |
| `Entity.spawnAtLocation` | Now requires `ServerLevel` as first param |
| `player.isSneaking()` | `player.isShiftKeyDown()` |
| `player.openHandledScreen()` | `player.openMenu()` |
| `player.sendMessage(text, false)` | `player.sendSystemMessage(text)` |
| `markDirty()` | `setChanged()` |
| `getDefaultState()` | `defaultBlockState()` |
| `appendProperties` | `createBlockStateDefinition` |
| `with(state, val)` | `setValue(state, val)` |
| `get(state)` | `getValue(state)` |
| `isClient` (field on Level) | `isClientSide()` (method) |
| `SoundEvent.of(id)` | `SoundEvent.createVariableRangeEvent(Identifier)` |
| `Identifier.of(str)` | `Identifier.parse(str)` (single arg) / `Identifier.fromNamespaceAndPath(ns, path)` |
| `ResourceKey.getValue()` | `ResourceKey.identifier()` |
| `NonNullList.ofSize(n, e)` | `NonNullList.withSize(n, e)` |
| `Block.createCuboidShape(...)` | `Block.box(...)` |
| `Block.createCodec(...)` | `BlockBehaviour.simpleCodec(...)` |
| `createBlockEntity` | `newBlockEntity` |
| `WorldRenderer.getLightmapCoordinates` | REMOVED (old render pipeline) |
| `MatrixStack` | `PoseStack` at `com.mojang.blaze3d.vertex` |
| `VertexConsumerProvider` | `SubmitNodeCollector` |
| `ModelTransformationMode` | `ItemDisplayContext` |
| `GuiGraphics` | `GuiGraphicsExtractor` (completely different API) |
| `Screen.render(...)` | `Screen.extractRenderState(GuiGraphicsExtractor, int, int, float)` |
| `AbstractContainerScreen.backgroundHeight/Width` | `imageHeight/imageWidth` (final fields) |
| `playerInventoryTitleY` | `inventoryLabelY` |
| `titleY/titleX` | `titleLabelY/titleLabelX` |
| `hasClickedOutside(x, y, left, top, button)` | `hasClickedOutside(x, y, left, top)` — 4 params, no button |
| `Minecraft.getInstance().screen` | `Minecraft.getInstance().gui.screen()` |
| `Minecraft.getInstance().setScreen` | `Minecraft.getInstance().gui.setScreen` |
| `GameRules.Key<GameRules.IntRule>` | `GameRule<Integer>` (no inner classes) |
| `GameRules.register(name, category, ...)` | Use Fabric `GameRuleBuilder.forInteger(default).category(cat).range(min,max).buildAndRegister(id)` |
| `GameRules.get(key)` | `GameRules.get(gameRule)` returns T directly |
| `HoverEvent` (class) | `HoverEvent` (interface) — use `new HoverEvent.ShowText(Component)` |
| `Style.withHoverEvent(new HoverEvent(...))` | `Style.withHoverEvent(new HoverEvent.ShowText(...))` |
| `MutableComponent.formatted(ChatFormatting)` | `MutableComponent.withStyle(ChatFormatting)` |
| `ContainerLevelAccess.EMPTY` | DOES NOT EXIST — use `ContainerLevelAccess.create(null, BlockPos.ZERO)` |
| `Text.translatable/literal/empty` | `Component.translatable/literal/empty` |
| `LootPool.builder().with(...)` | `LootPool.lootPool().add(...)` |
| `LootPool.builder().conditionally(...)` | `.when(...)` |
| `RandomChanceLootCondition.builder(f)` | `LootItemRandomChanceCondition.randomChance(f)` |
| `BuiltInLootTables.X.getValue()` | `BuiltInLootTables.X.identifier()` |
| `LootPoolEntryType` | REMOVED (loot type unrolling) |
| `LeafEntry` | REMOVED |
| `DataComponentType.Builder.codec(c)` | `.persistent(c)` |
| `DataComponentType.Builder.packetCodec(c)` | `.networkSynchronized(c)` |
| `DataComponentMap.builder().add(t,v)` | `.set(t,v)` |
| `LootContextParameterSet` | DOES NOT EXIST — use `LootContextParamSet` / `LootContextParamSets` |
| `FallingBlockEntity` | at `net.minecraft.world.entity.item.FallingBlockEntity` |
| `ItemEntity` | at `net.minecraft.world.entity.item.ItemEntity` |
| `EnchantedBookItem` | REMOVED (use `Items.ENCHANTED_BOOK`) |
| `SuspiciousStewItem` | REMOVED (use `Items.SUSPICIOUS_STEW`) |
| `EnchantmentLevelEntry` | REMOVED |
| `StringNbtReader` | may have moved; verify per-use |
| `ItemStackComponentizationFix.StackData` | `ItemStackData` (private, needs access widener) |

### Fabric API v26.2 Changes
| Old | New |
|---|---|
| `GameRuleFactory` → `GameRuleBuilder` | `GameRuleBuilder.forInteger(default).category(cat).range(min,max).buildAndRegister(Identifier)` |
| `GameRuleCategory` | Vanilla at `net.minecraft.world.level.gamerules.GameRuleCategory` |
| `TooltipComponentCallback` | `ClientTooltipComponentCallback` |
| `ExtendedScreenHandlerFactory` | `ExtendedMenuProvider` (`fabric.api.menu.v1`) |
| `FabricReadView/FabricWriteView` | `FabricValueInput/FabricValueOutput` (`fabric.api.serialization.v1.value`) |
| `FabricLootPoolBuilder.with/.conditionally` | `.add/.when` |
| `ResourceManagerHelper` | REMOVED — use `ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id, listener)` |
| `SimpleResourceReloader` | `SimpleReloadListener` |
| `ResourceLoader.registerReloader` | `ResourceLoader.registerReloadListener` |
| `LootTableEvents.MODIFY` callback | `(ResourceKey<LootTable>, LootTable.Builder, LootTableSource, HolderLookup.Provider)` |
| `TradeOfferInternals` | REMOVED from Fabric 26.2 |
| `TradeOfferHelper` | REMOVED from Fabric 26.2 |
| `VillagerTrades.TRADES` map | REMOVED (trades are data-driven) |
| `VillagerTrades.WANDERING_TRADER_TRADES` | REMOVED |
| `VillagerTrades.ItemListing` | REMOVED (interface gone) |
| `ScreenEvents.afterRender` | `ScreenEvents.afterExtract(Screen)` → `AfterExtract` |
| `ItemGroupEvents` → `CreativeModeTabEvents` | `fabric.api.creativetab.v1` |
| `ClientTickEvents.START_WORLD_TICK` | `START_LEVEL_TICK` |
| `ServerWorldEvents` → `ServerLevelEvents` | |

### owo-lib 0.13 Changes
| Old | New |
|---|---|
| `ImplementedInventory` | REMOVED |
| `BlockRegistryContainer/ItemRegistryContainer` | `AutoRegistryContainer` (requires `getRegistry()` returning `Registry<T>`) |
| `BaseOwoHandledScreen` | `BaseOwoContainerScreen` |
| `BaseUIModelHandledScreen` | `BaseUIModelContainerScreen` (has `(S, Inventory, Component, Class<R>, Identifier)` ctor) |
| `Containers.*` (owo UI) | `UIContainers.*` |
| `Component` (owo UI) | `UIComponent` |
| `OwoUIDrawContext` | `OwoUIGraphics` |
| `OfflineDataLookup` | REMOVED |
| `WorldOps` | REMOVED |
| `CodecUtils` | EXISTS at `io.wispforest.owo.serialization.CodecUtils` |
| `RegistriesAttribute` | EXISTS at `io.wispforest.owo.serialization.RegistriesAttribute` |
| `MenuUtils` | at `io.wispforest.owo.client.screens.MenuUtils` (NOT `io.wispforest.owo.util`) |
| `SlotGenerator`, `SyncedProperty`, `ValidatingSlot` | at `io.wispforest.owo.client.screens.*` |
| `BaseOwoHandledScreenAccessor` | at `io.wispforest.owo.mixin.ui.access.*` |
| `LootOps.injectItem(ItemLike, float, Identifier...)` | EXISTS |
| `FieldRegistrationHandler.register(Class, String, boolean)` | 3 args (Class, String, boolean) |
| `OwoItemGroup.builder(id, icon)` | EXISTS |
| `Icon.of(Supplier<ItemStack>)` | EXISTS |
| `OwoNetChannel` | at `io.wispforest.owo.network.OwoNetChannel` |
| `TextOps` | at `io.wispforest.owo.text.TextOps` |
| `ConfigSynchronizer` | at `io.wispforest.owo.config.ConfigSynchronizer` |

### MC 26.2 Rendering Pipeline Changes
| Old | New |
|---|---|
| `BlockEntityRenderer<T>` | `BlockEntityRenderer<T, S extends BlockEntityRenderState>` (2 type args) |
| `render(entity, tickDelta, matrices, vertexConsumers, light, overlay)` | `submit(State, PoseStack, SubmitNodeCollector, CameraRenderState)` |
| (no render state) | `createRenderState()` returns state object |
| `WorldRenderer.getLightmapCoordinates(world, pos)` | REMOVED |
| `MultiBufferSource` | `SubmitNodeCollector` |
| `VertexConsumerProvider.Immediate` | REMOVED |
| `ClientTooltipComponent.drawText(textRenderer, text, x, y, matrix)` | `extractText(GuiGraphicsExtractor, Font, int, int)` |
| `ClientTooltipComponent.drawItems(textRenderer, x, y, matrix, tooltipData, z)` | `extractImage(Font, int, int, int, int, GuiGraphicsExtractor)` |

### MC 26.2 Merchant/Trade API
| Old | New |
|---|---|
| `Merchant.setCustomer/getCustomer` | `setTradingPlayer/getTradingPlayer` |
| `Merchant.setOffersFromServer` | `overrideOffers(MerchantOffers)` |
| `Merchant.trade(MerchantOffer)` | `notifyTrade(MerchantOffer)` |
| `Merchant.onSellingItem(ItemStack)` | `notifyTradeUpdated(ItemStack)` |
| `Merchant.getExperience` | `getVillagerXp()` |
| `Merchant.setExperienceFromServer` | `overrideXp(int)` |
| `Merchant.getYesSound()` | `getNotifyTradeSound()` |
| `Merchant.isLeveledMerchant()` | REMOVED (no override needed) |
| `Merchant.isTradeDisabled()` | REMOVED |
| `Merchant Offer.getOriginalFirstBuyItem()` | `getBaseCostA()` |
| `Merchant Offer.getSecondBuyItem()` | `getCostB()` (returns ItemStack, NOT Optional) |
| `Merchant Offer.getSellItem()` | `getResult()` |
| `Merchant Offer.use()` | `take(ItemStack, ItemStack)` (2 params) |
| `TradedItem` | `ItemCost` at `net.minecraft.world.item.trading.ItemCost` |
| `MerchantMenu.autofill` | `tryMoveItems(int)` |
| `MerchantMenu.merchant` field | `trader` (private final) |
| `MerchantMenu.container` field | `tradeContainer` (private final, MerchantContainer type) |
| `MerchantMenu.playYesSound` | `playTradeSound()` (private) |

### MC 26.2 Block Entity Renderer API
```
BlockEntityRenderer<T, S extends BlockEntityRenderState>:
  - createRenderState() → S
  - submit(S state, PoseStack, SubmitNodeCollector, CameraRenderState) → void
  
BlockEntityRenderState: base class (at net.minecraft.client.renderer.blockentity.state)
SubmitNodeCollector: at net.minecraft.client.renderer
CameraRenderState: at net.minecraft.client.renderer.state.level
```

### TooltipProvider interface (MC 26.2)
```java
void addToTooltip(Item.TooltipContext context, Consumer<Component> lines, TooltipFlag flag, DataComponentGetter componentGetter)
```

### ClientTooltipComponent (MC 26.2)
```java
static ClientTooltipComponent create(FormattedCharSequence) // text
static ClientTooltipComponent create(TooltipProvider data) // data
int getHeight(Font)
int getWidth(Font)
void extractText(GuiGraphicsExtractor, Font, int, int)
void extractImage(Font, int, int, int, int, GuiGraphicsExtractor)
```

---

## 3. CCA Removal (RESOLVED ✅)

CCA (Cardinal Components API) has no MC 26.2 version. Replaced with:
- `ModComponents.java`: Static `ConcurrentHashMap<UUID, long[]>` persistence via `DataInputStream/DataOutputStream`
- `CurrencyComponent.java`: Wraps `ModComponents.getCurrencyData(uuid)`, no CCA interfaces
- Lifecycle: `SERVER_STARTING` → `init(path)`, `SERVER_STOPPING` → `saveAll()`
- Death/respawn: `copyOnRespawn(oldPlayer, newPlayer, alive)` 
- No data migration from old CCA saves (acceptable for port)

---

## 4. Stub Inventory (What Needs Implementing)

### 🔴 CRITICAL — Feature Breakage

#### 4.1 Villager Trade System (14 adapter files + 3 registry files)
**Impact:** Villager currency trades do NOT work. This is the mod's core feature.
**Root cause:** `VillagerTrades.TRADES` map (static) removed in MC 26.2. `VillagerTrades.ItemListing` interface removed. Fabric's `TradeOfferInternals`/`TradeOfferHelper` removed.

**Current state:** All 14 adapter `deserialize()` methods return `null`. `TradeJsonAdapter.deserialize()` returns `Object`. `NumismaticVillagerTradesRegistry` methods are empty stubs. `RemappingTradeWrapper` is a no-op. `VillagerTradesResourceListener` logs a warning and does nothing.

**What needs implementing:**
- **Option A:** Find new Fabric/Vanilla API for trade modification (research needed — 26.2 may have data-driven trades via JSON datapacks)
- **Option B:** Mixin into villager trade loading to modify trades at runtime
- **Option C:** Completely remove villager trade feature, keep only shop/piggy bank
- **Files:** All 14 adapters, `NumismaticVillagerTradesRegistry`, `RemappingTradeWrapper`, `VillagerTradesResourceListener`, `VillagerTradesHandler`, `TradeJsonAdapter`, `VillagerJsonHelper`

**Original approach (1.21):** Modified static `TradeOffers.PROFESSION_TO_LEVELED_TRADE` map by wrapping each factory with `RemappingTradeWrapper` that converts emerald costs to coin costs. JSON datapacks loaded custom trade definitions via `VillagerTradesResourceListener`.

#### 4.2 ShopBlockEntityRender — No-op
**Impact:** Floating item above shop block does NOT render.
**Current state:** `submit()` method is empty no-op.
**What needs implementing:** Reimplement with MC 26.2 `BlockEntityRenderer<T, S>` API. Need `createRenderState()` + `submit(State, PoseStack, SubmitNodeCollector, CameraRenderState)`. Original rendered a spinning item above the shop using `ItemDisplayContext.GROUND`, `OverlayTexture`, rotation matrix.
**File:** `ShopBlockEntityRender.java`

#### 4.3 CurrencyTooltipProvider.addToTooltip — Empty
**Impact:** Tooltip for currency items shows nothing.
**What needs implementing:** Add coin count lines to tooltip via `lines.accept(Component.literal(...).withStyle(...))`.
**File:** `CurrencyTooltipProvider.java` (line 36)

#### 4.4 CurrencyTooltipComponent — drawText/drawItems removed
**Impact:** Custom tooltip rendering for currency (coin icons + discount strikethrough) does NOT work.
**What needs implementing:** Implement `extractText(GuiGraphicsExtractor, Font, int, int)` and `extractImage(Font, int, int, int, int, GuiGraphicsExtractor)`.
**File:** `CurrencyTooltipComponent.java` (lines 52-54)

#### 4.5 MoneyBagLootEntry — Not Registered
**Impact:** Money bags cannot appear in dungeon/structure loot chests.
**Current state:** `MoneyBagLootEntry` class exists and compiles, but `MONEY_BAG_ENTRY` registration is commented out. `LootPoolEntryType` registry removed in 26.2 (loot type unrolling). Need to figure out how to register custom loot entries in 26.2.
**Files:** `NumismaticOverhaul.java` (line 75, 114), `MoneyBagLootEntry.java`

#### 4.6 NumismaticOverhaul.CONFIG.load() — Commented Out
**Impact:** Config hot-reload via `reloadMobDropConfig` doesn't reload from disk.
**Current state:** Line 191 has `// CONFIG.load();`. The `reloadMobDropConfig` method calls `loadMobDropConfig(null)` after clearing the map, but without re-reading config from disk, changes won't take effect.
**File:** `NumismaticOverhaul.java` (line 191)

### 🟡 MODERATE — Degraded Functionality

#### 4.7 PiggyBankBlock — collectComponents() Missing
**Impact:** When piggy bank is broken in creative mode, dropped item doesn't carry NBT data (stored coins lost on pickup).
**Current state:** Line 138: `// stack.applyComponentsFrom(piggyBank.collectComponents());`
**What needs implementing:** `piggyBank.collectComponents()` — gather inventory contents as data components.
**File:** `PiggyBankBlock.java` (line 138)

#### 4.8 ShopScreenHandler — afterDataUpdate Callback Missing
**Impact:** Client-side UI doesn't refresh when server sends synced data updates.
**Current state:** Line 62: `// afterDataUpdate removed` — the `tradeEditBuffer.observe()` callback does nothing on client.
**What needs implementing:** Trigger screen refresh when `tradeEditBuffer` updates. Maybe `Minecraft.getInstance().gui.screen()` refresh call.
**File:** `ShopScreenHandler.java` (line 62)

#### 4.9 NumismaticCommand — Offline Player Support Missing
**Impact:** `/currency total` command only counts online players, not offline.
**Root cause:** `OfflineDataLookup` removed from owo-lib 0.13.
**Current state:** Lines 45-53: offline player scanning is commented out.
**What needs implementing:** Alternative offline player data access (e.g., read player data files directly from world folder).
**File:** `NumismaticCommand.java`

#### 4.10 ItemStackComponentizationFix — Migration Code Missing
**Impact:** Old pre-data-component items from 1.21 worlds may not migrate correctly.
**Current state:** Line 15: `// TODO: Migration to data components removed due to private ItemStackData access in MC 26.2`
**What needs implementing:** Access widener already added for `ItemStackComponentizationFix$ItemStackData`. May need accessor mixin or direct field access.
**File:** `ItemStackComponentizationFixin.java`

#### 4.11 ShopScreen — addFormatter Missing
**Impact:** Price input field accepts non-numeric characters (no input validation).
**Current state:** Line 217: `// TODO: addFormatter removed - digit-only validation handled by responder`
**What needs implementing:** Add digit-only filter to EditBox via `addFormatter(TextFormatter)` or `setFilter` equivalent.
**File:** `ShopScreen.java`

#### 4.12 CoinItem/MoneyBagItem — Trade Slot Handling Missing
**Impact:** Coins and money bags can't be used as trade items in merchant UI.
**Current state:** Lines 32/63: `// TODO: handle trade slots if needed`
**What needs implementing:** Implement `useOn` or trade slot interaction if needed.
**Files:** `CoinItem.java`, `MoneyBagItem.java`

### 🟢 LOW — Cosmetic/Edge Case

#### 4.13 MoneyBagLootEntry — Player-Balance-Aware Drop Removed
**Impact:** Loot entry always drops configured range, not adjusted by player balance.
**Original behavior:** Used `LootContextParameters.LUCK` and player balance to adjust drop amount.
**File:** `MoneyBagLootEntry.java`

---

## 5. Files Changed (67 total)

### Build/Config (5 files)
- `build.gradle` — Loom 1.17, Java 25, owo 0.13, endec, removed CCA/REI/ModMenu
- `gradle.properties` — MC 26.2, loader 0.19.3, fabric 0.152.2, owo 0.13.0
- `gradle/wrapper/gradle-wrapper.properties` — Gradle 9.6.1
- `src/main/resources/numismatic-overhaul.accesswidener` — v2 official + 3 entries
- `src/main/resources/numismatic-overhaul.mixins.json` — JAVA_25, removed TradeOfferInternalsMixin, added ServerLevelMixin

### Core Server (13 files)
- `NumismaticOverhaul.java` — Vanilla gamerules, MenuType, loot table injection, ModComponents lifecycle
- `ModComponents.java` — ConcurrentHashMap persistence (replaces CCA)
- `NumismaticCommand.java` — Commands without OfflineDataLookup
- `NumismaticOverhaulConfigModel.java` — `.location()` rename
- `NumismaticOverhaulBlocks.java` — AutoRegistryContainer, BlockEntityType
- `NumismaticOverhaulItems.java` — AutoRegistryContainer
- `CurrencyComponent.java` — No CCA interfaces, wraps ModComponents
- `MoneyBagComponent.java` — DataComponentType registration
- `MoneyBagLootEntry.java` — LootPoolSingletonContainer, codec()
- `Currency.java` — ItemLike at correct package
- `CurrencyHelper.java` — ItemContainerContents API
- `CurrencyConverter.java` — Minor import changes
- `CurrencyItem.java` — Minor import changes

### Block/Container (8 files)
- `ShopBlockEntity.java` — BaseContainerBlockEntity, ValueOutput/Input, WorldlyContainer
- `ShopBlock.java` — BaseEntityBlock, useWithoutItem, affectNeighborsAfterRemoval
- `ShopMerchant.java` — Merchant interface with new method names
- `ShopScreenHandler.java` — AbstractContainerMenu, MenuUtils, gui.screen()
- `ShopOffer.java` — ItemCost, DataComponentMap, isSameItemSameComponents
- `PiggyBankBlock.java` — HorizontalDirectionalBlock, EntityBlock, BlockPlaceContext
- `PiggyBankBlockEntity.java` — BaseContainerBlockEntity, ValueInput/Output
- `PiggyBankScreenHandler.java` — ContainerLevelAccess, AbstractContainerMenu

### Client/UI (6 files)
- `NumismaticOverhaulClient.java` — UIComponent, OwoUIGraphics, BaseOwoHandledScreenAccessor
- `ShopScreen.java` — BaseOwoContainerScreen, mouseDown lambdas, field renames
- `PiggyBankScreen.java` — BaseOwoContainerScreen, extractRenderState
- `PurseLayerElement.java` — UIComponent, afterExtract, keyShift/keySprint
- `CurrencyTooltipComponent.java` — ClientTooltipComponent, Font, extractText/extractImage stubs
- `ShopBlockEntityRender.java` — BlockEntityRenderer<T,S>, submit() no-op

### Network (3 files)
- `RequestPurseActionC2SPacket.java` — Inventory.add, minor renames
- `ShopScreenHandlerRequestC2SPacket.java` — containerMenu rename
- `UpdateShopScreenS2CPacket.java` — gui.screen() access

### Mixins (7 files)
- `BundleItemMixin.java` — getItem() rename
- `ItemStackComponentizationFixin.java` — Empty (migration commented out)
- `LayerInstanceAccessor.java` — @Mutable annotation
- `LivingEntityMixin.java` — dropCustomDeathLoot, Mth.randomBetween
- `MerchantScreenHandlerMixin.java` — tryMoveItems, playTradeSound, trader field
- `ServerPlayerEntityMixin.java` — destroyVanishingCursedItems, drop 2-arg
- `ServerLevelMixin.java` — ServerLevel import fix

### Items (5 files)
- `CoinItem.java` — InteractionResult.SUCCESS, Item.Properties
- `MoneyBagItem.java` — getHoverName, has() instead of contains()
- `CurrencyTooltipData.java` → renamed concept, implements TooltipProvider
- `NumismaticOverhaulItems.java` — AutoRegistryContainer<Item>
- `NumismaticOverhaulConfigModel.java` — .location()

### Villagers (17 files — ALL STUBBED)
- `NumismaticVillagerTradesRegistry.java` — Empty methods
- `RemappingTradeWrapper.java` — No-op wrapper
- `VillagerTradesResourceListener.java` — Logs warning, does nothing
- `TradeJsonAdapter.java` — Returns Object, not typed
- `VillagerTradesHandler.java` — Compiles but trade registration does nothing
- `VillagerJsonHelper.java` — Compiles, helper methods work
- All 14 adapters (BuyStack, BuyTag, DimensionAwareSellStack, EnchantItem, ProcessItem, SellDyedArmor, SellEnchantedItem, SellMap, SellMapTag, SellPotionContainerItem, SellSingleEnchantment, SellStack, SellSusStew, SellTag) — All return null

---

## 6. Build Commands

```bash
cd /home/thinh0704hcm/minecraft-server/numismatic-overhaul
JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64 ./gradlew compileJava --no-daemon
```

---

## 7. Sources of Truth

### Scraped Fabric Porting Docs
- `.firecrawl/porting-overview.md` — Build changes for 26.1+
- `.firecrawl/porting-fabric-api.md` — 601 lines of Fabric API renames
- `.firecrawl/fabric-blog-261.md` — Fabric blog confirming breaking changes
- `.firecrawl/neoforge-primer-261.md` — 5034 lines vanilla class changes (reference only)

### MC 26.2 Jars (javap-verified)
- Merged-deobf jar: `~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.2/`
- owo-lib jar: `~/.gradle/caches/modules-2/files-2.1/io.wispforest/owo-lib/0.13.0+26.1/`
- endec jar: `~/.gradle/caches/modules-2/files-2.1/io.wispforest/endec/0.1.12/`
- Fabric API jar: via Gradle dependency cache

### Git History
- Original codebase at HEAD `52753a4` (MC 1.21.1, Yarn mappings, CCA, REI)
- Original adapter implementations preserved in git history for reference
- Use `git show HEAD:<filepath>` to see original 1.21.1 code

---

## 8. Risk Assessment

| Area | Risk | Reason |
|---|---|---|
| Villager trades | 🔴 HIGH | Entire system gutted; need new API discovery |
| Shop rendering | 🟡 MEDIUM | Render pipeline changed but API exists |
| Loot injection | 🟡 MEDIUM | MoneyBagLootEntry registration unclear in 26.2 |
| Client UI | 🟡 MEDIUM | owo UI mostly ported, some callbacks missing |
| Container logic | 🟢 LOW | Most methods ported, compiles green |
| Persistence | 🟢 LOW | CCA → ConcurrentHashMap works |
| Mixins | 🟡 MEDIUM | Compiles but runtime injection targets unverified |
| Config | 🟢 LOW | owo-sentinel generated, basic structure works |
