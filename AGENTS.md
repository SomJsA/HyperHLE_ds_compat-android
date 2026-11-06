# DeadSpace Legacy / HyperHLE_ds_compat

## Android实际呈现效果

- 私有ES2与兼容呈现必须使用相同的配置到处理步骤映射，不能把AA/锐化/FSR常数0传给shader后把请求值记成active。FXAA与CAS/RCAS分别执行；中间FBO/纹理只属当前PresentState，旋转仅第一步应用，shadow仅最后一步应用。目标分配/程序/uniform失败要恢复绑定并明确回退，不能算推荐档通过。
- 显式`TOUCHHLE_DS_PRESENT_VERIFY=1`才做有界uniform读回，按实际context/program/参数保存最多16组；默认不新增查询，release后清空。需要原生shader像素、正常干净构建、实际游戏的步骤和uniform读回共同验证。见`dev-docs/ANDROID-PRESENT-EFFECTS.md`；旧a05资源/移动证据不证明推荐FXAA/CAS曾生效。

## Android EAGL传输缓存维护

- ES1传输纹理/源FBO及ES2程序/对象/尺寸/附件记录归实际backing EAGL上下文持有；owner解析与driver相同并保留worker/main fallback。独立生成辅助对象，owner当前时释放，再恢复既有线程映射；CPU-onlyRGBA scratch可按宿主线程保留。不要整文件覆盖PC/Android呈现实现或改变Android同步/渲染线程默认值。真实宿主双context像素/附件/释放回归与完整APK构建分别记录，不能算Android设备生命周期或玩家10h旋转复现。见`dev-docs/ANDROID-EAGL-TRANSFER-CONTEXT.md`。

## Android原生字体角色维护

- 标题/前端/章节及目标标签使用用户精确MStiffHei，字幕/正文/游戏内原生UI使用Swei Regular。原生场景归属来自已验证的LayerMainMenu和LayerGameWorld绘制入口，不移植PC输入控制器，也不依赖Java overlay开启。暂停/临时HUD缺失保持角色；CGFont/UIFont的三个GlyphBuffer缓存调用者与原生ID分配器必须一致。Java字号按实际Paint字体度量归一化，不能恢复新face共用旧Noto1000/1448常量。须分别核对res/asset/native/APK和实际设备；宿主JIT测试不能冒充设备通过。见`dev-docs/ANDROID-FONT-ROLES.md`。

## 呈现上下文缓存维护

- 呈现shader/program/uniform、FSR texture/FBO和失败标记必须属于实际GL context；同线程或sharegroup不代表FBO可复用。Window内部合成、每个EAGLContext和Android私有shader context各持有PresentState。不得恢复成thread_local全局GL句柄；资源释放必须在所属context当前时执行。双context、6x/4x和FSR像素回归及双端构建证据见`dev-docs/PC-PRESENT-CONTEXT-CACHE.md`。Android完整构建/API21检查不等于设备生命周期验收；保留平台渲染线程默认值和原有实现差异。

## 开始工作

- 用户最新指令优先。完整性能、原生输入/UI、PC/Android release 和最终验收目标不能因打包成功而缩小。
- 先读 `dev-docs/R15-RELIABILITY.md`、`tools/translation/PATCH-MODEL.md`；运行 `tools/project-guard/guard.py`，参见其 README。历史经验、旧截图和旧状态不是当前配置来源。
- 先复核分支、HEAD、status、submodule。只显式暂存本次文件；禁止 `git add -A`、reset 或覆盖未知改动。不要把实验工作区整个纳入 Git。
- PC 与 Android 是不同实现基线。共享数据/协议，逐段移植；不得用 PC 整个文件覆盖 Android，尤其是 `gles_guest.rs`、`gles/util.rs` 和输入/窗口代码。

## 不可回退的契约

