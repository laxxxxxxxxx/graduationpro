package com.mentalhealth.common.controller;

import com.mentalhealth.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/file")
@Api(tags = "文件上传")
public class FileController {

    @Value("${file.upload-path:uploads/}")
    private String uploadPath;

    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public Result<String> upload(@RequestParam("file") MultipartFile file, @RequestParam(defaultValue = "common") String type) {
        if (file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        // 确定子目录
        String subDir = "others";
        if (type.equals("image") || file.getContentType().startsWith("image/")) {
            subDir = "images";
        } else if (type.equals("video") || file.getContentType().startsWith("video/")) {
            subDir = "videos";
        } else if (type.equals("audio") || file.getContentType().startsWith("audio/")) {
            subDir = "audios";
        }

        // 创建目录
        File dir = new File(uploadPath + subDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + extension;

        // 保存文件
        File dest = new File(dir.getAbsolutePath() + File.separator + fileName);
        try {
            file.transferTo(dest);
            // 返回相对路径给前端
            String relativePath = "/uploads/" + subDir + "/" + fileName;
            log.info("文件上传成功: {}", relativePath);
            return Result.success(relativePath);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
