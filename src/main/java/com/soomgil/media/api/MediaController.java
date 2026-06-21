package com.soomgil.media.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.media.api.dto.CreateMediaFileRequest;
import com.soomgil.media.api.dto.CreateUploadUrlRequest;
import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.media.api.dto.UploadUrlResponse;
import com.soomgil.media.infrastructure.persistence.MediaFileMapper;
import com.soomgil.media.infrastructure.persistence.MediaFileRecord;
import jakarta.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/media")
public class MediaController extends ApiControllerSupport {

	private final MediaFileMapper mediaFileMapper;

	public MediaController(MediaFileMapper mediaFileMapper) {
		this.mediaFileMapper = mediaFileMapper;
	}

	@PostMapping("/upload-url")
	public UploadUrlResponse createUploadUrl(@Valid @RequestBody CreateUploadUrlRequest request) {
		return notImplemented();
	}

	@PostMapping(consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public MediaFile createMediaFile(@Valid @RequestBody CreateMediaFileRequest request) {
		return notImplemented();
	}

	@PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public MediaFile uploadMediaFile(
		@org.springframework.security.core.annotation.AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam("file") MultipartFile file
	) {
		UUID mediaId = UUID.randomUUID();
		String originalFilename = file.getOriginalFilename();
		String ext = "";
		if (originalFilename != null && originalFilename.contains(".")) {
			ext = originalFilename.substring(originalFilename.lastIndexOf("."));
		}
		String filename = mediaId.toString() + ext;

		// 로컬 static 폴더에 파일 쓰기
		String userDir = System.getProperty("user.dir");
		File uploadDir;
		if (userDir.endsWith("backend")) {
			uploadDir = new File(userDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "uploads");
		} else {
			uploadDir = new File(userDir + File.separator + "backend" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "static" + File.separator + "uploads");
		}
		if (!uploadDir.exists()) {
			uploadDir.mkdirs();
		}
		File dest = new File(uploadDir, filename);
		try {
			file.transferTo(dest);
		} catch (IOException e) {
			throw new RuntimeException("File upload failed", e);
		}

		// 빌드 static 폴더 (런타임 즉시 서빙용)에 추가 쓰기
		File buildUploadDir;
		if (userDir.endsWith("backend")) {
			buildUploadDir = new File(userDir + File.separator + "build" + File.separator + "resources" + File.separator + "main" + File.separator + "static" + File.separator + "uploads");
		} else {
			buildUploadDir = new File(userDir + File.separator + "backend" + File.separator + "build" + File.separator + "resources" + File.separator + "main" + File.separator + "static" + File.separator + "uploads");
		}
		if (!buildUploadDir.exists()) {
			buildUploadDir.mkdirs();
		}
		File buildDest = new File(buildUploadDir, filename);
		try {
			java.nio.file.Files.copy(dest.toPath(), buildDest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.err.println("Failed to copy upload file to runtime build directory: " + e.getMessage());
		}

		String publicUrlStr = "http://localhost:8080/uploads/" + filename;
		Instant now = Instant.now();

		mediaFileMapper.insert(
			mediaId,
			currentUser.userId(),
			"LOCAL",
			"default-bucket",
			filename,
			publicUrlStr,
			file.getContentType(),
			file.getSize(),
			null, null,
			null, null,
			"ACTIVE",
			now
		);

		return new MediaFile(
			mediaId,
			URI.create(publicUrlStr),
			file.getContentType(),
			file.getSize(),
			null, null,
			"ACTIVE",
			OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
		);
	}

	@GetMapping("/{mediaId}")
	public MediaFile getMediaFile(@PathVariable UUID mediaId) {
		MediaFileRecord record = mediaFileMapper.findById(mediaId)
			.orElseThrow(() -> new RuntimeException("Media file not found"));

		return new MediaFile(
			record.id(),
			record.publicUrl() != null ? URI.create(record.publicUrl()) : null,
			record.mimeType(),
			record.byteSize(),
			record.width(),
			record.height(),
			record.status(),
			OffsetDateTime.ofInstant(record.createdAt(), ZoneOffset.UTC)
		);
	}

	@DeleteMapping("/{mediaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMediaFile(@PathVariable UUID mediaId) {
		notImplemented();
	}
}
