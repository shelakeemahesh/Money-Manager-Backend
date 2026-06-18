package in.maheshshelakee.moneymanager.service.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    public Map<String, Object> getUploadSignature(String uploadPreset) {
        long timestamp = System.currentTimeMillis() / 1000L;
        
        // Construct string to sign: timestamp=<timestamp>&upload_preset=<preset><apiSecret>
        // Note: parameters sorted alphabetically: t(imestamp) before u(pload_preset).
        String stringToSign = "timestamp=" + timestamp + "&upload_preset=" + uploadPreset + apiSecret;
        
        String signature = sha1(stringToSign);
        
        Map<String, Object> response = new HashMap<>();
        response.put("signature", signature);
        response.put("timestamp", timestamp);
        response.put("apiKey", apiKey);
        response.put("cloudName", cloudName);
        response.put("uploadPreset", uploadPreset);
        
        return response;
    }

    private String sha1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-1 algorithm not found", e);
            throw new RuntimeException("Error hashing signature", e);
        }
    }
}
