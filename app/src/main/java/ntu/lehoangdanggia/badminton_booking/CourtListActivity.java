package ntu.lehoangdanggia.badminton_booking;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import android.content.Intent;
import static android.app.Activity.RESULT_OK;
import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

        if (timelineAdapter == null) {
            timelineAdapter = new CourtTimelineAdapter(courtRowList);
            timelineAdapter.setCellWidthDp(currentCellWidthDp);

            timelineAdapter.setOnCellClickListener((String courtName, TimeCell cell) -> {

                if ("Lịch ngày".equals(cell.getStatus())) {
                    String bID = cell.getPhoneNumber();

                    new android.app.AlertDialog.Builder(CourtListActivity.this)
                            .setTitle("Xác nhận hủy lịch đặt")
                            .setMessage("Hủy toàn bộ lượt đặt sân này của:\n" + cell.getCustomerName() + "?")
                            .setPositiveButton("Xóa lịch", (dialog, which) -> {
                                db.collection("Booking").document(bID)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(CourtListActivity.this, "Đã hủy lịch đặt thành công!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(err -> {
                                            Toast.makeText(CourtListActivity.this, "Lỗi hủy sân: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            })
                            .setNegativeButton("Đóng", null)
                            .show();
                    return;
                }

                if (!"Trống".equals(cell.getStatus())) {
                    Toast.makeText(CourtListActivity.this, "Sân này không thể chọn!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (cell.isSelected()) {
                    cell.setSelected(false);

                    // Khi bỏ chọn, trả về giá trị mặc định sạch
                    cell.setPhoneNumber("");

                    for (int i = 0; i < selectedCellsList.size(); i++) {
                        TimeCell selected = selectedCellsList.get(i);
                        if (selected.getTimeLabel().equals(cell.getTimeLabel()) && courtName.equals(selected.getCustomerName())) {
                            selectedCellsList.remove(i);
                            break;
                        }
                    }
                } else {
                    cell.setSelected(true);
                    cell.setCustomerName(courtName); // Lưu tên tạm thời ("Sân 1") để BookingConfirm hiển thị giao diện

                    // 🛠️ ĐÃ SỬA: Tìm và nạp đúng mã ID Firestore của sân vào phoneNumber của ô được click
                    for (int i = 0; i < tenTinh.length; i++) {
                        if (tenTinh[i].equals(courtName)) {
                            cell.setPhoneNumber(idTinh[i]); // Lưu mã băm (VD: "7o8mrirBibMAop6nig3K") để BookingConfirm bốc ra làm courtID
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
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    tvFilterDate.setText(formattedDate + " 📅");

                    if (tvDateLabel != null) {
                        tvDateLabel.setText("Ngày\n" + String.format("%02d/%02d", selectedDay, selectedMonth + 1));
                    }

                    listenToFirestoreBookings(tvFilterDate.getText().toString());
                }, year, month, day);
        datePickerDialog.show();
    }

    private void showCellWidthDialog() {
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
}