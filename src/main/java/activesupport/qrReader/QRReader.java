package activesupport.qrReader;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import de.taimos.totp.TOTP;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Hex;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QRReader {

    public static String decodeQRCodeAndGetSecretFromURL(String imageURL) {
        try {
            URL url = new URL(imageURL);

            BufferedImage bf = ImageIO.read(url.openStream());
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bf)));

            Result result = new MultiFormatReader().decode(bitmap);

            Pattern p = Pattern.compile("[\\d\\D]{52}(?=&period)");
            Matcher matcher = p.matcher(result.getText());

            if (matcher.find())
                return matcher.group();

        } catch (IOException | NotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static String decodeQRCodeAndGetSecretFromFile(String filePath) {
        try {

            BufferedImage bf = ImageIO.read(Files.newInputStream(Paths.get(filePath)));
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(bf)));

            Result result = new MultiFormatReader().decode(bitmap);

            Pattern p = Pattern.compile("[\\d\\D]{52}(?=&period)");
            Matcher matcher = p.matcher(result.getText());

            if (matcher.find())
                return matcher.group();

        } catch (IOException | NotFoundException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static String getTOTPCode(String secretKey) {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        String hexKey = Hex.encodeHexString(bytes);
        return TOTP.getOTP(hexKey);
    }
}