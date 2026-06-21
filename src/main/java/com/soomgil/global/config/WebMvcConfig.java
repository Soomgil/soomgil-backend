package com.soomgil.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.File;

/**
 * 로컬 정적 업로드 파일을 실시간으로 서빙하기 위해 리소스 핸들러를 등록한다.
 *
 * <p>이 핸들러를 등록하면 build static 디렉토리 복사나 재빌드 필요 없이
 * 물리 src 디렉토리의 uploads 내부 파일들이 실시간으로 웹 브라우저에 렌더링된다.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String userDir = System.getProperty("user.dir");
		String uploadPath;
		if (userDir.endsWith("backend")) {
			uploadPath = userDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "uploads" + File.separator;
		} else {
			uploadPath = userDir + File.separator + "backend" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "uploads" + File.separator;
		}

		// Windows 파일 시스템 절대 경로 포맷 지원
		registry.addResourceHandler("/uploads/**")
			.addResourceLocations("file:///" + uploadPath.replace("\\", "/"));
	}
}
