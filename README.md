# V2EX LogScope

<!-- Plugin description -->
V2EX LogScope is an IntelliJ IDEA plugin that renders V2EX content in a log-style window so you can keep up with the community without leaving your IDE.  
V2EX LogScope 是一款 IntelliJ IDEA 插件，通过日志风格的工具窗口在 IDE 内隐蔽浏览 V2EX 话题。

Key Features  
主要特性

- Stealth log-themed UI with MASK toggle  
  隐匿日志风格界面，并提供 MASK 切换
- Browse different nodes (Hot, Tech, Creative, Play, Hot Topics, All, etc.)  
  覆盖热门、技术、创意、好玩、最热、全部等多种节点
- View topic summaries, details and replies with pagination  
  支持话题摘要、详情与回复分页阅读
- Proxy configuration and API Token management  
  内置代理与 API Token 设置面板
- Keyboard-friendly controls and compact layout  
  紧凑布局，交互体验贴近终端日志

Technical Features  
技术特点

- Uses V2EX API v2  
  使用 V2EX API v2
- Supports HTTP/SOCKS proxy  
  支持 HTTP/SOCKS 代理
- Simple layout focused on readability  
  以易读性为优先的简洁布局

<!-- Plugin description end -->

## What's New (0.0.4)

- Added inline reply composer with API v2 + legacy fallback，支持在 IDE 内直接回复 V2EX 话题  
- Reply composer is hidden behind a toolbar toggle to keep the log view compact，新增工具栏按钮控制回复框显隐  
- Fixed several localized labels that previously displayed as garbled text，修复资源文件中的中文乱码

## Installation Requirements

- IntelliJ IDEA 2023.2.3 (build 232.*) – currently the only verified version; newer builds pending validation  
  IntelliJ IDEA 2023.2.3（构建 232.*），暂时唯一实测版本，后续版本待验证
- API Token from the V2EX settings page

## Usage

1. 打开 `Settings -> Tools -> V2EX LogScope` 配置 API Token  
2. 根据需要填写代理信息  
3. 打开底部工具栏中的 `System Log` 工具窗口  
4. 在左侧节点区域选择任意 V2EX 分类  
5. 点击任意日志行即可阅读话题与回复；需要伪装时使用 MASK 切换

## Developer

[@jsnjfz](https://github.com/jsnjfz)

## Acknowledgements

Configuration UI is inspired by [FormatToday/v2-viewer](https://github.com/FormatToday/v2-viewer)  
配置界面参考了 [FormatToday/v2-viewer](https://github.com/FormatToday/v2-viewer)，感谢原作者的优秀实现

## Screenshots

![LogScope screenshot](page.jpg)

## Repository

[logscope](https://github.com/jsnjfz/logscope)
