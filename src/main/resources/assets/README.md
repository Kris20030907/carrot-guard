# Carrot Guard Assets

Place PNG textures in this directory. The game loads these files from the classpath and falls back to Java2D drawing when a file is missing.

The current files are generated placeholder assets for the first art pass. Carrot, tower, and enemy sprites are cropped from an AI-assisted source sheet and exported as transparent PNG files. Keep the same filenames when swapping in refined hand-drawn or AI-assisted art later.

Regenerate the placeholder pack after compiling test helpers:

```bash
java -cp target/classes:/tmp/carrot-guard-test-out com.ktpro.carrotguard.AssetSpriteGenerator src/main/resources/assets /path/to/source-sheet.png
```

Supported names:

- `grass.png`
- `path.png`
- `carrot.png`
- `tower_basic.png`
- `tower_slow.png`
- `tower_splash.png`
- `projectile_basic.png`
- `projectile_slow.png`
- `projectile_splash.png`
- `enemy_normal.png`
- `enemy_fast.png`
- `enemy_tank.png`
- `obstacle_crate.png`
- `obstacle_rock.png`
