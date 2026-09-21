package com.tsp.jimi_api.global;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Localised server-generated messages (the strings JIMI builds itself, not the
 * LLM's `response`). The LLM is told to reply in the user's language; this
 * catalogue does the same for the handful of fixed messages (confirmation
 * prompts, "no events", etc.).
 *
 * <p>The language code comes from the LLM (it returns the ISO-639-1 code of the
 * user's message). Unknown languages fall back to English — the LLM's own
 * `response`/`answer` still adapts to any language, so only these fixed strings
 * are English-only for unsupported locales. Add a language by adding a column.
 */
public final class Messages {

    /** Keys for every server-generated, user-facing string. */
    public enum Key {
        NEEDS_CONNECTION,
        NO_EVENTS,
        NO_MATCH,
        MULTIPLE_MATCHES,
        DELETE_CONFIRM,       // arg: event description
        DELETE_CONFIRM_MANY,  // arg: count (events listed after)
        UPDATE_CONFIRM,       // arg: event description
        UPDATE_CONFIRM_MANY,  // arg: count
        UPDATE_DETAILS,       // arg: changes
        CANCELLED,
        DELETED,
        UPDATED
    }

    private static final String DEFAULT_LANG = "en";

    private static final Map<String, Map<Key, String>> CATALOG = Map.of(
            "en", english(),
            "fr", french());

    private Messages() {
    }

    /**
     * Localised message for {@code lang} (falls back to English), with optional
     * {@code %s} arguments filled in.
     */
    public static String get(final String lang, final Key key, final Object... args) {
        String code = lang == null ? DEFAULT_LANG : lang.toLowerCase(Locale.ROOT).trim();
        Map<Key, String> table = CATALOG.getOrDefault(code, CATALOG.get(DEFAULT_LANG));
        String template = table.getOrDefault(key, CATALOG.get(DEFAULT_LANG).get(key));
        return args.length == 0 ? template : String.format(template, args);
    }

    private static Map<Key, String> english() {
        EnumMap<Key, String> m = new EnumMap<>(Key.class);
        m.put(Key.NEEDS_CONNECTION, "I'd love to help! First connect the calendar you'd like me to "
                + "manage (Google, Apple or Outlook) and we're good to go. 📅");
        m.put(Key.NO_EVENTS, "You don't have any events scheduled. 🎉");
        m.put(Key.NO_MATCH, "I couldn't find a matching event. Could you tell me the title or date "
                + "so I'm sure I act on the right one?");
        m.put(Key.MULTIPLE_MATCHES, "I found several matching events. Which one do you mean?");
        m.put(Key.DELETE_CONFIRM, "Delete %s? This can't be undone.");
        m.put(Key.DELETE_CONFIRM_MANY, "Delete these %d events? This can't be undone.");
        m.put(Key.UPDATE_CONFIRM, "Update %s?");
        m.put(Key.UPDATE_CONFIRM_MANY, "Update these %d events?");
        m.put(Key.UPDATE_DETAILS, "New details: %s");
        m.put(Key.CANCELLED, "Okay, I won't make any changes. ✅");
        m.put(Key.DELETED, "Done — I've removed it from your calendar. 🗑️");
        m.put(Key.UPDATED, "Done — your calendar is updated. ✅");
        return m;
    }

    private static Map<Key, String> french() {
        EnumMap<Key, String> m = new EnumMap<>(Key.class);
        m.put(Key.NEEDS_CONNECTION, "Avec plaisir ! Connecte d'abord le calendrier que tu veux que je "
                + "gère (Google, Apple ou Outlook) et c'est parti. 📅");
        m.put(Key.NO_EVENTS, "Tu n'as aucun événement prévu. 🎉");
        m.put(Key.NO_MATCH, "Je n'ai pas trouvé d'événement correspondant. Peux-tu me donner le titre "
                + "ou la date pour être sûr d'agir sur le bon ?");
        m.put(Key.MULTIPLE_MATCHES, "J'ai trouvé plusieurs événements correspondants. Lequel veux-tu ?");
        m.put(Key.DELETE_CONFIRM, "Supprimer %s ? C'est irréversible.");
        m.put(Key.DELETE_CONFIRM_MANY, "Supprimer ces %d événements ? C'est irréversible.");
        m.put(Key.UPDATE_CONFIRM, "Modifier %s ?");
        m.put(Key.UPDATE_CONFIRM_MANY, "Modifier ces %d événements ?");
        m.put(Key.UPDATE_DETAILS, "Nouveaux détails : %s");
        m.put(Key.CANCELLED, "D'accord, je ne change rien. ✅");
        m.put(Key.DELETED, "C'est fait — je l'ai retiré de ton calendrier. 🗑️");
        m.put(Key.UPDATED, "C'est fait — ton calendrier est à jour. ✅");
        return m;
    }
}
