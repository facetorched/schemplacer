![icon](./src/main/resources/assets/schemplacer/icon.png)

# Schematic Placer
An addon for [WorldEdit](https://enginehub.org/worldedit) that streamlines placing schematic files in Minecraft.

## Commands
#### `schemplace <filename> [<pos>] [<ignoreAir>]` 
loads and places a schematic. Loading is done asynchronously and placement happens over multiple ticks if necessary (see [batchSize](#batchSize)).
#### `schemremove <filename> [<pos>] [<ignoreAir>]` 
Sets blocks to air that are contained in the schematic. Uses the same loading and placement logic as `placeschem`.
#### `schemanimate <filename> [<pos>] [<ticksPerFrame>] [<start>] [<end>] [<step>] [<loop>] [<removeWhenDone>] [<clearPrev>] [<ignoreAir>]`
Sequentially places schematics using the same logic as `schemplace`.
#### `schemstopanimate <filename> [<pos>] [<ticksPerFrame>] [<start>] [<end>] [<step>] [<loop>] [<removeWhenDone>] [<clearPrev>] [<ignoreAir>]`
Stops any currently running animation that matches the given parameters.
#### `schemitem <line>` 
Attaches a command to an item by adding the command to the item's Lore. The command is activated using right click. If relevant, shift right clicking will invert the command (place -> remove), (animate -> stopanimate) and vice-versa. Supported commands are `schemplace`, `schemremove`, `schemanimate`, `schemstopanimate`.

| Parameter            | Default Value   | Explanation |
|----------------------|-----------------|-------------|
| `<filename>`         | *required*      | Path or pattern for the schematic file(s). Supports `%d` formatting for frame numbers. |
| `[<pos>]`            | Player position | World coordinates where the schematic is pasted. Accepts absolute or relative (`~ ~ ~`) values. |
| `[<ticksPerFrame>]`  | `0`             | Number of ticks between the start of each frame. If exceeded, the next frame will start as soon as possible. |
| `[<start>]`          | `0`             | First frame number in the sequence. |
| `[<end>]`            | `-1`            | Last frame number in the sequence (inclusive). `-1` = keep going until the schematic does not exist. |
| `[<step>]`           | `1`             | How much to increment (or decrement if negative) the frame number each step. |
| `[<loop>]`           | `false`         | If `true`, restart at `<start>` when the sequence finishes. |
| `[<removeWhenDone>]` | `true`          | If `true`, remove the last schematic frame when the animation completes. |
| `[<clearPrev>]`      | `true`          | If `true`, automatically remove blocks from the previous frame before placing the next. |
| `[<ignoreAir>]`      | `true`          | If `true`, skip air blocks when pasting (can increase performance). |

## Configuration

The config file will be located in the .minecraft folder at `config/schemplacer.json`.
#### `batchSize`
The maximum number of blocks that can be placed per game tick.

#### `cacheSchematics`
Whether to cache all schematics into memory to increase placement speed.

#### `schematicDir`
Path to the location where scematics are loaded from. Accepts local paths to `.minecraft` or absolute paths.

#### `commandOutput`
Whether to output command feedback to the player.
