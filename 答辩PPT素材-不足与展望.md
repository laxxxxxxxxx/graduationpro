# 答辩PPT素材 - 项目不足与展望

---

## 一、当前项目不足

### 1. 功能完整性不足

#### 量表题目不完整
- **问题**：SCL-90仅30题（完整版90题），UPI仅30题（完整版60题）
- **影响**：测评结果准确性受限
- **原因**：时间有限，优先保证核心功能

#### 缺少后台管理系统
- **问题**：无管理员界面
- **影响**：无法进行内容审核、用户管理
- **原因**：重点放在用户端功能开发

#### 社交功能不完善
- **问题**：缺少资源收藏、评论、学习笔记
- **影响**：用户互动体验不足
- **原因**：开发优先级较低

---

### 2. 技术架构待优化

#### 性能问题
- **问题**：推荐算法实时计算，响应时间较长（2-3秒）
- **影响**：用户体验不够流畅
- **原因**：未引入缓存层

#### 缺少缓存机制
- **问题**：频繁查询数据库
- **影响**：数据库压力大
- **原因**：项目初期追求快速迭代

#### 测试覆盖不足
- **问题**：单元测试覆盖率<10%
- **影响**：代码质量难以保证
- **原因**：开发时间紧张

---

### 3. 数据安全与隐私

#### 敏感数据加密不完善
- **问题**：测评结果等敏感数据未加密存储
- **影响**：存在数据泄露风险
- **原因**：优先考虑功能实现

#### 缺少接口限流
- **问题**：无防刷机制
- **影响**：可能被恶意攻击
- **原因**：测试环境未暴露此问题

---

### 4. 用户体验待提升

#### 交互细节不足
- **问题**：缺少下拉刷新、骨架屏、加载动画
- **影响**：用户体验不够友好
- **原因**：UI/UX设计时间不足

#### 错误提示不够明确
- **问题**：部分错误信息过于简单
- **影响**：用户难以理解问题
- **原因**：异常处理优先级较低

---

## 二、遇到的问题与解决方案

### 问题1：数据库外键约束失败 ⭐

**问题描述**：
```
Cannot add or update a child row: a foreign key constraint fails
```

**原因分析**：
- SQL脚本中硬编码了用户ID（3,4,5...）
- 实际插入时ID是AUTO_INCREMENT，值不匹配
- 外键约束导致插入失败

**解决方案**：
```sql
-- 修改前（错误）
INSERT INTO user_assessment (user_id, scale_id, ...) 
VALUES (3, 1, ...);

-- 修改后（正确）
INSERT INTO user_assessment (user_id, scale_id, ...) 
VALUES (
  (SELECT id FROM user WHERE username='student001'), 
  1, 
  ...
);
```

**经验总结**：
- 使用子查询动态获取关联ID
- 避免硬编码ID值
- 重置AUTO_INCREMENT确保ID连续

---

### 问题：推荐资源不在学习库中 

**问题描述**：
个性化推荐的资源在学习资源列表中找不到，用户点击推荐后显示"资源不存在"

**原因分析**：

协同过滤算法基于行为数据计算，可能返回已删除的资源ID，未验证资源是否存在和已发布

**解决方案**：

**SQL过滤**

```sql
-- 添加JOIN和状态检查
SELECT er.id, er.title
FROM education_resource er
JOIN user_resource_rating urr ON er.id = urr.resource_id
WHERE er.status = 1  -- 只查询已发布资源
```

**代码验证**

```java
// 遍历推荐结果，移除无效资源
Iterator<Map<String, Object>> iterator = recommendations.iterator();
while (iterator.hasNext()) {
    Map<String, Object> rec = iterator.next();
    Long resourceId = rec.get("resourceId");
    EducationResource resource = resourceMapper.selectById(resourceId);
    
    if (resource == null || resource.getStatus() != 1) {
        iterator.remove();  // 移除无效资源
    }
}
```

**方案3：热门资源补充**
```java
// 如果推荐数量不足，用热门资源补充
if (recommendations.size() < topN) {
    List<Map<String, Object>> hotResources = getHotResources(userId, topN - recommendations.size());
    recommendations.addAll(hotResources);
}
```

**经验总结**：
- 推荐系统必须验证资源有效性
- 多重保证：SQL + 代码 + 过滤
- 准备兜底方案（热门资源）

---

### 问题3：心理测评提交失败

**问题描述**：
```json
{
  "code": 500,
  "message": "系统异常,请联系管理员"
}
```

**原因分析**：
1. 前端传递数据类型与后端期望不一致
2. `request.get()` 返回Object，直接强转失败
3. Loading未正确配对

**解决方案**：

**后端修复**：
```java
// 安全的类型转换
Integer scaleId = request.get("scaleId") instanceof Integer 
    ? (Integer) request.get("scaleId") 
    : Integer.parseInt(request.get("scaleId").toString());

// 添加异常处理
try {
    // 业务逻辑
    return Result.success(result);
} catch (Exception e) {
    log.error("提交测评失败", e);
    return Result.error("提交失败: " + e.getMessage());
}
```

**前端修复**：
```javascript
async doSubmit() {
  try {
    uni.showLoading({ title: '提交中...' })
    // 提交逻辑
    uni.hideLoading()  // ✅ 成功时隐藏
  } catch (err) {
    uni.hideLoading()  // ✅ 失败时也隐藏
    uni.showModal({
      title: '提交失败',
      content: err.message,
      showCancel: false
    })
  }
}
```

**经验总结**：
- 前后端数据类型要对齐
- 异常必须捕获并返回友好提示
- Loading必须配对使用

---

