package com.team103.service;

import java.util.*;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import com.team103.model.Teacher;

@Service
public class TeacherSearchService {

    private final MongoTemplate mongo;

    public TeacherSearchService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    // --- 필드 별칭(혼용 스키마 대응) ---
    private static final List<String> T_ID    = List.of("Teacher_ID","teacherId","id","username");
    private static final List<String> T_NAME  = List.of("Teacher_Name","teacherName","name");
    private static final List<String> T_ACA_1 = List.of("Academy_Number","academyNumber");
    private static final List<String> T_ACA_N = List.of("Academy_Numbers","academyNumbers");

    private static final List<String> C_SUBJECT   = List.of("Subject","subject","Course_Subject","courseSubject","Course_Name","courseName","Name","name");
    private static final List<String> C_ACA_1     = List.of("Academy_Number","academyNumber");
    private static final List<String> C_ACA_N     = List.of("Academy_Numbers","academyNumbers");
    private static final List<String> C_T_ID_1    = List.of("Teacher_ID","teacherId","TeacherId","teacher","Teacher","username","id");
    private static final List<String> C_T_ID_N    = List.of("Teachers","teachers","teacherIds","Teacher_IDs");

    private Criteria orRegex(List<String> keys, String keyword) {
        return new Criteria().orOperator(
            keys.stream().map(k -> Criteria.where(k).regex(keyword, "i")).toArray(Criteria[]::new)
        );
    }

    private Criteria orAca(List<String> singleKeys, List<String> arrayKeys, Integer num) {
        List<Criteria> ors = new ArrayList<>();
        singleKeys.forEach(k -> ors.add(Criteria.where(k).is(num)));
        arrayKeys.forEach(k -> ors.add(Criteria.where(k).is(num))); // 배열 필드에도 is(num)로 포함 매칭
        return new Criteria().orOperator(ors.toArray(new Criteria[0]));
    }

    /** subject/academyNumber를 classes에서 먼저 필터 → 교사ID 집합 */
    private Set<String> teacherIdsFromClasses(String subject, Integer academyNumber) {
        boolean need = StringUtils.hasText(subject) || academyNumber != null;
        if (!need) return null;

        List<Criteria> ands = new ArrayList<>();
        if (StringUtils.hasText(subject)) ands.add(orRegex(C_SUBJECT, subject));
        if (academyNumber != null)       ands.add(orAca(C_ACA_1, C_ACA_N, academyNumber));

        Query q = new Query();
        if (!ands.isEmpty()) q.addCriteria(new Criteria().andOperator(ands.toArray(new Criteria[0])));
        q.limit(2000);

        List<Document> classes = mongo.find(q, Document.class, "classes");

        Set<String> ids = new HashSet<>();
        for (Document d : classes) {
            for (String k : C_T_ID_1) {
                Object v = d.get(k);
                if (v instanceof String s && !s.isBlank()) ids.add(s.trim());
            }
            for (String k : C_T_ID_N) {
                Object v = d.get(k);
                if (v instanceof Collection<?> col) {
                    for (Object e : col) {
                        if (e != null) {
                            String s = String.valueOf(e).trim();
                            if (!s.isBlank()) ids.add(s);
                        }
                    }
                }
            }
        }
        return ids;
    }

    /** 메인 검색: 모든 파라미터를 AND로 결합 */
    public List<Teacher> search(String id, String name, String subject, Integer academyNumber,
                                Integer page, Integer size, String sortKey, String sortDir) {

        // 1) classes에서 subject/academyNumber 필터 → 교사ID 집합
        Set<String> fromClasses = teacherIdsFromClasses(subject, academyNumber);
        if (StringUtils.hasText(subject) && fromClasses != null && fromClasses.isEmpty()) {
            return List.of(); // 과목 요구했는데 해당 수업이 없으면 즉시 빈 결과
        }

        // 2) teachers에서 id/name/academyNumber 필터 + (있다면) fromClasses 교집합
        List<Criteria> ands = new ArrayList<>();
        if (StringUtils.hasText(id))   ands.add(orRegex(T_ID,   id));
        if (StringUtils.hasText(name)) ands.add(orRegex(T_NAME, name));
        if (academyNumber != null)     ands.add(orAca(T_ACA_1, T_ACA_N, academyNumber));
        if (fromClasses != null) {
            List<Criteria> inOr = new ArrayList<>();
            T_ID.forEach(k -> inOr.add(Criteria.where(k).in(fromClasses)));
            ands.add(new Criteria().orOperator(inOr.toArray(new Criteria[0])));
        }

        Query tq = new Query();
        if (!ands.isEmpty()) tq.addCriteria(new Criteria().andOperator(ands.toArray(new Criteria[0])));

        // 선택: 정렬/페이지
        if (StringUtils.hasText(sortKey)) {
            Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
            tq.with(Sort.by(dir, sortKey));
        }
        int pageSafe = (page == null || page < 0) ? 0 : page;
        int sizeSafe = (size == null || size <= 0 || size > 200) ? 50 : size;
        tq.skip((long) pageSafe * sizeSafe).limit(sizeSafe);

        return mongo.find(tq, Teacher.class, "teachers");
    }
}
