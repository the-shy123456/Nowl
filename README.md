<div align="center">

# 🌙 Nowl · 校园商品 × 跑腿 × AI 一体化平台

<p>
  <strong>Night Owl（夜猫子）校园交易平台</strong><br/>
  把「二手交易」「校园跑腿」「AI 助手」「后台治理」融合在一个完整系统中
</p>

<p>
  <img src="https://img.shields.io/badge/Frontend-Vue%203%20%2B%20TS-42b883?style=for-the-badge" alt="frontend" />
  <img src="https://img.shields.io/badge/Backend-Spring%20Boot%203-6db33f?style=for-the-badge" alt="backend" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="license" />
</p>

<p>
  <a href="#-项目亮点">项目亮点</a> ·
  <a href="#-功能矩阵">功能矩阵</a> ·
  <a href="#-快速开始">快速开始</a> ·
  <a href="#-系统架构">系统架构</a> ·
  <a href="#-文档导航">文档导航</a>
</p>

</div>

---

## ✨ 项目亮点

- **场景融合**：商品交易 + 跑腿服务 + AI 互动推荐，覆盖校园高频需求
- **治理完善**：包含后台管理、IAM 权限体系、风控链路、纠纷处理闭环
- **工程化完整**：前后端分层清晰，后端多模块，支持中间件按需启用
- **学习价值高**：适合作为课程设计、毕业设计或中大型全栈项目实践样板

---

## 🖼 界面预览

<p>
  <img src="docs/assets/home.png" width="48%" alt="首页" />
  <img src="docs/assets/market.png" width="48%" alt="集市" />
</p>
<p>
  <img src="docs/assets/errand.png" width="48%" alt="跑腿" />
  <img src="docs/assets/admin.png" width="48%" alt="后台管理" />
</p>
<p>
  <img src="docs/assets/ChatAI.png" width="48%" alt="AI 对话" />
</p>

---

## 🧩 功能矩阵

### 用户侧能力

- **商品链路**：发布 → 风控 → AI 审核 →（必要时）人工复核 → 上架
- **订单链路**：下单防超卖锁 → 托管阶段 → 退款/纠纷互斥 → 结算/结束
- **跑腿链路**：发布与审核 → 接单互斥 → 履约 → 延迟自动确认
- **社交能力**：关注 / 拉黑 / 私聊 / 消息中心
- **AI 能力**：AI 对话、在售商品推荐、智能审核辅助

### 平台治理能力

- **多级权限**：超管、学校级/校区级管理员、运营角色（IAM）
- **风控体系**：黑白名单、阈值规则、关键词策略、高级信号、工单管理
- **搜索推荐**：Elasticsearch 检索高亮 + Redis 热搜/历史 + 推荐在线融合
- **通知系统**：消息落库 + WebSocket 推送 + `bizType` 业务跳转

---

## 🏗 系统架构

- **前端**：Vue 3 + TypeScript + Vite + Pinia
- **后端**：Spring Boot 3 多模块（`web / security / core / admin / search / recommend / ai / gateway`）
- **中间件（按需启用）**：MySQL、Redis、RocketMQ、Elasticsearch、XXL-JOB

仓库结构：

```text
Nowl/
├── Nowl-front/     # Vue3 前端
├── Nowl-backend/   # Spring Boot 多模块后端
├── sql/            # 数据库初始化脚本（推荐执行这里）
└── docs/           # 设计说明书 / 中间件部署文档
```

---

## 🚀 快速开始

### 1) 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+（建议）与 npm

最低中间件要求：

- MySQL 8+
- Redis 6+

可选中间件（不启用则功能降级）：

- RocketMQ（异步审核、索引同步、延迟任务）
- Elasticsearch（检索与高亮）
- XXL-JOB（推荐离线任务）

### 2) 初始化数据库

推荐执行根目录初始化脚本（建库建表 + 角色权限 + 风控规则 + 分类/学校种子数据）：

```bash
mysql -u root -p < sql/nowl_init.sql
```

### 3) 配置环境变量

参考根目录 `.env.example`（请勿提交真实密钥）。

> 后端通过系统环境变量或 IDE 运行配置读取，`.env` 不会被 Spring Boot 自动加载。

必需项：

- `DB_URL` `DB_USERNAME` `DB_PASSWORD`
- `REDIS_HOST` `REDIS_PORT`（有密码再加 `REDIS_PASSWORD`）
- `JWT_SECRET`

可选项：

- AI：`OPENAI_API_KEY` `OPENAI_BASE_URL` `OPENAI_MODEL`
- COS：`COS_SECRET_ID` `COS_SECRET_KEY` `COS_REGION` `COS_BUCKET_NAME` `COS_BASE_URL`
- MQ：`ROCKETMQ_NAME_SERVER`
- ES：`ES_HOST`
- XXL：`XXL_JOB_ADMIN_ADDRESSES`
- 短信：`SMS_ENABLED` `SMS_SPUG_URL`

### 4) 启动后端业务服务（8080）

```bash
cd Nowl-backend
mvn -q -DskipTests -pl unimarket-web -am spring-boot:run
```

### 5) 启动网关（推荐，8090）

```bash
cd Nowl-backend
mvn -q -DskipTests -pl unimarket-gateway -am spring-boot:run
```

### 6) 启动前端（5173）

```bash
cd Nowl-front
npm install
npm run dev
```

开发环境访问：

- Frontend: `http://localhost:5173`
- Gateway: `http://localhost:8090`
- Backend: `http://localhost:8080`

---

## 🔐 管理员初始化（可选）

初始化 SQL 会注入角色与权限点，但不会自动创建管理员账号。
可先注册普通账号，再在数据库绑定管理员角色与范围。

```sql
INSERT INTO iam_user_role(user_id, role_id, status)
SELECT 123, role_id, 1
FROM iam_role
WHERE role_code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE status = 1;

INSERT INTO iam_admin_scope_binding(user_id, scope_type, status)
VALUES (123, 'ALL', 1);
```

> 将 `123` 替换为你的实际用户 ID。

---

## 📚 文档导航

- 系统设计说明书：`docs/Nowl系统设计说明书.md`
- 中间件部署文档：`docs/中间件部署文档.md`
- 项目经历说明：`docs/简历项目经历-Nowl.md`
- 风控压测说明：`docs/风控压测说明.md`

---

## 🤝 适用人群

- 想做**毕业设计 / 课程设计**的同学
- 想学习**Vue + Spring Boot 全栈工程化**实践的开发者
- 想了解**校园交易 + 平台治理 + 风控体系**落地方式的同学

如果这个项目对你有帮助，欢迎点个 **Star** ⭐

---

## 📄 License

MIT License，详见 `LICENSE`。
