package cn.github.spinner.util;

import cn.github.spinner.context.UserInput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConsoleFileManager {
    public static final String CONSOLE_PREFIX = "MQL Console ";

    private static final String PLUGIN_DIRECTORY = "SpinnerHelper";
    private static final String CONSOLES_DIRECTORY = "consoles";
    private static final String MQL_EXTENSION = ".mql";
    private static final Pattern NEW_CONSOLE_PATTERN = Pattern.compile("^" + Pattern.quote(CONSOLE_PREFIX) + "(\\d+)$");
    private static final Pattern INVALID_FILE_NAME_CHARS = Pattern.compile("[\\\\/:*?\"<>|]");

    private ConsoleFileManager() {
    }

    public static @NotNull ConsoleManager ensureDefaultConsole(@NotNull Project project) {
        return ensureConsole(project, UserInput.DEFAULT_MQL_CONSOLE);
    }

    public static @NotNull ConsoleManager createNewConsole(@NotNull Project project) {
        return ensureConsole(project, CONSOLE_PREFIX + nextConsoleIndex(project));
    }

    public static @NotNull ConsoleManager ensureConsole(@NotNull Project project, @NotNull String consoleName) {
        VirtualFile consoleFile = ensureConsoleFile(project, consoleName);
        return ensureConsole(project, consoleName, consoleFile);
    }

    public static @NotNull ConsoleManager ensureConsole(@NotNull Project project,
                                                        @NotNull String consoleName,
                                                        @NotNull VirtualFile consoleFile) {
        ConsoleManager consoleManager = UserInput.getInstance().getConsole(project, consoleName);
        if (consoleManager == null) {
            consoleManager = new ConsoleManager(project, consoleName, consoleFile);
            UserInput.getInstance().putConsole(project, consoleName, consoleManager);
        } else if (!consoleFile.equals(consoleManager.getConsoleFile())) {
            consoleManager.setConsoleFile(consoleFile);
        }
        return consoleManager;
    }

    public static @NotNull List<ConsoleManager> loadProjectConsoles(@NotNull Project project) {
        List<ConsoleManager> consoles = new ArrayList<>();
        Path consoleDirectory;
        try {
            consoleDirectory = getConsoleDirectory(project);
            Files.createDirectories(consoleDirectory);
        } catch (IllegalStateException | IOException ignored) {
            return consoles;
        }

        try (var paths = Files.list(consoleDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(ConsoleFileManager::isMqlFile)
                    .sorted(Comparator.comparing(path -> toConsoleName(path.getFileName().toString()), String.CASE_INSENSITIVE_ORDER))
                    .forEach(path -> {
                        VirtualFile virtualFile = refreshAndFindFile(path);
                        if (virtualFile != null) {
                            consoles.add(ensureConsole(project, toConsoleName(path.getFileName().toString()), virtualFile));
                        }
                    });
        } catch (IOException ignored) {
            return consoles;
        }
        return consoles;
    }

    public static void openConsole(@NotNull Project project, @NotNull ConsoleManager consoleManager) {
        FileEditorManager.getInstance(project).openFile(consoleManager.getConsoleFile(), true);
    }

    public static @NotNull String renameConsole(@NotNull Project project,
                                                @NotNull ConsoleManager consoleManager,
                                                @NotNull String newConsoleName) {
        String normalizedName = normalizeConsoleName(newConsoleName);
        String oldConsoleName = consoleManager.getConsoleName();
        if (oldConsoleName.equals(normalizedName)) {
            return oldConsoleName;
        }

        Map<String, ConsoleManager> consoleMap = UserInput.getInstance().mqlConsole.computeIfAbsent(project, key -> new java.util.concurrent.ConcurrentHashMap<>());
        ConsoleManager existedConsole = consoleMap.get(normalizedName);
        if (existedConsole != null && existedConsole != consoleManager) {
            throw new IllegalStateException("Console already exists: " + normalizedName);
        }

        Path targetFile = getConsoleDirectory(project).resolve(toFileName(normalizedName));
        if (Files.exists(targetFile)) {
            throw new IllegalStateException("Console file already exists: " + targetFile.getFileName());
        }

        VirtualFile consoleFile = consoleManager.getConsoleFile();
        if (consoleFile == null || !consoleFile.isValid()) {
            throw new IllegalStateException("Console file is unavailable.");
        }
        var document = FileDocumentManager.getInstance().getDocument(consoleFile);
        if (document != null) {
            FileDocumentManager.getInstance().saveDocument(document);
        }

        try {
            ApplicationManager.getApplication().runWriteAction(() -> {
                try {
                    consoleFile.rename(ConsoleFileManager.class, toFileName(normalizedName));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw new IllegalStateException("Rename console file failed: " + e.getCause().getMessage(), e.getCause());
        }

        consoleManager.setConsoleName(normalizedName);
        consoleManager.setConsoleFile(consoleFile);
        consoleMap.remove(oldConsoleName);
        consoleMap.put(normalizedName, consoleManager);
        return normalizedName;
    }

    public static @NotNull String getConsoleName(@NotNull Project project, @NotNull VirtualFile file) {
        if (isManagedConsoleFile(project, file)) {
            return toConsoleName(file.getName());
        }
        return file.getName();
    }

    public static boolean isManagedConsoleFile(@NotNull Project project, @NotNull VirtualFile file) {
        if (!file.isInLocalFileSystem()) {
            return false;
        }
        try {
            Path filePath = Path.of(file.getPath()).normalize();
            return filePath.startsWith(getConsoleDirectory(project).normalize()) && isMqlFile(filePath);
        } catch (IllegalStateException ignored) {
            return false;
        }
    }

    private static int nextConsoleIndex(@NotNull Project project) {
        int maxIndex = 0;
        Map<String, ConsoleManager> consoleMap = UserInput.getInstance().mqlConsole.get(project);
        if (consoleMap != null) {
            OptionalInt mapMax = consoleMap.keySet().stream()
                    .mapToInt(ConsoleFileManager::extractConsoleIndex)
                    .filter(index -> index > 0)
                    .max();
            maxIndex = Math.max(maxIndex, mapMax.orElse(0));
        }
        Path consoleDirectory = getConsoleDirectory(project);
        if (Files.isDirectory(consoleDirectory)) {
            try (var paths = Files.list(consoleDirectory)) {
                OptionalInt fileMax = paths.filter(Files::isRegularFile)
                        .filter(ConsoleFileManager::isMqlFile)
                        .map(path -> toConsoleName(path.getFileName().toString()))
                        .mapToInt(ConsoleFileManager::extractConsoleIndex)
                        .filter(index -> index > 0)
                        .max();
                maxIndex = Math.max(maxIndex, fileMax.orElse(0));
            } catch (IOException ignored) {
                return maxIndex + 1;
            }
        }
        return maxIndex + 1;
    }

    private static @NotNull VirtualFile ensureConsoleFile(@NotNull Project project, @NotNull String consoleName) {
        Path consoleDirectory = getConsoleDirectory(project);
        Path consoleFile = consoleDirectory.resolve(toFileName(consoleName));
        try {
            Files.createDirectories(consoleDirectory);
            if (Files.notExists(consoleFile)) {
                Files.createFile(consoleFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Create console file failed: " + consoleFile, e);
        }

        VirtualFile virtualFile = refreshAndFindFile(consoleFile);
        if (virtualFile == null) {
            throw new IllegalStateException("Console file is unavailable: " + consoleFile);
        }
        virtualFile.setCharset(StandardCharsets.UTF_8);
        return virtualFile;
    }

    private static @NotNull Path getConsoleDirectory(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalStateException("Project base path is unavailable.");
        }
        return Path.of(basePath, ".idea", PLUGIN_DIRECTORY, CONSOLES_DIRECTORY);
    }

    private static VirtualFile refreshAndFindFile(@NotNull Path path) {
        return LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
    }

    private static boolean isMqlFile(@NotNull Path path) {
        return path.getFileName().toString().endsWith(MQL_EXTENSION);
    }

    private static @NotNull String toFileName(@NotNull String consoleName) {
        return consoleName.endsWith(MQL_EXTENSION) ? consoleName : consoleName + MQL_EXTENSION;
    }

    private static @NotNull String toConsoleName(@NotNull String fileName) {
        return fileName.endsWith(MQL_EXTENSION)
                ? fileName.substring(0, fileName.length() - MQL_EXTENSION.length())
                : fileName;
    }

    private static int extractConsoleIndex(@NotNull String consoleName) {
        Matcher matcher = NEW_CONSOLE_PATTERN.matcher(consoleName);
        if (!matcher.matches()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static @NotNull String normalizeConsoleName(@NotNull String consoleName) {
        String normalizedName = consoleName.trim();
        if (normalizedName.endsWith(MQL_EXTENSION)) {
            normalizedName = normalizedName.substring(0, normalizedName.length() - MQL_EXTENSION.length()).trim();
        }
        if (normalizedName.isEmpty()) {
            throw new IllegalStateException("Console name must not be empty.");
        }
        if (".".equals(normalizedName) || "..".equals(normalizedName) || INVALID_FILE_NAME_CHARS.matcher(normalizedName).find()) {
            throw new IllegalStateException("Console name contains invalid file name characters.");
        }
        return normalizedName;
    }
}
