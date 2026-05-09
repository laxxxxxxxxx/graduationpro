package com.mentalhealth.module.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mentalhealth.common.exception.BusinessException;
import com.mentalhealth.module.community.entity.CommunityCategory;
import com.mentalhealth.module.community.entity.CommunityComment;
import com.mentalhealth.module.community.entity.CommunityPost;
import com.mentalhealth.module.community.entity.PostLike;
import com.mentalhealth.module.community.mapper.CommunityCategoryMapper;
import com.mentalhealth.module.community.mapper.CommunityCommentMapper;
import com.mentalhealth.module.community.mapper.CommunityPostMapper;
import com.mentalhealth.module.community.mapper.PostLikeMapper;
import com.mentalhealth.module.community.service.CommunityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommunityServiceImpl implements CommunityService {
    
    @Autowired
    private CommunityPostMapper postMapper;
    
    @Autowired
    private CommunityCommentMapper commentMapper;
    
    @Autowired
    private CommunityCategoryMapper categoryMapper;

    @Autowired
    private PostLikeMapper postLikeMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createPost(Long userId, CommunityPost post) {
        post.setUserId(userId);
        post.setStatus(1); // 直接通过审核(测试环境)
        post.setAuditResult(1);
        post.setViewCount(0);
        post.setCommentCount(0);
        post.setLikeCount(0);
        post.setIsTop(0);
        post.setIsEssence(0);
        
        // 生成匿名ID
        post.setAnonymousId("用户" + String.format("%04d", (userId % 10000)));
        
        // 关键词过滤(简化版)
        if (containsSensitiveWords(post.getTitle()) || containsSensitiveWords(post.getContent())) {
            throw new BusinessException("内容包含敏感词汇");
        }
        
        postMapper.insert(post);
        log.info("用户{}发布帖子成功", userId);
    }
    
    @Override
    public Page<CommunityPost> getPostList(Integer pageNum, Integer pageSize, Integer categoryId, Long userId, Boolean onlyLikes) {
        Page<CommunityPost> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getStatus, 1); // 只查询已审核通过的
        
        if (onlyLikes != null && onlyLikes && userId != null) {
            // 查询点赞过的帖子
            List<PostLike> likes = postLikeMapper.selectList(new LambdaQueryWrapper<PostLike>()
                    .eq(PostLike::getUserId, userId));
            List<Long> postIds = likes.stream().map(PostLike::getPostId).collect(Collectors.toList());
            if (postIds.isEmpty()) {
                return page; // 返回空页
            }
            wrapper.in(CommunityPost::getId, postIds);
        } else if (userId != null && categoryId == null) {
            // 如果是查询"我的发布" (通常 mine 参数传进来时 categoryId 为空)
            // 注意：这里逻辑可以根据具体前端传参调整，目前假设传了 userId 且不是 onlyLikes 就是查自己的
            // 为了安全，我这里增加一个逻辑：如果 mine 参数映射到 userId
            // 实际上在 Controller 层我已经把 mine 转换逻辑做了一半，这里补全
        }
        
        // 如果是查询特定用户的发布
        // (修正：增加一个逻辑分支明确处理 userId 过滤)
        // 实际上我们可以根据 Controller 传过来的逻辑，如果 userId 有值且不是 onlyLikes，
        // 且前端明确想要查"我的"，我们需要一个标识。
        // 这里简化处理：如果 categoryId 为 -1 表示查我的（或者增加参数）
        // 考虑到接口一致性，我直接增加 userId 过滤条件
        if (userId != null && !onlyLikes && categoryId == null) {
             wrapper.eq(CommunityPost::getUserId, userId);
        }

        if (categoryId != null && categoryId > 0) {
            wrapper.eq(CommunityPost::getCategoryId, categoryId);
        }
        
        wrapper.orderByDesc(CommunityPost::getIsTop)
               .orderByDesc(CommunityPost::getCreatedAt);
        
        Page<CommunityPost> resultPage = postMapper.selectPage(page, wrapper);
        
        // 填充分类名称
        if (resultPage.getRecords() != null && !resultPage.getRecords().isEmpty()) {
            List<CommunityCategory> categories = categoryMapper.selectList(null);
            Map<Integer, String> categoryMap = categories.stream()
                    .collect(Collectors.toMap(CommunityCategory::getId, CommunityCategory::getName));
            
            resultPage.getRecords().forEach(post -> {
                if (post.getCategoryId() != null) {
                    post.setCategoryName(categoryMap.get(post.getCategoryId()));
                }
            });
        }
        
        return resultPage;
    }
    
    @Override
    public Map<String, Object> getPostDetail(Long postId, Long userId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        
        // 增加浏览量
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);

        // 填充分类名称
        if (post.getCategoryId() != null) {
            CommunityCategory category = categoryMapper.selectById(post.getCategoryId());
            if (category != null) {
                post.setCategoryName(category.getName());
            }
        }
        
        // 检查是否点赞
        boolean liked = false;
        if (userId != null) {
            Long count = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                    .eq(PostLike::getUserId, userId)
                    .eq(PostLike::getPostId, postId));
            liked = count > 0;
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("post", post);
        result.put("liked", liked);
        result.put("currentUserId", userId);
        
        return result;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createComment(Long userId, Long postId, CommunityComment comment) {
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setAnonymousId("用户" + String.format("%04d", (userId % 10000)));
        comment.setLikeCount(0);
        comment.setStatus(1);
        
        // 关键词过滤
        if (containsSensitiveWords(comment.getContent())) {
            throw new BusinessException("评论包含敏感词汇");
        }
        
        commentMapper.insert(comment);
        
        // 更新帖子评论数
        CommunityPost post = postMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(post);
        }
        
        log.info("用户{}发表评论", userId);
    }
    
    @Override
    public List<CommunityComment> getComments(Long postId) {
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getPostId, postId);
        wrapper.eq(CommunityComment::getStatus, 1);
        wrapper.orderByAsc(CommunityComment::getCreatedAt);
        return commentMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long userId, Long postId) {
        // 检查是否已点赞
        Long count = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getUserId, userId)
                .eq(PostLike::getPostId, postId));
        if (count > 0) {
            return;
        }

        // 插入点赞记录
        PostLike postLike = new PostLike();
        postLike.setUserId(userId);
        postLike.setPostId(postId);
        postLikeMapper.insert(postLike);

        // 更新点赞数
        CommunityPost post = postMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long userId, Long postId) {
        // 删除点赞记录
        int deleted = postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getUserId, userId)
                .eq(PostLike::getPostId, postId));
        
        if (deleted > 0) {
            // 更新点赞数
            CommunityPost post = postMapper.selectById(postId);
            if (post != null && post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
                postMapper.updateById(post);
            }
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePost(Long userId, Long postId, CommunityPost post) {
        CommunityPost existing = postMapper.selectById(postId);
        if (existing == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("只能修改自己的帖子");
        }
        if (existing.getStatus() == 2 || existing.getStatus() == 3) {
            throw new BusinessException("该帖子无法编辑");
        }
        
        // 关键词过滤
        if (containsSensitiveWords(post.getTitle()) || containsSensitiveWords(post.getContent())) {
            throw new BusinessException("内容包含敏感词汇");
        }
        
        existing.setTitle(post.getTitle());
        existing.setContent(post.getContent());
        if (post.getCategoryId() != null) {
            existing.setCategoryId(post.getCategoryId());
        }
        existing.setStatus(1); // 编辑后重新进入已发布状态
        
        postMapper.updateById(existing);
        log.info("用户{}编辑帖子{}成功", userId, postId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long userId, Long postId) {
        CommunityPost existing = postMapper.selectById(postId);
        if (existing == null) {
            throw new BusinessException("帖子不存在");
        }
        if (!existing.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的帖子");
        }
        
        // 软删除：设置status=3
        existing.setStatus(3);
        postMapper.updateById(existing);
        log.info("用户{}删除帖子{}成功", userId, postId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long commentId) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评论");
        }
        
        // 软删除：设置status=2
        comment.setStatus(2);
        commentMapper.updateById(comment);
        
        // 更新帖子评论数
        CommunityPost post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postMapper.updateById(post);
        }
        
        log.info("用户{}删除评论{}成功", userId, commentId);
    }
    
    @Override
    public List<CommunityCategory> getCategories() {
        LambdaQueryWrapper<CommunityCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityCategory::getStatus, 1)
               .orderByAsc(CommunityCategory::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }
    
    /**
     * 敏感词检测(简化版)
     */
    private boolean containsSensitiveWords(String text) {
        if (text == null) return false;
        String[] sensitiveWords = {"敏感词1", "敏感词2"}; // 实际应从配置或数据库读取
        for (String word : sensitiveWords) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }
}
