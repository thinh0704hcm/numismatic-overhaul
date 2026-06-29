[Skip to content](https://docs.fabricmc.net/develop/porting/#VPContent)

On this page

## Page Authors

[![cassiancc](https://wsrv.nl/?af=&maxage=7d&url=https%3A%2F%2Fgithub.com%2Fcassiancc.png%3Fsize%3D32&default=https%3A%2F%2Fdocs.fabricmc.net%2Fassets%2Favatater.png)](https://github.com/cassiancc)[![ChampionAsh5357](https://wsrv.nl/?af=&maxage=7d&url=https%3A%2F%2Fgithub.com%2FChampionAsh5357.png%3Fsize%3D32&default=https%3A%2F%2Fdocs.fabricmc.net%2Fassets%2Favatater.png)](https://github.com/ChampionAsh5357)

# Porting to 26.1 26.1.2 [​](https://docs.fabricmc.net/develop/porting/\#h1)

Guidelines for porting to Minecraft 26.1, the latest version of Minecraft.

The 26.1 version of Minecraft is unobfuscated, as were its snapshots. With this in mind, you'll need to make more changes to your build scripts than usual in order to port to it.

INFO

These docs discuss migrating from **1.21.11** to **26.1**. If you're looking for another migration, switch to the target version by using the dropdown in the top-right corner.

## Prerequisites [​](https://docs.fabricmc.net/develop/porting/\#prerequisites)

If your mod is still using Fabric's Yarn Mappings, you'll first need to [migrate your mod to Mojang's official mappings](https://docs.fabricmc.net/develop/porting/mappings/) before porting to 26.1.

If you are using IntelliJ IDEA, you will also need to update it to `2025.3` or higher for full Java 25 support.

## Updating the Build Script [​](https://docs.fabricmc.net/develop/porting/\#build-script)

Start by updating your mod's `gradle/wrapper/gradle-wrapper.properties`, `gradle.properties`, and `build.gradle` to the latest versions, then follow the steps below. If you run into trouble, consider referencing the [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod/tree/26.1).

01. Update Gradle to the latest version by running the following command: `./gradlew wrapper --gradle-version latest`
02. Bump Minecraft, Fabric Loader, Fabric Loom and Fabric API, either in `gradle.properties` (recommended) or in `build.gradle`. Find the recommended versions of the Fabric components on the [Fabric Develop site](https://fabricmc.net/develop/).
03. At the top of `build.gradle`, change the version of Loom you are using from `id "fabric-loom"` to `id "net.fabricmc.fabric-loom"`. If you specify Loom in `settings.gradle`, change it there as well.
04. Remove the `mappings` line from the dependencies section of `build.gradle`.
05. Replace any instances of `modImplementation`, `modCompileOnly` or `modApi` with `implementation`, `compileOnly` and `api`.
06. Remove or replace any mods made for versions before 26.1 with versions compatible with this update.
    - No existing mods for 1.21.11 or older versions of Minecraft will work on 26.1, even as a compile-only dependency.
07. If needed, update the header of your [access widener or class tweaker](https://docs.fabricmc.net/develop/class-tweakers/) to replace `named` with `official`.
08. Set Java compatibility to 25 instead of 21.
09. Replace any mentions of `remapJar` with `jar`.
10. Refresh Gradle by using the refresh button in the top-right corner of IntelliJ IDEA. If this button is not visible, you can force caches to be cleared by running `./gradlew --refresh-dependencies`.

## Updating the Code [​](https://docs.fabricmc.net/develop/porting/\#porting-guides)

After the build script has been updated to 26.1, you can now go through your mod and update any code that has changed to make it compatible with the snapshot.

- [Fabric for Minecraft 26.1 on the Fabric blog](https://fabricmc.net/2026/03/14/261.html) contains a high-level explanation of the changes made to Fabric API in 26.1.
- [Fabric API 26.1 Porting Guide](https://docs.fabricmc.net/develop/porting/fabric-api) lists the renames made to Fabric API in 26.1 snapshots to match Mojang's names.
- [_Java Edition 26.1_ on the Minecraft Wiki](https://minecraft.wiki/w/Java_Edition_26.1) is an unofficial summary of the contents of the update.
- [NeoForge's _Minecraft 1.21.11 -> 26.1 Mod Migration Primer_](https://github.com/neoforged/.github/blob/main/primers/26.1/index.md) covers migrating from 1.21.11 to 26.1, focusing only on vanilla code changes.

Copied