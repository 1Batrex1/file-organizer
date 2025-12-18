package bartek.fileorganizer.core;

import bartek.fileorganizer.model.AppConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;

@Slf4j
public class DirectoryWatcher implements Runnable {

    private final AppConfig config;
    private final FileProcessor fileProcessor;
    private final Path directoryPath;


    public DirectoryWatcher(AppConfig config, Consumer<String> uiCallback) {
        this.config = config;
        this.directoryPath = Paths.get(config.sourceDirectory());
        this.fileProcessor = new FileProcessor(config, uiCallback);
    }


    @Override
    public void run() {

        Path path = Paths.get(config.sourceDirectory());

        if (!Files.exists(path)) {
            log.error("Folder doesn't exists: {}", path);
            return;
        }

        log.info("Starting to watch directory: {}", path);

        try(WatchService watchService = FileSystems.getDefault().newWatchService())
        {
            path.register(watchService,StandardWatchEventKinds.ENTRY_CREATE);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;

                try{
                    key = watchService.take();
                }
                catch (InterruptedException exception)
                {
                    log.info("Directory watcher interrupted, stopping.");
                    return;
                }

                for(WatchEvent<?> event : key.pollEvents())
                {
                    WatchEvent.Kind<?> kind = event.kind();

                    if(kind == StandardWatchEventKinds.OVERFLOW)
                    {
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();

                    log.info("New file detected: {}", fileName);

                    Path fullPath = directoryPath.resolve(fileName);
                    fileProcessor.processFile(fullPath);

                    boolean valid = key.reset();
                    if (!valid) {
                        log.warn("WatchKey no longer valid, stopping watcher.");
                        break;
                    }
                }
            }
        }
        catch (IOException exception)
        {
            log.error("Critical error in WatchService", exception);        }

    }
}
