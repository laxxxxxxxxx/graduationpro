package com.mentalhealth.module.resource.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mentalhealth.common.result.Result;
import com.mentalhealth.common.utils.JwtUtil;
import com.mentalhealth.module.resource.entity.*;
import com.mentalhealth.module.resource.service.ResourceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/resource")
@Api(tags = "教育资源")
public class ResourceController {
    
    @Autowired
    private ResourceService resourceService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @GetMapping("/list")
    @ApiOperation("获取资源列表")
    public Result<Page<EducationResource>> getList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer categoryId) {
        log.info("获取资源列表: pageNum={}, pageSize={}, type={}, categoryId={}", pageNum, pageSize, type, categoryId);
        Page<EducationResource> page = resourceService.getResourceList(pageNum, pageSize, type, categoryId);
        log.info("资源列表加载完成: total={}", page.getTotal());
        return Result.success(page);
    }
    
    @GetMapping("/{id}")
    @ApiOperation("获取资源详情")
    public Result<Map<String, Object>> getDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        Long userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        }
        Map<String, Object> detail = resourceService.getResourceDetail(id, userId);
        return Result.success(detail);
    }
    
    @GetMapping("/search")
    @ApiOperation("搜索资源")
    public Result<List<EducationResource>> search(@RequestParam String keyword) {
        List<EducationResource> list = resourceService.searchResource(keyword);
        return Result.success(list);
    }
    
    @PostMapping("/{id}/study")
    @ApiOperation("记录学习进度")
    public Result<Void> recordStudy(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {
        
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        
        Integer progress = request.getOrDefault("progress", 0);
        Integer duration = request.getOrDefault("duration", 0);
        
        resourceService.recordStudy(userId, id, progress, duration);
        return Result.success("记录成功", null);
    }
    
    @GetMapping("/categories")
    @ApiOperation("获取分类列表")
    public Result<List<ResourceCategory>> getCategories() {
        List<ResourceCategory> categories = resourceService.getCategories();
        return Result.success(categories);
    }
    
    @GetMapping("/recommendations")
    @ApiOperation("获取推荐资源")
    public Result<List<EducationResource>> getRecommendations(
            @RequestParam(defaultValue = "5") Integer limit) {
        List<EducationResource> list = resourceService.getRecommendations(limit);
        return Result.success(list);
    }

    @PostMapping("/{id}/like")
    @ApiOperation("点赞/取消点赞")
    public Result<Map<String, Object>> toggleLike(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Map<String, Object> result = resourceService.toggleLike(userId, id);
        return Result.success(result);
    }

    @PostMapping("/{id}/favorite")
    @ApiOperation("收藏/取消收藏")
    public Result<Map<String, Object>> toggleFavorite(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        Map<String, Object> result = resourceService.toggleFavorite(userId, id);
        return Result.success(result);
    }

    @GetMapping("/{id}/comments")
    @ApiOperation("获取资源评论列表")
    public Result<List<ResourceComment>> getComments(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long id) {
        Long userId = null;
        if (token != null && token.startsWith("Bearer ")) {
            userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        }
        List<ResourceComment> comments = resourceService.getComments(id, userId);
        return Result.success(comments);
    }

    @PostMapping("/{id}/comment")
    @ApiOperation("发表评论/回复")
    public Result<ResourceComment> addComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        String content = (String) request.get("content");
        Long parentId = request.get("parentId") != null ? Long.valueOf(request.get("parentId").toString()) : null;
        ResourceComment comment = resourceService.addComment(userId, id, parentId, content);
        return Result.success("评论成功", comment);
    }

    @DeleteMapping("/comment/{commentId}")
    @ApiOperation("删除评论")
    public Result<Void> deleteComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long commentId) {
        String actualToken = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(actualToken);
        Integer role = jwtUtil.getRoleFromToken(actualToken);
        String username = jwtUtil.getUsernameFromToken(actualToken);
        
        boolean isAdmin = (role != null && role == 3) || "admin".equals(username);
        
        resourceService.deleteComment(commentId, userId, isAdmin);
        return Result.success("删除成功", null);
    }

    @GetMapping("/favorites")
    @ApiOperation("获取用户收藏列表")
    public Result<List<EducationResource>> getUserFavorites(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<EducationResource> list = resourceService.getUserFavorites(userId);
        return Result.success(list);
    }

    @GetMapping("/likes")
    @ApiOperation("获取用户点赞列表")
    public Result<List<EducationResource>> getUserLikes(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.replace("Bearer ", ""));
        List<EducationResource> list = resourceService.getUserLikes(userId);
        return Result.success(list);
    }

    @PostMapping
    @ApiOperation("上传资源(管理员)")
    public Result<Void> createResource(
            @RequestHeader("Authorization") String token,
            @RequestBody EducationResource resource) {
        checkAdmin(token);
        resourceService.createResource(resource);
        return Result.success("上传成功", null);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新资源(管理员)")
    public Result<Void> updateResource(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id,
            @RequestBody EducationResource resource) {
        checkAdmin(token);
        resource.setId(id);
        resourceService.updateResource(resource);
        return Result.success("更新成功", null);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("下架资源(管理员)")
    public Result<Void> deleteResource(
            @RequestHeader("Authorization") String token,
            @PathVariable Long id) {
        checkAdmin(token);
        resourceService.deleteResource(id);
        return Result.success("下架成功", null);
    }

    private void checkAdmin(String token) {
        String actualToken = token.replace("Bearer ", "");
        Integer role = jwtUtil.getRoleFromToken(actualToken);
        String username = jwtUtil.getUsernameFromToken(actualToken);
        if (!((role != null && role == 3) || "admin".equals(username))) {
            throw new com.mentalhealth.common.exception.BusinessException(403, "权限不足，仅管理员可操作");
        }
    }
}
