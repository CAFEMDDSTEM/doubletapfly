# Double Tap Fly（双击空格飞行 + 快速放置）

**纯客户端** NeoForge 模组。服务器**不需要**安装任何东西。

- Minecraft：**26.1.x**（编译目标 26.1.2）
- NeoForge：**26.1.2.76**（要求 `[26.1.2.76,)`）
- Java：**25**
- 模组 ID：`doubletapfly`
- 版本：**1.8.0**

## 功能一览

- **双击空格飞行** —— 和创造模式完全一样的物理。
- **快速放置** —— 解除原版每 4 tick 才能放一个方块的冷却，可选一次放多个。
- **悬空放置** —— 对着空气也能放方块，选中位置用红色方框标出。
- **快速使用** —— 吃东西 / 扔末影珍珠 / 扔药水更快，和放方块分开控制。
- **玩家坐标** —— 在聊天栏列出其他玩家的坐标和离你的距离，坐标**点击即可复制**。
- **传送** —— `!tp` / `!tp 玩家名` / `!tp x y z`，直接改本地玩家坐标。**注意原版服务端只接受约 10 格以内的单包移动**，详见下文。
- **可自定义按键** —— 「选项 → 控制 → 按键控制」里多出「双击空格飞行」分类。
- **指令为主** —— 所有设置都能用聊天指令改和查：`!doubletapfly help` / `status`、`!fastplace`、`!config ...`。
- **原生设置界面** —— 用的是 NeoForge 自带、由配置自动生成的 Minecraft 原生界面（不是自制 GUI），也可以随时用指令解决。
- **彩色提示** —— 开启绿色、关闭红色，直接显示在聊天框。
- **防护** —— 飞行防摔落伤害；配合投影建造很省心。

## 一、双击空格飞行

原版客户端本来就写好了完整的「创造模式飞行」，缺的只是 `abilities.flying` 这个权限位
（生存模式永远是 `false`）。本模组在客户端把它打开，服务端只会看到一个「在正常移动」
的玩家。

> **服务器必须设置 `allow-flight=true`**（`server.properties`）。否则连续悬空 4 秒
> 会被原版踢出。如果确实改不了，可以打开配置里的 `anti_kick` 兜底。

## 二、快速放置（配合投影模组）

原版在 `Minecraft#startUseItem()` 里写死了 `rightClickDelay = 4`，也就是**每秒最多放 5 个方块**。
这就是投影铺方块慢的根源。本模组做了两件事：

1. **解除冷却**：`place_delay` 默认 1，也就是每 tick 都能放一次（约 20 个/秒，4 倍速）。
2. **一次多放**（可选）：`blocks_per_tick` 大于 1 时，会以准星命中的面为中心，
   把周围同一平面上的方块一起补上，最多补到设定数量。

这两条和投影是天然配合的：投影告诉你哪里要放，快速放置负责放得快。
如果你同时开着 Litematica / Forgematica 的**简易放置（Easy Place）**，
每次放置还会自动取用正确的方块和朝向。

### 关于投影模组本身（重要）

投影模组 **Litematica 本体只有 Fabric 版**，和这个 NeoForge 模组装不到同一个客户端里。
NeoForge 上要用它的非官方移植：

| 模组 | 作用 | 地址 |
|---|---|---|
| **Forgematica** | Litematica 的 (Neo)Forge 移植，支持 26.1.x | https://modrinth.com/mod/forgematica |
| **MaFgLib** | Forgematica 的前置库 | https://modrinth.com/mod/mafglib |

把这两个和本模组一起放进 `mods/` 就能同时用投影 + 飞行 + 快速放置。

## 三、悬空放置

对着空气按住右键，会在你**能摸到的最远处**放一个方块 —— 不需要旁边有参照方块。

原理不复杂：原版判断放置位置时，会先看准星指着的那个方块**能不能被替换**（`canBeReplaced`），
能替换就把方块放进它自己那一格，不能才放到相邻格。空气永远可以被替换，所以只要把「准星
打空了」这件事变成「准星指着终点那一格」，服务端就会照常接受这个放置。

开启后，待放置的位置会画一个**红色方框**（`AirPlaceRender = false` 可以关掉）。
默认是关闭的，用 `!airplace` 或 `!config AirPlace = true` 打开。

