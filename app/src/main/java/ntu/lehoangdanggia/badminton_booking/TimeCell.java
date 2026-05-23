package ntu.lehoangdanggia.badminton_booking;

public class TimeCell {
    private String timeLabel;    // "05:00", "05:30"...
    private String status;       // "Trống", "Cố định", "Lịch ngày", "Linh hoạt"
    private String customerName;
    private String phoneNumber;

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
}