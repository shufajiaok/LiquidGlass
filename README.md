# LiquidGlass for Jetpack Compose

**真·液态玻璃**：一个 Compose 组件，自己模糊自己**身后**的那一片背景，并带折射、色散和高光边。

> Real backdrop blur + refraction + dispersion for Jetpack Compose.
> No third-party dependency, no shader file — just Compose + `RenderEffect`.

**它和"半透明色块"的区别**：不是给容器调个 alpha，而是把身后的背景**重画一遍**、做真 GPU 模糊、
再沿轮廓做位移与色散。所以背景保持清晰，只有玻璃内部那一片是糊的。

- 不依赖任何三方 UI 库（不用 haze、不用 RuntimeShader，也不用 AGSL 文件）
- 纯 Compose 自绘，**零 KSP / 零 kapt**
- API 31+ 有真模糊；低版本自动退化成「半透明膜层 + 边缘光」，不会崩

---

## 快速开始

> **先跑起来看看**：clone 下来直接跑 `:sample` 模块 —— 一张玻璃卡片 + 一块**能拖动的玻璃** +
> 一个玻璃按钮 + 一个折射强度滑块。背板是代码画出来的（渐变 + 色块），**不需要任何图片资源**。

### 1. 引入

**方式 A：源码方式**（最省事，推荐先这么试）

把 `liquidglass/` 这个目录拷进你的工程，然后：

```kotlin
// settings.gradle.kts
include(":liquidglass")

// app/build.gradle.kts
dependencies { implementation(project(":liquidglass")) }
```

**方式 B：JitPack**（仓库已发布在 [github.com/shufajiaok/LiquidGlass](https://github.com/shufajiaok/LiquidGlass)）

> JitPack 是**按需构建**的：第一次用它会在云端现编一次，所以要先去
> [jitpack.io/#shufajiaok/LiquidGlass](https://jitpack.io/#shufajiaok/LiquidGlass) 点一下
> Look up，等它把 0.1.2 那条变成绿色再依赖。之后就是缓存好的了。

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven("https://jitpack.io")
    }
}

