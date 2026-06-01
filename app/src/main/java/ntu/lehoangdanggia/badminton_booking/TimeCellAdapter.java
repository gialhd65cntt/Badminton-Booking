package ntu.lehoangdanggia.badminton_booking;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TimeCellAdapter extends RecyclerView.Adapter<TimeCellAdapter.CellViewHolder> {
    public interface OnCellClickListener {
        void onCellClick(TimeCell cell);
    }

    private OnCellClickListener cellClickListener;

    public void setOnCellClickListener(OnCellClickListener listener) {
        this.cellClickListener = listener;
    }
    private int cellWidthDp = 30;
    private List<TimeCell> cellList;

    public TimeCellAdapter(List<TimeCell> cellList) {
        this.cellList = cellList;
    }
    public void setCellWidthDp(int cellWidthDp) {
        this.cellWidthDp = cellWidthDp;
    }

    @NonNull
    @Override
    public CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_cell, parent, false);
        int pixelWidth = (int) (cellWidthDp * 3.5 * parent.getContext().getResources().getDisplayMetrics().density);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(pixelWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(lp);
        return new CellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CellViewHolder holder, int position) {
        TimeCell cell = cellList.get(position);

        holder.itemView.setOnClickListener(v -> {
            if (cellClickListener != null) {
                cellClickListener.onCellClick(cell);
            }
        });

        // 1. Reset sạch giao diện của ô về mặc định ban đầu để tránh lỗi lưu đệm Holder cũ
        holder.layoutBookedInfo.setVisibility(View.GONE);
        holder.tvCustomerName.setText("");
        holder.tvPhone.setText("");

        // 2. Kiểm tra trạng thái để nhuộm màu nền
        if ("Trống".equals(cell.getStatus())) {
            holder.itemView.setBackgroundColor(Color.WHITE);
        } else {
            // Tô màu nền dựa theo trạng thái đặt
            if ("Cố định".equals(cell.getStatus())) {
                holder.itemView.setBackgroundColor(Color.parseColor("#38BDF8")); // Xanh dương
            } else {
                holder.itemView.setBackgroundColor(Color.parseColor("#10B981")); // Xanh lá ("Lịch ngày")
            }

            // 3. XỬ LÝ HIỂN THỊ CHỮ TRẮNG (BẮT BUỘC HIỆN)
            String rawInfo = cell.getCustomerName();

            // Log ra Logcat để bạn dễ giám sát tiến trình đổ dữ liệu
            android.util.Log.d("HIEN_THI_CHU", "Ô " + cell.getTimeLabel() + " có chuỗi thông tin: '" + rawInfo + "'");

            // Bật layout chứa chữ lên
            holder.layoutBookedInfo.setVisibility(View.VISIBLE);

            if (rawInfo != null && !rawInfo.trim().isEmpty()) {
                if (rawInfo.contains("\n")) {
                    String[] parts = rawInfo.split("\n");

                    // Phòng hờ chuỗi null ma từ Firebase
                    String name = (parts[0] == null || "null".equals(parts[0])) ? "Khách za" : parts[0];
                    String phone = (parts[1] == null || "null".equals(parts[1])) ? "" : parts[1];

                    holder.tvCustomerName.setText(name);
                    holder.tvPhone.setText(phone);
                } else {
                    // Nếu chuỗi không có dấu xuống dòng, hiện thẳng toàn bộ chuỗi lên ô tên
                    holder.tvCustomerName.setText(rawInfo);
                    holder.tvPhone.setText("");
                }
            } else {
                // Cứu nguy: Nếu dữ liệu bị rỗng ma, vẫn ép hiện chữ để admin biết ô này đã có chủ
                holder.tvCustomerName.setText("Đã đặt");
                holder.tvPhone.setText(cell.getPhoneNumber() != null ? "Hệ thống" : "");
            }
        }

        // Nếu ô đang được Admin chạm chọn (Màu cam)
        if (cell.isSelected()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#F59E0B"));
        }
    }

    @Override
    public int getItemCount() { return cellList.size(); }

    public static class CellViewHolder extends RecyclerView.ViewHolder {
        // 🛠️ ĐÃ SỬA: Loại bỏ thuộc tính tvStatus dư thừa khỏi ViewHolder
        LinearLayout layoutBookedInfo;
        TextView tvCustomerName, tvPhone;

        public CellViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutBookedInfo = itemView.findViewById(R.id.layoutBookedInfo);
            tvCustomerName = itemView.findViewById(R.id.tvCellCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCellPhone);
        }
    }
}