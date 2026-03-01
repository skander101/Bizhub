package com.bizhub.model.services.common.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Navigation Bot Service - Natural language intent recognition for page navigation.
 * Uses keyword-based matching to understand user navigation commands.
 */
public class AiNavigationBotService {

    public enum NavigationIntent {
        GO_TO_LOGIN,
        GO_TO_SIGNUP,
        GO_TO_PROFILE,
        GO_TO_USER_MANAGEMENT,
        GO_TO_FORMATIONS,
        GO_TO_REVIEWS,
        GO_BACK,
        HELP,
        QUERY_DATABASE,
        UNKNOWN
    }

    public record BotResponse(String message, NavigationIntent intent, boolean isNavigationCommand) {}

    private final Map<NavigationIntent, List<String>> intentKeywords;
    private final Map<NavigationIntent, String> intentDescriptions;

    public AiNavigationBotService() {
        this.intentKeywords = initializeKeywords();
        this.intentDescriptions = initializeDescriptions();
    }

    private Map<NavigationIntent, List<String>> initializeKeywords() {
        Map<NavigationIntent, List<String>> keywords = new HashMap<>();

        keywords.put(NavigationIntent.GO_TO_LOGIN, Arrays.asList(
            "go to login", "go to sign in"
        ));

        keywords.put(NavigationIntent.GO_TO_SIGNUP, Arrays.asList(
            "go to signup", "go to register"
        ));

        keywords.put(NavigationIntent.GO_TO_PROFILE, Arrays.asList(
            "go to profile", "go to account", "go to settings"
        ));


        keywords.put(NavigationIntent.GO_TO_USER_MANAGEMENT, Arrays.asList(
            "go to users", "go to user management"
        ));

        keywords.put(NavigationIntent.GO_TO_FORMATIONS, Arrays.asList(
            "go to formations", "go to training", "go to courses"
        ));

        keywords.put(NavigationIntent.GO_TO_REVIEWS, Arrays.asList(
            "go to reviews", "go to feedback", "go to avis"
        ));


        keywords.put(NavigationIntent.GO_BACK, Arrays.asList(
            "go back", "back", "go back", "previous", "return", "go home", "previous page"
        ));

        keywords.put(NavigationIntent.HELP, Arrays.asList(
            "help", "what can you do", "commands", "assist", "support", "how to",
            "guide", "navigation", "options", "menu", "available commands",
            "id", "ids", "user_id", "password", "token", "secret", "key", "hash",
            "sensitive", "private", "confidential", "authentication"
        ));

        keywords.put(NavigationIntent.QUERY_DATABASE, Arrays.asList(
            "how many", "count", "number of", "what are", "show me", "list",
            "reviews for", "formations with", "database", "query",
            "tell me", "give me", "find", "search for", "average", "total",
            "oldest", "youngest", "newest", "latest", "first", "last",
            "which", "who", "best", "worst", "most", "least", "popular",
            "trending", "popping", "top", "highest", "lowest", "rated",
            "formation", "user", "review", "application", "request",
            "right now", "currently", "active", "recent", "compare",
            "rating", "rate", "score", "stats", "statistics", "info",
            "about", "detail", "describe", "what is", "how much", "how old"
        ));

        return keywords;
    }

    private Map<NavigationIntent, String> initializeDescriptions() {
        Map<NavigationIntent, String> desc = new HashMap<>();
        desc.put(NavigationIntent.GO_TO_LOGIN, "Navigate to the login page");
        desc.put(NavigationIntent.GO_TO_SIGNUP, "Create a new account");
        desc.put(NavigationIntent.GO_TO_PROFILE, "View and edit your profile");
        desc.put(NavigationIntent.GO_TO_USER_MANAGEMENT, "Manage system users");
        desc.put(NavigationIntent.GO_TO_FORMATIONS, "Browse available formations/training");
        desc.put(NavigationIntent.GO_TO_REVIEWS, "View and manage reviews");
        desc.put(NavigationIntent.GO_BACK, "Navigate to previous page");
        desc.put(NavigationIntent.HELP, "Show available commands or refuse sensitive information requests");
        desc.put(NavigationIntent.QUERY_DATABASE, "Query database information");
        return desc;
    }

    /**
     * Process user input and return bot response with navigation intent.
     */
    public BotResponse processInput(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return new BotResponse("I'm here to help you navigate! Try typing 'help' for available commands.",
                NavigationIntent.UNKNOWN, false);
        }

        String normalizedInput = normalizeInput(userInput);
        NavigationIntent intent = findBestMatch(normalizedInput);

        String response = generateResponse(intent, normalizedInput);
        boolean isNavCommand = intent != NavigationIntent.UNKNOWN && intent != NavigationIntent.HELP;

