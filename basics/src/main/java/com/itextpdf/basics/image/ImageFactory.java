package com.itextpdf.basics.image;

import com.itextpdf.basics.PdfException;
import com.itextpdf.basics.Utilities;
import com.itextpdf.basics.codec.CCITTG4Encoder;
import com.itextpdf.basics.codec.TIFFFaxDecoder;
import com.itextpdf.basics.io.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public final class ImageFactory {

    private static final byte[] gif = new byte[]{'G', 'I', 'F'};
    private static final byte[] jpeg = new byte[]{(byte) 0xFF, (byte) 0xD8};
    private static final byte[] jpeg2000_1 = new byte[]{0x00, 0x00, 0x00, 0x0c};
    private static final byte[] jpeg2000_2 = new byte[]{(byte) 0xff, (byte) 0x4f, (byte) 0xff, 0x51};
    private static final byte[] png = new byte[]{(byte) 137, 80, 78, 71};
    private static final byte[] wmf = new byte[]{(byte) 0xD7, (byte) 0xCD};
    private static final byte[] bmp = new byte[]{'B', 'M'};
    private static final byte[] tiff_1 = new byte[]{'M', 'M', 0, 42};
    private static final byte[] tiff_2 = new byte[]{'I', 'I', 42, 0};
    private static final byte[] jbig2 = new byte[]{(byte) 0x97, 'J', 'B', '2', '\r', '\n', 0x1a, '\n'};

    public static Image getImage(byte[] bytes, boolean recoverImage) {
        return getImageInstance(bytes, recoverImage);
    }

    public static Image getImage(byte[] bytes) {
        return getImage(bytes, false);
    }

    public static Image getImage(URL url, boolean recoverImage) {
        return getImageInstance(url, recoverImage);
    }

    public static Image getImage(URL url) {
        return getImage(url, false);
    }

    public static Image getImage(String filename, boolean recoverImage) throws MalformedURLException {
        return getImage(Utilities.toURL(filename), recoverImage);
    }

    public static Image getImage(String filename) throws MalformedURLException {
        return getImage(filename, false);
    }

    public static Image getImage(final int width, final int height, final boolean reverseBits,
                                    final int typeCCITT, final int parameters, final byte[] data,
                                    final int[] transparency) {
        if (transparency != null && transparency.length != 2)
            throw new PdfException(PdfException.TransparencyLengthMustBeEqualTo2WithCcittImages);
        if (typeCCITT != RawImage.CCITTG4 && typeCCITT != RawImage.CCITTG3_1D && typeCCITT != RawImage.CCITTG3_2D)
            throw new PdfException(PdfException.CcittCompressionTypeMustBeCcittg4Ccittg3_1dOrCcittg3_2d);
        if (reverseBits)
            TIFFFaxDecoder.reverseBits(data);
        RawImage image = new RawImage(data, Image.RAW);
        image.setTypeCcitt(typeCCITT);
        image.height = height;
        image.width = width;
        image.colorSpace = parameters;
        image.transparency = transparency;
        return image;
    }

    public static Image getImage(final int width, final int height, final int components,
                                    final int bpc, final byte[] data, final int[] transparency) {
        if (transparency != null && transparency.length != components * 2)
            throw new PdfException(PdfException.TransparencyLengthMustBeEqualTo2WithCcittImages);
        if (components == 1 && bpc == 1) {
            byte g4[] = CCITTG4Encoder.compress(data, width, height);
            return ImageFactory.getImage(width, height, false, RawImage.CCITTG4, RawImage.CCITT_BLACKIS1, g4, transparency);
        }
        RawImage image = new RawImage(data, Image.RAW);
        image.height = height;
        image.width = width;
        if (components != 1 && components != 3 && components != 4)
            throw new PdfException(PdfException.ComponentsMustBe1_3Or4);
        if (bpc != 1 && bpc != 2 && bpc != 4 && bpc != 8)
            throw new PdfException(PdfException.BitsPerComponentMustBe1_2_4or8);
        image.colorSpace = components;
        image.bpc = bpc;
        image.data = data;
        image.transparency = transparency;
        return image;
    }

    public static Image getBmpImage(URL url, boolean noHeader, int size) {
        byte[] imageType = readImageType(url);
        if (imageTypeIs(imageType, bmp)) {
            Image image = new BmpImage(url, noHeader, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BmpImageHelper.processImage(image, baos);
            return image;
        }
        throw new IllegalArgumentException("BMP image expected.");
    }

    public static Image getBmpImage(byte[] bytes, boolean noHeader, int size) {
        byte[] imageType = readImageType(bytes);
        if (noHeader || imageTypeIs(imageType, bmp)) {
            Image image = new BmpImage(bytes, noHeader, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BmpImageHelper.processImage(image, baos);
            return image;
        }
        throw new IllegalArgumentException("BMP image expected.");
    }

    public static Image getGifImage(URL url, int frame) {
        byte[] imageType = readImageType(url);
        if (imageTypeIs(imageType, gif)) {
            Image image = new GifImage(url, frame);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GifImageHelper.processImage(image, baos);
            return image;
        }
        throw new IllegalArgumentException("GIF image expected.");
    }

    public static Image getGifImage(byte[] bytes, int frame) {
        byte[] imageType = readImageType(bytes);
        if (imageTypeIs(imageType, gif)) {
            Image image = new GifImage(bytes, frame);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GifImageHelper.processImage(image, baos);
            return image;
        }
        throw new IllegalArgumentException("GIF image expected.");

    }

    public static Image getJbig2Image(URL url, int page) {
        if (page < 1)
            throw new IllegalArgumentException("The page number must be greater than 0");
        byte[] imageType = readImageType(url);
        if (imageTypeIs(imageType, jbig2)) {
            Image image = new Jbig2Image(url, page);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Jbig2ImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new IllegalArgumentException("JBIG2 image expected.");
    }

    public static Image getJbig2Image(byte[] bytes, int page) {
        if (page < 1)
            throw new IllegalArgumentException("The page number must be greater than 0");
        byte[] imageType = readImageType(bytes);
        if (imageTypeIs(imageType, jbig2)) {
            Image image = new Jbig2Image(bytes, page);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Jbig2ImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new IllegalArgumentException("JBIG2 image expected.");

    }

    public static Image getJpegImage(URL url) {
        byte[] imageType = readImageType(url);
        if (imageTypeIs(imageType, jpeg)) {
            Image image = new JpegImage(url);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JpegImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new IllegalArgumentException("JPEG image expected.");
    }

    public static Image getJpegImage(byte[] bytes) {
        byte[] imageType = readImageType(bytes);
        if (imageTypeIs(imageType, jpeg)) {
            Image image = new JpegImage(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JpegImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new IllegalArgumentException("JPEG image expected.");

    }

    public static Image getJpeg2000Image(URL url) {
        byte[] imageType = readImageType(url);
        if (imageTypeIs(imageType, jpeg2000_1) || imageTypeIs(imageType, jpeg2000_2)) {
            Image image = new Jpeg2000Image(url);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Jpeg2000ImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new IllegalArgumentException("JPEG2000 image expected.");
    }

    public static Image getJpeg2000Image(byte[] bytes) {
        byte[] imageType = readImageType(bytes);
        if (imageTypeIs(imageType, jpeg2000_1) || imageTypeIs(imageType, jpeg2000_2)) {
            Image image = new Jpeg2000Image(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Jpeg2000ImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new IllegalArgumentException("JPEG2000 image expected.");

    }

    public static Image getPngImage(URL url) {
        byte[] imageType = readImageType(url);
        if (imageTypeIs(imageType, png)) {
            Image image = new PngImage(url);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PngImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new IllegalArgumentException("PNG image expected.");
    }

    public static Image getPngImage(byte[] bytes) {
        byte[] imageType = readImageType(bytes);
        if (imageTypeIs(imageType, png)) {
            Image image = new PngImage(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PngImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new IllegalArgumentException("PNG image expected.");
    }

    public static Image getTiffImage(URL url, boolean recoverFromImageError, int page, boolean direct) {
        byte[] imageType = readImageType(url);
        if (imageTypeIs(imageType, tiff_1) || imageTypeIs(imageType, tiff_2)) {
            Image image = new TiffImage(url, recoverFromImageError, page, direct);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TiffImageHelper.processImage(image, baos);
            return image;
        }
        throw new IllegalArgumentException("TIFF image expected.");
    }

    public static Image getTiffImage(byte[] bytes, boolean recoverFromImageError, int page, boolean direct) {
        byte[] imageType = readImageType(bytes);
        if (imageTypeIs(imageType, tiff_1) || imageTypeIs(imageType, tiff_2)) {
            Image image = new TiffImage(bytes, recoverFromImageError, page, direct);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TiffImageHelper.processImage(image, baos);
            return image;
        }
        throw new IllegalArgumentException("TIFF image expected.");
    }

    public static Image getRawImage(byte[] bytes) {
        return new RawImage(bytes, Image.RAW);
    }

    private static Image getImageInstance(URL source, boolean recoverImage) {
        byte[] imageType = readImageType(source);
        if (imageTypeIs(imageType, gif)) {
            Image image = new GifImage(source, 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GifImageHelper.processImage(image, baos);
            return image;
        } else if (imageTypeIs(imageType, jpeg)) {
            Image image = new JpegImage(source);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JpegImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        } else if (imageTypeIs(imageType, jpeg2000_1) || imageTypeIs(imageType, jpeg2000_2)) {
            Image image = new Jpeg2000Image(source);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Jpeg2000ImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        } else if (imageTypeIs(imageType, png)) {
            Image image = new PngImage(source);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PngImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        } else if (imageTypeIs(imageType, bmp)) {
            Image image = new BmpImage(source, false, 0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BmpImageHelper.processImage(image, baos);
            return image;
        } else if (imageTypeIs(imageType, tiff_1) || imageTypeIs(imageType, tiff_2)) {
            Image image = new TiffImage(source, recoverImage, 1, false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TiffImageHelper.processImage(image, baos);
            return image;
        } else if (imageTypeIs(imageType, jbig2)) {
            Image image = new Jbig2Image(source, 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Jbig2ImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new PdfException(PdfException.ImageFormatCannotBeRecognized);
    }

    private static Image getImageInstance(byte[] bytes, boolean recoverImage) {
        byte[] imageType = readImageType(bytes);
        if (imageTypeIs(imageType, gif)) {
            Image image = new GifImage(bytes, 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GifImageHelper.processImage(image, baos);
            return image;
        } else if (imageTypeIs(imageType, jpeg)) {
            Image image = new JpegImage(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JpegImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        } else if (imageTypeIs(imageType, jpeg2000_1) || imageTypeIs(imageType, jpeg2000_2)) {
            Image image = new Jpeg2000Image(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Jpeg2000ImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        } else if (imageTypeIs(imageType, png)) {
            Image image = new PngImage(bytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PngImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        } else if (imageTypeIs(imageType, bmp)) {
            Image image = new BmpImage(bytes, false, 0);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BmpImageHelper.processImage(image, baos);
            return image;
        } else if (imageTypeIs(imageType, tiff_1) || imageTypeIs(imageType, tiff_2)) {
            Image image = new TiffImage(bytes, recoverImage, 1, false);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TiffImageHelper.processImage(image, baos);
            return image;
        } else if (imageTypeIs(imageType, jbig2)) {
            Image image = new Jbig2Image(bytes, 1);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Jbig2ImageHelper.processImage(image, baos);
            image.data = baos.toByteArray();
            return image;
        }
        throw new PdfException(PdfException.ImageFormatCannotBeRecognized);
    }

    private static boolean imageTypeIs(byte[] imageType, byte[] compareWith) {
        for (int i = 0; i < compareWith.length; i++) {
            if (imageType[i] != compareWith[i])
                return false;
        }
        return true;
    }

    private static <T> byte[] readImageType(T source) {
        InputStream is = null;
        try {
            if (source instanceof URL) {
                is = ((URL) source).openStream();
            } else {
                is = new ByteArrayInputStream((byte[])source);
            }
            byte[] bytes = new byte[8];
            is.read(bytes);
            return bytes;
        } catch (IOException e) {
            throw new PdfException(PdfException.IoException, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) { }
            }
        }

    }
}
