package com.visualthreat.data.activity;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.visualthreat.data.canbus.CANBus;
import com.visualthreat.data.canbus.CANClient;
import com.visualthreat.data.canbus.CANEventListener;
import com.visualthreat.data.canbus.CANRawMessage;
import com.visualthreat.data.log.ExceptionHandler;
import com.visualthreat.data.menu.Menu;
import com.visualthreat.data.R;
import com.visualthreat.data.menu.RadialMenuWidget;
import com.visualthreat.data.serial.UsbService;
import com.visualthreat.data.speech.SpeechRecognizerManager;
import com.visualthreat.data.log.WriteLog;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * Created by USER on 1/2/2017.
 */
public class MainActivity extends Activity implements SpeechRecognizerManager.OnResultListener,RadialMenuWidget.OnTouch,CANClient,CANEventListener,EventPressMenu {

    private static final String TAG = MainActivity.class.getSimpleName();
    // speech voice
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private SpeechRecognizerManager mSpeechRecognizerManager;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private ListView mConversationView;
    private RadialMenuWidget PieMenu;
    private LinearLayout linear;
    private WriteLog writeLog = null;
    private TextView tv_status;
    private MainActivity main;
    private int height;
    private Menu menu_center, menu_turn_left, menu_turn_right, menu_backup, menu_straight,
    menu_traffic, menu_u_turn, menu_local, menu_freeway, menu_red_light, menu_green_light;
    private boolean flagsStart = false;
    private Context context;
    private Thread threadTimeout;
    private CANBus CANBus;

