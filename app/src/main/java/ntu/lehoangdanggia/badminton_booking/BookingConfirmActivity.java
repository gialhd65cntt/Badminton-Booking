package ntu.lehoangdanggia.badminton_booking;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BookingConfirmActivity extends AppCompatActivity {
    private com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();

    private TextView tvBookingDetails;
    private EditText edtCustomerName, edtPhoneNumber;
    private Button btnConfirmBooking, btnSelectBill;
    private ImageView imgVietQR, imgBillPreview;

    private ArrayList<TimeCell> chosenSlots;
    private String bookingDate;
    private int tongTien = 0;
    private String groupBookingID;
    private String cleanDate;

    private Uri imageUri = null; // Lưu đường dẫn ảnh bill được chọn
    private CountDownTimer pendingTimer;
    private boolean isBookingSuccessful = false; // Cờ kiểm tra trạng thái thoát
    private ArrayList<String> createdDocumentIDs = new ArrayList<>(); // Lưu các ID đã giữ chỗ để xóa nếu hủy

    private final String BANK_ID = "vcb";
    private final String ACCOUNT_NO = "1234567890";
    private final String ACCOUNT_NAME = "LE HOANG DANG GIA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirm);

        tvBookingDetails = findViewById(R.id.tvBookingDetails);
        edtCustomerName = findViewById(R.id.edtCustomerName);
        edtPhoneNumber = findViewById(R.id.edtPhoneNumber);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        btnSelectBill = findViewById(R.id.btnSelectBill);
        imgVietQR = findViewById(R.id.imgVietQR);
        imgBillPreview = findViewById(R.id.imgBillPreview);

        if (getIntent() != null) {
            chosenSlots = (ArrayList<TimeCell>) getIntent().getSerializableExtra("CHOSEN_SLOTS");
            bookingDate = getIntent().getStringExtra("BOOKING_DATE");
        }

        groupBookingID = "BK" + System.currentTimeMillis() / 1000;

        cleanDate = bookingDate.replace(" 📅", "").trim();
        if (cleanDate.contains("/")) {
            String[] dParts = cleanDate.split("/");
            cleanDate = dParts[2] + "-" + dParts[1] + "-" + dParts[0];
        }

        tinhTongTienLichDat();
        hienThiThongTinCaDat();
        taoMaQRThanhToan();

        // ⏱️ HÀNH ĐỘNG 1: Đẩy lịch "pending" lên ngay để giữ chỗ (Khóa lưới 2 phút)
        giuChoTamtruoc();

        // Nút chọn ảnh hóa đơn từ máy điện thoại
        btnSelectBill.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 101);
        });

        // Nút bấm xác nhận đặt chính thức
        btnConfirmBooking.setOnClickListener(v -> {
            String name = edtCustomerName.getText().toString().trim();
            String phone = edtPhoneNumber.getText().toString().trim();

            if (name.isEmpty()) { edtCustomerName.setError("Vui lòng nhập tên!"); return; }
            if (phone.isEmpty()) { edtPhoneNumber.setError("Vui lòng nhập SĐT!"); return; }
            if (imageUri == null) {
                Toast.makeText(this, "Bạn bắt buộc phải tải lên ảnh Bill chuyển khoản để đối chiếu!", Toast.LENGTH_LONG).show();
                return;
            }

            // Tiến hành upload ảnh lên Firebase Storage rồi cập nhật trạng thái đặt sân
            uploadBillVaXacNhan(name, phone);
        });
    }

    // Nhận kết quả chọn ảnh từ thư viện trả về
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            imgBillPreview.setVisibility(View.VISIBLE);
            imgBillPreview.setImageURI(imageUri); // Hiển thị ảnh cho khách xem trước
        }
    }

    private void giuChoTamtruoc() {
        if (chosenSlots == null || chosenSlots.isEmpty()) return;

        Map<String, ArrayList<TimeCell>> slotsByCourt = gopCaTheoSan();
        WriteBatch batch = db.batch();
        int documentIndex = 0;

        for (Map.Entry<String, ArrayList<TimeCell>> entry : slotsByCourt.entrySet()) {
            String currentCourtID = entry.getKey();
            ArrayList<TimeCell> courtCells = entry.getValue();

            int i = 0;
            while (i < courtCells.size()) {
                TimeCell startCell = courtCells.get(i);
                String[] startParts = startCell.getTimeLabel().split("\\s*-\\s*");
                String currentStart = startParts[0];
                String currentEnd = startParts[1];

                int j = i + 1;
                while (j < courtCells.size()) {
                    if (courtCells.get(j).getTimeLabel().split("\\s*-\\s*")[0].equals(currentEnd)) {
                        currentEnd = courtCells.get(j).getTimeLabel().split("\\s*-\\s*")[1];
                        j++;
                    } else { break; }
                }

                String individualDocID = groupBookingID + "_" + documentIndex;
                createdDocumentIDs.add(individualDocID); // Đút vào danh sách quản lý tạm hủy
                documentIndex++;

                Map<String, Object> bookingMap = new HashMap<>();
                bookingMap.put("bookingID", individualDocID);
                bookingMap.put("courtID", currentCourtID);
                bookingMap.put("date", cleanDate);
                bookingMap.put("startTime", currentStart);
                bookingMap.put("endTime", currentEnd);
                bookingMap.put("status", "pending"); // Trạng thái giữ chỗ tạm thời, trên lưới hiển thị bạn xử lý màu khác hoặc màu đỏ luôn tùy ý

                DocumentReference docRef = db.collection("Booking").document(individualDocID);
                batch.set(docRef, bookingMap);
                i = j;
            }
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            startTimerGiuCho(); // Bắt đầu đếm ngược 2 phút sau khi giữ chỗ thành công
        });
    }

    private void startTimerGiuCho() {
        pendingTimer = new CountDownTimer(60000, 1000) { // 60.000 miligiây = 1 phút
            @Override
            public void onTick(long millisUntilFinished) {
                btnConfirmBooking.setText("XÁC NHẬN ĐẶT SÂN (" + millisUntilFinished / 1000 + "s)");
            }

            @Override
            public void onFinish() {
                if (!isBookingSuccessful) {
                    Toast.makeText(BookingConfirmActivity.this, "Đã hết thời gian 2 phút giữ chỗ! Lịch của bạn tự động bị hủy.", Toast.LENGTH_LONG).show();
                    xoaLichGiuChoTamtram();
                    finish();
                }
            }
        }.start();
    }

    private void uploadBillVaXacNhan(String name, String phone) {
        Toast.makeText(this, "Đang xử lý và nén ảnh hóa đơn...", Toast.LENGTH_SHORT).show();

        try {
            // 1. Đọc file ảnh từ Uri của máy ảo chuyển sang dạng Bitmap
            android.graphics.Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);

            // 2. Thu nhỏ kích thước ảnh (Scale) để giảm bớt dung lượng xuống dưới 1MB cực kỳ an toàn
            // Giữ nguyên tỷ lệ, giới hạn chiều rộng tối đa là 800px (vẫn nhìn rõ chữ trên hóa đơn)
            int maxSize = 800;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxSize || height > maxSize) {
                float bitmapRatio = (float) width / (float) height;
                if (bitmapRatio > 1) {
                    width = maxSize;
                    height = (int) (width / bitmapRatio);
                } else {
                    height = maxSize;
                    width = (int) (height * bitmapRatio);
                }
                bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            // 3. Nén chất lượng ảnh xuống 50% để tối ưu hóa chuỗi String lưu vào Firestore
            java.io.ByteArrayOutputStream byteArrayOutputStream = new java.io.ByteArrayOutputStream();
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // 4. Mã hóa mảng byte ảnh thành chuỗi ký tự văn bản Base64
            String base64ImageString = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

            // Gộp chuỗi định dạng chuẩn để các thư viện như Glide có thể đọc trực tiếp
            String fullBase64Url = "data:image/jpeg;base64," + base64ImageString.trim().replaceAll("\\s+", "");

            // 5. Đẩy dữ liệu chữ này lên hàm cập nhật trạng thái "confirm" của Firestore
            capNhatThanhConfirmChinhThuc(name, phone, fullBase64Url);

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi mã hóa ảnh: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    private void capNhatThanhConfirmChinhThuc(String name, String phone, String billUrl) {
        WriteBatch batch = db.batch();
        int pricePerDoc = tongTien / createdDocumentIDs.size(); // Chia đều tạm thời chi phí

        for (String docID : createdDocumentIDs) {
            DocumentReference docRef = db.collection("Booking").document(docID);

            Map<String, Object> updates = new HashMap<>();
            updates.put("UserID", name);
            updates.put("phoneNumber", phone);
            updates.put("status", "confirm"); // Đổi sang xác nhận chính thức để hiện chữ trên lưới
            updates.put("totalPrice", pricePerDoc);
            updates.put("paymentStatus", "paid");
            updates.put("paymentMethod", "banking");
            updates.put("billImage", billUrl); // Lưu giữ link ảnh hóa đơn để Admin kiểm tra chéo

            batch.update(docRef, updates);
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            isBookingSuccessful = true; // Bật cờ thành công để không bị hàm hủy xóa nhầm dữ liệu
            if (pendingTimer != null) pendingTimer.cancel(); // Tắt bộ đếm giờ
            Toast.makeText(this, "Đặt sân thành công! Giao dịch của bạn đã hoàn tất.", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
    }

    private void xoaLichGiuChoTamtram() {
        WriteBatch batch = db.batch();
        for (String docID : createdDocumentIDs) {
            DocumentReference docRef = db.collection("Booking").document(docID);
            batch.delete(docRef);
        }
        batch.commit();
    }

    // Xử lý tình huống: Khách bấm nút Back, thoát ngang hoặc tắt màn hình giữa chừng
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingTimer != null) pendingTimer.cancel();
        if (!isBookingSuccessful) {
            xoaLichGiuChoTamtram(); // Khách thoát ra ngoài khi chưa xong, xóa ngay các ô giữ chỗ tạm
        }
    }

    private Map<String, ArrayList<TimeCell>> gopCaTheoSan() {
        Map<String, ArrayList<TimeCell>> slotsByCourt = new HashMap<>();
        for (TimeCell cell : chosenSlots) {
            String courtId = cell.getPhoneNumber();
            if (!slotsByCourt.containsKey(courtId)) {
                slotsByCourt.put(courtId, new ArrayList<>());
            }
            slotsByCourt.get(courtId).add(cell);
        }
        for (ArrayList<TimeCell> list : slotsByCourt.values()) {
            java.util.Collections.sort(list, (c1, c2) -> c1.getTimeLabel().compareTo(c2.getTimeLabel()));
        }
        return slotsByCourt;
    }

    private void tinhTongTienLichDat() {
        tongTien = 0; if (chosenSlots == null) return;
        for (TimeCell cell : chosenSlots) { tongTien += calculatePriceForSlot(cell.getTimeLabel()); }
    }

    private void taoMaQRThanhToan() {
        if (tongTien <= 0) return;
        String encodedInfo = (groupBookingID + "%20THANH%20TOAN%20SAN");
        String qrUrl = "https://img.vietqr.io/image/" + BANK_ID + "-" + ACCOUNT_NO + "-compact2.jpg?amount=" + tongTien + "&addInfo=" + encodedInfo + "&accountName=" + ACCOUNT_NAME.replace(" ", "%20");
        Glide.with(this).load(qrUrl).into(imgVietQR);
    }

    private void hienThiThongTinCaDat() {
        if (chosenSlots == null || chosenSlots.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("📅 Ngày chơi: ").append(bookingDate).append("\n-------------------------------------------\n");
        for (TimeCell cell : chosenSlots) { sb.append("📍 ").append(cell.getCustomerName()).append("   |   🕒 ca: ").append(cell.getTimeLabel()).append("\n"); }
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