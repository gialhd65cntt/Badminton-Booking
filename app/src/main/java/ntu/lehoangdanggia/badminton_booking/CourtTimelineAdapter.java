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

    // --- CHÈN THÊM HÀM NÀY VÀO TRONG CourtTimelineAdapter.java ---
    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.contains("UPDATE_SELECTION")) {
            // NẾU LÀ LỆNH CẬP NHẬT CHỌN: Chỉ bảo Adapter con thông báo thay đổi dữ liệu, tuyệt đối không tạo lại LayoutManager
            if (holder.rvTimeCells.getAdapter() != null) {
                holder.rvTimeCells.getAdapter().notifyDataSetChanged();
            }
        } else {
            // Nếu không có payload, chạy lại hàm bind toàn bộ mặc định của bạn
            super.onBindViewHolder(holder, position, payloads);
        }
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

        // 1. TẠM THỜI GỠ bộ lắng nghe cuộn cũ ra để tránh xung đột kéo chéo khi khởi tạo lại lưới
        if (globalScrollListener != null) {
            holder.rvTimeCells.removeOnScrollListener(globalScrollListener);
        }

        holder.rvTimeCells.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        holder.rvTimeCells.setAdapter(cellAdapter);

        // 2. Ép cuộn theo thanh tiêu đề giờ (Xử lý biệt lập, an toàn)
        if (holder.itemView.getContext() instanceof CourtListActivity) {
            CourtListActivity activity = (CourtListActivity) holder.itemView.getContext();
            android.widget.HorizontalScrollView headerScroll = activity.findViewById(R.id.headerScroll);
            if (headerScroll != null) {
                final int currentScrollX = headerScroll.getScrollX();

                holder.rvTimeCells.post(() -> {
                    // Tạm gỡ listener lần nữa bên trong block chạy sau để chắc chắn không bị kích hoạt ngược
                    if (globalScrollListener != null) {
                        holder.rvTimeCells.removeOnScrollListener(globalScrollListener);
                    }

                    // Cuộn về đúng tọa độ của thanh giờ trên toolbar
                    holder.rvTimeCells.scrollTo(currentScrollX, 0);

                    // 3. CHỈ KHI CUỘN XONG XUÔI: Mới gắn lại Listener cuộn để người dùng vuốt tay bình thường
                    if (globalScrollListener != null) {
                        holder.rvTimeCells.addOnScrollListener(globalScrollListener);
                    }
                });
            }
        }

        // Lưu RecyclerView này vào danh sách quản lý nếu chưa có
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