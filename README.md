# Vague Player ( 朦胧音乐 

探索兔子玻璃拟态 (Liquid Glassmorphism) 与流体交互的极限美学。

Vague Player 是一款现代基于 Jetpack Compose 构建的 Android 本地音乐播放器。

后续有考虑开发Windows端  敬请期待

---

## ✨ 核心特性 (Key Features)

### 🎨 极致视觉设计 (Visual Excellence)
*   **液态玻璃引擎 (Liquid Glass Engine)**: 全局采用定制的 AGSL Shader 和 RenderEffect，模拟真实液态的张力、光线折射与色散（Chromatic Aberration）。
*   **无缝形变 (Seamless Morphing)**: 采用 Shared Element Transition 技术，Mini Player 与全屏播放器之间实现丝滑的形态转换，专辑封面与控件布局完美衔接。
*   **流体底栏 (Liquid Dock)**: 独创的底栏设计，迷你播放器与导航栏能够像水银般动态融合与分离，根据手势产生有机形变。
*   **实时模糊 (Stable Glass Blur)**: 利用深度定制的 `Haze` 库实现的高性能毛玻璃效果，修复了各种渲染崩溃，适配明暗模式，层次分明。

### 🎵 纯粹音乐体验 (Pure Music)
*   **无缝播放 (Gapless Playback)**: 基于 ExoPlayer/Media3 深度优化，消除切歌间隙，适合古典乐与现场录音专辑。
*   **智能队列管理**: 
    *   **真正的随机播放 (True Random Shuffle)**: 基于 Fisher-Yates 算法，确保每一轮播放的绝对随机与公平，支持循环检测自动重洗，杜绝伪随机的重复感。
    *   **下一首播放 (Play Next)**: 侧滑歌曲即可快速插入。
    *   **多选模式**: 长按批量管理，支持拖拽、删除、批量添加。
*   **沉浸式歌词**: 极简的歌词界面，支持标准 LRC 格式同步滚动。
*   **睡眠定时器**: 优雅的玻璃质感倒计时工具，伴你入眠。

### 📂 灵活的库管理
*   **文件夹过滤**: 支持添加自定义扫描路径，智能排除干扰音频。
*   **软删除机制**: 并非直接物理删除，而是将其“移除”至回收站，随时可恢复，防止误删。
*   **快速索引**: 字母侧边栏（A-Z Sidebar），从数千首歌曲中瞬间定位。

---

## 🛠 技术栈 (Tech Stack)

本项目完全使用 **Kotlin** 编写，采用现代化的 **Modern Android Development (MAD)** 架构。

*   **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Architecture**: MVVM + Unidirectional Data Flow (UDF)
*   **Media Core**: [AndroidX Media3](https://developer.android.com/media3) (ExoPlayer)
*   **Images**: [Coil](https://coil-kt.github.io/coil/)
*   **Graphics & Effects**:
    *   **AGSL Shaders**: 自定义着色器语言，用于实现复杂的玻璃折射与液态融合。
    *   **RenderEffect**: Android 12+ 高级渲染效果。
    *   **Haze**: Compose 第三方模糊与遮罩库。
*   **Persistence**: DataStore & Room

---

## 📦 构建与运行 (Build & Run)

### 环境要求
*   Android Studio Koala Feature Drop (2024.1.2) 或更高版本
*   JDK 17+
*   Android SDK Platform API 35

### 编译步骤
1.  克隆仓库:
    ```bash
    git clone https://github.com/your-repo/vague-player.git
    ```
2.  在 Android Studio 中打开项目。
3.  同步 Gradle 依赖。
4.  运行 Debug 包:
    ```bash
    ./gradlew assembleDebug
    ```
    构建产物位于: `app/build/outputs/apk/debug/app-debug.apk`

---

## 🤝 贡献与致谢

Vague Player 的诞生离不开开源社区的灵感与支持。特别感谢：
*   **Google Deepmind** - Provided coding assistance via Agentic AI.
*   **Chris Banes** - For the amazing `Haze` library.

---

> *"Design is not just what it looks like and feels like. Design is how it works."*

© 2026 Yun. All Rights Reserved.






