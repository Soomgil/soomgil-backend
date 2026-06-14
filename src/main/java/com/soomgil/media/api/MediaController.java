package com.soomgil.media.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.media.api.dto.CreateMediaFileRequest;
import com.soomgil.media.api.dto.CreateUploadUrlRequest;
import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.media.api.dto.UploadUrlResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/media")
public class MediaController extends ApiControllerSupport {

	@PostMapping("/upload-url")
	public UploadUrlResponse createUploadUrl(@Valid @RequestBody CreateUploadUrlRequest request) {
		return notImplemented();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MediaFile createMediaFile(@Valid @RequestBody CreateMediaFileRequest request) {
		return notImplemented();
	}

	@GetMapping("/{mediaId}")
	public MediaFile getMediaFile(@PathVariable UUID mediaId) {
		return notImplemented();
	}

	@DeleteMapping("/{mediaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMediaFile(@PathVariable UUID mediaId) {
		notImplemented();
	}
}
