# 数据库完整结构 - ER图绘制参考

**数据库名**：mental_health  
**字符集**：utf8mb4  
**表总数**：20个

---

## 一、用户模块（2个表）

### 1. user - 用户表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 学号/用户名 |
| password | VARCHAR(255) | NOT NULL | BCrypt加密密码 |
| email | VARCHAR(100) | UNIQUE | 邮箱 |
| phone | VARCHAR(20) | | 手机号 |
| real_name | VARCHAR(255) | | 真实姓名(AES加密) |
| gender | TINYINT | | 性别: 0-女 1-男 2-其他 |
| age | INT | | 年龄 |
| university | VARCHAR(100) | | 学校 |
| major | VARCHAR(100) | | 专业 |
| grade | VARCHAR(20) | | 年级 |
| role | TINYINT | DEFAULT 1 | 角色: 1-学生 2-咨询师 3-管理员 |
| avatar | VARCHAR(255) | | 头像URL |
| status | TINYINT | DEFAULT 1 | 状态: 0-禁用 1-正常 |
| openid | VARCHAR(100) | | 微信OpenID |
| session_key | VARCHAR(255) | | 微信SessionKey |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- idx_username (username)
- idx_email (email)
- idx_openid (openid)

---

### 2. user_privacy - 用户隐私设置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| user_id | BIGINT | FK, NOT NULL, UNIQUE | 用户ID → user.id |
| show_real_name | TINYINT | DEFAULT 0 | 是否显示真实姓名 |
| show_university | TINYINT | DEFAULT 0 | 是否显示学校 |
| allow_recommend | TINYINT | DEFAULT 1 | 是否允许个性化推荐 |
| data_retention_days | INT | DEFAULT 365 | 数据保留天数 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)

**关系**：user (1) : (1) user_privacy

---

## 二、教育资源模块（4个表）

### 3. resource_category - 资源分类表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 分类ID |
| name | VARCHAR(50) | NOT NULL | 分类名称 |
| parent_id | INT | DEFAULT 0 | 父分类ID（支持多级分类） |
| sort_order | INT | DEFAULT 0 | 排序 |
| status | TINYINT | DEFAULT 1 | 状态: 0-禁用 1-启用 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

### 4. education_resource - 教育资源表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 资源ID |
| title | VARCHAR(200) | NOT NULL | 资源标题 |
| type | TINYINT | NOT NULL | 类型: 1-文章 2-视频 3-音频 4-课程 |
| category_id | INT | NOT NULL | 分类ID → resource_category.id |
| cover_url | VARCHAR(255) | | 封面图URL |
| content | LONGTEXT | | 内容(HTML/链接) |
| duration | INT | | 时长(秒) |
| difficulty | TINYINT | | 难度: 1-入门 2-进阶 3-高级 |
| tags | VARCHAR(255) | | 标签(逗号分隔) |
| author | VARCHAR(50) | | 作者 |
| source | VARCHAR(100) | | 来源 |
| copyright_status | TINYINT | DEFAULT 1 | 版权状态: 1-已授权 |
| view_count | INT | DEFAULT 0 | 浏览量 |
| like_count | INT | DEFAULT 0 | 点赞数 |
| status | TINYINT | DEFAULT 0 | 状态: 0-待审核 1-已发布 2-下架 |
| publish_time | DATETIME | | 发布时间 |
| created_by | BIGINT | | 创建者ID → user.id |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- idx_category (category_id)
- idx_status (status)
- idx_type (type)
- ft_title_content (title, content) - 全文索引

**外键**：
- category_id → resource_category.id

**关系**：resource_category (1) : (N) education_resource

---

### 5. user_learning_record - 用户学习记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | FK, NOT NULL | 用户ID → user.id |
| resource_id | BIGINT | FK, NOT NULL | 资源ID → education_resource.id |
| progress | INT | DEFAULT 0 | 学习进度(0-100%) |
| completed | TINYINT | DEFAULT 0 | 是否完成: 0-否 1-是 |
| study_duration | INT | DEFAULT 0 | 学习时长(秒) |
| last_study_time | DATETIME | | 最后学习时间 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)
- resource_id → education_resource.id (ON DELETE CASCADE)

