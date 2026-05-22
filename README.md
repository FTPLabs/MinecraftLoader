# Players ESP — Minecraft Forge 1.21.1

A client-side Forge mod that draws **3D bounding boxes** and **health bars** around all players within render range.

## Features

- **3D wireframe boxes** around each player's hitbox  
- **Health bar** rendered above the box (green → red)  
- **Toggle with `G` key** — chat message confirms state  
- **64 block** render range (configurable in source)  
- Works only client-side — no server installation needed

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| Forge | 52.0.47+ |
| Java | 21 |

## Build from Source

```bash
# Clone the repo
git clone https://github.com/FTPLabs/MinecraftLoader.git
cd MinecraftLoader

# Build (downloads Minecraft/Forge automatically on first run — takes ~10 min)
./gradlew build

# Output
build/libs/playersesp-1.0.0.jar
```

## Install

1. Drop `playersesp-1.0.0.jar` into `.minecraft/mods/`
2. Launch Minecraft 1.21.1 with Forge 52.0.47+
3. Press **G** in-game to toggle ESP

## CI / Releases

GitHub Actions automatically builds and publishes a release JAR on every push to `main`.  
See the [**Releases**](../../releases) page to download a pre-built JAR.

## License

MIT
