# R15 项目自查

Android呈现路由检查防止私有ES2重新用常数shader参数绕过AA/锐化/FSR配置，检查两张上下文中间目标的release和ES2 uniform查询委托。`test_present_effects.py`覆盖旧路径、丢失AA读取、伪造注释/字符串及释放遗漏负例；该结构检查不替代真实shader像素和游戏uniform/GL证据。

EAGL传输缓存检查覆盖PC和Android的已知GL句柄/缓存类型及owned-state锚点，允许CPU-onlyRGBA scratch；Rust反斜杠续行字符串不能隐藏后续真实代码。此结构检查不替代实际上下文像素、附件、释放和Android设备生命周期验收。

Android角色字体按用户指定TitleChinese/StoryChinese精确SHA验证native res与Java asset，并检查最终libtouchHLE.so和APK asset均包含对应字节。旧章节资源保留历史用途，不能由资源存在推断新face已经在设备显示。变更来自用户明确字体要求，不自动重采样失败基线。

Python3.10+，标准库。默认只读本地文件，不连接ADB、不唤醒设备、不启动游戏、不生成IPA、不修复/覆盖数据。

从任一 R15 仓库运行（路径替换为当前实际工作树）：

```powershell
python tools/project-guard/guard.py --pc-root E:/CodexProjects/DeadSpaceLegacy/work/HyperHLE_ds_compat-r15-integration-20261031 --android-root E:/CodexProjects/DeadSpaceLegacy/work/HyperHLE_ds_compat-r15-android-integration-20261031 --report E:/CodexProjects/DeadSpaceLegacy/report/guard-source-new.json
```

可追加 `--pc-package <解压发布目录>`、`--android-package <含manifest.json与APK的目录>`、`--protected-manifest <路径到SHA256的JSON>`。报告路径必须全新，拒绝覆盖。检查失败返回1；参数/输出冲突返回2。未指定的范围明确 skipped。`scoped-checks-passed` 只表示本次文件检查通过，绝不是全项目验收。

检查：工作树/submodule清单、SBIN与JSON键值一致、当前资源指纹/文本修正/章节藏头、PC/Android样式与字体副本、Android既有光照实现、有限API21禁用模式、发布清单文件/声明源码快照、编译后内嵌文本、APK字体/样式/native库/原IPA、受保护文件。Git未提交改动作为工作中信息记录；发布检查会拒绝有未提交的声明编译输入或实现提交后改过的运行时代码。新增规则不应依赖源码排版/行号。

契约更新要说明用户要求、资源来源和重验范围。不要为消除失败而抄当前文件的哈希；先查明变化是否授权、是否需要重新构建。`contract.json` 的光照指纹是当前平台专属基线，变化需要有对应场景证据。历史工作台预设不会作为正式默认参与比对。

```powershell
python -m unittest discover -s tools/project-guard -p "test_*.py"
python tools/project-guard/run_save_tests.py --repo <Android工作树> --fixture-documents <只读原始Documents夹具> --output <全新测试输出目录> --javac <javac路径> --java <java路径>
```

第二条编译并执行仓库内真实 Java 存档/IPA保护探针；只在新目录建夹具副本，原始夹具前后逐文件哈希核验。使用只有slot0 checkpoint/level的实验夹具，测试将克隆出另两槽，不能声称三个原生有效进度。测试类使用桌面JDK API，不打包进Android；生产代码API21约束单独检查。这不是设备/SAF/原生换甲动画验收。追加 `--pc-repo <PC工作树> --csc <csc.exe路径>` 可编译执行 `tools/tests/SaveBundleTest.cs` 的PC三槽ZIP/拒绝写入回归。若用户游戏进程正在运行，它会安全拒绝，不应为测试杀掉用户进程。

发布与人工验收流程、证据层级及既知反复问题见 `dev-docs/R15-RELIABILITY.md`。当前实际设备/发布状态由项目最新交接及报告记录，不把机器IP或某次HEAD写成永久默认。

PC QTE 的额外检查会提取提示发布函数，拒绝直接调用旧定位扫描或 framebuffer 读取，并要求 tap/swipe 两条原生内存几何入口。它忽略排版、注释和字符串，保留发布函数以外显式开启的历史兼容代码；这是直接调用检查，不冒充完整调用图或运行时验收。负例覆盖条件分支中重新引入扫描以及丢失原生入口。

## 设备任务结束熄屏

批量测试前先做完整字节传输 canary，数据应包含全部256种字节。本轮 Windows
客户端实测，原始 `shell -T` 二进制stdin在首个 `0x1A` 处截断；文本编码后由
应用UID在远端解码、返回SHA并独立读回验证的方式通过。不能因脚本退出0就推定
远端文件完整。`exec-in` 仅负责输入方向，不读取子命令的stdout，也不提供该
子命令退出状态；`exec-out`只复制设备stdout，不能向它的stdin发送脚本。只读脚本改用显式`adb -s <serial> shell -T <经shlex引用的run-as/sh -c命令>`；避免远端sh等待从未发送的stdin而超时。本轮真实检查证实显式命令约0.2秒完成，旧exec-out输入脚本180秒超时属于控制器错误，安装未开始。见 [ADB官方实现](https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/client/commandline.cpp)。

种子必须先写入新的临时目录，验证归档及解包后逐文件SHA，最后才移走旧测试
sandbox并安装新树。写入失败保留旧树，立即停批次修复；不能继续启动一串缺少
sandbox的用例，再把这些基础设施错误归因于资源档位。焦点、原生进程或日志读取
失败须保留现场画面/焦点/退出原因，不能只留下空AssertionError或覆盖最后有效日志。

仅在用户已经授权的设备上，显式运行：

```powershell
python tools/project-guard/device_job.py --adb <adb.exe> --serial <当前serial> --output <全新日志目录> --timeout 300 -- python <有限设备任务.py> --serial <同一serial>
```

此入口会实际发送ADB；它不属于离线guard，不自动唤醒或查询设备。成功、失败、超时/中断均在finally请求KEYCODE_SLEEP，保留任务与熄屏返回码。子任务仍须自己负责触点/按键释放、独立包/UID、原数据恢复和有限生命周期；包装器不把“熄屏请求已发送”冒充设备状态读回，也不代替这些清理。不得包装含旧硬编码设备地址、未经核对的历史脚本。对应测试全部mock，不连接ADB。