    private static final int NUM_ENTRIES_TO_SHOW = 6;
    private String[] entris = new String[NUM_ENTRIES_TO_SHOW];
    private int entry_index = 0;
    private long lastRefreshTime = 0;
    private TextView tvClear;

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private UsbService usbService;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(CANBus.GetHandler());
            CANBus.SetUSBSerial(usbService);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);
        context = this;
        SetUpSpeechText();
        CANBus = new CANBus(this);

        // Register crash listener of app to debug
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
    }

    //init speech to text
    private void SetUpSpeechText() {
        //declare menu circle
        main = this;
        tv_status = (TextView) findViewById(R.id.tv_status);
        linear = (LinearLayout)findViewById(R.id.linear);
        tvClear = (TextView) findViewById(R.id.clear);
        tvClear.setOnTouchListener(mOnTouchClear);

        //declare core speech to text
        mSpeechRecognizerManager = new SpeechRecognizerManager(this);
        mSpeechRecognizerManager.setOnResultListner(this);

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this,
                R.layout.message);
        mConversationView = (ListView) findViewById(R.id.tv_response);
        mConversationView.setAdapter(mConversationArrayAdapter);

        //declare item menu of circle menu
        menu_center = new Menu("car",null,R.drawable.car);
        menu_center.SetEventPressMenu(this);
        menu_turn_left = new Menu("turn left","Turn Left",R.drawable.icon_left);
        menu_turn_left.SetEventPressMenu(this);
        menu_turn_right = new Menu("turn right","Turn Right",R.drawable.icon_right);
        menu_turn_right.SetEventPressMenu(this);
        menu_backup = new Menu("backup","Backup",R.drawable.icon_backup);
        menu_backup.SetEventPressMenu(this);
        menu_straight = new Menu("straight","Straight",R.drawable.icon_straight);
        menu_straight.SetEventPressMenu(this);
        menu_traffic = new Menu("traffic","Traffic",R.drawable.icon_traffic);
        menu_traffic.SetEventPressMenu(this);
        menu_u_turn = new Menu("u turn","U Turn",R.drawable.icon_u_turn);
        menu_u_turn.SetEventPressMenu(this);
        menu_local = new Menu("local","Local",R.drawable.icon_local);
        menu_local.SetEventPressMenu(this);
        menu_freeway = new Menu("freeway","Freeway",R.drawable.icon_highway);
        menu_freeway.SetEventPressMenu(this);
        menu_red_light = new Menu("red light","Red Light",R.drawable.icon_red_light);
        menu_red_light.SetEventPressMenu(this);
        menu_green_light = new Menu("green light","Green Light",R.drawable.icon_green_light);
        menu_green_light.SetEventPressMenu(this);

        linear.post(new Runnable() {
            @Override
            public void run() {
                height = linear.getHeight();
                Add_menu_circle(linear,height);
                PieMenu.Set_OnTouch(main);
            }
        });

        //check permission for android > 6.0
        int permissionCheckRecordAudio = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        int permissionCheckWriteExternal = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheckRecordAudio == PackageManager.PERMISSION_DENIED || permissionCheckWriteExternal == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
    }

    private View.OnTouchListener mOnTouchClear = new View.OnTouchListener(){

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()){
                case MotionEvent.ACTION_DOWN:
                    tvClear.setTextColor(Color.GRAY);
                    break;
                case MotionEvent.ACTION_UP:
                    tvClear.setTextColor(Color.WHITE);
                    mConversationArrayAdapter.clear();
                    break;
            }
            return true;
        }
    };
    /**
     * return result user choose permission
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mSpeechRecognizerManager = new SpeechRecognizerManager(this);
                mSpeechRecognizerManager.setOnResultListner(this);
            } else {
                finish();
            }
        }
    }

    /**
     * callback when the app destroy
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        CANBus.unRegisterListener(this);
        CANBus.stop();
        mSpeechRecognizerManager.OnDestroy();
        writeLog.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CANBus.registerListener(this);
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    /**
     * return result detect
     * @param result result
     */
    @Override
    public void OnResult(int result) {
        ResetAll();
        switch (result) {
            case SpeechRecognizerManager.START:
                //declare log file
                if(writeLog != null) {
                    writeLog.close();
                }
                writeLog = new WriteLog(this);
                String log = writeLog.GeneralLog("Command Begin");
                mConversationArrayAdapter.insert(log,0);
                writeLog.addLog(log);
                tv_status.setText("Command Begin");
                menu_center.setIcon(R.drawable.car_connect);
                flagsStart = true;

                //start
                CANBus.start(writeLog);

                break;
            case SpeechRecognizerManager.STOP:
                //stop
                CANBus.stop();
                flagsStart = false;
                menu_center.setIcon(R.drawable.car);
                log = writeLog.GeneralLog("Command Stop Logging");
                mConversationArrayAdapter.insert(log,0);
                writeLog.addLog(log);
                tv_status.setText("Command Stop Logging");
                break;
            case SpeechRecognizerManager.TURN_LEFT:
                handleTurnLeft();
                break;
            case SpeechRecognizerManager.TURN_RIGHT:
                handleTurnRight();
                break;
            case SpeechRecognizerManager.BACKUP:
                hanleBackup();
                break;
            case SpeechRecognizerManager.STRAIGHT:
                handreStraight();
                break;
            case SpeechRecognizerManager.TRAFFIC:
                handleTraffic();
                break;
            case SpeechRecognizerManager.U_TURN:
                handleUTurn();
                break;
            case SpeechRecognizerManager.LOCAL:
                handleLocal();
                break;
            case SpeechRecognizerManager.FREEWAY:
                handleFreeWay();
                break;
            case SpeechRecognizerManager.RED_LIGHT:
                handleRedLight();
                break;
            case SpeechRecognizerManager.GREEN_LIGHT:
                handleGreenLight();
                break;
        }
        PieMenu.DrawAgain();
    }

    /**
     * when press TurnLeft or voice turn left
     */
    private void handleTurnLeft(){
        String log =  writeLog.GeneralLog("Command Turn Left");
        mConversationArrayAdapter.insert(log,0);
        writeLog.addLog(log);
        tv_status.setText("Command Turn Left");
        menu_turn_left.setIcon(R.drawable.icon_left_green);
    }

    /**
     * when press TurnRight or voice TurnRight
     */
    private void handleTurnRight(){
        String log = writeLog.GeneralLog("Command Turn Right");
        mConversationArrayAdapter.insert(log,0);
        writeLog.addLog(log);
        tv_status.setText("Command Turn Right");
        menu_turn_right.setIcon(R.drawable.icon_right_green);
    }

    /**
     * when press Backup or voice Backup
     */
    private void hanleBackup(){
        String log = writeLog.GeneralLog("Command BackUp");
        mConversationArrayAdapter.insert(log ,0);
        writeLog.addLog(log);
        tv_status.setText("Command BackUp");
        menu_backup.setIcon(R.drawable.icon_backup_green);
    }

    /**
     * when press Straight or voice Straight
     */
    private void handreStraight(){
        String log = writeLog.GeneralLog("Command Straight");
        mConversationArrayAdapter.insert(log,0);
        writeLog.addLog(log);
        tv_status.setText("Command Straight");
        menu_straight.setIcon(R.drawable.icon_straight_green);
    }

    /**
     * when press Traffic or voice Traffic
     */
    private void handleTraffic(){
        String log = writeLog.GeneralLog("Command Traffic");
        mConversationArrayAdapter.insert(log,0);
        writeLog.addLog(log);
        tv_status.setText("Command Traffic");
        menu_traffic.setIcon(R.drawable.icon_traffic_green);
    }

    /**
     * when press U Turn or voice U Turn
     */
    private void handleUTurn(){
        String log = writeLog.GeneralLog("Command U Turn");
        mConversationArrayAdapter.insert(log,0);
        writeLog.addLog(log);
        tv_status.setText("Command U Turn");
        menu_u_turn.setIcon(R.drawable.icon_u_turn_green);
    }

    /**
     * when press Local or voice Local
     */
    private void handleLocal(){
        String log = writeLog.GeneralLog("Command Local");
        mConversationArrayAdapter.insert(log ,0);
        writeLog.addLog(log);
        tv_status.setText("Command Local");
        menu_local.setIcon(R.drawable.icon_local_green);
    }

    /**
     * when press FreeWay or voice FreeWay
     */
    private void handleFreeWay(){
        String log = writeLog.GeneralLog("Command Free Way");
        mConversationArrayAdapter.insert(log,0);
        writeLog.addLog(log);
        tv_status.setText("Command Free Way");
        menu_freeway.setIcon(R.drawable.icon_highway_green);
    }

    /**
     * when press ReaLight or voice RedLight
     */
    private void handleRedLight(){
        String log = writeLog.GeneralLog("Command Red Light");
        mConversationArrayAdapter.insert(log,0);
        writeLog.addLog(log);
        tv_status.setText("Command Red Light");
        menu_red_light.setIcon(R.drawable.icon_red_light_green);
    }

    /**
     * when press GreenLight or GreenRight
     */
    private void handleGreenLight(){
        String log = writeLog.GeneralLog("Command Green Light");
        mConversationArrayAdapter.insert(log,0);
        writeLog.addLog(log);
        tv_status.setText("Command Green Light");
        menu_green_light.setIcon(R.drawable.icon_green_light_green);
    }

    /**
     * Convert dp to px
     * @param dp dp
     * @return px
     */
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }

    /**
     * create menu circle
     * @param l linnear of linnear
     * @param toolbar_height height
     */
    private void Add_menu_circle(LinearLayout l,int toolbar_height){
        Display display = getWindowManager().getDefaultDisplay();
        int width = l.getWidth();
        int height = l.getHeight();
        PieMenu = new RadialMenuWidget(getBaseContext());

        PieMenu.setAnimationSpeed(0L);
        PieMenu.setIconSize(width/15 - 20, width/15);
        PieMenu.setTextSize(13);
        int MaxSize;
        int MinSize;
        int r2MinSize;
        int r2MaxSize;
        int cRadius;

        if((width/6)*4 < height) {
            MaxSize = (width/8) * 2;
            MinSize = MaxSize - width/8 - 10;
            r2MinSize = MaxSize + 20;
            r2MaxSize = r2MinSize + 50;
            cRadius = MinSize;
        }
        else{
            MaxSize = (height/3) - 20;
            MinSize = MaxSize - height/6 - 10;
            r2MinSize = MaxSize + 20;
            r2MaxSize = r2MinSize + 50;
            cRadius = MinSize;
        }

        PieMenu.setInnerRingRadius(MinSize, MaxSize);
        PieMenu.setOuterRingRadius(r2MinSize, r2MaxSize);
        PieMenu.setCenterCircleRadius(cRadius);
        PieMenu.setCenterLocation(width / 2 - dpToPx(2), toolbar_height / 2);

        PieMenu.setCenterCircle(menu_center);
        PieMenu.addMenuEntry(menu_turn_left);
        PieMenu.addMenuEntry(menu_turn_right);
        PieMenu.addMenuEntry(menu_straight);
        PieMenu.addMenuEntry(menu_backup);
        PieMenu.addMenuEntry(menu_traffic);
        PieMenu.addMenuEntry(menu_u_turn);
        PieMenu.addMenuEntry(menu_local);
        PieMenu.addMenuEntry(menu_freeway);
        PieMenu.addMenuEntry(menu_red_light);
        PieMenu.addMenuEntry(menu_green_light);
        l.addView(PieMenu);
    }

    /**
     * callback when touch on button of PieMenu
     */
    @Override
    public void onTouch() {

    }

    @Override
    public void onTouchCenter() {
        if(flagsStart){
            mSpeechRecognizerManager.Stop();
        }
        else {
            mSpeechRecognizerManager.Wakeup();
        }
    }

    /**
     * reset all image menu
     */
    private void ResetAll() {
        menu_turn_left.setIcon(R.drawable.icon_left);
        menu_turn_right.setIcon(R.drawable.icon_right);
        menu_backup.setIcon(R.drawable.icon_backup);
        menu_straight.setIcon(R.drawable.icon_straight);
        menu_traffic.setIcon(R.drawable.icon_traffic);
        menu_u_turn.setIcon(R.drawable.icon_u_turn);
        menu_local.setIcon(R.drawable.icon_local);
        menu_freeway.setIcon(R.drawable.icon_highway);
        menu_red_light.setIcon(R.drawable.icon_red_light);
        menu_green_light.setIcon(R.drawable.icon_green_light);
    }

    @Override
    public void disconnected(int id) {

    }

    @Override
    public void writeLog(int id, final String text) {
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConversationArrayAdapter.insert(writeLog.addLog(text),0);
            }
        });*/

    }

    @Override
    public void writeLog(int id, String text, Throwable t) {

    }

    @Override
    public void onEvent(final String event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!flagsStart) {
                    return;
                }
                entris[entry_index] = event;
                if(lastRefreshTime + 500 < System.currentTimeMillis()) {
                    int tmpIndex = entry_index;
                    mConversationArrayAdapter.clear();
                    for(int i = 0; i < NUM_ENTRIES_TO_SHOW ; i++) {
                        if(entris[tmpIndex] == null || entris[tmpIndex].length() == 0) {
                            break;
                        }
                        mConversationArrayAdapter.insert(entris[tmpIndex], i);
                        tmpIndex--;
                        if(tmpIndex < 0) {
                            tmpIndex += NUM_ENTRIES_TO_SHOW;
                        }
                    }
                    lastRefreshTime = System.currentTimeMillis();
                }
                entry_index = (++entry_index) % NUM_ENTRIES_TO_SHOW;

            }
        });
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void OnClickMenu(String name) {
        Log.d(TAG,"OnClclMenu: " + name);
        ResetAll();
        if(flagsStart){
            if(name.equals("turn left")){
                handleTurnLeft();
            }
            else if(name.equals("turn right")){
                handleTurnRight();
            }
            else if(name.equals("straight")){
                handreStraight();
            }
            else if(name.equals("backup")){
                hanleBackup();
            }
            else if(name.equals("traffic")){
                handleTraffic();
            }
            else if(name.equals("u turn")){
                handleUTurn();
            }
            else if(name.equals("local")){
                handleLocal();
            }
            else if(name.equals("freeway")){
                handleFreeWay();
            }
            else if(name.equals("red light")){
                handleRedLight();
            }
            else if(name.equals("green light")){
                handleGreenLight();
            }
        }
    }

    public class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    //mActivity.get().display.append(data);
                    break;
                case UsbService.CTS_CHANGE:
                    //Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    //Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.SYNC_READ:
                    String buffer = (String) msg.obj;
                    buffer = buffer + "\r\n";
                    mActivity.get().mConversationArrayAdapter.insert(buffer,0);
                    break;
            }
        }
    }
}
