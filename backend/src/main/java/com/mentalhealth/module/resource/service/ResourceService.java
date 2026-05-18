package com.mentalhealth.module.resource.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mentalhealth.module.resource.entity.*;

import java.util.List;
import java.util.Map;

public interface ResourceService {
    
    Page<EducationResource> getResourceList(Integer pageNum, Integer pageSize, Integer type, Integer categoryId);
    
    Map<String, Object> getResourceDetail(Long id, Long userId);
    
    List<EducationResource> searchResource(String keyword);
    
    void recordStudy(Long userId, Long resourceId, Integer progress, Integer duration);
    
    List<ResourceCategory> getCategories();
    
    List<EducationResource> getRecommendations(Integer limit);

    // 点赞/取消点赞
    Map<String, Object> toggleLike(Long userId, Long resourceId);

    // 收藏/取消收藏
    Map<String, Object> toggleFavorite(Long userId, Long resourceId);

    // 获取评论列表
    List<ResourceComment> getComments(Long resourceId, Long userId);

    // 发表评论
    ResourceComment addComment(Long userId, Long resourceId, Long parentId, String content);

    // 删除评论
    void deleteComment(Long commentId, Long userId, boolean isAdmin);

    // 获取用户收藏列表
    List<EducationResource> getUserFavorites(Long userId);

    // 获取用户点赞列表
    List<EducationResource> getUserLikes(Long userId);

    void createResource(EducationResource resource);

    void updateResource(EducationResource resource);

    void deleteResource(Long id);
}
