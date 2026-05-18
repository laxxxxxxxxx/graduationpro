package com.mentalhealth.module.community.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mentalhealth.module.community.entity.CommunityCategory;
import com.mentalhealth.module.community.entity.CommunityPost;
import com.mentalhealth.module.community.entity.CommunityComment;

import java.util.List;
import java.util.Map;

public interface CommunityService {
    
    void createPost(Long userId, CommunityPost post);
    
    Page<CommunityPost> getPostList(
            Integer pageNum,
            Integer pageSize,
            Integer categoryId,
            Long userId,
            Boolean onlyMine,
            Boolean onlyLikes);
    
    Map<String, Object> getPostDetail(Long postId, Long userId);
    
    void updatePost(Long userId, Long postId, CommunityPost post);
    
    void deletePost(Long userId, Long postId, boolean isAdmin);
    
    void createComment(Long userId, Long postId, CommunityComment comment);
    
    List<CommunityComment> getComments(Long postId);
    
    void deleteComment(Long userId, Long commentId, boolean isAdmin);
    
    void likePost(Long userId, Long postId);
    
    void unlikePost(Long userId, Long postId);
    
    List<CommunityCategory> getCategories();
}
