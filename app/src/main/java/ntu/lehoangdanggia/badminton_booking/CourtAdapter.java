package ntu.lehoangdanggia.badminton_booking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import ntu.lehoangdanggia.badminton_booking.FirebaseHelper;
import java.util.List;

public class  CourtAdapter extends RecyclerView.Adapter<CourtAdapter.CourtViewHolder> {

    private List<Court> courtList;

    public CourtAdapter(List<Court> courtList) {
        this.courtList = courtList;
    }

    @NonNull
    @Override
    public CourtViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_court, parent, false);
        return new CourtViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourtViewHolder holder, int position) {
        Court court = courtList.get(position);

        holder.tvCourtName.setText(court.getCourtName());
        holder.tvPrice.setText(String.format("%,d đ/giờ", court.getPricePerHour()));

        // Bạn có thể kiểm tra nếu model của bạn có thêm trường "type" thì hiển thị, không thì ẩn đi hoặc để mặc định
        // holder.tvType.setText("Loại thảm: " + court.getType());

        // Xử lý hiển thị trạng thái màu sắc Pastel cho đẹp
        if ("Available".equalsIgnoreCase(court.getStatus())) {
            holder.tvStatus.setText("Trống");
            holder.tvStatus.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_light));
        } else {
            holder.tvStatus.setText("Đã đặt");
            holder.tvStatus.setBackgroundColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_light));
        }
    }

    @Override
    public int getItemCount() {
        return courtList != null ? courtList.size() : 0;
    }

    public static class CourtViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourtName, tvStatus, tvType, tvPrice;

        public CourtViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourtName = itemView.findViewById(R.id.tvItemCourtName);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);
            tvType = itemView.findViewById(R.id.tvItemType);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
        }
    }
}