### 怎么用（步骤）

1. **先打开它** —— 默认是关的。聊天框输入 `!airplace`，看到绿色的「悬空放置：已开启」才算开。
   也可以在「选项 → 控制 → 按键控制」里给「开关悬空放置」绑一个键，或者在设置界面里勾。
2. **手上拿方块**（`BlockItem`；刷怪蛋、盔甲架、烟花也可以）。空手或者拿食物之类的不生效。
3. **准星对着空气** —— 也就是前方没有可点击的方块，准星打在虚空/天空里。
   （如果准星指着方块，那就是普通放置，走原版逻辑。）
4. 这时你能触及的最远处那一格会出现**红色方框**，就是你这次要放的位置。
   没看到框说明条件没满足：要么开关没开、要么手上不是方块、要么准星其实指着东西。
5. **按住右键**，方块会放到红框那一格。

配合快速放置（默认开着）手感会好很多：准星扫过去，一次能铺一串。

## 四、快速使用

原版的 4 tick 冷却不只限制放方块，也限制**使用物品** —— 食物、药水、末影珍珠、雪球等等
用完后都要等 4 tick。这一项把那段冷却缩短，`FastUseCooldown` 默认 0（最快）。

它和快速放置是两个独立开关：`!fastplace` 管放方块，`!fastuse` 管用物品，互不影响。

## 五、玩家坐标

输入 `!players`（或按你绑的「输出玩家坐标」键），聊天栏会列出其他玩家：

```
[双击空格飞行] 玩家坐标（3 人，按距离从近到远）
  Steve · 12.3 格 · 128 64 -512
  Alex · 45.8 格 · 140 70 -480
[双击空格飞行] 点击名字或坐标即可复制。…
```

**名字和坐标都可以点一下直接复制**（悬停会提示），复制出来的是 `128 64 -512` 这种纯坐标，
方便你粘到指令里。

两点限制要说清楚：客户端只能拿到**同维度**、且**在服务端追踪范围内**（原版默认是玩家周边
几十格，服务器可调）的玩家实体，超出这个范围的玩家客户端根本不知道他们在哪，所以列不出来。

## 六、传送（!tp）

```
!tp                  → 传送到预设点（HomeX / HomeY / HomeZ）
!tp Steve            → 传送到玩家 Steve 的位置
!tp 128 64 -512      → 传送到指定坐标
```

实现就是直接改本地玩家坐标（`setPos`），客户端下一个 tick 会把新位置照常打包发给服务端。

> ### ⚠️ 重要限制：原版只认约 10 格以内的单包移动
>
> `ServerGamePacketListenerImpl#handleMovePlayer` 里有这么一段：
>
> ```java
> double movedDist = xDist*xDist + yDist*yDist + zDist*zDist;   // 平方距离
> float metersPerTick = isFallFlying ? 300.0F : 100.0F;
> if (movedDist - expectedDist > metersPerTick * deltaPackets) {
>     LOGGER.warn("{} moved too quickly! ...");
>     this.teleport(player.getX(), player.getY(), player.getZ(), ...);   // 拉回原位
>     return;
> }
> ```
>
> `movedDist` 是**平方**距离，阈值却只有 `100` —— 所以真正允许的是 `sqrt(100) = 10 格`。
> 超过这个距离，服务端会记一条 `moved too quickly` 并**把你拉回原地**。
>
> **并且 `allow-flight=true` 关不掉这个校验** —— 它只管「悬空超过 4 秒踢人」那一条，
> 跟移动距离校验是两个独立的检查。服务端只要还是原版（或 Paper/Spigot），这条就一定在。
>
> 所以默认情况下：**近距离（约 10 格内）能过，远距离会被拉回**。模组会在距离超阈值时于聊天栏
> 给出黄色提示（阈值可用 `!config TeleportWarn = 100` 调整，0 关闭）。

### ✅ 解决办法：一条原版游戏规则

那整段距离校验外面还包着一层开关：

```java
private boolean shouldCheckPlayerMovement(boolean isFallFlying) {
    ...
    GameRules gameRules = this.player.level().getGameRules();
    return !gameRules.get(GameRules.PLAYER_MOVEMENT_CHECK) ? false : ...;
}
```

