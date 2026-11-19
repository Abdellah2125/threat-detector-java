package com.threatdetector.detection;

import java.util.List;

/** Human-readable catalog used by the "How it works" screen. */
public final class RuleCatalog {
    private RuleCatalog() {
    }

    public record RuleSpec(String id, String title, int points, String signal) {
    }

    public static final List<RuleSpec> URL_RULES = List.of(
            new RuleSpec("url.no_https", "Uses plain HTTP", 20, "The scheme is not https."),
            new RuleSpec("url.ip_host", "Host is an IP address", 25, "A literal IPv4 or IPv6 host."),
            new RuleSpec("url.userinfo", "URL contains credentials", 15, "A username/password part is embedded."),
            new RuleSpec("url.punycode", "Host uses punycode", 15, "The host contains xn-- and can spoof a brand."),
            new RuleSpec("url.many_subdomains", "Host has many subdomains", 10, "Four or more subdomain levels."),
            new RuleSpec("url.suspicious_tld", "Suspicious TLD", 15, "zip, mov, xyz, top, tk, gq, cf or click."),
            new RuleSpec("url.shortener", "Link shortener", 10, "A known shortener host hides the destination."),
            new RuleSpec("url.nonstandard_port", "Nonstandard port", 10, "A port other than 80 or 443."),
            new RuleSpec("url.long_url", "Unusually long URL", 10, "More than 120 characters."),
            new RuleSpec("url.bait_keywords", "Bait keywords", 10, "login, verify, secure, account, update, wallet, prize, free."));

    public static final List<RuleSpec> MESSAGE_RULES = List.of(
            new RuleSpec("msg.urgency", "Urgency language", 20, "urgent, immediately, act now, final notice."),
            new RuleSpec("msg.credential_request", "Credential request", 30, "An action verb paired with a secret."),
            new RuleSpec("msg.financial_bait", "Financial bait", 20, "prize, lottery, gift card, crypto, refund."),
            new RuleSpec("msg.threat_pressure", "Threat or pressure", 20, "suspended, legal action, police, locked."),
            new RuleSpec("msg.shortened_link", "Shortened link", 15, "The text contains a known shortener."),
            new RuleSpec("msg.callback_request", "Off-platform callback", 10, "WhatsApp, Telegram or a phone number."),
            new RuleSpec("msg.all_caps", "Mostly uppercase", 5, "More than half of letters are uppercase."));

    public static int totalRules() {
        return URL_RULES.size() + MESSAGE_RULES.size();
    }
}
