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
                cellClickListener.onCellClick(cell); // Kích hoạt callback truyền đi
            }
        });

        // Đổi màu nền ô theo trạng thái giống ảnh mẫu
        if ("Trống".equals(cell.getStatus())) {
            holder.itemView.setBackgroundColor(Color.WHITE);
            holder.tvStatus.setText(""); // Ô trống thì để trống chữ hoặc hiện chữ "Trống" tùy bạn
        } else {
            // Nếu là "Lịch ngày" hoặc "Cố định" -> Đổi màu nền tương ứng
            if ("Cố định".equals(cell.getStatus())) {
                holder.itemView.setBackgroundColor(Color.parseColor("#38BDF8")); // Xanh dương
            } else {
                holder.itemView.setBackgroundColor(Color.parseColor("#10B981")); // Xanh lá đặt lịch
            }

            // HIỂN THỊ TÊN VÀ SĐT TRỰC TIẾP LÊN Ô CHO ADMIN XEM
            String displayInfo = cell.getCustomerName() + "\n" + cell.getPhoneNumber();
            holder.tvStatus.setText(displayInfo);
            holder.tvStatus.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);

        }

        if (cell.isSelected()) {
            holder.itemView.setBackgroundColor(Color.parseColor("#F59E0B")); // Màu cam đang chọn
        }

        // Giữ nguyên sự kiện holder.itemView.setOnClickListener...
    }

    @Override
    public int getItemCount() { return cellList.size(); }

    public static class CellViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus;

        LinearLayout layoutBookedInfo;
        TextView tvCustomerName, tvPhone;

        public CellViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutBookedInfo = itemView.findViewById(R.id.layoutBookedInfo);
            tvCustomerName = itemView.findViewById(R.id.tvCellCustomerName);
            tvPhone = itemView.findViewById(R.id.tvCellPhone);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}