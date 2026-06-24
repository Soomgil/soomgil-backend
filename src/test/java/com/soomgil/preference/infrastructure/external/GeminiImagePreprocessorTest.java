package com.soomgil.preference.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class GeminiImagePreprocessorTest {

	private final GeminiImagePreprocessor preprocessor = new GeminiImagePreprocessor();

	@Test
	void resizesAndCompressesLargeImageForGmsRequest() throws Exception {
		byte[] source = createPng(1600, 1000);

		GeminiImagePreprocessor.PreparedImage result = preprocessor.prepare(source).orElseThrow();
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));

		assertThat(result.mediaType()).isEqualTo(MediaType.IMAGE_JPEG);
		assertThat(result.bytes()).hasSizeLessThanOrEqualTo(GeminiImagePreprocessor.MAX_OUTPUT_BYTES);
		assertThat(Math.max(decoded.getWidth(), decoded.getHeight())).isLessThanOrEqualTo(512);
	}

	@Test
	void rejectsUndecodableImage() {
		assertThat(preprocessor.prepare(new byte[] {1, 2, 3})).isEmpty();
	}

	private byte[] createPng(int width, int height) throws Exception {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		try {
			for (int x = 0; x < width; x += 20) {
				graphics.setColor(new Color(x % 255, (x * 2) % 255, (x * 3) % 255));
				graphics.fillRect(x, 0, 20, height);
			}
		}
		finally {
			graphics.dispose();
		}
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", output);
			return output.toByteArray();
		}
	}
}
