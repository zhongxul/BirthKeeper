# 生日记录与提醒安卓 App 技术与实现设计（离线版）

> 基于《生日记录提醒App-初步设计方案（纯离线版）》进行工程化落地，目标是让团队可直接进入开发与联调。

---

## 1. 范围与约束

## 1.1 项目范围
本版本仅实现以下核心能力：
- 本地联系人生日管理（增删改查、分组、备注）；
- 身份证号解析生日/性别/年龄；
- 身份证图片离线 OCR 识别并回填；
- 本地提醒（提前 N 天 + 当天）；
- 本地备份与恢复。

## 1.2 非目标（明确不做）
- 云端账号系统、云同步、多设备实时共享；
- 在线 OCR、在线 NLP、在线推荐服务；
- 服务端 API 与远程数据库。

## 1.3 平台与运行要求
- Android 8.0（API 26）及以上；
- 首发仅中文界面；
- 离线可用，不依赖网络。

## 1.4 实施状态同步（截至 2026-02-13）
- 架构与模块拆分已按本文落地：`app`、`feature-*`、`core-*`。
- 核心业务实现已可运行：
  - 联系人管理、身份证解析、OCR 回填、本地提醒、备份恢复。
  - 身份证号加密存储、脱敏展示、导入失败事务回滚。
- 测试状态：
  - 单元测试与仓储集成测试已补齐关键路径并通过。
  - 真机 `androidTest` 已覆盖新增联系人、通知跳转、OCR 兜底回填。
- 当前主要差距：
  - `debug` 口径冷启动波动较大（真机 20 次 COLD：`TotalTime P95=1605ms`，开发参考）。
  - `release` 口径冷启动已达标（真机 20 次 COLD：`TotalTime P95=535ms`）。

---

## 2. 总体技术架构

采用 **Clean-ish MVVM + Repository + Local First**：

- **UI 层（Compose）**：页面展示、交互事件、状态渲染；
- **Domain 层（UseCase）**：业务规则（身份证解析、提醒生成、脱敏显示）；
- **Data 层（Repository）**：统一访问 Room、加密存储、OCR 组件；
- **System 层**：WorkManager/AlarmManager、Notification、CameraX、ML Kit。

### 2.1 分层依赖约束
- UI 仅依赖 ViewModel，不直接访问数据库；
- UseCase 不依赖 Android UI API；
- Repository 对外暴露接口，不泄漏底层实现细节。

### 2.2 模块拆分建议
- `app`：应用入口、DI 装配、导航；
- `feature-person`：联系人与详情、编辑；
- `feature-capture`：身份证拍摄/上传/OCR；
- `feature-reminder`：提醒配置与提醒列表；
- `core-data`：Room、DAO、RepositoryImpl；
- `core-domain`：UseCase、Entity、Validator；
- `core-common`：工具类（日期、脱敏、日志、Result 封装）。

---

## 3. 关键业务实现

## 3.1 身份证号解析
输入 18 位身份证号后执行：
1. 格式检查（长度、数字位、末位 X）；
2. 校验位验证（ISO 7064:1983, MOD 11-2）；
3. 提取出生日期（第 7-14 位）并校验有效性；
4. 提取性别（第 17 位奇偶）；
5. 按当前日期计算年龄。

### 3.1.1 Kotlin 伪代码
```kotlin
fun parseIdCard(id: String, now: LocalDate): ParseResult {
    if (!IdValidator.isFormatValid(id)) return ParseResult.Invalid("格式错误")
    if (!IdValidator.isChecksumValid(id)) return ParseResult.Invalid("校验位错误")

    val birth = LocalDate.parse(id.substring(6, 14), DateTimeFormatter.ofPattern("yyyyMMdd"))
    val gender = if (id[16].digitToInt() % 2 == 1) Gender.MALE else Gender.FEMALE
    val age = Period.between(birth, now).years

    return ParseResult.Valid(birth, gender, age)
}
```

## 3.2 身份证 OCR（离线）
流程：
1. CameraX 拍摄或相册选图；
2. 图像预处理（旋转矫正、裁剪提示、压缩）；
3. ML Kit 文本识别；
4. 用正则抽取姓名/身份证号；
5. 身份证号再走解析校验；
6. 回填表单，用户确认保存。

### 3.2.1 OCR 失败兜底策略
- 未识别到身份证号：提示“请拍摄正面、避免反光”；
- 识别到多个候选号：展示候选列表供用户选择；
- 校验位失败：提示手动修改，不自动保存。

## 3.3 提醒调度
采用“双保险”策略：
- **WorkManager**：每日固定时段扫描未来 N 天生日；
- **AlarmManager**：对当天即将触发的提醒做精确定时补偿。

触发规则：
- 用户配置 `offsets=[7,3,1,0]`；
- 计算目标日期 `birthday - offset`；
- 目标日期匹配今天时创建通知；
- 通知点击进入详情页。

## 3.4 农历支持（二期）
- 引入本地农历换算工具库（纯离线）；
- 按每年公历映射生成当年提醒计划；
- 闰月生日需在设置页让用户选择提醒规则（闰月当天 / 非闰同月同日）。

---

## 4. 本地数据设计

## 4.1 Room 实体

