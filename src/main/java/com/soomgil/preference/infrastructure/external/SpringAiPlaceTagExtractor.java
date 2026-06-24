package com.soomgil.preference.infrastructure.external;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.application.command.dto.SavePlaceTagCandidateCommand;
import com.soomgil.preference.application.service.PlaceTagExtractor;
import com.soomgil.preference.infrastructure.persistence.row.SelectablePreferenceTagRow;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * KTO 장소 텍스트와 사진을 Gemini로 분류해 preference 태그 후보를 생성한다.
 *
 * <p>외부 이미지 원본은 GMS gateway 요청 제한에 맞게 축소·압축하며,
 * 디코딩 또는 압축에 실패한 이미지는 제외하고 나머지 입력으로 분석을 계속한다.
 */
@Component
public class SpringAiPlaceTagExtractor implements PlaceTagExtractor {

	private static final int MAX_IMAGES = 1;
	private static final int MAX_TOTAL_IMAGE_BYTES = 80 * 1024;
	private static final int MAX_SOURCE_IMAGE_BYTES = 5 * 1024 * 1024;
	private static final String SYSTEM_PROMPT = """
		당신은 한국 여행 장소의 취향 태그 분류기입니다.
		제공된 장소 텍스트와 사진을 함께 보고 허용된 태그 코드만 선택하세요.
		장소를 직접 구분하는 태그만 최대 10개 반환하고, 추측이 약하면 제외하세요.
		confidence와 weight는 0에서 1 사이 숫자여야 합니다.
		""";
	private final ObjectProvider<ChatModel> chatModelProvider;
	private final RestClient restClient;
	private final GeminiImagePreprocessor imagePreprocessor;

	public SpringAiPlaceTagExtractor(ObjectProvider<ChatModel> chatModelProvider) {
		this.chatModelProvider = chatModelProvider;
		this.restClient = RestClient.builder().build();
		this.imagePreprocessor = new GeminiImagePreprocessor();
	}

	@Override
	public List<SavePlaceTagCandidateCommand> extract(
		TourismPlaceFeedItem place,
		List<SelectablePreferenceTagRow> allowedTags
	) {
		ChatModel model = chatModelProvider.getIfAvailable();
		if (model == null) {
			throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
		}
		Set<String> allowedCodes = allowedTags.stream().map(SelectablePreferenceTagRow::code).collect(Collectors.toSet());
		String dictionary = allowedTags.stream()
			.map(tag -> tag.code() + "=" + tag.displayName())
			.collect(Collectors.joining(", "));
		String prompt = """
			장소명: %s
			분류: %s
			주소: %s
			설명: %s
			허용 태그: %s
			""".formatted(place.name(), place.category(), place.address(), place.description(), dictionary);
		List<GeminiImagePreprocessor.PreparedImage> images = downloadImages(place.photos());
		ExtractionResponse response = ChatClient.create(model).prompt()
			.system(SYSTEM_PROMPT)
			.user(user -> {
				user.text(prompt);
				images.forEach(image -> user.media(image.mediaType(), new ByteArrayResource(image.bytes())));
			})
			.call()
			.entity(ExtractionResponse.class);
		if (response == null || response.tags() == null) {
			return List.of();
		}
		return response.tags().stream()
			.filter(tag -> tag.code() != null && allowedCodes.contains(tag.code()))
			.limit(10)
			.map(tag -> new SavePlaceTagCandidateCommand(
				tag.code(), normalized(tag.confidence()), normalized(tag.weight()), true, tag.rationale()
			))
			.toList();
	}

	private BigDecimal normalized(BigDecimal value) {
		if (value == null) return BigDecimal.ZERO;
		return value.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(4, java.math.RoundingMode.HALF_UP);
	}

	private List<GeminiImagePreprocessor.PreparedImage> downloadImages(List<String> urls) {
		List<GeminiImagePreprocessor.PreparedImage> images = new ArrayList<>();
		int totalBytes = 0;
		for (String url : urls) {
			if (images.size() >= MAX_IMAGES) {
				break;
			}
			GeminiImagePreprocessor.PreparedImage image = downloadImage(url);
			if (image == null || totalBytes + image.bytes().length > MAX_TOTAL_IMAGE_BYTES) {
				continue;
			}
			images.add(image);
			totalBytes += image.bytes().length;
		}
		return List.copyOf(images);
	}

	private GeminiImagePreprocessor.PreparedImage downloadImage(String value) {
		try {
			var response = restClient.get().uri(value).retrieve().toEntity(byte[].class);
			byte[] body = response.getBody();
			if (body == null || body.length == 0 || body.length > MAX_SOURCE_IMAGE_BYTES) {
				return null;
			}
			return imagePreprocessor.prepare(body).orElse(null);
		}
		catch (RuntimeException exception) {
			return null;
		}
	}

	public record ExtractionResponse(List<ExtractedTag> tags) {
	}

	public record ExtractedTag(String code, BigDecimal confidence, BigDecimal weight, String rationale) {
	}
}
