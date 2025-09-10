# Schematic Placer
An addon for WorldEdit that streamlines placing schematics in Minecraft.

## Commands
### `placeschem <filename> [<pos>]` 
loads and places a schematic. Loading is done asynchronously and placement happens over multiple ticks if necessary (see [batchSize](#batchSize))
### `removeschem <filename> [<pos>]` 
Sets all non-air blocks to air that are contained in the schematic. Uses the same loading and placement logic as `placeschem`.
### `schemitem <line>` 
Attaches a command to an item by adding the command to the item's Lore. The command is activated using right click. If relevant, shift right clicking will invert the command (place -> remove). Supported commands are `placeschem`, and `removeschem`.

## Configuration

The config file will be located in the .minecraft folder at `config/schemplacer.json`.
### batchSize
The maximum number of blocks that can be placed per game tick.