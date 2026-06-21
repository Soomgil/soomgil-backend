package com.soomgil.preference.infrastructure.external;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.application.command.dto.SavePlaceTagCandidateCommand;
import com.soomgil.preference.application.service.PlaceTagExtractor;
import com.soomgil.preference.infrastructure.persistence.row.SelectablePreferenceTagRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SpringAiPlaceTagExtractor implements PlaceTagExtractor {

	private static final String SYSTEM_PROMPT = """
		당신은 한국 여행 장소의 취향 태그 분류기입니다.
		제공된 장소 텍스트와 사진을 함께 보고 허용된 태그 코드만 선택하세요.
		장소를 직접 구분하는 태그만 최대 10개 반환하고, 추측이 약하면 제외하세요.
		confidence와 weight는 0에서 1 사이 숫자여야 합니다.
		""";
	private final ObjectProvider<ChatModel> chatModelProvider;
	private final RestClient restClient;

	public SpringAiPlaceTagExtractor(ObjectProvider<ChatModel> chatModelProvider) {
		this.chatModelProvider = chatModelProvider;
		this.restClient = RestClient.builder().build();
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
		List<ImageInput> images = place.photos().stream().limit(3)
			.map(this::downloadImage).filter(java.util.Objects::nonNull).toList();
		ExtractionResponse response = ChatClient.create(model).prompt()
			.system(SYSTEM_PROMPT)
			.user(user -> {
				user.text(prompt);
				images.forEach(image -> user.media(image.mediaType(), image.resource()));
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

	private ImageInput downloadImage(String value) {
		try {
			var response = restClient.get().uri(value).retrieve().toEntity(byte[].class);
			byte[] body = response.getBody();
			if (body == null || body.length == 0 || body.length > 5 * 1024 * 1024) {
				return null;
			}
			MediaType mediaType = response.getHeaders().getContentType();
			if (mediaType == null || !"image".equals(mediaType.getType())) {
				mediaType = MediaType.IMAGE_JPEG;
			}
			return new ImageInput(mediaType, new ByteArrayResource(body));
		}
		catch (RuntimeException exception) {
			return null;
		}
	}

	private record ImageInput(MediaType mediaType, ByteArrayResource resource) {
	}

	public record ExtractionResponse(List<ExtractedTag> tags) {
	}

	public record ExtractedTag(String code, BigDecimal confidence, BigDecimal weight, String rationale) {
	}
}
