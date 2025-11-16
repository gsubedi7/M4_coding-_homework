package theater;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * This class generates a statement for a given invoice of performances.
 */
public class StatementPrinter {
    private Invoice invoice;
    private Map<String, Play> plays;

    public StatementPrinter(Invoice invoice, Map<String, Play> plays) {
        this.invoice = invoice;
        this.plays = plays;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public Map<String, Play> getPlay() {
        return plays;
    }

    /**
     * Returns a formatted statement of the invoice associated with this printer.
     * @return the formatted statement
     * @throws RuntimeException if one of the play types is not known
     */
    public String statement() {
        final int base = 40000;
        final int thousand = 1000;
        final int thirty = 30;
        final int hundred = 100;
        int totalAmount = 0;
        int volumeCredits = 0;
        final StringBuilder result = new StringBuilder("Statement for " + invoice.getCustomer()
                + System.lineSeparator());

        for (Performance p : getPerformances()) {
            final Play play = plays.get(p.getPlayID());

            int thisAmount = 0;
            switch (play.getType()) {
                case "tragedy":
                    thisAmount = getAmount(p, base, thousand, thirty);
                    break;
                case "comedy":
                    thisAmount = Constants.COMEDY_BASE_AMOUNT;
                    if (p.getAudience() > Constants.COMEDY_AUDIENCE_THRESHOLD) {
                        thisAmount += Constants.COMEDY_OVER_BASE_CAPACITY_AMOUNT
                                + (Constants.COMEDY_OVER_BASE_CAPACITY_PER_PERSON
                                * (p.getAudience() - Constants.COMEDY_AUDIENCE_THRESHOLD));
                    }
                    thisAmount += Constants.COMEDY_AMOUNT_PER_AUDIENCE * p.getAudience();
                    break;
                default:
                    throw new RuntimeException(String.format("unknown type: %s", play.getType()));
            }

            // add volume credits
            volumeCredits += getVolumeCredits(p, play);

            // print line for this order
            result.append(String.format("  %s: %s (%s seats)%n", play.getName(),
                    usd().format(thisAmount / hundred), p.getAudience()));
            totalAmount += thisAmount;
        }
        result.append(String.format("Amount owed is %s%n", usd().format(totalAmount / hundred)));
        result.append(String.format("You earned %s credits%n", volumeCredits));
        return result.toString();
    }

    private static NumberFormat usd() {
        return NumberFormat.getCurrencyInstance(Locale.US);
    }

    private static int getVolumeCredits(Performance performance, Play play) {
        int result = 0;
        result += Math.max(performance.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD, 0);
        // add extra credit for every five comedy attendees
        if ("comedy".equals(play.getType())) {
            result += performance.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
        }
        return result;
    }

    private List<Performance> getPerformances() {
        return invoice.getPerformances();
    }

    private static int getAmount(Performance performance, int base, int thousand, int thirty) {
        int thisAmount;
        thisAmount = base;
        if (performance.getAudience() > Constants.TRAGEDY_AUDIENCE_THRESHOLD) {
            thisAmount += thousand * (performance.getAudience() - thirty);
        }
        return thisAmount;
    }
}
