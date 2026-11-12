# Android 当前运行自检与资料布局 v1

宿主 applicationId 为 `org.touchhle.android.somjs`，Java/JNI namespace 仍是 `org.touchhle.android`。旧宿主和它的数据保持独立；旧进度使用既有三槽 ZIP 导入。不能以改包名为由清除旧应用或盲拷未知新根。

新安装默认应用专属 Documents 下稳定叶目录 `DeadSpace IOS移植`；这是 Android/data 内的应用目录，不是公共 Documents 的 SAF 授权桥接。可从系统文件选择器的本应用 provider 访问。卸载前应导出三槽 ZIP。已有 SharedPreferences 或原生进度保留旧版根；显式选择固定绝对路径，卷暂不可用时不悄悄回退并创建空进度。切换不搬移、合并或覆盖存档；游戏未退出/导入进行中时拒绝切换。

`storage-layout.json` 与 PC 使用同一 schema1、game、exchange、native_profile、pc_config、android_config 契约。原生目录及 SBIN 不变；Android 配置副本为 `config/android/launcher.json`，schema1/game/values。导入配置只接纳已知键及正确类型/范围，保留未知字段但不猜测转换；存储路径不跨根导入。未知/损坏布局与配置拒绝写入，原文件保留。配置采用 Android AtomicFile，元数据布局初始化串行加文件锁。Game、存档ZIP/编辑/节点和系统文档 provider 使用统一根，MainActivity通过ApplicationContext解析避免getExternalFilesDir递归。

交互式启动直接进入游戏，不展示启动自检或性能短测；画质预设和运行参数由启动器直接保存后生效。Android 不上传运行数据；需要诊断时使用显式日志开关和独立诊断流程，不改变正常启动路径。

Android 产品路径不请求、不运行约30秒短测，也不保存硬件短测缓存；性能和设备建议应在独立诊断构建中完成，不阻塞正常游玩。

本轮只读/编译证据不能冒充设备安装、布局负例、UI或最终发布验收。公共Documents授权桥接、纹理档位产品接入与完整实战性能/原有R15门槛继续保留，见项目最新状态入口。正常主release必须编译native；复用.so仅限有逐库指纹的隔离SaveLab。
