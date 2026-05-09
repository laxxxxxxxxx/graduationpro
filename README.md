# 大学生心理健康教育与自测平台

本项目是一个面向大学生的心理健康教育与自测系统，包含微信小程序前端、Spring Boot 后端和 MySQL 数据库。核心能力包括学习资源、心理测评、情绪日记、匿名社区、个人中心，以及基于用户行为、资源内容和心理画像的混合推荐。

## 技术栈

- 前端：UniApp / Vue，面向微信小程序
- 后端：Spring Boot 2.7.18，MyBatis Plus 3.5.3
- 数据库：MySQL 5.7，字符集 utf8mb4
- 推荐：User-CF、Item-CF、Content-Based、Profile-Based 混合排序

## 项目结构

```text
.
├── backend/                    # 后端服务
├── frontend/                   # UniApp 小程序
├── init_database.sql           # 建库、建表、基础字典和管理员账号
├── init_assessment_questions.sql # 心理测评题目
├── init_recommendation.sql     # 推荐系统表和演示数据
├── init_resource_content.sql   # 学习资源正文、简介和媒体地址
└── README.md                   # 项目入口文档
```

## 快速开始

### 1. 初始化数据库

按顺序执行：

```bash
mysql -u root -p < init_database.sql
mysql -u root -p mental_health < init_assessment_questions.sql
mysql -u root -p mental_health < init_recommendation.sql
mysql -u root -p mental_health < init_resource_content.sql
```

`init_database.sql` 已合并资源互动、测评报告、社区分类等结构，不再需要额外执行历史升级脚本。

### 2. 启动后端

```bash
cd backend
mvn clean compile -DskipTests
mvn spring-boot:run
```

默认接口地址：`http://localhost:8080/api`

数据库连接配置位于 `backend/src/main/resources/application.yml`。

### 3. 启动前端

使用微信开发者工具导入 `frontend` 目录。前端请求封装位于 `frontend/utils/request.js`，如后端地址变化，在这里调整基础 URL。

## 测试账号

| 账号 | 密码 | 说明 |
| --- | --- | --- |
| admin | admin123 | 管理员 |
| student001 ~ student008 | 123456 | 推荐系统演示用户 |

## 功能模块

| 模块 | 主要能力 |
| --- | --- |
| 用户认证 | 注册、登录、JWT 鉴权、个人资料 |
| 学习资源 | 分类筛选、详情、学习记录、点赞、收藏、评论 |
| 心理测评 | SDS、SCL-90、UPI 量表答题、报告生成、历史记录 |
| 情绪日记 | 情绪记录、列表、趋势统计 |
| 匿名社区 | 分类发帖、评论、点赞、我的发布、我的点赞 |
| 个性化推荐 | 用户协同过滤、物品协同过滤、内容推荐、心理画像推荐 |
| 个人中心 | 资料维护、隐私设置、学习记录 |

## SQL 维护原则

- 新环境初始化只维护四个脚本：`init_database.sql`、`init_assessment_questions.sql`、`init_recommendation.sql`、`init_resource_content.sql`。
- 表结构变化优先并入 `init_database.sql`，避免继续堆叠一次性迁移脚本。
- 临时排查、验证、演示脚本不要长期放在根目录；需要时放到本地临时文件或重新生成。

## 常用验证

```bash
# 后端编译
cd backend
mvn clean compile -DskipTests

# 前端依赖
cd frontend
npm install
```

最后整理：2026-05-09