**关系**：
- user (1) : (N) user_learning_record
- education_resource (1) : (N) user_learning_record

---

### 6. resource_keywords - 资源关键词表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| resource_id | BIGINT | FK, NOT NULL | 资源ID → education_resource.id |
| keyword | VARCHAR(50) | NOT NULL | 关键词 |
| tfidf_score | DECIMAL(5,2) | | TF-IDF分数 |
| category | VARCHAR(50) | | 关键词类别: problem/solution |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**外键**：
- resource_id → education_resource.id (ON DELETE CASCADE)

**唯一约束**：
- uk_resource_keyword (resource_id, keyword)

**关系**：education_resource (1) : (N) resource_keywords

---

## 三、心理测评模块（3个表）

### 7. assessment_scale - 量表表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 量表ID |
| name | VARCHAR(100) | NOT NULL | 量表名称 |
| code | VARCHAR(50) | UNIQUE, NOT NULL | 量表编码(SDS/SCL90/UPI) |
| description | TEXT | | 量表描述 |
| total_questions | INT | NOT NULL | 总题数 |
| estimated_time | INT | | 预计用时(分钟) |
| scoring_rule | TEXT | | 评分规则(JSON) |
| interpretation_template | TEXT | | 结果解读模板 |
| status | TINYINT | DEFAULT 1 | 状态: 0-禁用 1-启用 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

### 8. scale_question - 量表题目表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | INT | PK, AUTO_INCREMENT | 题目ID |
| scale_id | INT | FK, NOT NULL | 量表ID → assessment_scale.id |
| question_no | INT | NOT NULL | 题目序号 |
| question_text | TEXT | NOT NULL | 题目内容 |
| question_type | TINYINT | DEFAULT 1 | 题型: 1-单选 2-多选 |
| options | JSON | NOT NULL | 选项JSON |
| score_mapping | JSON | | 计分映射JSON |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**外键**：
- scale_id → assessment_scale.id (ON DELETE CASCADE)

**唯一约束**：
- uk_scale_question (scale_id, question_no)

**索引**：
- idx_scale (scale_id)

**关系**：assessment_scale (1) : (N) scale_question

---

### 9. user_assessment - 用户测评记录表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | FK, NOT NULL | 用户ID → user.id |
| scale_id | INT | FK, NOT NULL | 量表ID → assessment_scale.id |
| total_score | DECIMAL(10,2) | | 总分 |
| result_level | VARCHAR(20) | | 结果等级: 正常/轻度/中度/重度 |
| interpretation | TEXT | | 专业解读 |
| suggestions | TEXT | | 干预建议 |
| answers | JSON | NOT NULL | 答题详情JSON |
| completion_time | INT | | 完成用时(秒) |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)
- scale_id → assessment_scale.id

**索引**：
- idx_user_created (user_id, created_at)
- idx_scale (scale_id)

**关系**：
- user (1) : (N) user_assessment
- assessment_scale (1) : (N) user_assessment

---

## 四、情绪日记模块（1个表）

### 10. emotion_diary - 情绪日记表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 日记ID |
| user_id | BIGINT | FK, NOT NULL | 用户ID → user.id |
| mood_score | INT | NOT NULL | 情绪评分(1-10) |
| mood_tag | VARCHAR(50) | | 情绪标签: 开心/焦虑/平静等 |
| content | TEXT | | 日记内容 |
| weather | VARCHAR(20) | | 天气 |
| energy_level | INT | | 精力水平(1-10) |
| sleep_quality | INT | | 睡眠质量(1-10) |
| trigger_event | VARCHAR(255) | | 触发事件 |
| diary_date | DATE | NOT NULL | 日记日期 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)

**索引**：
- idx_user_date (user_id, diary_date)

**关系**：user (1) : (N) emotion_diary

---

## 五、社区互动模块（2个表）

