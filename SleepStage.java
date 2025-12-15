package ac.kr.project;

public class SleepStage {
    public long startTime;
    public long endTime;
    public int stage;      // 0=Wake,1=Light,2=Deep,3=REM
    public float movement; // window 내 movement RMS
    public float breathRate; // breaths per minute (estimated)

    public SleepStage(long start, long end, int stage, float mov, float breath) {
        this.startTime = start;
        this.endTime = end;
        this.stage = stage;
        this.movement = mov;
        this.breathRate = breath;

    }
    @Override
    public String toString() {
        return "SleepStage{" +
                "start=" + startTime +
                ", end=" + endTime +
                ", stage=" + stage +
                ", mov=" + String.format("%.3f", movement) +
                ", breath=" + breathRate +
                '}';
    }
}
