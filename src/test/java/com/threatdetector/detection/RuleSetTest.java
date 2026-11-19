package com.threatdetector.detection;

import com.threatdetector.model.Finding;
import com.threatdetector.model.RiskLevel;
import com.threatdetector.model.ScanItem;
import com.threatdetector.model.ScanOutcome;
import com.threatdetector.model.TargetKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleSetTest {
    private final RuleSet rules = new RuleSet();

    private ScanOutcome scan(TargetKind kind, String target) {
        return rules.evaluate(new ScanItem(0, null, kind, target));
    }

    private ScanOutcome.Scored scored(TargetKind kind, String target) {
        ScanOutcome outcome = scan(kind, target);
        assertInstanceOf(ScanOutcome.Scored.class, outcome, "expected scored outcome, got " + outcome);
        return (ScanOutcome.Scored) outcome;
    }

    private List<String> ids(ScanOutcome.Scored outcome) {
        return outcome.findings().stream().map(Finding::ruleId).toList();
    }

    @Test
    void clean_https_url_is_low_with_no_findings() {
        ScanOutcome.Scored result = scored(TargetKind.URL, "https://example.com");
        assertEquals(0, result.score());
        assertEquals(RiskLevel.LOW, result.risk());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void http_ip_login_url_is_medium() {
        ScanOutcome.Scored result = scored(TargetKind.URL, "http://192.168.1.10/login");
        assertEquals(55, result.score());
        assertEquals(RiskLevel.MEDIUM, result.risk());
        assertTrue(ids(result).containsAll(List.of("url.no_https", "url.ip_host", "url.bait_keywords")));
    }

    @Test
    void punycode_suspicious_tld_url_is_high() {
        ScanOutcome.Scored result = scored(TargetKind.URL, "http://xn--bcher-kva.tk/login");
        assertEquals(60, result.score());
        assertEquals(RiskLevel.HIGH, result.risk());
    }

    @Test
    void shortener_scores_ten() {
        ScanOutcome.Scored result = scored(TargetKind.URL, "https://bit.ly/abc");
        assertEquals(10, result.score());
        assertEquals("url.shortener", result.findings().get(0).ruleId());
    }

    @Test
    void urgent_phishing_message_is_high() {
        ScanOutcome.Scored result = scored(TargetKind.MESSAGE,
                "URGENT: your account is locked. Verify your password within 24 hours or lose access: http://bit.ly/x9f2");
        assertEquals(85, result.score());
        assertEquals(RiskLevel.HIGH, result.risk());
        assertTrue(ids(result).containsAll(List.of("msg.urgency", "msg.credential_request",
                "msg.threat_pressure", "msg.shortened_link")));
    }

    @Test
    void normal_otp_notification_is_not_flagged_as_credential_request() {
        ScanOutcome.Scored result = scored(TargetKind.MESSAGE, "Your verification code is 123456");
        assertEquals(0, result.score());
        assertTrue(result.findings().isEmpty());
    }

    @Test
    void clean_message_is_low() {
        assertEquals(0, scored(TargetKind.MESSAGE, "Hey, are we still meeting at 5?").score());
    }

    @Test
    void malformed_url_is_not_verified() {
        assertInstanceOf(ScanOutcome.NotVerified.class, scan(TargetKind.URL, "http://[bad"));
    }

    @Test
    void unsupported_scheme_is_not_verified() {
        ScanOutcome outcome = scan(TargetKind.URL, "ftp://example.com");
        assertEquals("INVALID_URL", ((ScanOutcome.NotVerified) outcome).code());
    }

    @Test
    void url_without_scheme_is_normalized_and_scanned() {
        assertEquals(0, scored(TargetKind.URL, "example.com").score());
    }

    @Test
    void empty_input_is_not_verified() {
        assertEquals("EMPTY_INPUT", ((ScanOutcome.NotVerified) scan(TargetKind.MESSAGE, "   ")).code());
    }

    @Test
    void oversized_message_is_not_verified() {
        String message = "a".repeat(RuleSet.MAX_MESSAGE_CHARS + 1);
        assertEquals("INPUT_TOO_LONG", ((ScanOutcome.NotVerified) scan(TargetKind.MESSAGE, message)).code());
    }

    @Test
    void oversized_url_is_not_verified() {
        String url = "https://example.com/" + "a".repeat(RuleSet.MAX_URL_CHARS);
        assertEquals("INPUT_TOO_LONG", ((ScanOutcome.NotVerified) scan(TargetKind.URL, url)).code());
    }

    @Test
    void not_verified_is_never_low() {
        ScanOutcome outcome = scan(TargetKind.URL, "not a url with spaces");
        assertEquals("INVALID_URL", ((ScanOutcome.NotVerified) outcome).code());
    }

    @Test
    void each_url_rule_fires_on_its_representative_input() {
        String longUrl = "https://example.com/" + "a".repeat(120);
        String[][] cases = {
                {"http://example.com", "url.no_https"},
                {"https://192.168.1.10", "url.ip_host"},
                {"https://user:pass@example.com", "url.userinfo"},
                {"https://xn--bcher-kva.example.com", "url.punycode"},
                {"https://a.b.c.d.example.com", "url.many_subdomains"},
                {"https://example.tk", "url.suspicious_tld"},
                {"https://bit.ly/x", "url.shortener"},
                {"https://example.com:8080", "url.nonstandard_port"},
                {longUrl, "url.long_url"},
                {"https://example.com/login", "url.bait_keywords"}
        };
        for (String[] testCase : cases) {
            List<String> fired = ids(scored(TargetKind.URL, testCase[0]));
            assertTrue(fired.contains(testCase[1]), testCase[0] + " should trigger " + testCase[1] + ", got " + fired);
        }
    }

    @Test
    void each_message_rule_fires_on_its_representative_input() {
        String[][] cases = {
                {"Urgent: act now", "msg.urgency"},
                {"Verify your password now", "msg.credential_request"},
                {"You won a prize", "msg.financial_bait"},
                {"Your account is suspended", "msg.threat_pressure"},
                {"go to bit.ly/x", "msg.shortened_link"},
                {"Call me on WhatsApp", "msg.callback_request"},
                {"THIS IS A VERY LONG MESSAGE WRITTEN IN CAPS", "msg.all_caps"}
        };
        for (String[] testCase : cases) {
            List<String> fired = ids(scored(TargetKind.MESSAGE, testCase[0]));
            assertTrue(fired.contains(testCase[1]), testCase[0] + " should trigger " + testCase[1] + ", got " + fired);
        }
    }

    @Test
    void userinfo_rule_reports_the_embedded_credentials() {
        ScanOutcome.Scored result = scored(TargetKind.URL, "https://user:pass@example.com/account");
        assertTrue(ids(result).contains("url.userinfo"));
        assertTrue(result.findings().stream()
                .filter(f -> f.ruleId().equals("url.userinfo"))
                .anyMatch(f -> !f.evidence().isEmpty()));
    }

    @Test
    void many_subdomains_counts_only_subdomain_levels() {
        assertEquals(10, scored(TargetKind.URL, "https://a.b.c.d.example.com").score());
        assertEquals(0, scored(TargetKind.URL, "https://www.example.com").score());
    }

    @Test
    void long_url_threshold_is_strictly_above_120() {
        assertEquals(0, scored(TargetKind.URL, "https://example.com/" + "a".repeat(100)).score());
        assertEquals(10, scored(TargetKind.URL, "https://example.com/" + "a".repeat(120)).score());
    }

    @Test
    void nonstandard_port_rule_ignores_80_and_443() {
        assertTrue(ids(scored(TargetKind.URL, "https://example.com:8080")).contains("url.nonstandard_port"));
        assertFalse(ids(scored(TargetKind.URL, "https://example.com:443")).contains("url.nonstandard_port"));
    }

    @Test
    void suspicious_tld_rule_uses_the_last_label() {
        assertTrue(ids(scored(TargetKind.URL, "https://example.xyz")).contains("url.suspicious_tld"));
        assertFalse(ids(scored(TargetKind.URL, "https://tk.example.com")).contains("url.suspicious_tld"));
    }

    @Test
    void callback_rule_detects_phone_numbers() {
        assertTrue(ids(scored(TargetKind.MESSAGE, "Call 0555 12 34 56 for details."))
                .contains("msg.callback_request"));
    }

    @Test
    void all_caps_rule_needs_more_than_twenty_letters() {
        assertFalse(ids(scored(TargetKind.MESSAGE, "HELP ME NOW")).contains("msg.all_caps"));
        assertTrue(ids(scored(TargetKind.MESSAGE, "THIS IS A VERY LONG MESSAGE WRITTEN IN CAPS"))
                .contains("msg.all_caps"));
    }

    @Test
    void catalog_contains_seventeen_rules() {
        assertEquals(10, RuleCatalog.URL_RULES.size());
        assertEquals(7, RuleCatalog.MESSAGE_RULES.size());
        assertEquals(17, RuleCatalog.totalRules());
    }
}
