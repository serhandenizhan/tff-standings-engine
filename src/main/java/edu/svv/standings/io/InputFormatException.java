package edu.svv.standings.io;

/**
 * Girdi dosyasında biçim hatası olduğunda fırlatılır (V-08).
 * Mesaj dosya adını ve (varsa) 1 tabanlı satır numarasını içerir.
 */
public class InputFormatException extends RuntimeException {

    public InputFormatException(String message) {
        super(message);
    }

    public InputFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
