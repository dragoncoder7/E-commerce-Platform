package com.ecommerce.system.controller;

import com.ecommerce.common.core.domain.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
public class FileUploadController {
    
    @Value("${file.upload.path:/data/upload}")
    private String uploadPath;
    
    @Value("${file.upload.base-url:http://localhost:8081}")
    private String baseUrl;
    
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.fail("文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return Result.fail("文件名不能为空");
        }
        
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        if (!isImageFile(extension)) {
            return Result.fail("只支持图片文件");
        }
        
        try {
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = UUID.randomUUID().toString() + extension;
            String relativePath = "images/" + datePath + "/" + fileName;
            
            Path uploadDir = Paths.get(uploadPath, "images", datePath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            Path targetPath = uploadDir.resolve(fileName);
            file.transferTo(targetPath.toFile());
            
            String fileUrl = baseUrl + "/upload/" + relativePath;
            return Result.success(fileUrl);
            
        } catch (IOException e) {
            return Result.fail("文件上传失败：" + e.getMessage());
        }
    }
    
    private boolean isImageFile(String extension) {
        String ext = extension.toLowerCase();
        return ext.equals(".jpg") || ext.equals(".jpeg") || 
               ext.equals(".png") || ext.equals(".gif") || 
               ext.equals(".webp");
    }
}
