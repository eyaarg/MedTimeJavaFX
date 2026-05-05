package esprit.fx.utils;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

public class ImageUtils {


    public static String encodeImageToBase64(File file) throws Exception {
        // 1. Lire tous les octets du fichier
        byte[] fileContent = Files.readAllBytes(file.toPath());

        // 2. Convertir ces octets en chaîne Base64
        return Base64.getEncoder().encodeToString(fileContent);
    }
}