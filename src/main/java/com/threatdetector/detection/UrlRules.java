package com.threatdetector.detection;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;

/** Ten local rules inspecting URL structure. */
public final class UrlRules {
    private UrlRules() {
    }

    public static List<Rule> create() {
        List<Rule> rules = new ArrayList<>();
        rules.add(rule("url.no_https", "Uses plain HTTP", 20, UrlRules::noHttps));
        rules.add(rule("url.ip_host", "Host is an IP address", 25, UrlRules::ipHost));
        rules.add(rule("url.userinfo", "URL contains credentials", 15, UrlRules::userinfo));
        rules.add(rule("url.punycode", "Host uses punycode", 15, UrlRules::punycode));
        rules.add(rule("url.many_subdomains", "Host has many subdomains", 10, UrlRules::manySubdomains));
        rules.add(rule("url.suspicious_tld", "Host uses a suspicious TLD", 15, UrlRules::suspiciousTld));
        rules.add(rule("url.shortener", "URL uses a link shortener", 10, UrlRules::shortener));
        rules.add(rule("url.nonstandard_port", "URL uses a nonstandard port", 10, UrlRules::nonstandardPort));
        rules.add(rule("url.long_url", "URL is unusually long", 10, UrlRules::longUrl));
        rules.add(rule("url.bait_keywords", "URL contains bait keywords", 10, UrlRules::baitKeywords));
        return List.copyOf(rules);
    }

    private static Rule rule(String id, String title, int points,
                             BiFunction<URI, String, List<String>> check) {
        return input -> {
            if (input instanceof RuleInput.Url url) {
                return Optional.ofNullable(check.apply(url.uri(), url.raw()))
                        .map(evidence -> new RuleHit(id, title, points, evidence));
            }
            return Optional.empty();
        };
    }

    private static List<String> noHttps(URI url, String raw) {
        return "https".equalsIgnoreCase(url.getScheme()) ? null : List.of("scheme=" + url.getScheme());
    }

    private static List<String> ipHost(URI url, String raw) {
        String host = url.getHost();
        if (host == null) return null;
        String value = host.replace("[", "").replace("]", "");
        if (value.matches("\\d{1,3}(\\.\\d{1,3}){3}")) return List.of("host=" + host);
        if (value.contains(":") && value.matches("[0-9a-fA-F:.]+")) return List.of("host=" + host);
        return null;
    }

    private static List<String> userinfo(URI url, String raw) {
        String info = url.getRawUserInfo();
        return info != null && !info.isEmpty() ? List.of("userinfo present") : null;
    }

    private static List<String> punycode(URI url, String raw) {
        String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
        boolean hit = host.contains("xn--") || raw.toLowerCase(Locale.ROOT).contains("xn--");
        return hit ? List.of("host=" + host) : null;
    }

    private static List<String> manySubdomains(URI url, String raw) {
        String host = url.getHost();
        if (host == null) return null;
        String trimmed = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
        int levels = Math.max(0, trimmed.split("\\.", -1).length - 2);
        return levels >= 4 ? List.of("subdomain_levels=" + levels) : null;
    }

    private static List<String> suspiciousTld(URI url, String raw) {
        List<String> tlds = List.of("zip", "mov", "xyz", "top", "tk", "gq", "cf", "click");
        String host = url.getHost();
        if (host == null) return null;
        String trimmed = host.endsWith(".") ? host.substring(0, host.length() - 1) : host;
        int dot = trimmed.lastIndexOf('.');
        if (dot < 0) return null;
        String tld = trimmed.substring(dot + 1).toLowerCase(Locale.ROOT);
        return tlds.contains(tld) ? List.of("tld=" + tld) : null;
    }

    private static List<String> shortener(URI url, String raw) {
        List<String> shorteners = List.of("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly",
                "is.gd", "buff.ly", "cutt.ly", "rb.gy");
        String host = url.getHost();
        if (host == null) return null;
        String value = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        return shorteners.contains(value) ? List.of("host=" + value) : null;
    }

    private static List<String> nonstandardPort(URI url, String raw) {
        int port = url.getPort();
        return port != -1 && port != 80 && port != 443 ? List.of("port=" + port) : null;
    }

    private static List<String> longUrl(URI url, String raw) {
        int length = (int) raw.chars().count();
        return length > 120 ? List.of("length=" + length) : null;
    }

    private static List<String> baitKeywords(URI url, String raw) {
        List<String> bait = List.of("login", "verify", "secure", "account", "update", "wallet", "prize", "free");
        String host = url.getHost() == null ? "" : url.getHost();
        String haystack = (host + " " + url.getPath()).toLowerCase(Locale.ROOT);
        for (String keyword : bait) {
            if (haystack.contains(keyword)) return List.of("matched=" + keyword);
        }
        return null;
    }
}
