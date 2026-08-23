package com.soomgil.media.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class MapOverlayImageProcessorTest {

	private final MapOverlayImageProcessor processor = new MapOverlayImageProcessor();

	@Test
	void reencodesWithoutMetadataAndLimitsLongestEdgeTo2048Pixels() throws Exception {
		BufferedImage source = new BufferedImage(4096, 1024, BufferedImage.TYPE_INT_RGB);
		source.setRGB(10, 10, Color.ORANGE.getRGB());
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		ImageIO.write(source, "jpeg", encoded);

		ProcessedMapOverlay result = processor.process(encoded.toByteArray(), "image/jpeg");
		BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(result.bytes()));

		assertThat(result.mimeType()).isEqualTo("image/png");
		assertThat(result.width()).isEqualTo(2048);
		assertThat(result.height()).isEqualTo(512);
		assertThat(decoded.getWidth()).isEqualTo(2048);
		assertThat(decoded.getHeight()).isEqualTo(512);
	}
}
