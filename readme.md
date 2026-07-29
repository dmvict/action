# Custom Actions mod for Wurm Unlimited (Client)

Requires [Ago's Client Mod Launcher](https://github.com/ago1024/WurmClientModLauncher/releases) to run.

This mod is free software: you can redistribute it and/or modify it under the terms of the [GNU Lesser General Public License](http://www.gnu.org/licenses/lgpl-3.0.en.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

See the [forum thread](https://forum.wurmonline.com/index.php?/topic/136575-released-improved-compass-no-winter-better-tooltips-custom-actions-max-toolbelt-time-lock-skill-gain-tracker-updated-june-2/) for more details and my other mods.


### Brief instructions
Type "act_show on" in the console. This will let you see action IDs on the right click menu.

![action ids](http://i.imgur.com/aY4voyx.jpg)

Bind your hotkey using 'bind <key> "act <id> hover"'. The double quotes around the action are required. For example bind ctrl+b "act 222 hover" will open the bank window if you hover your mouse over a settlement token and press ctrl+b

![binding](http://i.imgur.com/2WXkmXf.jpg)

### All options

```
bind B "act <id> <target>"
bind B "act <id> <target> | <id> <target>"
bind B "act <id> <target> | <id> <target> | <id> <target>"
```
_Chaining commands will queue actions. You must have high enough ML for the number of actions_

| Target      |                                                                  |
| ----------- | ---------------------------------------------------------------- |
| `hover`     | Uses the specified action upon the hovered item, object or tile  |
| `selected`  | Uses the specified action upon the selected tile/object          |
| `activated` | Uses the specified action upon the currently activated item      |
| `toolbelt`  | Activates the tool in belt slot `<id>`                           |
| `body`      | Uses the specified action upon the characters body               |
| `tile`      | Uses the specified action on current tile                        |
| `tile_{dir}`| Uses the specified action on nearby tile (n,e,w,s,ne,nw,se,sw)   |
| `area`      | Uses the specified action on 3x3 tiles around current tile       |
| `@tb{n}`    | Uses the specified action on item in toolbelt slot #n            |
| `@eq{n}`    | Uses the specified action on item in activated character slot #n |
| `@neaby{n}` | Uses the specified action for items and creatures in radius #n   |

### Examples

| Command                         | Description                                |
| ------------------------------- | ------------------------------------------ |
| `bind space "act 3 toolbelt \| 154 tile \| 4 toolbelt \| 318 tile"` | Activate slot 3 (say, a shovel) and pack the tile you are standing on, after that it will activate slot 4 (say a rake) and cultivate the same tile |
| `bind r "act 163 hand"` | Repair your current tool |
| `bind f "act 183 @tb1"` | Drinks from toolbelt slot #1 (if it contains water) |

---

# Default action extension for mod
#### by [dmvict](https://github.com/dmvict)

The extension aims to improve game experience by fine tuning of interaction with game world using restricted number of keybindings. 

### General information

- the mod is distributed `as is`, any bugfixes and next support are not included 
- it's the extension of original mod, it tries to keep original behavior and provides additional features that could be uses side by side
- known features and limitations are described in this document
- any reasonable changes to code or documentation are welcomed

### Breaking changes 

The new version of the mod is fully compatible with the previous versions. The exclution is the renaming of action `tool` to `activated`. The change has sense since the actions declared via rule `act 1 activated` will be applied to any activated item in user invertory/backpack/character window. 

The other breaking changes are not known at the moment. 

### Quick start

There are two ways to begin work with default actions:
- launched game
- game is not launched

#### Quick start with launched game

- open console (press `F1`) and type `act_default save`. It creates file with properties for default actions. You can find the file in directory with mod file `action.jar`. 
- navigate to directory `[your game directory]/mods/action`
- open file `act_default.properties` via your preffered editor 
- put example of config and save file
```properties 
# 162 - repair
activated.hatchet=162
# 183 - drink
activated.water=183

# 223 - forage
hover.Grass=223
# 145 - mine
hover.Cave\ wall=145
```

- in game console window type `act_default load`. It will loads changed file and you'll able to work with default actions 
- make a few keybindings to interact with mod. For example: `bind F "act default hover"` and `bind R "act default activated"` 
- open inventory and activate `hatchet` and then press `R`. It will repair hatchet. If you activate water in some container and then press binded key, then character will start drinking 
- to try hover actions, find a grass tile that is named `Grass` and press binded key. The character shoud start foraging. Please, sure that tile is named exactly `Grass` because any other text will broke rule

#### Quick start without launched game

- navigate to directory `[your game directory]/mods/action` and create file `act_default.properties`
- open file `act_default.properties` via your preffered editor 
- put example of config above and save file
- launch game 
- make a few keybindings to interact with mod. For example: `bind F "act default hover"` and `bind R "act default activated"` 
- open inventory and activate `hatchet` and then press `R`. It will repair hatchet. If you activate water in some container and then press binded key, then character will start drinking 
- to try hover actions, find a grass tile that is named `Grass` and press binded key. The character shoud start foraging. Please, sure that tile is named exactly `Grass` because any other text will broke rule

### Command interface and config file 

#### Default action commands 

The extension introduces special command `act_default`. It allows to make a few action with config:
- `act_default save` - saves current in-memory config into config file. It is useful if you have updated in runtime config and want to save it. **Important:** the command rewrites original file and it saves raw config. It means that if you have comments then they will be cleared. The updated config will have alphabetically sorted target sections. Please, backup your original config manually if it is required. 
- `act_default load` - loads configuration from config file when game is launched. It is useful if you've changed some rule when game is opened and want to use it immediately.
- `act_default print` - print current rules (keys), designed for debugging and could be used before saving updated file 

#### How to make keybind 

The action introduces `default` and `alt` (alternative) actions per target.

It's almost the same as the original command except that the action id you have to put keyword `default` or `alt`. 

Example:
```
bind F "act default hover"
bind Shift+R "act alt activated"
```

**Notice.** Action piping via `|` symbol is not changed and simultaneously it's not tested. 

#### Config file

All target rules is stored in the file `act_default.properties` in directory of the action.  The file uses [`.properties` file](https://en.wikipedia.org/wiki/.properties) syntax. The format of the file is choosen for reducing size of bundled file `action.jar`.

Each line of file declares rule for performed action. The syntax of the rule is:
```
[target_name].[rule_name]=[default_action_id]|[alternative_action_id]
```
where 
- `target_name` - name of targeted action. It could be: `hover`, `selected`, `activated`, `toolbelt`, `body`, `tile`, `tile_n`, `tile_ne`, `tile_nw`, `tile_s`, `tile_se`, `tile_sw`, `tile_e`, `tile_w`, `area`, `@tb{n}`, `@eq{n}`, `@neaby{n}` 
- `rule_name` - rule with special syntax. It depends on target and is described in topics below. General rule syntax described in next section
- `default_action_id` and `alternative_action_id` - numbers that represents action in game. You can see that numbers using command `act_show on` and the place correct values. Example: `3|6`. **Note:** you could use syntax with only `default_action_id` and omit `alternative_action_id` as in example in quick start. It means that the alternative action is `1` - examine object. 

##### Kinds of rules 

All rules is divided by two categories:
- strict rules 
- patterns 

###### Strict rules 

This kind of rules use full descriptor of an object. As said above, it depends on target. In the example for quick start all rules are strict. It means that each them describes full name that used for a target. 

The sequence of declaration of strict rules does not affect behavior. If action matches strict rule it will perform action declared in the rule.

###### Patterns

Pattern rules are strings with special symbols and syntax that allows to apply an action for variety of game objects. The order of declaration of patterns affects behavior. 

The special symbols are:
- `*` - any number of symbols 
- `&&` - boolean AND that combines two rules

The special modifier is:
- `@activated` - the pattern after this modifier matches activated item. The modifier works only as second rule. Placing it as the first rule doesn't work.

The special rules is:
- `default` - apply default or alternative action for object if the other rules have no matches 
- `default&&@activated[pattern]` - apply default action for object if the other rules have no matches. It depends on activated item

**Examples of patterns**

The provided examples used target `hover` and mockup actions `1|1`.

```properties
# The rule declares that hovered object could have any beggining but should ends with `stump`
# Examples: `oakenwood stump`, `willow stump`, etc. 
# Default action is 1 and alternative action is 1
hover.*stump=1|1

# The rule declares that hovered object could have any ending but should starts with `Pile`
# Examples: `Pile of logs`, `Pile of clay`, etc. 
# Default action is 1 and alternative action is 1
hover.Pile*=1|1

# The rule declares that hovered object should contains word `horse`
# Examples: `Venerable horse, Gray`, `Aged horse, White`, etc. 
# Default action is 1 and alternative action is 1
hover.*horse*=1|1

# The rule declares that hovered object should have name that starts with `small` and ends with `cart`
# Examples: `small oakenwood cart`, `small willow cart`, etc. 
# Default action is 1 and alternative action is 1
hover.small*&&*cart=1|1

# The rule declares that hovered object should be dirt tile and if `shovel` is activated, that the rule will match
# Examples: `Dirt`, `Dirt (flat)`, etc. 
# Default action is 1 and alternative action is 1
hover.Dirt*&&@activatedshovel=1|1

# The rule declares that hovered object should be a horse and if any sword is activated, that the rule will match
# Examples: `Venerable horse, Gray`, `Aged horse, White`, etc. 
# Default action is 1 and alternative action is 1
hover.*horse*&&@activated*sword=1|1

# The rule declares that if the other rules failed and the shovel is activated now, then the action will be performed
# Examples: `Cobblestone`, `Grass`, etc. 
# Default action is 1 and alternative action is 1
hover.default&&@activatedshovel=1|1

# The rule declares that the actions will be performed on hovered objects if all other rules fail 
# including defaults with activated items 
# Examples: `Cobblestone`, `Grass`, etc. 
# Default action is 1 and alternative action is 1
hover.default=1|1
```

##### Summary, features, limitations of config file

- you could write target rules in any order and do not sort it
- the order of declaration of strict rules does not affect behavior
- the order of declaration of patterns affects behavior. The first matched pattern has higher priority, keep correct patterns order
- the rule `default` has lower priority than the other rules 
- the rule `default` has lower priority than `default&&@activated`
- the rule `default` is only rule that could be a first part of pattern `&&`
- the declaration of strict rule as the first part of `&&` pattern doesn't work. You have to add `*` in correct places to make it working
- the config file has only one option `runtime_update` that does not rely to target rules. The option allows to update strict rules from matched patterns in runtime. It has next logic: if `runtime_update=true` and mod found a match in pattern that doesn't use `@activated` modifier, then list of strict rules will be populated by this match. It allow to avoid repeated searches in patterns. Known limitation is that the runtime update could not work correctly if config contains several similar rules will fallback:
```properties
hover.*horse*&&*Gray=2|2
hover.*horse*=1|1 
``` 
In that case, if `runtime_update` is enabled, then the first match will be used for all other action calls. 
- the performance of pattern matching didn't investigated. The suggestion about performance is next: better to use less patterns and more strict rules to have quick response 

### Target sections behavior

#### Hover 

**Tested.**

The most complex target because it has non intuitive things that are dictated both game logic and implementation. 

**Behavior.** Performs an action on hovered object or on hovered item in container window. 
- for hovered object full hovered name is used to check in pattern. It's important because hovered name could have very different structure and contains special symbols. Some external mods could change hovered names
- for hovered items in container window the mod matches name of activated item. The reason of that is in game logic, it is that hovered item in container does not produce hovered name

**Motivation.** In real world, when we're looking on some object we know the actions that we could do with that object and we have some preffered actions. It is important that if we have some tool (object) in our hands we also know how to apply the tool on object. 

**Examples.** 
```properties 
# if we hover any grass tile we prefer to forage it, the alternative is botanizing
hover.Grass*=223|224
# we could hover a pelt object on a floor and then the character will try improve it if hovered 
# but real usage is next:
# if pelt is activated, then try to improve a hovered item in some opened window, the alternative is repairing
hover.*pelt*=192|162
```

#### Activated

**Tested.**

Applies an action to an activated item in your invertory/backpack/character window.

**Behavior.** The action matches only base name of activated item. It does not use description after comma symbol. For example, if you have iron and steel item in your invertory, then the action will be the same.  

Since it works with activated item, then it has no sense to use pattern `&&@activated`. 

**Motivation.** If we prepare some item, then we know what we prefer to do with it. 

**Examples.** 
```properties 
# if we activate water, then we prefer to drink it, alternatively pure on ground 
activated.water=183|7
# if activated any sword, then we prefer to repair it (in the most of the cases it will be character window) 
# alternatively we equip it (invertory)
activated.*sword=162|582
```

#### Selected

Applies an action to an selected item in game world. It doesn't work with selected item in some container.

**Tested.**

**Behavior.** The action matches full name of selected item. It's important because selected name could have very different structure and contains special symbols. Some external mods could change selected names  

**Motivation.** If we select some item, then we know what we prefer to do with it and how apply a tool to it. 

**Examples.** 
```properties 
# if we select some bush, then we prefer to pick sprout, alternatively cut it down 
selected.*bush*=187|96
```

#### Toolbelt

**Tested.**

Selects a tool in toolbelt according to a hovered object.

**Behavior.** The action matches full name of hovered item and selects tool from toolbelt. As described in hover target, there is possible situation where you hover some window and there is no hovered object. In that case the selected item is used for determining required tool. Be care about names. Available range of values is 1-10 (toolbelt slots that is depends on quality of toolbelt).

**Motivation.** When we see an object, then we know what tool we could use for it. 

**Examples.** 
```properties 
# lets suggest that tool #1 is hatchet and tool #2 is sickle. Than if hover a tree
# then by default we get hatchet, othewise sickle 
toolbelt.*tree*=1|2
```

#### Body 

Applies action to body item. 

**Didn't tested because not found way to test it.**

**Behavior.** Matches body item name as in original mod and applies action. 

**Motivation.** Motivation is the same. We're applying correct action to body item. 

**Examples.** 

Not available. 

#### @Tb, @Eq, @Nearby 

Applies action to toolbelt item in some slot (@tb), equiped item in some slot (@eq) or to objects in radius determined by number (@nearby). 

**Partially tested.**

**Behavior.** The available keys is only `default` and `default&&@activated`.  

**Motivation.** Sinse the target determines only slots/radius, then we have only available default actions for it. To vary the behavior we can use `&&@activated` pattern. 

**Examples.** 

```properties 
# repaire the tool in slot #1 by default, otherwise examine it
@tb1.default=162|1

# repaire the character window item in slot #1 by default, otherwise examine it
@eq1.default=162|1

# cut down objects in radius 2m if hatchet is activated, otherwise examine it
@neaby2.default&&@activatedhatchet=96|1
```

#### Area, tile, tile_n, tile_ne, tile_nw, tile_s, tile_se, tile_sw, tile_e, tile_w 

Applies action area around (9 tiles), or to selected tile. 

**Didn't tested.**

**Behavior.** The available keys is only `default` and `default&&@activated`.  

**Motivation.** Game provides no known way to determine tile on which character is standing and we cannot depends on hovered behavior because it has more variations. To vary the behavior we can use `&&@activated` pattern. 

**Examples.** 

```properties 
# cut down all things around, otherwise examine it 
area.default=96|1

# pick sprout on current tile if sickle is activated, otherwise if sicle is activated prune it 
tile.default&&@activatedsickle=187|373

# pick sprout on current tile in nord to character tile if sickle is activated, 
# otherwise if sicle is activated prune it 
tile_n.default&&@activated=187|373
```

### Examples of usage  

Take into account that the examples will work when option `runtime_update=false` (default value). 

#### Switching of tools by single keybind 

To have such behavior we can setup two rules: fallback and matching activated tool.

The example works with toolbelt with two slots.
```properties 
# we activate sickle in cases when we're hovering a tree and hatched already activated
toolbelt.*tree*&&@activatedhatchet=2|1
# we activate hatchet in slot #1 when hovering some tree 
# in any case except that the tool is already activated 
toolbelt.*tree*=1|2
```

#### Perform correct action depending on activated item 

To have such behavior we can setup many rules, at least it shold have one rule.

```properties 
# single action (digging) that will be activated if only shovel is activated
hover.Tar*&&activatedshovel=144|1

# cut down selected tree if it selected and hatchet is activated 
selected.*tree*&&@activatedhatchet=96|1
# pick sprout from selected tree if it selected and sickle is activated, otherwise prune it 
selected.*tree*&&@activatedsickle=187|373
# in any other case forage or botanize this tile
selected.*tree*=223|224 
```

#### Combination of selected item and hovered object in window 

Above described case where activated item is used for improving/repairing some item in a window. It's good example. The other important usage of this behavior is healing because character window allows to heal wounds by hovering it. The healing item should be activated.

```properties 
# make first aid with cotton if wound is hovered and cotton is activated
hover.cotton=196|1
# make treating with healing cover if wound is hovered and healing cover is activated
hover.healing\ cover=284|1
```
#### Drinking from container 

Above described case you have to activate water to drink. Some containers allow to drink from it, so it is better to use them instead of water itself. There are possible other examples where container is prefferable to items inside, the author doesn't know them.

```properties 
# if water skin is activated, drink from it, otherwise repair it 
activated.water\ skin=183|162
```