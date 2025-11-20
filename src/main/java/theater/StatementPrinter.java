package theater;

import java.text.NumberFormat;
import java.util.HashMap;
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
        final List<Performance> performances = getPerformances();
        final Map<String, Integer> amountsPerPerformance = new HashMap<>();
        final Map<String, Integer> audiencePerPerformance = new HashMap<>();

        final int totalAmount = getTotalAmount(performances, base, thousand, thirty, amountsPerPerformance,
                audiencePerPerformance);

        final StringBuilder result =
                new StringBuilder("Statement for " + invoice.getCustomer() + System.lineSeparator());

        for (Performance p : performances) {
            final Play play = plays.get(p.getPlayID());

            result.append(String.format(
                    "  %s: %s (%s seats)%n",
                    play.getName(),
                    usd().format(amountsPerPerformance.get(p.getPlayID()) / hundred),
                    audiencePerPerformance.get(p.getPlayID())
            ));
        }

        result.append(String.format("Amount owed is %s%n", usd().format(totalAmount / hundred)));
        result.append(String.format("You earned %s credits%n", getTotalVolumeCredits(performances)));

        return result.toString();
    }

    private int getTotalAmount(List<Performance> performances, int base, int thousand,
                               int thirty, Map<String, Integer> amountsPerPerformance,
                               Map<String, Integer> audiencePerPerformance) {
        int totalAmount = 0;

        for (Performance p : performances) {
            final Play play = plays.get(p.getPlayID());
            int thisAmount;

            switch (play.getType()) {
                case "tragedy":
                    thisAmount = getAmount(p, base, thousand, thirty);
                    break;

                case "comedy":
                    thisAmount = Constants.COMEDY_BASE_AMOUNT;
                    if (p.getAudience() > Constants.COMEDY_AUDIENCE_THRESHOLD) {
                        thisAmount += Constants.COMEDY_OVER_BASE_CAPACITY_AMOUNT
                                + Constants.COMEDY_OVER_BASE_CAPACITY_PER_PERSON
                                * (p.getAudience() - Constants.COMEDY_AUDIENCE_THRESHOLD);
                    }
                    thisAmount += Constants.COMEDY_AMOUNT_PER_AUDIENCE * p.getAudience();
                    break;

                default:
                    throw new RuntimeException("Unknown type: " + play.getType());
            }

            amountsPerPerformance.put(p.getPlayID(), thisAmount);
            audiencePerPerformance.put(p.getPlayID(), p.getAudience());
            totalAmount += thisAmount;
        }
        return totalAmount;
    }

    private int getTotalVolumeCredits(List<Performance> performances) {
        int volumeCredits = 0;

        for (Performance p : performances) {
            volumeCredits += getVolumeCredits(p);
        }

        return volumeCredits;
    }

    private int getVolumeCredits(Performance performance) {
        final Play play = plays.get(performance.getPlayID());

        int result = Math.max(
                performance.getAudience() - Constants.BASE_VOLUME_CREDIT_THRESHOLD,
                0
        );

        if ("comedy".equals(play.getType())) {
            result += performance.getAudience() / Constants.COMEDY_EXTRA_VOLUME_FACTOR;
        }

        return result;
    }

    private static NumberFormat usd() {
        return NumberFormat.getCurrencyInstance(Locale.US);
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

