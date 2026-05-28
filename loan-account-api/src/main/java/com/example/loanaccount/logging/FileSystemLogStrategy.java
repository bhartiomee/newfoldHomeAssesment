package com.example.loanaccount.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

public class FileSystemLogStrategy implements LogStrategy {
    private final Path logDirectory;
    private final Clock clock;
    private final Object writeLock = new Object();

    public FileSystemLogStrategy(Path logDirectory, Clock clock) {
        this.logDirectory = logDirectory;
        this.clock = clock;
        try {
            Files.createDirectories(logDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create log directory: " + logDirectory, exception);
        }
    }

    @Override
    public String name() {
        return "file";
    }

    @Override
    public void log(AuditLogEntry entry) throws IOException {
        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        Path logFile = logDirectory.resolve("api-requests-" + today + ".log");
        byte[] bytes = (entry.toJson() + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        synchronized (writeLock) {
            try (FileChannel channel = FileChannel.open(
                    logFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            )) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
            }
        }
    }
}
