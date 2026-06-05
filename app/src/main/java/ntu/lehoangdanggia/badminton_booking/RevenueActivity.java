package ntu.lehoangdanggia.badminton_booking;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class RevenueActivity extends AppCompatActivity {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private TextView tvSelectedTimeLabel, tvTotalRevenue;
    private Button btnChangeTime;
    private Spinner spinnerFilterType;
    private RecyclerView rvRevenueHistory;

    private RevenueAdapter adapter;
    private ArrayList<Map<String, Object>> allConfirmBookings = new ArrayList<>();
    private ArrayList<Map<String, Object>> filteredList = new ArrayList<>();

    private Calendar currentCalendar = Calendar.getInstance();
    private int filterMode = 0; // 0: Ngày, 1: Tuần, 2: Tháng, 3: Năm
    private DecimalFormat moneyFormatter = new DecimalFormat("#,### VNĐ");
    private SimpleDateFormat firestoreDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue);

        tvSelectedTimeLabel = findViewById(R.id.tvSelectedTimeLabel);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        btnChangeTime = findViewById(R.id.btnChangeTime);
        spinnerFilterType = findViewById(R.id.spinnerFilterType);
        rvRevenueHistory = findViewById(R.id.rvRevenueHistory);

        rvRevenueHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RevenueAdapter(filteredList);
        rvRevenueHistory.setAdapter(adapter);

        // Lắng nghe sự kiện đổi chế độ lọc trên Spinner
        spinnerFilterType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterMode = position;
                capNhatGiaoDienThoiGian();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Nút bấm thay đổi mốc thời gian xem doanh thu
        btnChangeTime.setOnClickListener(v -> {
            DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                currentCalendar.set(Calendar.YEAR, year);
                currentCalendar.set(Calendar.MONTH, month);
                currentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                capNhatGiaoDienThoiGian();
            }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        // Tải toàn bộ dữ liệu đơn hàng có trạng thái "confirm" về bộ nhớ đệm một lần duy nhất để tính toán mượt mà
        taiToanBoDuLieuConfirm();
    }

    private void taiToanBoDuLieuConfirm() {
        db.collection("Booking")
                .whereEqualTo("status", "confirm")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allConfirmBookings.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        allConfirmBookings.add(doc.getData());
                    }
                    // Đọc xong dữ liệu gốc thì tiến hành phân loại hiển thị lần đầu
                    capNhatGiaoDienThoiGian();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi tải lịch sử: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void capNhatGiaoDienThoiGian() {
        switch (filterMode) {
            case 0: // THEO NGÀY
                String strDate = firestoreDateFormat.format(currentCalendar.getTime());
                tvSelectedTimeLabel.setText("📅 Ngày: " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(currentCalendar.getTime()));
                locVaTinhDoanhThu(strDate, "NGAY");
                break;

            case 1: // THEO TUẦN
                Calendar firstDayOfWeek = (Calendar) currentCalendar.clone();
                firstDayOfWeek.set(Calendar.DAY_OF_WEEK, firstDayOfWeek.getFirstDayOfWeek());
                Calendar lastDayOfWeek = (Calendar) firstDayOfWeek.clone();
                lastDayOfWeek.add(Calendar.DAY_OF_YEAR, 6);

                SimpleDateFormat weekFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                tvSelectedTimeLabel.setText("📅 Tuần: " + weekFormat.format(firstDayOfWeek.getTime()) + " Từ " + weekFormat.format(firstDayOfWeek.getTime()) + " Đến " + weekFormat.format(lastDayOfWeek.getTime()));
                locVaTinhDoanhThuTuan(firstDayOfWeek.getTime(), lastDayOfWeek.getTime());
                break;

            case 2: // THEO THÁNG
                int month = currentCalendar.get(Calendar.MONTH) + 1;
                int yearM = currentCalendar.get(Calendar.YEAR);
                tvSelectedTimeLabel.setText("📅 Tháng: " + (month < 10 ? "0" + month : month) + "/" + yearM);
                String prefixMonth = yearM + "-" + (month < 10 ? "0" + month : month);
                locVaTinhDoanhThu(prefixMonth, "THANG");
                break;


        }
    }

    // Xử lý lọc chuỗi so sánh khớp (Dành cho Ngày, Tháng, Năm)
    private void locVaTinhDoanhThu(String targetText, String type) {
        filteredList.clear();
        int tongDoanhThu = 0;

        for (Map<String, Object> booking : allConfirmBookings) {
            String dateStr = (String) booking.get("date"); // Định dạng yyyy-MM-dd
            if (dateStr == null) continue;

            boolean phuHop = false;
            if (type.equals("NGAY") && dateStr.equals(targetText)) phuHop = true;
            else if (type.equals("THANG") && dateStr.startsWith(targetText)) phuHop = true;
            else if (type.equals("NAM") && dateStr.startsWith(targetText)) phuHop = true;

            if (phuHop) {
                filteredList.add(booking);
                Long price = (Long) booking.get("totalPrice");
                if (price != null) tongDoanhThu += price.intValue();
            }
        }

        tvTotalRevenue.setText(moneyFormatter.format(tongDoanhThu));
        adapter.notifyDataSetChanged();
    }

    // Xử lý lọc khoảng thời gian nằm giữa (Dành cho bộ lọc Tuần)
    private void locVaTinhDoanhThuTuan(Date start, Date end) {
        filteredList.clear();
        int tongDoanhThu = 0;

        for (Map<String, Object> booking : allConfirmBookings) {
            String dateStr = (String) booking.get("date");
            if (dateStr == null) continue;

            try {
                Date bookingDate = firestoreDateFormat.parse(dateStr);
                // Nếu ngày chơi nằm trong khoảng từ đầu tuần tới cuối tuần
                if (bookingDate != null && !bookingDate.before(start) && !bookingDate.after(end)) {
                    filteredList.add(booking);
                    Long price = (Long) booking.get("totalPrice");
                    if (price != null) tongDoanhThu += price.intValue();
                }
            } catch (Exception ignored) {}
        }

        tvTotalRevenue.setText(moneyFormatter.format(tongDoanhThu));
        adapter.notifyDataSetChanged();
    }

    // ─── CLASS ADAPTER ĐỂ ĐỔ DỮ LIỆU RECYCLERVIEW ───
    private class RevenueAdapter extends RecyclerView.Adapter<RevenueAdapter.RevViewHolder> {
        private ArrayList<Map<String, Object>> list;
        public RevenueAdapter(ArrayList<Map<String, Object>> list) { this.list = list; }

        @Override
        public RevViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_revenue_history, parent, false);
            return new RevViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RevViewHolder holder, int position) {
            Map<String, Object> item = list.get(position);
            holder.tvUser.setText("Khách: " + item.get("UserID"));
            holder.tvTime.setText("Giờ: " + item.get("startTime") + " - " + item.get("endTime") + " (" + item.get("date") + ")");
            holder.tvDocID.setText("Mã đơn: " + item.get("bookingID"));

            Long price = (Long) item.get("totalPrice");
            holder.tvPrice.setText("+" + moneyFormatter.format(price != null ? price : 0));
        }

        @Override
        public int getItemCount() { return list.size(); }

        class RevViewHolder extends RecyclerView.ViewHolder {
            TextView tvUser, tvTime, tvDocID, tvPrice;
            public RevViewHolder(android.view.View iv) {
                super(iv);
                tvUser = iv.findViewById(R.id.tvItemUser);
                tvTime = iv.findViewById(R.id.tvItemTime);
                tvDocID = iv.findViewById(R.id.tvItemDocID);
                tvPrice = iv.findViewById(R.id.tvItemPrice);
            }
        }
    }
}