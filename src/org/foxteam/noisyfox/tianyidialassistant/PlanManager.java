package org.foxteam.noisyfox.tianyidialassistant;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class PlanManager {

    private Context mContext = null;
    MyDataBaseHelper mDatabase;
    private List<Plan> mPlans = new LinkedList<Plan>();

    public PlanManager(Context context) {
        mContext = context;
        mDatabase = new MyDataBaseHelper(context);
        load();
    }

    public synchronized void shutdownTask() {
        boolean needWait = false;
        synchronized (mTaskWaitQueue) {
            if (mTaskThread != null && mTaskThread.isAlive()) {
                mTaskRun = false;
                mTaskWaitQueue.notify();
                needWait = true;
            }
        }
        if (needWait) {
            try {
                mTaskThread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public synchronized void postTask(List<Plan> plans) {
        if (plans == null) {
            return;
        }
        synchronized (mTaskWaitQueue) {
            boolean add = false;
            for (Plan p : plans) {
                p = new Plan(p);
                mTaskWaitQueue.offer(p);
                add = true;
            }
            mTaskWaitQueue.notifyAll();
            if (add && !mTaskRun) {
                mTaskRun = true;
                mTaskThread = new TaskThread();
                mTaskThread.start();
            }
        }
    }

    TaskThread mTaskThread = null;
    private boolean mTaskRun = false;
    private HashMap<Long, Plan> mTaskPoolMap = new HashMap<Long, Plan>();
    private Queue<Plan> mTaskPoolQueue = new LinkedList<Plan>();
    private Queue<Plan> mTaskWaitQueue = new LinkedList<Plan>();

    private class TaskThread extends Thread {
        @Override
        public void run() {
            while (mTaskRun) {
                synchronized (mTaskWaitQueue) {
                    while (!mTaskWaitQueue.isEmpty()) {
                        Plan p = mTaskWaitQueue.poll();
                        if (!mTaskPoolMap.containsKey(Long.valueOf(p.id))) {
                            mTaskPoolMap.put(Long.valueOf(p.id), p);
                            mTaskPoolQueue.add(p);
                        }
                    }
                    if (mTaskPoolQueue.isEmpty()) {
                        try {
                            mTaskWaitQueue.wait(200 * 1000);
                        } catch (InterruptedException e) {
                        }
                        if (!mTaskRun) {
                            return;
                        }
                        while (!mTaskWaitQueue.isEmpty()) {
                            Plan p = mTaskWaitQueue.poll();
                            if (!mTaskPoolMap.containsKey(Long.valueOf(p.id))) {
                                mTaskPoolMap.put(Long.valueOf(p.id), p);
                                mTaskPoolQueue.add(p);
                            }
                        }
                        if (mTaskPoolQueue.isEmpty()) {
                            mTaskRun = false;
                            return;
                        }
                    }
                }
                Plan plan = mTaskPoolQueue.poll();
                mTaskPoolMap.remove(Long.valueOf(plan.id));
                // do task
                Log.d("task", "Task executed!");
                boolean success = false;
                switch (plan.action) {
                    case 0:// 上传密码并连接网络
                        success = uploadPswAndConnect();
                        break;
                    case 1:// 连接网络
                        success = toggleConnection(true);
                        break;
                    case 2:// 断开网络
                        success = toggleConnection(false);
                        break;
                    case 3:// 上传密码
                        success = uploadPsw();
                        break;
                }
                if (!success && plan.retry < 3) {
                    synchronized (mTaskWaitQueue) {
                        mTaskWaitQueue.offer(plan);
                    }
                    plan.retry++;
                }
            }

        }
    }

    private boolean uploadPswAndConnect() {
        if (!uploadPsw())
            return false;

        return toggleConnection(true);
    }

    private boolean uploadPsw() {
        PSWOperator pswOper = new PSWOperator(mContext);
        String psw = pswOper.getLastPsw(false);
        if (psw.isEmpty()) {
            return true;
        }

        OpenWrtHelper owh = MyApplication.getOpenWrtHelper();

        owh.loadSSHSettings(mContext);
        boolean needWifiClose = owh.beforeAction(mContext);
        boolean success = owh.updatePSW(psw);
        owh.afterAction(mContext, needWifiClose, true);

        return success;
    }

    private boolean toggleConnection(boolean connect) {
        OpenWrtHelper owh = MyApplication.getOpenWrtHelper();

        owh.loadSSHSettings(mContext);
        boolean needWifiClose = owh.beforeAction(mContext);
        boolean success = owh.toggleDial(connect);
        owh.afterAction(mContext, needWifiClose, true);

        return success;
    }

    public List<Plan> getAllPlans(int timeType) {
        List<Plan> plans = new LinkedList<Plan>();
        SQLiteDatabase db = mDatabase.getReadableDatabase();
        Cursor c = db.query("plan", null, "time_type = " + timeType, null,
                null, null, null);

        if (c.moveToFirst()) {
            do {
                Plan p = new Plan();
                p.id = c.getLong(c.getColumnIndex("id"));
                p.time = c.getInt(c.getColumnIndex("time_type"));
                p.action = c.getInt(c.getColumnIndex("action_type"));
                p.time_hour = c.getInt(c.getColumnIndex("time_hour"));
                p.time_minute = c.getInt(c.getColumnIndex("time_minute"));

                plans.add(p);
            } while (c.moveToNext());
        }

        return plans;
    }

    private void load() {
        mPlans.clear();

        SQLiteDatabase db = mDatabase.getReadableDatabase();
        Cursor c = db.query("plan", null, null, null, null, null, null);

        if (c.moveToFirst()) {
            do {
                Plan p = new Plan();
                p.id = c.getLong(c.getColumnIndex("id"));
                p.time = c.getInt(c.getColumnIndex("time_type"));
                p.action = c.getInt(c.getColumnIndex("action_type"));
                p.time_hour = c.getInt(c.getColumnIndex("time_hour"));
                p.time_minute = c.getInt(c.getColumnIndex("time_minute"));

                mPlans.add(p);
            } while (c.moveToNext());
        }
    }

    private Plan getPlanById(long id) {
        getAllPlans(0);
        SQLiteDatabase db = mDatabase.getReadableDatabase();
        Cursor c = db.query("plan", null, "id = " + id, null, null, null, null);

        if (c.moveToFirst()) {
            Plan p = new Plan();
            p.id = c.getLong(c.getColumnIndex("id"));
            p.time = c.getInt(c.getColumnIndex("time_type"));
            p.action = c.getInt(c.getColumnIndex("action_type"));
            p.time_hour = c.getInt(c.getColumnIndex("time_hour"));
            p.time_minute = c.getInt(c.getColumnIndex("time_minute"));

            return p;
        }

        return null;
    }

    private static class MyDataBaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "plan.db";
        private static final int DB_VERSION = 1;

        public MyDataBaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE plan(id integer primary key autoincrement, time_type, action_type, time_hour, time_minute, enabled);";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }

    public PlanAdapter getAdapter(Context context) {
        return new PlanAdapter(context);
    }

    public static class Plan {
        public Plan() {

        }

        public Plan(Plan p) {
            id = p.id;
            time = p.time;
            action = p.action;
            time_hour = p.time_hour;
            time_minute = p.time_minute;
        }

        public long id = 0;
        public int time = 0;
        public int action = 0;
        public int time_hour = 0;
        public int time_minute = 0;

        private int retry = 0;
    }

    public static interface OnPlanEditListener {
        public void onPlanEdit(Plan plan);
    }

    public class PlanAdapter extends BaseAdapter implements OnClickListener {

        Context mContext;

        public PlanAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return mPlans.size();
        }

        @Override
        public Object getItem(int position) {
            return mPlans.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mPlans.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = LayoutInflater.from(mContext).inflate(
                        R.layout.plan_list_item, null);

            Plan plan = (Plan) getItem(position);

            View button_delete = convertView.findViewById(R.id.button_delete);
            TextView textView_des = (TextView) convertView
                    .findViewById(R.id.textView_des);

            button_delete.setTag(getItemId(position));
            button_delete.setOnClickListener(this);

            String str_action = mContext.getResources().getStringArray(
                    R.array.plan_action)[plan.action];

            String strDes;
            if (plan.time == 0) {
                strDes = mContext.getResources().getString(
                        R.string.text_plan_des_refresh, str_action);
            } else {
                strDes = mContext.getResources().getString(
                        R.string.text_plan_des_daytime, plan.time_hour,
                        plan.time_minute, str_action);
            }

            textView_des.setText(strDes);

            return convertView;
        }

        @Override
        public void onClick(View v) {
            long id = (Long) v.getTag();
            SQLiteDatabase db = mDatabase.getWritableDatabase();
            db.delete("plan", "id = ?", new String[]{String.valueOf(id)});
            load();
            this.notifyDataSetChanged();
        }

    }

    public static void showPlanEditDialog(Context context, Plan p,
                                          final OnPlanEditListener onEdit) {
        final Plan plan;
        if (p == null) {
            plan = new Plan();
        } else {
            plan = p;
        }

        final View layout = LayoutInflater.from(context).inflate(
                R.layout.new_plan_dialog, null);
        final AlertDialog ad = new AlertDialog.Builder(context).create();

        final Spinner spinner_time = (Spinner) layout
                .findViewById(R.id.spinner_time);
        final Spinner spinner_action = (Spinner) layout
                .findViewById(R.id.spinner_action);
        final TimePicker timePicker = (TimePicker) layout
                .findViewById(R.id.timePicker_time);

        spinner_time.setSelection(plan.time);
        spinner_action.setSelection(plan.action);

        if (plan.time == 1) {
            timePicker.setCurrentHour(plan.time_hour);
            timePicker.setCurrentMinute(plan.time_minute);
        }

        layout.findViewById(R.id.button_cancel).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ad.cancel();
                    }
                });

        layout.findViewById(R.id.button_ok).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        plan.time = spinner_time.getSelectedItemPosition();
                        plan.action = spinner_action.getSelectedItemPosition();
                        plan.time_hour = timePicker.getCurrentHour();
                        plan.time_minute = timePicker.getCurrentMinute();
                        ad.cancel();
                        if (onEdit != null) {
                            onEdit.onPlanEdit(plan);
                        }
                    }
                });

        spinner_time.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (position == 1) {
                    timePicker.setVisibility(View.VISIBLE);
                } else {
                    timePicker.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }

        });

        ad.setView(layout, 0, 0, 0, 0);
        ad.show();
    }

    public void createPlan(Context context, final OnPlanEditListener l) {
        showPlanEditDialog(context, null, new OnPlanEditListener() {
            @Override
            public void onPlanEdit(Plan plan) {
                ContentValues cv = new ContentValues();
                cv.put("time_type", plan.time);
                cv.put("action_type", plan.action);
                cv.put("time_hour", plan.time_hour);
                cv.put("time_minute", plan.time_minute);
                cv.put("enabled", true);
                SQLiteDatabase db = mDatabase.getWritableDatabase();
                long id = db.insert("plan", null, cv);
                if (id == -1) {
                    if (l != null) {
                        l.onPlanEdit(null);
                        return;
                    }
                }
                plan.id = id;
                Log.d("id", "id:" + id);
                load();
                if (l != null) {
                    l.onPlanEdit(plan);
                }
            }
        });
    }

    public void editPlan(Context context, final long id,
                         final OnPlanEditListener l) {
        Plan p = getPlanById(id);

        if (p == null) {
            if (l != null) {
                l.onPlanEdit(null);
            }
            return;
        }

        showPlanEditDialog(context, p, new OnPlanEditListener() {
            @Override
            public void onPlanEdit(Plan plan) {
                ContentValues cv = new ContentValues();
                cv.put("time_type", plan.time);
                cv.put("action_type", plan.action);
                cv.put("time_hour", plan.time_hour);
                cv.put("time_minute", plan.time_minute);
                cv.put("enabled", true);
                SQLiteDatabase db = mDatabase.getWritableDatabase();
                int i = db.update("plan", cv, "id = ?",
                        new String[]{String.valueOf(id)});
                if (i < 1) {
                    if (l != null) {
                        l.onPlanEdit(null);
                        return;
                    }
                }
                load();
                if (l != null) {
                    l.onPlanEdit(plan);
                }
            }
        });
    }

}
