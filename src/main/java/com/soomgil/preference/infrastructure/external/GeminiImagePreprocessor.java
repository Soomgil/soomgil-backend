package com.soomgil.preference.infrastructure.external;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.springframework.http.MediaType;

/**
 * Gemini 멀티모달 요청에 첨부할 이미지를 작은 JPEG로 정규화한다.
 *
 * <p>GMS gateway의 요청 본문 제한을 넘지 않도록 해상도와 JPEG 품질을 단계적으로 낮춘다.
 * 디코딩할 수 없거나 제한 크기 안으로 줄일 수 없는 이미지는 제외한다.
 */
final class GeminiImagePreprocessor {

	static final int MAX_OUTPUT_BYTES = 80 * 1024;
	private static final int[] MAX_DIMENSIONS = {512, 448, 384, 320, 256};
	private static final float[] JPEG_QUALITIES = {0.82f, 0.68f, 0.54f};

	Optional<PreparedImage> prepare(byte[] sourceBytes) {
		try {
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(sourceBytes));
			if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) {
				return Optional.empty();
			}
			for (int maxDimension : MAX_DIMENSIONS) {
				BufferedImage resized = resize(source, maxDimension);
				for (float quality : JPEG_QUALITIES) {
					byte[] encoded = encodeJpeg(resized, quality);
					if (encoded.length <= MAX_OUTPUT_BYTES) {
						return Optional.of(new PreparedImage(MediaType.IMAGE_JPEG, encoded));
					}
				}
			}
			return Optional.empty();
		}
		catch (IOException | RuntimeException exception) {
			return Optional.empty();
		}
	}

	private BufferedImage resize(BufferedImage source, int maxDimension) {
		double scale = Math.min(1.0, (double) maxDimension / Math.max(source.getWidth(), source.getHeight()));
		int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
		BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = target.createGraphics();
		try {
			graphics.setColor(Color.WHITE);
			graphics.fillRect(0, 0, width, height);
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.drawImage(source, 0, 0, width, height, null);
		}
		finally {
			graphics.dispose();
		}
		return target;
	}

	private byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
		if (!writers.hasNext()) {
			throw new IOException("JPEG ImageWriter is unavailable.");
		}
		ImageWriter writer = writers.next();
		try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			 MemoryCacheImageOutputStream output = new MemoryCacheImageOutputStream(bytes)) {
			ImageWriteParam parameters = writer.getDefaultWriteParam();
			parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			parameters.setCompressionQuality(quality);
			writer.setOutput(output);
			writer.write(null, new IIOImage(image, null, null), parameters);
			output.flush();
			return bytes.toByteArray();
		}
		finally {
			writer.dispose();
		}
	}

	record PreparedImage(MediaType mediaType, byte[] bytes) {
	}
}
