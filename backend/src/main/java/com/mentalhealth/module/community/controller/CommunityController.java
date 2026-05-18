package com.mentalhealth.module.community.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mentalhealth.common.exception.BusinessException;
import com.mentalhealth.common.result.Result;
import com.mentalhealth.common.utils.JwtUtil;
import com.mentalhealth.module.community.entity.CommunityCategory;
import com.mentalhealth.module.community.entity.CommunityComment;
import com.mentalhealth.module.community.entity.CommunityPost;
import com.mentalhealth.module.community.service.CommunityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/community")
@Api(tags = "社区互动")
public class CommunityController {
    
    @Autowired
    private CommunityService communityService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/posts")
    @ApiOperation("发布帖子")
    public Result<Void> createPost(
            @RequestHeader("Authorization") String token,
            @RequestBody CommunityPost post) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        communityService.createPost(userId, post);
        return Result.success("发布成功,等待审核", null);
    }
    
    @GetMapping("/posts")
    @ApiOperation("获取帖子列表")
    public Result<Page<CommunityPost>> getPostList(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) Boolean likes) {
        
        Long userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        }
        
        boolean onlyMine = Boolean.TRUE.equals(mine);
        boolean onlyLikes = Boolean.TRUE.equals(likes);
        if ((onlyMine || onlyLikes) && userId == null) {
            throw new BusinessException(401, "请先登录");
        }

        Page<CommunityPost> page = communityService.getPostList(pageNum, pageSize, categoryId, userId, onlyMine, onlyLikes);
        return Result.success(page);
    }
    
    @GetMapping("/posts/{id}")
    @ApiOperation("获取帖子详情")
    public Result<Map<String, Object>> getPostDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        
        Long userId = null;
        if (token != null) {
            String actualToken = token.replace("Bearer ", "");
            userId = jwtUtil.getUserIdFromToken(actualToken);
        }
        
        Map<String, Object> detail = communityService.getPostDetail(id, userId);
        return Result.success(detail);
    }
    
    @PostMapping("/posts/{postId}/comments")
    @ApiOperation("发表评论")
    public Result<Void> createComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @RequestBody CommunityComment comment) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        communityService.createComment(userId, postId, comment);
        return Result.success("评论成功", null);
    }
    
    @GetMapping("/posts/{postId}/comments")
    @ApiOperation("获取评论列表")
    public Result<List<CommunityComment>> getComments(@PathVariable Long postId) {
        List<CommunityComment> comments = communityService.getComments(postId);
        return Result.success(comments);
    }
    
    @PostMapping("/posts/{id}/like")
    @ApiOperation("点赞帖子")
    public Result<Void> likePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        communityService.likePost(userId, id);
        return Result.success("点赞成功", null);
    }
    
    @DeleteMapping("/posts/{id}/like")
    @ApiOperation("取消点赞")
    public Result<Void> unlikePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        communityService.unlikePost(userId, id);
        return Result.success("取消点赞", null);
    }
    
    @GetMapping("/categories")
    @ApiOperation("获取社区分类列表")
    public Result<List<CommunityCategory>> getCategories() {
        List<CommunityCategory> categories = communityService.getCategories();
        return Result.success(categories);
    }
    
    @PutMapping("/posts/{id}")
    @ApiOperation("编辑帖子")
    public Result<Void> updatePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody CommunityPost post) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        communityService.updatePost(userId, id, post);
        return Result.success("编辑成功", null);
    }
    
    @DeleteMapping("/posts/{id}")
    @ApiOperation("删除帖子")
    public Result<Void> deletePost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        Integer role = jwtUtil.getRoleFromToken(actualToken);
        String username = jwtUtil.getUsernameFromToken(actualToken);
        
        boolean isAdmin = (role != null && role == 3) || "admin".equals(username);
        
        communityService.deletePost(userId, id, isAdmin);
        return Result.success("删除成功", null);
    }
    
    @DeleteMapping("/comments/{id}")
    @ApiOperation("删除评论")
    public Result<Void> deleteComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        Integer role = jwtUtil.getRoleFromToken(actualToken);
        String username = jwtUtil.getUsernameFromToken(actualToken);
        
        boolean isAdmin = (role != null && role == 3) || "admin".equals(username);
        
        communityService.deleteComment(userId, id, isAdmin);
        return Result.success("删除成功", null);
    }
}
