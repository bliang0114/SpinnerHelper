package cn.github.spinner.util;

import com.intellij.openapi.application.PathManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

public final class MatrixConnectorPackageManager {
    public static final String FILE_NAME = "matrix-connector-1.3.0.jar";
    public static final String DRIVER_CLASS_NAME = "cn.github.connector.MatrixCommonDriver";

    private static final URI DOWNLOAD_URI = URI.create("https://gitee.com/bliang0114/SpinnerHelper/releases/download/3.0.0/" + FILE_NAME);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(3);

    private MatrixConnectorPackageManager() {
    }

    public static File getCachedJarFile() {
        return getCachedJarPath().toFile();
    }

    public static Path getCachedJarPath() {
        return Path.of(PathManager.getConfigPath(), "SpinnerHelper", "matrix-drivers", FILE_NAME);
    }

    public static boolean isDownloaded() {
        return isUsableJar(getCachedJarPath());
    }

    public static synchronized File getOrDownload() throws IOException, InterruptedException {
        Path target = getCachedJarPath();
        if (isUsableJar(target)) {
            return target.toFile();
        }

        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), "matrix-connector-", ".jar.tmp");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
            HttpRequest request = HttpRequest.newBuilder(DOWNLOAD_URI)
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode());
            }
            try (InputStream inputStream = response.body()) {
                Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!isUsableJar(temp)) {
                throw new IOException("Downloaded file is empty");
            }
            moveDownloadedFile(temp, target);
            return target.toFile();
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static boolean isUsableJar(Path path) {
        try {
            return Files.isRegularFile(path) && Files.size(path) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private static void moveDownloadedFile(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
