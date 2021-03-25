package com.naver.test

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    //업데이트 시간을 저장할 프로퍼티
    var localUpdatetime:String? = null
    var serverUpdateTime: String? = null
    //페이지 번호
    var pageno: Int? = 1
    //페이지 당 데이터 개수
    var pagecount: Int? = 15
    //데이터 목록을 저장할 리스트
    var itemList: MutableList<Item>? = null
    //데이터 개수를 저장할 변수
    var count: Int? = null
    //ListView에 출력하기 위한 Adapter
    var itemAdapter: ItemAdapter? = null
    //화면에 출력 중 인 뷰
    var listview : ListView? = null
    var downloadview: ProgressBar? = null
    //데이터 파싱을 위한 프로퍼티
    var json:String? = null

    var displayHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val helper = DBHelper(this@MainActivity)
            val db = helper.readableDatabase
            val cursor= db.rawQuery("select itemid, itemname, price, description, pictureurl,updatedate from item order by itemid desc", null)
                while (cursor.moveToNext()){
                    val item = Item()
                    item.itemid = cursor.getInt(0)
                    item.itemname = cursor.getString(1)
                    item.price = cursor.getInt(2)
                    item.description = cursor.getString(3)
                    item.pictureurl = cursor.getString(4)
                    item.updatedate = cursor.getString(5)
                    itemList!!.add(item)
                }
                        db.close();
            Log.e("데이터 출력", itemList.toString())
            itemAdapter!!.notifyDataSetChanged()
            downloadview!!.visibility = View.GONE
        }
    }

    inner class ItemDownloadThread : Thread() {
        override fun run() {
            try {
                //다운로드 받을 주소 생성
                var url: URL = URL("http://172.30.34.114:8080/item/list")
                //연결 객체 생성
                val con = url!!.openConnection() as HttpURLConnection
                //옵션 설정
                con.requestMethod = "GET" //전송 방식 선택
                con.useCaches = false //캐시 사용 여부 설정
                con.connectTimeout = 30000 //접속 시도 시간 설정
                //문자열을 다운로드 받기 위한 스트림을 생성
                val br = BufferedReader(InputStreamReader(con.inputStream))
                val sb: StringBuilder = StringBuilder()
                //문자열을 읽어서 저장
                while (true) {
                    val line = br.readLine() ?: break
                    sb.append(line.trim())
                }
                json = sb.toString()
                //읽은 데이터 확인
                Log.e("읽은 데이터", json!!)
                //사용한 스트림과 연결 해제
                br.close()
                con.disconnect()
            } catch (e: Exception) {
                Log.e("다운로드 실패", e.message!!)
            }
            if(json != null) {
                val data = JSONObject(json)
                //데이터 개수 저장하기
                count = data.getInt("count")
                val fos = openFileOutput(
                    "count.txt",
                    Context.MODE_PRIVATE
                )
                fos.write("${count}".toByteArray())
                fos.close()
                //데이터 목록 가져오기
                val ar = data.getJSONArray("list")
                val helper = DBHelper(this@MainActivity)
                val db = helper.writableDatabase
                for (i in 0 until ar.length()) {
                    val obj = ar.getJSONObject(i)
                    val row = ContentValues()
                    row.put("itemid", obj.getInt("itemid"))
                    row.put("itemname", obj.getString("itemname"))
                    row.put("price", obj.getInt("price"))
                    row.put("description", obj.getString("description"))
                    row.put("pictureurl", obj.getString("pictureurl"))
                    row.put("updatedate", obj.getString("updatedate"))
                    db.insert("item", null, row)
                }
                db.close()
                displayHandler.sendEmptyMessage(1)
            }
        }
    }

    //업데이트 된 시간을 가져오는 스레드
    inner class UpdateTimeThread : Thread() {
        var task : Int = 1
        override fun run() {
            //업데이트 된 시간을 가져오기
            try {
                //다운로드 받을 주소 생성
                var url: URL = URL("http://172.30.34.114:8080/item/updatetime")
                //연결 객체 생성
                val con = url!!.openConnection() as HttpURLConnection
                //옵션 설정
                con.requestMethod = "GET" //전송 방식 선택
                con.useCaches = false //캐시 사용 여부 설정
                con.connectTimeout = 30000 //접속 시도 시간 설정
                //문자열을 다운로드 받기 위한 스트림을 생성
                val br = BufferedReader(InputStreamReader(con.inputStream))
                val sb: StringBuilder = StringBuilder()
                //문자열을 읽어서 저장
                while (true) {
                    val line = br.readLine() ?: break
                    sb.append(line.trim())
                }
                json = sb.toString()
                br.close()
                con.disconnect()
            } catch (e: Exception) {
                Log.e("다운로드 실패", e.message!!)
            }
            //json 파싱
            if(json != null){
                val data = JSONObject(json)
                serverUpdateTime = data.getString("result")
            }else{
                serverUpdateTime = ""
            }
            if(task == 1){
                val fos = openFileOutput("updatetime.txt", Context.MODE_PRIVATE)
                fos.write(serverUpdateTime?.toByteArray())
                fos.close()
                Log.e("업데이트 시간 저장", serverUpdateTime.toString())
                Log.e("로그", "서버에서 데이터를 새로 가져옵니다.")
                val helper = DBHelper(this@MainActivity)
                val db = helper.writableDatabase
                val itemSQL = "delete from item"
                db.execSQL(itemSQL)
                db.close()
                pageno = 1
                ItemDownloadThread().start()
            }
            else if(task == 2){
                if(serverUpdateTime.equals(localUpdatetime)){
                    Log.e("로그", "서버에서 데이터를 가져오지 않습니다.")
                    displayHandler.sendEmptyMessage(1)
                }else{
                    Log.e("4", "여기까지")
                    val fos = openFileOutput("updatetime.txt", Context.MODE_PRIVATE)
                    fos.write(serverUpdateTime?.toByteArray())
                    fos.close()
                    Log.e("로그", "서버에서 데이터를 다시 가져옵니다.")
                    val helper = DBHelper(this@MainActivity)
                    val db = helper.writableDatabase
                    val itemSQL = "delete from item"
                    db.execSQL(itemSQL)
                    db.close()
                    ItemDownloadThread().start()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        itemList = mutableListOf<Item>()
        listview = findViewById<ListView>(R.id.listview)
        downloadview = findViewById<ProgressBar>(R.id.downloadview)
        itemAdapter = ItemAdapter(this, itemList!!, R.layout.item_cell)
        listview?.adapter = itemAdapter
        listview?.setDivider(ColorDrawable(Color.RED))
        listview?.setDividerHeight(3)
    }
    override fun onResume() {
        super.onResume()
        //서버 업데이트 시간을 가져오기
        val updateTimeThread = UpdateTimeThread()
        try {
            //로컬 업데이트 시간을 가져오기
            val fis = openFileInput("updatetime.txt")
            val data = ByteArray(fis.available())
            while (fis.read(data) != -1) {
                Log.e("시작", "로컬 파일 읽기")
            }
            localUpdatetime = String(data)
            Log.e("로컬 업데이트 시간", localUpdatetime.toString())
            fis.close()
            val fisCount = openFileInput("count.txt")
            val dataCount = ByteArray(fisCount.available())
            while (fis.read(dataCount) != -1) {
                Log.e("시작", "로컬 파일 읽기")
            }
            count = String(dataCount).toInt()
            fisCount.close()
            Log.e("시작", "로컬 파일 있음")
            updateTimeThread.task = 2
            updateTimeThread.start()
        } catch (e: Exception) {
            Log.e("시작", "로컬 파일 없음")
            updateTimeThread.task = 1
            updateTimeThread.start()
        }
    }
}