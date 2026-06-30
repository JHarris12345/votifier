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
     * <p>
     * Three distinct address values are recorded side-by-side so the source of
     * each can be judged independently:
     * <ul>
     *     <li>{@code voterIP} &ndash; the address the voting <em>website</em>
     *     placed in the vote payload ({@link Vote#getAddress()}). This is logged
     *     <strong>exactly</strong> as received; it is whatever the site chose to
     *     send and is frequently a placeholder ({@code 127.0.0.1}), the server's
     *     own address, or missing entirely. Votifier never measures this.</li>
     *     <li>{@code receivedFrom} &ndash; the cleaned IP of the TCP peer that
     *     actually connected to Votifier (mapped {@code ::ffff:} addresses
     *     collapsed, port dropped). Behind a proxy/forwarder this is the
     *     proxy's or localhost's address, not the voter's.</li>
     *     <li>{@code rawAddress} &ndash; the unmodified socket string for that
     *     same connection, preserving the port and any IPv6-mapped form, for
     *     diagnostics.</li>
     * </ul>
     *
     * @param vote             the vote that was received
     * @param remoteAddress    the cleaned address of the connection that
     *                         delivered the vote, may be {@code null}
     * @param rawRemoteAddress the raw, unmodified socket string for the same
     *                         connection, may be {@code null}
     */
    public static synchronized void log(Vote vote, String remoteAddress, String rawRemoteAddress) {
        if (vote == null) {
            return;
        }

        try {
            if (!LOG_DIR.isDirectory() && !LOG_DIR.mkdirs()) {
                return;
            }

            ZonedDateTime now = ZonedDateTime.now(TimeZone.getTimeZone("Europe/London").toZoneId());
            File logFile = new File(LOG_DIR, now.format(FILE_FORMAT) + ".log");

            // voterIP is logged verbatim (whatever the voting site sent);
            // receivedFrom/rawAddress describe the actual TCP connection.
            String voterAddress = vote.getAddress() == null ? "unknown" : vote.getAddress();
            String sender = remoteAddress == null ? "unknown" : remoteAddress;
            String rawSender = rawRemoteAddress == null ? "unknown" : rawRemoteAddress;

            String line = now.format(LINE_FORMAT)
                    + " | service=" + vote.getServiceName()
                    + " | username=" + vote.getUsername()
                    + " | voterIP=" + voterAddress
                    + " | receivedFrom=" + sender
                    + " | rawAddress=" + rawSender
                    + " | voteTimestamp=" + vote.getTimeStamp()
                    + System.lineSeparator();

            Files.write(logFile.toPath(), line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ex) {
            // Logging must never interfere with vote processing; swallow errors.
        }
    }
}
