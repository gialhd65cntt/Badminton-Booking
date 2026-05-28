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

    private String[] timeSlots = {
            "05:00 - 05:30", "05:30 - 06:00", "06:00 - 06:30", "06:30 - 07:00",
            "07:00 - 07:30", "07:30 - 08:00", "08:00 - 08:30", "08:30 - 09:00",
            "09:00 - 09:30", "09:30 - 10:00", "10:00 - 10:30", "10:30 - 11:00"
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
                String timeRange = cell.getTimeRangeString();

                if ("Trống".equals(cell.getStatus())) {
                    // Tính tiền dựa vào khung giờ
                    int price = calculatePriceForSlot(timeRange);
                    DecimalFormat formatter = new DecimalFormat("#,###");
                    String priceFormatted = formatter.format(price) + "đ";

                    // Hiển thị Dialog xác nhận đặt sân
                    new AlertDialog.Builder(CourtListActivity.this)
                            .setTitle("Xác nhận chọn sân")
                            .setMessage("Bạn đang chọn đặt lịch với thông tin:\n\n"
                                    + "📍 Sân: " + courtName + "\n"
                                    + "🕒 Thời gian: " + timeRange + "\n"
                                    + "💰 Giá ca này: " + priceFormatted)
                            .setPositiveButton("Tiếp tục đặt", (dialog, which) -> {
                                Toast.makeText(CourtListActivity.this,
                                        "Đang chuyển đến màn hình điền thông tin phiếu đặt...",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Hủy", null)
                            .show();
                } else {
                    // Nếu ô đã được đặt, hiện thông tin chi tiết lịch đặt
                    new AlertDialog.Builder(CourtListActivity.this)
                            .setTitle("Thông tin lịch đặt")
                            .setMessage("Sân: " + courtName + "\n"
                                    + "Thời gian: " + timeRange + "\n"
                                    + "Trạng thái: " + cell.getStatus() + "\n" // Thêm () vào đây
                                    + "Khách hàng: " + cell.getCustomerName() + "\n" // Thêm () vào đây
                                    + "Số điện thoại: " + cell.getPhoneNumber()) // Thêm () vào đây
                            .setPositiveButton("Đóng", null)
                            .show();
                }
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
        cellsCourt1.add(new TimeCell("05:00 - 05:30", "Trống", "", ""));
        cellsCourt1.add(new TimeCell("05:30 - 06:00", "Trống", "", ""));
        cellsCourt1.add(new TimeCell("06:00 - 06:30", "Lịch ngày", "Khoa", "0123"));
        cellsCourt1.add(new TimeCell("06:30 - 07:00", "Lịch ngày", "Khoa", "0123"));
        cellsCourt1.add(new TimeCell("07:00 - 07:30", "Cố định", "Tú", "0366"));
        cellsCourt1.add(new TimeCell("07:30 - 08:00", "Lịch ngày", "Tú", ""));
        cellsCourt1.add(new TimeCell("08:00 - 08:30", "Trống", "", ""));
        cellsCourt1.add(new TimeCell("08:30 - 09:00", "Trống", "", ""));
        cellsCourt1.add(new TimeCell("09:00 - 09:30", "Trống", "", ""));
        cellsCourt1.add(new TimeCell("09:30 - 10:00", "Trống", "", ""));
        cellsCourt1.add(new TimeCell("10:00 - 10:30", "Trống", "", ""));
        cellsCourt1.add(new TimeCell("10:30 - 11:00", "Trống", "", ""));
        cellsCourt1.add(new TimeCell("11:00 - 11:30", "Trống", "", ""));

        // 2. Tự động tạo dữ liệu trống chạy theo mảng timeSlots cho Sân 2
        List<TimeCell> cellsCourt2 = new ArrayList<>();
        for (String slot : timeSlots) {
            cellsCourt2.add(new TimeCell(slot, "Trống", "", ""));
        }

        // 3. Nạp tất cả vào danh sách hàng của CourtRow
        courtRowList.add(new CourtRow("Sân 1", cellsCourt1));
        courtRowList.add(new CourtRow("Sân 2", cellsCourt2));

        // Sử dụng bộ nhớ độc lập cho Sân 3 và Sân 4 để tránh lỗi thay đổi dữ liệu chéo nhau
        courtRowList.add(new CourtRow("Sân 3", new ArrayList<>(cellsCourt2)));
        courtRowList.add(new CourtRow("Sân 4", new ArrayList<>(cellsCourt2)));
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
}