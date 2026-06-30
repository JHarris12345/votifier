package com.vexsoftware.votifier.util;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Helpers for turning the raw addresses Netty hands us into clean,
 * human-readable IP strings.
 * <p>
 * Votifier's listener socket is typically dual-stack, so an incoming IPv4
 * connection is often presented as an IPv4-mapped IPv6 address
 * ({@code ::ffff:1.2.3.4}) and {@link SocketAddress#toString()} adds a leading
 * slash and a trailing port ({@code /::ffff:1.2.3.4:8192}). That mapped form is
 * the <em>same</em> address as the plain IPv4 {@code 1.2.3.4}; this class
 * normalizes it so the logged value matches the dotted-decimal IPv4 other
 * systems (such as the player database) record.
 *
 * @author Claude
 */
public final class AddressUtil {

    /** Prefix marking an IPv4-mapped IPv6 address, e.g. {@code ::ffff:1.2.3.4}. */
    private static final String V4_MAPPED_PREFIX = "::ffff:";

    private AddressUtil() {
    }

    /**
     * Extracts a clean IP string from a socket address, dropping the port and
     * collapsing IPv4-mapped IPv6 addresses to plain IPv4.
     *
     * @param address the socket address, may be {@code null}
     * @return the normalized IP, or {@code null} if {@code address} is null
     */
    public static String formatSocketAddress(SocketAddress address) {
        if (address == null) {
            return null;
        }
        if (address instanceof InetSocketAddress) {
            InetAddress inet = ((InetSocketAddress) address).getAddress();
            if (inet != null) {
                return normalize(inet.getHostAddress());
            }
        }
        return normalize(address.toString());
    }

    /**
     * Normalizes a raw IP string: strips any IPv6 zone identifier and collapses
     * an IPv4-mapped IPv6 address ({@code ::ffff:1.2.3.4}) to plain IPv4.
     *
     * @param ip the raw IP string, may be {@code null}
     * @return the normalized IP, or the input unchanged if it is null/not mapped
     */
    public static String normalize(String ip) {
        if (ip == null) {
            return null;
        }

        // Drop an IPv6 zone/scope id (e.g. "fe80::1%eth0").
        int zone = ip.indexOf('%');
        if (zone != -1) {
            ip = ip.substring(0, zone);
        }

        // Collapse an IPv4-mapped IPv6 address to its dotted-decimal IPv4 form.
        if (ip.length() > V4_MAPPED_PREFIX.length()
                && ip.regionMatches(true, 0, V4_MAPPED_PREFIX, 0, V4_MAPPED_PREFIX.length())
                && ip.indexOf('.') != -1) {
            ip = ip.substring(V4_MAPPED_PREFIX.length());
        }

        return ip;
    }
}
