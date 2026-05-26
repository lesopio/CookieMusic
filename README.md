<div align="center">

# CookieMusic

**一款基于 Jetpack Compose 构建的本地音乐播放器**

Material 3 动态主题 · 状态栏悬浮歌词
<br/>

![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7.6-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-1.3.1-6750A4?style=for-the-badge)
![Media3](https://img.shields.io/badge/Media3%20ExoPlayer-1.4.1-FF6F00?style=for-the-badge)
<br/>

</div>



## 项目简介

**CookieMusic** 是一款面向 Android 的本地音乐播放器，采用 **Kotlin + Jetpack Compose + Material 3** 构建。

目前支持自动扫描本地音频、文件夹导入管理、动态主题、歌词解析、状态栏悬浮歌词、音效调节、播放历史、收藏夹、全局搜索、睡眠定时与平板自适应布局

---

## 功能特性

| 功能模块 | 说明 |
|:--|:--|
| 本地播放 | 自动扫描设备音频文件，支持文件夹导入与管理 |
| 智能主题 | Material 3 动态配色，支持跟随专辑封面、系统或手动切换 |
| 歌词体验 | 内嵌歌词解析，支持状态栏悬浮歌词，锁屏也能查看 |
| 专业音效 | 内置均衡器、低音增强、虚拟环绕、响度增强 |
| 自适应布局 | 手机使用底部导航，平板使用侧边导航 |
| 播放管理 | 播放历史、收藏夹、全局搜索、睡眠定时 |
| 高刷适配 | 自动启用设备最高刷新率，动画更加丝滑 |
| 音频信息 | 实时展示格式、码率、采样率、位深、声道等信息 |

---

## 应用截图

<div align="center">

<table>
  <tr>
    <td align="center"><img src="https://github.com/user-attachments/assets/489e6cd0-6879-4e8e-93b9-316dcf324edf" width="230" /></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/b221bc5c-208c-4178-94df-fceaf9b57afb" width="230" /></td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/user-attachments/assets/14f17ad5-48e3-4e5f-bd1f-144590d7195c" width="230" /></td>
    <td align="center"><img src="https://github.com/user-attachments/assets/e85b174e-560a-4fc1-bf90-6b942a1f58af" width="230" /></td>
  </tr>
</table>

</div>

---

## 技术栈

```txt
CookieMusic
├── Language
│   └── Kotlin 2.0.20
│
├── UI
│   ├── Jetpack Compose 1.7.6
│   ├── Material 3 1.3.1
│   └── Window Size Class
│
├── Media
│   ├── Media3 ExoPlayer 1.4.1
│   ├── MediaSessionService
│   └── Android AudioEffect
│
├── Image
│   └── Coil 2.7.0
│
└── Build
    └── Java 17
```

---

## 架构特点

### 单 Activity + Compose Navigation

全 Compose 声明式 UI，无 Fragment，页面结构更轻量，维护成本更低。

### ViewModel + StateFlow

使用响应式状态管理，让 UI 状态、播放状态和业务逻辑更清晰地解耦。

### MediaSessionService

支持后台播放、通知栏控制、锁屏控制与系统媒体会话集成。

### 音频特效链路

```txt
Equalizer → BassBoost → Virtualizer → LoudnessEnhancer
```

内置均衡器、低音增强、虚拟环绕与响度增强

### 自适应双布局

根据屏幕宽度自动切换不同导航形态：

```txt
手机：Bottom Navigation
平板：Navigation Rail
```

---

## 系统要求

| 项目 | 要求 |
|:--|:--|
| Android 版本 | Android 8.0，API 26 及以上 |
| 基础权限 | 本地音频文件读取权限 |
| 可选权限 | 悬浮歌词需要开启悬浮窗权限 |

---

## 下载安装

### GitHub Releases

到 [Releases](../../releases) 页面下载各个版本 APK。

### 夸克网盘

```txt
链接：https://pan.quark.cn/s/3f733ef9a167
提取码：HiLw
```

---

## 使用指南

### 首次启动

1. 安装并打开 CookieMusic
2. 授予本地音频文件读取权限
3. 应用会自动扫描设备中的音乐文件
4. 如需添加指定目录，可进入「设置 → 导入管理」手动导入文件夹

### 快捷操作

| 操作 | 功能 |
|:--|:--|
| 播放页左右滑动 | 切换上一首 / 下一首 |
| 播放页长按封面 | 切换主题配色跟随模式 |
| 通知栏 / 锁屏 | 播放控制与进度拖拽 |
| 状态栏悬浮歌词 | 可在设置中开启，并支持位置与样式调节 |

---

## 版本记录

| 版本 | 日期 | 更新内容 |
|:--|:--|:--|
| v5.0.0-alpha | 2026.05 | Material 3 重构、状态栏歌词、自适应布局、音效体验优化 |
| v5.0.1-alpha | 2026.05.26 |修复了部分已知问题，其他的还在看...修复了夜间模式不显示的bug并且增加了主页动画但是比较卡顿还在找原因... |

完整更新记录请查看 [Releases](../../releases)。

---

## 常见问题

### Q: 为什么扫描不到部分音乐？

请检查文件格式是否受支持，例如 MP3、FLAC、AAC 等主流格式，并确认已经授予存储权限。部分系统可能需要手动开启「所有文件访问」权限。

### Q: 悬浮歌词不显示怎么办？

请在系统设置中为 CookieMusic 开启「悬浮窗权限」，并在应用内进入「设置 → 状态栏歌词」启用~

### Q: 为什么高刷新率没有生效？

有bug，还在找（笑

---
## 接下来要做的事情
    1.增加本地音频源与云音频源
    2.优化动画
    3.读取屏幕圆角值，自适应


## 反馈与建议

如果你有idea或者发现问题，欢迎通过你反馈给我~

- 提交 [Issue](../../issues)
- 发送邮件至：`lesop@foxmail.com`

---

<div align="center">

**CookieMusic**

</div>
