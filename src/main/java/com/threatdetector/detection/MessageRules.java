package com.threatdetector.detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/** Seven local rules inspecting message content (email/SMS). */
public final class MessageRules {
    private MessageRules() {
    }

    public static List<Rule> create() {
        List<Rule> rules = new ArrayList<>();
        rules.add(rule("msg.urgency", "Message creates artificial urgency", 20, containsAny(
                "urgent", "immediately", "act now", "final notice", "within 24 hours")));
        rules.add(rule("msg.credential_request", "Message requests credentials", 30, MessageRules::credentialRequest));
        rules.add(rule("msg.financial_bait", "Message offers a financial reward", 20, containsAny(
                "prize", "lottery", "gift card", "crypto", "investment", "refund", "cash reward", "free wallet")));
        rules.add(rule("msg.threat_pressure", "Message threatens consequences", 20, containsAny(
                "suspended", "legal action", "court", "debt", "police", "arrest", "locked",
                "lose access", "destroyed", "held")));
        rules.add(rule("msg.shortened_link", "Message contains a shortened link", 15, containsAny(
                "bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "cutt.ly", "rb.gy")));
        rules.add(rule("msg.callback_request", "Message asks for an off-platform reply", 10,
                MessageRules::callbackRequest));
        rules.add(rule("msg.all_caps", "Message shouts in all caps", 5, MessageRules::allCaps));
        return List.copyOf(rules);
    }

    private static Rule rule(String id, String title, int points, Predicate<String> check) {
        return input -> {
            if (input instanceof RuleInput.Message message) {
                return check.test(message.text())
                        ? Optional.of(new RuleHit(id, title, points, List.of("message content")))
                        : Optional.empty();
            }
            return Optional.empty();
        };
    }

    private static Predicate<String> containsAny(String... needles) {
        return text -> {
            String lower = text.toLowerCase(Locale.ROOT);
            for (String needle : needles) {
                if (lower.contains(needle)) return true;
            }
            return false;
        };
    }

    private static boolean credentialRequest(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("verification code is") || lower.contains("your code is")) return false;
        boolean action = containsAny("verify", "confirm", "enter", "provide", "send", "reset", "share")
                .test(lower);
        boolean secret = containsAny("password", "otp", "pin", "card number", "cvv").test(lower);
        return action && secret;
    }

    private static boolean callbackRequest(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("whatsapp") || lower.contains("telegram")) return true;
        return text.matches("(?s).*(\\+?\\d[\\d\\s.-]{8,}).*");
    }

    private static boolean allCaps(String text) {
        long letters = text.chars().filter(Character::isLetter).count();
        if (letters <= 20) return false;
        long upper = text.chars().filter(Character::isUpperCase).count();
        return upper * 2 > letters;
    }
}
