package bartek.fileorganizer.core;

import bartek.fileorganizer.model.AppConfig;
import bartek.fileorganizer.model.Rule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class FileProcessor {

    private final AppConfig config;
    private final Consumer<String> uiCallback;

    public void processFile(Path filePath) {
        String fileName = filePath.getFileName().toString();

        Optional<Rule> matchingRule = config.rules().stream()
                .filter(rule -> rule.matches(fileName))
                .findFirst();

        if (matchingRule.isEmpty()) {
            log.info("No matching rule found for {}", fileName);
            return;
        }

        Rule rule = matchingRule.get();
        log.info("Found matching rule for {}: move to {}", fileName, rule.targetFolder());

        try {
            if (waitForFileLock(filePath)) {
                moveFile(filePath, rule.targetFolder());
            } else {
                log.error("Could not acquire lock for file: {}", fileName);
            }
        } catch (IOException e) {
            log.error("Error processing file {}: {}", fileName, e.getMessage());
        } catch (InterruptedException e) {
            log.error("Unable to get access to lock file {}: {}", fileName, e.getMessage());
        }
    }

    private void moveFile(Path source, String targetFolder) throws IOException {
        Path sourceDir = Paths.get(config.sourceDirectory());
        Path targetDir = sourceDir.resolve(targetFolder);

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path targetFile = targetDir.resolve(source.getFileName());

        int counter = 1;

        if(Files.exists(targetFile)) {
            while (Files.exists(targetFile)) {
                String fileName = source.getFileName().toString();

                String extension = "";
                String nameWithoutExtension = fileName.substring(fileName.lastIndexOf('.'));
                if (fileName.contains(".")) {
                    extension = fileName.substring(fileName.lastIndexOf('.'));
                    nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
                }
                targetFile = targetDir.resolve(nameWithoutExtension + " (" + counter + ")" + extension);
                counter++;
            }
        }
        Files.move(source, targetFile);

        log.info("Moved {} to {}", source, targetFile);

        uiCallback.accept("Moved " + source.getFileName() + " to " + targetFolder);
    }

    private boolean waitForFileLock(Path path) throws InterruptedException {
        int maxAttempts = 10;
        int sleepTime = 1000;
        for (int i = 0; i < maxAttempts; i++) {
            try (FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE);
                 FileLock lock = channel.tryLock()) {

                if (lock != null) {
                    return true;
                }
            } catch (IOException _) {
            }

            log.debug("File is locked waiting... ({}/{})", i + 1, maxAttempts);
            Thread.sleep(sleepTime);
        }
        return false;
    }

    public void scanExistingFiles() {
        Path sourceDir = Paths.get(config.sourceDirectory());

        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            log.error("Source directory does not exist or is not a directory: {}", sourceDir);
            return;
        }

        log.info("Scanning existing files");
        uiCallback.accept("Scanning existing files");
        try (Stream<Path> stream = Files.list(sourceDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(this::processFile);
        } catch (IOException e) {
            log.error("Error scanning existing files: {}", e.getMessage());
            uiCallback.accept("Error scanning existing files: " + e.getMessage());
        }
    }
}
