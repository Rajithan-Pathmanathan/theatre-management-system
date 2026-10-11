package com.vijaya.theatre.canteen;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** UC-19 — one QR per snack order, encoding its pickup-verification token. Same pattern as payment.QrCodeGenerator. */
final class QrCodeGenerator {

    private QrCodeGenerator() {
    }

    static byte[] generatePng(String content, int sizePx) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code for: " + content, e);
        }
    }
}