而 `player_movement_check` 是原版 26.1 的正式游戏规则（`GameRules.PLAYER_MOVEMENT_CHECK`，
默认 `true`）。在服务端执行：

```
/gamerule player_movement_check false
```

`shouldCheckPlayerMovement()` 会直接返回 `false`，**整段 moved-too-quickly 校验被跳过**，
`!tp` 就能跨任意距离生效，也不会有服务端警告日志。这条：

- 是**纯原版功能**，不需要插件、不需要数据包、不需要模组
- 写在存档的 `level.dat` 里，**立即生效、不用重启**
- 和 `allow-flight` 是两码事（后者只管「悬空超过 4 秒踢人」）

关掉之后模组那个黄色距离提示就没意义了，用 `!config TeleportWarn = 0` 关掉即可。

唯一剩下的限制：**传送到实心方块内部**仍然会被拒（走的是另一条 `moved wrongly` /
`isEntityCollidingWithAnythingNew` 的碰撞检查，和这个 gamerule 无关），目标点要落在空气里。

## 安装

1. 玩家安装 NeoForge **26.1.2.76**（或更高的 26.1.x）。
2. 把 `doubletapfly-26.1.2-1.8.0.jar` 放进 `.minecraft/mods/`。
3. 进入服务器，双击空格即可飞行；按住右键就是快速放置。

## 按键设置

**选项 → 控制 → 按键控制** 里会多出一个分类「双击空格飞行」：

| 按键名 | 默认值 | 作用 |
|---|---|---|
| 开关双击空格飞行 | 右 Shift | 打开 / 关闭飞行功能 |
| 开关快速放置 | 未绑定 | 打开 / 关闭快速放置 |
| 开关悬空放置 | 未绑定 | 打开 / 关闭悬空放置 |
| 开关快速使用 | 未绑定 | 打开 / 关闭快速使用 |
| 输出玩家坐标 | 未绑定 | 按下就在聊天栏打印玩家坐标列表 |

点一下就能重新绑定成任意按键（和原版按键完全一样的操作）。

## 聊天指令

在聊天框里直接输入即可，**指令只在本地处理，不会被发到服务器**。
指令前面有 `!`，末尾可以带分号，大小写不敏感。

不打开任何界面，全部靠聊天框：

| 指令 | 作用 |
|---|---|
| `!help` | 列出全部功能 |
| `!help <功能名>` | 某个功能的详细介绍和使用步骤 |
| `!doubletapfly` | 开关飞行（绿色 = 已开启，红色 = 已关闭）|
| `!doubletapfly help` | 在聊天框打印完整指令表 |
| `!doubletapfly status` | 打印当前所有设置（开/关用绿/红标出）|
| `!doubletapfly gui` | 打开设置界面（和模组列表里那个「配置」是同一个）|
| `!help` | 列出全部功能 |
| `!help <功能名>` | 某个功能的详细介绍 + 使用方法 |
| `!fastplace` | 开关快速放置（绿色 / 红色）|
| `!airplace` | 开关悬空放置（绿色 / 红色）|
| `!fastuse` | 开关快速使用（绿色 / 红色）|
| `!players` | 打印已知玩家的坐标和距离 |
| `!tp` | 传送到预设点 |
| `!tp <玩家名>` | 传送到该玩家 |
| `!tp <x> <y> <z>` | 传送到指定坐标 |
| `!config Flight = true` | 飞行总开关（等同 `!doubletapfly`）|
| `!config FlySpeed = 1` | 飞行速度倍率（0.1 ~ 10，1 = 原版创造速度）|
| `!config DoubleTapWindow = 7` | 双击判定窗口 tick（2 ~ 20）|
| `!config FallProtection = true` | 飞行时免摔落（true / false）|
| `!config FastPlace = true` | 快速放置开关（也可写 `false` / `on` / `off`）|
| `!config PlaceDelay = 1` | 两次放置之间等几 tick（0 ~ 4，原版是 4）|
| `!config PlacePerTick = 4` | 每 tick 最多放几个方块（1 ~ 12，默认 1）|
| `!config AntiKick = true` | 反踢开关（服务器没开 allow-flight 时用）|
| `!config AntiKickInterval = 40` | 反踢间隔 tick（10 ~ 75）|
| `!config AirPlace = true` | 悬空放置开关（等同 `!airplace`）|
| `!config AirPlaceRender = true` | 是否画红色提示框（true / false）|
| `!config FastUse = true` | 快速使用开关（等同 `!fastuse`）|
| `!config FastUseCooldown = 0` | 使用物品后的冷却 tick（0 ~ 4，原版是 4）|
| `!config PlayerListRange = 0` | 只列出这个距离内的玩家（0 ~ 512，0 = 不限）|
| `!config PlayerListSelf = false` | 列表中是否包含自己（true / false）|
| `!config HomeX / HomeY / HomeZ = 0 64 0` | `!tp` 不带参数时去的预设点 |
| `!config TeleportWarn = 10` | 超过多少格就在聊天栏警告（0 = 不警告）|

