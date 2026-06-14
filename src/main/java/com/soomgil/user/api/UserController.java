package com.soomgil.user.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.user.api.dto.PagedUserSummary;
import com.soomgil.user.api.dto.UpdateMeRequest;
import com.soomgil.user.api.dto.UpdateUserSettingsRequest;
import com.soomgil.user.api.dto.User;
import com.soomgil.user.api.dto.UserPublicProfile;
import com.soomgil.user.api.dto.UserSettings;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class UserController extends ApiControllerSupport {

	@GetMapping("/me")
	public User getMe() {
		return notImplemented();
	}

	@PatchMapping("/me")
	public User updateMe(@Valid @RequestBody UpdateMeRequest request) {
		return notImplemented();
	}

	@DeleteMapping("/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMe() {
		notImplemented();
	}

	@GetMapping("/me/settings")
	public UserSettings getMySettings() {
		return notImplemented();
	}

	@PatchMapping("/me/settings")
	public UserSettings updateMySettings(@Valid @RequestBody UpdateUserSettingsRequest request) {
		return notImplemented();
	}

	@GetMapping("/users")
	public PagedUserSummary searchUsers(
		@RequestParam(required = false) String query,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@GetMapping("/users/{userId}")
	public UserPublicProfile getUserProfile(@PathVariable UUID userId) {
		return notImplemented();
	}
}
