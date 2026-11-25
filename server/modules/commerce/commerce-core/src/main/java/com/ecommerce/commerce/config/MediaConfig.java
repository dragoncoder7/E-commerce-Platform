package com.ecommerce.commerce.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.nio.file.Path;

/** 只读媒体资源映射。 */
@Configuration
public class MediaConfig implements WebMvcConfigurer {
  @Value("${MEDIA_PATH:./media}")
  private String path;

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String uri = Path.of(path).toAbsolutePath().toUri().toString();
    registry.addResourceHandler("/media/**").addResourceLocations(uri.endsWith("/") ? uri : uri + "/");
  }
}
