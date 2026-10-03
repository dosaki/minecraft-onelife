package net.dosaki.onelife.grave;

/** English ordinals for the death counter: words up to 99, digits with a suffix from 100. */
public final class Ordinals {

    private static final String[] BELOW_TWENTY = {
        "", "First", "Second", "Third", "Fourth", "Fifth", "Sixth", "Seventh", "Eighth", "Ninth", "Tenth",
        "Eleventh", "Twelfth", "Thirteenth", "Fourteenth", "Fifteenth", "Sixteenth", "Seventeenth", "Eighteenth",
        "Nineteenth"
    };
    private static final String[] TENS = {
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };
    private static final String[] TENTHS = {
        "", "", "Twentieth", "Thirtieth", "Fortieth", "Fiftieth", "Sixtieth", "Seventieth", "Eightieth", "Ninetieth"
    };

    private Ordinals() {}

    public static String of(int n) {
        if (n <= 0) throw new IllegalArgumentException("n must be >= 1, was " + n);
        if (n < 20) return BELOW_TWENTY[n];
        if (n < 100) {
            int tens = n / 10;
            int ones = n % 10;
            return ones == 0 ? TENTHS[tens] : TENS[tens] + "-" + BELOW_TWENTY[ones];
        }
        return n + suffix(n);
    }

    private static String suffix(int n) {
        int lastTwo = n % 100;
        if (lastTwo >= 11 && lastTwo <= 13) return "th";
        return switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
