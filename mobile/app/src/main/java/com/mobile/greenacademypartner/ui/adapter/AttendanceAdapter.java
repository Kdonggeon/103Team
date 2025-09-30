package com.mobile.greenacademypartner.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mobile.greenacademypartner.R;
import com.mobile.greenacademypartner.model.attendance.AttendanceResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final Context context;

    // 원본 + 화면 표시용
    private final List<AttendanceResponse> allItems = new ArrayList<>();
    private final List<AttendanceResponse> items    = new ArrayList<>();

    // (정규화된) 클래스명 → Days_Of_Week(1=월…7=일) 매핑
    private final Map<String, List<Integer>> classDowMapByName = new HashMap<>();

    // 선택된 요일(1=월 … 7=일). null이면 전체
    private Integer selectedDowMon1ToSun7 = null;

    // 디버그 옵션: 매핑 실패시에도 표시(진단용). 실제 배포는 false 권장.
    private static final boolean DEBUG_INCLUDE_UNMAPPED = false;

    // (호환용) setDisplayDate용 포맷
    private static final TimeZone KST = TimeZone.getTimeZone("Asia/Seoul");
    private static final Locale   KR  = Locale.KOREA;
    private static final SimpleDateFormat ISO_DATE_FMT;
    static {
        ISO_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", KR);
        ISO_DATE_FMT.setLenient(false);
        ISO_DATE_FMT.setTimeZone(KST);
    }

    public AttendanceAdapter(Context context, List<AttendanceResponse> initial) {
        this.context = context;
        setAll(initial);
    }

    /** 103DB.classes의 Days_Of_Week 매핑 주입 (키: 클래스명, 내부에서 정규화하여 저장) */
    public void setClassDowMap(Map<String, List<Integer>> byClassName) {
        classDowMapByName.clear();
        if (byClassName != null) {
            for (Map.Entry<String, List<Integer>> e : byClassName.entrySet()) {
                String keyNorm = norm(e.getKey());
                List<Integer> dows = e.getValue();
                if (keyNorm != null && !keyNorm.isEmpty() && dows != null && !dows.isEmpty()) {
                    classDowMapByName.put(keyNorm, dows);
                }
            }
        }
        applyFilter(); // 매핑 갱신 시 즉시 반영
    }

    /** 전체 데이터 주입 */
    public void setAll(List<AttendanceResponse> list) {
        allItems.clear();
        if (list != null) allItems.addAll(list);

        // date 오름차순(널 안전)
        Collections.sort(allItems, new Comparator<AttendanceResponse>() {
            @Override public int compare(AttendanceResponse a, AttendanceResponse b) {
                String da = a != null ? a.getDate() : null;
                String db = b != null ? b.getDate() : null;
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return da.compareTo(db);
            }
        });

        applyFilter();
    }

    /** 시간표 스타일: 요일(1=월 … 7=일)로 직접 필터 */
    public void setDisplayDow(int dowMon1ToSun7) {
        if (dowMon1ToSun7 < 1 || dowMon1ToSun7 > 7) {
            this.selectedDowMon1ToSun7 = null; // 범위 밖이면 전체
        } else {
            this.selectedDowMon1ToSun7 = dowMon1ToSun7;
        }
        applyFilter();
    }

    /** (호환) 날짜 문자열(yyyy-MM-dd) → 내부에서 dow 계산 후 setDisplayDow와 동일 처리 */
    public void setDisplayDate(String dateIso) {
        Integer dow = toDowMon1ToSun7FromIso(dateIso);
        this.selectedDowMon1ToSun7 = dow; // null이면 전체
        applyFilter();
    }

    /** 필터 적용: 아이템 daysOfWeek 우선 → 없으면 (정규화된) className 매핑 사용 */
    private void applyFilter() {
        items.clear();

        // 선택 요일 없으면 전체
        if (selectedDowMon1ToSun7 == null) {
            items.addAll(allItems);
            notifyDataSetChanged();
            return;
        }

        final int want = selectedDowMon1ToSun7;

        for (AttendanceResponse ar : allItems) {
            if (ar == null) continue;

            // 1) 우선 아이템 자체의 daysOfWeek 사용
            List<Integer> dowsFromItem = null;
            try { dowsFromItem = ar.getDaysOfWeek(); } catch (Throwable ignored) {}
            if (dowsFromItem != null && !dowsFromItem.isEmpty()) {
                if (dowsFromItem.contains(want)) {
                    items.add(ar);
                } else if (DEBUG_INCLUDE_UNMAPPED) {
                    // 진단용: 매칭 실패해도 보여주기
                    items.add(ar);
                }
                continue;
            }

            // 2) 없으면 (정규화된) 클래스명 기반 매핑 사용
            String clsNameNorm = norm(ar.getClassName());
            List<Integer> mapped = classDowMapByName.get(clsNameNorm);
            if (mapped != null && mapped.contains(want)) {
                items.add(ar);
            } else if (mapped == null && DEBUG_INCLUDE_UNMAPPED) {
                // 진단용: 맵에 키 없을 때도 표시해서 사라짐 현상 추적
                items.add(ar);
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AttendanceAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AttendanceAdapter.ViewHolder holder, int position) {
        AttendanceResponse item = items.get(position);

        String academy   = safe(item.getAcademyName());
        String className = safe(item.getClassName());
        String date      = safe(item.getDate());
        String status    = safe(item.getStatus());

        holder.tvAcademy.setText(academy);
        holder.tvClass.setText(className);
        holder.tvDate.setText(date);
        holder.tvStatus.setText(status.isEmpty() ? "출석 정보 없음" : status);

        if (holder.tvTimeRange != null) {
            String s = normalizeTime(item.getStartTime());
            String e = normalizeTime(item.getEndTime());
            if (!s.isEmpty() && !e.isEmpty()) {
                holder.tvTimeRange.setText(s + "~" + e);
                holder.tvTimeRange.setVisibility(View.VISIBLE);
            } else {
                holder.tvTimeRange.setText("");
                holder.tvTimeRange.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAcademy;     // @id/text_academy_name
        final TextView tvClass;       // @id/text_class_name
        final TextView tvDate;        // @id/text_date
        final TextView tvStatus;      // @id/text_status
        final TextView tvTimeRange;   // @id/text_time_range (선택)

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAcademy   = itemView.findViewById(R.id.text_academy_name);
            tvClass     = itemView.findViewById(R.id.text_class_name);
            tvDate      = itemView.findViewById(R.id.text_date);
            tvStatus    = itemView.findViewById(R.id.text_status);
            tvTimeRange = itemView.findViewById(R.id.text_time_range);
        }
    }

    // ===== 유틸 =====

    private static String safe(String s) { return s == null ? "" : s; }

    private static String normalizeTime(String t) {
        if (t == null || t.trim().isEmpty()) return "";
        try {
            String[] p = t.split(":");
            int hh = Integer.parseInt(p[0].trim());
            int mm = (p.length > 1) ? Integer.parseInt(p[1].trim()) : 0;
            if (hh < 0 || hh > 23 || mm < 0 || mm > 59) return "";
            return String.format(KR, "%02d:%02d", hh, mm);
        } catch (Exception e) { return ""; }
    }

    /** "yyyy-MM-dd" → 1(월)…7(일) (호환용) */
    private static Integer toDowMon1ToSun7FromIso(String iso) {
        if (iso == null || iso.trim().isEmpty()) return null;
        try {
            java.util.Date d = ISO_DATE_FMT.parse(iso.trim());
            Calendar cal = Calendar.getInstance(KST, KR);
            cal.setTime(d);
            int c = cal.get(Calendar.DAY_OF_WEEK); // SUNDAY=1 … SATURDAY=7
            return (c == Calendar.SUNDAY) ? 7 : (c - 1);
        } catch (ParseException e) {
            return null;
        }
    }

    /** 클래스명 정규화: 공백 제거 + 소문자 */
    private static String norm(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