数值超出范围会自动收进范围并给黄色提示；指令写错会给红色错误，并提示你输入 `!doubletapfly help`。
所有设置都会立刻生效并写回配置文件。

## 设置界面

不想敲指令时，有两个入口打开设置界面：

1. **模组列表** → 找到 Double Tap Fly → 点「配置」按钮（NeoForge 的标准入口）。
2. 聊天框输入 `!doubletapfly gui`。

这个界面是 **NeoForge 自带的 `ConfigurationScreen`** —— 由配置定义自动生成，控件全是
Minecraft 原生的开关 / 滑块 / 列表，中英文标签和提示都做了翻译，没有一行自制 GUI 代码。

## 配置文件

`.minecraft/config/doubletapfly-client.toml`：

```toml
[general]
    # 飞行总开关
    enabled = true
    # 两次空格之间允许的间隔（tick）。原版创造模式是 7 tick。
    double_tap_window = 7
    # 飞行速度倍率，1.0 = 原版创造速度
    fly_speed = 1.0

[fall_damage]
    # true  = 飞行时不会摔伤（和创造模式一致）
    # false = 服务端会照常累计下落距离，飞完落地会摔伤
    protection = true

[anti_kick]
    # 只在服务器 allow-flight=false 时才需要打开
    enabled = false
    interval = 40

[fast_place]
    # 快速放置总开关
    enabled = true
    # 两次放置之间等几 tick。原版 4，1 = 每 tick 一次
    place_delay = 1
    # 每 tick 最多放几个。1 = 只放准星指着的那个（最稳）
    # 调大之后会顺带把准星周围同一平面的方块一起补上
    blocks_per_tick = 1
```

## 关于摔落伤害

服务端靠客户端上报的位置自己累计下落距离，只有「向上移动」或「声称站在地上」才会清零。
开启 `protection`（默认）后，模组会在这段距离接近「安全摔落高度」之前，让**那一 tick**
的移动包报告 `onGround = true`，服务端按正常逻辑算出 0 伤害并清零，飞行不受影响。

停止飞行后从空中掉落，仍然会正常受到摔落伤害——和生存模式一样。

## 自己编译

```bash
# 需要 JDK 25
./gradlew build
# 产物：build/libs/doubletapfly-26.1.2-1.8.0.jar
```

开发环境里启动客户端：

```bash
./gradlew runClient
```

## 目录结构

```
build.gradle.kts                                构建脚本（ModDevGradle 2.0.140）
gradle.properties                               版本 / 模组元数据
src/main/resources/META-INF/neoforge.mods.toml  模组描述
src/main/resources/doubletapfly.mixins.json     Mixin 配置
src/main/resources/assets/doubletapfly/lang/    中英文案（按键名、提示语）
src/main/java/com/fluxt/doubletapfly/
    DoubleTapFlyMod.java                        模组入口（仅客户端）
    DoubleTapFlyConfig.java                     配置读写
    ClientFlightController.java                 双击检测 / 飞行状态 / 速度 / 防摔落
    FastPlaceHelper.java                        快速放置：冷却覆盖 + 周围补方块
    KeyBindings.java                            可在按键控制里修改的快捷键
    ChatCommands.java                           !doubletapfly / !fastplace / !config
    ChatFeedback.java                           带颜色的聊天提示
    mixin/LocalPlayerMixin.java                 上报 onGround 的注入点
    mixin/MinecraftMixin.java                   覆盖原版 4 tick 放置冷却
```

## 注意

这是给**你自己的服务器**用的。在别人的服务器上使用同类客户端功能前，
请先确认对方的规则允许。
