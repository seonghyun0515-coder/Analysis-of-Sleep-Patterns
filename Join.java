package ac.kr.project;

import android.app.Activity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class Join extends AppCompatActivity {

    EditText edtJID, edtJPe;
    Button btnJoinRegistration;

    SQLiteDatabase sqlDB; //DB 쓰기 전용, 읽기 전용으로 열기 처리
    myDBHelper myHelper;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.join); //디자인을 화면에 설정

        edtJID = (EditText) findViewById(R.id.jId);
        edtJPe = (EditText) findViewById(R.id.jPe);
        btnJoinRegistration = (Button) findViewById(R.id.jRegistration);
        myHelper=new myDBHelper(this);

        //회원가입 DB 생성 -> 테이블 생성 -> 회원추가 Insert

        //jId, jPw 입력란의 회원정보 입력 후 [회원가입버튼] Insert처리
        btnJoinRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sqlDB = myHelper.getWritableDatabase(); //쓰기 전용 DB 열기
                sqlDB.execSQL("INSERT INTO JoinInfo VALUES('"
                        + edtJID.getText().toString()+"', '"
                        + edtJPe.getText().toString() +"');"); //쿼리문 수향 메서드, 아이디/패스워드 레코드 추가

                //회원레코드1건 추가 완료 안내메세지 필요
                Toast.makeText(getApplicationContext(), "가입됨",
                        Toast.LENGTH_LONG).show();

                //메인으로 로그인하기 위해 화면 이동 처리
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        }); //btnJoinRegistration()
    } //onCreate()

    public class myDBHelper extends SQLiteOpenHelper {
        //code-overde 생성자() DB 생성, onCreate(), onUpgrade()

        //생성자 DB 생성
        public myDBHelper(Context context) {
            super(context, "LoginDB", null, 1); //DB명 생성
        }

        @Override
        public void onCreate(SQLiteDatabase db) { //DB에 테이블 생성
            db.execSQL("CREATE TABLE JoinInfo(uId TEXT, uPassword TEXT);"); //테이블 생성 쿼리 실행

        } //onCreate()

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //추상메서드 override 테이블이 있으면 삭제하고 다시 생성
            db.execSQL("DROP TABLE IF EXISTS JoinInfo"); //쿼리문으로 수행
            onCreate(db);

        } //onUpgrade()
    } //myDBHelper()
    //Join.java에서 jID, jPe의 값으로 회원가입 Insert -> 쓰기 전용DB 열기



} //Main
