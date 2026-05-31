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

        // 🛠️ ĐÃ SỬA: Bỏ xóa text tvStatus, chỉ cần ẩn layout thông tin đi khi ô trống
        holder.layoutBookedInfo.setVisibility(View.GONE);

        if ("Trống".equals(cell.getStatus())) {
            holder.itemView.setBackgroundColor(Color.WHITE);
        } else {
            // 1. Đổi màu nền theo trạng thái hệ thống
            if ("Cố định".equals(cell.getStatus())) {
                holder.itemView.setBackgroundColor(Color.parseColor("#38BDF8")); // Xanh dương
            } else {
                holder.itemView.setBackgroundColor(Color.parseColor("#10B981")); // Xanh lá đặt lịch
            }

            // 2. Hiện thị layout chứa thông tin khách hàng lên
            holder.layoutBookedInfo.setVisibility(View.VISIBLE);

            // 3. Tách chuỗi Tên và SĐT dựa vào dấu xuống dòng "\n"
            String rawInfo = cell.getCustomerName();
            if (rawInfo != null && rawInfo.contains("\n")) {
                String[] parts = rawInfo.split("\n");
                holder.tvCustomerName.setText(parts[0]); // Đổ vào ô tên công khai
                holder.tvPhone.setText(parts[1]);        // Đổ vào ô SĐT công khai
            } else {
                holder.tvCustomerName.setText(rawInfo);
                holder.tvPhone.setText("");
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