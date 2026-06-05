package ntu.lehoangdanggia.badminton_booking;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import android.content.Intent;
import static android.app.Activity.RESULT_OK;
import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.DecimalFormat;
import android.widget.Toast;
import java.util.List;

public class CourtListActivity extends AppCompatActivity {
    private String monthlyStartDateYYYYMMDD = "";
    private String monthlyEndDateYYYYMMDD = "";
    private com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
    private com.google.firebase.firestore.ListenerRegistration firestoreListener;

    private LinearLayout layoutTimeLabels;
    private RecyclerView rvCourtTimeline;
    private HorizontalScrollView headerScroll;
    private TextView tvFilterDate, tvFilterCellWidth, tvDateLabel;

    private List<CourtRow> courtRowList = new ArrayList<>();
    private CourtTimelineAdapter timelineAdapter;
    private LinearLayout layoutQuickBookingBar;
    private TextView tvQuickBookingSummary, tvQuickBookingPrice;
    private android.widget.Button btnQuickBookingSubmit;

    private List<TimeCell> selectedCellsList = new ArrayList<>();

    private String[] timeSlots = {
            "05:00 - 05:30", "05:30 - 06:00", "06:00 - 06:30", "06:30 - 07:00",
            "07:00 - 07:30", "07:30 - 08:00", "08:00 - 08:30", "08:30 - 09:00",
            "09:00 - 09:30", "09:30 - 10:00", "10:00 - 10:30", "10:30 - 11:00",
            "11:00 - 11:30", "11:30 - 12:00", "12:00 - 12:30", "12:30 - 13:00",
            "13:00 - 13:30", "13:30 - 14:00", "14:00 - 14:30", "14:30 - 15:00",
            "15:00 - 15:30", "15:30 - 16:00", "16:00 - 16:30", "16:30 - 17:00",
            "17:00 - 17:30", "17:30 - 18:00", "18:00 - 18:30", "18:30 - 19:00",
            "19:00 - 19:30", "19:30 - 20:00", "20:00 - 20:30", "20:30 - 21:00",
            "21:00 - 21:30", "21:30 - 22:00"
    };

    private int currentCellWidthDp = 30;
    private boolean isSyncing = false;
    private androidx.activity.result.ActivityResultLauncher<Intent> bookingLauncher;

