# Nowl 校园交易与跑腿平台（前后端分离）

## 项目一句话

Nowl 是一个面向校园场景的二手交易与跑腿平台，围绕“内容治理 + 资金安全 + 可观测/可运营”设计：商品/跑腿发布接入风控与 AI 审核，订单链路通过分布式锁与延迟任务保证并发安全与状态一致，配套后台审核/纠纷裁定/风控中心，搜索推荐与消息通知构成完整闭环。

## 项目经历（简历可直接使用，推荐版本）

**项目名称：** Nowl 校园交易与跑腿平台  
**项目角色：** 后端为主（含前端联调/全栈协作）  
**技术栈：** Spring Boot 3、Spring Security、MyBatis-Plus、Redis/Redisson、RocketMQ、Elasticsearch、XXL-JOB、Vue3 + TS + Vite + Pinia  

- 采用 Spring Boot 多模块拆分（`unimarket-web/security/core/admin/search/recommend/ai/gateway`），明确 Controller 入口层与核心业务域层边界，降低耦合并提升可维护性。
- 设计并落地“资金安全优先”的订单状态机：下单、支付托管、发货、收货、退款、纠纷互斥与结束态；用分布式锁串行化状态流转，避免重复操作导致的超卖/重复结算。
- 引入 Redis + Redisson 分布式锁，按业务维度设计锁粒度：
  - 商品下单互斥：`order:lock:goods:{productId}` 防止同一商品并发下单超卖。
  - 订单生命周期互斥：`order:lock:lifecycle:{orderId}` 串行化支付/发货/收货/退款。
  - 跑腿生命周期互斥：`errand:lock:lifecycle:{taskId}` 防止并发接单/重复完结。
  - 纠纷互斥：`dispute:create:{targetType}:{contentId}` 防重复发起；`dispute:reply:{recordId}` 防并发补充证据。
- 用 RocketMQ 实现异步化与最终一致：
  - 商品发布后先落库为“待审核 + 未上架”，在事务 `afterCommit` 发送审核消息，由 `GoodsAuditListener` 异步执行审核，避免未审核内容被浏览/下单。
  - 业务变更通过同步消息驱动 ES 索引更新（goods/errand），降低主链路耗时。
  - 使用延迟消息实现“订单自动确认收货（7天）/跑腿自动确认（24h）”等延迟任务，消费端抢 `order:lock:lifecycle` 与人工操作互斥，保证幂等与安全。
- 自研风控引擎（Risk Control）并提供后台风控中心：
  - 决策链路固定：行为管控（`risk_behavior_control`）→ 白名单 → 黑名单 → 高级信号（登录失败/IP突发/设备指纹窗口）→ 规则引擎（阈值/关键词，`windowMinutes + maxCount`）。
  - 风控中心支持事件/工单/规则管理，行为管控支持按事件类型封禁/限制并设置 `expire_time`（长期或定时失效）。
- 集成 AI 能力并做“能力层/业务层”拆分：
  - `unimarket-ai` 仅负责模型调用（文本/图片审核、聊天、估价），提示词统一约束为 JSON 输出并做解析兜底。
  - `core/aiassistant` 负责业务编排：风控接入、历史上下文、函数调用（Function Calling）与商品卡片填充；在服务端 Prompt 明确“严禁编造商品信息”，商品卡片一律由后端实时查询填充，避免模型幻觉污染业务。
- 构建 Elasticsearch 搜索闭环：
  - 商品搜索 multiMatch 覆盖 `title/title.pinyin/description/categoryName`，关键词场景按 `_score + hotScore + 互动指标` 综合排序，支持 `<em>` 高亮。
  - 热搜/历史通过 Redis 维护（List + ZSet），接口提供 hot/suggest/history/clear-history 完整能力。
- 通知系统落地“后端决定跳转目标”：通知落库后 WebSocket 推送；后端根据 `type + relatedId` 解析 `bizType`，前端只做路由映射与 `markAsRead`，降低前端写死跳转导致的错误。
- 为推荐系统提供离线计算入口：XXL-JOB 定时任务计算商品相似度矩阵（协同过滤 + 内容相似）与用户偏好画像，在线侧做融合兜底输出。

## 精简版要点（投递时 5-7 条）

- 多模块后端拆分 + 网关统一入口，清晰分层（入口/业务域/后台治理/搜索推荐/AI/风控）。
- Redisson 分布式锁按商品/订单/跑腿/纠纷设计锁粒度，解决并发下单超卖、重复结算与状态叠加。
- RocketMQ 异步审核 + ES 同步 + 延迟任务（自动确认收货/跑腿自动确认），事务 afterCommit 投递保证一致性。
- 风控引擎：行为管控、黑白名单、高级信号窗口、阈值/关键词规则（`windowMinutes`），后台风控中心可运营。
- AI 审核/对话/估价：能力层与业务编排层拆分；服务端 Function Calling 严禁编造，卡片数据后端实时查询填充。
- ES 搜索：multiMatch（含拼音）+ 高亮 + 排序策略；Redis 热搜/历史闭环。
- 通知系统：落库 + WebSocket 推送；后端解析 `bizType` 驱动前端跳转与已读。

## 可量化信息（建议你补充后更像“真人项目”）

以下数值请你按实际情况补上（不要编）：

- 峰值并发：下单/接单接口 QPS、并发人数、是否压测。
- 数据规模：ES 索引商品/跑腿条数，热搜/历史留存策略（如 7 天/10 条）。
- 性能指标：发布/搜索/下单平均耗时，慢 SQL 优化点。
- 稳定性：并发测试用例覆盖的场景（重复提交、双管理员竞争审核、延迟消息重复消费等）。

## 面试官高频追问（你要能讲清楚）

- “你们怎么防止同一商品超卖？”
  - 讲锁粒度：`order:lock:goods:{productId}`，先锁再读商品状态与审核状态，再写订单与改商品状态。
- “为什么 MQ 要 afterCommit 才发？”
  - 讲一致性：避免事务回滚但消息已发导致消费者读不到数据或错误同步。
- “风控能控制多久？规则如何配置？”
  - 行为管控/黑白名单看 `expire_time`；阈值规则看 `rule_config.windowMinutes`；高级信号窗口写在代码里（如登录失败 30m、设备指纹 24h、IP 突发 5m）。
- “AI 怎么避免胡说八道？”
  - 能力层只输出结构化结果；业务层通过 Function Calling 先查真实数据，再由服务端填充 cards；Prompt 明确禁止编造。
- “消息跳转为什么要后端决定？”
  - 后端掌握业务类型与权限边界，避免前端写死导致的‘不存在/无权’错误与路由不一致。

## 参考文档（面试前自查）

- `E:\Nowl\docs\Nowl系统设计说明书.md`
- `E:\Nowl\docs\中间件部署文档.md`

