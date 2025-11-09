// src/main/java/com/team103/service/SeatBoardService.java
package com.team103.service;

import com.team103.dto.SeatBoardResponse;
import com.team103.model.Attendance;
import com.team103.model.Course;
import com.team103.model.Room;
import com.team103.model.WaitingRoom;
import com.team103.repository.AttendanceRepository;
import com.team103.repository.CourseRepository;
import com.team103.repository.RoomRepository;
import com.team103.repository.StudentRepository;
import com.team103.repository.WaitingRoomRepository;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SeatBoardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HM  = DateTimeFormatter.ofPattern("HH:mm");

    private final CourseRepository courseRepo;
    private final AttendanceRepository attRepo;
    private final RoomRepository roomRepo;
    private final StudentRepository studentRepo;
    private final WaitingRoomRepository waitingRepo;

    public SeatBoardService(CourseRepository courseRepo,
                            AttendanceRepository attRepo,
                            RoomRepository roomRepo,
                            StudentRepository studentRepo,
                            WaitingRoomRepository waitingRepo) {
        this.courseRepo = courseRepo;
        this.attRepo = attRepo;
        this.roomRepo = roomRepo;
        this.studentRepo = studentRepo;
        this.waitingRepo = waitingRepo;
    }

    /* ─────────────── util ─────────────── */
    public static String todayYmd() { return LocalDate.now(KST).format(YMD); }
    public static String nowHm() { return LocalTime.now(KST).format(HM); }
    private static boolean isBlank(String s){ return s==null||s.trim().isEmpty(); }
    private static <T> T nvl(T v,T d){ return v!=null?v:d; }
    private static String firstNonBlank(String a,String b){ return !isBlank(a)?a:!isBlank(b)?b:null; }
    private static Integer parseIntOrNull(String s){
        try{ return s==null?null:Integer.valueOf(s.replaceAll("[^0-9]","")); }
        catch(Exception e){return null;}
    }
    private static Integer seatNumberOf(Room.VectorSeat v){ return v==null?null:parseIntOrNull(v.getLabel()); }
    private static int seatOrderOf(Room.VectorSeat v){ Integer n=seatNumberOf(v); return n==null?Integer.MAX_VALUE:n; }

    private static Object tryInvoke(Object t,String n,Class<?>[] tp,Object[] a){
        if(t==null)return null;
        try{
            Method m=tp==null?t.getClass().getMethod(n):t.getClass().getMethod(n,tp);
            m.setAccessible(true);
            return a==null?m.invoke(t):m.invoke(t,a);
        }catch(Exception e){return null;}
    }
    private static String tryGetString(Object t,String n){Object v=tryInvoke(t,n,null,null);return v==null?null:String.valueOf(v);}
    @SuppressWarnings("unused")
    private static boolean within(String s,String e,String n){return s!=null&&e!=null&&n.compareTo(s)>=0&&n.compareTo(e)<=0;}

    /* ─────────────── 이름맵 / 웨이팅룸 ─────────────── */
    private Map<String,String> resolveStudentNames(Set<String> ids){
        Map<String,String> map=new HashMap<>();
        if(ids==null||ids.isEmpty())return map;
        studentRepo.findByStudentIdIn(new ArrayList<>(ids)).forEach(s->{
            if(s==null)return;
            String sid=tryGetString(s,"getStudentId");
            String nm=firstNonBlank(tryGetString(s,"getStudentName"),tryGetString(s,"getName"));
            if(sid!=null&&nm!=null)map.put(sid,nm);
        });
        return map;
    }

    private List<SeatBoardResponse.WaitingItem> loadWaiting(int academyNumber,List<String> roster,Map<String,String> nameById){
        if(academyNumber<=0)return List.of();
        List<WaitingRoom> raws=waitingRepo.findByAcademyNumber(academyNumber)
                .stream().filter(w->roster.contains(w.getStudentId())).toList();
        List<SeatBoardResponse.WaitingItem> out=new ArrayList<>();
        for(WaitingRoom w:raws){
            SeatBoardResponse.WaitingItem it=new SeatBoardResponse.WaitingItem();
            it.setStudentId(w.getStudentId());
            it.setStudentName(nameById.get(w.getStudentId()));
            it.setStatus(w.getStatus());
            it.setCheckedInAt(w.getCheckedInAt());
            out.add(it);
        }
        out.sort(Comparator.comparing(SeatBoardResponse.WaitingItem::getCheckedInAt,Comparator.nullsLast(String::compareTo)));
        return out;
    }

    /* ─────────────── 좌석판 조회 ─────────────── */
    public SeatBoardResponse getSeatBoard(String classId,String date){
        final String ymd=isBlank(date)?todayYmd():date.trim();
        Course course=courseRepo.findByClassId(classId)
                .orElseThrow(()->new RuntimeException("class not found: "+classId));

        Integer roomNumber=null;
        try{
            Object v=tryInvoke(course,"getRoomFor",new Class[]{String.class},new Object[]{ymd});
            if(v!=null)roomNumber=Integer.valueOf(String.valueOf(v));
        }catch(Exception ignore){}
        if(roomNumber==null)throw new RuntimeException("room not set: "+classId+" @ "+ymd);

        List<Integer> academies=new ArrayList<>();
        try{
            @SuppressWarnings("unchecked")
            List<Integer> tmp=(List<Integer>)tryInvoke(course,"getAcademyNumbersSafe",null,null);
            if(tmp!=null)academies=tmp;
        }catch(Exception ignore){}
        Integer academyNumber=!academies.isEmpty()?academies.get(0):(Integer)tryInvoke(course,"getAcademyNumber",null,null);

        // ── 단계적으로 방 조회 (캡처 이슈 회피)
        Room room = null;
        if (academyNumber != null) {
            room = roomRepo.findByRoomNumberAndAcademyNumber(roomNumber, academyNumber).orElse(null);
        }
        if (room == null) {
            List<Room> lst;
            try { lst = roomRepo.findByRoomNumber(roomNumber); }
            catch (Throwable t) { lst = Collections.emptyList(); }
            if (lst == null || lst.isEmpty()) {
                throw new RuntimeException("room not found: room=" + roomNumber + ", academy=" + academyNumber);
            }
            room = lst.get(0);
            academyNumber = room.getAcademyNumber();
        }

        Attendance att=ensureAttendanceDoc(classId,ymd,course);
        Map<String,String> statusByStudent=buildStatusMap(att);

        // seatLabel->studentId
        Map<String,String> studentBySeatLabel=new HashMap<>();
        if(att.getSeatAssignments()!=null){
            for(Attendance.SeatAssign a:att.getSeatAssignments()){
                if(a==null)continue;
                String k=firstNonBlank(a.getSeatLabel(),a.getSeatId());
                if(!isBlank(k)&&!isBlank(a.getStudentId()))studentBySeatLabel.put(k,a.getStudentId());
            }
        }

        // Course.Seat_Map 병합 (String/Integer key 모두 처리)
        try{
            Object raw=tryInvoke(course,"getSeatMap",null,null);
            if(raw instanceof Map<?,?> top){
                Object sub=top.get(String.valueOf(roomNumber));
                if(sub==null)sub=top.get(roomNumber);
                if(sub instanceof Map<?,?> inner){
                    for(var e:inner.entrySet()){
                        String k=String.valueOf(e.getKey());
                        String v=String.valueOf(e.getValue());
                        studentBySeatLabel.putIfAbsent(k,v);
                    }
                }
            }
        }catch(Exception ignore){}

        // 이름 맵
        Set<String> ids=new HashSet<>(studentBySeatLabel.values());
        if(att.getAttendanceList()!=null)
            att.getAttendanceList().forEach(it->{ if(it!=null&&!isBlank(it.getStudentId()))ids.add(it.getStudentId()); });
        Map<String,String> nameById=resolveStudentNames(ids);

        // 좌석 빌드
        List<SeatBoardResponse.SeatStatus> seats=new ArrayList<>();
        String layoutType="grid";

        // 우선 V2 → 없으면 V1 → 없으면 grid
        List<Room.VectorSeat> vec = null;
        try { vec = (List<Room.VectorSeat>) Room.class.getMethod("getVectorLayoutV2").invoke(room); } catch (Exception ignore) {}
        if ((vec == null || vec.isEmpty())) {
            try { vec = (List<Room.VectorSeat>) Room.class.getMethod("getVectorLayout").invoke(room); } catch (Exception ignore) {}
        }

        if (vec != null && !vec.isEmpty()) {
            layoutType = "vector";
            for (Room.VectorSeat v : vec.stream().sorted(Comparator.comparingInt(SeatBoardService::seatOrderOf)).toList()) {
                SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();

                Integer num = seatNumberOf(v);
                s.setSeatNumber(num);

                // 벡터 좌표/크기/회전
                s.setX(v.getX());
                s.setY(v.getY());
                s.setW(v.getW());
                s.setH(v.getH());
                s.setR(v.getR());

                // grid 필드 null
                s.setRow(null);
                s.setCol(null);

                s.setDisabled(Boolean.TRUE.equals(v.getDisabled()));

                String label = firstNonBlank(v.getLabel(), num == null ? null : String.valueOf(num));
                String sid = studentBySeatLabel.get(label);
                s.setStudentId(sid);
                s.setStudentName(sid != null ? nameById.get(sid) : null);
                s.setAttendanceStatus(sid != null ? statusByStudent.getOrDefault(sid, "미기록") : "미기록");

                seats.add(s);
            }
        } else if (room.getLayout()!=null && !room.getLayout().isEmpty()) {
            layoutType = "grid";
            room.getLayout().stream()
                .sorted(Comparator.comparingInt(c -> c.getSeatNumber()==null?9999:c.getSeatNumber()))
                .forEach(c -> {
                    SeatBoardResponse.SeatStatus s = new SeatBoardResponse.SeatStatus();
                    s.setSeatNumber(c.getSeatNumber());
                    s.setRow(c.getRow());
                    s.setCol(c.getCol());
                    // 벡터 좌표 null
                    s.setX(null); s.setY(null); s.setW(null); s.setH(null); s.setR(null);
                    s.setDisabled(Boolean.TRUE.equals(c.getDisabled()));

                    String label = c.getSeatNumber()!=null ? String.valueOf(c.getSeatNumber()) : null;
                    String sid   = studentBySeatLabel.get(label);

                    s.setStudentId(sid);
                    s.setStudentName(sid!=null ? nameById.get(sid) : null);
                    // ✅ 오타 수정: getOrDefault
                    s.setAttendanceStatus(sid!=null ? statusByStudent.getOrDefault(sid,"미기록") : "미기록");

                    seats.add(s);
                });
        }

        // 출결 카운트
        int present=0,late=0,absent=0,move=0,none=0;
        for(String st:statusByStudent.values()){
            if(st==null||st.isBlank()||"미기록".equals(st)){none++;continue;}
            switch(st){
                case "출석","PRESENT" -> present++;
                case "지각","LATE" -> late++;
                case "결석","ABSENT" -> absent++;
                case "이동","휴식","MOVE","BREAK" -> move++;
                default -> none++;
            }
        }

        // 웨이팅
        List<String> roster=att.getAttendanceList()!=null?
                att.getAttendanceList().stream().map(Attendance.Item::getStudentId).filter(Objects::nonNull).toList():
                List.of();
        List<SeatBoardResponse.WaitingItem> waiting=academyNumber!=null?
                loadWaiting(academyNumber,roster,nameById):List.of();

        // 응답
        SeatBoardResponse r=new SeatBoardResponse();
        SeatBoardResponse.CurrentClass cc=new SeatBoardResponse.CurrentClass();
        cc.setClassId(course.getClassId());
        cc.setClassName((String)tryInvoke(course,"getClassName",null,null));
        r.setCurrentClass(cc);
        r.setDate(ymd);
        r.setLayoutType(layoutType);
        r.setSeats(seats);
        r.setWaiting(waiting);
        r.setPresentCount(present);
        r.setLateCount(late);
        r.setAbsentCount(absent);
        r.setMoveOrBreakCount(move);
        r.setNotRecordedCount(none);

        // 벡터/그리드 분기 필드
        if ("vector".equals(layoutType)) {
            Double cw = room.getVectorCanvasW() != null ? room.getVectorCanvasW() : 1.0;
            Double ch = room.getVectorCanvasH() != null ? room.getVectorCanvasH() : 1.0;
            r.setCanvasW(cw);
            r.setCanvasH(ch);
            r.setRows(null);
            r.setCols(null);
        } else {
            r.setRows(room.getRows());
            r.setCols(room.getCols());
            r.setCanvasW(null);
            r.setCanvasH(null);
        }
        return r;
    }

    /* ─────────────── 기타 헬퍼 ─────────────── */
    private Map<String,String> buildStatusMap(Attendance att){
        Map<String,String> map=new HashMap<>();
        if(att.getAttendanceList()==null)return map;
        att.getAttendanceList().forEach(it->{
            if(it==null||isBlank(it.getStudentId()))return;
            map.put(it.getStudentId(),isBlank(it.getStatus())?"미기록":it.getStatus());
        });
        return map;
    }

    public void assignSeat(String classId,String date,String seatLabel,String studentId){
        if(isBlank(seatLabel)||isBlank(studentId))
            throw new IllegalArgumentException("seatLabel/studentId required");
        String ymd=isBlank(date)?todayYmd():date.trim();

        Attendance att=ensureAttendanceDoc(classId,ymd,null);
        List<Attendance.SeatAssign> list=new ArrayList<>(nvl(att.getSeatAssignments(),List.of()));
        list.removeIf(x->seatLabel.equals(x.getSeatLabel())||studentId.equals(x.getStudentId()));
        Attendance.SeatAssign a=new Attendance.SeatAssign();
        a.setSeatLabel(seatLabel);
        a.setStudentId(studentId);
        list.add(a);
        att.setSeatAssignments(list);
        ensureAttendanceStatus(att,studentId,"출석");
        attRepo.save(att);

        // 웨이팅 삭제
        try{
            Course c=courseRepo.findByClassId(classId).orElse(null);
            Integer an=(Integer)tryInvoke(c,"getAcademyNumber",null,null);
            if(an!=null)waitingRepo.deleteByAcademyNumberAndStudentId(an,studentId);
        }catch(Exception ignore){}
    }

    public void unassignSeat(String classId,String date,String seatLabel){
        String ymd=isBlank(date)?todayYmd():date.trim();
        Attendance att=attRepo.findFirstByClassIdAndDate(classId,ymd);
        if(att==null||att.getSeatAssignments()==null)return;
        att.getSeatAssignments().removeIf(x->seatLabel.equals(x.getSeatLabel()));
        attRepo.save(att);
    }

    public void moveOrBreak(String classId,String date,String studentId,String status){
        String ymd=isBlank(date)?todayYmd():date.trim();
        Attendance att=ensureAttendanceDoc(classId,ymd,null);
        if(att.getSeatAssignments()!=null)
            att.getSeatAssignments().removeIf(x->studentId.equals(x.getStudentId()));
        ensureAttendanceStatus(att,studentId,isBlank(status)?"이동":status);
        attRepo.save(att);
    }

    private Attendance ensureAttendanceDoc(String classId, String ymd, Course opt) {
        Attendance att = attRepo.findFirstByClassIdAndDate(classId, ymd);
        if (att != null) return att;

        att = new Attendance();
        att.setClassId(classId);
        att.setDate(ymd);
        att.setAttendanceList(new ArrayList<>());
        att.setSeatAssignments(new ArrayList<>());

        Course c = (opt != null) ? opt : courseRepo.findByClassId(classId).orElse(null);
        if (c != null) {
            Object rosterObj = tryInvoke(c, "getStudents", null, null);
            if (rosterObj instanceof List<?> roster) {
                for (Object sidObj : roster) {
                    if (sidObj == null) continue;
                    String sid = String.valueOf(sidObj).trim();
                    if (sid.isEmpty()) continue;

                    Attendance.Item it = new Attendance.Item();
                    it.setStudentId(sid);
                    it.setStatus("미기록");
                    att.getAttendanceList().add(it);
                }
            }
        }
        return attRepo.save(att);
    }

    private void ensureAttendanceStatus(Attendance att,String sid,String st){
        if(att.getAttendanceList()==null)att.setAttendanceList(new ArrayList<>());
        for(Attendance.Item it:att.getAttendanceList()){
            if(sid.equals(it.getStudentId())){it.setStatus(st);return;}
        }
        Attendance.Item it=new Attendance.Item();
        it.setStudentId(sid);it.setStatus(st);
        att.getAttendanceList().add(it);
    }
}
