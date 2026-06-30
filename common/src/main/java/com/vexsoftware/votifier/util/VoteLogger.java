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

/**
 * Appends every received vote to a monthly YAML log file inside a {@code logs}
 * directory located in the server's working directory (the root).
 * <p>
 * Each month gets its own file named {@code [yyyy]-[MM].yml} (for example
 * {@code 2026-06.yml}), and every vote is appended as a YAML list entry so the
 * whole file remains valid, parseable YAML.
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
     * Names the monthly file, e.g. {@code 2026-06} (the {@code .yml} extension
     * is appended separately).
     */
    private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Human-readable timestamp recorded for each vote.
     */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    private VoteLogger() {
    }

    /**
     * Logs a single vote to the current month's YAML log file.
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

            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            File logFile = new File(LOG_DIR, now.format(FILE_FORMAT) + ".yml");

            // The voter's IP as reported by the voting service. The actual TCP
            // connection originates from the voting service's server, so this
            // (vote.getAddress()) is the closest we can get to the real voter's
            // IP. We also record the delivering server's address for reference.
            String voterAddress = vote.getAddress() == null ? "unknown" : vote.getAddress();
            String sender = remoteAddress == null ? "unknown" : remoteAddress;

            String nl = System.lineSeparator();
            String entry = "- timestamp: " + yaml(now.format(TIME_FORMAT)) + nl
                    + "  service: " + yaml(vote.getServiceName()) + nl
                    + "  username: " + yaml(vote.getUsername()) + nl
                    + "  voterIP: " + yaml(voterAddress) + nl
                    + "  receivedFrom: " + yaml(sender) + nl
                    + "  voteTimestamp: " + yaml(vote.getTimeStamp()) + nl;

            Files.write(logFile.toPath(), entry.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ex) {
            // Logging must never interfere with vote processing; swallow errors.
        }
    }

    /**
     * Renders a value as a double-quoted YAML scalar, escaping the characters
     * that would otherwise break a quoted string. This keeps usernames and
     * service names safe even if they contain colons, quotes, or backslashes.
     */
    private static String yaml(String value) {
        if (value == null) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder(value.length() + 2);
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
