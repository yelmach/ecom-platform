package ecom.media_service.service;

import java.util.Arrays;
import java.util.Objects;

public record ValidatedFile(
        byte[] bytes,
        String contentType,
        String extension) {

    public ValidatedFile {
        bytes = Arrays.copyOf(Objects.requireNonNull(bytes), bytes.length);
        Objects.requireNonNull(contentType);
        Objects.requireNonNull(extension);
    }

    @Override
    public byte[] bytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ValidatedFile other)) {
            return false;
        }
        return Arrays.equals(bytes, other.bytes)
                && contentType.equals(other.contentType)
                && extension.equals(other.extension);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(bytes);
        result = 31 * result + contentType.hashCode();
        result = 31 * result + extension.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ValidatedFile[bytes=" + Arrays.toString(bytes)
                + ", contentType=" + contentType
                + ", extension=" + extension
                + "]";
    }
}
