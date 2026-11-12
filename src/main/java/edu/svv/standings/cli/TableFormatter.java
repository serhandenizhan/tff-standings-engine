package edu.svv.standings.cli;

import edu.svv.standings.StandingRow;

import java.util.List;

/**
 * Puan tablosunu hizalı düz metin olarak biçimlendirir (FR-15).
 */
public class TableFormatter {

    private static final String[] HEADERS = {"Pos", "Team", "P", "W", "D", "L", "GF", "GA", "GD", "Pts"};
    private static final String LEGEND = "* = unresolved tie (requires playoff)";

    public String format(List<StandingRow> table) {
        int columns = HEADERS.length;
        String[][] cells = new String[table.size()][columns];
        int[] widths = new int[columns];
        for (int c = 0; c < columns; c++) {
            widths[c] = HEADERS[c].length();
        }

        boolean anyUnresolved = false;
        for (int r = 0; r < table.size(); r++) {
            StandingRow row = table.get(r);
            cells[r] = new String[] {
                String.valueOf(row.getPosition()),
                row.getTeam().name(),
                String.valueOf(row.getPlayed()),
                String.valueOf(row.getWon()),
                String.valueOf(row.getDrawn()),
                String.valueOf(row.getLost()),
                String.valueOf(row.getGoalsFor()),
                String.valueOf(row.getGoalsAgainst()),
                String.valueOf(row.getGoalDifference()),
                String.valueOf(row.getPoints())
            };
            for (int c = 0; c < columns; c++) {
                widths[c] = Math.max(widths[c], cells[r][c].length());
            }
            anyUnresolved |= row.isUnresolved();
        }

        StringBuilder sb = new StringBuilder();
        appendRow(sb, HEADERS, widths, false);
        for (int r = 0; r < table.size(); r++) {
            appendRow(sb, cells[r], widths, table.get(r).isUnresolved());
        }
        if (anyUnresolved) {
            sb.append(LEGEND).append('\n');
        }
        return sb.toString();
    }

    private void appendRow(StringBuilder sb, String[] cells, int[] widths, boolean unresolved) {
        for (int c = 0; c < cells.length; c++) {
            if (c > 0) {
                sb.append("  ");
            }
            // takım adı sola, sayılar sağa hizalanır
            if (c == 1) {
                sb.append(String.format("%-" + widths[c] + "s", cells[c]));
            } else {
                sb.append(String.format("%" + widths[c] + "s", cells[c]));
            }
        }
        if (unresolved) {
            sb.append(" *");
        }
        sb.append('\n');
    }
}