// app/build.gradle.kts
dependencies { implementation("com.github.shufajiaok.LiquidGlass:liquidglass:0.1.2") }
```

### 2. 铺背板

玻璃要有东西可模糊。用 `GlassBackdrop` 铺一层背板，它顺便把背板信息喂给下面所有的玻璃：

```kotlin
setContent {
    MaterialTheme {
        GlassBackdrop(image = rememberBackdropImage(File("/sdcard/bg.jpg"))) {
            // ⚠️ Scaffold / TopAppBar / NavigationBar 的容器色都要显式设成透明，
            //    否则它们会把背板盖住（玻璃就没什么可模糊的了）
            Scaffold(containerColor = Color.Transparent) { padding -> /* 你的内容 */ }
        }
    }
}
```

### 3. 放一块玻璃

```kotlin
LiquidGlassSurface(
    modifier = Modifier.size(240.dp, 120.dp),
    shape = MaterialTheme.shapes.large,
) {
    Text("Hello", Modifier.align(Alignment.Center))
}
```

需要点击就用 `onClick = { ... }`（它自带 `clickable` + 涟漪）。

**想让参数全局可调**（比如接到设置页的滑块上）：

```kotlin
CompositionLocalProvider(
    LocalGlassParams provides GlassParams(transparency = 0.45f, dispersion = 0.5f)
) {
    // 这棵子树里所有玻璃一起变
}
```

---

## 基础组件

| 组件 | 作用 |
|---|---|
| **`LiquidGlassSurface`** | 玻璃本体。`shape` / `blurRadius` / `edgeWidth` / `onClick` / `enabled` |
| **`GlassBackdrop`** | 铺满一张背板图 + 一层遮罩，并通过 `LocalBackdrop` 把背板喂给玻璃 |
| **`rememberBackdropImage(file)`** | 从文件读背板图（`remember` 住，不会重复解码） |
| **`GlassParams` / `LocalGlassParams`** | 六个可调参数，一次下发全树生效 |
| **`RefractMode`** | 两种折射做法（`SCALE` / `FIELD`） |
| **`GlassDefaults`** | 默认值、推荐区间、小控件的推荐模糊半径与边缘光宽度 |
| **`Backdrop` / `LocalBackdrop`** | 背板契约。**自己实现背板就实现它**（见下） |

### 自己提供背板

背板不需要是一张图 —— 任何能提供 `ImageBitmap + 尺寸 + 位置 + 遮罩浓度` 的东西都行：

```kotlin
CompositionLocalProvider(
    LocalBackdrop provides Backdrop(image = myBitmap, size = IntSize(w, h), origin = Offset.Zero, scrim = 0.4f)
) { /* 玻璃放这儿 */ }
```

> ⚠️ 这条路的**前提是"背板是静态的"**。要模糊的是滚动的内容（列表从玻璃下面滚过去），
> 就得换成图层捕获那一类方案（haze 的做法），`Backdrop` 和 `BackdropSlice` 要重写。

---

## 参数

`GlassParams` 的六项都能实时改（[`GlassDefaults`](liquidglass/src/main/java/com/jiale/liquidglass/GlassDefaults.kt) 里给了推荐区间）：

| 参数 | 默认 | 说明 |
|---|---|---|
| `transparency` | 0.38 | 通透度。**膜层浓度 = 1 − 它**。玻璃上要压正文时别超过 0.5 |
| `refractDp` | 5.0 | 折射强度，单位是**轮廓处的位移量（dp）** |
| `refractMode` | `SCALE` | `SCALE` 整体放大（最省）/ `FIELD` 边缘折射（更好看） |
| `edgeWidthDp` | 2.0 | 边缘光带宽度。**越宽整体越淡**，宽度大时用 `edgeGlow` 补亮度 |
| `edgeGlow` | 1.0 | 边缘光亮度倍率（0.4–2.5） |
| `dispersion` | 0.35 | 色散（色差）强度 |
| `legacyBackdrop` | false | **复刻 0.5.1 的"错位"写法**，见下一节 |

**模糊半径和边缘光宽度是 `LiquidGlassSurface` 的入参，不是全局参数** —— 因为它们必须跟着控件尺寸走：

```kotlin
// 小控件（按钮 ≈ 40dp、指示器 ≈ 32dp）要用收窄的那套
LiquidGlassSurface(
    modifier = Modifier.height(40.dp),
    shape = RoundedCornerShape(percent = 50),
    blurRadius = GlassDefaults.COMPACT_BLUR,        // 6dp，不是 12dp
    edgeWidth = GlassDefaults.COMPACT_EDGE_WIDTH,   // 1.2dp
) { /* ... */ }
```

> **这条不是小事**：半径一旦接近控件高度，玻璃里就只剩一团均匀色块。
> 12dp 的模糊放在 32dp 高的胶囊上，里面什么都看不出来。

---

## `legacyBackdrop`：0.5.1 的那种"错位"（默认关）

**这不是折射，是错误对齐 —— 但看起来很像光斜着进来。**

打开之后，节点改用 `Modifier.size`（它**服从父级约束**）→ 被夹成**组件尺寸**，出血完全不生效：

```kotlin
// 关（现在）：节点真的比组件大一圈出血，模糊能在界外采到约 3σ 的真实邻居
Modifier.offset(x = -bleedDp, y = -bleedDp).requiredSize(组件尺寸 + 2×出血)

