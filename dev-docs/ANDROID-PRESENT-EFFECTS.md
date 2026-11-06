# Android 实际呈现效果

2026-11-06。该变更位于 Android 集成树，PC 集成树及冻结 v11 不变。

此前 `fsr_es2::draw_present_current` 给共用 shader 传入 `sharpen=0,
fsr=0,aa=0`，但启动日志打印环境中的请求值。因此开启推荐 FXAA/CAS
仍可能只画普通拷贝。共用 fragment shader 的 FXAA 分支还会从输入纹理
重新采样并覆盖本次调用算出的 CAS/RCAS；同时传两个非零参数不代表组合。

保留 a05 源码快照和 exact shader 输入的设备夹具已在 Adreno610 复现两项
负结果，各覆盖两个独立 EGL context 和四个旋转角。72 项检查同时覆盖
GL 错误、uniform 读回、alpha 以及分阶段 AA/CAS 的实际像素变化。
这些是 shader/驱动组件证据，真实 HLE 路径仍需干净正常构建与游戏回归。

`present_effects::Plan` 用固定三项数组选择处理步骤，不在每帧分配列表：

| 请求 | 实际步骤 |
| --- | --- |
| 全关 | 普通拷贝/原兼容呈现 |
| CAS | CAS |
| FXAA | FXAA |
| FXAA + CAS | FXAA → CAS |
| FSR 质量档 | EASU → RCAS |
| FSR 质量档 + FXAA | EASU → FXAA → RCAS |
| FSR 快速档 | EASU |
| FSR 快速档 + FXAA | EASU → FXAA |

推荐档 FSR 关闭、FXAA 开启、CAS 0.5 现在需要两步。FXAA 先写最终输出
尺寸的临时纹理，CAS 再从该纹理采样。FSR 质量档组合最多需要两张临时纹理。
旋转矩阵仅用于第一步，后续使用单位矩阵。deprecated shadow lift 仅用于
最后一步。两个中间目标及失败标记均归实际 `PresentState`，尺寸变化和
释放仍在其 owner 当前时处理；失败退出也恢复 FBO、纹理和 viewport。

原生私有 ES2 调用者刚绑定了 FBO0 和源纹理，直接传递这些已知绑定；兼容
调用者读取实际绑定。不跨上下文共用附件，不借用 guest ES1 的 shader API。
缺少必须的 uniform 或分配/链接失败会明确回退，不能记为完整效果通过。

`[DSpresent-effects] submitted` 记录提交的步骤。私有 ES2 在最终 GL 检查
通过后才记录 `shader present active`，其中效果名称来自成功步骤，不再
打印请求值代替实际状态。启用多个效果会改变实际画质和 GPU 工作量，旧的
未生效 FPS 不能当作相同画质的性能基线。

显式 `TOUCHHLE_DS_PRESENT_VERIFY=1` 在当前 Android ES2 program 上读回
uAA/uSharpen/uFsr/uShadow 和 CURRENT_PROGRAM，记录 draw 后待处理错误与
查询错误。每个实际 PresentState 最多16组不同 program/尺寸/参数，release
清空；默认没有这些额外查询。验证失败在该 context 中保持失败并明确回退，
不把被读取的 GL 错误掩盖成成功。新增 GLES2Native GetUniformfv 委托用于该
标准 ES2 查询。

纯规划/预算8项测试可独立编译 `src/gles/present_effects.rs`。完整证据入口为
`report/android-present-effective-features-20261106`。旧章节入口、资源上传GL0、
短走廊与原数据恢复证据继续保留；完整推荐档、灯光箱子、跨关、10h旋转、
全剧情/Boss、实体输入/武器/音频、额外GPU、发布与人工验收均分别核实。