### 问题4：微信小程序编译错误

**问题描述**：
```
SassError: unmatched "}"
:key 不支持表达式 option.value||index
```

**原因分析**：
1. 删除样式时遗留多余的 `}`
2. 微信小程序不支持 `:key` 中使用复杂表达式

**解决方案**：

**修复1：SCSS语法错误**
```scss
// 修改前（错误）
}

}  // ← 多余的大括号
</style>

// 修改后（正确）
}
</style>
```

**修复2：:key 表达式**
```vue
<!-- 修改前（错误） -->
<view v-for="(option, index) in options" :key="option.value || index">

<!-- 修改后（正确） -->
<view v-for="(option, index) in options" :key="index">
```

**经验总结**：
- 修改代码后仔细检查语法
- 微信小程序有特殊限制
- 使用简单表达式

---

### 问题5：API函数名不匹配

**问题描述**：
```
TypeError: (0 , _assessment.getQuestions) is not a function
```

**原因分析**：
- API导出名：`getScaleQuestions`
- 前端导入名：`getQuestions`
- 函数名不一致

**解决方案**：
```javascript
// 修改前（错误）
import { getQuestions } from '@/api/assessment.js'
const res = await getQuestions(this.scaleId)

// 修改后（正确）
import { getScaleQuestions } from '@/api/assessment.js'
const res = await getScaleQuestions(this.scaleId)
```

**经验总结**：
- 前后端API命名要统一
- 使用IDE的自动补全避免拼写错误
- 建立API文档规范

---

## 三、下一步开发计划

### 短期计划（1-2个月）

#### 1. 完善核心功能
- [ ] 补充SCL-90剩余60题
- [ ] 补充UPI剩余30题
- [ ] 实现SCL-90各因子分计算
- [ ] 添加资源收藏/点赞功能
- [ ] 实现资源评论功能

#### 2. 优化用户体验
- [ ] 添加下拉刷新
- [ ] 实现骨架屏加载
- [ ] 优化错误提示
- [ ] 完善动画效果

#### 3. 技术优化
- [ ] 引入Redis缓存
- [ ] 优化数据库查询
- [ ] 添加接口限流
- [ ] 编写单元测试

---

### 中期计划（3-6个月）

#### 1. 增强功能
- [ ] 实现微信一键登录
- [ ] 添加学习笔记功能
- [ ] 实现学习打卡系统
- [ ] 添加情绪日历视图
- [ ] 实现测评报告导出

#### 2. 数据分析
- [ ] 用户行为分析
- [ ] 推荐效果评估
- [ ] 群体心理健康统计
- [ ] 数据可视化报表

#### 3. 安全加固
- [ ] 敏感数据加密
- [ ] XSS防护
- [ ] SQL注入防护
- [ ] 接口签名验证

---

### 长期计划（6-12个月）

#### 1. 后台管理系统
- [ ] 管理员登录
- [ ] 用户管理
- [ ] 内容审核
- [ ] 数据统计
- [ ] 系统配置

#### 2. AI增强
- [ ] 深度学习推荐模型（Neural CF）
- [ ] AI情绪分析
- [ ] 智能问答机器人
- [ ] 个性化干预方案

#### 3. 平台化
- [ ] 专业咨询师入驻
- [ ] 在线心理咨询
- [ ] 危机干预机制
- [ ] 多平台支持（APP/Web）

---

## 四、项目反思与收获

### 技术收获
1. **全栈开发能力**
   - 掌握了Spring Boot后端开发
   - 熟悉了UniApp前端框架
   - 学会了MySQL数据库设计

2. **算法实现能力**
   - 实现了三种推荐算法
   - 理解了协同过滤原理
   - 掌握了TF-IDF关键词提取

3. **工程实践能力**
   - 前后端分离架构
   - RESTful API设计
   - JWT认证机制

---

### 经验教训

#### 1. 需求分析要全面
- **教训**：初期未考虑后台管理
- **改进**：先做完整的需求分析

#### 2. 数据设计要规范
- **教训**：外键约束导致初始化失败
- **改进**：使用子查询避免硬编码

#### 3. 异常处理要完善
- **教训**：错误提示不友好
- **改进**：统一异常处理机制

#### 4. 测试要及时
- **教训**：后期发现大量Bug
- **改进**：边开发边测试

---

### 创新点总结

1. **混合推荐算法** ⭐⭐⭐
   - User-CF + Item-CF + Content-Based
   - 加权融合策略
   - 推荐理由可解释

2. **完整服务链** ⭐⭐
   - 教育 → 测评 → 干预 → 追踪
   - 闭环心理健康服务

3. **隐私保护** ⭐⭐
   - 匿名社区
   - 数据加密
   - 隐私设置

---

## 五、答辩演讲要点

### 讲述不足时的技巧

✅ **正确表述**：
- "由于时间限制，部分功能尚未完善"
- "当前版本侧重核心功能实现"
- "后续计划中已包含这些优化"

❌ **避免表述**：
- "这个功能我没做"
- "这里有个Bug"
- "时间不够了"

---

### 讲述问题解决的技巧

✅ **强调**：
- 问题发现过程
- 分析思路
- 解决方案
- 经验总结

❌ **避免**：
- 过度技术细节
- 抱怨困难
- 推卸责任

---

### 讲述未来计划的技巧

✅ **展示**：
- 清晰的规划
- 可行性分析
- 优先级排序
- 时间节点

❌ **避免**：
- 空泛的目标
- 不切实际的想法
- 没有重点

---

**文档版本**：v1.0  
**适用场景**：毕业设计答辩PPT制作  
**建议页数**：5-8页PPT
