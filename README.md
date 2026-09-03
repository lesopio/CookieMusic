<div align="center">

# CookieMusic

**一款专注本地音乐、同步歌词与个性化播放体验的 Android 播放器**

离线音乐库 · 双语/逐字歌词 · MediaSession 后台播放 · 专辑取色主题

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7.6-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Media3](https://img.shields.io/badge/Media3%20ExoPlayer-1.4.1-FF6F00?style=for-the-badge)

</div>

## 项目简介

CookieMusic 是使用 Kotlin、Jetpack Compose 和 Material 3 构建的 Android 本地音乐播放器。它可以扫描系统音乐库，也可以通过系统文件选择器导入单曲、整个文件夹和歌词文件。

当前版本为 **0.9.0**，最低支持 **Android 8.0（API 26）**。仓库目前只包含 Android 客户端，没有 Windows、鸿蒙或在线音乐服务实现。

## 已实现功能

### 本地音乐库

- 扫描 Android 系统媒体库，并缓存已有歌曲以加快启动展示
- 按歌曲、歌手、专辑和文件夹浏览
- 从系统文件选择器导入多首歌曲或整个文件夹
- 保留导入文件和文件夹的长期读取权限
- 在导入管理中查看、移除导入项，并提示授权失效或文件缺失
- 尝试识别从不同入口发现的同一首歌，减少重复歌曲及收藏、歌单失效

### 播放与队列

- 播放、暂停、上一首、下一首和进度拖动
- 顺序播放、单曲循环和随机播放
- 查看当前播放队列并点击任意歌曲切换
- 完整播放页、歌词页底部控制条和常驻迷你播放器
- Media3 MediaSession 后台播放及系统媒体控制
- 拔出耳机等音频输出变化时自动处理播放
- 睡眠定时，到时自动暂停

### 歌词

- 支持 LRC、SRT、TTML/XML 歌词
- 支持 ID3 USLT/SYLT 和 FLAC Vorbis Comment 内嵌歌词
- 外部歌词优先，缺失时回退到音乐文件内嵌歌词
- 完整歌词页自动跟随当前行，点击歌词可跳转进度
- 支持带逐词时间的 TTML 卡拉 OK 高亮
- 自动整理原文与译文，分层显示双语歌词
- 全库双语歌词索引与统计
- 可选后台顶部悬浮歌词，可调颜色、位置、宽度、字号和暂停隐藏时间
- 可选将当前歌词显示在系统媒体标题中（不同设备的锁屏和控制中心刷新效果可能不同）

### 收藏、歌单、搜索与历史

- 收藏或取消收藏歌曲
- 新建、重命名和删除歌单
- 添加、移除歌曲并调整歌单内顺序
- 从指定位置播放整个歌单
- 按歌曲名、歌手和专辑搜索
- 歌曲持续播放约 10 秒后记录历史、最近播放时间和累计次数

### 播放页与个性化

- “当前”和“极简”两套播放页主题
- 专辑封面模糊取色与缓慢流动的柔光背景
- 日间、夜间和跟随系统显示模式
- 自定义强调色、预设颜色、最近颜色
- 实验性的全局强调色跟随当前专辑封面
- 可调动画强度及清晰、均衡、氛围、低功耗预设
- 可选胶囊封面旋转、三段歌词预览和高级切歌淡入动画
- 窄屏底部导航、宽屏侧边导航，以及独立的横屏播放器和均衡器布局
- 边到边界面、透明状态栏和导航栏

### 音效与音质信息

- 五段均衡器和七种预设
- 低音增强、虚拟环绕和响度增强
- HiFi 模式：关闭应用内均衡和增强，减少额外处理
- 系统声音设置和蓝牙设置快捷入口
- 显示可读取到的格式、比特率、采样率、位深、声道和文件大小

> Android AudioEffect 的可用性取决于设备、系统和音频输出路径。这里的 HiFi 模式不代表独立高解析解码、硬件认证或音质保证。

## 应用截图

<div align="center">

<table>
  <tr>
    <td align="center"><img src="https://github.com/user-attachments/assets/489e6cd0-6879-4e8e-93b9-316dcf324edf" width="230" alt="CookieMusic screenshot 1" /></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/b221bc5c-208c-4178-94df-fceaf9b57afb" width="230" alt="CookieMusic screenshot 2" /></td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/user-attachments/assets/14f17ad5-48e3-4e5f-bd1f-144590d7195c" width="230" alt="CookieMusic screenshot 3" /></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/e85b174e-560a-4fc1-bf90-6b942a1f58af" width="230" alt="CookieMusic screenshot 4" /></td>
  </tr>
</table>

</div>

## 快速开始

1. 从 [GitHub Releases](../../releases) 下载 APK 并安装。
2. 首次打开时授予本地音频读取权限；Android 13 及以上还会请求通知权限。
3. 应用会自动扫描系统媒体库。
4. 如需添加系统媒体库之外的内容，进入“设置 → 导入管理”，选择歌曲或文件夹。
5. 如需歌词，可在同一页面导入歌词文件；应用会按文件名匹配当前曲库。
6. 如需后台悬浮歌词，进入“设置 → 状态栏歌词”并授予悬浮窗权限。

## 权限说明

| 权限 | 用途 | 是否必需 |
|:--|:--|:--|
| 音频/存储读取 | 扫描 Android 系统媒体库 | 使用系统音乐库时需要 |
| 通知 | 展示系统媒体播放通知 | Android 13 及以上建议授予 |
| 前台媒体播放 | 支持后台播放 | 播放服务需要 |
| 修改音频设置 | 均衡器及增强效果 | 使用音效时需要 |
| 悬浮窗 | 在其他应用上方显示当前歌词 | 仅悬浮歌词需要 |
| 唤醒锁 | 后台播放期间保持必要运行 | 播放服务使用 |

通过系统文件选择器导入的内容使用 Android Storage Access Framework 授权，不要求“所有文件访问”权限。

## 技术架构

```text
Compose UI
    ↓ StateFlow
PlayerViewModel
    ├── SongRepository ── Room / MediaStore / SAF / 歌词解析
    ├── MusicController ── MediaController
    │                         ↓
    │                  MediaSessionService + ExoPlayer
    └── StatusLyricOverlayService ── WindowManager 悬浮歌词
```

主要技术：

- Kotlin 2.0.20、Java 17
- Jetpack Compose 1.7.6、Material 3 1.3.1
- Media3 ExoPlayer 与 MediaSessionService 1.4.1
- Room 2.6.1
- Coil 2.7.0
- MediaStore 与 Storage Access Framework
- Android Equalizer、BassBoost、Virtualizer、LoudnessEnhancer

音乐库数据库将“歌曲身份”和“实际文件来源”分开保存，使系统扫描、单曲导入和文件夹导入可以共享稳定的收藏与歌单引用。播放器 UI 不直接持有 ExoPlayer，而是通过 MediaController 与后台 MediaSessionService 同步当前歌曲、队列、播放位置和播放模式。

## 从源码构建

前置条件：

- JDK 17
- Android SDK（项目当前使用 compileSdk/targetSdk 36）
- Windows 可使用仓库中的 `gradlew.bat`

```powershell
# 编译 Debug Kotlin 并运行单元测试
.\gradlew.bat compileDebugKotlin testDebugUnitTest --console=plain

# 构建 Debug APK
.\gradlew.bat assembleDebug --console=plain

# 构建未签名或按本机配置签名的 Release APK
.\gradlew.bat assembleRelease --console=plain
```

本机 SDK 路径应写入未提交的 `local.properties`。Release 签名材料不包含在仓库中。

## 当前限制

- 仅支持 Android 本地音乐，没有在线曲库、云同步或桌面/鸿蒙客户端。
- 搜索当前覆盖歌曲名、歌手和专辑，暂不按文件夹名检索。
- 播放队列支持查看和点播，但暂不支持直接删除、清空或拖动重排临时队列。
- 播放历史暂不提供清空入口，历史行也没有直接播放操作。
- 动态背景当前是视觉时间动画，不是真实音频频谱或节拍采样。
- 舞台粒子、歌词粒子、进度粒子、封面运动和高刷新率选择尚未形成可用功能。
- 睡眠定时依赖当前应用进程；进程被系统彻底终止后不会可靠恢复。
- 悬浮歌词、系统媒体歌词和 AudioEffect 在不同厂商设备上的兼容性需要实际验证。

## 测试状态

当前自动化测试主要覆盖歌曲身份匹配和去重边界。歌词格式、导入授权、歌单、历史、播放服务、音效和 Compose UI 仍需要补充自动化测试，并结合真机回归验证。

## 反馈与建议

- 提交 [Issue](../../issues)
- 邮箱：`lesop@foxmail.com`

---

<div align="center">

**CookieMusic **

</div>
