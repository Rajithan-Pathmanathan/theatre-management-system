package com.vijaya.theatre.reporting;

import com.vijaya.theatre.reporting.dto.DailyReportResponse;
import com.vijaya.theatre.reporting.dto.ShowReportResponse;

/** FR-48 (Phase 2 export path). CSV, not PDF — less code, and nothing in the SRS asks for a printable layout. */
final class ReportCsvWriter {

    private static final String HEADER = "Movie,Start Time,Tickets Sold,Collections,Online Tickets,Online Collections,Physical Tickets,Physical Collections\n";

    private ReportCsvWriter() {
    }

    static String daily(DailyReportResponse report) {
        StringBuilder csv = new StringBuilder(HEADER);
        for (ShowReportResponse show : report.shows()) {
            appendRow(csv, show.movieTitle(), show.startTime().toString(), show.ticketsSold(), show.collections(),
                    show.online().ticketsSold(), show.online().collections(), show.physical().ticketsSold(), show.physical().collections());
        }
        appendRow(csv, "TOTAL", "", report.totalTicketsSold(), report.totalCollections(),
                report.online().ticketsSold(), report.online().collections(), report.physical().ticketsSold(), report.physical().collections());
        return csv.toString();
    }

    static String show(ShowReportResponse show) {
        StringBuilder csv = new StringBuilder(HEADER);
        appendRow(csv, show.movieTitle(), show.startTime().toString(), show.ticketsSold(), show.collections(),
                show.online().ticketsSold(), show.online().collections(), show.physical().ticketsSold(), show.physical().collections());
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, String movieTitle, String startTime, int ticketsSold, Object collections,
                                   int onlineTickets, Object onlineCollections, int physicalTickets, Object physicalCollections) {
        csv.append(escape(movieTitle)).append(',')
                .append(startTime).append(',')
                .append(ticketsSold).append(',')
                .append(collections).append(',')
                .append(onlineTickets).append(',')
                .append(onlineCollections).append(',')
                .append(physicalTickets).append(',')
                .append(physicalCollections).append('\n');
    }

    /** Only the movie title can ever contain a comma, quote, or newline — every other field here is a number or a time. */
    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
