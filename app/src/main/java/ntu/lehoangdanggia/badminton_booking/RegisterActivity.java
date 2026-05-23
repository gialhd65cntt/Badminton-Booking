package ntu.lehoangdanggia.badminton_booking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import ntu.lehoangdanggia.badminton_booking.FirebaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // 1. Khai báo các thành phần giao diện
    private TextInputEditText etFullName, etPhone, etEmail, etPassword;
    private MaterialButton btnRegister;
    private TextView tvLoginLink;

    // 2. Khai báo Firebase Auth và Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 3. Ánh xạ các View từ file XML sang Java
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);

        // 4. Lấy Instance của Firebase thông qua Helper đã tạo hôm trước
        mAuth = FirebaseHelper.getAuthInstance();
        db = FirebaseHelper.getFirestoreInstance();

        // 5. Bắt sự kiện khi người dùng nhấn nút Đăng ký
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                xulyDangKy();
            }
        });

        // 6. Bắt sự kiện chuyển sang màn hình Đăng nhập nếu đã có tài khoản
        tvLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                 Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                 startActivity(intent);
                Toast.makeText(RegisterActivity.this, "Chuyển sang màn hình Đăng nhập", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void xulyDangKy() {
        // Lấy chuỗi văn bản người dùng nhập vào
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Kiểm tra dữ liệu đầu vào (Validation) - Tránh lỗi hệ thống
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Vui lòng nhập họ và tên");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Vui lòng nhập số điện thoại");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải từ 6 ký tự trở lên");
            return;
        }

        // Vô hiệu hóa nút bấm tạm thời để tránh người dùng nhấn liên tục nhiều lần
        btnRegister.setEnabled(false);

        // Thực hiện tạo tài khoản trên Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Lấy UID duy nhất do Firebase cấp cho User này
                        String uid = mAuth.getCurrentUser().getUid();

                        // Tạo cấu trúc Map để lưu thông tin chi tiết vào Firestore
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("userId", uid);
                        userMap.put("fullName", fullName);
                        userMap.put("phoneNumber", phone);
                        userMap.put("email", email);
                        userMap.put("role", "customer"); // Quyền mặc định là khách hàng

                        // Đẩy dữ liệu vào collection "Users", đặt tên Document chính là UID
                        db.collection("Users").document(uid)
                                .set(userMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                                    btnRegister.setEnabled(true);

                                    // Đăng ký xong thì chuyển thẳng vào MainActivity (Màn hình chính)
                                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish(); // Đóng RegisterActivity lại để khi bấm Back không bị quay lại form đăng ký
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(RegisterActivity.this, "Lỗi lưu thông tin: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    btnRegister.setEnabled(true);
                                });
                    } else {
                        // Thất bại từ bước Auth (Ví dụ: Email đã có người sử dụng)
                        Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        btnRegister.setEnabled(true);
                    }
                });
    }
}