package ntu.lehoangdanggia.badminton_booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    // 1. Khai báo các View của giao diện mới
    private TextView tvAdminName;
    private CardView menuStatus, menuSales, menuStorage, menuRevenue;
    private CardView menuBranches, menuCustomers, menuMembership, menuFinance;
    private BottomNavigationView bottomNavigation;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2. Ánh xạ các thành phần giao diện mới
        initViews();

        // 3. Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // 4. Tải thông tin Admin (Tên hiển thị)
        loadAdminProfile();

        // 5. Cài đặt sự kiện click cho các ô chức năng
        setupMenuClickListeners();

        // 6. Cài đặt sự kiện cho thanh Bottom Navigation phía dưới cùng
        setupBottomNavigation();
    }

    private void initViews() {
        tvAdminName = findViewById(R.id.tvAdminName);

        // Ánh xạ 8 ô chức năng dạng lưới
        menuStatus = findViewById(R.id.menuStatus);
        menuSales = findViewById(R.id.menuSales);
        menuStorage = findViewById(R.id.menuStorage);
        menuRevenue = findViewById(R.id.menuRevenue);
        menuBranches = findViewById(R.id.menuBranches);
        menuCustomers = findViewById(R.id.menuCustomers);
        menuMembership = findViewById(R.id.menuMembership);
        menuFinance = findViewById(R.id.menuFinance);

        // Ánh xạ thanh điều hướng dưới cùng
        bottomNavigation = findViewById(R.id.bottomNavigation);
    }

    private void loadAdminProfile() {
        if (mAuth.getCurrentUser() != null) {
            String currentUserId = mAuth.getCurrentUser().getUid();

            // Lấy họ tên Admin từ Firestore để hiển thị lên màn hình chính
            db.collection("Users").document(currentUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("fullName");
                            if (fullName != null && !fullName.isEmpty()) {
                                tvAdminName.setText(fullName);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this, "Không thể tải thông tin admin", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setupMenuClickListeners() {
        // 1. Ô Trạng thái sân (Nơi bạn sẽ mở danh sách sân cầu lông)
        menuStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạm thời thông báo, sau này bạn tạo CourtListActivity thì mở ra ở đây
                Toast.makeText(MainActivity.this, "Mở quản lý Trạng thái sân", Toast.LENGTH_SHORT).show();

                // Ví dụ chuyển sang màn hình danh sách sân (nếu có):
               Intent intent = new Intent(MainActivity.this, CourtListActivity.class);
                 startActivity(intent);
            }
        });

        // 2. Ô Bán hàng
        menuSales.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Mở chức năng Bán hàng", Toast.LENGTH_SHORT).show()
        );

        // 3. Quản lý kho và dịch vụ
        menuStorage.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Mở Quản lý kho & dịch vụ", Toast.LENGTH_SHORT).show()
        );

        // 4. Doanh thu & lợi nhuận
        menuRevenue.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Mở Thống kê Doanh thu", Toast.LENGTH_SHORT).show()
        );

        // 5. Quản lý chi nhánh
        menuBranches.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Mở Quản lý chi nhánh", Toast.LENGTH_SHORT).show()
        );

        // 6. Quản lý khách hàng
        menuCustomers.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Mở Quản lý khách hàng", Toast.LENGTH_SHORT).show()
        );

        // 7. Hạng thành viên
        menuMembership.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Mở Quản lý Hạng thành viên", Toast.LENGTH_SHORT).show()
        );

        // 8. Quản lý thu chi
        menuFinance.setOnClickListener(v ->
                Toast.makeText(MainActivity.this, "Mở Quản lý thu chi tài chính", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupBottomNavigation() {
        // Lắng nghe sự kiện người dùng bấm vào các mục ở thanh dưới cùng
        bottomNavigation.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Toast.makeText(MainActivity.this, "Bạn đang ở Trang Chủ", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_booking) {
                    Toast.makeText(MainActivity.this, "Mở chức năng Đặt lịch", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_approve) {
                    Toast.makeText(MainActivity.this, "Mở chức năng Duyệt đơn đặt sân", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            }
        });

        // Đặt mục "Trang Chủ" được chọn mặc định khi mở màn hình
        bottomNavigation.setSelectedItemId(R.id.nav_home);
    }
}