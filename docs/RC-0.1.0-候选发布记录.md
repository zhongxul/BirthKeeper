# BirthKeeper RC 候选发布记录（0.1.0）

记录时间：2026-02-13  
记录版本：`0.1.0 (versionCode=1)`

## 1. 候选版本范围
- 核心功能：联系人管理、身份证解析、OCR 回填、本地提醒、备份恢复
- 测试补齐：单元测试、仓储集成测试、关键真机 `androidTest`
- 性能回归：`release` 口径冷启动基线已采集

## 2. 关键验证结果

### 2.1 真机自动化（V2171A / Android 15）
- `MainActivityAndroidTest`：4/4 通过
  - 新增联系人流程
  - 通知跳转编辑流程
  - OCR 手动兜底回填流程
  - 提醒状态流转：`PLANNED -> SENT -> CLICKED -> DONE`

### 2.2 冷启动性能（20 次 COLD）
- `debug`：`TotalTime` 平均 `1477.8ms`，`P95 1605ms`（开发参考）
- `release`：`TotalTime` 平均 `453.6ms`，`P95 535ms`（验收达标）

对应数据文件：
- `docs/startup-benchmark-2026-02-13.csv`
- `docs/startup-benchmark-2026-02-13-release.csv`

## 3. 隐私与发布检查
- 已完成：`docs/发布前隐私与发布检查清单.md`
- 已落实硬化：
  - `android:allowBackup="false"`
  - `android:usesCleartextTraffic="false"`

## 4. 当前阻塞与剩余动作
- 需补充至少 1 台中端机 `release` 冷启动复测
- 发布前需切换正式签名（当前 `release` 为本地回归签名）
- 备份加密口令策略需进一步强化（避免固定口令）

## 5. 复现命令（节选）

```powershell
. "D:\Video Downloader\环境一键配置.ps1"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.zhongxul.birthkeeper.MainActivityAndroidTest"
.\gradlew.bat :app:installRelease
.\scripts\startup-benchmark.ps1 -Runs 20 -OutputCsv .\docs\startup-benchmark-2026-02-13-release.csv
```
