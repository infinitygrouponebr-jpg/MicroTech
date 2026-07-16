# MicroTech Asset Guidelines

These rules are the approved visual standard for MicroTech. Follow them whenever creating, improving, or fixing visual assets.

## General style

- Minecraft vanilla / pixel art
- simple futuristic tech
- clean, legible, professional
- compact, functional, survival-friendly
- not overly magical
- no excessive glow, fantasy particles, or visual noise

The goal is for items, chips, machines, and components to feel like one mod family.

## Pixel art rules

For item textures, especially 16x16 or 32x32:

1. Clear silhouette.
2. Strong dark outline.
3. Light from upper left.
4. Shadows in lower and right areas.
5. 4 to 5 tones per main color.
6. Controlled contrast.
7. Avoid flat color blocks.
8. Avoid visual noise.
9. Avoid tiny details that blur out.
10. Avoid soft or realistic gradients.
11. Keep readability in inventory and hotbar.
12. Check that the item still reads at small size.

## Palette and materials

Common MicroTech visual materials:

- dark graphite
- dark tech blue
- metallic gray
- white or light metallic
- energetic cyan
- electric blue
- orange or copper for processing
- red or orange for heat
- green or teal for mining
- yellow or gold for solar or energy

Shadows can be slightly cooler. Highlights can be slightly more vivid. Use light hue shifting when it helps.

## Chips / upgrades

Approved chip style:

- 16x16 Minecraft/vanilla tech pixel art
- dark graphite or dark blue body
- dark outline
- light from upper left
- shadow in lower right
- 4 to 5 tones per color
- controlled contrast
- small legible circuits
- colored core or central detail
- technological, not magical
- each chip needs its own identity by function or category
- not just a colored square
- no excess noise
- no blurry detail

Chip visual categories:

- Speed: motion, arrows, diagonal lines
- Efficiency: clean core, economy, optimization
- Input: arrow entering
- Output: arrow leaving
- Range: radial mark, expansion
- Area: grid, volume, expansion
- Filter: funnel, selection, target
- Fortune: shine, yield, extra output
- Solar Focus: sun, lens, golden energy

## Dusts

The current dusts are approved and protected.

Do not change without explicit request:

- coal_dust.png
- iron_dust.png
- copper_dust.png
- gold_dust.png
- lapis_dust.png
- diamond_dust.png
- emerald_dust.png
- netherite_dust.png

If a future task involves chips, machines, GUI, energy, Tech Crusher, Tech Miner, or Electric Furnace, keep the dusts intact.

Only change dusts if the user explicitly asks to alter, remake, or modify them.

## Armor and tech items

- graphite and metallic gray base
- light metallic details
- cyan or electric blue lights
- discreet energy cores
- robust technical look
- not magical
- keep good inventory readability
- if 32x32, add detail without clutter

Tech Armor:

- graphite, white/metallic, and cyan identity
- chestplate can have a cyan energy core
- all pieces should belong to the same visual family
- each piece must remain individually recognizable

## Machines

- compact design
- metallic or graphite base
- cyan or blue energy details
- functional parts visible
- no random detail clutter
- model must match the texture
- use `.noOcclusion()` on custom 3D blocks when needed
- avoid transparent floor/base bugs
- check lower faces if a bright square appears

For machine items in inventory:

- not too large
- not too small
- fits inside the slot box
- no broken fallback
- display model must be correct

## GUI / interface

Approved GUI style:

- dark tech look
- short text
- no overlap
- no cut hotbar
- vertical energy bar
- energy tooltip on hover
- compact values on screen, for example `16k / 50k FE`
- full values in tooltip
- tooltips only on real hover
- tooltips must not stick to the screen

Do not repeat past issues:

- text on top of slots
- invisible slots
- buttons overlapping slots
- GUI too tall and hiding the hotbar
- long values colliding with other info

## Particles and effects

Prefer industrial or tech effects, not magical ones:

- `ELECTRIC_SPARK`
- `CRIT`
- subtle `SMOKE`
- controlled cyan particles
- gray or metallic particles
- small sparks
- metallic or industrial sounds

Avoid:

- purple magical particles
- portal or enchant effects without reason
- exaggerated glow
- particle spam

## Asset protection

Do not alter approved assets without explicit request.

Protected assets:

- current dusts
- approved chips
- approved Tech Armor
- approved Tech Miner
- approved Tech Crusher
- approved Gravite
- approved Flight Chip

When a task is not visual, do not redesign assets.

## Final report requirements

When creating or improving any texture or image, always report:

1. Files changed or created.
2. Resolution used.
3. Visual technique applied.
4. How the MicroTech style was preserved.
5. How the item or block remains legible in inventory.
6. Confirmation that there was no missing texture.
7. Confirmation that protected assets were not changed.