// 开（0.5.1）：size() 服从父级约束 → 节点被夹成组件尺寸
Modifier.offset(x = -bleedDp, y = -bleedDp).size(组件尺寸)
```

**两件事同时发生**（这也是为什么它没法用"平移一点"来近似）：

1. **坐标换算仍按"组件 + 2×出血"算**（`dstOffset`、`transformOrigin` 都是），而节点只有组件那么大
   → 内容整体往**左上**偏一个 bleed；
2. 节点**覆盖不到玻璃的右下角**（整整差一个 bleed）→ 那条带上**没有背板内容**，只剩膜层。

合起来读作"光从左上斜着进来"。这个观感在早期版本里是**免费**得到的（那正是一个 bug 的副作用），
修掉对齐之后它就没了 —— `FIELD` 只在贴边 12.5dp 一条带里折、中心严格不动，观感会明显"变平"。

⚠️ **代价**：滚动/拖动时玻璃里的内容会和外面脱节，右下角会露出一块只有膜层的区域。
它和 `SCALE` / `FIELD` **正交**（只改节点的测量尺寸，不动任何位移场），所以是个独立开关。

> **一条经验**：复刻一个"效果好但不正确"的行为时，**复刻它的写法，别做等效近似**。
> 这里试过"只在 `dstOffset` 上加个偏移量"，结果只有①、没有②，完全不像。

## 它是怎么做的

四件事，缺一件就不像玻璃：

### 1. 重画背板并模糊

Compose 的 `Modifier.blur` 只能模糊**节点自己的内容**，模糊不了"身后"。所以这里按
`ContentScale.Crop` 的同一套缩放与偏移把背板图再画一遍，**只截取本控件周围那一小块**，
再对它做 `BlurEffect`（`RenderEffect`，真 GPU 模糊）。

只截一小块不是优化偏好而是必须的：按整屏尺寸建模糊图层，每帧要处理 260 万像素。
节点尺寸 = 控件 + **1.8 倍模糊半径**的出血 —— 模糊要在边界**外**采到约 3σ 的真实邻居像素，
不然边缘会被 clamp 采样抹出一条假边，背景一复杂、控件一移动那圈就会闪。

### 2. 透镜感（位移）

把背板**放大 5% 上下**再显示，位移就等于"缩放增量 × 到中心的距离"。两种做法：

| 做法 | 位移场 | 成本 |
|---|---|---|
| `SCALE` 整体放大 | 线性，**中场也在动**（中心为 0、边缘最大） | **1 次绘制** |
| `FIELD` 边缘折射 | 只在贴边一条带里，内圈严格不动，沿带 `smoothstep` 衰减到 0 | 3–6 次带裁剪绘制 |

`FIELD` 的几何用 `Shape.createOutline` **逐层内缩**切环，不用椭圆环——椭圆和外接矩形在角上对不齐，
带会横穿圆角。位移量按两个方向**分别折算**（÷ 半宽 / 半高），所以长边短边的位移量一致。

### 3. 边缘（玻璃和"半透明色块"的分界全在这儿）

- **顶部反射白雾**：绝对高度 12dp，不是按比例（按比例在小控件上会把上半整片糊住）
- **内缘亮带**：紧贴上下边缘 3dp / 2dp 的窄渐变。这是"显厚"的关键——厚度不在正面在侧壁
- **边缘光带**：由外到内三层叠出的渐变，不是 1dp 硬描边（实边读作"描了一道边"，那是塑料）

### 4. 色散（色差）

游戏后处理里那套 **chromatic aberration**：同一张背板画三遍，每遍只取一个通道
（R 往内收、B 往外偏 —— 蓝光折射率最高），再用 `BlendMode.Plus` 加性叠加。
位移只发生在贴边一条带里，完全重合的地方三遍加起来正好还原原值，所以画面整体不变亮，
只有错开的边缘才出彩边。

---

## 性能预算

**一个玻璃面每帧的绘制次数**（`drawImage` + `clipPath` 数）：

| 场景 | 次数 |
|---|---|
| 整体放大，无色散 | 1 |
| 整体放大 + 色散 | ≈ 10 |
| 边缘折射 + 色散 | ≈ 16 |

**真正的成本不在这张表里，而在"同屏有几块玻璃"** —— 每块每帧都要重画一遍背板 + 一次离屏模糊。
所以：

- 同屏 1–3 块小面（按钮、指示器）≈ 无感
- 一页 5 张卡片全都玻璃 → 明显掉帧

**快速估算**：玻璃面数 × 上表次数。想省，先减面数，再考虑降 `refractMode` 到 `SCALE`。

---

## 硬约束与踩过的坑

这些每一条都是真机上踩出来的，改代码前值得先看一眼。

### 玻璃内部

| 约束 | 为什么 |
|---|---|
| 出血节点必须用 `requiredSize` **并用 `Box(matchParentSize())` 包一层** | `size()` 服从父级约束，控件被 `height(40.dp)` 钉死时出血完全不生效（边缘闪）；直接当子节点又会把外层 Box 撑大 → 尺寸无限增长 |
| 位置/尺寸**只在 draw / layer 阶段读** | 在组合阶段读会让每次滚动都触发重组，玻璃里的背板慢半帧，看起来"内容在玻璃里游" |
| 不要改成"预模糊"（糊好存 bitmap） | 更快，但会把架构锁死在"背板 = 静态图"，堵掉后续演进路线 |
| 环与环**精确相邻**：不留缝也不重叠 | 三通道的覆盖集合必须严格一致：**缝**里只有 G 被画到 → 偏青绿；**重叠**处 R/B 多画一遍 → 偏品红。接缝那一两个亚像素的抗锯齿交给整层的模糊，比任何补丁都管用 |
| `Path.op` 只有**三参**成员 | 要写 `Path().apply { op(a, b, Op) }`，没有 `path.op(other, Op)` 这种两参重载 |

### 用的时候

| 现象 | 原因 |
|---|---|
| **对话框 / 底部弹层里的玻璃没有模糊** | 弹层是独立 window，`positionInRoot()` 和主窗口的背板对不上。组件会自动退化成"磨砂膜层 + 边缘光"（这是**设计如此**，不是 bug） |
| **玻璃里是一团均匀色块** | 模糊半径接近了控件高度。换 `GlassDefaults.COMPACT_BLUR`，或让控件大一点 |
| **低版本（API < 31）玻璃"没效果"** | 没有 `RenderEffect`。此时不做重画 —— 画一块**清晰**的背景在玻璃里反而像挖了个洞 |
| **玻璃边缘有一圈杂色** | 检查 `dispersion` 和环的几何：任何"三通道覆盖集合不一致"都会显成偏色 |

---

## 局限

- **背板必须是静态的**。要模糊滚动内容得换图层捕获方案（本项目没做）。
- **API 31 以下没有真模糊**，只有膜层 + 边缘光。
- 每块玻璃每帧一次离屏图层 + 一次 `RenderEffect`，**面积大且数量多时请谨慎**。
- 用的是 `MaterialTheme.colorScheme.surface` 作为膜层色，所以在 `MaterialTheme` 之外用会拿不到主题色。

## 版本基线

| 项 | 版本 |
|---|---|
| Kotlin | 2.0.20 |
| Compose BOM | 2024.09.00 |
| AGP | 8.7.3 |
| Gradle | 8.9 |
| compileSdk / minSdk | 35 / 26 |
| 运行时 | API 31+ 才有真模糊 |

## 从哪来

从个人项目 **HealthCheckIn**（0.7.4）的玻璃引擎里剥出来的，仓库在 [shufajiaok/LiquidGlass](https://github.com/shufajiaok/LiquidGlass)（那边还带一个「玻璃实验台」页面，
可以在真机上拖动一块玻璃、实时拖六个参数，这套数值就是那样调出来的）。

剥离时只做了两件事：把项目特有的配置常量换成 [`GlassDefaults`](liquidglass/src/main/java/com/jiale/liquidglass/GlassDefaults.kt)，
把背景实现从"文件路径"泛化成 `ImageBitmap`。**算法与经验注释都是原样保留的** ——
那些「为什么不这么做」的说明比代码本身值钱。

## 与 HealthCheckIn 的同步

这个库是从上游 App（HealthCheckIn）里剥出来的，**上游还在继续改**。两边是两份代码，
所以改完上游要手动同步过来 —— 下面这张表就是映射关系：

| 上游 App | 本库 | 差异 |
|---|---|---|
| `ui/common/LiquidGlass.kt` | `liquidglass/LiquidGlass.kt` | package、常量的来源（`AppSettings.*` → `GlassDefaults.*`）。**其余逐行一致** |
| `ui/common/ImageBackground.kt` | `liquidglass/GlassBackdrop.kt` | 参数从 `File` 泛化成 `ImageBitmap`；多了 `rememberBackdropImage` |
| `Models.kt` 里的 `RefractMode` | `liquidglass/RefractMode.kt` | 去掉了 `@Serializable`（本库不依赖序列化） |
| `Models.kt` 里的 `GLASS_*` 常量 | `liquidglass/GlassDefaults.kt` | 名字去掉前缀，数值与区间一致 |
| `AppSettings` + 设置页 / 实验页的滑块 | —— | 本库不含 UI 与持久化，只到 `GlassParams` |

**同步的做法**：把两边的注释与空行去掉后 diff 一次，只应该剩下上面那三类必然差异：

```bash
grep -vE '^[[:space:]]*(\*|//|/\*|$)' A/LiquidGlass.kt > a.txt
grep -vE '^[[:space:]]*(\*|//|/\*|$)' B/LiquidGlass.kt > b.txt
diff a.txt b.txt
```

出现别的差异就是漂移了，逐条搬过来。**改完两边都要各自编一次** —— 库这边
`:liquidglass:assembleDebug` 和 `:sample:assembleDebug` 都过，才说明 API 真的还能被调用。

## License

MIT
