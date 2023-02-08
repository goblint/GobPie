package util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;

/**
 * The Class FileWatcher.
 * <p>
 * A utility class for watching files for changes.
 *
 * @author Juhan Oskar Hennoste
 * @since 0.0.3
 */

public class FileWatcher implements AutoCloseable {

    private final WatchService watchService;
    private final Path relativeTargetFile;

    private final Logger log = LogManager.getLogger(FileWatcher.class);


    public FileWatcher(Path targetFile) {
        Path normalizedTargetFile = targetFile.toAbsolutePath().normalize();
        Path targetFileFolder = normalizedTargetFile.getParent();

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            targetFileFolder.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
            this.relativeTargetFile = targetFileFolder.relativize(normalizedTargetFile);
        } catch (IOException e) {
            // Reasonably certain this should not happen unless your setup is very broken
            throw new UncheckedIOException("Failed to create file watcher for " + normalizedTargetFile, e);
        }
    }


    /**
     * Waits until the target file is modified (creating or deleting the file also counts as a modification)
     */
    public void waitForModified() throws InterruptedException {
        // Wait for event corresponding to the desired file
        boolean modifiedEventFound = false;
        while (!modifiedEventFound) {
            WatchKey key = watchService.take();
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.context().equals(relativeTargetFile)) {
                    modifiedEventFound = true;
                }
            }
            key.reset();
        }

        // Short delay to ensure that all events caused by the same operation are consumed
        // See https://stackoverflow.com/questions/16777869/java-7-watchservice-ignoring-multiple-occurrences-of-the-same-event
        // TODO: The required delay can apparently be several seconds. An alternative solution without long delays would be better
        Thread.sleep(50);

        // Consume all available events so next call to this method does not get old events
        WatchKey key;
        while ((key = watchService.poll()) != null) {
            key.pollEvents();
            key.reset();
        }
    }

    /**
     * Checks if the target file has been modified since the last call to {@link #checkModified()} or {@link #waitForModified()}.
     *
     * @return True if the file has been modified since the last check
     */
    public boolean checkModified() {
        boolean modifiedEventFound = false;
        WatchKey key;
        while ((key = watchService.poll()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.context().equals(relativeTargetFile)) {
                    modifiedEventFound = true;
                }
            }
            key.reset();
        }
        return modifiedEventFound;
    }

    @Override
    public void close() {
        try {
            watchService.close();
        } catch (IOException e) {
            // Reasonably certain this should not happen unless your setup is very broken
            throw new UncheckedIOException("Failed to close file watcher", e);
        }
    }

}