- 原版/已验收 IPA、存档和发布件不可覆盖。原 IPA + 运行时只读 SBIN 挂载是正式模型。生成派生 IPA 仍需用户明确确认；普通构建、检查不得调用旧 IPA 重打包脚本。
- 当前默认样式源是 `src/ds_overlay_default.json`，Android asset 副本必须匹配。`tools/overlay-studio/default-style.json` 是历史工作台预设，不可直接部署作正式默认。字号以 PC 为准：**“目标：”标题18，正文独立适配**；章节和字幕的布局、字体度量也须比对，不能只比较字号数值。保留用户显式设置。
- 台账导入后同时核验996键的 native SBIN/JSON、占位符、章节藏头、字体、文本修正清单。`CREDITS` 是信用点，`credits_list` 是制作名单。不得只改 JSON。
- 默认键鼠 Ctrl/G/B 定位，手柄 L3 转身、R3 定位。教程与重绑定来自实际动作语义；不要把 canonical dispatch 表误当物理默认。至少验证一次非默认绑定。
- E 交互沿用原生距离/朝向/战斗门槛。按键释放、站点进入、移动/疾跑状态及模态/换甲动画必须有明确所有者；回归覆盖连续进入/退出及完整换甲动画，不能以屏蔽原生门槛掩盖故障。
- PC QTE 按键提示也必须读取原生 HUD 布局/渲染参数；E 主路径改为内存不代表提示路径已清理。禁止在被抓等状态轮询全分辨率 framebuffer 找提示。未知原生代码保留原提示，不自动退回抓帧。运行 project-guard 的 QTE 发布函数检查，不能只核对资源哈希。性能窗口覆盖原生 state26 前的完整被抓阶段，不能仅截取最后一次低频存活探针之前的轻负载部分。
- 三槽 ZIP 包含共享 Documents 依赖和合法零字节依赖；空 SBIN/损坏/越界/重复/路径穿越拒绝且无写入。编辑仅改用户选择的已有字段；Android checkpoint/level 同步时也不能连带覆盖未改的信用点/节点。导入/编辑前自动备份，恢复须字节核验。克隆槽位夹具不等于三个原生有效进度。
- HD PNG 的实际上传路径、尺寸、alpha-aware mip 必须在运行时观察。自动原图 mip 修复不能抢在选定替换图之前。8x限制与显存估算如实标注，估算不是实测峰值。
- 多核先用于纯资源处理：RGBA/mip/预解码任务只能读取输入和写入独立输出；GL、guest内存/游戏状态及原生输入继续由所属线程管理。不得用 unsafe Send/Sync 强行共享 Environment。并行输出须逐字节对照；加载收益不能冒充实战主循环多核或稳定帧率。
- Android minSdk21；新增 API 必须检查可用版本。主 release 必须正常完整构建；复用 `.so` 仅限隔离 SaveLab，并核验其与发布件逐库一致。

- 新Android分发默认应用专属Documents/DeadSpace IOS移植；保留已有旧根。游戏/ZIP/编辑器/节点/provider必须用RuntimeStorage统一解析，不能只改MainActivity。schema1共享PC路径契约，未知/损坏布局或配置拒绝写入；应用专属Documents不能宣称公共SAF目录已经桥接。稳定宿主applicationId含somjs，与原生com.ea.deadspace.bv和Java/JNI namespace独立。
- 启动自检只读取本次目录且PID相符的实际报告；请求/生效/待确认/回退影响分别展示。未知硬件请求短测，未经产品内同意不执行，不自动覆盖用户画质。有效开火state3等游戏帧和完整被抓阶段必须保留；不能靠过窄状态白名单隐去长帧。当前HUD/world/player要一致，原生present而非界面刷新才计数。

## 运行与证据

- PC 优先 `TOUCHHLE_DS_CONTROL_INBOX` 和隐藏测试进程；不注入宿主键鼠、不强制前台。不终止未知进程/VM。
- ADB/VM 授权与用户偏好沿会话持续，不重复询问。ADB 显式 serial/package/UID，测试用独立包和副本；升级前新鲜备份当前 APK/数据，不能用最早备份推定当前版本。**ADB不用时熄屏，测试 finally 中 KEYCODE_SLEEP**；离线 guard 不查询设备。
- 有限设备任务优先用 `tools/project-guard/device_job.py` 包装（README示例），其finally在失败/超时/中断后仍请求熄屏；子任务仍须自行恢复数据/释放输入，不能把包装器当完整清理。包装器和子任务必须指向同一明确serial。
- 隔离前写可恢复日志；恢复校验完整路径和目标不存在，保留测试结果，恢复原目录/设置/APK并核验哈希。恢复脚本不得盲目重跑，禁止 clear/uninstall 原应用。
- 串行管理同一设备/私有 runtime 的 worker。开始新测试前确认上一 worker 已结束；测试 finally 释放触点/按键和休眠，不让后台自动化与用户冲突。
- 批量设备测试先验证包含全部256种字节的小传输。种子在新目录传输、解包、逐文件SHA验证完成后，才替换已知测试 sandbox；基础设施失败立即停批次，不连带累计为游戏/资源失败。Windows二进制stdin与ADB单向通道的边界见project-guard README，不能把exec-in返回0当作远端写入完成。
- 先辨认真实菜单和游戏状态，不写死“前12项就是章节”。当前17项交错菜单映射见 guard contract；更改时从实际菜单重新审计。探针含义先看平台实现，Android F6 不是 PC 检查点。
- 超时先分清导航/观测器失败与游戏失败；状态变化日志不是持续快照。通过结论须关联原生事件、时间推进和实际画面；预览不能代替原生章节/目标/字幕验收。
- `frame 300 reached`只证明present仍活跃，不证明主菜单准备好；输入ACK也不证明菜单已切换。档案/模式/章节各页分别确认，等待实际UI和动画结束。玩家对象已创建仍可能是过场（已确认state29）；性能入窗必须另有可操作状态和实画面，不能把过场算移动/战斗。
- AF日志中的`actual`若仅来自请求值钳制，不能当作驱动读回；HD上传必须核验GL错误，过滤设置与MSAA回退单列。不能把失败设置回退到原图后的帧率记为高档资源性能。
- 源码、构建资源、产物、实际安装四层分别核对。发布清单记录 implementation commit、编译输入/文件 SHA、样式/字体/文本及 native库；文档 HEAD 不冒充构建来源。APK安装成功不等于装对版本。
- 性能固定场景/配置/预热，记录 native present interval；截图、探针、加载/暂停和温漂单列。动态场景样本不能当静止AB，封顶FPS不能证明余量。请求开启不等于实际启用：MSAA/FSR/FG回退必须检查。VM llvmpipe不算第二硬件GPU。
- 人工验收和未完成门槛保留在最新状态文件，不能由脚本批量改成通过。新状态明确指向上一记录与实际证据；历史记录保留并标识失效原因。

