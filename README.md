# Musket Mod (NeoForge 1.21.1)

Adds a **Rifled Musket** and **Musket Balls** to Minecraft 1.21.1.

## Controls
| Input | State | Result |
|---|---|---|
| **Left-click** | Loaded | **Fire** |
| **Right-click** (hold 5s) | Empty | **Reload** — rams a ball home |
| **Right-click** (hold) | Loaded | **Aim down iron sights** — zooms in, steadies the shot |

Hip-firing sprays; about 0.7s of aim gives a pinpoint shot. The musket can't mine or melee.

## Features
- **7 hearts (14 damage)** per shot
- **Ammo is stored in the gun** — load in advance and it stays charged until you fire,
  even if you swap slots or stash it in a chest
- **Shoots through blocks** — the ball ignores terrain and only stops on a living target
- **Slim 3D model** — thin barrel, walnut stock, brass bands, working iron sights. The
  hammer sits down when empty, half-cock while loading, full cock when loaded
- **Ammo HUD** — `Ammo [1/1]` above the hotbar, ball count, reload bar, steadiness bar
- Muzzle smoke and flash, 256 durability, Combat creative tab
- **One musket per hotbar + offhand**

## Crafting
**Rifled Musket** (shaped): Diamond Block centre, Gold Block left of centre, Stick bottom-right.

**Musket Ball** (shapeless): 1 Gunpowder + 1 Iron Ingot -> 1 Musket Ball

## Building
Requires Java 21. Run `gradle build`; the jar lands in `build/libs/`.
This repo also builds automatically on GitHub — push, open **Actions**, download the
`musketmod-jar` artifact. Drop the jar into a NeoForge 1.21.1 `mods` folder.
