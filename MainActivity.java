package ac.kr.project;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends Join {

    EditText edtId, edtPw;
    Button btnMainJoin, btnMainLogin;
    //화면의 구성이나, DB(테이블) 사용이 Join,java유사
    //로그인 아이디 일치(0,1), 비번일치(0,1)==>저장할 flog변수 선언
    int idFlag=0;//불일치 상태 초기화
    int pwFlag = 0;//불일치상태



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtId = (EditText) findViewById(R.id.edtId);
        edtPw = (EditText) findViewById(R.id.edtPe);
        btnMainJoin = (Button) findViewById(R.id.mainJoinBtn);
        btnMainLogin = (Button) findViewById(R.id.mainLoginBtn);

        //로그인화면 회원가입 처리 필요

        btnMainJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 회원가입 화면 이동
                Intent intent = new Intent(getApplicationContext(), Join.class); //화면 이동 인스턴스
                startActivity(intent);
            }
        }); //btnMainJoin()
        //로그인 처리ㅣ (아이디,비번, 핸드폰화면에서 가져옴)==>DB와 비교=>정상회원=>허기된 메뉴로 이동
        btnMainLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //edtId, edtPw에서 값을 가져옴==>임시저장
                //DB와 레코드 비교
                //회원정보를 2차원으로 select해서 cursor에 저장
                Cursor cursor;
                sqlDB=myHelper.getWritableDatabase();//쓰기전용으로 DB열기
                cursor=sqlDB.rawQuery("SELECT * FROM JoinInfo;",null);
                String mainId=null;//edtId,edtPw의 값을 임시 저장, 회원인지 확인을 레코드건별로 비교하기 위해
                String mainPw=null;
                String dbId=null;//첫번쨰 건을 저장할 dbId=null, dbPw=null;
                String dbPw=null;

                idFlag=0;
                pwFlag = 0;
                while(cursor.moveToNext()){//다음건이 있으면
                    dbId=cursor.getString(0);//DB첫 레코드ID임시저장첫번쨰 회원이 있다고 가정
                    dbPw=cursor.getString(1);//DB첫레코드패스워드 임시저장
                    mainId=edtId.getText().toString();//입력란에 ID임시 저장
                    mainPw=edtPw.getText().toString();
                    //입력란과 DB첫레코드 비교:di일치&&비번일치==>정상회원,id일치 비번불일치(다음레코드비교)
                    //아이디일치 비번 불일치==>비밀번호 오류입니다.
                    if(dbId.equals(mainId)){//아이디일치
                        idFlag=1;//비번 일치 확인
                        if(dbPw.equals(mainPw)){//아이디 비번 일치=>정상회원=>허가된 메뉴로 이동
                            pwFlag=1;
                            Toast.makeText(getApplicationContext(), "정상회원입니다.", Toast.LENGTH_SHORT).show();

                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean("isLoggedIn", true); // 로그인 상태 저장
                            editor.putString("userId", mainId);
                            editor.apply();

                            //허가된 회면으로 이동 menu.xml,Menu.java
                            Intent intent = new Intent(getApplicationContext(), Menu.class); //화면 이동 인스턴스
                            startActivity(intent);
                        }else {//아이디일치, 비번 불일치==>비번 오류 메세지
                            Toast.makeText(getApplicationContext(),"비밀번호를 다시 입력하세요", Toast.LENGTH_SHORT).show();
                        }
                    }else{//아이디

                    }
                }//while
                //회원가입을 독려(유도)
                cursor.close();
                sqlDB.close();
                if(idFlag==0&&pwFlag==0){//회원가입 안내
                    Toast.makeText(getApplicationContext(), "회원가입하세요", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}