        return new BotResponse(response, intent, isNavCommand);
    }

    private String normalizeInput(String input) {
        return input.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "") // Remove special characters
            .replaceAll("\\s+", " ")        // Normalize whitespace
            .trim();
    }

    private NavigationIntent findBestMatch(String input) {
        NavigationIntent bestMatch = NavigationIntent.UNKNOWN;
        int bestScore = 0;

        // First check for exact "go to" matches - these should take priority for navigation
        if (input.toLowerCase().startsWith("go to ")) {
            for (Map.Entry<NavigationIntent, List<String>> entry : intentKeywords.entrySet()) {
                int score = calculateMatchScore(input, entry.getValue());
                if (score > bestScore) {
                    bestScore = score;
                    bestMatch = entry.getKey();
                }
            }
            // If we found a "go to" match, return it immediately
            if (bestScore > 0 && bestMatch != NavigationIntent.QUERY_DATABASE) {
                return bestMatch;
            }
        }

        // For all other queries, prioritize QUERY_DATABASE
        for (Map.Entry<NavigationIntent, List<String>> entry : intentKeywords.entrySet()) {
            NavigationIntent intent = entry.getKey();
            int score = calculateMatchScore(input, entry.getValue());
            
            // Only allow navigation intents if they have "go to" keywords
            if (intent != NavigationIntent.QUERY_DATABASE && intent != NavigationIntent.HELP && intent != NavigationIntent.UNKNOWN) {
                // Check if this intent has "go to" keywords
                boolean hasGoToKeywords = entry.getValue().stream().anyMatch(kw -> kw.startsWith("go to"));
                if (!hasGoToKeywords) {
                    score = 0; // Don't allow navigation without "go to"
                }
            }
            
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry.getKey();
            }
        }

        // FALLBACK: anything that isn't navigation or help → send to AI database assistant
        if (bestMatch == NavigationIntent.UNKNOWN) {
            bestMatch = NavigationIntent.QUERY_DATABASE;
        }

        return bestMatch;
    }

    private int calculateMatchScore(String input, List<String> keywords) {
        int score = 0;

        for (String keyword : keywords) {
            // Exact match gets highest score
            if (input.equals(keyword)) {
                score += 10;
            }
            // Contains full phrase
            else if (input.contains(keyword)) {
                score += 5;
            }
            // Words match - only exact word matches, not partial
            else {
                String[] inputWords = input.split(" ");
                String[] keywordWords = keyword.split(" ");
                for (String kw : keywordWords) {
                    for (String iw : inputWords) {
                        if (iw.equals(kw)) {
                            score += 2;
                        }
                    }
                }
            }
        }

        return score;
    }

    private String generateResponse(NavigationIntent intent, String normalizedInput) {
        switch (intent) {
            case GO_TO_LOGIN:
                return "Taking you to the login page...";
            case GO_TO_SIGNUP:
                return "Opening the registration page...";
            case GO_TO_PROFILE:
                return "Navigating to your profile...";
            case GO_TO_USER_MANAGEMENT:
                return "Taking you to user management...";
            case GO_TO_FORMATIONS:
                return "Opening the formations page...";
            case GO_TO_REVIEWS:
                return "Showing the reviews page...";
            case GO_BACK:
                return "Going back...";
            case HELP:
                // Check if it's a sensitive info request
                if (normalizedInput.contains("id") || normalizedInput.contains("password") || 
                    normalizedInput.contains("token") || normalizedInput.contains("secret") ||
                    normalizedInput.contains("hash") || normalizedInput.contains("key") ||
                    normalizedInput.contains("sensitive") || normalizedInput.contains("private") ||
                    normalizedInput.contains("confidential") || normalizedInput.contains("authentication")) {
                    return "🔒 I cannot provide sensitive information like IDs, passwords, tokens, or authentication data for security reasons.";
                }
                return generateHelpMessage();
            case QUERY_DATABASE:
                return "I'll query the database for you...";
            case UNKNOWN:
            default:
                return "I didn't understand that. Try typing 'help' to see what I can do!";
        }
    }

    private String generateHelpMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 **BizHub Navigation Assistant**\n\n");
        sb.append("Here's what I can help you with:\n\n");

        for (NavigationIntent intent : NavigationIntent.values()) {
            if (intent == NavigationIntent.UNKNOWN) continue;

            String description = intentDescriptions.get(intent);
            List<String> keywords = intentKeywords.get(intent);

            if (description != null && keywords != null && !keywords.isEmpty()) {
                sb.append("• **").append(capitalizeFirst(keywords.get(0))).append("** - ");
                sb.append(description).append("\n");
            }
        }

        sb.append("\nJust type what you want to do, like:\n");
        sb.append("• \"go to profile\"\n");
        sb.append("• \"go to formations\"\n");
        sb.append("• \"go to reviews\"\n");
        sb.append("• \"how many reviews for formation F2\"\n");
        sb.append("• \"show me all users\"");

        return sb.toString();
    }

    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Execute navigation based on intent using the provided NavigationService.
     * Returns true if navigation was executed, false otherwise.
     */
    public boolean executeNavigation(NavigationIntent intent, NavigationService navService) {
        if (navService == null) return false;

        try {
            switch (intent) {
                case GO_TO_LOGIN:
                    navService.goToLogin();
                    return true;
                case GO_TO_SIGNUP:
                    navService.goToSignup();
                    return true;
                case GO_TO_PROFILE:
                    navService.goToProfile();
                    return true;
                case GO_TO_USER_MANAGEMENT:
                    navService.goToUserManagement();
                    return true;
                case GO_TO_FORMATIONS:
                    navService.goToFormations();
                    return true;
                case GO_TO_REVIEWS:
                    navService.goToReviews();
                    return true;
                case GO_BACK:
                    // Note: Back navigation requires history tracking
                    // For now, go to profile as fallback
                    navService.goToProfile();
                    return true;
                default:
                    return false;
            }
        } catch (Exception e) {
            return false;
        }
    }
}
