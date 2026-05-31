package ntu.lehoangdanggia.badminton_booking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BookingConfirmActivity extends AppCompatActivity {
    private com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
    private TextView tvBookingDetails;
    private EditText edtCustomerName, edtPhoneNumber;
    private Button btnConfirmBooking;

    private ArrayList<TimeCell> chosenSlots;
    private String bookingDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirm);

        tvBookingDetails = findViewById(R.id.tvBookingDetails);
        edtCustomerName = findViewById(R.id.edtCustomerName);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        if (getIntent() != null) {
            chosenSlots = (ArrayList<TimeCell>) getIntent().getSerializableExtra("CHOSEN_SLOTS");
            bookingDate = getIntent().getStringExtra("BOOKING_DATE");
        }

        hienThiThongTinCaDat();

        btnConfirmBooking.setOnClickListener(v -> {
            String name = edtCustomerName.getText().toString().trim();
            String phone = edtPhoneNumber.getText().toString().trim();

            if (name.isEmpty()) { edtCustomerName.setError("Vui lòng nhập tên!"); return; }
            if (phone.isEmpty()) { edtPhoneNumber.setError("Vui lòng nhập SĐT!"); return; }
            if (chosenSlots == null || chosenSlots.isEmpty()) return;

            // --- TÌM GIỜ BẮT ĐẦU VÀ GIỜ KẾT THÚC TỪ CÁC CA ĐÃ CHỌN ---
            ArrayList<String> startTimes = new ArrayList<>();
            ArrayList<String> endTimes = new ArrayList<>();
            int tongTien = 0;
            String tenSan = chosenSlots.get(0).getCustomerName(); // Tên sân ghim tạm

            for (TimeCell cell : chosenSlots) {
                // Chuỗi gốc: "17:00 - 17:30" -> Cắt ra start: "17:00", end: "17:30"
                String[] parts = cell.getTimeLabel().split(" - ");
                startTimes.add(parts[0]);
                endTimes.add(parts[1]);
                tongTien += calculatePriceForSlot(cell.getTimeLabel());
            }

            // Sắp xếp để lấy mốc nhỏ nhất và lớn nhất
            Collections.sort(startTimes);
            Collections.sort(endTimes);
            String starTimeStr = startTimes.get(0);          // Ví dụ: "17:00"
            String endTimeStr = endTimes.get(endTimes.size() - 1); // Ví dụ: "19:00"

            // Chuẩn hóa định dạng ngày từ "23/05/2026 📅" thành "2026-05-23" cho khớp DB của bạn
            String cleanDate = bookingDate.replace(" 📅", "").trim();
            if (cleanDate.contains("/")) {
                String[] dParts = cleanDate.split("/");
                cleanDate = dParts[2] + "-" + dParts[1] + "-" + dParts[0]; // Chuyển thành yyyy-MM-dd
            }

            // Tạo bookingID ngẫu nhiên ngẫu hứng hoặc dùng timestamp
            String bookingID = "BK" + System.currentTimeMillis() / 1000;

            // Đóng gói đúng các trường bạn yêu cầu lên Firestore
            Map<String, Object> bookingMap = new HashMap<>();
            bookingMap.put("bookingID", bookingID);
            bookingMap.put("UserID", name); // Lưu tạm tên người đặt vào trường UserID của bạn
            bookingMap.put("courtID", tenSan); // ID sân hoặc Tên sân (e.g., "Sân 1")
            bookingMap.put("date", cleanDate);
            bookingMap.put("starTime", starTimeStr);
            bookingMap.put("endTime", endTimeStr);
            bookingMap.put("status", "confirm");
            bookingMap.put("totalPrice", tongTien);
            // Bạn có thể lưu thêm SĐT vào map nếu muốn: bookingMap.put("phoneNumber", phone);

            // Đẩy lên Firestore
            db.collection("bookings").document(bookingID)
                    .set(bookingMap)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(BookingConfirmActivity.this, "Đã lưu phiếu đặt " + bookingID, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(BookingConfirmActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void hienThiThongTinCaDat() {
        if (chosenSlots == null || chosenSlots.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("📅 Ngày chơi: ").append(bookingDate).append("\n-------------------------------------------\n");
        int tongTien = 0;
        for (TimeCell cell : chosenSlots) {
            sb.append("📍 ").append(cell.getCustomerName()).append("   |   🕒 ca: ").append(cell.getTimeLabel()).append("\n");
            tongTien += calculatePriceForSlot(cell.getTimeLabel());
        }
        DecimalFormat formatter = new DecimalFormat("#,###");
        sb.append("-------------------------------------------\n💰 Tổng chi phí: ").append(formatter.format(tongTien)).append(" vnđ");
        tvBookingDetails.setText(sb.toString());
    }

    private int calculatePriceForSlot(String timeSlot) {
        if (timeSlot == null || timeSlot.isEmpty()) return 0;
        try {
            int startHour = Integer.parseInt(timeSlot.substring(0, 2));
            return (startHour >= 5 && startHour < 12) ? 65000 : (startHour >= 12 && startHour < 17) ? 70000 : 80000;
        } catch (Exception e) { return 0; }
    }
}