## 输出与检索

- 先限定目录和文件类型，排除 vendor/target/build/cache；大目录状态先摘要，不把整个未跟踪文件列表带入上下文。
- Windows 文本显式 UTF-8；控制台 JSON 用 ASCII 转义或设置 UTF-8，检查每步退出码再执行依赖步骤。
- 看图前量字节，>400KB先缩至<=1568px，优先局部；不重复看同图，结论落盘。长日志定位后摘要。
- 请求体字节才决定413；tokens不是体积。rollout 有 compact 时文件大小会虚高，不能据此叫停。沿用用户全局上下文卫生规则。

## PNG纯资源处理维护

- `src/image_premultiply.rs`保持原f32中间alpha舍入；opaque/zero快路径必须逐字节等价。worker仅借用不重叠RGBA切片，不跨线程传Image、STB全局、GL或Environment。线程创建失败只处理未被取走的块，不能重算整图使半透明像素二次变暗。
- 组件基准/5项等价恢复测试见 `dev-docs/PNG-RESOURCE-PERFORMANCE.md` 和 `tools/tests/PngPremultiplyBench.rs`；加载分量收益不能标成实战FPS或门电梯被动卡顿已修复。已交付采集包仍按自身implementation commit/map分析，不能更新旧ZIP。

## Android PNG解压维护

- 系统zlib只替换deflate，保留CgBI原始流、STB解析和原解压器失败回退；回滚TOUCHHLE_DS_PNG_INFLATE=legacy。解码/RGBA分量收益不能冒充游戏FPS，原始PNG/IPA不改写；实际与配置回退分开验收。参考dev-docs/ANDROID-PNG-INFLATE.md。

## Android JIT快路径维护

- `TOUCHHLE_DS_CPU_FASTMEM=1`当前是显式实验，默认页表保留；必须保护完整且与宿主页对齐的非空null段，并保留Dynarmic页表/原Rust兼容回调。4KiB guest null段遇16KiB宿主页不得扩大保护吞掉有效地址，应回退。析构先销毁JIT再恢复保护；不可用取消空地址语义或开启unsafe优化换性能。组件基准不替代正常APK/ART、真实玩法和时钟验收。见`dev-docs/ANDROID-JIT-FASTMEM.md`。

## Android骨骼数学影子验证

- `TOUCHHLE_DS_SKIN_MATH=shadow`只比较，始终采用原版ARM输出。完整函数指纹未知不得挂钩；快照必须防null/溢出/超界/别名，原版首条PUSH和返回栈语义必须验证。4权重早停规则、非融合乘加和FPSCR异常位0x9F都需与原版一致，不能只看XYZ或让无效数据悄悄变零。组件/影子诊断开销不能作实战增益；真实数据零差异及完整fallback验证前不得把宿主结果设为默认。见`dev-docs/ANDROID-SKIN-MATH-SHADOW.md`。


## Android宿主数学实验

- `TOUCHHLE_DS_SKIN_MATH=host`仍默认关闭。每个函数先64次真实原版输出与FPSCR匹配，之后每256次复查；任何差异禁用本进程宿主路径。未知指纹/不支持布局/写入边界失败仍运行原版，完整结果算出并重新检查目标后才写入。测试用FORCE_FALLBACK_EVERY不是玩家默认。见dev-docs/ANDROID-SKIN-MATH-HOST.md。
- 原生函数计时只在NATIVE_MATH_PROFILE=1开启，完整指纹匹配后保留原函数；inclusive elapsed含嵌套/桥接/调度，父子不可相加或当独占CPU。默认未开启不得宣称已优化相关函数。

## Android渲染线程实验

- RENDER_THREAD=sync/queue 默认关闭；所有GL上下文与物理交换必须属于同一Window工作线程。指针调用/查询/绘制仍同步，未接入owned快照前不得异步化。不得给Environment/Window加unsafe Send/Sync。更改Native GLES签名后执行 tools/renderer/generate-native-proxy.py --check。背景停泊/表面重建/销毁顺序、原生UI及输入延迟需要实测，组件收益不代表游戏60帧。见dev-docs/ANDROID-RENDER-OWNER.md。
