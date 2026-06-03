package webserver.http;

import java.nio.charset.StandardCharsets;

public class ChunkedDecoder {

    private byte[] body;
    private String errorReason;

    public enum Result { DECODING, COMPLETE, ERROR }

    public Result decode(StringBuilder buffer) {
        while (true) {
            int crlf = buffer.indexOf("\r\n");
            if (crlf < 0) return Result.DECODING;

            String sizeLine = buffer.substring(0, crlf).trim();
            int semicolon = sizeLine.indexOf(';');
            if (semicolon >= 0) sizeLine = sizeLine.substring(0, semicolon);

            int chunkSize;
            try {
                chunkSize = Integer.parseInt(sizeLine, 16);
            } catch (NumberFormatException e) {
                errorReason = "Invalid chunk size: " + sizeLine;
                return Result.ERROR;
            }

            buffer.delete(0, crlf + 2);

            if (chunkSize == 0) {
                if (buffer.length() >= 2 && buffer.substring(0, 2).equals("\r\n")) {
                    buffer.delete(0, 2);
                }
                if (body == null) body = new byte[0];
                return Result.COMPLETE;
            }

            if (buffer.length() < chunkSize + 2) return Result.DECODING;

            String chunkData = buffer.substring(0, chunkSize);
            buffer.delete(0, chunkSize + 2);

            byte[] chunkBytes = chunkData.getBytes(StandardCharsets.ISO_8859_1);
            if (body == null) {
                body = chunkBytes;
            } else {
                byte[] combined = new byte[body.length + chunkBytes.length];
                System.arraycopy(body, 0, combined, 0, body.length);
                System.arraycopy(chunkBytes, 0, combined, body.length, chunkBytes.length);
                body = combined;
            }
        }
    }

    public byte[] getBody() {
        return body;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void reset() {
        body = null;
        errorReason = null;
    }
}
