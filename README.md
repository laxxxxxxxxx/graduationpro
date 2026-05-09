# 大学生心理健康教育与自测平台

> **主文档**：[项目说明文档.md](项目说明文档.md) — 功能详解、API清单、数据库设计、变更记录

## 🚀 快速开始（3步）

```bash
# 1. 初始化数据库（按顺序执行）
mysql -u root -p < init_database.sql
mysql -u root -p < init_assessment_questions.sql
mysql -u root -p < init_recommendation.sql
mysql -u root -p < upgrade_assessment_report.sql

# 2. 启动后端
cd backend
mvn clean compile -DskipTests
mvn spring-boot:run

# 3. 微信开发者工具导入 frontend 目录
```

**测试账号**：admin / admin123（管理员），student001~008 / 123456（学生）  
**API文档**：http://localhost:8080/api/doc.html

---

## 📂 项目简介

- **前端**：UniApp 微信小程序
- **后端**：Spring Boot 2.7.18 + MyBatis Plus 3.5.3
- **数据库**：MySQL 5.7
- **核心创新**：混合推荐算法（User-CF + Item-CF + Content-Based）+ 测评联动推荐画像

## 📚 文档索引

| 文档 | 用途 |
|------|------|
| **[项目说明文档.md](项目说明文档.md)** | 🔥 唯一真相源（功能/API/数据库/变更记录） |
| [使用指南.md](使用指南.md) | 详细使用教程 |
| [答辩指南.md](答辩指南.md) | 毕业设计答辩准备 |

---

**最后更新**：2026-05-07 · **版本**：v2.0