### 11. community_post - 帖子表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 帖子ID |
| user_id | BIGINT | FK, NOT NULL | 用户ID → user.id |
| title | VARCHAR(200) | NOT NULL | 帖子标题 |
| content | TEXT | NOT NULL | 帖子内容 |
| category | VARCHAR(50) | | 分类 |
| tags | VARCHAR(255) | | 标签(逗号分隔) |
| is_anonymous | TINYINT | DEFAULT 0 | 是否匿名: 0-否 1-是 |
| view_count | INT | DEFAULT 0 | 浏览量 |
| comment_count | INT | DEFAULT 0 | 评论数 |
| like_count | INT | DEFAULT 0 | 点赞数 |
| status | TINYINT | DEFAULT 1 | 状态: 0-隐藏 1-正常 2-已删除 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)

**索引**：
- idx_user_created (user_id, created_at)
- idx_status_created (status, created_at)

**关系**：user (1) : (N) community_post

---

### 12. community_comment - 评论表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 评论ID |
| post_id | BIGINT | FK, NOT NULL | 帖子ID → community_post.id |
| user_id | BIGINT | FK, NOT NULL | 用户ID → user.id |
| parent_id | BIGINT | DEFAULT 0 | 父评论ID（支持楼中楼） |
| content | TEXT | NOT NULL | 评论内容 |
| like_count | INT | DEFAULT 0 | 点赞数 |
| status | TINYINT | DEFAULT 1 | 状态: 0-隐藏 1-正常 2-已删除 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**外键**：
- post_id → community_post.id (ON DELETE CASCADE)
- user_id → user.id (ON DELETE CASCADE)

**索引**：
- idx_post_created (post_id, created_at)
- idx_parent (parent_id)

**关系**：
- community_post (1) : (N) community_comment
- user (1) : (N) community_comment
- community_comment (1) : (N) community_comment（自关联，楼中楼）

---

## 六、推荐系统模块（4个表）

### 13. user_resource_rating - 用户资源评分表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | FK, NOT NULL | 用户ID → user.id |
| resource_id | BIGINT | FK, NOT NULL | 资源ID → education_resource.id |
| rating_type | TINYINT | NOT NULL | 行为类型: 1-浏览 2-点赞 3-收藏 4-完成 5-分享 |
| score | DECIMAL(3,2) | DEFAULT 0 | 评分值 |
| weight | DECIMAL(3,2) | DEFAULT 1.0 | 权重(时间衰减) |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)
- resource_id → education_resource.id (ON DELETE CASCADE)

**唯一约束**：
- uk_user_resource_type (user_id, resource_id, rating_type)

**索引**：
- idx_user (user_id)
- idx_resource (resource_id)

**关系**：
- user (1) : (N) user_resource_rating
- education_resource (1) : (N) user_resource_rating

---

### 14. user_interest_tags - 用户兴趣标签表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| user_id | BIGINT | FK, NOT NULL | 用户ID → user.id |
| tag_name | VARCHAR(50) | NOT NULL | 标签名称 |
| tag_category | VARCHAR(50) | | 标签分类: stress/anxiety/study |
| interest_score | DECIMAL(5,2) | DEFAULT 0 | 兴趣度(0-100) |
| source | VARCHAR(50) | | 来源: assessment/diary/behavior |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)

**唯一约束**：
- uk_user_tag (user_id, tag_name)

**索引**：
- idx_user (user_id)

**关系**：user (1) : (N) user_interest_tags

---

### 15. recommendation_result - 推荐结果表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录ID |
| user_id | BIGINT | FK, NOT NULL | 用户ID → user.id |
| resource_id | BIGINT | FK, NOT NULL | 资源ID → education_resource.id |
| algorithm_type | VARCHAR(50) | NOT NULL | 算法类型: collaborative/content_based/hybrid |
| confidence_score | DECIMAL(5,2) | | 置信度(0-100) |
| reason | VARCHAR(200) | | 推荐理由 |
| display_order | INT | | 显示顺序 |
| is_clicked | TINYINT | DEFAULT 0 | 是否点击: 0-否 1-是 |
| is_effective | TINYINT | DEFAULT 0 | 是否有效: 0-否 1-是 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| expired_at | DATETIME | | 过期时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)
- resource_id → education_resource.id (ON DELETE CASCADE)

**索引**：
- idx_user_expired (user_id, expired_at)
- idx_algorithm (algorithm_type)

**关系**：
- user (1) : (N) recommendation_result
- education_resource (1) : (N) recommendation_result

---

