package com.example.shiftsync;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shiftsync.models.User;
import java.util.List;

//בדומה לרשימת ההודעות - אדפטר לרשימת העובדים לחיבור לתצוגה
public class EmployeesAdapter extends RecyclerView.Adapter<EmployeesAdapter.ViewHolder> {


    public interface OnEmployeeClickListener {
        void onDeleteClick(User user); // לחיצה על פח אשפה
        void onEditClick(User user);   // לחיצה על עיפרון (עריכת שכר)
    }

    // רשימת העובדים שמוצגת כרגע על המסך
    private List<User> employees;

    // המאזין לאירועים
    private OnEmployeeClickListener listener;

    //בנאי
    public EmployeesAdapter(List<User> employees, OnEmployeeClickListener listener) {
        this.employees = employees;
        this.listener = listener;
    }

    //עדכון הרשימה
    public void updateList(List<User> newList) {
        this.employees = newList;
        notifyDataSetChanged(); // פקודה ל-RecyclerView לצייר מחדש את הכל
    }

    //אותן פונקציות בסיסיות של הrecyclerview
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = employees.get(position);

        //  הצגת טקסטים
        holder.tvName.setText(user.getFullName());
        holder.tvId.setText("ת.ז: " + user.getIdNumber());
        holder.tvRate.setText("שכר שעתי: " + user.getHourlyRate());

        //  טיפול בתמונת פרופיל

        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            holder.ivProfile.setImageBitmap(ImageUtils.stringToBitmap(user.getProfileImage()));
        } else {
            holder.ivProfile.setImageResource(R.mipmap.ic_launcher_round);
        }

        //  הגדרת כפתורי הפעולה (עריכה ומחיקה)
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(user));
        holder.btnEdit.setOnClickListener(v -> listener.onEditClick(user));
    }


    @Override
    public int getItemCount() {
        return employees == null ? 0 : employees.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        // חיבור ל item_employee.xml
        TextView tvName, tvId, tvRate;
        ImageButton btnDelete, btnEdit; 
        ImageView ivProfile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // קישור ה-Java ל-XML לפי ה-IDs שהגדרנו
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvId = itemView.findViewById(R.id.tvEmployeeId);
            tvRate = itemView.findViewById(R.id.tvEmployeeRate);

            btnDelete = itemView.findViewById(R.id.btnDeleteEmployee);
            btnEdit = itemView.findViewById(R.id.btnEditEmployee);

            ivProfile = itemView.findViewById(R.id.ivEmployeeProfile);
        }
    }
}