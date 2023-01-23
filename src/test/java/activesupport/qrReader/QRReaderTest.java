package activesupport.qrReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static activesupport.qrReader.QRReader.*;

public class QRReaderTest {

    @Test
    public void readText() {
        String decodedQR = decodeQRCodeAndGetSecretFromFile("src/test/resources/download.png");
        String authCode = getTOTPCode(decodedQR);
        Assertions.assertNotNull(authCode);
        System.out.println(authCode);
    }
}