### 4.1.1 `person`
- `id: Long (PK)`
- `name: String`
- `id_number_encrypted: String?`
- `birthday_solar: String (yyyy-MM-dd)`
- `birthday_lunar: String?`
- `gender: Int`
- `relation: String`
- `note: String?`
- `avatar_uri: String?`
- `is_deleted: Int`（软删除，便于恢复）
- `created_at: Long`
- `updated_at: Long`

### 4.1.2 `reminder_config`
- `id: Long (PK)`
- `person_id: Long (FK -> person.id)`
- `offsets_json: String`（如 `[7,3,1,0]`）
- `remind_time: String`（HH:mm）
- `enabled: Int`

### 4.1.3 `reminder_log`
- `id: Long (PK)`
- `person_id: Long`
- `target_date: String`
- `offset_day: Int`
- `status: Int`（planned/sent/clicked/done）
- `created_at: Long`

## 4.2 索引建议
- `person(name)`：搜索优化；
- `person(birthday_solar)`：生日列表排序；
- `reminder_log(target_date, status)`：每日任务扫描。

## 4.3 数据迁移策略
- Room Migration 必须显式编写，不允许 `fallbackToDestructiveMigration`；
- 每次 schema 变更同步更新 `schema/` 导出文件并写迁移测试。

---

## 5. 安全与隐私实现

## 5.1 敏感数据存储
- 身份证号使用 AES-GCM 加密后再入库；
- 密钥由 Android Keystore 管理，禁止明文硬编码；
- UI 默认脱敏展示，仅在编辑态短暂明文显示。

## 5.2 图片与缓存
- OCR 源图仅放在应用私有目录缓存；
- 识别成功后默认删除临时图；
- 用户若主动保存原图，需二次确认并提示风险。

## 5.3 日志规范
- 生产日志禁止输出身份证号、姓名全量信息；
- 使用 `userId/hash`、`maskedId` 替代。

---

## 6. 页面与交互落地

## 6.1 页面清单
1. 首页（最近生日 + 快捷新增）；
2. 联系人列表页（搜索/筛选/分组）；
3. 新增/编辑页（手填、身份证号解析、OCR 回填）；
4. 日历页（月视图标记生日）；
5. 提醒中心（待处理、已处理）；
6. 设置页（提醒规则、备份恢复、隐私设置）。

## 6.2 状态流建议
- `UiState`：Loading / Success / Empty / Error；
- 单次事件（Toast、导航）使用 `Channel/SharedFlow`；
- 输入校验错误用字段级错误态（`nameError`, `idError`）。

---

## 7. 本地备份与恢复

## 7.1 导出
- 导出内容：`person + reminder_config`；
- 导出格式：JSON（或 protobuf）+ 文件级 AES 加密；
- 文件后缀建议：`.bkup`。

## 7.2 导入
- 校验文件版本号与完整性；
- 支持“覆盖导入”与“合并导入”；
- 冲突处理：按 `updated_at` 较新优先，并提供明细预览。

## 7.3 失败回滚
- 导入流程包裹在数据库事务中；
- 任一步骤失败即回滚并提示原因。

---

## 8. 测试与质量保障

## 8.1 单元测试
- 身份证解析（正常/异常/边界）；
- 日期与年龄计算（闰年、生日当天）；
- 提醒计划生成（offset 组合）；
- 脱敏逻辑与加解密工具。

## 8.2 集成测试
- Room CRUD + Migration；
- WorkManager 调度与通知创建；
- 备份导入导出闭环。

## 8.3 UI 测试
- 新增联系人全流程；
- OCR 识别失败后手动修正；
- 提醒通知点击跳转正确性。

## 8.4 验收指标
- OCR 识别成功率（清晰样本）≥ 90%；
- 本地提醒触发成功率 ≥ 95%；
- 本地恢复成功率 ≥ 99%；
- 冷启动 < 1.5s（中端机）。

---

## 9. 开发排期建议（示例 6 周）

- **第 1 周**：项目脚手架、数据库 schema、基础页面框架；
- **第 2 周**：联系人 CRUD + 身份证号解析；
- **第 3 周**：CameraX + ML Kit OCR + 回填流程；
- **第 4 周**：提醒调度（WorkManager/AlarmManager）+ 通知链路；
- **第 5 周**：备份恢复 + 隐私加密；
- **第 6 周**：测试补齐、性能优化、发布准备。

---

## 10. 风险与应对

1. **部分机型后台限制导致提醒延迟**  
   应对：WorkManager + AlarmManager 双策略 + 厂商白名单引导。

2. **身份证 OCR 对反光/遮挡敏感**  
   应对：拍摄引导蒙层 + 实时质量提示 + 手动编辑兜底。

3. **本地备份文件被误删**  
   应对：提供“最近导出记录”与导出后提示用户复制到安全位置。

4. **敏感数据安全风险**  
   应对：默认脱敏、加密存储、一键清空、日志脱敏四层保护。

---

## 11. 交付物清单
- 技术设计文档（本文）；
- 数据库 ER 与 migration 列表；
- 核心模块接口文档（Repository / UseCase）；
- 测试用例清单（单测/集成/UI）；
- 上线前隐私自检清单。

