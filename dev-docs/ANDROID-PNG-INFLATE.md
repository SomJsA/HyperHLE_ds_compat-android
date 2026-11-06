# Android PNG解压快速路径

2026-11-03，原型实机数据保存在report后续状态入口及R15-ANDROID-OPT-20261103-A。Android只替换stb PNG内部的deflate展开步骤，使用API21已有系统zlib；原有PNG解析、CgBI颜色/alpha处理和字节等价premultiply保持。STB原解压器以STBI_ONLY_ZLIB独立静态编译，系统路径失败先释放临时内存再回退；TOUCHHLE_DS_PNG_INFLATE=legacy或0显式回滚。原版与高清PNG不改写，PC和vendor子模块不变。

标准流用MAX_WBITS，CgBI原始deflate用-MAX_WBITS。动态增长遵守INT_MAX、使用STB相同分配器，保持失败原因，绝不跨线程共享解码器或GL。计数器是解码线程本地计数，首个成功PNG仅记录一次，不是全进程GPU/帧耗时。RGBA纯后处理worker依旧独立。

设备原型同进程ABBAAB，17图102次完整PNG解码逐字节一致，含1/2/4/8/16位、灰度/RGB/alpha、五滤波、palette+tRNS、Adam7、CgBI原始deflate。错误Adler但STB可接受的夹具实际走旧解压器且像素相同。真实8192图69291a12中位3820.476→3331.315ms，d98fa9a2为3553.128→3252.480ms。只是独立shell CPU分量，非游戏FPS、全关加载或全部设备保证。

复测tools/tests/AndroidPngInflateBench.c需NDK aarch64 API21编译，链接本模块lib.c、android_png_legacy.c和-lz -lm，包含vendor/stb目录。读取只读PNG；同一程序环境切换legacy/system，比较完整RGBA及真实调用计数。正式release必须正常完整构建，另验证实际游戏、纹理上传与恢复，不用这个基准替代最终验收。

## 上传同步对照入口

Android旧实现只检查TOUCHHLE_DS_TEX_UPLOAD_FINISH是否存在，导致0/off甚至flush仍调用Finish。新纯策略将0/off/false映射Off、flush映射Flush，1/旧未知值保持Finish，首个实际上传日志记录生效策略，精确profile区分Finish和Flush。默认1保持既有基线，需实机正确性和固定条件对照后再决定默认；不能把修正开关语义直接当FPS收益。

生产C源独立程序再次通过17图102解码；另对原IPA全部334张PNG（283张CgBI）做2004次完整解码逐字节对照，全部一致。未改写IPA或原PNG。来源、PNG索引和设备原始结果落在本轮报告目录；不得据此称完整关卡/帧率通过。
