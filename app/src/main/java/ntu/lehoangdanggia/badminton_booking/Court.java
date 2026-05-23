package ntu.lehoangdanggia.badminton_booking;

public class Court {
    private String courtId; // Dùng để lưu lại Document ID ngẫu nhiên của Firebase khi cần chỉnh sửa/xóa sân
    private String courtName;
    private long pricePerHour; // Firebase mặc định lưu số nguyên là kiểu Long, dùng long để tránh lỗi ép kiểu (cast)
    private String status;

    // 1. BẮT BUỘC phải có một Constructor rỗng (không tham số) để Firebase tự động đổ dữ liệu vào
    public Court() {
    }

    // 2. Constructor có tham số (phòng khi bạn muốn tự tạo đối tượng trong code)
    public Court(String courtName, long pricePerHour, String status) {
        this.courtName = courtName;
        this.pricePerHour = pricePerHour;
        this.status = status;
    }

    // 3. Toàn bộ các hàm Getter và Setter để Adapter và MainActivity gọi lấy dữ liệu
    public String getCourtId() {
        return courtId;
    }

    public void setCourtId(String courtId) {
        this.courtId = courtId;
    }

    public String getCourtName() {
        return courtName;
    }

    public void setCourtName(String courtName) {
        this.courtName = courtName;
    }

    public long getPricePerHour() {
        return pricePerHour;
    }

    public void setPricePerHour(long pricePerHour) {
        this.pricePerHour = pricePerHour;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}