    private final String[] idTinh = {"7o8mrirBibMAop6nig3K", "CLVNJ0UEA1f9Zy809FnP", "rkRLeRKngVJ24sE4Tbex"};
    private final String[] tenTinh = {"Sân 1", "Sân 2", "Sân 3"};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_court_list);

        layoutTimeLabels = findViewById(R.id.layoutTimeLabels);
        rvCourtTimeline = findViewById(R.id.rvCourtTimeline);
        headerScroll = findViewById(R.id.headerScroll);
        tvFilterDate = findViewById(R.id.tvFilterDate);
        tvFilterCellWidth = findViewById(R.id.tvFilterCellWidth);
        tvDateLabel = findViewById(R.id.tvDateLabel);
        ImageView btnBack = findViewById(R.id.btnBack);

        layoutQuickBookingBar = findViewById(R.id.layoutQuickBookingBar);
        tvQuickBookingSummary = findViewById(R.id.tvQuickBookingSummary);
        tvQuickBookingPrice = findViewById(R.id.tvQuickBookingPrice);
        btnQuickBookingSubmit = findViewById(R.id.btnQuickBookingSubmit);
        Button btnMainMonthlyBooking = findViewById(R.id.btnMainMonthlyBooking);

        // 2. Cài đặt sự kiện: Khi click vào nút Đặt lịch thì gọi hàm trượt BottomSheet lên
        btnMainMonthlyBooking.setOnClickListener(v -> {

            // 🚀 GỌI HÀM Ở ĐÂY:
            showMonthlyBookingDialog();

        });

        bookingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        selectedCellsList.clear();
                        updateQuickBookingBar();
                        Toast.makeText(this, "Hệ thống đã cập nhật lịch đặt sân!", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnQuickBookingSubmit.setOnClickListener(v -> {
            if (selectedCellsList.isEmpty()) {
                Toast.makeText(CourtListActivity.this, "Vui lòng chọn ít nhất một ca trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // 1. Lấy ngày giờ thực tế hiện tại của hệ thống (Ngày hôm nay)
                Calendar currentSystemTime = Calendar.getInstance();
                java.text.SimpleDateFormat systemFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                String todayStr = systemFormat.format(currentSystemTime.getTime());
                java.util.Date todayDate = systemFormat.parse(todayStr); // Chuẩn Date của hôm nay

                // 2. Lấy ngày Admin đang xem trên bộ lọc và đưa về chuẩn Date
                String rawDate = tvFilterDate.getText().toString().replace(" 📅", "").trim();
                java.text.SimpleDateFormat viewFormat;
                if (rawDate.contains("/")) {
                    viewFormat = new java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault()); // Đọc được cả "6/6" và "06/06"
                } else {
                    viewFormat = new java.text.SimpleDateFormat("yyyy-M-d", java.util.Locale.getDefault());
                }
                java.util.Date viewedDate = viewFormat.parse(rawDate);

                // 3. CHỈ TIẾN HÀNH KIỂM TRA QUÁ GIỜ NẾU NGÀY ĐẶT CHÍNH LÀ NGÀY HÔM NAY
                if (viewedDate != null && viewedDate.equals(todayDate)) {
                    int currentHour = currentSystemTime.get(Calendar.HOUR_OF_DAY);
                    int currentMinute = currentSystemTime.get(Calendar.MINUTE);
                    int nowInMinutes = (currentHour * 60) + currentMinute;

                    for (TimeCell cell : selectedCellsList) {
                        String timeLabel = cell.getTimeLabel(); // Ví dụ: "05:00 - 05:30"
                        int startHour = Integer.parseInt(timeLabel.substring(0, 2));
                        int startMinute = Integer.parseInt(timeLabel.substring(3, 5));
                        int slotStartInMinutes = (startHour * 60) + startMinute;

                        if (slotStartInMinutes <= nowInMinutes) {
                            Toast.makeText(CourtListActivity.this, "⚠️ Ca [" + timeLabel + "] của ngày hôm nay đã quá giờ chơi, vui lòng bỏ chọn!", Toast.LENGTH_LONG).show();
                            return; // Chặn đứng luồng, không cho chuyển trang
                        }
                    }
                }

                // 👉 Nếu viewedDate là ngày mai, ngày kia (viewedDate.after(todayDate)) -> Bỏ qua khối IF trên và chạy thẳng xuống dưới!

            } catch (Exception e) {
                e.printStackTrace();
            }

            // ─── CHUYỂN TRANG SANG MÀN HÌNH XÁC NHẬN (CHỈ CHẠY KHI HỢP LỆ) ───
            Intent intent = new Intent(CourtListActivity.this, BookingConfirmActivity.class);
            ArrayList<TimeCell> bundleList = new ArrayList<>(selectedCellsList);
            intent.putExtra("CHOSEN_SLOTS", bundleList);
            intent.putExtra("BOOKING_DATE", tvFilterDate.getText().toString());

            bookingLauncher.launch(intent);
        });
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime()) + " 📅";
        tvFilterDate.setText(currentDate);
        if (tvDateLabel != null) {
            java.text.SimpleDateFormat labelFormat = new java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault());
            tvDateLabel.setText("Ngày\n" + labelFormat.format(calendar.getTime()));
        }

        tvFilterDate.setOnClickListener(v -> showDatePicker());
        tvFilterCellWidth.setOnClickListener(v -> showCellWidthDialog());

        initEmptyCourts();
        updateTimelineView();

        listenToFirestoreBookings(currentDate);
    }

    private void initEmptyCourts() {
        courtRowList.clear();
        for (int i = 0; i < idTinh.length; i++) {
            String idSanFirestore = idTinh[i];
            String tenHienThi = tenTinh[i];

            List<TimeCell> cells = new ArrayList<>();
            for (String slot : timeSlots) {
                cells.add(new TimeCell(slot, "Trống", "", idSanFirestore));
            }
            courtRowList.add(new CourtRow(tenHienThi, cells));
        }
    }

    private void listenToFirestoreBookings(String targetDate) {
        // 1. Chuẩn hóa ngày về định dạng YYYY-MM-DD để truy vấn Firebase
        String cleanDate = targetDate.replace(" 📅", "").trim();
        if (cleanDate.contains("/")) {
            String[] parts = cleanDate.split("/");
            cleanDate = parts[2] + "-" + parts[1] + "-" + parts[0];
        }

        if (firestoreListener != null) firestoreListener.remove();

        firestoreListener = db.collection("Booking")
                .whereEqualTo("date", cleanDate)
                .whereEqualTo("status", "confirm")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        android.util.Log.e("FirestoreError", "Lỗi kết nối Firebase: " + e.getMessage());
                        return;
                    }

                    if (snapshots == null) return;

                    // 🛠️ GIẢI PHÁP CHÍ MẠNG: Khởi tạo một lưới sạch hoàn toàn mới trong bộ nhớ đệm Local
                    List<CourtRow> localCourtRows = new ArrayList<>();
                    for (int i = 0; i < idTinh.length; i++) {
                        String idSanFirestore = idTinh[i];
                        String tenHienThi = tenTinh[i];

                        List<TimeCell> cells = new ArrayList<>();
                        for (String slot : timeSlots) {
                            cells.add(new TimeCell(slot, "Trống", "", ""));
                        }
                        localCourtRows.add(new CourtRow(tenHienThi, cells));
                    }

                    // 2. Đổ dữ liệu từ Firestore vào lưới đệm Local vừa tạo
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snapshots) {
                        String bookingID = doc.getString("bookingID");
                        String courtID = doc.getString("courtID");
                        String startTime = doc.getString("startTime");
                        String endTime = doc.getString("endTime");
                        String userID = doc.getString("UserID");
                        String customerPhone = doc.getString("phoneNumber");

                        if (customerPhone == null || customerPhone.isEmpty()) customerPhone = "N/A";
                        if (userID == null || userID.isEmpty()) userID = "Khách vãng lai";

                        if (courtID == null || startTime == null || endTime == null) continue;

                        int bStart = convertTimeToMinutes(startTime.trim());
                        int bEnd = convertTimeToMinutes(endTime.trim());

                        // Tìm hàng sân trong lưới local khớp với courtID
                        for (int i = 0; i < idTinh.length; i++) {
                            if (idTinh[i].equals(courtID)) {
                                CourtRow row = localCourtRows.get(i);

                                for (TimeCell cell : row.getTimeCells()) {
                                    // Sử dụng biểu thức chính quy để cắt chuỗi an toàn, chấp nhận cả có hoặc không có khoảng trắng xung quanh dấu gạch ngang
                                    String[] cellParts = cell.getTimeLabel().split("\\s*-\\s*");
                                    if (cellParts.length < 2) continue;

                                    int cellStart = convertTimeToMinutes(cellParts[0].trim());
                                    int cellEnd = convertTimeToMinutes(cellParts[1].trim());

                                    // Kiểm tra nếu ca giờ nằm trong khoảng thời gian đặt
                                    if (cellStart >= bStart && cellEnd <= bEnd) {
                                        cell.setStatus("Lịch ngày");
                                        cell.setCustomerName(userID.trim() + "\n" + customerPhone.trim());
                                        cell.setPhoneNumber(bookingID);
                                    }
                                }
                                break;
                            }
                        }
                    }
                    // 🛠️ GIẢI PHÁP CHÍ MẠNG: Xóa sạch lưới cũ, nạp toàn bộ lưới local sạch vào biến toàn cục
                    courtRowList.clear();
                    courtRowList.addAll(localCourtRows);

                    // Ép Adapter nhận danh sách mới và vẽ lại toàn bộ giao diện từ đầu
                    if (timelineAdapter != null) {
                        // Khi lệnh này chạy, onBindViewHolder ở Bước 1 sẽ được kích hoạt
                        // và tự động giữ vị trí cuộn cực kỳ chính xác!
                        timelineAdapter.notifyDataSetChanged();
                    }
                });
    }

    private int convertTimeToMinutes(String timeStr) {
        String[] parts = timeStr.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private void updateTimelineView() {
        buildTimelineHeaders();

        TextView tvFilterDate = findViewById(R.id.tvFilterDate);
        String rawDate = tvFilterDate.getText().toString(); // Kết quả: "23/05/2026 📅"
        String cleanDateTemp = rawDate.replace(" 📅", "").trim(); // Tạo biến tạm để biến đổi

        // Đảo chuỗi từ dd/MM/yyyy sang yyyy-MM-dd để cấu trúc so sánh thời gian chính xác
        if (cleanDateTemp.contains("/")) {
            String[] dParts = cleanDateTemp.split("/");
            cleanDateTemp = dParts[2] + "-" + dParts[1] + "-" + dParts[0];
        }

// 🎯 ĐÂY CHÍNH LÀ CHÌA KHÓA: Tạo một biến final thực sự để dùng cho Lambda phía dưới
        final String cleanDate = cleanDateTemp;
        if (timelineAdapter == null) {
            timelineAdapter = new CourtTimelineAdapter(courtRowList);
            timelineAdapter.setCellWidthDp(currentCellWidthDp);

            timelineAdapter.setOnCellClickListener((String courtName, TimeCell cell) -> {

                if ("Lịch ngày".equals(cell.getStatus())) {
                    String bID = cell.getPhoneNumber(); // Mã bookingID của cụm ca

                    // 1. Tạo View từ file layout XML custom (dialog_booking_detail.xml)
                    android.view.LayoutInflater inflater = CourtListActivity.this.getLayoutInflater();
                    android.view.View dialogView = inflater.inflate(R.layout.dialog_booking_detail, null);

                    TextView tvDialogDetails = dialogView.findViewById(R.id.tvDialogDetails);
                    ImageView imgDialogBill = dialogView.findViewById(R.id.imgDialogBill);

                    // Gán thông báo chờ trong lúc Firebase tải dữ liệu
                    tvDialogDetails.setText("Đang tải dữ liệu từ hệ thống...");

                    // 2. Truy vấn trực tiếp tài liệu dựa trên bID để lấy SĐT, khoảng ca gộp và chuỗi ảnh hóa đơn
                    db.collection("Booking").document(bID).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String user = documentSnapshot.getString("UserID");
                                    String phone = documentSnapshot.getString("phoneNumber");
                                    String startTime = documentSnapshot.getString("startTime");
                                    String endTime = documentSnapshot.getString("endTime");
                                    String date = documentSnapshot.getString("date");
                                    Long price = documentSnapshot.getLong("totalPrice");
                                    String base64Image = documentSnapshot.getString("billImage");

                                    java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
                                    String detailsText = "👤 Khách hàng: " + user + "\n"
                                            + "📞 Số điện thoại: " + phone + "\n"
                                            + "🏟️ Vị trí: " + courtName + "\n"
                                            + "📅 Ngày chơi: " + date + "\n"
                                            + "🕒 Thời gian: " + startTime + " - " + endTime + "\n"
                                            + "💰 Tổng tiền: " + (price != null ? formatter.format(price) : "0") + " vnđ";

                                    tvDialogDetails.setText(detailsText);

                                    // Giải mã chuỗi Base64 và đổ ảnh lên ImageView bằng Glide
                                    if (base64Image != null && !base64Image.isEmpty()) {
                                        com.bumptech.glide.Glide.with(CourtListActivity.this)
                                                .load(base64Image)
                                                .placeholder(android.R.drawable.progress_horizontal)
                                                .error(android.R.drawable.stat_notify_error)
                                                .into(imgDialogBill);
                                    } else {
                                        imgDialogBill.setImageResource(android.R.drawable.ic_menu_report_image);
                                    }
                                } else {
                                    tvDialogDetails.setText("Không tìm thấy thông tin đơn đặt lịch này!");
                                }
                            })
                            .addOnFailureListener(e -> {
                                tvDialogDetails.setText("Lỗi kết nối Firestore: " + e.getMessage());
                            });

                    // 3. Khởi tạo Dialog hiển thị cấu trúc custom
                    new android.app.AlertDialog.Builder(CourtListActivity.this)
                            .setView(dialogView)
                            .setPositiveButton("Hủy lịch đặt này", (dialog, which) -> {
                                // Thực hiện xóa tài liệu ra khỏi hệ thống để giải phóng ô lịch
                                db.collection("Booking").document(bID)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(CourtListActivity.this, "Đã hủy cụm lịch đặt thành công!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(err -> {
                                            Toast.makeText(CourtListActivity.this, "Lỗi hủy sân: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("Đóng", null)
                            .show();
                    return;
                }

                // 🛠️ ĐÃ SỬA: Bộ lọc phân bóc tách Ngày/Giờ chuẩn hóa cấu trúc Date
                if ("Trống".equals(cell.getStatus())) {
                    try {
                        // 1. Lấy ngày thực tế của hệ thống (Hôm nay) đưa về chuẩn Date sạch
                        Calendar currentSystemTime = Calendar.getInstance();
                        java.text.SimpleDateFormat systemFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                        String todayStr = systemFormat.format(currentSystemTime.getTime());
                        java.util.Date todayDate = systemFormat.parse(todayStr);

                        // 2. Đọc chuỗi ngày trên bộ lọc giao diện (Hỗ trợ định dạng d/M/yyyy linh hoạt)
                        String rawDateStr = tvFilterDate.getText().toString().replace(" 📅", "").trim();
                        java.text.SimpleDateFormat viewFormat;
                        if (rawDateStr.contains("/")) {
                            viewFormat = new java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault());
                        } else {
                            viewFormat = new java.text.SimpleDateFormat("yyyy-M-d", java.util.Locale.getDefault());
                        }
                        java.util.Date viewedDate = viewFormat.parse(rawDateStr);

                        if (viewedDate != null) {
                            // 🎯 TÌNH HUỐNG 1: Nếu ngày đang xem thực sự nằm trong QUÁ KHỨ
                            if (viewedDate.before(todayDate)) {
                                Toast.makeText(CourtListActivity.this, "❌ Không thể đặt sân cho ngày trong quá khứ!", Toast.LENGTH_SHORT).show();
                                return; // Chặn đứng, không cho chọn
                            }

                            // 🎯 TÌNH HUỐNG 2: Nếu ngày đang xem CHÍNH LÀ NGÀY HÔM NAY -> Check giờ đồng hồ
                            if (viewedDate.equals(todayDate)) {
                                String timeLabel = cell.getTimeLabel(); // Ví dụ: "05:00 - 05:30"
                                int startHour = Integer.parseInt(timeLabel.substring(0, 2));
                                int startMinute = Integer.parseInt(timeLabel.substring(3, 5));

                                Calendar slotStartTime = (Calendar) currentSystemTime.clone();
                                slotStartTime.set(Calendar.HOUR_OF_DAY, startHour);
                                slotStartTime.set(Calendar.MINUTE, startMinute);
                                slotStartTime.set(Calendar.SECOND, 0);

                                if (currentSystemTime.after(slotStartTime)) {
                                    Toast.makeText(CourtListActivity.this, "⏱️ Ca này đã quá giờ chơi của hôm nay, không thể chọn!", Toast.LENGTH_SHORT).show();
                                    return; // Chặn đứng, không cho chọn
                                }
                            }

                            // 🎯 TÌNH HUỐNG 3: Nếu là ngày MAI (6/6), ngày KIA... (viewedDate.after(todayDate))
                            // Code tự động đi xuyên qua bộ lọc an toàn này, cho phép tích chọn thoải mái!
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // ─── GIỮ NGUYÊN CODE XỬ LÝ CHỌN SÂN CŨ CỦA BẠN Ở DƯỚI ĐÂY ───
                if (cell.isSelected()) {
                    cell.setSelected(false);
                    cell.setPhoneNumber("");

                    for (int i = 0; i  < selectedCellsList.size(); i++) {
                        TimeCell selected = selectedCellsList.get(i);
                        if (selected.getTimeLabel().equals(cell.getTimeLabel()) && courtName.equals(selected.getCustomerName())) {
                            selectedCellsList.remove(i);
                            break;
                        }
                    }
                } else {
                    cell.setSelected(true);
                    cell.setCustomerName(courtName);

                    for (int i = 0; i < tenTinh.length; i++) {
                        if (tenTinh[i].equals(courtName)) {
                            cell.setPhoneNumber(idTinh[i]);
                            break;
                        }
                    }

                    selectedCellsList.add(cell);
                }

                for (int i = 0; i < courtRowList.size(); i++) {
                    if (courtRowList.get(i).getCourtName().equals(courtName)) {
                        timelineAdapter.notifyItemChanged(i, "UPDATE_SELECTION");
                        break;
                    }
                }

                updateQuickBookingBar();
            });

            RecyclerView.OnScrollListener synchronizedScrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (isSyncing) return;
                    isSyncing = true;

                    if (headerScroll != null) {
                        headerScroll.scrollBy(dx, 0);
                    }
                    for (RecyclerView rv : timelineAdapter.getRegisteredScrollViews()) {
                        if (rv != recyclerView) {
                            rv.scrollBy(dx, 0);
                        }
                    }
                    isSyncing = false;
                }
            };

            timelineAdapter.setOnScrollListener(synchronizedScrollListener);
            rvCourtTimeline.setLayoutManager(new LinearLayoutManager(this));
            rvCourtTimeline.setHasFixedSize(true);
            rvCourtTimeline.setAdapter(timelineAdapter);
        } else {
            timelineAdapter.setCellWidthDp(currentCellWidthDp);
            timelineAdapter.notifyDataSetChanged();
        }
    }
    private void buildTimelineHeaders() {
        if (layoutTimeLabels == null) return;
        layoutTimeLabels.removeAllViews();

        int cellWidth = (int) (currentCellWidthDp * 3.5 * getResources().getDisplayMetrics().density);

        for (String time : timeSlots) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cellWidth, LinearLayout.LayoutParams.MATCH_PARENT);
            tv.setLayoutParams(params);
            tv.setGravity(Gravity.CENTER);
            tv.setText(time);
            tv.setTextColor(android.graphics.Color.parseColor("#475569"));
            tv.setTextSize(currentCellWidthDp > 25 ? 12 : 10);
            tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tv.setBackgroundResource(R.drawable.bg_court_name_cell);
            layoutTimeLabels.addView(tv);
        }
    }

    private void showDatePicker() {
        // 1. Khởi tạo Calendar mặc định là ngày hiện tại
        final Calendar c = Calendar.getInstance();

        // 2. 🎯 ĐOẠN SỬA LỖI: Đọc ngày đang hiển thị trên ô Text để đồng bộ lên lịch
        try {
            String rawDate = tvFilterDate.getText().toString();
            String cleanDate = rawDate.replace(" 📅", "").trim(); // Ví dụ: "09/06/2026"

            if (!cleanDate.isEmpty() && cleanDate.contains("/")) {
                String[] parts = cleanDate.split("/");
                int d = Integer.parseInt(parts[0]);
                int m = Integer.parseInt(parts[1]) - 1; // Tháng trong Calendar chạy từ 0 - 11
                int y = Integer.parseInt(parts[2]);

                // Đặt lại mốc thời gian hiển thị cho đúng ngày trên ô text
                c.set(Calendar.DAY_OF_MONTH, d);
                c.set(Calendar.MONTH, m);
                c.set(Calendar.YEAR, y);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        // 3. Hiển thị DatePickerDialog dựa theo ngày đã được đồng bộ
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    tvFilterDate.setText(formattedDate + " 📅");

                    if (tvDateLabel != null) {
                        tvDateLabel.setText("Ngày\n" + String.format("%02d/%02d", selectedDay, selectedMonth + 1));
                    }

                    // Gọi lắng nghe dữ liệu mới từ Firebase cho đúng ngày được chọn
                    listenToFirestoreBookings(tvFilterDate.getText().toString());

                    // Vẽ lại giao diện lưới Timeline theo ngày mới chọn
                    updateTimelineView();

                    // Xóa sạch danh sách ca đã chọn cũ tránh việc bấm nhầm ca quá giờ của ngày cũ
                    selectedCellsList.clear();
                    updateQuickBookingBar();
                }, year, month, day);
        datePickerDialog.show();
    }    private void showCellWidthDialog() {
        String[] options = {"Nhỏ (Cột 20) 🔍", "Vừa (Cột 30) 🔎", "Lớn (Cột 40) 放大"};
        int checkedItem = 1;
        if (currentCellWidthDp == 20) checkedItem = 0;
        if (currentCellWidthDp == 40) checkedItem = 2;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chỉnh độ rộng cột giờ:");
        builder.setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
            switch (which) {
                case 0: currentCellWidthDp = 20; break;
                case 1: currentCellWidthDp = 30; break;
                case 2: currentCellWidthDp = 40; break;
            }
            tvFilterCellWidth.setText(currentCellWidthDp + " ↕");
            updateTimelineView();
            dialog.dismiss();
        });
        builder.show();
    }

    private int calculatePriceForSlot(String timeSlot) {
        if (timeSlot == null || timeSlot.isEmpty()) return 0;
        try {
            int startHour = Integer.parseInt(timeSlot.substring(0, 2));
            return (startHour >= 5 && startHour < 12) ? 65000 : (startHour >= 12 && startHour < 17) ? 70000 : 80000;
        } catch (Exception e) { return 0; }
    }

    private void updateQuickBookingBar() {
        if (selectedCellsList.isEmpty()) {
            layoutQuickBookingBar.setVisibility(android.view.View.GONE);
            return;
        }

        layoutQuickBookingBar.setVisibility(android.view.View.VISIBLE);

        List<String> uniqueCourts = new ArrayList<>();
        int totalPrice = 0;

        for (TimeCell cell : selectedCellsList) {
            String courtName = cell.getCustomerName();
            if (!uniqueCourts.contains(courtName)) {
                uniqueCourts.add(courtName);
            }
            totalPrice += calculatePriceForSlot(cell.getTimeLabel());
        }

        StringBuilder courtsBuilder = new StringBuilder();
        for (int i = 0; i < uniqueCourts.size(); i++) {
            courtsBuilder.append(uniqueCourts.get(i));
            if (i < uniqueCourts.size() - 1) {
                courtsBuilder.append(", ");
            }
        }

        double totalHours = selectedCellsList.size() * 0.5;
        String durationText = (totalHours % 1 == 0) ? (int)totalHours + " tiếng" : totalHours + " tiếng";

        DecimalFormat formatter = new DecimalFormat("#,###");
        String priceFormatted = formatter.format(totalPrice) + " vnđ";

        tvQuickBookingSummary.setText(courtsBuilder.toString() + " • " + durationText + " (" + selectedCellsList.size() + " ca)");
        tvQuickBookingPrice.setText("Tổng tiền: " + priceFormatted);
    }
    // 🎯 Hàm này phải nằm TRONG class CourtListActivity nhưng NGOÀI các hàm khác

    private void showMonthlyBookingDialog() {

        EditText edtCustomerName = findViewById(R.id.edtAdminCustomerName);
        EditText edtCustomerPhone = findViewById(R.id.edtAdminCustomerPhone);

        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.dialog_monthly_booking, null);
        bottomSheet.setContentView(v);

        TextView tvDialogStartDate = v.findViewById(R.id.tvDialogStartDate);
        TextView tvDialogEndDate = v.findViewById(R.id.tvDialogEndDate);
        CheckBox cbT2 = v.findViewById(R.id.cbT2); CheckBox cbT3 = v.findViewById(R.id.cbT3);
        CheckBox cbT4 = v.findViewById(R.id.cbT4); CheckBox cbT5 = v.findViewById(R.id.cbT5);
        CheckBox cbT6 = v.findViewById(R.id.cbT6); CheckBox cbT7 = v.findViewById(R.id.cbT7);
        CheckBox cbCN = v.findViewById(R.id.cbCN);
        Spinner spStartTime = v.findViewById(R.id.spStartTime);
        Spinner spEndTime = v.findViewById(R.id.spEndTime);
        Button btnCheckAvailableCourts = v.findViewById(R.id.btnCheckAvailableCourts);
        TextView tvAvailableCourtsTitle = v.findViewById(R.id.tvAvailableCourtsTitle);
        com.google.android.material.chip.ChipGroup chipGroupCourts = v.findViewById(R.id.chipGroupCourts);

        // 1. NẠP DỮ LIỆU KHUNG GIỜ TỪ 5G - 22G VÀO SPINNER
        List<String> hoursList = new ArrayList<>();
        for (int h = 5; h <= 22; h++) {
            hoursList.add(String.format("%02d:00", h));
            if (h != 22) hoursList.add(String.format("%02d:30", h)); // Không thêm 22:30 vì kịch khung là 22g
        }
        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, hoursList);
        spStartTime.setAdapter(spinnerAdapter);
        spEndTime.setAdapter(spinnerAdapter);

        // Mặc định chọn mốc ban đầu cho đỡ trống
        spStartTime.setSelection(0); // 05:00
        spEndTime.setSelection(2);   // 06:00

        // 2. SỰ KIỆN CHỌN NGÀY BẮT ĐẦU VÀ KẾT THÚC
        tvDialogStartDate.setOnClickListener(v1 -> {
            final Calendar c = Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                tvDialogStartDate.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year));
                monthlyStartDateYYYYMMDD = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        tvDialogEndDate.setOnClickListener(v1 -> {
            final Calendar c = Calendar.getInstance();
            new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                tvDialogEndDate.setText(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year));
                monthlyEndDateYYYYMMDD = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // 3. LOGIC KIỂM TRA SÂN TRỐNG THỰC TẾ
        btnCheckAvailableCourts.setOnClickListener(v2 -> {
            if (monthlyStartDateYYYYMMDD.isEmpty() || monthlyEndDateYYYYMMDD.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn đầy đủ Khoảng ngày!", Toast.LENGTH_SHORT).show();
                return;
            }

            String reqStart = spStartTime.getSelectedItem().toString();
            String reqEnd = spEndTime.getSelectedItem().toString();

            if (convertTimeToMinutes(reqStart) >= convertTimeToMinutes(reqEnd)) {
                Toast.makeText(this, "Giờ kết thúc phải lớn hơn giờ bắt đầu!", Toast.LENGTH_SHORT).show();
                return;
            }

            List<Integer> targetDays = new ArrayList<>();
            if (cbCN.isChecked()) targetDays.add(Calendar.SUNDAY);
            if (cbT2.isChecked()) targetDays.add(Calendar.MONDAY);
            if (cbT3.isChecked()) targetDays.add(Calendar.TUESDAY);
            if (cbT4.isChecked()) targetDays.add(Calendar.WEDNESDAY);
            if (cbT5.isChecked()) targetDays.add(Calendar.THURSDAY);
            if (cbT6.isChecked()) targetDays.add(Calendar.FRIDAY);
            if (cbT7.isChecked()) targetDays.add(Calendar.SATURDAY);

            if (targetDays.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một Thứ lặp lại!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo ra danh sách ngày cụ thể dựa trên cấu hình thứ
            List<String> activeDates = new ArrayList<>();
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(monthlyStartDateYYYYMMDD));
                java.util.Date endDateObj = sdf.parse(monthlyEndDateYYYYMMDD);

                while (!cal.getTime().after(endDateObj)) {
                    if (targetDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                        activeDates.add(sdf.format(cal.getTime()));
                    }
                    cal.add(Calendar.DATE, 1);
                }
            } catch (Exception e) { e.printStackTrace(); }

            if (activeDates.isEmpty()) {
                Toast.makeText(this, "Khoảng ngày đã chọn không chứa các Thứ bạn cần!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Thực hiện quét Firebase để lọc tìm các sân bận, từ đó suy ra sân trống
            db.collection("Booking")
                    .whereIn("date", activeDates)
                    .whereEqualTo("status", "confirm")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // Tập hợp lưu các idSân bị bận trong khung giờ yêu cầu
                        java.util.HashSet<String> busyCourtIds = new java.util.HashSet<>();

                        int rStart = convertTimeToMinutes(reqStart);
                        int rEnd = convertTimeToMinutes(reqEnd);

                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            String cID = doc.getString("courtID");
                            String bStartStr = doc.getString("startTime");
                            String bEndStr = doc.getString("endTime");

                            if (cID == null || bStartStr == null || bEndStr == null) continue;

                            int bStart = convertTimeToMinutes(bStartStr.trim());
                            int bEnd = convertTimeToMinutes(bEndStr.trim());

                            // Thuật toán kiểm tra giao nhau giữa 2 khoảng thời gian
                            if (Math.max(rStart, bStart) < Math.min(rEnd, bEnd)) {
                                busyCourtIds.add(cID); // Sân này bị cấn lịch rồi!
                            }
                        }

                        // Tiến hành hiển thị danh sách sân trống lên giao diện
                        chipGroupCourts.removeAllViews();
                        boolean hasAvailable = false;

                        // Duyệt qua mảng idTinh và tenTinh có sẵn trong Activity của bạn
                        for (int i = 0; i < idTinh.length; i++) {
                            String currentCourtId = idTinh[i];
                            String currentCourtName = tenTinh[i];

                            // Nếu ID sân này KHÔNG nằm trong danh sách bận -> Nghĩa là nó TRỐNG SUỐT CẢ THÁNG
                            if (!busyCourtIds.contains(currentCourtId)) {
                                hasAvailable = true;

                                com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(CourtListActivity.this);
                                chip.setText(currentCourtName + " ✅");
                                chip.setChipBackgroundColorResource(android.R.color.holo_green_light);
                                chip.setTextColor(android.graphics.Color.parseColor("#065F46"));
                                chip.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

                                // 🚀 SỰ KIỆN CLICK CHỌN SÂN LÀ ĐẶT LUÔN TỰ ĐỘNG
                                chip.setOnClickListener(view -> {
                                    String inputName = edtCustomerName.getText().toString().trim();
                                    String inputPhone = edtCustomerPhone.getText().toString().trim();

                                    // Ép Admin phải nhập tên để tránh việc lưu lịch trống danh tính
                                    if (inputName.isEmpty()) {
                                        Toast.makeText(CourtListActivity.this, "⚠️ Admin vui lòng nhập Tên khách hàng trước khi chọn sân!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    if (inputPhone.isEmpty()) {
                                        inputPhone = "Không có"; // Giá trị mặc định nếu không nhập SĐT
                                    }

                                    final String finalPhone = inputPhone; // Biến final để dùng trong Lambda
                                    new android.app.AlertDialog.Builder(CourtListActivity.this)
                                            .setTitle("⚡ Xác nhận đặt nhanh lịch tháng")
                                            .setMessage("Đặt cố định sân: " + currentCourtName + "\nKhách hàng: " + inputName + "\nKhung giờ: " + reqStart + " - " + reqEnd)                                            .setPositiveButton("ĐỒNG Ý", (dialog, which) -> {
                                                executeBulkMonthlyBooking(activeDates, currentCourtId, reqStart, reqEnd, inputName, finalPhone, bottomSheet);                                            })
                                            .setNegativeButton("HỦY", null)
                                            .show();
                                });

                                chipGroupCourts.addView(chip);
                            }
                        }

                        if (!hasAvailable) {
                            tvAvailableCourtsTitle.setText("❌ Rất tiếc, không còn sân nào trống hoàn toàn vào khung giờ này!");
                            tvAvailableCourtsTitle.setTextColor(android.graphics.Color.RED);
                        } else {
                            tvAvailableCourtsTitle.setText("🏟️ Các sân trống (Bấm vào sân để ĐẶT NGAY):");
                            tvAvailableCourtsTitle.setTextColor(android.graphics.Color.parseColor("#475569"));
                        }
                        tvAvailableCourtsTitle.setVisibility(View.VISIBLE);

                    }).addOnFailureListener(err -> {
                        Toast.makeText(CourtListActivity.this, "Lỗi kiểm tra lịch: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        bottomSheet.show();
    }

    private void executeBulkMonthlyBooking(List<String> listDates, String courtId, String startTime, String endTime, String customerName, String customerPhone, com.google.android.material.bottomsheet.BottomSheetDialog dialogToClose) {
        com.google.firebase.firestore.WriteBatch batch = db.batch();

        for (String targetDate : listDates) {
            String customBookingID = "BKM_ADMIN_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);

            java.util.Map<String, Object> bData = new java.util.HashMap<>();
            bData.put("bookingID", customBookingID);
            bData.put("courtID", courtId);
            bData.put("date", targetDate);
            bData.put("startTime", startTime);
            bData.put("endTime", endTime);
            bData.put("status", "confirm");
            bData.put("bookingType", "monthly");

            // 🎯 LƯU TÊN VÀ SĐT DO ADMIN NHẬP VÀO FIRESTORE
            bData.put("UserID", customerName);
            bData.put("phoneNumber", customerPhone);

            com.google.firebase.firestore.DocumentReference dRef = db.collection("Booking").document(customBookingID);
            batch.set(dRef, bData);
        }

        batch.commit().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "🎉 Đã khóa lịch tháng thành công cho khách: " + customerName, Toast.LENGTH_LONG).show();
            if (dialogToClose != null) dialogToClose.dismiss();
        }).addOnFailureListener(err -> {
            Toast.makeText(this, "Lỗi hệ thống: " + err.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}