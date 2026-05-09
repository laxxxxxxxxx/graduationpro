package com.mentalhealth.module.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mentalhealth.common.exception.BusinessException;
import com.mentalhealth.module.resource.entity.*;
import com.mentalhealth.module.resource.mapper.*;
import com.mentalhealth.module.resource.service.ResourceService;
import com.mentalhealth.module.auth.entity.User;
import com.mentalhealth.module.auth.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ResourceServiceImpl implements ResourceService {
    
    @Autowired
    private ResourceMapper resourceMapper;
    
    @Autowired
    private ResourceCategoryMapper categoryMapper;
    
    @Autowired
    private LearningRecordMapper learningRecordMapper;

    @Autowired
    private ResourceLikeMapper likeMapper;

    @Autowired
    private ResourceFavoriteMapper favoriteMapper;

    @Autowired
    private ResourceCommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;
    
    @Override
    public Page<EducationResource> getResourceList(Integer pageNum, Integer pageSize, Integer type, Integer categoryId) {
        Page<EducationResource> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EducationResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EducationResource::getStatus, 1);
        
        if (type != null) {
            wrapper.eq(EducationResource::getType, type);
        }
        if (categoryId != null) {
            wrapper.eq(EducationResource::getCategoryId, categoryId);
        }
        
        wrapper.orderByDesc(EducationResource::getCreatedAt);
        return resourceMapper.selectPage(page, wrapper);
    }
    
    @Override
    public Map<String, Object> getResourceDetail(Long id, Long userId) {
        EducationResource resource = resourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }
        
        // 增加浏览量
        resource.setViewCount(resource.getViewCount() + 1);
        resourceMapper.updateById(resource);

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("resource", resource);

        if (userId != null) {
            // 查询用户是否点赞
            LambdaQueryWrapper<ResourceLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(ResourceLike::getUserId, userId);
            likeWrapper.eq(ResourceLike::getResourceId, id);
            result.put("isLiked", likeMapper.selectCount(likeWrapper) > 0);

            // 查询用户是否收藏
            LambdaQueryWrapper<ResourceFavorite> favWrapper = new LambdaQueryWrapper<>();
            favWrapper.eq(ResourceFavorite::getUserId, userId);
            favWrapper.eq(ResourceFavorite::getResourceId, id);
            result.put("isFavorited", favoriteMapper.selectCount(favWrapper) > 0);

            // 查询用户学习记录
            LambdaQueryWrapper<UserLearningRecord> recordWrapper = new LambdaQueryWrapper<>();
            recordWrapper.eq(UserLearningRecord::getUserId, userId);
            recordWrapper.eq(UserLearningRecord::getResourceId, id);
            UserLearningRecord record = learningRecordMapper.selectOne(recordWrapper);
            result.put("studyRecord", record);
        } else {
            result.put("isLiked", false);
            result.put("isFavorited", false);
            result.put("studyRecord", null);
        }
        
        return result;
    }
    
    @Override
    public List<EducationResource> searchResource(String keyword) {
        LambdaQueryWrapper<EducationResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(EducationResource::getTitle, keyword)
                           .or()
                           .like(EducationResource::getContent, keyword));
        wrapper.eq(EducationResource::getStatus, 1);
        wrapper.last("LIMIT 20");
        return resourceMapper.selectList(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordStudy(Long userId, Long resourceId, Integer progress, Integer duration) {
        LambdaQueryWrapper<UserLearningRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserLearningRecord::getUserId, userId);
        wrapper.eq(UserLearningRecord::getResourceId, resourceId);
        
        UserLearningRecord record = learningRecordMapper.selectOne(wrapper);
        
        if (record == null) {
            record = new UserLearningRecord();
            record.setUserId(userId);
            record.setResourceId(resourceId);
            record.setProgress(progress);
            record.setStudyDuration(duration);
            record.setLastStudyTime(new Date());
            record.setCompleted(progress >= 100 ? 1 : 0);
            learningRecordMapper.insert(record);
        } else {
            record.setProgress(Math.max(record.getProgress(), progress));
            record.setStudyDuration(record.getStudyDuration() + duration);
            record.setLastStudyTime(new Date());
            record.setCompleted(record.getProgress() >= 100 ? 1 : 0);
            learningRecordMapper.updateById(record);
        }
        
        log.info("用户{}学习资源{},进度{}%", userId, resourceId, progress);
    }
    
    @Override
    public List<ResourceCategory> getCategories() {
        LambdaQueryWrapper<ResourceCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceCategory::getStatus, 1);
        wrapper.orderByAsc(ResourceCategory::getSortOrder);
        return categoryMapper.selectList(wrapper);
    }
    
    @Override
    public List<EducationResource> getRecommendations(Integer limit) {
        LambdaQueryWrapper<EducationResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EducationResource::getStatus, 1);
        wrapper.orderByDesc(EducationResource::getViewCount);
        wrapper.last("LIMIT " + limit);
        return resourceMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleLike(Long userId, Long resourceId) {
        LambdaQueryWrapper<ResourceLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceLike::getUserId, userId);
        wrapper.eq(ResourceLike::getResourceId, resourceId);

        ResourceLike existing = likeMapper.selectOne(wrapper);
        EducationResource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }

        Map<String, Object> result = new HashMap<>();

        if (existing != null) {
            // 取消点赞
            likeMapper.deleteById(existing.getId());
            resource.setLikeCount(Math.max(0, resource.getLikeCount() - 1));
            resourceMapper.updateById(resource);
            result.put("liked", false);
            result.put("likeCount", resource.getLikeCount());
        } else {
            // 点赞
            ResourceLike like = new ResourceLike();
            like.setUserId(userId);
            like.setResourceId(resourceId);
            likeMapper.insert(like);
            resource.setLikeCount(resource.getLikeCount() + 1);
            resourceMapper.updateById(resource);
            result.put("liked", true);
            result.put("likeCount", resource.getLikeCount());
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggleFavorite(Long userId, Long resourceId) {
        LambdaQueryWrapper<ResourceFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceFavorite::getUserId, userId);
        wrapper.eq(ResourceFavorite::getResourceId, resourceId);

        ResourceFavorite existing = favoriteMapper.selectOne(wrapper);
        EducationResource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new BusinessException("资源不存在");
        }

        Map<String, Object> result = new HashMap<>();

        if (existing != null) {
            // 取消收藏
            favoriteMapper.deleteById(existing.getId());
            resource.setFavoriteCount(Math.max(0, resource.getFavoriteCount() - 1));
            resourceMapper.updateById(resource);
            result.put("favorited", false);
            result.put("favoriteCount", resource.getFavoriteCount());
        } else {
            // 收藏
            ResourceFavorite fav = new ResourceFavorite();
            fav.setUserId(userId);
            fav.setResourceId(resourceId);
            favoriteMapper.insert(fav);
            resource.setFavoriteCount(resource.getFavoriteCount() + 1);
            resourceMapper.updateById(resource);
            result.put("favorited", true);
            result.put("favoriteCount", resource.getFavoriteCount());
        }

        return result;
    }

    @Override
    public List<ResourceComment> getComments(Long resourceId, Long userId) {
        LambdaQueryWrapper<ResourceComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceComment::getResourceId, resourceId);
        wrapper.eq(ResourceComment::getStatus, 1);
        wrapper.orderByDesc(ResourceComment::getCreatedAt);
        List<ResourceComment> allComments = commentMapper.selectList(wrapper);

        // 填充用户信息
        for (ResourceComment comment : allComments) {
            User user = userMapper.selectById(comment.getUserId());
            if (user != null) {
                comment.setUsername(user.getUsername());
                comment.setAvatar(user.getAvatar());
            }
        }

        // 分离顶级评论和回复
        List<ResourceComment> topComments = allComments.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());

        for (ResourceComment top : topComments) {
            List<ResourceComment> replies = allComments.stream()
                    .filter(c -> c.getParentId() != null && c.getParentId().equals(top.getId()))
                    .collect(Collectors.toList());
            top.setReplies(replies);
        }

        return topComments;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceComment addComment(Long userId, Long resourceId, Long parentId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }

        ResourceComment comment = new ResourceComment();
        comment.setResourceId(resourceId);
        comment.setUserId(userId);
        comment.setParentId(parentId != null ? parentId : 0L);
        comment.setContent(content.trim());
        comment.setLikeCount(0);
        comment.setStatus(1);
        commentMapper.insert(comment);

        // 更新资源评论数
        EducationResource resource = resourceMapper.selectById(resourceId);
        if (resource != null) {
            resource.setCommentCount(resource.getCommentCount() + 1);
            resourceMapper.updateById(resource);
        }

        // 填充用户信息
        User user = userMapper.selectById(userId);
        if (user != null) {
            comment.setUsername(user.getUsername());
            comment.setAvatar(user.getAvatar());
        }

        return comment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        ResourceComment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己的评论");
        }

        // 软删除
        comment.setStatus(2);
        commentMapper.updateById(comment);

        // 更新资源评论数
        EducationResource resource = resourceMapper.selectById(comment.getResourceId());
        if (resource != null) {
            resource.setCommentCount(Math.max(0, resource.getCommentCount() - 1));
            resourceMapper.updateById(resource);
        }
    }

    @Override
    public List<EducationResource> getUserFavorites(Long userId) {
        LambdaQueryWrapper<ResourceFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceFavorite::getUserId, userId);
        wrapper.orderByDesc(ResourceFavorite::getCreatedAt);
        List<ResourceFavorite> favorites = favoriteMapper.selectList(wrapper);

        if (favorites.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> resourceIds = favorites.stream()
                .map(ResourceFavorite::getResourceId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<EducationResource> resWrapper = new LambdaQueryWrapper<>();
        resWrapper.in(EducationResource::getId, resourceIds);
        resWrapper.eq(EducationResource::getStatus, 1);
        return resourceMapper.selectList(resWrapper);
    }
}
