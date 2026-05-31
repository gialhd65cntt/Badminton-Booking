package ntu.lehoangdanggia.badminton_booking;
import java.io.Serializable;
public class TimeCell implements Serializable {
    private String timeLabel;    // "05:00", "05:30"...
    private String status;       // "Trống", "Cố định", "Lịch ngày", "Linh hoạt"
    private String customerName;
    private String phoneNumber;
    private boolean isSelected = false; // Thuộc tính mới

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public TimeCell(String timeLabel, String status, String customerName, String phoneNumber) {
        this.timeLabel = timeLabel;
        this.status = status;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
    }

    public String getTimeLabel() { return timeLabel; }
    public String getStatus() { return status; }
    public String getCustomerName() { return customerName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getTimeRangeString() {
        return timeLabel;
    }

    public void setCustomerName(String courtName) {
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}