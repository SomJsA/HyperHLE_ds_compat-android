# DEAD SPACE MOBILE · R5.1 / Android

## 本版更新

- **扩展游戏文字语言**：中文（贴吧汉化组）、English、Français、Deutsch、Italiano、한국어、Español。高清章节、目标、字幕与原生游戏语言同步；已有语言设置切换前自动备份并校验。当前内置原包的日语文本为空，日语选项会明确标注并使用 **English 回退**。
- **暂停界面语言一致**：只有“中文启动器 + 中文游戏”使用中文暂停页；启动器或游戏任一为非中文，暂停、贡献说明、字幕显示设置与预览、返回启动器/退出确认均使用英文。启动器界面与游戏文字仍独立选择和保存。
- **更新版本标识统一**：更新检查器直接读取构建发布标签，与包内版本使用同一 `20261116-r5.1`，修复旧 R5 检查器仍显示 R4、误报自身更新的问题。
- **增加可见的文件完整性检查**：首次启动以及手动检查会校验 APK、ZIP CRC/长度、必需资源和内置游戏资源指纹。可取消；失败时留在启动器，提供双语说明与详情复制。文件损坏仍需重新下载完整 APK 覆盖安装，无需关闭高清文字组件。
- **改善英文等语言的字幕断词**：保留原生字幕时序，按词边界换行，减少单词被拆到两行的情况。

### 下载版本

| 版本 | 纹理与默认光影 |
| --- | --- |
| DATA | 原版纹理；高级光影默认关闭 |
| HD | 内置 4× 高清纹理；高级光影默认开启 |

保留 R5 的自动内部分辨率和手动 1×–8× 选择。没有新增全场景帧率提升承诺。

## 如何选择语言

左侧进入 **01 高清文字 / 01 HD text** → 保持高清文字组件勾选 → 在 **高清语言 / HD language** 下拉框选择语言 → 启动游戏。右上角中文/English按钮只切换启动器界面。

下图是沿用 R5 的用户红圈标注原图，用于说明下拉框位置；**R5.1 可选语言比图中的 R5 更多**。

![R5 用户红圈 English 操作示意](https://github.com/SomJsA/HyperHLE_ds_compat-android/releases/download/20261116-r5.1/R5-language-switch-guide.jpg)

## 各语言实际开场字幕（仅截取字幕区域）

以下来自本轮 DATA/HD 实机新档案，不是字幕预览器；只裁剪字幕区域，没有调亮或生成游戏像素。每版8个选择项均确认了原生难度页和至少两条开场字幕，另完成两版英文启动器配中文游戏的交叉验证，共18组用例。日语一行明确展示英文回退，不代表日文翻译已完成。

**DATA**

![R5.1 DATA 多语言字幕裁剪](https://github.com/SomJsA/HyperHLE_ds_compat-android/releases/download/20261116-r5.1/R51-DATA-language-captions.jpg)

**HD**

![R5.1 HD 多语言字幕裁剪](https://github.com/SomJsA/HyperHLE_ds_compat-android/releases/download/20261116-r5.1/R51-HD-language-captions.jpg)

[下载16张单独字幕裁剪图与来源/裁剪记录](https://github.com/SomJsA/HyperHLE_ds_compat-android/releases/download/20261116-r5.1/R51-language-caption-crops.zip)。

## 尚待改进与已知问题

- 法、德、意、西的**原生难度菜单**少数重音字形显示不完整；本轮高清开场字幕显示正常。该原生字形问题尚未修复，根因仍需定位。
- 当前内置原包没有可用的日语译文，暂用英文回退。
- 本轮是语言与开场的有限验收。完整剧情、跨关、长时热稳定、所有武器/实体输入、可听音频及更多设备/GPU验证仍开放；不把本轮结果当作完整 R15 验收。

欢迎在 [Community Issues](https://github.com/SomJsA/DeadSpaceMobile-Community/issues) 反馈，并附上 DATA/HD、语言、设备型号、Android版本及截图或日志。

## What's new / English

R5.1 synchronizes native and HD text for Chinese, English, French, German, Italian, Korean and Spanish. The bundled Japanese resource has no translated values, so its selector explicitly falls back to English.

The pause screen, contributor text, subtitle controls/preview and return/exit confirmations are English whenever either the launcher UI or selected game text is non-Chinese. Launcher UI and game text remain separate choices.

The updater now reads the build's release tag, fixing the stale R4 identifier in the R5 checker. First launch and manual verification check the APK, ZIP CRC/lengths and bundled resource fingerprint. Verification is visible and cancellable; failures stay in the launcher with bilingual details. Damaged downloads require a complete replacement download, not disabling HD text. Word-boundary caption wrapping has also improved.

DATA retains original textures and defaults advanced lighting off; HD includes 4× textures and defaults advanced lighting on. Each package completed eight fresh-game language-selector checks, plus one English-launcher/Chinese-game cross-check: 18 cases total. The images above are actual cropped opening captions, with individual crops attached.

**Known issues:** some accented characters in the French/German/Italian/Spanish native difficulty menus render incompletely; the tested HD captions are correct. Japanese is an explicit English fallback. Full-game, long-run, audio/input and additional-hardware acceptance remain open.

## 校验与源码 / Checksums and source

两 APK 与已验收本地候选字节一致，均通过 CRC、签名、对齐和资源指纹核验。本版不生成派生 IPA 或新 marker，不包含用户存档。

See `SHA256SUMS.txt`, `R51-ACCEPTANCE.json` and `R51-RELEASE-MANIFEST.json` in the assets. [Buildable source and exact build-input notes](https://github.com/SomJsA/HyperHLE_ds_compat-android/blob/20261116-r5.1/docs/releases/20261116-r5.1/BUILDING-SOURCE.md).
