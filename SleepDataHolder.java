package ac.kr.project;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe holder for sleep stages with helper methods to update last stage.
 */
public class SleepDataHolder {

    private static final List<SleepStage> stages = new ArrayList<>();
    private static long sessionStart = 0;
    private static long sessionEnd = 0;

    // -------------------------
    // 기본 관리
    // -------------------------
    public static synchronized void clear() {
        stages.clear();
        sessionStart = 0;
        sessionEnd = 0;
    }

    public static synchronized void addStage(SleepStage s) {
        // 직전 단계와 같은 stage라면 병합 (endTime 갱신, movement/breath 평균)
        if (!stages.isEmpty()) {
            SleepStage last = stages.get(stages.size() - 1);
            if (last.stage == s.stage) {
                last.endTime = s.endTime;
                last.movement = (last.movement + s.movement) / 2f;
                last.breathRate = (last.breathRate + s.breathRate) / 2;
                return;
            }
        }
        stages.add(s);
    }

    public static synchronized List<SleepStage> getStages() {
        return new ArrayList<>(stages);
    }

    // -------------------------
    // 세션 시간 저장
    // -------------------------
    public static synchronized void setSessionStart(long t) { sessionStart = t; }
    public static synchronized void setSessionEnd(long t) { sessionEnd = t; }
    public static synchronized long getSessionStart() { return sessionStart; }
    public static synchronized long getSessionEnd() { return sessionEnd; }

    // -------------------------
    // 1분 미만 측정 → 강제 Awake 보정
    // -------------------------
    public static synchronized void adjustForShortSession() {
        long duration = sessionEnd - sessionStart;

        if (duration < 60_000) {
            stages.clear();
            stages.add(new SleepStage(sessionStart, sessionEnd, 0, 0f, 0));
        }
    }

    // -------------------------
    // 마지막 스테이지 접근/편집
    // -------------------------
    public static synchronized SleepStage getLastStage() {
        if (stages.isEmpty()) return null;
        return stages.get(stages.size() - 1);
    }

    public static synchronized void replaceLastStage(SleepStage s) {
        if (stages.isEmpty()) {
            stages.add(s);
        } else {
            stages.set(stages.size() - 1, s);
        }
    }

    public static synchronized boolean isEmpty() {
        return stages.isEmpty();
    }
}
