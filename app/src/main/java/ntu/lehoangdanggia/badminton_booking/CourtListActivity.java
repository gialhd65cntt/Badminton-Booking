package ntu.lehoangdanggia.badminton_booking;

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
import java.text.DecimalFormat; // Fix lỗi Cannot resolve symbol 'DecimalFormat'
import android.widget.Toast;
import java.util.List;

public class CourtListActivity extends AppCompatActivity {


    private LinearLayout layoutTimeLabels;
    private RecyclerView rvCourtTimeline;
    private HorizontalScrollView headerScroll;
    private TextView tvFilterDate, tvFilterCellWidth, tvDateLabel;

    private List<CourtRow> courtRowList;
    private CourtTimelineAdapter timelineAdapter;
    private LinearLayout layoutQuickBookingBar;
    private TextView tvQuickBookingSummary, tvQuickBookingPrice;
    private android.widget.Button btnQuickBookingSubmit;

    // Danh sách chứa các ô người dùng đang Click chọn
    private List<TimeCell> selectedCellsList = new ArrayList<>();
    // Biến lưu tên sân đang chọn (để kiểm tra xem khách có chọn lộn sang sân khác không)
    private String selectedCourtName = "";
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

    // Biến lưu độ rộng ô (mặc định ban đầu là 30)
    private int currentCellWidthDp = 30;
    private boolean isSyncing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_court_list);

        // Ánh xạ các View
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

        btnQuickBookingSubmit.setOnClickListener(v -> {
            // Logic chuyển sang màn hình hóa đơn với selectedCellsList
            Toast.makeText(this, "Chuyển tới thanh toán " + selectedCellsList.size() + " ca!", Toast.LENGTH_SHORT).show();
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 1. XỬ LÝ CHỌN NGÀY
        tvFilterDate.setOnClickListener(v -> showDatePicker());

        // 2. XỬ LÝ CHỈNH ĐỘ TO NHỎ CỦA LƯỚI
        tvFilterCellWidth.setOnClickListener(v -> showCellWidthDialog());

        // Khởi tạo khung hình ban đầu
        updateTimelineView();
    }

    // Hàm dựng và cập nhật lại toàn bộ giao diện khi có thay đổi độ to nhỏ

    private void updateTimelineView() {
        // Vẽ lại thanh tiêu đề giờ ở trên
        buildTimelineHeaders();

        // Nạp data giả lập (Nếu có data thật từ Firebase thì nạp ở đây)
        if (courtRowList == null) {
            setupFakeData();
        }

        // Khởi tạo hoặc cập nhật Adapter
        if (timelineAdapter == null) {
            timelineAdapter = new CourtTimelineAdapter(courtRowList);

            // Truyền kích thước ô vào cho adapter quản lý
            timelineAdapter.setCellWidthDp(currentCellWidthDp);

            timelineAdapter.setOnCellClickListener((String courtName, TimeCell cell) -> {
                // Nếu ô đã có người đặt rồi thì không cho chọn
                if (!"Trống".equals(cell.getStatus())) {
                    Toast.makeText(CourtListActivity.this, "Sân này đã có người đặt!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // KHÓA SÂN: Tránh việc người dùng bấm nhầm Ca 1 của Sân 1 và Ca 2 của Sân 2 cùng lúc
                if (!selectedCellsList.isEmpty() && !selectedCourtName.equals(courtName)) {
                    Toast.makeText(CourtListActivity.this, "Vui lòng chỉ chọn các ca trên cùng một sân!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // XỬ LÝ CHỌN / HUỶ CHỌN (Toggle)
                if (cell.isSelected()) {
                    // Nếu đang chọn rồi -> Bấm lại thì hủy chọn
                    cell.setSelected(false);
                    selectedCellsList.remove(cell);
                } else {
                    // Nếu chưa chọn -> Thêm vào danh sách giỏ hàng
                    cell.setSelected(true);
                    selectedCellsList.add(cell);
                    selectedCourtName = courtName; // Ghim tên sân hiện tại lại
                }

                // Báo cho Adapter vẽ lại ô vừa click để đổi màu (Ví dụ: Đổi sang màu cam/vàng nhạt đang chọn)
                for (int i = 0; i < courtRowList.size(); i++) {
                    if (courtRowList.get(i).getCourtName().equals(courtName)) {
                        timelineAdapter.notifyItemChanged(i); // Chỉ vẽ lại hàng sân này, giữ nguyên vị trí cuộn giờ!
                        break;
                    }
                }

                // CẬP NHẬT GIAO DIỆN THANH THÔNG BÁO DƯỚI ĐÁY
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
            // Nếu adapter đã có, chỉ cần cập nhật lại kích thước và báo vẽ lại dữ liệu
            timelineAdapter.setCellWidthDp(currentCellWidthDp);
            timelineAdapter.notifyDataSetChanged();
        }
    }

    private void buildTimelineHeaders() {
        if (layoutTimeLabels == null) return;
        layoutTimeLabels.removeAllViews();

        // Tính pixel dựa trên biến cấu hình kích thước linh hoạt cuat ô
        int cellWidth = (int) (currentCellWidthDp * 3.5 * getResources().getDisplayMetrics().density);

        for (String time : timeSlots) {
            TextView tv = new TextView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cellWidth, LinearLayout.LayoutParams.MATCH_PARENT);
            tv.setLayoutParams(params);
            tv.setGravity(Gravity.CENTER);
            tv.setText(time);
            tv.setTextColor(android.graphics.Color.parseColor("#475569"));
            tv.setTextSize(currentCellWidthDp > 25 ? 12 : 10); // Tự thu nhỏ chữ nếu ô quá nhỏ
            tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            tv.setBackgroundResource(R.drawable.bg_court_name_cell);
            layoutTimeLabels.addView(tv);
        }
    }

    // Hộp thoại hiển thị chọn Ngày
    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    tvFilterDate.setText(formattedDate + " 📅");

                    // Cập nhật luôn ô nhãn "Thứ / Ngày" nhỏ ở góc lưới cho đồng bộ
                    if (tvDateLabel != null) {
                        tvDateLabel.setText("Ngày\n" + String.format("%02d/%02d", selectedDay, selectedMonth + 1));
                    }

                    // Ở đây bạn có thể gọi hàm load dữ liệu sân từ Firebase theo ngày vừa chọn
                }, year, month, day);
        datePickerDialog.show();
    }

    // Hộp thoại hiển thị chọn Độ rộng lưới (Thu nhỏ / Phóng to)
    private void showCellWidthDialog() {
        String[] options = {"Nhỏ (Cột 20) 🔍", "Vừa (Cột 30) 🔎", "Lớn (Cột 40) 放大"};
        int checkedItem = 1; // Mặc định hiển thị là Vừa (30)
        if (currentCellWidthDp == 20) checkedItem = 0;
        if (currentCellWidthDp == 40) checkedItem = 2;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chỉnh độ rộng cột giờ:");
        builder.setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
            switch (which) {
                case 0:
                    currentCellWidthDp = 20;
                    break;
                case 1:
                    currentCellWidthDp = 30;
                    break;
                case 2:
                    currentCellWidthDp = 40;
                    break;
            }
            // Cập nhật chữ trên bộ lọc
            tvFilterCellWidth.setText(currentCellWidthDp + " ↕");

            // Vẽ lại toàn bộ giao diện bảng theo kích cỡ mới ngay lập tức
            updateTimelineView();
            dialog.dismiss();
        });
        builder.show();
    }

    private void setupFakeData() {
        courtRowList = new ArrayList<>();

        // 1. Giả lập dữ liệu cho Sân 1 khớp chính xác với định dạng khoảng giờ mới
        List<TimeCell> cellsCourt1 = new ArrayList<>();
        for (String slot : timeSlots) {
            // Fix cứng một vài ca đã đặt ở Sân 1 để test giao diện
            if (slot.equals("06:00 - 06:30") || slot.equals("06:30 - 07:00")) {
                cellsCourt1.add(new TimeCell(slot, "Lịch ngày", "Khoa", "0123"));
            } else if (slot.equals("07:00 - 07:30")) {
                cellsCourt1.add(new TimeCell(slot, "Cố định", "Tú", "0366"));
            } else if (slot.equals("07:30 - 08:00")) {
                cellsCourt1.add(new TimeCell(slot, "Lịch ngày", "Tú", ""));
            } else {
                // Các ca còn lại của Sân 1 là Trống
                cellsCourt1.add(new TimeCell(slot, "Trống", "", ""));
            }
        }
        courtRowList.add(new CourtRow("Sân 1", cellsCourt1));

        // ---- 2. TỰ ĐỘNG TẠO DỮ LIỆU TRỐNG CHO SÂN 2 ----
        List<TimeCell> cellsCourt2 = new ArrayList<>();
        for (String slot : timeSlots) {
            cellsCourt2.add(new TimeCell(slot, "Trống", "", ""));
        }
        courtRowList.add(new CourtRow("Sân 2", cellsCourt2));

        // ---- 3. TỰ ĐỘNG TẠO DỮ LIỆU TRỐNG CHO SÂN 3 ----
        List<TimeCell> cellsCourt3 = new ArrayList<>();
        for (String slot : timeSlots) {
            cellsCourt3.add(new TimeCell(slot, "Trống", "", ""));
        }
        courtRowList.add(new CourtRow("Sân 3", cellsCourt3));

        // ---- 4. TỰ ĐỘNG TẠO DỮ LIỆU TRỐNG CHO SÂN 4 ----
        List<TimeCell> cellsCourt4 = new ArrayList<>();
        for (String slot : timeSlots) {
            cellsCourt4.add(new TimeCell(slot, "Trống", "", ""));
        }
        courtRowList.add(new CourtRow("Sân 4", cellsCourt4));
    }
    private int calculatePriceForSlot(String timeSlot) {
        if (timeSlot == null || timeSlot.isEmpty()) return 0;

        // Ví dụ: Lấy ra mốc giờ bắt đầu để xét khung (Ví dụ chuỗi "05:00 - 05:30" lấy ra "05")
        String startHourStr = timeSlot.substring(0, 2);
        int startHour = Integer.parseInt(startHourStr);

        // Bảng tính giá tiền (Đơn vị: VNĐ cho mỗi 30 phút)
        if (startHour >= 5 && startHour < 12) {
            return 65000; // Ca sáng sớm (5g - 8g): 30k / 30 phút (tương đương 60k/giờ)
        } else if (startHour >= 12 && startHour < 17) {
            return 70000; // Ca sáng - trưa thấp điểm (8g - 16g): 25k / 30 phút
        } else {
            return 80000; // Ca chiều tối cao điểm (16g - 22g): 40k / 30 phút (tương đương 80k/giờ)
        }
    }
    private void updateQuickBookingBar() {
        if (selectedCellsList.isEmpty()) {
            layoutQuickBookingBar.setVisibility(android.view.View.GONE);
            selectedCourtName = ""; // Giải phóng khóa sân
            return;
        }

        layoutQuickBookingBar.setVisibility(android.view.View.VISIBLE);

        // 1. Tính tổng thời gian (Mỗi ô mặc định là 30 phút = 0.5 giờ)
        double totalHours = selectedCellsList.size() * 0.5;
        String durationText = (totalHours % 1 == 0) ? (int)totalHours + " tiếng" : totalHours + " tiếng";

        // 2. Tính tổng tiền của tất cả các ô cộng lại
        int totalPrice = 0;
        for (TimeCell cell : selectedCellsList) {
            totalPrice += calculatePriceForSlot(cell.getTimeLabel());
        }

        // Định dạng số tiền
        DecimalFormat formatter = new DecimalFormat("#,###");
        String priceFormatted = formatter.format(totalPrice) + " vnđ";

        // 3. Hiển thị lên màn hình đúng định dạng yêu cầu
        tvQuickBookingSummary.setText(selectedCourtName + " • " + durationText + " (" + selectedCellsList.size() + " ca)");
        tvQuickBookingPrice.setText("Tổng tiền: " + priceFormatted);
    }

}