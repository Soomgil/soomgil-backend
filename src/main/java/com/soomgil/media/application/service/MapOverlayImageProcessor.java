package com.soomgil.media.application.service;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/** MAP_OVERLAY 이미지를 orientation 보정하고 긴 변 2048px PNG로 재인코딩한다. */
@Component
public final class MapOverlayImageProcessor {

	private static final int MAX_EDGE = 2048;

	/**
	 * JPG, PNG, WebP를 decode한 뒤 metadata 없이 안전한 PNG로 반환한다.
	 *
	 * @param bytes 원본 이미지 bytes
	 * @param mimeType 검증된 원본 MIME
	 * @return 정제 이미지
	 */
	public ProcessedMapOverlay process(byte[] bytes, String mimeType) {
		if (bytes == null || bytes.length == 0 || mimeType == null) {
			throw invalidImage();
		}
		try {
			BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
			if (decoded == null) {
				throw invalidImage();
			}
			int orientation = "image/jpeg".equals(mimeType) ? jpegExifOrientation(bytes) : 1;
			BufferedImage oriented = orient(decoded, orientation);
			BufferedImage resized = resize(oriented);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			if (!ImageIO.write(resized, "png", output)) {
				throw invalidImage();
			}
			return new ProcessedMapOverlay(output.toByteArray(), "image/png", resized.getWidth(), resized.getHeight());
		}
		catch (IOException | RuntimeException exception) {
			if (exception instanceof BusinessException businessException) {
				throw businessException;
			}
			throw new BusinessException(ErrorCode.MEDIA_METADATA_MISMATCH, "Map overlay image could not be processed.");
		}
	}

	private BufferedImage resize(BufferedImage image) {
		double ratio = Math.min(1D, (double) MAX_EDGE / Math.max(image.getWidth(), image.getHeight()));
		int width = Math.max(1, (int) Math.round(image.getWidth() * ratio));
		int height = Math.max(1, (int) Math.round(image.getHeight() * ratio));
		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = result.createGraphics();
		try {
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.drawImage(image, 0, 0, width, height, null);
		}
		finally {
			graphics.dispose();
		}
		return result;
	}

	private BufferedImage orient(BufferedImage source, int orientation) {
		if (orientation <= 1 || orientation > 8) {
			return source;
		}
		int width = source.getWidth();
		int height = source.getHeight();
		boolean swap = orientation >= 5;
		BufferedImage target = new BufferedImage(
			swap ? height : width,
			swap ? width : height,
			BufferedImage.TYPE_INT_ARGB
		);
		AffineTransform transform = switch (orientation) {
			case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
			case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
			case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
			case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
			case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
			case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
			case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
			default -> new AffineTransform();
		};
		Graphics2D graphics = target.createGraphics();
		try {
			graphics.drawImage(source, transform, null);
		}
		finally {
			graphics.dispose();
		}
		return target;
	}

	private int jpegExifOrientation(byte[] bytes) {
		if (bytes.length < 4 || unsigned(bytes[0]) != 0xFF || unsigned(bytes[1]) != 0xD8) {
			return 1;
		}
		int offset = 2;
		while (offset + 4 <= bytes.length) {
			if (unsigned(bytes[offset]) != 0xFF) {
				offset++;
				continue;
			}
			int marker = unsigned(bytes[offset + 1]);
			int length = readUnsignedShort(bytes, offset + 2, false);
			if (length < 2 || offset + 2 + length > bytes.length) {
				return 1;
			}
			if (marker == 0xE1 && length >= 14 && matchesExif(bytes, offset + 4)) {
				return tiffOrientation(bytes, offset + 10, offset + 2 + length);
			}
			offset += length + 2;
		}
		return 1;
	}

	private boolean matchesExif(byte[] bytes, int offset) {
		return offset + 6 <= bytes.length
			&& bytes[offset] == 'E' && bytes[offset + 1] == 'x' && bytes[offset + 2] == 'i'
			&& bytes[offset + 3] == 'f' && bytes[offset + 4] == 0 && bytes[offset + 5] == 0;
	}

	private int tiffOrientation(byte[] bytes, int tiff, int end) {
		if (tiff + 8 > end) return 1;
		boolean little = bytes[tiff] == 'I' && bytes[tiff + 1] == 'I';
		boolean big = bytes[tiff] == 'M' && bytes[tiff + 1] == 'M';
		if (!little && !big) return 1;
		long directoryOffset = readUnsignedInt(bytes, tiff + 4, little);
		int directory = tiff + (int) directoryOffset;
		if (directory < tiff || directory + 2 > end) return 1;
		int count = readUnsignedShort(bytes, directory, little);
		for (int index = 0; index < count; index++) {
			int entry = directory + 2 + index * 12;
			if (entry + 12 > end) return 1;
			if (readUnsignedShort(bytes, entry, little) == 0x0112) {
				return readUnsignedShort(bytes, entry + 8, little);
			}
		}
		return 1;
	}

	private int readUnsignedShort(byte[] bytes, int offset, boolean little) {
		if (offset < 0 || offset + 2 > bytes.length) return 0;
		return little
			? unsigned(bytes[offset]) | unsigned(bytes[offset + 1]) << 8
			: unsigned(bytes[offset]) << 8 | unsigned(bytes[offset + 1]);
	}

	private long readUnsignedInt(byte[] bytes, int offset, boolean little) {
		if (offset < 0 || offset + 4 > bytes.length) return 0;
		long first = readUnsignedShort(bytes, little ? offset : offset + 2, little);
		long second = readUnsignedShort(bytes, little ? offset + 2 : offset, little);
		return first | second << 16;
	}

	private int unsigned(byte value) {
		return value & 0xFF;
	}

	private BusinessException invalidImage() {
		return new BusinessException(ErrorCode.MEDIA_METADATA_MISMATCH, "Map overlay image is invalid.");
	}
}
