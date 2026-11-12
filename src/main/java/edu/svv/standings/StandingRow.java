package edu.svv.standings;

import edu.svv.standings.model.Team;

/**
 * Puan tablosundaki bir takımın satırı (FR-05, FR-13).
 * Hesaplama sırasında değerler biriktirildiği için değiştirilebilir tutuldu.
 */
public class StandingRow {

    private final Team team;
    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;
    private int deductedPoints;
    private int forfeitLosses;
    private int position;
    private boolean unresolved;

    public StandingRow(Team team) {
        this.team = team;
    }

    /** Bir maçın sonucunu satıra işler (goller hükmen düzeltmesi yapılmış olmalı). */
    void addMatch(int scored, int conceded, boolean lostByForfeit) {
        played++;
        // DEF-004: toplam int aralığını aşarsa sessizce negatife dönmek yerine girdi reddedilir (V-09)
        try {
            goalsFor = Math.addExact(goalsFor, scored);
            goalsAgainst = Math.addExact(goalsAgainst, conceded);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Goal total out of range for team " + team.name(), e);
        }
        if (scored > conceded) {
            won++;
        } else if (scored == conceded) {
            drawn++;
        } else {
            lost++;
        }
        if (lostByForfeit) {
            forfeitLosses++;
        }
    }

    void addDeduction(int points) {
        // DEF-004: biriken silme int aralığını aşarsa puan pozitife dönüyordu; artık reddedilir (V-09)
        try {
            deductedPoints = Math.addExact(deductedPoints, points);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Deducted points out of range for team " + team.name(), e);
        }
    }

    void setPosition(int position) {
        this.position = position;
    }

    void setUnresolved(boolean unresolved) {
        this.unresolved = unresolved;
    }

    public Team getTeam() {
        return team;
    }

    public int getPlayed() {
        return played;
    }

    public int getWon() {
        return won;
    }

    public int getDrawn() {
        return drawn;
    }

    public int getLost() {
        return lost;
    }

    public int getGoalsFor() {
        return goalsFor;
    }

    public int getGoalsAgainst() {
        return goalsAgainst;
    }

    // A-01: averaj = atılan - yenilen
    public int getGoalDifference() {
        return goalsFor - goalsAgainst;
    }

    public int getDeductedPoints() {
        return deductedPoints;
    }

    // FMT 9/1: galibiyet 3, beraberlik 1; FR-07: silinen puan düşülür, negatif olabilir
    public int getPoints() {
        return won * 3 + drawn - deductedPoints;
    }

    public boolean hasForfeitLoss() {
        return forfeitLosses > 0;
    }

    public int getPosition() {
        return position;
    }

    public boolean isUnresolved() {
        return unresolved;
    }

    @Override
    public String toString() {
        return position + ". " + team.name() + " " + played + " " + won + " " + drawn + " " + lost
                + " " + goalsFor + ":" + goalsAgainst + " " + getPoints();
    }
}
