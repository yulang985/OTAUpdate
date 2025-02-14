# OTAUpdate 项目说明

## 项目概述
该项目主要涉及蓝牙低功耗（BLE）相关的操作，包括读写蓝牙特性、处理连接超时、读取RSSI值、读取PHY等功能。项目主要包含以下几个模块：

- `blelib`：处理蓝牙连接和数据交互的核心库。
- `otalib`：OTA（Over-The-Air）更新相关的库。
- `app`：Android应用模块。

## 构建与运行

### 环境要求
- Android SDK 编译版本：31
- 最低支持的 Android SDK 版本：21
- Java 版本：1.8

### 构建步骤
1. 克隆或下载本项目到本地。
2. 打开 Android Studio 并导入该项目。
3. 等待 Android Studio 同步项目依赖。

### 运行项目
在 Android Studio 中选择合适的模拟器或真机设备，然后点击运行按钮即可。

## 项目结构
### 主要模块
- **`OTAUpdate/app`**：Android 应用模块，包含应用的主要代码和资源。
- **`OTAUpdate/blelib`**：蓝牙低功耗库，处理蓝牙连接、读写特性等核心功能。
- **`OTAUpdate/otalib`**：OTA 更新库，负责设备的 OTA 升级相关操作。

### 重要文件说明
- **`build.gradle` 文件**：
  - `OTAUpdate/app/build.gradle`：应用模块的构建配置文件，包含应用的依赖库和编译选项。
  - `OTAUpdate/blelib/build.gradle`：蓝牙库的构建配置文件，定义了蓝牙库的编译选项和依赖。
  - `OTAUpdate/otalib/build.gradle`：OTA 库的构建配置文件，包含 OTA 库的依赖和编译设置。

### 主要类和函数
- **`Connector.java`**：
  - `asyncReadCharacteristic`：异步读取蓝牙特性。
  - `syncReadSingle`：同步单次读取蓝牙特性。
  - `syncReadRepeat`：同步重复读取蓝牙特性。
  - `addRunnable`：添加一个可运行的任务，用于处理连接超时。

- **`Connection.java`**：
  - `writeSingle`：单次写入蓝牙特性。
  - `read`：读取蓝牙特性。
  - `readRssi`：读取蓝牙设备的 RSSI 值。
  - `readPhy`：读取蓝牙设备的 PHY 值。

- **`AbstractOTAManager.java`**：
  - `write`：向蓝牙特性写入数据。
  - `writeAndRead`：先写入数据，然后读取蓝牙特性的值。

## 依赖信息
### 主要依赖库
- **AndroidX 相关库**：
  - `androidx.appcompat:appcompat:1.3.0`
  - `com.google.android.material:material:1.4.0`

- **测试相关库**：
  - `junit:junit:4.13.2`
  - `androidx.test.ext:junit:1.1.3`
  - `androidx.test.espresso:espresso-core:3.4.0`

- **网络相关库**：
  - `com.squareup.okhttp3:okhttp:4.9.0`

- **JSON 解析库**：
  - `com.alibaba:fastjson:1.1.72.android`

- **RxJava 相关库**：
  - `io.reactivex.rxjava2:rxjava:2.2.8`
  - `io.reactivex.rxjava2:rxandroid:2.1.1`
  - `com.github.tbruyelle:rxpermissions:0.10.2`

## 注意事项
- 在使用某些功能时，需要注意 Android 系统版本的兼容性，例如 `readPhy` 函数需要 Android 8.0（API 级别 26）及以上版本。
- 请确保设备的蓝牙功能正常开启，否则可能会影响蓝牙相关功能的使用。

## 贡献指南
如果你发现了项目中的问题或者有新的功能建议，欢迎通过以下方式贡献：

1. 提交 Issue：在项目的 GitHub 仓库中提交详细的问题描述或功能建议。
2. 提交 Pull Request：如果你有代码修改或新功能实现，欢迎提交 Pull Request 到项目的 `develop` 分支。

## 许可证
本项目采用 [MIT 许可证](https://opensource.org/licenses/MIT)，详情请查看 `LICENSE` 文件。
