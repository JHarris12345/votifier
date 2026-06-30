package com.vexsoftware.votifier.util;

import com.vexsoftware.votifier.model.Vote;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Appends every received vote to a monthly log file inside a {@code logs}
 * directory located in the server's working directory (the root).
 * <p>
 * Each month gets its own file named {@code [yyyy]-[MM]} (for example
 * {@code 2026-06}), and every vote is appended as a single line.
 *
 * @author Claude
 */
public final class VoteLogger {

    /**
     * The directory, relative to the working directory (root), that holds the
     * monthly vote log files.
     */
    private static final File LOG_DIR = new File("logs");

    /**
     * Names the monthly file, e.g. {@code 2026-06}.
     */
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Timestamp prefix for each logged line.
     */
    private static final DateTimeFormatter LINE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private VoteLogger() {
    }

    /**
     * Logs a single vote to the current month's log file.
     *
     * @param vote          the vote that was received
     * @param remoteAddress the address of the connection that delivered the
     *                      vote (the vote service's server), may be {@code null}
     */
    public static synchronized void log(Vote vote, String remoteAddress) {
        if (vote == null) {
            return;
        }

        try {
            if (!LOG_DIR.isDirectory() && !LOG_DIR.mkdirs()) {
                return;
            }

            ZonedDateTime now = ZonedDateTime.now(TimeZone.getTimeZone("Europe/London").toZoneId());
            File logFile = new File(LOG_DIR, now.format(FILE_FORMAT) + ".log");

            // The voter's IP as reported by the voting service. The actual TCP
            // connection originates from the voting service's server, so this
            // (vote.getAddress()) is the closest we can get to the real voter's
            // IP. We also record the delivering server's address for reference.
            String voterAddress = vote.getAddress() == null ? "unknown" : vote.getAddress();
            String sender = remoteAddress == null ? "unknown" : remoteAddress;

            String line = now.format(LINE_FORMAT)
                    + " | service=" + vote.getServiceName()
                    + " | username=" + vote.getUsername()
                    + " | voterIP=" + voterAddress
                    + " | receivedFrom=" + sender
                    + " | voteTimestamp=" + vote.getTimeStamp()
                    + System.lineSeparator();

            Files.write(logFile.toPath(), line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ex) {
            // Logging must never interfere with vote processing; swallow errors.
        }
    }
}
