package ntu.lehoangdanggia.badminton_booking;
import java.util.List;

public class CourtRow {
    private String courtName; // "Sân 1", "Sân 2"...
    private List<TimeCell> timeCells; // Danh sách 24 hoặc 48 ô thời gian trong ngày

    public CourtRow(String courtName, List<TimeCell> timeCells) {
        this.courtName = courtName;
        this.timeCells = timeCells;
    }

    public String getCourtName() { return courtName; }
    public List<TimeCell> getTimeCells() { return timeCells; }
}