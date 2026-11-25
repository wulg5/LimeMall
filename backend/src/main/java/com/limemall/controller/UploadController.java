package com.limemall.controller;

import com.limemall.common.BusinessException;
import com.limemall.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 图片上传接口。
 *
 * 安全说明：
 * 1. 文件名由服务端生成（UUID），不使用用户提交的文件名，杜绝路径穿越；
 * 2. 仅允许常见位图格式（排除 SVG，避免脚本注入）；
 * 3. 文件保存至配置的上传目录，经 /uploads/** 静态映射对外访问。
 */
@Tag(name = "后台-文件上传")
@RestController
@RequestMapping("/admin/upload")
public class UploadController {

    private static final List<String> ALLOWED_EXTENSIONS =
            List.of(".png", ".jpg", ".jpeg", ".gif", ".webp");

    @Value("${limemall.upload-dir}")
    private String uploadDir;

    @PostMapping("/image")
    @PreAuthorize("hasAnyAuthority('pms:product:create', 'pms:product:update', 'pms:brand:create', 'pms:brand:update')")
    @Operation(summary = "上传图片（返回访问URL）")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("仅支持图片文件");
        }
        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = original.contains(".")
                ? original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT)
                : "";
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 png/jpg/jpeg/gif/webp 格式");
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        Path directory = Paths.get(uploadDir);
        Files.createDirectories(directory);
        file.transferTo(directory.resolve(filename).toFile());
        return Result.ok("/uploads/" + filename);
    }
}
