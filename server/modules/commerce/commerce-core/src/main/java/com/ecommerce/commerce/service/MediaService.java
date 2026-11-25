package com.ecommerce.commerce.service;

import com.ecommerce.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/** 校验图片实际格式并记录上传归属。 */
@Service
@RequiredArgsConstructor
public class MediaService {
  private static final long MAX_BYTES = 5 * 1024 * 1024;
  private static final long MAX_PIXELS = 40_000_000;
  private final JdbcTemplate jdbc;
  @Value("${MEDIA_PATH:./media}")
  private String mediaPath;

  public String upload(MultipartFile file, String ownerType, Long ownerId) {
    if (file.isEmpty() || file.getSize() > MAX_BYTES) {
      throw new BusinessException(400, "请选择不超过 5MB 的 JPEG 或 PNG 图片");
    }
    Path target = null;
    try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new BusinessException(400, "图片格式无效");
      }
      ImageReader reader = readers.next();
      try {
        String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
        reader.setInput(input);
        if (!Set.of("jpeg", "png").contains(format) || (long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
          throw new BusinessException(400, "只支持 JPEG/PNG，且图片像素不能超过 4000 万");
        }
        Path directory = Path.of(mediaPath).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + "." + format;
        target = directory.resolve(name);
        if (!ImageIO.write(reader.read(0), format, target.toFile())) {
          throw new IOException("Image encoder unavailable");
        }
        String url = "/api/media/" + name;
        jdbc.update("INSERT INTO media_file(owner_type,owner_id,url) VALUES(?,?,?)", ownerType, ownerId, url);
        return url;
      } finally {
        reader.dispose();
      }
    } catch (IOException | RuntimeException exception) {
      if (target != null) {
        try {
          Files.deleteIfExists(target);
        } catch (IOException ignored) {
          // 保留原始异常；孤立文件可由维护任务清理。
        }
      }
      if (exception instanceof BusinessException business) {
        throw business;
      }
      throw new BusinessException(500, "图片保存失败，请稍后重试");
    }
  }
}
