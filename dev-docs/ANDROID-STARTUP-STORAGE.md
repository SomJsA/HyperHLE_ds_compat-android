# Android 当前运行自检与资料布局 v1

宿主 applicationId 为 `org.touchhle.android.somjs`，Java/JNI namespace 仍是 `org.touchhle.android`。旧宿主和它的数据保持独立；旧进度使用既有三槽 ZIP 导入。不能以改包名为由清除旧应用或盲拷未知新根。

新安装默认应用专属 Documents 下稳定叶目录 `DeadSpace IOS移植`；这是 Android/data 内的应用目录，不是公共 Documents 的 SAF 授权桥接。可从系统文件选择器的本应用 provider 访问。卸载前应导出三槽 ZIP。已有 SharedPreferences 或原生进度保留旧版根；显式选择固定绝对路径，卷暂不可用时不悄悄回退并创建空进度。切换不搬移、合并或覆盖存档；游戏未退出/导入进行中时拒绝切换。

`storage-layout.json` 与 PC 使用同一 schema1、game、exchange、native_profile、pc_config、android_config 契约。原生目录及 SBIN 不变；Android 配置副本为 `config/android/launcher.json`，schema1/game/values。导入配置只接纳已知键及正确类型/范围，保留未知字段但不猜测转换；存储路径和硬件/短测缓存不跨根导入。未知/损坏布局与配置拒绝写入，原文件保留。配置采用 Android AtomicFile，元数据布局初始化串行加文件锁。Game、存档ZIP/编辑/节点和系统文档 provider 使用统一根，MainActivity通过ApplicationContext解析避免getExternalFilesDir递归。

每次交互式启动展示自检，可继续后从暂停菜单重看。只读取本次独立目录并匹配当前PID的 native报告；中文挂载、实际分辨率、MSAA与AF请求/读回/回退及影响分别展示。待确认不算通过。后台读取不抓帧，不逐键记录；默认不做性能短测，不上传。自动化明确autorun默认不遮挡私有回放，可显式startup_check=true验屏；这不能替代产品UI验收。

未知硬件首次启动请求约30秒有效场景短测，取消或保留设置不会启动测试；已明确请求的下次短测只运行一次。硬件识别/结果不自动覆盖画质。缓存匹配硬件指纹和当前设置；已知Adreno610旧3x不达60与过滤回退证据只作有限建议。短测累积实际present时间，保留开火、近战和整个被抓/临死阶段；菜单、加载、HUD模态、死亡26、过场29以及未知状态不计入。当前HUD/world/player身份必须匹配，不能把残留对象作为可操作样本。短测不等于全关60或游戏时钟验收。

本轮只读/编译证据不能冒充设备安装、布局负例、UI/短测或最终发布验收。公共Documents授权桥接、纹理档位产品接入与完整实战性能/原有R15门槛继续保留，见项目最新状态入口。正常主release必须编译native；复用.so仅限有逐库指纹的隔离SaveLab。
