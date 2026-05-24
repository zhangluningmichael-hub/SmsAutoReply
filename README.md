# 短信自动回复 (SmsAutoReply)

一个 Android 短信自动回复与转发应用，支持自定义规则编辑。

## 功能特点

- **短信监听**：后台服务实时监听接收到的短信
- **自动回复**：根据规则自动回复短信
- **自动转发**：根据规则将短信转发到指定号码
- **规则引擎**：
  - 关键词匹配（包含/等于/开头/正则）
  - 号码匹配（精确/开头/包含/任意）
  - 动作：回复、转发、回复+转发
  - 规则排序（拖动改变优先级）
- **管理界面**：
  - 规则列表（开关、拖动排序、删除）
  - 规则编辑（新建/编辑）
  - 操作日志（历史记录）
  - 设置（服务开关、黑白名单）
- **数据存储**：使用 Room 数据库持久化
- **通知提醒**：操作时发送通知

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Gradle 8.5
- Android SDK 34 (Android 14)
- 最低支持 Android 7.0 (API 24)

## 编译步骤

### 方法一：使用 Android Studio（推荐）

1. **打开项目**
   - 启动 Android Studio
   - 点击 `File` → `Open...`
   - 选择 `SmsAutoReply` 项目根目录
   - 点击 `OK`

2. **等待 Gradle 同步**
   - Android Studio 会自动检测并同步 Gradle 配置
   - 如果提示下载 SDK，按照提示安装 Android SDK 34

3. **连接设备或启动模拟器**
   - 用 USB 连接 Android 设备（开启开发者选项和 USB 调试）
   - 或者启动一个模拟器（API 24+）

4. **编译并运行**
   - 点击工具栏绿色的 `Run` 按钮（或按 `Shift+F10`）
   - 选择目标设备
   - 等待编译和安装完成

### 方法二：命令行编译

1. **配置 Gradle Wrapper**（如果项目中没有 gradlew）

   Windows:
   ```
   gradle wrapper --gradle-version 8.5
   ```

2. **编译 APK**

   Windows:
   ```
   gradlew assembleDebug
   ```

3. **APK 位置**
   - Debug APK：`app/build/outputs/apk/debug/app-debug.apk`
   - Release APK：`app/build/outputs/apk/release/app-release.apk`

### 方法三：生成签名 APK（发布版）

1. 在 Android Studio 中点击 `Build` → `Generate Signed Bundle / APK`
2. 选择 `APK`
3. 创建或选择已有的 Key Store
4. 填写签名信息
5. 选择 `release` 构建类型
6. 点击 `Finish`

## 首次使用

1. **安装 APK 后打开应用**
2. **授予权限**：
   - 短信接收权限（`RECEIVE_SMS`）
   - 短信发送权限（`SEND_SMS`）
   - 通知权限（Android 13+）
3. **添加规则**：
   - 点击「规则管理」→ 右下角「+」→ 填写规则信息 → 保存
4. **启动服务**：
   - 进入「设置」→ 打开「自动回复服务」

## 项目结构

```
SmsAutoReply/
├── app/
│   ├── src/main/
│   │   ├── java/com/smsautoreply/app/
│   │   │   ├── MainActivity.java        # 主入口
│   │   │   ├── SmsReceiver.java          # 短信接收广播
│   │   │   ├── SmsService.java           # 后台服务
│   │   │   ├── RuleEngine.java           # 规则引擎
│   │   │   ├── NotificationHelper.java   # 通知帮助类
│   │   │   ├── BootReceiver.java         # 开机自启动
│   │   │   ├── db/
│   │   │   │   ├── AppDatabase.java      # 数据库
│   │   │   │   ├── RuleDao.java          # 规则 DAO
│   │   │   │   ├── LogDao.java           # 日志 DAO
│   │   │   │   ├── RuleEntity.java       # 规则实体
│   │   │   │   └── LogEntity.java        # 日志实体
│   │   │   ├── ui/
│   │   │   │   ├── RuleListActivity.java  # 规则列表
│   │   │   │   ├── RuleEditActivity.java  # 规则编辑
│   │   │   │   ├── LogActivity.java       # 日志页
│   │   │   │   └── SettingsActivity.java  # 设置页
│   │   │   └── adapter/
│   │   │       └── RuleAdapter.java       # 规则适配器
│   │   ├── res/                           # 资源文件
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── README.md
```

## 注意事项

1. **Android 11+ (API 30+)**：Google 限制了第三方应用读取短信的权限，部分设备可能需要设置为默认短信应用才能使用 `RECEIVE_SMS`。建议在支持的应用列表页面将该应用设为默认短信应用。

2. **Android 14 (API 34)**：前台服务需要声明 `foregroundServiceType`，本项目已正确处理。

3. **电池优化**：部分 OEM 厂商（华为、小米、OPPO 等）会限制后台运行，建议在系统设置中将本应用加入「受保护应用」或「无视电池优化」列表。

4. **权限说明**：
   - `RECEIVE_SMS`：监听收到的短信
   - `SEND_SMS`：自动回复短信
   - `READ_SMS`：读取短信内容
   - `FOREGROUND_SERVICE`：后台持久运行
   - `POST_NOTIFICATIONS`：发送通知（Android 13+）
   - `RECEIVE_BOOT_COMPLETED`：开机自启动

## 技术栈

- Java 8
- AndroidX
- Room Database (SQLite)
- Material Design 3 (Material Components)
- Gradle 8.5 + AGP 8.2.2
- minSdk 24, targetSdk 34, compileSdk 34