### 16. user_psychological_profile - 用户心理画像表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 主键ID |
| user_id | BIGINT | FK, NOT NULL, UNIQUE | 用户ID → user.id |
| stress_level | DECIMAL(5,2) | | 压力水平(0-100) |
| anxiety_level | DECIMAL(5,2) | | 焦虑水平(0-100) |
| depression_level | DECIMAL(5,2) | | 抑郁水平(0-100) |
| dominant_issues | VARCHAR(255) | | 主要问题(逗号分隔) |
| coping_style | VARCHAR(50) | | 应对方式 |
| last_assessment_date | DATETIME | | 最后测评日期 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**外键**：
- user_id → user.id (ON DELETE CASCADE)

**关系**：user (1) : (1) user_psychological_profile

---

## 七、系统管理模块（1个表）

### 17. system_config - 系统配置表

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 配置ID |
| config_key | VARCHAR(100) | UNIQUE, NOT NULL | 配置键 |
| config_value | TEXT | NOT NULL | 配置值 |
| description | VARCHAR(255) | | 描述 |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

## 八、ER图关系总结

### 核心关系

```
user (用户主表)
├── user_privacy (1:1) - 隐私设置
├── user_psychological_profile (1:1) - 心理画像
├── user_learning_record (1:N) - 学习记录
│   └── education_resource
├── user_assessment (1:N) - 测评记录
│   └── assessment_scale
│       └── scale_question
├── emotion_diary (1:N) - 情绪日记
├── community_post (1:N) - 帖子
│   └── community_comment (1:N) - 评论（含自关联）
├── user_resource_rating (1:N) - 资源评分
│   └── education_resource
├── user_interest_tags (1:N) - 兴趣标签
└── recommendation_result (1:N) - 推荐结果
    └── education_resource

resource_category (1:N) education_resource
education_resource (1:N) resource_keywords
education_resource (1:N) user_learning_record
```

---

## 九、ER图绘制建议

### 使用工具推荐

1. **Draw.io**（免费，推荐）
   - 网址：https://app.diagrams.net/
   - 支持导入SQL
   - 导出多种格式

2. **MySQL Workbench**
   - 可逆向工程生成ER图
   - 菜单：Database → Reverse Engineer

3. **Navicat**
   - 模型功能可生成ER图
   - 支持导出图片

4. **PowerDesigner**
   - 专业数据库设计工具
   - 功能强大但收费

---

### ER图绘制步骤（以Draw.io为例）

1. **创建实体**
   - 每个表一个矩形
   - 标注表名和主键

2. **添加属性**
   - 主键：下划线或PK标记
   - 外键：FK标记
   - 普通字段：直接列出

3. **绘制关系**
   - 1:1 关系：单线
   - 1:N 关系：带箭头
   - 标注关系名称

4. **布局优化**
   - user表放中间
   - 相关模块分组
   - 避免交叉线

---

### ER图布局建议

```
                    [resource_category]
                          |
                    [education_resource] ─── [resource_keywords]
                          |
[user] ─── [user_privacy] ─── [user_learning_record]
   |                                   
   ├── [user_psychological_profile]     
   ├── [user_assessment] ─── [assessment_scale] ─── [scale_question]
   ├── [emotion_diary]
   ├── [community_post] ─── [community_comment]
   ├── [user_resource_rating] ─── (关联education_resource)
   ├── [user_interest_tags]
   └── [recommendation_result] ─── (关联education_resource)

[system_config] (独立表)
```

---

## 十、数据量统计（测试环境）

| 表名 | 记录数 | 说明 |
|------|--------|------|
| user | 8 | 测试用户 |
| education_resource | 42 | 学习资源 |
| resource_category | 6 | 资源分类 |
| assessment_scale | 3 | 量表 |
| scale_question | 80 | 题目 |
| user_assessment | 6 | 测评记录 |
| emotion_diary | 0 | 情绪日记 |
| community_post | 0 | 帖子 |
| user_resource_rating | 29 | 用户行为 |
| user_interest_tags | 15 | 兴趣标签 |
| resource_keywords | ~100 | 资源关键词 |
| recommendation_result | 动态 | 推荐结果 |

---

**文档版本**：v1.0  
**最后更新**：2026年4月  
**适用场景**：ER图绘制、数据库设计文档
