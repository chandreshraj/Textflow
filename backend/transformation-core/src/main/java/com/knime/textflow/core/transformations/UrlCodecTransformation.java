package com.knime.textflow.core.transformations;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.auto.service.AutoService;
import static com.knime.textflow.core.Parameters.choice;
import com.knime.textflow.core.Processor;
import com.knime.textflow.core.TransformException;
import com.knime.textflow.core.Transformation;

@AutoService(Transformation.class)
public final class UrlCodecTransformation implements Transformation {
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    @Override
    public Descriptor descriptor() {
        return new Descriptor("url-codec", "URL encode/decode",
                "Encode or decode a URL component using UTF-8. Spaces become %20; + stays literal when decoding.",
                List.of(choice("mode", "URL mode", "Encode", List.of("Encode", "Decode"))));
    }

    @Override
    public String apply(String input, Map<String, Object> parameters) {
        try {
            ByteBuffer bytes = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(input));
            return "Encode".equals(parameters.get("mode")) ? encode(bytes) : decode(bytes);
        } catch (CharacterCodingException e) {
            throw new TransformException("Input must contain valid Unicode and percent-encoded UTF-8.", null, "input");
        }
    }

    private String encode(ByteBuffer bytes) {
        int length = 0;
        for (int i = bytes.position(); i < bytes.limit(); i++) {
            length += isUnreserved(bytes.get(i) & 0xFF) ? 1 : 3;
            if (length > Processor.MAX_TEXT) {
                throw new TransformException("Encoded output exceeds 100000 UTF-16 code units.", null, "input");
            }
        }
        var output = new StringBuilder(length);
        while (bytes.hasRemaining()) {
            int value = bytes.get() & 0xFF;
            if (isUnreserved(value))
                output.append((char) value);
            else
                output.append('%').append(HEX[value >>> 4]).append(HEX[value & 15]);
        }
        return output.toString();
    }

    private String decode(ByteBuffer bytes) throws CharacterCodingException {
        var decoded = new ByteArrayOutputStream(bytes.remaining());
        while (bytes.hasRemaining()) {
            int value = bytes.get() & 0xFF;
            if (value == '%') {
                if (bytes.remaining() < 2)
                    throw invalidEscape();
                int high = hex(bytes.get() & 0xFF);
                int low = hex(bytes.get() & 0xFF);
                if (high < 0 || low < 0)
                    throw invalidEscape();
                value = (high << 4) | low;
            }
            decoded.write(value);
        }
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(decoded.toByteArray())).toString();
    }

    private static boolean isUnreserved(int value) {
        return value >= 'a' && value <= 'z' || value >= 'A' && value <= 'Z'
                || value >= '0' && value <= '9' || value == '-' || value == '.' || value == '_' || value == '~';
    }

    private static int hex(int value) {
        if (value >= '0' && value <= '9')
            return value - '0';
        if (value >= 'a' && value <= 'f')
            return value - 'a' + 10;
        if (value >= 'A' && value <= 'F')
            return value - 'A' + 10;
        return -1;
    }

    private static TransformException invalidEscape() {
        return new TransformException("Each percent escape must contain two hexadecimal digits, for example %20.", null,
                "input");
    }
}
