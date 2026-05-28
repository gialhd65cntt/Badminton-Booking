package ntu.lehoangdanggia.badminton_booking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class CourtTimelineAdapter extends RecyclerView.Adapter<CourtTimelineAdapter.RowViewHolder> {

    private int cellWidthDp = 30; // Biến lưu kích thước ô nhận từ Activity

    public void setCellWidthDp(int cellWidthDp) {
        this.cellWidthDp = cellWidthDp;
    }

    private List<CourtRow> courtRows;
    // Danh sách lưu trữ các RecyclerView con đang hiển thị trên màn hình
    private List<RecyclerView> registeredScrollViews = new ArrayList<>();
    private RecyclerView.OnScrollListener globalScrollListener;

    public CourtTimelineAdapter(List<CourtRow> courtRows) {
        this.courtRows = courtRows;
    }

    // Hàm dùng để truyền bộ lắng nghe cuộn đồng bộ từ Activity vào từng dòng sân
    public void setOnScrollListener(RecyclerView.OnScrollListener listener) {
        this.globalScrollListener = listener;
    }

    // Hàm lấy ra toàn bộ các RecyclerView con đang hoạt động để Activity đồng bộ hóa
    public List<RecyclerView> getRegisteredScrollViews() {
        return registeredScrollViews;
    }
    public interface OnCellClickListener {
        void onCellClick(String courtName, TimeCell cell); // Đảm bảo ở đây là TimeCell chứ không phải Object nhé!
    }

    private OnCellClickListener cellClickListener;

    public void setOnCellClickListener(OnCellClickListener listener) {
        this.cellClickListener = listener;
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_court_timeline, parent, false);
        return new RowViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {

        CourtRow row = courtRows.get(position);
        holder.tvCourtName.setText(row.getCourtName());

        TimeCellAdapter cellAdapter = new TimeCellAdapter(row.getTimeCells());
        cellAdapter.setCellWidthDp(cellWidthDp);
        cellAdapter.setOnCellClickListener(cell -> {
            if (cellClickListener != null) {
                cellClickListener.onCellClick(row.getCourtName(), cell);
            }
        });
        holder.rvTimeCells.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        holder.rvTimeCells.setAdapter(cellAdapter);

        // Xóa bộ lắng nghe cũ trước khi đăng ký để tránh trùng lặp sự kiện khi cuộn dọc
        if (globalScrollListener != null) {
            holder.rvTimeCells.removeOnScrollListener(globalScrollListener);
            holder.rvTimeCells.addOnScrollListener(globalScrollListener);
        }

        // Lưu RecyclerView này vào danh sách quản lý
        if (!registeredScrollViews.contains(holder.rvTimeCells)) {
            registeredScrollViews.add(holder.rvTimeCells);
        }
    }

    @Override
    public int getItemCount() {
        return courtRows != null ? courtRows.size() : 0;
    }

    public static class RowViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourtName;
        RecyclerView rvTimeCells;

        public RowViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourtName = itemView.findViewById(R.id.tvRowCourtName);
            rvTimeCells = itemView.findViewById(R.id.rvTimeCells);
        }
    }
}