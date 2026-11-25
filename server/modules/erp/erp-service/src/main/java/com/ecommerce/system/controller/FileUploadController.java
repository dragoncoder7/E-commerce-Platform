package com.ecommerce.system.controller;

import com.ecommerce.commerce.service.MediaService;
import com.ecommerce.common.core.domain.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** ERP 图片上传。 */
@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class FileUploadController {
  private final MediaService media;

  @PostMapping("/image")
  public Result<String> upload(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
    return Result.success(media.upload(file, "erp", (Long) request.getAttribute("actorId")));
  }
}
