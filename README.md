# BirthKeeper

离线生日记录与提醒 Android 应用。

## 项目状态（截至 2026-02-13）
- 核心功能已初步实现，可在真机完成主流程使用。
- 第 1-5 周能力已落地：联系人管理、身份证解析、OCR 回填、本地提醒、隐私加密、备份恢复。
- 第 6 周主要在收口：测试补齐基本完成，`release` 口径冷启动已达标。

## 已落地能力
- 联系人：新增/编辑/删除/查询、身份证号脱敏展示。
- 身份证解析：格式校验、校验位验证、生日/性别/年龄提取。
- OCR：CameraX/相册入口、ML Kit 离线识别、失败手动兜底回填。
- 提醒：WorkManager 每日扫描 + AlarmManager 精确补偿 + 通知点击跳转。
- 隐私：身份证号 AES-GCM 加密存储、日志脱敏。
- 备份恢复：本地导出/导入（覆盖与合并）+ 事务回滚。
- 测试：单元测试、集成测试、关键真机 `androidTest` 已覆盖。

## 当前差距
- 冷启动在 `debug` 口径波动较大（仅作为开发参考，不作为发布验收口径）。
- 当前回归结果（真机 `V2171A`，20 次 COLD）：
  - `debug`：`TotalTime` 平均 `1477.8ms`，`P95 1605ms`
  - `release`：`TotalTime` 平均 `453.6ms`，`P95 535ms`（已满足 `<1.5s`）

详见：`docs/第6周-性能回归记录.md`
- 隐私与发布检查：`docs/发布前隐私与发布检查清单.md`

## 目录说明
- `app`：应用入口、导航、提醒调度与通知链路。
- `feature-person`：联系人列表/编辑与录入流程。
- `feature-capture`：身份证拍照/选图/OCR/回填。
- `feature-reminder`：提醒配置与提醒中心。
- `core-domain`：实体、UseCase、仓储接口。
- `core-data`：Room、DAO、Repository、迁移与备份实现。
- `core-common`：日期、脱敏、加解密等公共工具。

## 常用命令
先注入环境：

```powershell
. "D:\Video Downloader\环境一键配置.ps1"
```

构建与测试：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

冷启动基线采集：

```powershell
.\scripts\startup-benchmark.ps1 -Runs 20 -OutputCsv .\docs\startup-benchmark-2026-02-13.csv
```

`release` 口径（先安装 release 包）：

```powershell
.\gradlew.bat :app:installRelease
.\scripts\startup-benchmark.ps1 -Runs 20 -OutputCsv .\docs\startup-benchmark-2026-02-13-release.csv
```
